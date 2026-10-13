package com.corclan.clan;

import com.corclan.icons.MemberCosmetics;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The staff's promotion assistant for the Gem League: who has earned a higher gem rank, and who is close. Pure Java so
 * it can be unit tested; the plugin feeds it the clan's member list (names, rank titles, join dates from the game) and
 * the gz's each member has given. It only ever suggests: a staff member changes the rank in the game by hand, because
 * plugins can't (and must not) change clan ranks.
 * <p>
 * A member whose gz's nobody counted (no plugin, or clan sync off) counts as 0 gz: steps that need gz's can't be
 * checked for them, so they are never suggested on time alone.
 */
public final class Promotions
{
	/** One step up the Gem League: how long in the clan and how many gz's given it takes to go from one gem to the next. */
	public static final class Step
	{
		public final String from;
		public final String to;
		/** days since the member joined the clan */
		public final int days;
		/** all-time gz's given; 0 = time only */
		public final int gz;

		Step(String from, String to, int days, int gz)
		{
			this.from = from;
			this.to = to;
			this.days = days;
			this.gz = gz;
		}
	}

	/**
	 * The CoR Gem League ladder, lowest first. Everyone starts at Opal; the first rank-up needs 100 gz's and Zenyte 5000
	 * (Ray); the steps between are spread out evenly. You need the plugin to rank up: uncounted gz's are 0. Edit here.
	 * Titles must match {@link OrgChart#GEM_LEAGUE}.
	 */
	public static final List<Step> LADDER = Collections.unmodifiableList(Arrays.asList(
		new Step("Opal", "Sapphire", 7, 100),
		new Step("Sapphire", "Emerald", 14, 250),
		new Step("Emerald", "Ruby", 30, 500),
		new Step("Ruby", "Diamond", 60, 1000),
		new Step("Diamond", "Dragonstone", 90, 2000),
		new Step("Dragonstone", "Onyx", 180, 3500),
		new Step("Onyx", "Zenyte", 365, 5000)));

	/** members this many days or fewer from their next step show as coming up */
	static final int SOON_DAYS = 7;
	/** ... or with this share of the step's gz's still to give, at most */
	static final double SOON_GZ_SHARE = 0.2;

	/** One clan member as the assistant needs it. */
	public static final class Member
	{
		final String name;
		final String title;
		/** null when the game didn't say */
		final LocalDate joined;
		final boolean online;

		public Member(String name, String title, LocalDate joined, boolean online)
		{
			this.name = name;
			this.title = title;
			this.joined = joined;
			this.online = online;
		}
	}

	/** A member who is due a promotion, or close to one. */
	public static final class Candidate
	{
		public final String name;
		/** their gem now */
		public final String from;
		/** the highest gem they have earned (due), or the next one (coming up) */
		public final String to;
		public final int days;
		/** all-time gz's given, or null when nobody counted them */
		public final Integer gz;
		public final boolean online;
		/** coming up only: days and gz's still to go (0 when that part is met) */
		public final int daysLeft;
		public final int gzLeft;

		Candidate(String name, String from, String to, int days, Integer gz, boolean online, int daysLeft, int gzLeft)
		{
			this.name = name;
			this.from = from;
			this.to = to;
			this.days = days;
			this.gz = gz;
			this.online = online;
			this.daysLeft = daysLeft;
			this.gzLeft = gzLeft;
		}

		/** coming up only, e.g. "3 days" or "3 days, 12 gz" */
		public String still()
		{
			List<String> parts = new ArrayList<>();
			if (daysLeft > 0)
			{
				parts.add(daysLeft == 1 ? "1 day" : daysLeft + " days");
			}
			if (gzLeft > 0)
			{
				parts.add(gzLeft + " gz");
			}
			return String.join(", ", parts);
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof Candidate))
			{
				return false;
			}
			Candidate c = (Candidate) o;
			return days == c.days && online == c.online && daysLeft == c.daysLeft && gzLeft == c.gzLeft
				&& name.equals(c.name) && from.equals(c.from) && to.equals(c.to) && Objects.equals(gz, c.gz);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(name, from, to, days, gz, online, daysLeft, gzLeft);
		}
	}

	/** What the assistant found. */
	public static final class Result
	{
		/** earned a higher gem; highest target first */
		public final List<Candidate> due;
		/** close to the next gem; closest first */
		public final List<Candidate> soon;

		Result(List<Candidate> due, List<Candidate> soon)
		{
			this.due = Collections.unmodifiableList(due);
			this.soon = Collections.unmodifiableList(soon);
		}

		@Override
		public boolean equals(Object o)
		{
			return o instanceof Result && due.equals(((Result) o).due) && soon.equals(((Result) o).soon);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(due, soon);
		}
	}

	private Promotions()
	{
	}

	/**
	 * @param today the game's date (UTC)
	 * @param gzGiven all-time gz's given, by {@link MemberCosmetics#key}; a missing name means not counted
	 */
	public static Result check(List<Member> members, LocalDate today, Map<String, Integer> gzGiven)
	{
		List<Candidate> due = new ArrayList<>();
		List<Candidate> soon = new ArrayList<>();
		for (Member m : members)
		{
			int step = stepFrom(m.title);
			if (m.name == null || step < 0 || m.joined == null)
			{
				continue;
			}
			int days = (int) Math.max(0, ChronoUnit.DAYS.between(m.joined, today));
			Integer gz = gzGiven.get(MemberCosmetics.key(m.name));

			int reached = step;
			while (reached < LADDER.size() && met(LADDER.get(reached), days, gz))
			{
				reached++;
			}
			String from = LADDER.get(step).from;
			if (reached > step)
			{
				due.add(new Candidate(m.name, from, LADDER.get(reached - 1).to, days, gz, m.online, 0, 0));
				continue;
			}

			Step next = LADDER.get(step);
			int daysLeft = Math.max(0, next.days - days);
			int gzLeft = Math.max(0, next.gz - (gz == null ? 0 : gz));
			if (daysLeft <= SOON_DAYS && gzLeft <= Math.ceil(next.gz * SOON_GZ_SHARE))
			{
				soon.add(new Candidate(m.name, from, next.to, days, gz, m.online, daysLeft, gzLeft));
			}
		}
		due.sort(Comparator.comparingInt((Candidate c) -> -stepTo(c.to))
			.thenComparing(c -> c.name, String.CASE_INSENSITIVE_ORDER));
		soon.sort(Comparator.comparingInt((Candidate c) -> c.daysLeft)
			.thenComparingInt(c -> c.gzLeft)
			.thenComparing(c -> c.name, String.CASE_INSENSITIVE_ORDER));
		return new Result(due, soon);
	}

	private static boolean met(Step step, int days, Integer gz)
	{
		return days >= step.days && (gz == null ? 0 : gz) >= step.gz;
	}

	/** index of the step that leaves this gem, or -1 (not a gem, or already Zenyte) */
	private static int stepFrom(String title)
	{
		if (title == null)
		{
			return -1;
		}
		String t = OrgChart.normalize(title);
		for (int i = 0; i < LADDER.size(); i++)
		{
			if (OrgChart.normalize(LADDER.get(i).from).equals(t))
			{
				return i;
			}
		}
		return -1;
	}

	private static int stepTo(String title)
	{
		for (int i = 0; i < LADDER.size(); i++)
		{
			if (LADDER.get(i).to.equals(title))
			{
				return i;
			}
		}
		return -1;
	}
}

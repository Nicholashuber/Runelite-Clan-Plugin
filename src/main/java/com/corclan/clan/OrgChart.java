package com.corclan.clan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Lays the clan's rank groups out as the CoR org chart. Two chains of boxes, top to bottom:
 * <ul>
 * <li>staff: three tiers of leadership ranks, matched by title</li>
 * <li>competitive: the Gem League entry ranks (Zenyte ... Opal) players start in, then the letter tiers
 * (S = Soul ... F = Gnome child) players will graduate into. The letter tiers aren't open yet, so they
 * come last and are marked {@link Tier#lockedNote locked}</li>
 * </ul>
 * Ranks are matched by their in-game title, since clans rename ranks freely. Every listed rank is
 * shown, vacant if nobody holds it. Anything left over goes into {@link Chart#others}.
 */
public final class OrgChart
{
	/** One tier's definition: a label, its rank titles in display order, and optional letter grades. */
	static final class TierDef
	{
		final String name;
		final List<String> titles;
		/** one letter per title (e.g. "SABCDEF"), or null for ungraded tiers */
		final String grades;

		TierDef(String name, String... titles)
		{
			this(name, null, Arrays.asList(titles));
		}

		private TierDef(String name, String grades, List<String> titles)
		{
			if (grades != null && grades.length() != titles.size())
			{
				throw new IllegalArgumentException(name + ": need one grade per title");
			}
			this.name = name;
			this.titles = titles;
			this.grades = grades;
		}

		static TierDef graded(String name, String grades, String... titles)
		{
			return new TierDef(name, grades, Arrays.asList(titles));
		}
	}

	/** The CoR staff hierarchy, top tier first. Edit here to change the chart. */
	static final List<TierDef> STAFF_TIERS = Arrays.asList(
		new TierDef("Owners", "Owner", "Deputy Owner"),
		new TierDef("Staff", "Marshal", "General", "Brigadier", "Colonel", "Administrator"),
		new TierDef("Hall of Fame", "Major", "Goon", "Executive", "Short green guy", "Hero"));

	/** Entry league, highest first: players start at Opal and work up to Zenyte. */
	static final TierDef GEM_LEAGUE = new TierDef("Gem League",
		"Zenyte", "Onyx", "Dragonstone", "Diamond", "Ruby", "Emerald", "Sapphire", "Opal");

	/** Competitive bracket, S tier first (placeholder titles; edit here). Earned by gz count. */
	static final TierDef LETTER_TIERS = TierDef.graded("Challenger League", "SABCDEF",
		"Soul", "Achiever", "Beast", "Competitor", "Dragon", "Elite", "Gnome child");

	/** Why the Challenger League is greyed out; set to null once its requirements are announced. */
	static final String LETTER_TIERS_LOCKED = "Not unlocked yet. Requirements to be announced soon";

	/** One rank slot in a tier: the title as charted, and whoever holds it (empty = vacant). */
	public static final class Slot
	{
		/** letter grade for the competitive tiers ("S", "A", ...), otherwise null */
		public final String grade;
		public final String title;
		public final List<ClanRoster.Member> members;

		Slot(String grade, String title, List<ClanRoster.Member> members)
		{
			this.grade = grade;
			this.title = title;
			this.members = Collections.unmodifiableList(members);
		}

		public int onlineCount()
		{
			return (int) members.stream().filter(m -> m.online).count();
		}
	}

	public static final class Tier
	{
		public final String name;
		public final List<Slot> slots;
		/** why nobody can reach this tier yet (show it greyed out), or null when it is open */
		public final String lockedNote;

		Tier(String name, List<Slot> slots, String lockedNote)
		{
			this.name = name;
			this.slots = Collections.unmodifiableList(slots);
			this.lockedNote = lockedNote;
		}
	}

	public static final class Chart
	{
		/** leadership tiers, top first */
		public final List<Tier> staff;
		/** the Gem League, then the (locked) letter tiers */
		public final List<Tier> competitive;
		/** rank groups that aren't in any tier, highest rank first */
		public final List<ClanRoster.RankGroup> others;

		Chart(List<Tier> staff, List<Tier> competitive, List<ClanRoster.RankGroup> others)
		{
			this.staff = Collections.unmodifiableList(staff);
			this.competitive = Collections.unmodifiableList(competitive);
			this.others = Collections.unmodifiableList(others);
		}
	}

	private OrgChart()
	{
	}

	/**
	 * @param groups     the roster from {@link ClanRoster#group}, highest rank first
	 * @param rankTitles every rank the clan has set up (rank number to title), including ranks
	 *                   nobody holds (their titles are shown as the game spells them)
	 */
	public static Chart build(List<ClanRoster.RankGroup> groups, Map<Integer, String> rankTitles)
	{
		// all known ranks: the clan's setup, plus any rank a member holds that the setup didn't list
		Map<Integer, String> ranks = new TreeMap<>(Collections.reverseOrder());
		ranks.putAll(rankTitles);
		for (ClanRoster.RankGroup g : groups)
		{
			ranks.putIfAbsent(g.rank, g.title);
		}

		Map<Integer, ClanRoster.RankGroup> byRank = new LinkedHashMap<>();
		for (ClanRoster.RankGroup g : groups)
		{
			byRank.put(g.rank, g);
		}

		Set<Integer> charted = new HashSet<>();
		List<Tier> staff = new ArrayList<>();
		for (TierDef def : STAFF_TIERS)
		{
			staff.add(titledTier(def, ranks, byRank, charted));
		}

		Tier gems = titledTier(GEM_LEAGUE, ranks, byRank, charted);
		Tier letters = titledTier(LETTER_TIERS, ranks, byRank, charted);
		List<Tier> competitive = Arrays.asList(gems,
			new Tier(letters.name, letters.slots, LETTER_TIERS_LOCKED));

		List<ClanRoster.RankGroup> others = new ArrayList<>();
		for (ClanRoster.RankGroup g : groups)
		{
			if (!charted.contains(g.rank))
			{
				others.add(g);
			}
		}
		return new Chart(staff, competitive, others);
	}

	/** A tier whose slots are fixed titles; each slot gathers every rank number carrying that title. */
	private static Tier titledTier(TierDef def, Map<Integer, String> ranks, Map<Integer, ClanRoster.RankGroup> byRank,
		Set<Integer> charted)
	{
		List<Slot> slots = new ArrayList<>();
		for (int i = 0; i < def.titles.size(); i++)
		{
			String title = def.titles.get(i);
			String grade = def.grades == null ? null : String.valueOf(def.grades.charAt(i));
			List<ClanRoster.Member> members = new ArrayList<>();
			String shownTitle = title;
			for (Map.Entry<Integer, String> rank : ranks.entrySet())
			{
				if (!charted.contains(rank.getKey()) && normalize(rank.getValue()).equals(normalize(title)))
				{
					shownTitle = rank.getValue();
					charted.add(rank.getKey());
					ClanRoster.RankGroup g = byRank.get(rank.getKey());
					if (g != null)
					{
						members.addAll(g.members);
					}
				}
			}
			slots.add(new Slot(grade, shownTitle, members));
		}
		return new Tier(def.name, slots, null);
	}

	/** Case, spacing and the common "Marshall" spelling don't matter when matching titles. */
	public static String normalize(String title)
	{
		String t = title == null ? "" : title.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
		return t.equals("marshall") ? "marshal" : t;
	}
}

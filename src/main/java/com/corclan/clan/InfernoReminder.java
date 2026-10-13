package com.corclan.clan;

import com.corclan.icons.MemberCosmetics;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Set;

/**
 * JoeBoob's daily Infernal cape reminder: every time he logs in he is told to go and get one, with a running count of
 * the days he has been reminded. Only he and uimfartqueef ever see it (on his client when he logs in, on Ray's when Ray
 * sees him log in). Each client keeps its own count, raised once per calendar day it reminds. Pure logic.
 */
public final class InfernoReminder
{
	/** Who gets reminded, as {@link MemberCosmetics#key} names. */
	public static final Set<String> TARGETS = Collections.singleton("joeboob");
	/** Who sees him being reminded, besides himself. */
	public static final String WATCHER = "uimfartqueef";

	static final String COLOR = "ff5a1f";

	private InfernoReminder()
	{
	}

	public static boolean isTarget(String name)
	{
		return name != null && TARGETS.contains(MemberCosmetics.key(name));
	}

	public static boolean isWatcher(String name)
	{
		return name != null && WATCHER.equals(MemberCosmetics.key(name));
	}

	/**
	 * The count after reminding today: one more than before on the first reminder of a day, the same again on later
	 * logins that day.
	 * @param stored the last {@link #store} value, or null the first time
	 */
	public static int countAfter(String stored, LocalDate today)
	{
		Day last = Day.parse(stored);
		if (last == null)
		{
			return 1;
		}
		return last.date.equals(today) ? last.count : last.count + 1;
	}

	/** What to keep after reminding today with {@code count}. */
	public static String store(LocalDate today, int count)
	{
		return today + "|" + count;
	}

	/** The line on JoeBoob's own client. */
	public static String targetLine(String iconTag, int day)
	{
		return (iconTag == null ? "" : iconTag + " ") + "<col=" + COLOR + ">Daily reminder #" + day
			+ ": go and get your Infernal cape.</col>";
	}

	/** The line on Ray's client when he sees JoeBoob log in. */
	public static String watcherLine(String iconTag, String name, int day)
	{
		return (iconTag == null ? "" : iconTag + " ") + "<col=" + COLOR + ">" + Legion.clean(name)
			+ " has been reminded to get his Infernal cape (day " + day + "). Still no cape.</col>";
	}

	private static final class Day
	{
		final LocalDate date;
		final int count;

		Day(LocalDate date, int count)
		{
			this.date = date;
			this.count = count;
		}

		static Day parse(String stored)
		{
			if (stored == null)
			{
				return null;
			}
			String[] parts = stored.split("\\|");
			try
			{
				int count = Integer.parseInt(parts[1]);
				return count < 1 ? null : new Day(LocalDate.parse(parts[0]), count);
			}
			catch (DateTimeParseException | NumberFormatException | ArrayIndexOutOfBoundsException e)
			{
				return null;
			}
		}
	}
}

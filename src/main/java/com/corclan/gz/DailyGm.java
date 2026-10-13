package com.corclan.gz;

import java.util.Locale;

/**
 * The clan's daily "gm": the first gm a member says in clan chat each day (UTC) earns a pack's worth of TCG points
 * from the clan server. Gm's are not counted or shown anywhere. Same rule as the server's isGm, so only a real gm is
 * ever sent. Pure logic.
 */
public final class DailyGm
{
	public static final int MAX_LENGTH = 80;
	private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

	private DailyGm()
	{
	}

	/** "gm", "GM!", "gmgm", "gmmm all", "good morning" ... but not "gmail" or "dogma". */
	public static boolean isGm(String message)
	{
		if (message == null)
		{
			return false;
		}
		String trimmed = message.trim();
		if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH)
		{
			return false;
		}
		String words = trimmed.toLowerCase(Locale.ROOT)
			.replaceAll("<[^>]*>", "")
			.replaceAll("[^a-z\\s]", " ")
			.trim()
			.replaceAll("\\s+", " ");
		if (words.isEmpty())
		{
			return false;
		}
		for (String word : words.split(" "))
		{
			if (word.matches("(gm)+m*"))
			{
				return true;
			}
		}
		return words.contains("good morning") || words.contains("goodmorning");
	}

	/** The UTC day {@code millis} falls in, as a day number; the gm bonus resets when it changes. */
	public static long utcDay(long millis)
	{
		return Math.floorDiv(millis, DAY_MILLIS);
	}
}

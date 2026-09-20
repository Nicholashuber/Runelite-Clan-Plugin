package com.corclan.gz;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Pure text logic that decides whether a clan chat line is a "gz" (congratulations).
 * No RuneLite dependencies so it can be unit tested directly.
 */
public final class GzDetector
{
	private static final Set<String> GZ_TOKENS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		"gz", "gzs",
		"grats", "gratz", "grtz", "gratulations", "gratulation",
		"congrats", "congratz", "congratulations", "congratulation"
	)));

	private GzDetector()
	{
	}

	/**
	 * @param message   raw chat message (may contain caps, punctuation, repeated letters)
	 * @param maxLength messages longer than this are never counted, so "gz" buried in a long
	 *                  unrelated sentence does not count
	 */
	public static boolean isGz(String message, int maxLength)
	{
		if (message == null)
		{
			return false;
		}
		String trimmed = message.trim();
		if (trimmed.isEmpty() || trimmed.length() > maxLength)
		{
			return false;
		}
		String normalized = normalize(trimmed);
		if (normalized.isEmpty())
		{
			return false;
		}
		for (String token : normalized.split(" "))
		{
			if (GZ_TOKENS.contains(token))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Lower-cases, drops everything except letters and spaces, and collapses runs of the same
	 * letter ("gzzzz" -> "gz", "graaats" -> "grats").
	 */
	static String normalize(String message)
	{
		String lower = message.toLowerCase(Locale.ROOT);
		StringBuilder sb = new StringBuilder(lower.length());
		char prev = 0;
		for (int i = 0; i < lower.length(); i++)
		{
			char c = lower.charAt(i);
			boolean letter = c >= 'a' && c <= 'z';
			if (!letter && c != ' ')
			{
				// punctuation / digits / emoji act as separators
				c = ' ';
			}
			if (c == prev && c != ' ')
			{
				continue;
			}
			if (c == ' ' && prev == ' ')
			{
				continue;
			}
			sb.append(c);
			prev = c;
		}
		return sb.toString().trim();
	}
}

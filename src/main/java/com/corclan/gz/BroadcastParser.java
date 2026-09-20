package com.corclan.gz;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure text logic that turns a clan system broadcast ("Zezima has received a drop: ...") into the
 * name of the member being congratulated, or {@code null} when the broadcast is not gz-worthy
 * (coffer transactions, invites, kicks, deaths ...).
 */
public final class BroadcastParser
{
	/** Broadcasts containing any of these are never gz-worthy. */
	private static final List<String> IGNORE_FRAGMENTS = Arrays.asList(
		"coffer",
		"has been invited",
		"has invited",
		"has left the clan",
		"has been kicked",
		"has been removed",
		"has been promoted",
		"has been demoted",
		"has died",
		"has been defeated",
		"lost their hardcore",
		"welcome to",
		"has logged",
		"is now"
	);

	/** "&lt;name&gt; has ..." / "&lt;name&gt; received ..." / "&lt;name&gt; feels ..." */
	private static final Pattern SUBJECT = Pattern.compile(
		"^(.+?)\\s+(?:has|have|received|feels|just|got)\\b",
		Pattern.CASE_INSENSITIVE);

	private BroadcastParser()
	{
	}

	/**
	 * @return the member the broadcast is about, or {@code null} if the broadcast should be ignored
	 */
	public static String subjectOf(String broadcast)
	{
		if (broadcast == null)
		{
			return null;
		}
		String text = clean(broadcast);
		if (text.isEmpty())
		{
			return null;
		}
		String lower = text.toLowerCase(Locale.ROOT);
		for (String fragment : IGNORE_FRAGMENTS)
		{
			if (lower.contains(fragment))
			{
				return null;
			}
		}
		Matcher m = SUBJECT.matcher(text);
		if (!m.find())
		{
			return null;
		}
		String subject = m.group(1).trim();
		// player names are 1-12 chars; anything longer is not a name
		if (subject.isEmpty() || subject.length() > 12)
		{
			return null;
		}
		return subject;
	}

	/** Strips tags, replaces non-breaking spaces / underscores and collapses whitespace. */
	public static String clean(String text)
	{
		return text
			.replaceAll("<[^>]*>", "")
			.replace(' ', ' ')
			.replace('_', ' ')
			.replaceAll("\\s+", " ")
			.trim();
	}
}

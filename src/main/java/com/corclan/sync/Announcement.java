package com.corclan.sync;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The announcement banner at the top of the side panel: events and such, set by the clan leaders through the clan
 * server. Cleaned the same way the server cleans it, so a bad answer can never put markup in the panel. Immutable.
 */
public final class Announcement
{
	public static final int MAX_LENGTH = 300;
	public static final int MAX_LINES = 6;

	/** one or more lines, no markup */
	public final String text;
	/** the leader who set it */
	public final String by;
	/** when it was set, epoch millis; 0 if the server's time was unreadable */
	public final long updatedAt;

	Announcement(String text, String by, long updatedAt)
	{
		this.text = text;
		this.by = by;
		this.updatedAt = updatedAt;
	}

	/** @return the banner, or null when there is none (or it is empty once cleaned) */
	public static Announcement of(SyncModels.AnnouncementData data)
	{
		if (data == null)
		{
			return null;
		}
		String text = clean(data.getText());
		if (text.isEmpty())
		{
			return null;
		}
		String by = clean(data.getBy()).replace('\n', ' ');
		return new Announcement(text, by, parse(data.getUpdatedAt()));
	}

	/**
	 * Tags and angle brackets removed, spaces tidied, blank lines dropped, at most {@link #MAX_LINES} lines and
	 * {@link #MAX_LENGTH} characters. The server does the same before storing; this is for what it sends back.
	 */
	public static String clean(String raw)
	{
		if (raw == null)
		{
			return "";
		}
		String stripped = raw.replaceAll("<[^>]*>", "").replace("<", "").replace(">", "");
		List<String> lines = new ArrayList<>();
		for (String line : stripped.split("\\r?\\n"))
		{
			String tidy = line.replaceAll("[\\s\\u00A0]+", " ").trim();
			if (!tidy.isEmpty() && lines.size() < MAX_LINES)
			{
				lines.add(tidy);
			}
		}
		String text = String.join("\n", lines);
		return text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH).trim() : text;
	}

	private static long parse(String iso)
	{
		if (iso == null)
		{
			return 0L;
		}
		try
		{
			return Instant.parse(iso).toEpochMilli();
		}
		catch (DateTimeParseException | ArithmeticException e)
		{
			return 0L;
		}
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Announcement))
		{
			return false;
		}
		Announcement other = (Announcement) o;
		return updatedAt == other.updatedAt && text.equals(other.text) && by.equals(other.by);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(text, by, updatedAt);
	}
}

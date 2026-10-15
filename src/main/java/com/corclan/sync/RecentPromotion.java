package com.corclan.sync;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One of the clan's latest promotions from the clan feed: a plugin user's own client reported a higher rank. Shown to
 * everyone in the Promotions card. Names and titles are cleaned, since they come from the server.
 */
public final class RecentPromotion
{
	/** the feed lists this many; anything more is ignored */
	public static final int MAX = 3;
	private static final int MAX_TEXT = 20;

	public final String name;
	public final String from;
	public final String to;
	/** epoch millis, 0 when unreadable */
	public final long at;

	RecentPromotion(String name, String from, String to, long at)
	{
		this.name = name;
		this.from = from;
		this.to = to;
		this.at = at;
	}

	/** @return the readable entries, newest first as the server sent them, at most {@link #MAX} */
	public static List<RecentPromotion> list(List<SyncModels.PromotionData> data)
	{
		List<RecentPromotion> list = new ArrayList<>();
		for (SyncModels.PromotionData p : data)
		{
			if (p == null || list.size() >= MAX)
			{
				continue;
			}
			String name = clean(p.getRsn());
			String from = clean(p.getFrom());
			String to = clean(p.getTo());
			if (!name.isEmpty() && !from.isEmpty() && !to.isEmpty())
			{
				list.add(new RecentPromotion(name, from, to, parse(p.getAt())));
			}
		}
		return Collections.unmodifiableList(list);
	}

	static String clean(String raw)
	{
		if (raw == null)
		{
			return "";
		}
		String text = raw.replaceAll("<[^>]*>", "").replace("<", "").replace(">", "")
			.replaceAll("[\\s\\u00A0]+", " ").trim();
		return text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT).trim() : text;
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
		if (!(o instanceof RecentPromotion))
		{
			return false;
		}
		RecentPromotion p = (RecentPromotion) o;
		return at == p.at && name.equals(p.name) && from.equals(p.from) && to.equals(p.to);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, from, to, at);
	}
}

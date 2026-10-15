package com.corclan.raid;

import com.corclan.icons.MemberCosmetics;
import com.corclan.sync.SyncModels;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** One open group on the raid finder board, as the clan server sent it, cleaned. */
public final class RaidListing
{
	/** the panel never shows more than this */
	static final int MAX_LISTED = 50;
	private static final int MAX_NAME = 12;

	public final String name;
	public final Raid raid;
	public final int filled;
	public final int size;
	/** 0 when the host did not say */
	public final int world;
	public final String note;
	/** epoch millis the group went up (kept when the host updates it), 0 when unreadable */
	public final long postedAt;
	/** epoch millis the host last sent it (every few minutes while logged in), 0 when unreadable */
	public final long updatedAt;

	/** the host is inside the raid with their party; the group stays listed */
	public final boolean raiding;

	/** a host who hasn't sent their group for this long has logged out (their plugin resends every 5 minutes) */
	public static final long AWAY_MILLIS = 7 * 60_000L;

	RaidListing(String name, Raid raid, int filled, int size, int world, String note, long postedAt, long updatedAt,
		boolean raiding)
	{
		this.raiding = raiding;
		this.updatedAt = updatedAt;
		this.name = name;
		this.raid = raid;
		this.filled = filled;
		this.size = size;
		this.world = world;
		this.note = note;
		this.postedAt = postedAt;
	}

	/** The readable listings, in the server's order (newest first). */
	public static List<RaidListing> board(SyncModels.RaidsResponse response)
	{
		List<RaidListing> board = new ArrayList<>();
		for (SyncModels.RaidListingData d : response.getRaids())
		{
			if (d == null || board.size() >= MAX_LISTED)
			{
				continue;
			}
			Raid raid = Raid.of(d.getRaid());
			String name = clean(d.getRsn(), MAX_NAME);
			if (raid == null || name.isEmpty() || d.getSize() < 2 || d.getSize() > raid.maxSize
				|| d.getFilled() < 1 || d.getFilled() > d.getSize())
			{
				continue;
			}
			int world = d.getWorld() == null || d.getWorld() < 1 ? 0 : d.getWorld();
			board.add(new RaidListing(name, raid, d.getFilled(), d.getSize(), world, clean(d.getNote(), RaidAd.MAX_NOTE),
				parse(d.getPostedAt()), parse(d.getUpdatedAt()), d.isRaiding()));
		}
		return Collections.unmodifiableList(board);
	}

	/** Same host, same raid, same posting: a resend or an update of a group already seen. */
	public String key()
	{
		return MemberCosmetics.key(name) + "|" + raid.id + "|" + postedAt;
	}

	public boolean isHost(String player)
	{
		return player != null && MemberCosmetics.key(player).equals(MemberCosmetics.key(name));
	}

	/** The host hasn't sent their group for a while: logged out (it stays up for half an hour). */
	public boolean isAway(long now)
	{
		return updatedAt > 0 && now - updatedAt > AWAY_MILLIS;
	}

	/** "3/5" */
	public String slots()
	{
		return filled + "/" + size;
	}

	/** No tags or angle brackets (it goes into chat lines and HTML labels), one line, at most {@code max} characters. */
	static String clean(String raw, int max)
	{
		if (raw == null)
		{
			return "";
		}
		String text = raw.replaceAll("<[^>]*>", "").replace("<", "").replace(">", "")
			.replaceAll("[\\s\\u00A0]+", " ").trim();
		return text.length() > max ? text.substring(0, max).trim() : text;
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
		if (!(o instanceof RaidListing))
		{
			return false;
		}
		RaidListing l = (RaidListing) o;
		return raid == l.raid && filled == l.filled && size == l.size && world == l.world && postedAt == l.postedAt
			&& updatedAt == l.updatedAt && raiding == l.raiding && name.equals(l.name) && note.equals(l.note);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, raid, filled, size, world, note, postedAt, updatedAt, raiding);
	}
}

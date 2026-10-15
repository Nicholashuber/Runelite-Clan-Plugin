package com.corclan.raid;

import java.util.Objects;

/** Our own raid listing, as the player filled it in. Sizes are kept in range and the note cleaned. */
public final class RaidAd
{
	public static final int MAX_NOTE = 60;

	public final Raid raid;
	/** party members so far, us included */
	public final int filled;
	public final int size;
	public final String note;

	public RaidAd(Raid raid, int filled, int size, String note)
	{
		this.raid = raid;
		this.size = Math.max(2, Math.min(raid.maxSize, size));
		this.filled = Math.max(1, Math.min(this.size, filled));
		this.note = RaidListing.clean(note, MAX_NOTE);
	}

	/** "3/5" */
	public String slots()
	{
		return filled + "/" + size;
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof RaidAd))
		{
			return false;
		}
		RaidAd a = (RaidAd) o;
		return raid == a.raid && filled == a.filled && size == a.size && note.equals(a.note);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(raid, filled, size, note);
	}
}

package com.corclan.raid;

import java.util.List;

/** The raid finder's clan chat lines (shown on each plugin user's own client). Plain text; the plugin colours it. */
public final class RaidLines
{
	/** the advert names this many groups; the rest are counted */
	static final int ADVERT_GROUPS = 3;

	private RaidLines()
	{
	}

	/** "CoR raid finder: Bob is looking for ToB (3/5) on W330: need tank" */
	public static String newGroup(RaidListing l)
	{
		return "CoR raid finder: " + l.name + " is looking for " + l.raid.shortName + " (" + l.slots() + ")"
			+ (l.world > 0 ? " on W" + l.world : "") + (l.note.isEmpty() ? "" : ": " + l.note);
	}

	/** "CoR raid finder: Bob's ToB party is raiding now (4/5)" */
	public static String raiding(RaidListing l)
	{
		return "CoR raid finder: " + l.name + "'s " + l.raid.shortName + " party is raiding now (" + l.slots() + ")";
	}

	/** "CoR raid finder: ToB 3/5 (Bob, W330), CoX 4/5 raiding (Al) and 1 more. See the CoR side panel." */
	public static String advert(List<RaidListing> board)
	{
		StringBuilder text = new StringBuilder("CoR raid finder: ");
		int listed = Math.min(ADVERT_GROUPS, board.size());
		for (int i = 0; i < listed; i++)
		{
			RaidListing l = board.get(i);
			text.append(i > 0 ? ", " : "").append(l.raid.shortName).append(' ').append(l.slots())
				.append(l.raiding ? " raiding" : "").append(l.room.isEmpty() ? "" : " at " + l.room).append(" (").append(l.name).append(l.world > 0 ? ", W" + l.world : "").append(')');
		}
		if (board.size() > listed)
		{
			text.append(" and ").append(board.size() - listed).append(" more");
		}
		return text.append(". See the CoR side panel.").toString();
	}
}

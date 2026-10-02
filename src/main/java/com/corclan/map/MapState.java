package com.corclan.map;

/**
 * What the clan map is doing for this player, shown in the panel and announced in chat when it changes,
 * so it is obvious whether sharing works even when nobody else is on the map. Pure logic, unit tested.
 */
public enum MapState
{
	/** "Share my location" is off */
	OFF("Clan map: off (turn on 'Share my location')", null),
	/** sharing is on but you are not in the CoR party (or are in another party) */
	NEED_PARTY("Clan map: press Join CoR party in the CoR panel to share",
		"CoR: press Join CoR party in the CoR side panel to share your location on the clan map."),
	/** in the CoR party but not connected yet */
	CONNECTING("Clan map: connecting to the CoR party", null),
	PAUSED_WILDERNESS("Clan map: paused in the Wilderness",
		"CoR: location sharing paused in the Wilderness. Turn on 'Share in Wilderness' to keep sharing."),
	PAUSED_INSTANCE("Clan map: paused inside an instance",
		"CoR: location sharing paused inside this instance."),
	SHARING("Clan map: sharing",
		"CoR: sharing your location with the CoR party. Open the world map to see yourself (the rhino marked You).");

	/** panel line; {@link #SHARING} adds the clanmate count */
	public final String status;
	/** local game message when this state starts, or null for none */
	public final String announcement;

	MapState(String status, String announcement)
	{
		this.status = status;
		this.announcement = announcement;
	}

	public static MapState of(boolean shareLocation, boolean inCorParty, boolean partyConnected, LocationRules.Decision decision)
	{
		if (!shareLocation)
		{
			return OFF;
		}
		if (!inCorParty)
		{
			return NEED_PARTY;
		}
		if (!partyConnected)
		{
			return CONNECTING;
		}
		switch (decision)
		{
			case HIDE_WILDERNESS:
				return PAUSED_WILDERNESS;
			case HIDE_INSTANCE:
				return PAUSED_INSTANCE;
			default:
				return SHARING;
		}
	}

	/** Panel line, e.g. "Clan map: sharing, 2 clanmates on the map". */
	public String status(int clanmates)
	{
		if (this != SHARING)
		{
			return status;
		}
		return status + ", " + clanmates + (clanmates == 1 ? " clanmate" : " clanmates") + " on the map";
	}
}

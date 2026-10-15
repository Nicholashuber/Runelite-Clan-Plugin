package com.corclan.raid;

import java.awt.Color;

/** The raids the raid finder knows, with the largest party each allows. The ids go over the wire: never rename one. */
public enum Raid
{
	COX("cox", "CoX", "Chambers of Xeric", 100, 5, new Color(70, 170, 110)),
	TOB("tob", "ToB", "Theatre of Blood", 5, 5, new Color(200, 40, 45)),
	TOA("toa", "ToA", "Tombs of Amascut", 8, 4, new Color(225, 175, 55)),
	/**
	 * The upcoming raid. Full name and party size are placeholders until it is released. Nothing detects being inside
	 * it, so a TFA group never shows as raiding: the host takes it down by hand.
	 */
	TFA("tfa", "TFA", "TFA", 8, 4, new Color(150, 90, 220));

	/** sent to the clan server */
	public final String id;
	public final String shortName;
	public final String fullName;
	public final int maxSize;
	/** what the size box starts at */
	public final int defaultSize;
	/** the tag colour in the side panel */
	public final Color color;

	Raid(String id, String shortName, String fullName, int maxSize, int defaultSize, Color color)
	{
		this.id = id;
		this.shortName = shortName;
		this.fullName = fullName;
		this.maxSize = maxSize;
		this.defaultSize = defaultSize;
		this.color = color;
	}

	/** @return the raid with this id, or null */
	public static Raid of(String id)
	{
		for (Raid raid : values())
		{
			if (raid.id.equals(id))
			{
				return raid;
			}
		}
		return null;
	}

	/** what the raid box shows */
	@Override
	public String toString()
	{
		return shortName;
	}
}

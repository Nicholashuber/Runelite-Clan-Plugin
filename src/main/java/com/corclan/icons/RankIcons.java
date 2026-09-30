package com.corclan.icons;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Which chat icon a clan rank gets. Clan ranks are ints: guests are -1, member ranks count up from 0,
 * administrators are 100+, deputy owner 125, owner 126, J-Mods 127.
 *
 * The clan server can pick an icon per rank on its admin page; ranks it hasn't picked use the default rule.
 */
public final class RankIcons
{
	/** Server value meaning "show no CoR icon for this rank". */
	public static final String NO_ICON = "none";
	static final int ADMIN_RANK = 100;
	static final int JMOD_RANK = 127;

	private RankIcons()
	{
	}

	/** The original rule: administrators and up get the red rhino, everyone else the blue one, J-Mods nothing. */
	public static String defaultIcon(int rank)
	{
		if (rank == JMOD_RANK)
		{
			return null;
		}
		return rank >= ADMIN_RANK ? ClanIconService.KEY_STAFF : ClanIconService.KEY_MEMBER;
	}

	/**
	 * @param overrides rank -> icon key picked on the clan server ({@link #NO_ICON} for none)
	 * @param isKnownIcon whether this client has an icon with that key
	 * @return the icon key to draw, or null for no icon
	 */
	public static String resolve(int rank, Map<Integer, String> overrides, Predicate<String> isKnownIcon)
	{
		String picked = overrides.get(rank);
		if (picked == null)
		{
			return defaultIcon(rank);
		}
		if (NO_ICON.equals(picked))
		{
			return null;
		}
		// an icon this client doesn't have (e.g. not downloaded yet) falls back to the default rule
		return isKnownIcon.test(picked) ? picked : defaultIcon(rank);
	}
}

package com.corclan.clan;

import com.corclan.icons.MemberCosmetics;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The CoR leaders: they may ::shame a clanmate and edit the announcement banner. The clan server checks the same
 * names (its LEADERS setting) and that the request really comes from that player's account, so this list only
 * decides what the plugin offers; change both together.
 */
public final class ClanLeaders
{
	/** As {@link MemberCosmetics#key} names. DAYLlGHT is spelt with a lowercase L, not an I. */
	public static final Set<String> LEADERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		"uimfartqueef",
		"joeboob",
		"lavasockz",
		"dayllght")));

	private ClanLeaders()
	{
	}

	public static boolean isLeader(String name)
	{
		return name != null && LEADERS.contains(MemberCosmetics.key(name));
	}
}

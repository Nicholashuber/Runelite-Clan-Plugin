package com.corclan.clan;

import com.corclan.icons.MemberCosmetics;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Login alerts for the CoR leaders: when one of the legionnaires ({@link ClanLeaders}) logs in, every plugin user in
 * the clan gets a line in chat; when the Spartan (Ray) lands, a golden one with a little extra. World hops are not
 * logins: in the clan channel a hop is the player leaving and joining again a few seconds later, so a join soon after
 * that player left is ignored. Pure logic; the plugin calls it on the client thread.
 */
public final class Legion
{
	/** The one leader whose login is a landing, as a {@link MemberCosmetics#key} name. */
	public static final String SPARTAN = "uimfartqueef";
	/** TESTING ONLY: Runny Sharts lands like the Spartan so Ray can try it on that account; remove before merging. */
	static final Set<String> TEST_SPARTANS = Collections.singleton("runny sharts");
	/** A join this soon after the same player left the channel is a world hop (or a quick relog), not a login. */
	static final long HOP_MILLIS = 60_000L;

	static final String LEGION_COLOR = "e0603a";
	static final String SPARTAN_COLOR = "ffc832";

	/** leader key -> when they last left the clan channel */
	private final Map<String, Long> leftAt = new HashMap<>();

	public static boolean isLegionnaire(String name)
	{
		return ClanLeaders.isLeader(name) || isSpartan(name);
	}

	public static boolean isSpartan(String name)
	{
		return name != null && (SPARTAN.equals(MemberCosmetics.key(name)) || TEST_SPARTANS.contains(MemberCosmetics.key(name)));
	}

	/** A leader left the clan channel (logged out, or the first half of a world hop). */
	public void left(String name, long now)
	{
		if (isLegionnaire(name))
		{
			leftAt.put(MemberCosmetics.key(name), now);
		}
	}

	/** True when a leader joining the clan channel now has just logged in, not hopped worlds. */
	public boolean isLogin(String name, long now)
	{
		if (!isLegionnaire(name))
		{
			return false;
		}
		Long left = leftAt.remove(MemberCosmetics.key(name));
		return left == null || now - left > HOP_MILLIS;
	}

	public void clear()
	{
		leftAt.clear();
	}

	/**
	 * The chat line for a leader's login.
	 * @param iconTag an {@code <img=N>} tag to decorate it with, or null
	 */
	public static String line(String name, String iconTag)
	{
		String icon = iconTag == null ? "" : iconTag + " ";
		if (isSpartan(name))
		{
			return icon + "<col=" + SPARTAN_COLOR + ">A Spartan has landed!</col>" + (iconTag == null ? "" : " " + iconTag);
		}
		return icon + "<col=" + LEGION_COLOR + ">One of the legionnaires has logged in: " + clean(name) + "</col>";
	}

	/** Names come from the game, but they go into chat markup, so no tag can ever get through. */
	static String clean(String name)
	{
		return name == null ? "" : name.replace("<", "").replace(">", "").replace(' ', ' ').trim();
	}
}

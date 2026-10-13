package com.corclan.fun;

import net.runelite.api.gameval.SpotanimID;

/**
 * Bosses saying things over their heads, on this client only. The Maiden of Sugadinti (Theatre of Blood) shouts
 * "She's tensings!" when she throws the blood that leaves pools on the floor. One throw sends several blood
 * projectiles at once, so she says it once per throw. Pure logic; the plugin calls it on the client thread.
 */
public final class BossBanter
{
	public static final String MAIDEN_LINE = "She's tensings!";
	/** The Maiden's blood throw; each one lands as a pool of lingering blood. */
	static final int MAIDEN_BLOOD = SpotanimID.MAIDEN_BLOOD_PROJ;
	/** How long the line stays over her head, in client cycles (20 ms): 3 seconds. */
	public static final int SHOW_CYCLES = 150;
	/** Projectiles this soon after the last line belong to the same throw. */
	static final int SAME_THROW_CYCLES = 150;

	private int lastSaidAt;
	private boolean said;

	/** True when this projectile starts a new Maiden blood throw: she says her line now. */
	public boolean saysLine(int projectileId, int gameCycle)
	{
		if (projectileId != MAIDEN_BLOOD || (said && gameCycle - lastSaidAt < SAME_THROW_CYCLES))
		{
			return false;
		}
		said = true;
		lastSaidAt = gameCycle;
		return true;
	}

	/** True for the Maiden herself, at any health stage or difficulty, by her in-game name. */
	public static boolean isMaiden(String npcName)
	{
		return npcName != null && npcName.contains("Maiden of Sugadinti");
	}
}

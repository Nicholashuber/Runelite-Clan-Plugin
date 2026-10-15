package com.corclan.fun;

import net.runelite.api.SoundEffectVolume;

/**
 * Sound bites for raids, played on this client only. In the Chambers of Xeric, every death of a player in view plays
 * a chicken's death squawk for each CoR plugin user in the raid (each client plays it for itself; nothing is sent).
 * The game's own sound effects, so nothing is bundled in the plugin.
 */
public final class RaidSounds
{
	/** The game's "chicken_death" sound effect (OSRS Wiki, List of in-game sound IDs). */
	public static final int CHICKEN_DEATH = 356;

	private RaidSounds()
	{
	}

	/**
	 * The game's sound effect volume for a volume setting.
	 * @param volume 0 (silent) to 100 (as loud as the game plays sound effects)
	 */
	public static int volume(int volume)
	{
		int clamped = Math.max(0, Math.min(volume, 100));
		return Math.round(clamped * SoundEffectVolume.HIGH / 100f);
	}
}

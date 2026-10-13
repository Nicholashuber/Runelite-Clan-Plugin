package com.corclan.fun;

/**
 * Sound bites for raids, played on this client only. In the Chambers of Xeric, every death of a player in view
 * plays "Olm Sacrifice" for each CoR plugin user in the raid (each client plays it for itself; nothing is sent).
 */
public final class RaidSounds
{
	/** Next to {@code CorClanPlugin} in the resources: 22 kHz mono 16-bit PCM, which Java plays without extra libraries. */
	public static final String OLM_SACRIFICE = "olm_sacrifice.wav";
	/** Quieter than this is silence. */
	static final float MIN_GAIN_DB = -80f;

	private RaidSounds()
	{
	}

	/**
	 * The gain to play at, in decibels, for a volume setting.
	 * @param volume 0 (silent) to 100 (the clip's own loudness)
	 */
	public static float gainDb(int volume)
	{
		if (volume <= 0)
		{
			return MIN_GAIN_DB;
		}
		return (float) Math.max(MIN_GAIN_DB, 20 * Math.log10(Math.min(volume, 100) / 100.0));
	}
}

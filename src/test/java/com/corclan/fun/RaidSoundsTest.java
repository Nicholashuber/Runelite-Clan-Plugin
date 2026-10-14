package com.corclan.fun;

import static org.junit.Assert.assertEquals;
import net.runelite.api.SoundEffectVolume;
import org.junit.Test;

public class RaidSoundsTest
{
	@Test
	public void volumeMapsToTheGamesSoundEffectVolume()
	{
		assertEquals(SoundEffectVolume.HIGH, RaidSounds.volume(100));
		assertEquals(64, RaidSounds.volume(50));
		assertEquals(SoundEffectVolume.MUTED, RaidSounds.volume(0));
		assertEquals(SoundEffectVolume.MUTED, RaidSounds.volume(-5));
		assertEquals(SoundEffectVolume.HIGH, RaidSounds.volume(250));
	}
}

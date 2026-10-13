package com.corclan.fun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.corclan.CorClanPlugin;
import java.io.BufferedInputStream;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.junit.Test;

public class RaidSoundsTest
{
	@Test
	public void theClipIsAWavJavaCanPlay() throws Exception
	{
		InputStream raw = CorClanPlugin.class.getResourceAsStream(RaidSounds.OLM_SACRIFICE);
		assertNotNull("bundled next to CorClanPlugin", raw);
		try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(raw)))
		{
			AudioFormat format = in.getFormat();
			assertEquals(AudioFormat.Encoding.PCM_SIGNED, format.getEncoding());
			double seconds = in.getFrameLength() / format.getFrameRate();
			assertTrue("a short sound bite, was " + seconds + " s", seconds > 0.5 && seconds < 10);
		}
	}

	@Test
	public void volumeMapsToGain()
	{
		assertEquals(0f, RaidSounds.gainDb(100), 0.001f);
		assertEquals(-6.02f, RaidSounds.gainDb(50), 0.01f);
		assertEquals(RaidSounds.MIN_GAIN_DB, RaidSounds.gainDb(0), 0f);
		assertEquals(0f, RaidSounds.gainDb(250), 0.001f);
	}
}

package com.corclan.glow;

import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;

/** Glow ids go to the clan server in POST /v1/profile, which takes at most 32 ids of a-z, 0-9 and _ (32 long). */
public class GlowIdsTest
{
	private static List<String> allIds()
	{
		List<String> ids = new ArrayList<>();
		for (GlowEffect glow : GlowEffect.values())
		{
			ids.add(glow.id);
		}
		for (GemGlow glow : GemGlow.values())
		{
			ids.add(glow.id);
		}
		for (GemGlow.Part part : GemGlow.Part.values())
		{
			ids.add(part.id);
		}
		for (DevGlow glow : DevGlow.values())
		{
			ids.add(glow.id);
		}
		for (FounderGlow glow : FounderGlow.values())
		{
			ids.add(glow.id);
		}
		return ids;
	}

	@Test
	public void everyIdIsOneTheServerAccepts()
	{
		for (String id : allIds())
		{
			assertTrue(id, id.matches("[a-z0-9_]{1,32}"));
		}
	}

	@Test
	public void aPlayerWithEverythingOnStillFitsInOneProfile()
	{
		List<String> ids = allIds();
		assertTrue(ids.size() <= 32);
		assertTrue("ids must be unique", new HashSet<>(ids).size() == ids.size());
	}
}

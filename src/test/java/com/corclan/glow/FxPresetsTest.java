package com.corclan.glow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.gameval.SpotanimID;
import org.junit.Test;

public class FxPresetsTest
{
	private static List<String> typed(String... words)
	{
		return Arrays.asList(words);
	}

	@Test
	public void presetsArePickedByNameInAnyCase()
	{
		FxPresets.Pick pick = FxPresets.parse(typed("beef", "TORNADO"), false);
		assertEquals(3587, pick.id);
		assertEquals("Beef tornado", pick.label);
		assertNull(pick.extra);
		assertEquals(SpotanimID.FAIRY_FLOWER_RING, FxPresets.parse(typed("fairy", "ring"), false).id);
	}

	@Test
	public void theStartOfANameIsEnoughWhenOnlyOneMatches()
	{
		assertEquals(SpotanimID.LUC2_LUCIEN_LIGHTNING_SPOT, FxPresets.parse(typed("lucien"), false).id);
		// "tormented" starts several names
		assertNull(FxPresets.parse(typed("tormented"), false));
	}

	@Test
	public void aNumberAfterTheNameIsTheRiseOrHeight()
	{
		FxPresets.Pick pick = FxPresets.parse(typed("arceuus", "250"), false);
		assertEquals(1296, pick.id);
		assertEquals(Integer.valueOf(250), pick.extra);
	}

	@Test
	public void rawIdsOnlyInDeveloperMode()
	{
		assertNull(FxPresets.parse(typed("1296", "250"), false));
		assertTrue(FxPresets.typedAnId(typed("1296")));
		assertFalse(FxPresets.typedAnId(typed("beef")));

		FxPresets.Pick dev = FxPresets.parse(typed("1296", "250"), true);
		assertEquals(1296, dev.id);
		assertEquals(Integer.valueOf(250), dev.extra);
		assertEquals("1296", dev.label);
	}

	@Test
	public void nothingOrNonsenseMatchesNothing()
	{
		assertNull(FxPresets.parse(Collections.emptyList(), true));
		assertNull(FxPresets.parse(typed("nope"), true));
	}

	@Test
	public void everyCatalogGraphicIsAPreset()
	{
		for (TintedFx.Def def : TintedFx.CATALOG.values())
		{
			assertTrue(def.name, FxPresets.ALL.containsValue(def.spotId));
		}
	}
}

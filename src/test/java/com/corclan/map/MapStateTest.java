package com.corclan.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class MapStateTest
{
	private static final LocationRules.Decision SEND = LocationRules.Decision.SEND;

	@Test
	public void explainsWhatIsMissing()
	{
		assertEquals(MapState.OFF, MapState.of(false, true, true, SEND));
		assertEquals(MapState.NEED_PARTY, MapState.of(true, false, false, SEND));
		assertEquals(MapState.CONNECTING, MapState.of(true, true, false, SEND));
		assertEquals(MapState.SHARING, MapState.of(true, true, true, SEND));
	}

	@Test
	public void pausesAreNamed()
	{
		assertEquals(MapState.PAUSED_WILDERNESS, MapState.of(true, true, true, LocationRules.Decision.HIDE_WILDERNESS));
		assertEquals(MapState.PAUSED_INSTANCE, MapState.of(true, true, true, LocationRules.Decision.HIDE_INSTANCE));
	}

	@Test
	public void sharingStatusCountsClanmatesAndTellsYouToLook()
	{
		assertEquals("Clan map: sharing, 0 clanmates on the map", MapState.SHARING.status(0));
		assertEquals("Clan map: sharing, 1 clanmate on the map", MapState.SHARING.status(1));
		assertNotNull(MapState.SHARING.announcement);
		assertNull(MapState.OFF.announcement);
	}
}

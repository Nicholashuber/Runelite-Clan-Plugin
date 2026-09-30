package com.corclan.map;

import static org.junit.Assert.assertEquals;
import com.corclan.map.LocationRules.Decision;
import org.junit.Test;

public class LocationRulesTest
{
	@Test
	public void sharesOutsideTheWilderness()
	{
		assertEquals(Decision.SEND, LocationRules.decide(false, false, false));
		assertEquals(Decision.SEND, LocationRules.decide(false, false, true));
	}

	@Test
	public void wildernessNeedsTheSecondToggle()
	{
		assertEquals(Decision.HIDE_WILDERNESS, LocationRules.decide(true, false, false));
		assertEquals(Decision.SEND, LocationRules.decide(true, false, true));
	}

	@Test
	public void instancesAreNeverShared()
	{
		assertEquals(Decision.HIDE_INSTANCE, LocationRules.decide(false, true, true));
		assertEquals(Decision.HIDE_INSTANCE, LocationRules.decide(true, true, true));
	}
}

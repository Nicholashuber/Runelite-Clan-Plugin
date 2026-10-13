package com.corclan.fun;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.api.gameval.SpotanimID;
import org.junit.Test;

public class BossBanterTest
{
	@Test
	public void onceForEachBloodThrow()
	{
		BossBanter banter = new BossBanter();
		assertTrue(banter.saysLine(SpotanimID.MAIDEN_BLOOD_PROJ, 1000));
		// the rest of the same throw's projectiles
		assertFalse(banter.saysLine(SpotanimID.MAIDEN_BLOOD_PROJ, 1000));
		assertFalse(banter.saysLine(SpotanimID.MAIDEN_BLOOD_PROJ, 1020));
		// her next throw
		assertTrue(banter.saysLine(SpotanimID.MAIDEN_BLOOD_PROJ, 1000 + BossBanter.SAME_THROW_CYCLES));
	}

	@Test
	public void otherProjectilesSayNothing()
	{
		BossBanter banter = new BossBanter();
		assertFalse(banter.saysLine(SpotanimID.MAIDEN_SHADOW_PROJ, 1000));
		assertTrue(banter.saysLine(SpotanimID.MAIDEN_BLOOD_PROJ, 1000));
	}

	@Test
	public void theFirstThrowCountsEvenAtCycleZero()
	{
		assertTrue(new BossBanter().saysLine(SpotanimID.MAIDEN_BLOOD_PROJ, 0));
	}

	@Test
	public void knowsTheMaidenByName()
	{
		assertTrue(BossBanter.isMaiden("The Maiden of Sugadinti"));
		assertFalse(BossBanter.isMaiden("Nylocas Matomenos"));
		assertFalse(BossBanter.isMaiden(null));
	}
}

package com.corclan.clan;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class LegionTest
{
	@Test
	public void theFourLeadersAreTheLegionAndRayIsTheSpartan()
	{
		for (String name : new String[]{"UimFartQueef", "JoeBoob", "Lavasockz", "DAYLlGHT"})
		{
			assertTrue(name, Legion.isLegionnaire(name));
		}
		assertFalse(Legion.isLegionnaire("Zezima"));
		assertTrue(Legion.isSpartan("uimfartqueef"));
		assertFalse(Legion.isSpartan("JoeBoob"));
	}

	@Test
	public void aFirstJoinIsALogin()
	{
		assertTrue(new Legion().isLogin("JoeBoob", 1_000L));
	}

	@Test
	public void aQuickRejoinIsAWorldHop()
	{
		Legion legion = new Legion();
		legion.left("JoeBoob", 10_000L);
		assertFalse(legion.isLogin("JoeBoob", 18_000L));
		// the hop is used up: the next join without leaving again is not mistaken for another hop
		assertTrue(legion.isLogin("JoeBoob", 19_000L));
	}

	@Test
	public void rejoiningLongAfterLeavingIsALogin()
	{
		Legion legion = new Legion();
		legion.left("Lavasockz", 0L);
		assertTrue(legion.isLogin("Lavasockz", Legion.HOP_MILLIS + 1));
	}

	@Test
	public void othersAreNeverAnnounced()
	{
		Legion legion = new Legion();
		legion.left("Zezima", 0L);
		assertFalse(legion.isLogin("Zezima", 999_999L));
	}

	@Test
	public void theSpartanGetsGoldAndIconsBothSides()
	{
		String line = Legion.line("UimFartQueef", "<img=7>");
		assertTrue(line.startsWith("<img=7> "));
		assertTrue(line.endsWith(" <img=7>"));
		assertTrue(line.contains("<col=" + Legion.SPARTAN_COLOR + ">A Spartan has landed!"));
	}

	@Test
	public void legionnairesAreNamedAndMarkupCannotSneakIn()
	{
		String line = Legion.line("Joe<b>Boob", null);
		assertTrue(line.contains("One of the legionnaires has logged in: JoebBoob"));
		assertFalse(line.contains("<b>"));
	}
}

package com.corclan.gz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class BroadcastParserTest
{
	@Test
	public void drops()
	{
		assertEquals("Zezima", BroadcastParser.subjectOf("Zezima has received a drop: Twisted bow (1,000,000,000 coins)."));
		assertEquals("Zezima", BroadcastParser.subjectOf("Zezima received a drop: Dragon claws (100,000,000 coins)"));
	}

	@Test
	public void levelsAndPets()
	{
		assertEquals("Zezima", BroadcastParser.subjectOf("Zezima has reached level 99 in Attack."));
		assertEquals("Zezima", BroadcastParser.subjectOf("Zezima has reached a total level of 2277."));
		assertEquals("Zezima", BroadcastParser.subjectOf("Zezima has a funny feeling like they're being followed: Vorki at 1,000 kill count."));
		assertEquals("Zezima", BroadcastParser.subjectOf("Zezima feels something weird sneaking into their backpack: Rocky at 100 pickpockets."));
	}

	@Test
	public void achievements()
	{
		assertEquals("Zezima", BroadcastParser.subjectOf("Zezima has completed a quest: Dragon Slayer II"));
		assertEquals("Zezima", BroadcastParser.subjectOf("Zezima has completed the Elite Lumbridge & Draynor diary."));
		assertEquals("Zezima", BroadcastParser.subjectOf("Zezima has achieved a new personal best: Theatre of Blood 15:23."));
		assertEquals("Zezima", BroadcastParser.subjectOf("Zezima received a new collection log item: Dragon pickaxe (123/1477)"));
	}

	@Test
	public void namesWithSpacesNbspAndTags()
	{
		assertEquals("Iron Nick", BroadcastParser.subjectOf("Iron Nick has reached level 99 in Slayer."));
		assertEquals("Iron Nick", BroadcastParser.subjectOf("<col=ff0000>Iron Nick</col> has reached level 99 in Slayer."));
		assertEquals("Iron Nick", BroadcastParser.subjectOf("Iron_Nick has reached level 99 in Slayer."));
	}

	@Test
	public void ignoredBroadcasts()
	{
		assertNull(BroadcastParser.subjectOf("Zezima has deposited 1,000,000 coins into the coffer."));
		assertNull(BroadcastParser.subjectOf("Zezima has withdrawn 1,000,000 coins from the coffer."));
		assertNull(BroadcastParser.subjectOf("Zezima has been invited into the clan by Nick."));
		assertNull(BroadcastParser.subjectOf("Zezima has left the clan."));
		assertNull(BroadcastParser.subjectOf("Zezima has been kicked from the clan by Nick."));
		assertNull(BroadcastParser.subjectOf("Zezima has died and lost their Hardcore Ironman status."));
		assertNull(BroadcastParser.subjectOf("Zezima has been defeated by Nick in The Wilderness."));
		assertNull(BroadcastParser.subjectOf(""));
		assertNull(BroadcastParser.subjectOf(null));
		assertNull(BroadcastParser.subjectOf("Welcome to The Core!"));
		assertNull(BroadcastParser.subjectOf("A very long sentence that does not start with a name has something"));
	}
}

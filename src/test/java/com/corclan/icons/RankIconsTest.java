package com.corclan.icons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.Test;

public class RankIconsTest
{
	private static final Predicate<String> KNOWN = k -> k.equals("staff") || k.equals("member") || k.equals("crown");

	@Test
	public void defaultRuleMatchesTheServer()
	{
		assertEquals("staff", RankIcons.defaultIcon(126));
		assertEquals("staff", RankIcons.defaultIcon(100));
		assertEquals("member", RankIcons.defaultIcon(99));
		assertEquals("member", RankIcons.defaultIcon(0));
		assertEquals("member", RankIcons.defaultIcon(-1));
		assertNull(RankIcons.defaultIcon(127));
	}

	@Test
	public void serverPicksWinOverTheDefault()
	{
		Map<Integer, String> picks = new HashMap<>();
		picks.put(5, "crown");
		picks.put(0, RankIcons.NO_ICON);
		assertEquals("crown", RankIcons.resolve(5, picks, KNOWN));
		assertNull(RankIcons.resolve(0, picks, KNOWN));
		assertEquals("staff", RankIcons.resolve(126, picks, KNOWN));
	}

	@Test
	public void unknownPickedIconFallsBackToTheDefault()
	{
		Map<Integer, String> picks = Collections.singletonMap(5, "not-downloaded-yet");
		assertEquals("member", RankIcons.resolve(5, picks, KNOWN));
	}
}

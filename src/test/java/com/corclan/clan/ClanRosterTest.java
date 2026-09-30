package com.corclan.clan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.corclan.clan.ClanRoster.Member;
import com.corclan.clan.ClanRoster.RankGroup;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class ClanRosterTest
{
	private static List<String> names(RankGroup g)
	{
		return g.members.stream().map(m -> m.name).collect(Collectors.toList());
	}

	@Test
	public void groupsByRankHighestFirst()
	{
		List<RankGroup> groups = ClanRoster.group(Arrays.asList(
			new Member("Recruit1", 0, "Recruit", false),
			new Member("Boss", 126, "Owner", true),
			new Member("Cap", 5, "Captain", false),
			new Member("Deputy", 125, "Deputy Owner", false),
			new Member("Recruit2", 0, "Recruit", true)));

		assertEquals(Arrays.asList(126, 125, 5, 0), groups.stream().map(g -> g.rank).collect(Collectors.toList()));
		assertEquals("Owner", groups.get(0).title);
		assertEquals("Captain", groups.get(2).title);
		assertEquals(5, ClanRoster.total(groups));
		assertEquals(2, ClanRoster.online(groups));
	}

	@Test
	public void onlineFirstThenNameIgnoringCase()
	{
		List<RankGroup> groups = ClanRoster.group(Arrays.asList(
			new Member("zed", 0, "Recruit", false),
			new Member("Bob", 0, "Recruit", true),
			new Member("alice", 0, "Recruit", false),
			new Member("Carl", 0, "Recruit", true)));
		assertEquals(Arrays.asList("Bob", "Carl", "alice", "zed"), names(groups.get(0)));
		assertEquals(2, groups.get(0).onlineCount());
	}

	@Test
	public void missingTitleFallsBackToRankNumber()
	{
		List<RankGroup> groups = ClanRoster.group(Collections.singletonList(new Member("Someone", 7, null, false)));
		assertEquals("Rank 7", groups.get(0).title);
	}

	@Test
	public void skipsBlankNamesAndHandlesEmpty()
	{
		assertTrue(ClanRoster.group(Collections.emptyList()).isEmpty());
		List<RankGroup> groups = ClanRoster.group(Arrays.asList(new Member("", 0, "Recruit", false), null,
			new Member("Real", 0, "Recruit", false)));
		assertEquals(1, ClanRoster.total(groups));
	}

	@Test
	public void sameMembersGiveEqualGroups()
	{
		List<Member> a = Arrays.asList(new Member("Bob", 0, "Recruit", true), new Member("Ann", 5, "Captain", false));
		List<Member> b = Arrays.asList(new Member("Ann", 5, "Captain", false), new Member("Bob", 0, "Recruit", true));
		assertEquals(ClanRoster.group(a), ClanRoster.group(b));
	}
}

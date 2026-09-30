package com.corclan.clan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.corclan.clan.ClanRoster.Member;
import com.corclan.clan.OrgChart.Chart;
import com.corclan.clan.OrgChart.Slot;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;

public class OrgChartTest
{
	/** A clan set up like CoR: staff on top, letter tiers, then the gem league. */
	private static Map<Integer, String> corRanks()
	{
		Map<Integer, String> r = new HashMap<>();
		r.put(126, "Owner");
		r.put(125, "Deputy Owner");
		r.put(100, "Administrator");
		r.put(95, "Marshal");
		r.put(90, "General");
		r.put(85, "Brigadier");
		r.put(80, "Colonel");
		r.put(75, "Major");
		r.put(70, "Goon");
		r.put(65, "Executive");
		r.put(60, "Short green guy");
		r.put(55, "Hero");
		r.put(50, "Soul");
		r.put(45, "Achiever");
		r.put(40, "Beast");
		r.put(35, "Competitor");
		r.put(30, "Dragon");
		r.put(25, "Elite");
		r.put(20, "Gnome child");
		r.put(17, "Zenyte");
		r.put(16, "Onyx");
		r.put(15, "Dragonstone");
		r.put(14, "Diamond");
		r.put(13, "Ruby");
		r.put(12, "Emerald");
		r.put(11, "Sapphire");
		r.put(10, "Opal");
		return r;
	}

	private static Chart chart(Map<Integer, String> ranks, Member... members)
	{
		return OrgChart.build(ClanRoster.group(Arrays.asList(members)), ranks);
	}

	private static List<String> titles(OrgChart.Tier tier)
	{
		return tier.slots.stream().map(s -> s.title).collect(Collectors.toList());
	}

	private static Slot slot(OrgChart.Tier tier, String title)
	{
		return tier.slots.stream().filter(s -> s.title.equalsIgnoreCase(title)).findFirst().orElseThrow(AssertionError::new);
	}

	@Test
	public void staffTiersInTheCorOrder()
	{
		Chart c = chart(corRanks());
		assertEquals(3, c.staff.size());
		assertEquals(Arrays.asList("Owner", "Deputy Owner"), titles(c.staff.get(0)));
		assertEquals(Arrays.asList("Marshal", "General", "Brigadier", "Colonel", "Administrator"), titles(c.staff.get(1)));
		assertEquals(Arrays.asList("Major", "Goon", "Executive", "Short green guy", "Hero"), titles(c.staff.get(2)));
	}

	@Test
	public void letterTiersAreTheCompetitiveBracketSToF()
	{
		OrgChart.Tier letters = chart(corRanks()).competitive.get(0);
		assertEquals("Competitive tiers", letters.name);
		assertEquals(Arrays.asList("Soul", "Achiever", "Beast", "Competitor", "Dragon", "Elite", "Gnome child"), titles(letters));
		assertEquals(Arrays.asList("S", "A", "B", "C", "D", "E", "F"),
			letters.slots.stream().map(s -> s.grade).collect(Collectors.toList()));
	}

	@Test
	public void gemLeagueIsTheBottomBoxHighestFirst()
	{
		Chart c = chart(corRanks());
		assertEquals(2, c.competitive.size());
		OrgChart.Tier gems = c.competitive.get(1);
		assertEquals("Gem league", gems.name);
		assertEquals(Arrays.asList("Zenyte", "Onyx", "Dragonstone", "Diamond", "Ruby", "Emerald", "Sapphire", "Opal"), titles(gems));
	}

	@Test
	public void membersLandInTheirSlotAndEmptyRanksStayVacant()
	{
		Chart c = chart(corRanks(),
			new Member("Boss", 126, "Owner", true),
			new Member("Ace", 45, "Achiever", true),
			new Member("Newbie", 10, "Opal", false),
			new Member("Newbie2", 10, "Opal", true));

		assertEquals(1, slot(c.staff.get(0), "Owner").members.size());
		assertEquals(1, slot(c.competitive.get(0), "Achiever").members.size());
		assertEquals(2, slot(c.competitive.get(1), "Opal").members.size());
		assertEquals(1, slot(c.competitive.get(1), "Opal").onlineCount());
		assertTrue(slot(c.competitive.get(0), "Soul").members.isEmpty());
		assertTrue(slot(c.staff.get(1), "General").members.isEmpty());
		assertTrue(c.others.isEmpty());
	}

	@Test
	public void allSevenLetterTiersShowBeforeTheyExistInGame()
	{
		// a clan that hasn't set up the competitive ranks yet
		Chart c = chart(Collections.singletonMap(126, "Owner"));
		OrgChart.Tier letters = c.competitive.get(0);
		assertEquals(7, letters.slots.size());
		assertTrue(letters.slots.stream().allMatch(s -> s.members.isEmpty()));
		assertEquals("Gem league", c.competitive.get(1).name);
	}

	@Test
	public void gradeFollowsTheListNotTheInGameRankOrder()
	{
		// Dragon set up above Soul in-game: still graded D, and Soul still S
		Map<Integer, String> r = new HashMap<>();
		r.put(60, "Dragon");
		r.put(50, "Soul");
		Chart c = chart(r, new Member("Dee", 60, "Dragon", false));
		assertEquals("D", slot(c.competitive.get(0), "Dragon").grade);
		assertEquals(1, slot(c.competitive.get(0), "Dragon").members.size());
		assertEquals("S", slot(c.competitive.get(0), "Soul").grade);
	}

	@Test
	public void staffAndGemTiersHaveNoGrades()
	{
		Chart c = chart(corRanks());
		assertTrue(c.staff.stream().flatMap(t -> t.slots.stream()).allMatch(s -> s.grade == null));
		assertTrue(c.competitive.get(1).slots.stream().allMatch(s -> s.grade == null));
	}

	@Test
	public void titleMatchingIgnoresCaseSpacingAndMarshallSpelling()
	{
		Map<Integer, String> r = new HashMap<>();
		r.put(95, "Marshall");
		r.put(60, "  short   GREEN guy ");
		Chart c = chart(r, new Member("M", 95, "Marshall", false));
		assertEquals(1, slot(c.staff.get(1), "Marshall").members.size());
		assertEquals("  short   GREEN guy ", c.staff.get(2).slots.get(3).title);
		assertTrue(c.others.isEmpty());
	}

	@Test
	public void unchartedRanksGoToOthers()
	{
		Chart c = chart(Collections.singletonMap(30, "Sniper"), new Member("Pvp", 30, "Sniper", false),
			new Member("Boss", 126, "Owner", false));
		assertEquals(Collections.singletonList("Sniper"), c.others.stream().map(g -> g.title).collect(Collectors.toList()));
	}
}

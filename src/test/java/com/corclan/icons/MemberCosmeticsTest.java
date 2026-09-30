package com.corclan.icons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.Test;

public class MemberCosmeticsTest
{
	private static final Predicate<String> ICONS = k -> ClanIconService.MEMBER_KEYS.contains(k);
	private static final Map<String, List<String>> BUILTIN_ICONS =
		Collections.singletonMap("lavasockz", Arrays.asList("founder", "dev"));
	private static final Map<String, String> BUILTIN_TITLES = Collections.singletonMap("lavasockz", "Developer");

	private static MemberCosmetics build(String config)
	{
		return MemberCosmetics.build(BUILTIN_ICONS, BUILTIN_TITLES, config, ICONS);
	}

	@Test
	public void builtInsApplyWithNothingElse()
	{
		MemberCosmetics c = build("");
		assertEquals(Arrays.asList("founder", "dev"), c.iconsFor("lavasockz"));
		assertEquals("Developer", c.titleFor("lavasockz"));
		assertTrue(c.iconsFor("nobody").isEmpty());
	}

	@Test
	public void configOverridesBuiltIns()
	{
		MemberCosmetics c = build("Lavasockz=crown|Clan Dev\nZezima=star,gem");
		assertEquals(Collections.singletonList("crown"), c.iconsFor("lavasockz"));
		assertEquals("Clan Dev", c.titleFor("lavasockz"));
		assertEquals(Arrays.asList("star", "gem"), c.iconsFor("zezima"));
		assertNull(c.titleFor("zezima"));
	}

	@Test
	public void unknownIconsAndUnsafeTitlesAreDropped()
	{
		MemberCosmetics c = build("Bob=rainbow,crown,crown,star,gem,fire,skull|<col=ff0000>Boss\n"
			+ "Carl=crown|this title is far too long to fit\nDave=star|<>");
		assertEquals(Arrays.asList("crown", "star", "gem", "fire"), c.iconsFor("bob"));
		assertEquals("Boss", c.titleFor("bob"));
		assertEquals(Collections.singletonList("crown"), c.iconsFor("carl"));
		assertNull(c.titleFor("carl"));
		assertNull(c.titleFor("dave"));
	}

	@Test
	public void keysIgnoreCaseTagsAndSpacing()
	{
		assertEquals("iron nick", MemberCosmetics.key("<img=12>Iron NICK "));
		assertEquals("iron nick", MemberCosmetics.key("Iron_Nick"));
		MemberCosmetics c = build("Iron_Nick=gem");
		assertEquals(Collections.singletonList("gem"), c.iconsFor(MemberCosmetics.key("iron nick")));
	}
}

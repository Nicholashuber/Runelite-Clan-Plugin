package com.corclan.icons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.corclan.sync.SyncModels;
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

	private static MemberCosmetics build(List<SyncModels.Cosmetic> server, String config)
	{
		return MemberCosmetics.build(BUILTIN_ICONS, BUILTIN_TITLES, server, config, ICONS);
	}

	@Test
	public void builtInsApplyWithNothingElse()
	{
		MemberCosmetics c = build(Collections.emptyList(), "");
		assertEquals(Arrays.asList("founder", "dev"), c.iconsFor("lavasockz"));
		assertEquals("Developer", c.titleFor("lavasockz"));
		assertTrue(c.iconsFor("nobody").isEmpty());
	}

	@Test
	public void serverOverridesBuiltInsAndConfigOverridesServer()
	{
		List<SyncModels.Cosmetic> server = Arrays.asList(
			new SyncModels.Cosmetic("Lavasockz", Collections.singletonList("crown"), "Clan Dev"),
			new SyncModels.Cosmetic("Zezima", Arrays.asList("star", "gem"), null));

		MemberCosmetics fromServer = build(server, null);
		assertEquals(Collections.singletonList("crown"), fromServer.iconsFor("lavasockz"));
		assertEquals("Clan Dev", fromServer.titleFor("lavasockz"));
		assertEquals(Arrays.asList("star", "gem"), fromServer.iconsFor("zezima"));

		MemberCosmetics withConfig = build(server, "Zezima=fire|Event Host");
		assertEquals(Collections.singletonList("fire"), withConfig.iconsFor("zezima"));
		assertEquals("Event Host", withConfig.titleFor("zezima"));
	}

	@Test
	public void unknownIconsAndUnsafeTitlesAreDropped()
	{
		List<SyncModels.Cosmetic> server = Collections.singletonList(
			new SyncModels.Cosmetic("Bob", Arrays.asList("rainbow", "crown", "crown", "star", "gem", "fire", "skull"), "<col=ff0000>Boss"));
		MemberCosmetics c = build(server, "Carl=crown|this title is far too long to fit\nDave=star|<>");
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
		MemberCosmetics c = build(Collections.singletonList(
			new SyncModels.Cosmetic("Iron Nick", Collections.singletonList("gem"), null)), "");
		assertEquals(Collections.singletonList("gem"), c.iconsFor(MemberCosmetics.key("iron nick")));
	}
}

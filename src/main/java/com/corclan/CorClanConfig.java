package com.corclan;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(CorClanConfig.GROUP)
public interface CorClanConfig extends Config
{
	String GROUP = "corclan";

	@ConfigSection(
		name = "Links",
		description = "Buttons shown at the top of the CoR panel",
		position = 0
	)
	String linksSection = "links";

	@ConfigSection(
		name = "Chat icons",
		description = "Custom icons shown next to clan members in clan chat (only on your own client)",
		position = 1
	)
	String iconsSection = "icons";

	@ConfigSection(
		name = "GZ tracker",
		description = "Counts gz's in clan chat and attributes them to the latest clan broadcast",
		position = 2
	)
	String gzSection = "gz";

	@ConfigSection(
		name = "CoR party",
		description = "Optional: join the clan's RuneLite party to share gz counts, staff icon settings, your rank glow and (if you choose) your map position",
		position = 3
	)
	String partySection = "party";

	@ConfigSection(
		name = "Staff",
		description = "Clan icon settings managed by staff and shared through the CoR party",
		position = 5,
		closedByDefault = true
	)
	String staffSection = "staff";

	/** Used when the passphrase setting is empty. */
	String DEFAULT_PARTY_PASSPHRASE = "cor";

	@ConfigSection(
		name = "Rank glow",
		description = "Outlines clan members in the game world by rank. Only you see it",
		position = 4
	)
	String glowSection = "glow";

	@ConfigItem(
		keyName = "rankGlow",
		name = "Show clan rank glows",
		description = "Show the glows clan members picked for themselves. Turn off to see none",
		position = 0,
		section = glowSection
	)
	default boolean rankGlow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowGoldOutline",
		name = "Gold outline (Owner)",
		description = "A breathing golden outline around you. Owner only: set in the CoR side panel (Owner glow) or with ::myglow",
		position = 1,
		section = glowSection,
		hidden = true
	)
	default boolean glowGoldOutline()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowStormCloud",
		name = "Storm cloud (Owner)",
		description = "A dark storm cloud hanging over your head. Owner only: set in the CoR side panel (Owner glow) or with ::myglow",
		position = 2,
		section = glowSection,
		hidden = true
	)
	default boolean glowStormCloud()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowLightningStrikes",
		name = "Lightning strikes (Owner)",
		description = "Frequent lightning strikes on you. Owner only: set in the CoR side panel (Owner glow) or with ::myglow",
		position = 3,
		section = glowSection,
		hidden = true
	)
	default boolean glowLightningStrikes()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowShocks",
		name = "Shocks (Owner)",
		description = "Electric shocks crackling over your body. Owner only: set in the CoR side panel (Owner glow) or with ::myglow",
		position = 4,
		section = glowSection,
		hidden = true
	)
	default boolean glowShocks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowBigStrike",
		name = "Big strike (Owner)",
		description = "A rare, big lightning strike on you. Owner only: set in the CoR side panel (Owner glow) or with ::myglow",
		position = 5,
		section = glowSection,
		hidden = true
	)
	default boolean glowBigStrike()
	{
		return true;
	}

	@ConfigSection(
		name = "Challenger rank glows",
		description = "The glow other CoR plugin users see on you for your Challenger League tier, Soul (S) to Gnome child (F). Shared through the CoR party. Each needs its tier or higher. Placeholders for now: none are drawn yet",
		position = 6,
		closedByDefault = true
	)
	String challengerGlowSection = "challengerGlow";

	@ConfigItem(
		keyName = "glowSoul",
		name = "Soul glow",
		description = "Placeholder, not drawn yet. Needs the Soul rank or higher",
		position = 0,
		section = challengerGlowSection
	)
	default boolean glowSoul()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowAchiever",
		name = "Achiever glow",
		description = "Placeholder, not drawn yet. Needs the Achiever rank or higher",
		position = 1,
		section = challengerGlowSection
	)
	default boolean glowAchiever()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowBeast",
		name = "Beast glow",
		description = "Placeholder, not drawn yet. Needs the Beast rank or higher",
		position = 2,
		section = challengerGlowSection
	)
	default boolean glowBeast()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowCompetitor",
		name = "Competitor glow",
		description = "Placeholder, not drawn yet. Needs the Competitor rank or higher",
		position = 3,
		section = challengerGlowSection
	)
	default boolean glowCompetitor()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowDragon",
		name = "Dragon glow",
		description = "Placeholder, not drawn yet. Needs the Dragon rank or higher",
		position = 4,
		section = challengerGlowSection
	)
	default boolean glowDragon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowElite",
		name = "Elite glow",
		description = "Placeholder, not drawn yet. Needs the Elite rank or higher",
		position = 5,
		section = challengerGlowSection
	)
	default boolean glowElite()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowGnomeChild",
		name = "Gnome child glow",
		description = "Placeholder, not drawn yet. Needs the Gnome child rank or higher",
		position = 6,
		section = challengerGlowSection
	)
	default boolean glowGnomeChild()
	{
		return true;
	}

	@ConfigSection(
		name = "Gem tier glows",
		description = "The glow other CoR plugin users see on you for your Gem League rank, Opal to Zenyte. Shared through the CoR party. Each needs its gem or higher. Placeholders for now: none are drawn yet",
		position = 7,
		closedByDefault = true
	)
	String gemGlowSection = "gemGlow";

	@ConfigItem(
		keyName = "glowZenyte",
		name = "Zenyte glow",
		description = "Placeholder, not drawn yet. Needs the Zenyte rank or higher",
		position = 0,
		section = gemGlowSection
	)
	default boolean glowZenyte()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowOnyx",
		name = "Onyx glow",
		description = "Placeholder, not drawn yet. Needs the Onyx rank or higher",
		position = 1,
		section = gemGlowSection
	)
	default boolean glowOnyx()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowDragonstone",
		name = "Dragonstone glow",
		description = "Placeholder, not drawn yet. Needs the Dragonstone rank or higher",
		position = 2,
		section = gemGlowSection
	)
	default boolean glowDragonstone()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowDiamond",
		name = "Diamond glow",
		description = "Placeholder, not drawn yet. Needs the Diamond rank or higher",
		position = 3,
		section = gemGlowSection
	)
	default boolean glowDiamond()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowRuby",
		name = "Ruby glow",
		description = "Placeholder, not drawn yet. Needs the Ruby rank or higher",
		position = 4,
		section = gemGlowSection
	)
	default boolean glowRuby()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowEmerald",
		name = "Emerald glow",
		description = "Placeholder, not drawn yet. Needs the Emerald rank or higher",
		position = 5,
		section = gemGlowSection
	)
	default boolean glowEmerald()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowSapphire",
		name = "Sapphire glow",
		description = "Placeholder, not drawn yet. Needs the Sapphire rank or higher",
		position = 6,
		section = gemGlowSection
	)
	default boolean glowSapphire()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowOpal",
		name = "Opal glow",
		description = "Placeholder, not drawn yet. Needs the Opal rank or higher",
		position = 7,
		section = gemGlowSection
	)
	default boolean glowOpal()
	{
		return true;
	}

	@ConfigItem(
		keyName = "partyEnabled",
		name = "Join the CoR party",
		description = "Puts you in the clan's RuneLite party (RuneLite's own party service, the same one the Party plugin uses). "
			+ "Party members share gz counts, so leaderboards and weekly trophies match for everyone online, and get the "
			+ "icon settings from clan staff. Members also see the rank glow you picked. You can only be in one party, so this leaves any other party you are in.",
		warning = "This joins the CoR clan's RuneLite party and leaves any party you are in now (raids, bossing). "
			+ "Everyone in the party can see your character name. Continue?",
		position = 0,
		section = partySection
	)
	default boolean partyEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "partyPassphrase",
		name = "Party passphrase",
		description = "Everyone in CoR must use the same one. Leave empty for the default. Staff can pick a new one and "
			+ "share it in Discord to keep strangers out.",
		position = 1,
		section = partySection
	)
	default String partyPassphrase()
	{
		return "";
	}

	@ConfigItem(
		keyName = "shareLocation",
		name = "Share my location",
		description = "Show your world and map position to CoR party members on their world map, and see theirs. Only "
			+ "people who share can see others. Paused inside the Wilderness unless 'Share in Wilderness' is on, and "
			+ "always paused inside instances.",
		position = 2,
		section = partySection
	)
	default boolean shareLocation()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shareInWilderness",
		name = "Share in Wilderness",
		description = "Keep sharing your position inside the Wilderness, for clan PK trips. Off means you disappear from the "
			+ "clan map as soon as you enter the Wilderness and can't see others until you leave.",
		position = 3,
		section = partySection
	)
	default boolean shareInWilderness()
	{
		return false;
	}

	@ConfigItem(
		keyName = "clanMemberIcons",
		name = "Clan member icons",
		description = "Same format as 'Member icons'. Edited by clan staff (Administrator rank or higher) and sent to "
			+ "everyone in the CoR party; this box shows the clan's current list and is overwritten by staff updates.",
		position = 0,
		section = staffSection
	)
	default String clanMemberIcons()
	{
		return "";
	}

	@ConfigItem(
		keyName = "clanRankIcons",
		name = "Clan rank icons",
		description = "One per line: in-game rank title=icon or icon,icon|Title, e.g. Gnome child=gem|Gnome. Gives everyone "
			+ "holding that clan rank the icons and title, so promoting someone in game changes their icon. Edited by "
			+ "staff and shared like 'Clan member icons'.",
		position = 1,
		section = staffSection
	)
	default String clanRankIcons()
	{
		return "";
	}

	@ConfigItem(
		keyName = "discordUrl",
		name = "Discord invite",
		description = "Opened by the Discord button in the panel",
		position = 0,
		section = linksSection
	)
	default String discordUrl()
	{
		return "https://discord.gg/hSx5H7DP4";
	}

	@ConfigItem(
		keyName = "websiteUrl",
		name = "Website",
		description = "Opened by the Website button in the panel",
		position = 1,
		section = linksSection
	)
	default String websiteUrl()
	{
		return "";
	}

	@ConfigItem(
		keyName = "replaceRankIcons",
		name = "Custom rank icons",
		description = "Replace the default clan rank icon next to names in clan chat with CoR icons",
		position = 0,
		section = iconsSection
	)
	default boolean replaceRankIcons()
	{
		return true;
	}

	@ConfigItem(
		keyName = "memberIcons",
		name = "Member icons",
		description = "Your own additions, only on your client; they win over the clan staff's list. One per line: name=icon or name=icon,icon|Title. Icons: crown, trophy, star, skull, gem, fire, founder, dev. Example: Zezima=crown,star|Event Host",
		position = 1,
		section = iconsSection
	)
	default String memberIcons()
	{
		return "";
	}

	@ConfigItem(
		keyName = "gzKingIcon",
		name = "GZ King icon",
		description = "Give the member who has given the most gz's (all time, on this client) a special icon",
		position = 2,
		section = iconsSection
	)
	default boolean gzKingIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "weeklyTrophies",
		name = "Weekly top 3 trophies",
		description = "Give this week's top 3 gz givers (resets Sunday 00:00 UTC) a gold, silver or bronze trophy. Counted on this client, or across the CoR party when you are in it",
		position = 3,
		section = iconsSection
	)
	default boolean weeklyTrophies()
	{
		return true;
	}

	@ConfigItem(
		keyName = "gzTrackingEnabled",
		name = "Track gz's",
		description = "Count gz / grats / congrats messages in clan chat",
		position = 0,
		section = gzSection
	)
	default boolean gzTrackingEnabled()
	{
		return true;
	}

	@Range(min = 10, max = 600)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "gzWindowSeconds",
		name = "GZ window",
		description = "How long after a clan broadcast gz's are attributed to that member",
		position = 1,
		section = gzSection
	)
	default int gzWindowSeconds()
	{
		return 90;
	}

	@ConfigItem(
		keyName = "oneGzPerPersonPerBroadcast",
		name = "One gz per person",
		description = "Only count the first gz from each member for a given broadcast",
		position = 2,
		section = gzSection
	)
	default boolean oneGzPerPersonPerBroadcast()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackGuestClan",
		name = "Include guest clan chat",
		description = "Also count gz's and broadcasts from the guest clan channel",
		position = 3,
		section = gzSection
	)
	default boolean trackGuestClan()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show gz overlay",
		description = "On-screen box with your gz counts, the active gz window and a flash when a gz is counted",
		position = 4,
		section = gzSection
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showGzCount",
		name = "Show [GZ count] on gz lines",
		description = "Append the sender's all-time gz total to their chat line when they say gz (only you see it)",
		position = 5,
		section = gzSection
	)
	default boolean showGzCount()
	{
		return true;
	}

	@ConfigItem(
		keyName = "announceGz",
		name = "Chat message when counted",
		description = "Also print a local game message each time a gz is counted (only you see it)",
		position = 6,
		section = gzSection
	)
	default boolean announceGz()
	{
		return false;
	}

	@Range(min = 5, max = 200)
	@ConfigItem(
		keyName = "maxGzMessageLength",
		name = "Max gz message length",
		description = "Messages longer than this never count as a gz",
		position = 7,
		section = gzSection
	)
	default int maxGzMessageLength()
	{
		return 40;
	}

	@ConfigSection(
		name = "Discord drops",
		description = "Post your own clan broadcasts (drops, pets, levels) with a screenshot to the clan's Discord channel",
		position = 8
	)
	String discordSection = "discord";

	@ConfigItem(
		keyName = "discordWebhookUrl",
		name = "Webhook URL",
		description = "The clan's Discord webhook (staff pin it in Discord). Empty means nothing is ever posted. "
			+ "Only discord.com webhook links are used.",
		secret = true,
		position = 0,
		section = discordSection
	)
	default String discordWebhookUrl()
	{
		return "";
	}

	@ConfigItem(
		keyName = "discordDrops",
		name = "Post my broadcasts",
		description = "When a clan broadcast is about you (a drop, pet, level, collection log), post it to the webhook. "
			+ "Only your own broadcasts, so each one is posted once",
		position = 1,
		section = discordSection
	)
	default boolean discordDrops()
	{
		return true;
	}

	@ConfigItem(
		keyName = "discordScreenshot",
		name = "Include screenshot",
		description = "Attach a screenshot of your game client to the post",
		position = 2,
		section = discordSection
	)
	default boolean discordScreenshot()
	{
		return true;
	}
}

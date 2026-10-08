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
		name = "Clan sync",
		description = "Optional: report your own gz's, rank and glow picks to the CoR clan server and get clan-wide leaderboards, icons, titles and glows from it",
		position = 3
	)
	String syncSection = "sync";

	@ConfigSection(
		name = "CoR party",
		description = "Optional: join the clan's RuneLite party with the Join CoR party button in the CoR side panel, to show clanmates on the world map (if you choose to share your position)",
		position = 3
	)
	String partySection = "party";

	@ConfigSection(
		name = "Staff",
		description = "Clan icon lists kept on this client only. Used while Clan sync is off; with it on, the clan server's icons and titles replace them",
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
		keyName = "syncEnabled",
		name = "Sync with clan server",
		description = "Sends the gz's you say in clan chat, clan broadcasts about you, your clan rank, your glow picks, your clan's "
			+ "rank names (no player names), your character name, your clan's name and your RuneLite account hash to the CoR clan "
			+ "server, and loads the clan-wide gz counts, chat icons, titles and glow picks from it. Nothing about other "
			+ "players and nothing else from chat is sent.",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers. "
			+ "It sends the gz's you say in clan chat, clan broadcasts about you, your clan rank, your glow picks, your clan's "
			+ "rank names, your character name, your clan's name and your RuneLite account hash to the CoR clan server. "
			+ "Turn it on?",
		position = 0,
		section = syncSection
	)
	default boolean syncEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "rankGlow",
		name = "Show clan rank glows",
		description = "Show the glows clan members picked for themselves (their picks arrive through Clan sync; without it everyone shows their rank's default). Turn off to see none",
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

	@ConfigItem(
		keyName = "devGlowOutline",
		name = "Molten outline (Lavasockz)",
		description = "A flickering molten outline around you. Lavasockz only: set in the CoR side panel (Dev glow) or with ::devglow",
		position = 10,
		section = glowSection,
		hidden = true
	)
	default boolean devGlowOutline()
	{
		return true;
	}

	@ConfigItem(
		keyName = "devGlowFlames",
		name = "Flames of Zamorak (Lavasockz)",
		description = "Flames of Zamorak burning around you nonstop. Lavasockz only: set in the CoR side panel (Dev glow) or with ::devglow",
		position = 11,
		section = glowSection,
		hidden = true
	)
	default boolean devGlowFlames()
	{
		return true;
	}

	@ConfigItem(
		keyName = "founderGlowFlames",
		name = "Red Tormented Demon flames (DAYLlGHT)",
		description = "A Tormented Demon's fire, turned red, burning on you nonstop. DAYLlGHT only: set in the CoR side panel (Founder glow) or with ::founderglow",
		position = 12,
		section = glowSection,
		hidden = true
	)
	default boolean founderGlowFlames()
	{
		return true;
	}

	@ConfigItem(
		keyName = "founderGlowSwirl",
		name = "Black swirl (DAYLlGHT)",
		description = "A black swirl spiralling up you nonstop. DAYLlGHT only: set in the CoR side panel (Founder glow) or with ::founderglow",
		position = 15,
		section = glowSection,
		hidden = true
	)
	default boolean founderGlowSwirl()
	{
		return true;
	}

	@ConfigItem(
		keyName = "founderGlowSmoke",
		name = "Smoke cloud (DAYLlGHT)",
		description = "A smoke cloud swirling around you nonstop. DAYLlGHT only: set in the CoR side panel (Founder glow) or with ::founderglow",
		position = 13,
		section = glowSection,
		hidden = true
	)
	default boolean founderGlowSmoke()
	{
		return true;
	}

	@ConfigItem(
		keyName = "founderGlowSparkle",
		name = "Gem sparkle (DAYLlGHT)",
		description = "Red gem sparkles, matching the flames, twinkling over you. DAYLlGHT only: set in the CoR side panel (Founder glow) or with ::founderglow",
		position = 14,
		section = glowSection,
		hidden = true
	)
	default boolean founderGlowSparkle()
	{
		return true;
	}

	@ConfigSection(
		name = "Challenger rank glows (locked)",
		description = "Locked: Challenger League requirements to be announced soon",
		position = 7,
		closedByDefault = true
	)
	String challengerGlowSection = "challengerGlow";

	@ConfigItem(
		keyName = "glowSoul",
		name = "Soul glow",
		description = "Challenger League requirements to be announced soon",
		position = 0,
		section = challengerGlowSection,
		hidden = true
	)
	default boolean glowSoul()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowAchiever",
		name = "Achiever glow",
		description = "Challenger League requirements to be announced soon",
		position = 1,
		section = challengerGlowSection,
		hidden = true
	)
	default boolean glowAchiever()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowBeast",
		name = "Beast glow",
		description = "Challenger League requirements to be announced soon",
		position = 2,
		section = challengerGlowSection,
		hidden = true
	)
	default boolean glowBeast()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowCompetitor",
		name = "Competitor glow",
		description = "Challenger League requirements to be announced soon",
		position = 3,
		section = challengerGlowSection,
		hidden = true
	)
	default boolean glowCompetitor()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowDragon",
		name = "Dragon glow",
		description = "Challenger League requirements to be announced soon",
		position = 4,
		section = challengerGlowSection,
		hidden = true
	)
	default boolean glowDragon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowElite",
		name = "Elite glow",
		description = "Challenger League requirements to be announced soon",
		position = 5,
		section = challengerGlowSection,
		hidden = true
	)
	default boolean glowElite()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowGnomeChild",
		name = "Gnome child glow",
		description = "Challenger League requirements to be announced soon",
		position = 6,
		section = challengerGlowSection,
		hidden = true
	)
	default boolean glowGnomeChild()
	{
		return true;
	}

	@ConfigSection(
		name = "Gem tier glows",
		description = "The glow other CoR plugin users see on you for your Gem League rank, Opal to Zenyte. Your picks reach them through Clan sync. Each needs its gem or higher. You show the highest one you tick",
		position = 6,
		closedByDefault = true
	)
	String gemGlowSection = "gemGlow";

	@ConfigItem(
		keyName = "glowZenyte",
		name = "Zenyte glow",
		description = "Needs the Zenyte rank or higher",
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
		description = "Needs the Onyx rank or higher",
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
		description = "Needs the Dragonstone rank or higher",
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
		description = "Needs the Diamond rank or higher",
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
		description = "Needs the Ruby rank or higher",
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
		description = "Needs the Emerald rank or higher",
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
		description = "Needs the Sapphire rank or higher",
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
		description = "Needs the Opal rank or higher",
		position = 7,
		section = gemGlowSection
	)
	default boolean glowOpal()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowGemOutline",
		name = "Gem outline",
		description = "Show your gem's glowing outline. Also ::outline [on|off]",
		position = 8,
		section = gemGlowSection
	)
	default boolean glowGemOutline()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowGemSparkles",
		name = "Gem sparkles",
		description = "Show your gem's twinkling stars (Diamond and up). Also ::sparkles [on|off]",
		position = 9,
		section = gemGlowSection
	)
	default boolean glowGemSparkles()
	{
		return true;
	}

	@ConfigItem(
		keyName = "partyPassphrase",
		name = "Party passphrase",
		description = "Used by the Join CoR party button in the CoR side panel. Everyone in CoR must use the same one. "
			+ "Leave empty for the default. Staff can pick a new one and share it in Discord to keep strangers out.",
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
		description = "While you are in the CoR party (Join CoR party in the side panel), show your world and map position "
			+ "to party members on their world map, and see theirs. Only "
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
		description = "Same format as 'Member icons'. Kept on this client only and no longer shared. Used while Clan sync "
			+ "is off; with it on, the icons and titles set on the clan server replace this list.",
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
			+ "holding that clan rank the icons and title, so promoting someone in game changes their icon. Kept on "
			+ "this client only; with Clan sync on, the icon picked per rank on the clan server replaces this list.",
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
		description = "Your own additions, only on your client; they win over the clan's list. One per line: name=icon or name=icon,icon|Title. Icons: crown, trophy, star, skull, gem, fire, founder, dev. Example: Zezima=crown,star|Event Host",
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
		description = "Give the member who has given the most gz's (all time; clan-wide with Clan sync on, otherwise on this client) a special icon",
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
		description = "Give this week's top 3 gz givers (resets Sunday 00:00 UTC) a gold, silver or bronze trophy. Counted on this client, or clan-wide with Clan sync on",
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
		description = "Append the sender's all-time gz total (clan-wide with Clan sync on) to their chat line when they say gz (only you see it)",
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
}

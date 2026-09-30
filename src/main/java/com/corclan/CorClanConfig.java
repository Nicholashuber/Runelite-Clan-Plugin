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
		description = "Optional: share gz counts with the CoR clan server and get clan-wide leaderboards and icons",
		position = 3
	)
	String syncSection = "sync";

	@ConfigItem(
		keyName = "syncEnabled",
		name = "Sync with clan server",
		description = "Sends the clan broadcasts and gz messages you see in clan chat, your character name and your "
			+ "RuneLite account hash to the CoR clan server, and loads the clan-wide gz leaderboard, chat icons and member icons from it. "
			+ "Nothing else from chat is sent.",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers. "
			+ "It sends clan broadcasts and gz messages from clan chat, your character name and your RuneLite account hash "
			+ "to the CoR clan server. Turn it on?",
		position = 0,
		section = syncSection
	)
	default boolean syncEnabled()
	{
		return false;
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
		description = "One per line: name=icon or name=icon,icon|Title. Icons: crown, trophy, star, skull, gem, fire, founder, dev. Example: Zezima=crown,star|Event Host",
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
}

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

	@ConfigItem(
		keyName = "discordUrl",
		name = "Discord invite",
		description = "Opened by the Discord button in the panel",
		position = 0,
		section = linksSection
	)
	default String discordUrl()
	{
		return "https://discord.gg/";
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
		description = "One per line: name=icon. Icons: crown, trophy, star, skull, gem, fire. Example: Zezima=crown",
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

	@Range(min = 5, max = 200)
	@ConfigItem(
		keyName = "maxGzMessageLength",
		name = "Max gz message length",
		description = "Messages longer than this never count as a gz",
		position = 4,
		section = gzSection
	)
	default int maxGzMessageLength()
	{
		return 40;
	}
}

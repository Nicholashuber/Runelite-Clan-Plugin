package com.corclan.chat;

import com.corclan.icons.ClanIconService;
import com.corclan.icons.MemberCosmetics;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.util.Text;

/**
 * !tier and !rank in clan chat. The player types the command themselves, so everyone in the clan sees it;
 * every CoR plugin user's client then adds the sender's clan rank and its icon to that line, and for !rank
 * their all-time gz given too. Nothing is sent to the game or other players: plugins may not post chat.
 */
@Singleton
public class RankCommands
{
	static final String TIER = "!tier";
	static final String RANK = "!rank";
	/** same blue as the plugin's other chat titles */
	private static final String COLOR = "1046fb";

	private final Client client;
	private final ChatCommandManager chatCommandManager;
	private final ClanIconService iconService;
	private Supplier<Map<String, Integer>> gzGiven;
	private Supplier<MemberCosmetics> rankCosmetics;

	@Inject
	RankCommands(Client client, ChatCommandManager chatCommandManager, ClanIconService iconService)
	{
		this.client = client;
		this.chatCommandManager = chatCommandManager;
		this.iconService = iconService;
	}

	/**
	 * @param gzGiven       all-time gz given per display name, as the plugin shows them (party-wide in the CoR party)
	 * @param rankCosmetics the staff's icons per clan rank title
	 */
	public void startUp(Supplier<Map<String, Integer>> gzGiven, Supplier<MemberCosmetics> rankCosmetics)
	{
		this.gzGiven = gzGiven;
		this.rankCosmetics = rankCosmetics;
		chatCommandManager.registerCommand(TIER, this::onCommand);
		chatCommandManager.registerCommand(RANK, this::onCommand);
	}

	public void shutDown()
	{
		chatCommandManager.unregisterCommand(TIER);
		chatCommandManager.unregisterCommand(RANK);
	}

	private void onCommand(ChatMessage event, String message)
	{
		// our own clan's chat only: the rank titles are ours there
		if (event.getType() != ChatMessageType.CLAN_CHAT || event.getMessageNode() == null)
		{
			return;
		}
		String sender = displayName(event.getName());
		ClanChannel channel = client.getClanChannel();
		ClanSettings settings = client.getClanSettings();
		ClanChannelMember member = findMember(channel, sender);
		if (member == null || member.getRank() == null || settings == null)
		{
			return;
		}
		ClanTitle title = settings.titleForRank(member.getRank());
		if (title == null || title.getName() == null || title.getName().isEmpty())
		{
			return;
		}
		String icons = iconTags(MemberCosmetics.key(title.getName()), member);
		Integer gz = command(message).equals(RANK) ? gzGiven.get().getOrDefault(sender, 0) : null;
		MessageNode node = event.getMessageNode();
		node.setValue(node.getValue() + suffix(title.getName(), icons, gz));
		client.refreshChat();
	}

	/** The staff's icons for that rank title, or else the plugin's default rank icon. */
	private String iconTags(String rankKey, ClanChannelMember member)
	{
		List<String> keys = rankCosmetics.get().iconsFor(rankKey);
		StringBuilder sb = new StringBuilder();
		if (keys.isEmpty())
		{
			append(sb, iconService.tagFor(iconService.iconForRank(member.getRank())));
		}
		for (String key : keys)
		{
			append(sb, iconService.tagFor(key));
		}
		return sb.toString();
	}

	/**
	 * What goes after the command on the chat line, e.g. " [&lt;img=5&gt;Zenyte] 152 gz given".
	 * @param icons chat icon tags, possibly empty
	 * @param gz    gz given, or null to leave it out (!tier)
	 */
	static String suffix(String title, String icons, Integer gz)
	{
		StringBuilder sb = new StringBuilder(" <col=").append(COLOR).append(">[")
			.append(icons).append(Text.removeTags(title)).append("]");
		if (gz != null)
		{
			sb.append(' ').append(gz).append(" gz given");
		}
		return sb.append("</col>").toString();
	}

	/** The command word of a chat message, lower case, e.g. "!rank". */
	static String command(String message)
	{
		String trimmed = message == null ? "" : message.trim();
		int space = trimmed.indexOf(' ');
		return (space < 0 ? trimmed : trimmed.substring(0, space)).toLowerCase();
	}

	private static ClanChannelMember findMember(ClanChannel channel, String name)
	{
		if (channel == null || name.isEmpty())
		{
			return null;
		}
		String key = MemberCosmetics.key(name);
		for (ClanChannelMember member : channel.getMembers())
		{
			if (key.equals(MemberCosmetics.key(member.getName())))
			{
				return member;
			}
		}
		return null;
	}

	/** Name as shown in game (tags stripped, regular spaces), matching the plugin's gz counts. */
	private static String displayName(String name)
	{
		return name == null ? "" : Text.removeTags(name).replace(' ', ' ').trim();
	}

	private static void append(StringBuilder sb, String tag)
	{
		if (tag != null)
		{
			sb.append(tag);
		}
	}
}

package com.corclan;

import com.corclan.clan.ClanRoster;
import com.corclan.gz.BroadcastParser;
import com.corclan.gz.BroadcastRecord;
import com.corclan.gz.GzStats;
import com.corclan.gz.GzTracker;
import com.corclan.gz.Streaks;
import com.corclan.gz.WeekResult;
import com.corclan.icons.ClanIconService;
import com.corclan.icons.MemberCosmetics;
import com.corclan.ui.CorClanOverlay;
import com.corclan.ui.CorClanPanel;
import com.corclan.ui.PanelData;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.Player;
import java.util.TreeMap;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.ClanMemberJoined;
import net.runelite.api.events.ClanMemberLeft;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "CoR Clan",
	description = "Clan sidebar, custom clan chat icons and a gz tracker for the C o R clan.",
	tags = {"clan", "cor", "gz", "chat", "icons", "social"}
)
public class CorClanPlugin extends Plugin
{
	static final String STATS_KEY = "gzStats";
	static final String WEEKLY_KEY = "gzWeekly";
	static final String WEEK_START_KEY = "gzWeekStart";
	static final String WEEK_RESULTS_KEY = "gzWeekResults";
	private static final Type WEEK_RESULTS_TYPE = new TypeToken<List<WeekResult>>(){}.getType();
	/** Saved stats, not settings: changing them must not trigger a config refresh. */
	private static final List<String> STATS_KEYS = Arrays.asList(STATS_KEY, WEEKLY_KEY, WEEK_START_KEY, WEEK_RESULTS_KEY);
	private static final String CHAT_BUILD_CALLBACK = "chatMessageBuilding";
	/** Position of the name string relative to the top of the object stack in that callback. */
	private static final int NAME_STACK_OFFSET = 3;
	/** Placeholder default from the first build; replaced by the real invite once seen. */
	private static final String OLD_DISCORD_PLACEHOLDER = "https://discord.gg/";
	private static final Pattern IMG_TAG = Pattern.compile("<img=\\d+>");
	private static final int PANEL_LEADERBOARD_SIZE = 5;
	private static final int PANEL_GIVERS_SIZE = 10;

	/** Defaults that apply without any config. The "Member icons" config box overrides these. */
	private static final Map<String, List<String>> BUILTIN_MEMBER_ICONS = Collections.singletonMap(
		"lavasockz", Collections.unmodifiableList(Arrays.asList(ClanIconService.KEY_FOUNDER, ClanIconService.KEY_DEV)));
	private static final Map<String, String> BUILTIN_MEMBER_TITLES = Collections.singletonMap(
		"lavasockz", "Developer");

	/** Chat color for titles: the same blue as the crown icon. The closing tag restores the name color. */
	private static final String TITLE_COLOR = "1046fb";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	@Inject
	private CorClanConfig config;

	@Inject
	private ClanIconService iconService;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private CorClanOverlay overlay;

	@Inject
	private ItemManager itemManager;

	private final GzTracker tracker = new GzTracker();

	/** Rebuilt whenever config changes; read by chat rendering on the client thread. */
	private volatile MemberCosmetics cosmetics = MemberCosmetics.EMPTY;

	private CorClanPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp()
	{
		// RuneLite saved the placeholder default from an earlier build; drop it so the real default applies
		if (OLD_DISCORD_PLACEHOLDER.equals(configManager.getConfiguration(CorClanConfig.GROUP, "discordUrl")))
		{
			configManager.unsetConfiguration(CorClanConfig.GROUP, "discordUrl");
		}
		tracker.load(loadStats(STATS_KEY));
		tracker.loadWeekly(loadStats(WEEKLY_KEY), loadWeekStart());
		tracker.loadWeekResults(loadWeekResults());
		if (tracker.rollWeek(System.currentTimeMillis()))
		{
			persistStats();
		}
		rebuildCosmetics();
		iconService.ensureRegistered();

		panel = new CorClanPanel(config, itemManager, this::resetStats);
		navButton = NavigationButton.builder()
			.tooltip("CoR Clan")
			.icon(ImageUtil.loadImageResource(CorClanPlugin.class, "panel_icon.png"))
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(overlay);
		refreshPanel();
		log.debug("CoR Clan started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
		persistStats();
		tracker.resetSession();
		log.debug("CoR Clan stopped");
	}

	@Provides
	CorClanConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CorClanConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CorClanConfig.GROUP.equals(event.getGroup()) || STATS_KEYS.contains(event.getKey()))
		{
			return;
		}
		rebuildCosmetics();
		refreshPanel();
	}

	// ---------------------------------------------------------------- gz tracking

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!config.gzTrackingEnabled())
		{
			return;
		}
		ChatMessageType type = event.getType();
		long now = System.currentTimeMillis();

		if (type == ChatMessageType.CLAN_MESSAGE || (config.trackGuestClan() && type == ChatMessageType.CLAN_GUEST_MESSAGE))
		{
			String text = BroadcastParser.clean(Text.removeTags(event.getMessage()));
			String subject = BroadcastParser.subjectOf(text);
			if (subject == null)
			{
				return;
			}
			log.debug("Broadcast for {}: {}", subject, text);
			tracker.onBroadcast(subject, text, now);
			persistStats();
			refreshPanel();
			return;
		}

		if (type == ChatMessageType.CLAN_CHAT || (config.trackGuestClan() && type == ChatMessageType.CLAN_GUEST_CHAT))
		{
			String sender = displayName(event.getName());
			String message = Text.removeTags(event.getMessage());
			GzTracker.Settings settings = new GzTracker.Settings(
				config.gzWindowSeconds() * 1000L,
				config.oneGzPerPersonPerBroadcast(),
				config.maxGzMessageLength());
			if (tracker.onClanChat(sender, message, now, settings))
			{
				persistStats();
				refreshPanel();
				tagGzCount(event.getMessageNode(), sender);
				announce(sender, tracker.getLastGzSubject());
			}
		}
	}

	/** Appends "[GZ count: N]" to the chat line on this client, N being the sender's all-time gz total. */
	private void tagGzCount(MessageNode node, String sender)
	{
		if (!config.showGzCount() || node == null)
		{
			return;
		}
		int count = tracker.getAllTime().getGiven().getOrDefault(sender, 0);
		node.setValue(node.getValue() + " <col=" + TITLE_COLOR + ">[GZ count: " + count + "]</col>");
	}

	/** Local-only game message; nothing is sent to the game server or other players. */
	private void announce(String giver, String subject)
	{
		if (!config.announceGz())
		{
			return;
		}
		String text = subject == null
			? "CoR: gz from " + giver + " counted (no broadcast open)"
			: "CoR: gz from " + giver + " counted for " + subject
				+ " (" + tracker.getAllTime().getReceived().getOrDefault(subject, 0) + " total)";
		clientThread.invokeLater(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", text, null));
	}

	public GzTracker getTracker()
	{
		return tracker;
	}

	/** Display name of the logged-in character, or null when not logged in. Client thread only. */
	public String localPlayerName()
	{
		Player me = client.getLocalPlayer();
		if (me == null || me.getName() == null)
		{
			return null;
		}
		return displayName(me.getName());
	}

	/** The top gz giver as counted on this client. */
	private String gzKing()
	{
		return tracker.getAllTime().topGiver();
	}

	// ---------------------------------------------------------------- ::cor / ::test

	/**
	 * Double-colon commands are handled inside the client and never sent to the game server. This one
	 * prints a local CoR banner with the gz leaderboard. Only the player who typed it sees it.
	 * Colours come from RuneLite's chat colour types so they adapt to the opaque / transparent chatbox.
	 */
	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		String cmd = event.getCommand().toLowerCase();
		if (!cmd.equals("cor") && !cmd.equals("test"))
		{
			return;
		}
		GzStats stats = tracker.getAllTime();
		int rhino = iconService.indexFor(ClanIconService.KEY_STAFF);
		int gzIcon = iconService.indexFor(ClanIconService.KEY_GZ_KING);
		int crown = iconService.indexFor(ClanIconService.KEY_FOUNDER);

		ChatMessageBuilder banner = new ChatMessageBuilder();
		icon(banner, rhino);
		banner.append(ChatColorType.HIGHLIGHT).append("CoR Clan").append(ChatColorType.NORMAL)
			.append(" - " + stats.totalGiven() + " gz counted ");
		icon(banner, rhino);
		say(banner);

		List<Map.Entry<String, Integer>> givers = GzStats.top(stats.getGiven(), 1);
		ChatMessageBuilder kingLine = new ChatMessageBuilder();
		icon(kingLine, gzIcon);
		if (!givers.isEmpty())
		{
			kingLine.append(ChatColorType.NORMAL).append("GZ King: ")
				.append(ChatColorType.HIGHLIGHT).append(givers.get(0).getKey())
				.append(ChatColorType.NORMAL).append(" with " + givers.get(0).getValue() + " gz");
		}
		else
		{
			kingLine.append(ChatColorType.NORMAL).append("No GZ King yet. Say ")
				.append(ChatColorType.HIGHLIGHT).append("gz")
				.append(ChatColorType.NORMAL).append(" to claim the throne!");
		}
		say(kingLine);

		List<Map.Entry<String, Integer>> top = GzStats.top(stats.getReceived(), 3);
		if (!top.isEmpty())
		{
			ChatMessageBuilder topLine = new ChatMessageBuilder().append(ChatColorType.NORMAL).append("Most gz'd: ");
			int place = 1;
			for (Map.Entry<String, Integer> e : top.subList(0, Math.min(3, top.size())))
			{
				topLine.append(ChatColorType.NORMAL).append(place > 1 ? ", " : "")
					.append(e.getKey() + " ")
					.append(ChatColorType.HIGHLIGHT).append(String.valueOf(e.getValue()));
				place++;
			}
			say(topLine);
		}

		String me = localPlayerName();
		if (me != null)
		{
			ChatMessageBuilder meLine = new ChatMessageBuilder();
			icon(meLine, crown);
			meLine.append(ChatColorType.NORMAL).append("You: ")
				.append(ChatColorType.HIGHLIGHT).append(String.valueOf(stats.getGiven().getOrDefault(me, 0)))
				.append(ChatColorType.NORMAL).append(" gz given, ")
				.append(ChatColorType.HIGHLIGHT).append(String.valueOf(stats.getReceived().getOrDefault(me, 0)))
				.append(ChatColorType.NORMAL).append(" received on this client");
			say(meLine);
		}
	}

	private static void icon(ChatMessageBuilder builder, int index)
	{
		if (index >= 0)
		{
			builder.img(index).append(" ");
		}
	}

	private void say(ChatMessageBuilder message)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(message.build())
			.build());
	}

	// ---------------------------------------------------------------- chat icons

	/**
	 * RuneLite fires this callback from the chatbox builder script with the message id on top of
	 * the int stack and the name being rendered three slots down the object stack (the same slots
	 * RuneLite's own chat channel plugin uses for friends chat rank icons). Rewriting that string
	 * only changes how the line is drawn on this client.
	 */
	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!CHAT_BUILD_CALLBACK.equals(event.getEventName()))
		{
			return;
		}
		int[] intStack = client.getIntStack();
		int intSize = client.getIntStackSize();
		Object[] objectStack = client.getObjectStack();
		int objectSize = client.getObjectStackSize();
		if (intSize < 1 || objectSize < NAME_STACK_OFFSET)
		{
			return;
		}

		int uid = intStack[intSize - 1];
		MessageNode node = client.getMessages().get(uid);
		if (node == null)
		{
			return;
		}
		ChatMessageType type = node.getType();
		if (type != ChatMessageType.CLAN_CHAT && type != ChatMessageType.CLAN_GUEST_CHAT)
		{
			return;
		}
		int nameSlot = objectSize - NAME_STACK_OFFSET;
		Object slot = objectStack[nameSlot];
		if (!(slot instanceof String))
		{
			return;
		}
		String name = (String) slot;
		String key = MemberCosmetics.key(name);
		MemberCosmetics current = cosmetics;
		String tags = iconTagsFor(type, key, current);
		String title = current.titleFor(key);
		if (tags.isEmpty() && title == null)
		{
			return;
		}
		String base = config.replaceRankIcons() ? IMG_TAG.matcher(name).replaceAll("") : name;
		StringBuilder sb = new StringBuilder(tags);
		if (title != null)
		{
			sb.append("<col=").append(TITLE_COLOR).append(">[").append(title).append("]</col> ");
		}
		objectStack[nameSlot] = sb.append(base).toString();
	}

	/**
	 * Icons stack, left to right: member icons (built-in, config), the GZ King badge, then
	 * the rank rhino. Returns "" when there is nothing to show.
	 */
	private String iconTagsFor(ChatMessageType type, String key, MemberCosmetics current)
	{
		if (key.isEmpty())
		{
			return "";
		}
		StringBuilder sb = new StringBuilder();

		for (String icon : current.iconsFor(key))
		{
			append(sb, iconService.tagFor(icon));
		}

		if (config.gzKingIcon())
		{
			String king = gzKing();
			if (king != null && key.equals(MemberCosmetics.key(king)))
			{
				append(sb, iconService.tagFor(ClanIconService.KEY_GZ_KING));
			}
		}

		if (config.replaceRankIcons())
		{
			ClanChannel channel = type == ChatMessageType.CLAN_CHAT ? client.getClanChannel() : client.getGuestClanChannel();
			ClanChannelMember member = channel == null ? null : findMember(channel, key);
			if (member != null)
			{
				append(sb, iconService.tagFor(iconService.iconForRank(member.getRank())));
			}
		}
		return sb.toString();
	}

	private static void append(StringBuilder sb, String tag)
	{
		if (tag != null)
		{
			sb.append(tag);
		}
	}

	private static ClanChannelMember findMember(ClanChannel channel, String key)
	{
		for (ClanChannelMember member : channel.getMembers())
		{
			if (key.equals(MemberCosmetics.key(member.getName())))
			{
				return member;
			}
		}
		return null;
	}

	// ---------------------------------------------------------------- helpers

	/** Safe from any thread: builds a new immutable snapshot and swaps it in. */
	private void rebuildCosmetics()
	{
		cosmetics = MemberCosmetics.build(
			BUILTIN_MEMBER_ICONS,
			BUILTIN_MEMBER_TITLES,
			config.memberIcons(),
			iconService::isMemberKey);
	}

	/** Name as shown in game (tags stripped, regular spaces). */
	private static String displayName(String name)
	{
		if (name == null)
		{
			return "";
		}
		return Text.removeTags(name).replace(' ', ' ').trim();
	}

	private GzStats loadStats(String key)
	{
		String json = configManager.getConfiguration(CorClanConfig.GROUP, key);
		if (json == null || json.isEmpty())
		{
			return new GzStats();
		}
		try
		{
			GzStats stats = gson.fromJson(json, GzStats.class);
			return stats != null ? stats : new GzStats();
		}
		catch (JsonSyntaxException ex)
		{
			log.debug("Discarding unreadable gz stats", ex);
			return new GzStats();
		}
	}

	/** Start of the saved weekly stats' week, or 0 (forcing a fresh week) if missing or unreadable. */
	private long loadWeekStart()
	{
		String raw = configManager.getConfiguration(CorClanConfig.GROUP, WEEK_START_KEY);
		try
		{
			return raw == null ? 0L : Long.parseLong(raw);
		}
		catch (NumberFormatException ex)
		{
			return 0L;
		}
	}

	private List<WeekResult> loadWeekResults()
	{
		String json = configManager.getConfiguration(CorClanConfig.GROUP, WEEK_RESULTS_KEY);
		if (json == null || json.isEmpty())
		{
			return new ArrayList<>();
		}
		try
		{
			List<WeekResult> results = gson.fromJson(json, WEEK_RESULTS_TYPE);
			return results != null ? results : new ArrayList<>();
		}
		catch (JsonSyntaxException ex)
		{
			log.debug("Discarding unreadable weekly results", ex);
			return new ArrayList<>();
		}
	}

	private void persistStats()
	{
		configManager.setConfiguration(CorClanConfig.GROUP, STATS_KEY, gson.toJson(tracker.getAllTime()));
		configManager.setConfiguration(CorClanConfig.GROUP, WEEKLY_KEY, gson.toJson(tracker.getWeekly()));
		configManager.setConfiguration(CorClanConfig.GROUP, WEEK_START_KEY, String.valueOf(tracker.getWeekStart()));
		configManager.setConfiguration(CorClanConfig.GROUP, WEEK_RESULTS_KEY, gson.toJson(tracker.getWeekResults(), WEEK_RESULTS_TYPE));
	}

	private void resetStats()
	{
		clientThread.invokeLater(() ->
		{
			tracker.resetAllTime();
			tracker.resetSession();
			persistStats();
			refreshPanel();
		});
	}

	/** Builds a snapshot on the client thread (where the stats change) and hands it to Swing. */
	private void refreshPanel()
	{
		clientThread.invokeLater(() ->
		{
			CorClanPanel p = panel;
			if (p == null)
			{
				return;
			}
			PanelData data = panelData();
			SwingUtilities.invokeLater(() -> p.refresh(data));
		});
	}

	private PanelData panelData()
	{
		// a quiet week still has to roll over on screen, not just on the next gz
		if (tracker.rollWeek(System.currentTimeMillis()))
		{
			persistStats();
		}
		GzStats allTime = tracker.getAllTime();
		GzStats session = tracker.getSession();
		String me = localPlayerName();

		String mine = me == null
			? "Log in to see your own counts"
			: String.format("You: gave %d (%d today), got %d (%d today)",
				allTime.getGiven().getOrDefault(me, 0), session.getGiven().getOrDefault(me, 0),
				allTime.getReceived().getOrDefault(me, 0), session.getReceived().getOrDefault(me, 0));
		String summary = String.format("This client: %d given, %d received all time",
			allTime.totalGiven(), allTime.totalReceived());

		List<Map.Entry<String, Integer>> givers = GzStats.top(allTime.getGiven(), PANEL_GIVERS_SIZE);
		List<Map.Entry<String, Integer>> receivers = GzStats.top(allTime.getReceived(), PANEL_LEADERBOARD_SIZE);
		List<BroadcastRecord> recent = new ArrayList<>(allTime.getRecent());

		List<Map.Entry<String, Integer>> weeklyGivers = GzStats.top(tracker.getWeekly().getGiven(), PANEL_LEADERBOARD_SIZE);

		// every giver this client has counted
		List<Map.Entry<String, Integer>> allGivers = GzStats.top(allTime.getGiven(), Integer.MAX_VALUE);

		return new PanelData(mine, summary, givers, receivers, recent,
			weeklyGivers, tracker.getWeekStart(), tracker.longestStreak(), tracker.currentStreak(), allGivers,
			clanRoster(), clanRankTitles());
	}

	/**
	 * Every rank the clan has set up (rank number to title), including ranks nobody holds, so the
	 * org chart can show vacant ranks and find the letter tiers between Soul and Gnome child.
	 */
	private Map<Integer, String> clanRankTitles()
	{
		ClanSettings settings = client.getClanSettings();
		Map<Integer, String> titles = new TreeMap<>();
		if (settings == null)
		{
			return titles;
		}
		for (int rank = 0; rank <= ClanRank.OWNER.getRank(); rank++)
		{
			ClanTitle title = settings.titleForRank(new ClanRank(rank));
			if (title != null && title.getName() != null && !title.getName().isEmpty())
			{
				titles.put(rank, title.getName());
			}
		}
		return titles;
	}

	/**
	 * Clan members grouped by in-game rank, read from the game's clan data (client thread only).
	 * @return null when not logged in or not in a clan
	 */
	private List<ClanRoster.RankGroup> clanRoster()
	{
		ClanSettings settings = client.getClanSettings();
		if (settings == null)
		{
			return null;
		}
		ClanChannel channel = client.getClanChannel();
		List<ClanRoster.Member> members = new ArrayList<>();
		for (ClanMember member : settings.getMembers())
		{
			ClanRank rank = member.getRank();
			if (member.getName() == null || rank == null)
			{
				continue;
			}
			ClanTitle title = settings.titleForRank(rank);
			boolean online = channel != null && channel.findMember(member.getName()) != null;
			members.add(new ClanRoster.Member(displayName(member.getName()), rank.getRank(),
				title != null ? title.getName() : null, online));
		}
		return ClanRoster.group(members);
	}

	// the clan members section shows who is online, so redraw when that changes
	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged event)
	{
		refreshPanel();
	}

	@Subscribe
	public void onClanMemberJoined(ClanMemberJoined event)
	{
		refreshPanel();
	}

	@Subscribe
	public void onClanMemberLeft(ClanMemberLeft event)
	{
		refreshPanel();
	}
}

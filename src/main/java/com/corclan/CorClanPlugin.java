package com.corclan;

import com.corclan.gz.BroadcastParser;
import com.corclan.gz.BroadcastRecord;
import com.corclan.gz.GzStats;
import com.corclan.gz.GzTracker;
import com.corclan.gz.Streaks;
import com.corclan.gz.WeekResult;
import com.corclan.icons.ClanIconService;
import com.corclan.icons.MemberCosmetics;
import com.corclan.map.ClanMapPoints;
import com.corclan.map.LocationRules;
import com.corclan.sync.ClanApi;
import com.corclan.sync.SyncModels;
import com.corclan.sync.SyncQueue;
import com.corclan.ui.CorClanOverlay;
import com.corclan.ui.CorClanPanel;
import com.corclan.ui.PanelData;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
import net.runelite.api.GameState;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.VarbitID;
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
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "CoR Clan",
	description = "Clan sidebar, custom clan chat icons, a gz tracker and an opt-in clan map for the C o R clan.",
	tags = {"clan", "cor", "gz", "chat", "icons", "social", "map"}
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

	/** Defaults that apply without any config. The clan server and the "Member icons" config box override these. */
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
	private ClanApi clanApi;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ClanMapPoints mapPoints;

	// clan map: send every 5 game ticks (~3s) while "Share my location" is on
	private static final int LOCATION_TICKS = 5;
	private int locationTick;
	/** the server currently has our position, so it must be told when we stop */
	private boolean locationShared;
	private SyncModels.Reporter locationReporter;

	private final GzTracker tracker = new GzTracker();
	private final SyncQueue syncQueue = new SyncQueue();

	/** Rebuilt whenever config or server data changes; read by chat rendering on the client thread. */
	private volatile MemberCosmetics cosmetics = MemberCosmetics.EMPTY;

	// clan sync state, written from the client thread and OkHttp threads
	private volatile SyncModels.Reporter reporter;
	private volatile SyncModels.Leaderboard serverLeaderboard;
	private volatile List<SyncModels.Cosmetic> serverCosmetics = Collections.emptyList();
	/** once the server's icons and titles have loaded they replace the built-in defaults (the server owns them) */
	private volatile boolean serverCosmeticsLoaded;
	/** rank names are sent once per login */
	private boolean ranksReported;
	private volatile long lastServerUpdate;

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
		if (config.syncEnabled())
		{
			pullFromServer();
		}
		refreshPanel();
		log.debug("CoR Clan started");
	}

	@Override
	protected void shutDown()
	{
		if (config.syncEnabled())
		{
			// last chance to send what is queued; the server merges any duplicates
			flushSync();
		}
		stopSharingLocation();
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
		persistStats();
		tracker.resetSession();
		clearServerState();
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
		if ("syncEnabled".equals(event.getKey()))
		{
			if (config.syncEnabled())
			{
				pullFromServer();
			}
			else
			{
				clearServerState();
			}
		}
		if ("shareLocation".equals(event.getKey()) && !config.shareLocation())
		{
			clientThread.invokeLater(this::stopSharingLocation);
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
			if (type == ChatMessageType.CLAN_MESSAGE)
			{
				queueSync(SyncModels.Event.broadcast(subject, text, now));
			}
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
				if (type == ChatMessageType.CLAN_CHAT)
				{
					queueSync(SyncModels.Event.gz(sender, message, now));
				}
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

	/** The top gz giver: clan-wide when synced, otherwise as counted on this client. */
	private String gzKing()
	{
		SyncModels.Leaderboard lb = serverLeaderboard;
		if (lb != null && !lb.getGivers().isEmpty())
		{
			return lb.getGivers().get(0).getRsn();
		}
		return tracker.getAllTime().topGiver();
	}

	// ---------------------------------------------------------------- clan sync (opt-in)

	/** Client thread: remembers who is reporting and queues the event for the next batch. */
	private void queueSync(SyncModels.Event event)
	{
		if (!config.syncEnabled())
		{
			return;
		}
		long hash = client.getAccountHash();
		String me = localPlayerName();
		ClanChannel channel = client.getClanChannel();
		if (hash == -1 || me == null || channel == null || channel.getName() == null)
		{
			return;
		}
		reporter = new SyncModels.Reporter(Long.toString(hash), me, channel.getName());
		syncQueue.add(event);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			ranksReported = false;
			stopSharingLocation();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		tickLocation();
		reportRanks();
	}

	// ---------------------------------------------------------------- clan map (opt-in)

	/**
	 * Client thread, every ~3 seconds while "Share my location" is on: sends this player's own world and
	 * tile, and draws the clanmates the server sends back. Never inside instances; inside the Wilderness
	 * only when "Share in Wilderness" is on.
	 */
	private void tickLocation()
	{
		if (!config.shareLocation())
		{
			stopSharingLocation();
			return;
		}
		if (++locationTick < LOCATION_TICKS)
		{
			return;
		}
		locationTick = 0;

		Player me = client.getLocalPlayer();
		long hash = client.getAccountHash();
		ClanChannel channel = client.getClanChannel();
		if (client.getGameState() != GameState.LOGGED_IN || me == null || me.getName() == null || hash == -1
			|| channel == null || channel.getName() == null)
		{
			return;
		}
		boolean inWilderness = client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 1;
		LocationRules.Decision decision = LocationRules.decide(inWilderness, client.isInInstancedRegion(), config.shareInWilderness());
		if (decision != LocationRules.Decision.SEND)
		{
			stopSharingLocation();
			return;
		}

		WorldPoint here = me.getWorldLocation();
		SyncModels.Reporter r = new SyncModels.Reporter(Long.toString(hash), displayName(me.getName()), channel.getName());
		locationReporter = r;
		locationShared = true;
		clanApi.sendLocation(
			new SyncModels.LocationPayload(r, client.getWorld(), here.getX(), here.getY(), here.getPlane(), inWilderness),
			response -> clientThread.invokeLater(() ->
			{
				if (locationShared)
				{
					mapPoints.update(response.getPlayers());
				}
			}));
	}

	/** Client thread: tells the server to forget us and clears the map. Safe to call when not sharing. */
	private void stopSharingLocation()
	{
		if (!locationShared)
		{
			return;
		}
		locationShared = false;
		locationTick = 0;
		SyncModels.Reporter r = locationReporter;
		if (r != null)
		{
			clanApi.sendLocationStop(new SyncModels.LocationStopPayload(r));
		}
		mapPoints.clear();
	}

	/**
	 * Once per login (with sync on) sends the clan's rank numbers and their titles, e.g. 126 "Owner",
	 * 5 "Captain", so admins can pick an icon per rank. No player names are sent.
	 */
	private void reportRanks()
	{
		if (ranksReported || !config.syncEnabled())
		{
			return;
		}
		ClanSettings settings = client.getClanSettings();
		long hash = client.getAccountHash();
		String me = localPlayerName();
		if (settings == null || settings.getName() == null || hash == -1 || me == null)
		{
			return;
		}
		ranksReported = true;

		Map<Integer, String> titles = new TreeMap<>();
		List<ClanRank> ranks = new ArrayList<>(Arrays.asList(ClanRank.OWNER, ClanRank.DEPUTY_OWNER, ClanRank.ADMINISTRATOR, ClanRank.GUEST));
		for (ClanMember member : settings.getMembers())
		{
			ranks.add(member.getRank());
		}
		for (ClanRank rank : ranks)
		{
			if (rank == null || titles.containsKey(rank.getRank()))
			{
				continue;
			}
			ClanTitle title = settings.titleForRank(rank);
			if (title != null && title.getName() != null && !title.getName().isEmpty())
			{
				titles.put(rank.getRank(), title.getName());
			}
		}
		if (titles.isEmpty())
		{
			return;
		}
		List<SyncModels.RankTitle> payload = new ArrayList<>();
		titles.forEach((rank, title) -> payload.add(new SyncModels.RankTitle(rank, title)));
		clanApi.sendRanks(new SyncModels.RanksPayload(
			new SyncModels.Reporter(Long.toString(hash), me, settings.getName()), payload));
	}

	/** Sends queued sightings to the clan server in one batch. Runs off the client thread. */
	@Schedule(period = 30, unit = ChronoUnit.SECONDS, asynchronous = true)
	public void flushSync()
	{
		SyncModels.Reporter r = reporter;
		if (!config.syncEnabled() || r == null)
		{
			return;
		}
		List<SyncModels.Event> batch = syncQueue.drain(SyncQueue.MAX_BATCH);
		if (batch.isEmpty())
		{
			return;
		}
		clanApi.sendReport(new SyncModels.ReportPayload(r, System.currentTimeMillis(), batch), handled ->
		{
			if (!handled)
			{
				syncQueue.requeue(batch);
			}
		});
	}

	/** Refreshes the clan leaderboard and member icons from the clan server. */
	@Schedule(period = 2, unit = ChronoUnit.MINUTES, asynchronous = true)
	public void scheduledPull()
	{
		if (config.syncEnabled())
		{
			pullFromServer();
		}
	}

	private void pullFromServer()
	{
		clanApi.fetchLeaderboard(lb ->
		{
			if (!config.syncEnabled())
			{
				return;
			}
			serverLeaderboard = lb;
			lastServerUpdate = System.currentTimeMillis();
			refreshPanel();
		});
		clanApi.fetchCosmetics(response ->
		{
			if (!config.syncEnabled())
			{
				return;
			}
			serverCosmetics = response.getPlayers();
			serverCosmeticsLoaded = true;
			lastServerUpdate = System.currentTimeMillis();
			rebuildCosmetics();
			clientThread.invokeLater(client::refreshChat);
		});
		clanApi.fetchIcons(response -> clientThread.invokeLater(() ->
		{
			if (!config.syncEnabled())
			{
				return;
			}
			boolean iconsChanged = iconService.applyServerIcons(response.getIcons());
			boolean ranksChanged = iconService.applyRankIcons(response.getRankIcons());
			if (iconsChanged || ranksChanged)
			{
				// new icon names can now be given to players, and chat shows the new images
				rebuildCosmetics();
				client.refreshChat();
			}
		}));
	}

	private void clearServerState()
	{
		syncQueue.clear();
		reporter = null;
		serverLeaderboard = null;
		serverCosmetics = Collections.emptyList();
		serverCosmeticsLoaded = false;
		ranksReported = false;
		lastServerUpdate = 0;
		clientThread.invokeLater(() ->
		{
			if (iconService.clearServerIcons())
			{
				rebuildCosmetics();
				client.refreshChat();
			}
		});
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
		SyncModels.Leaderboard lb = serverLeaderboard;
		int rhino = iconService.indexFor(ClanIconService.KEY_STAFF);
		int gzIcon = iconService.indexFor(ClanIconService.KEY_GZ_KING);
		int crown = iconService.indexFor(ClanIconService.KEY_FOUNDER);

		ChatMessageBuilder banner = new ChatMessageBuilder();
		icon(banner, rhino);
		banner.append(ChatColorType.HIGHLIGHT).append("CoR Clan").append(ChatColorType.NORMAL)
			.append(lb != null ? " - clan-wide gz leaderboard " : " - " + stats.totalGiven() + " gz counted ");
		icon(banner, rhino);
		say(banner);

		List<Map.Entry<String, Integer>> givers = lb != null ? entries(lb.getGivers()) : GzStats.top(stats.getGiven(), 1);
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

		List<Map.Entry<String, Integer>> top = lb != null ? entries(lb.getReceivers()) : GzStats.top(stats.getReceived(), 3);
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
	 * Icons stack, left to right: member icons (built-in, clan server, config), the GZ King badge, then
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

	/**
	 * Safe from any thread: builds a new immutable snapshot and swaps it in. The built-in defaults only apply
	 * until the clan server's list has loaded; after that the admin page decides (it was seeded with them).
	 */
	private void rebuildCosmetics()
	{
		boolean fromServer = config.syncEnabled() && serverCosmeticsLoaded;
		cosmetics = MemberCosmetics.build(
			fromServer ? Collections.emptyMap() : BUILTIN_MEMBER_ICONS,
			fromServer ? Collections.emptyMap() : BUILTIN_MEMBER_TITLES,
			config.syncEnabled() ? serverCosmetics : Collections.emptyList(),
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

	private static List<Map.Entry<String, Integer>> entries(List<SyncModels.Entry> list)
	{
		List<Map.Entry<String, Integer>> out = new ArrayList<>(list.size());
		for (SyncModels.Entry e : list)
		{
			out.add(new AbstractMap.SimpleImmutableEntry<>(e.getRsn(), e.getCount()));
		}
		return out;
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

		SyncModels.Leaderboard lb = config.syncEnabled() ? serverLeaderboard : null;
		String syncStatus;
		if (!config.syncEnabled())
		{
			syncStatus = "Clan sync: off (turn on in settings)";
		}
		else if (lb == null)
		{
			syncStatus = "Clan sync: on, waiting for the clan server";
		}
		else
		{
			syncStatus = "Clan sync: on, updated " + new SimpleDateFormat("HH:mm").format(new Date(lastServerUpdate));
		}

		List<Map.Entry<String, Integer>> givers = lb != null
			? entries(lb.getGivers())
			: GzStats.top(allTime.getGiven(), PANEL_GIVERS_SIZE);
		List<Map.Entry<String, Integer>> receivers = lb != null
			? entries(lb.getReceivers())
			: GzStats.top(allTime.getReceived(), PANEL_LEADERBOARD_SIZE);
		List<BroadcastRecord> recent = new ArrayList<>(allTime.getRecent());

		// weekly and streaks are always this client's own counts; the clan server has no weekly data
		List<Map.Entry<String, Integer>> weeklyGivers = GzStats.top(tracker.getWeekly().getGiven(), PANEL_LEADERBOARD_SIZE);

		// every giver this client has counted; the clan server only sends its top few
		List<Map.Entry<String, Integer>> allGivers = GzStats.top(allTime.getGiven(), Integer.MAX_VALUE);

		return new PanelData(mine, summary, syncStatus, lb != null, givers, receivers, recent,
			weeklyGivers, tracker.getWeekStart(), tracker.longestStreak(), tracker.currentStreak(), allGivers);
	}
}

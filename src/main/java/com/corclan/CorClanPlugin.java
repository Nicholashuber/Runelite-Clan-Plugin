package com.corclan;

import com.corclan.chat.RankCommands;
import com.corclan.clan.ClanLeaders;
import com.corclan.clan.InfernoReminder;
import com.corclan.clan.Legion;
import com.corclan.clan.Promotions;
import com.corclan.raid.RaidAd;
import com.corclan.raid.RaidLines;
import com.corclan.raid.RaidListing;
import com.corclan.fun.RaidSounds;
import com.corclan.clan.ClanRoster;
import com.corclan.glow.DaylightAura;
import com.corclan.glow.DevGlow;
import com.corclan.glow.FounderGlow;
import com.corclan.glow.GemGlow;
import com.corclan.glow.GlowEffect;
import com.corclan.glow.GlowPicks;
import com.corclan.glow.HolyAura;
import com.corclan.glow.LavaAura;
import com.corclan.glow.NatureAura;
import com.corclan.glow.NatureGlow;
import com.corclan.glow.RankGlowOverlay;
import com.corclan.glow.ShameAura;
import com.corclan.glow.Shames;
import com.corclan.glow.SignatureGlowOverlay;
import com.corclan.gz.BroadcastParser;
import com.corclan.gz.DailyGm;
import com.corclan.gz.BroadcastRecord;
import com.corclan.gz.GzStats;
import com.corclan.gz.GzTracker;
import com.corclan.gz.Streaks;
import com.corclan.gz.WeekResult;
import com.corclan.icons.ClanIconService;
import com.corclan.icons.MemberCosmetics;
import com.corclan.icons.WeeklyTrophies;
import com.corclan.map.ClanMapPoints;
import com.corclan.map.LocationRules;
import com.corclan.map.MapState;
import com.corclan.party.CorLocation;
import com.corclan.sync.ClanApi;
import com.corclan.sync.ClanSnapshot;
import com.corclan.sync.GzView;
import com.corclan.sync.SyncModels;
import com.corclan.sync.SyncQueue;
import com.corclan.ui.CorClanOverlay;
import com.corclan.ui.CorClanPanel;
import com.corclan.ui.RaidFinderCard;
import com.corclan.ui.PanelData;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MessageNode;
import net.runelite.api.Player;
import net.runelite.api.SoundEffectID;
import java.util.TreeMap;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.ClanMemberJoined;
import net.runelite.api.events.ClanMemberLeft;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserJoin;
import net.runelite.client.party.events.UserPart;
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
	description = "Clan sidebar, custom clan chat icons and a gz tracker for the C o R clan, with opt-in clan-wide sync and a clan map.",
	tags = {"clan", "cor", "gz", "chat", "icons", "social", "sync", "party", "map"}
)
public class CorClanPlugin extends Plugin
{
	static final String STATS_KEY = "gzStats";
	static final String WEEKLY_KEY = "gzWeekly";
	static final String WEEK_START_KEY = "gzWeekStart";
	static final String WEEK_RESULTS_KEY = "gzWeekResults";
	/** recent gz times per player, so the hourly cap survives a restart */
	static final String RECENT_GZ_KEY = "gzRecent";
	/** JoeBoob's Infernal cape reminder count: on his own client, and on Ray's as he sees him log in ("date|count") */
	static final String INFERNO_SELF_KEY = "infernoReminderSelf";
	static final String INFERNO_SEEN_KEY = "infernoReminderSeen";
	private static final Type RECENT_GZ_TYPE = new TypeToken<Map<String, List<Long>>>(){}.getType();
	private static final Type WEEK_RESULTS_TYPE = new TypeToken<List<WeekResult>>(){}.getType();
	/** Saved stats, not settings: changing them must not trigger a config refresh. */
	private static final List<String> STATS_KEYS = Arrays.asList(STATS_KEY, WEEKLY_KEY, WEEK_START_KEY, WEEK_RESULTS_KEY,
		RECENT_GZ_KEY, INFERNO_SELF_KEY, INFERNO_SEEN_KEY);
	/** Saved by the build that shared gz counts and staff lists through the party; removed on start. */
	private static final List<String> RETIRED_KEYS = Arrays.asList("partyGz", "clanSettingsUpdatedAt");
	private static final String CHAT_BUILD_CALLBACK = "chatMessageBuilding";
	/** Position of the name string relative to the top of the object stack in that callback. */
	private static final int NAME_STACK_OFFSET = 3;
	/** Placeholder default from the first build; replaced by the real invite once seen. */
	private static final String OLD_DISCORD_PLACEHOLDER = "https://discord.gg/";
	private static final Pattern IMG_TAG = Pattern.compile("<img=\\d+>");
	private static final int PANEL_LEADERBOARD_SIZE = 5;
	private static final int PANEL_GIVERS_SIZE = 10;

	// clan sync
	/** The server refuses events older than 10 minutes; ours are dropped a little before that. */
	private static final long MAX_EVENT_AGE_MILLIS = 540_000L;
	/** Our rank and glow picks go to the server at most this often, however fast they change. */
	private static final long PROFILE_MIN_MILLIS = 15_000L;
	/** After a profile send failed, wait this long before trying again. */
	private static final long PROFILE_RETRY_MILLIS = 60_000L;
	/** Logging in loads the clan data right away, unless it was loaded this recently (world hops log in too). */
	private static final long PULL_MIN_MILLIS = 60_000L;
	/** joins this soon after the clan channel loads are it filling in, not people logging in */
	private static final long CHANNEL_SETTLE_MILLIS = 10_000L;
	/** how long "A Spartan has landed!" stays over his head, in client cycles (20 ms) */
	private static final int SPARTAN_OVERHEAD_CYCLES = 250;

	// CoR party
	/** Position updates at most every 5 game ticks (~3s), slower in big parties to spare RuneLite's party server. */
	private static final int LOCATION_TICKS = 5;
	/** Resend an unchanged position this often so others know we are still here. */
	private static final long LOCATION_HEARTBEAT_MILLIS = 30_000L;
	/** A clanmate who sent nothing for this long drops off the map. */
	private static final long LOCATION_EXPIRY_MILLIS = 90_000L;

	/** Defaults that apply without any config. The clan server and the "Member icons" config box override these. */
	private static final Map<String, List<String>> BUILTIN_MEMBER_ICONS = Collections.singletonMap(
		"lavasockz", Collections.unmodifiableList(Arrays.asList(ClanIconService.KEY_FOUNDER, ClanIconService.KEY_DEV)));
	/** No built-in titles: Lavasockz's icons already say founder and dev. */
	private static final Map<String, String> BUILTIN_MEMBER_TITLES = Collections.emptyMap();

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
	private RankGlowOverlay rankGlowOverlay;

	@Inject
	private HolyAura holyAura;

	@Inject
	private GlowPicks glowPicks;

	@Inject
	private LavaAura lavaAura;

	@Inject
	private DaylightAura daylightAura;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private NatureAura natureAura;

	@Inject
	private ShameAura shameAura;

	@Inject
	private SignatureGlowOverlay signatureGlowOverlay;

	@Inject
	private RankCommands rankCommands;

	@Inject
	private ItemManager itemManager;

	@Inject
	private PartyService partyService;

	@Inject
	private WSClient wsClient;

	@Inject
	private ClanMapPoints mapPoints;

	@Inject
	private ClanApi clanApi;

	private final GzTracker tracker = new GzTracker();
	/** our own gz's and broadcasts waiting to go to the clan server */
	private final SyncQueue syncQueue = new SyncQueue();

	/** Rebuilt whenever config or server data changes; read by chat rendering on the client thread. */
	private volatile MemberCosmetics cosmetics = MemberCosmetics.EMPTY;
	/** icons and titles per in-game rank title, from the local "Clan rank icons" list */
	private volatile MemberCosmetics rankCosmetics = MemberCosmetics.EMPTY;
	/** this week's top 3 givers -> trophy icon; replaced whole when the weekly counts change */
	private volatile Map<String, String> weeklyTrophies = Collections.emptyMap();

	/** gz counts shown everywhere: the clan server's with clan sync on, otherwise this client's (client thread) */
	private GzView view = GzView.choose(null, 0L, 0L, tracker.getAllTime(), tracker.getWeekly(), tracker.getWeekResults(), 0L);
	private volatile String gzKing;

	// clan sync state
	/** false once the plugin stopped, so late answers from the server are dropped */
	private volatile boolean started;
	/** the character the queued events belong to; they are only ever sent under this name */
	private volatile SyncModels.Reporter reporter;
	/** the last answer to GET /v1/clan, null until one arrived (or clan sync is off) */
	private volatile ClanSnapshot serverClan;
	private volatile long serverClanAt;
	/** once the server's icons have loaded, its pick per clan rank replaces the local "Clan rank icons" list */
	private volatile boolean serverIconsLoaded;
	/** the character the server refuses (name taken, or not in CoR): nothing more is sent for it this login */
	private volatile SyncModels.Reporter refusedReporter;
	private volatile ClanApi.Outcome syncRefusal;
	private final AtomicBoolean reportInFlight = new AtomicBoolean();
	// the rest of the sync state lives on the client thread
	/** rank names are sent once per login */
	private boolean ranksReported;
	/** the rank and glow picks the server last took from us (reporter, rank, glow ids); null = send them */
	private List<Object> sentProfile;
	private boolean profileInFlight;
	private long nextProfileAt;
	private long lastPull;
	/** who was already told in chat that their name is taken, so it is said once */
	private SyncModels.Reporter nameTakenTold;

	// CoR party state, client thread only
	/** the side panel currently shows the Owner glow section */
	private boolean panelShowsOwner;
	/** the side panel currently shows the Dev glow section (you are Lavasockz) */
	private boolean panelShowsDev;
	/** the side panel currently shows the Founder glow section (you are DAYLlGHT) */
	private boolean panelShowsFounder;
	/** the side panel currently shows the Nature glow section (you are JoeBoob) */
	private boolean panelShowsNature;
	/** the side panel currently offers the announcement's Edit button (you are a clan leader with sync on) */
	private boolean panelShowsLeader;
	/** the UTC day our daily gm was last answered by the clan server; it is sent at most once a day. Client thread. */
	private long gmAnsweredDay = -1;
	/** a gm is on its way to the clan server */
	private boolean gmInFlight;
	/** our own raid finder group is sent again this often, so it stays up while we play (the server drops it after 15 min) */
	private static final long RAID_RESEND_MILLIS = 5 * 60_000L;
	private static final String RAID_ALERT_COLOR = "4fb06e";
	/** every open raid finder group, newest first; swapped whole (read by the panel snapshot) */
	private volatile List<RaidListing> raidBoard = Collections.emptyList();
	/** our own raid finder group as last posted, and who posted it; null when we have none. Client thread. */
	private RaidAd myRaid;
	private SyncModels.Reporter myRaidReporter;
	private long raidSentAt;
	/** when we last took our group down; the board may still list it for a moment, so it is not taken back */
	private long raidStoppedAt;
	private static final long RAID_ADOPT_PAUSE_MILLIS = 90_000L;
	private int raidSentWorld;
	/** our group was last sent as raiding (we were inside the raid) */
	private boolean raidSentRaiding;
	/** groups already seen (so each new one is announced once), and whether this login has looked at the board yet */
	private final Set<String> seenRaids = new HashSet<>();
	/** groups seen raiding on their current trip, so each trip is announced once */
	private final Set<String> raidingSeen = new HashSet<>();
	private boolean raidBoardSeen;
	/** the open groups are advertised in chat (as game messages) this often */
	private static final long RAID_AD_MILLIS = 5 * 60_000L;
	/**
	 * The promotion assistant reads the whole clan roster, so it is worked out at most this often (or when your rank
	 * or clan changes), not on every panel refresh. Ranks, join dates and gz's move slowly; a minute behind is fine.
	 */
	private static final long PROMOTIONS_MILLIS = 60_000L;
	private Promotions.Result promotionsCache;
	private ClanRank promotionsRank;
	private long promotionsAt;
	/** our latest raid finder game message, and when it was shown; null when there is none */
	private MessageNode raidLine;
	private long raidLineAt;
	/** a clan chat line or another game message came in since our raid line: the next one is a new line */
	private boolean chatSinceRaidLine;
	/** we are adding our own raid line: our own chat handler must not read it as a clan broadcast */
	private boolean addingRaidLine;
	/** leaders' login alerts: tells a real login from a world hop */
	private final Legion legion = new Legion();
	/** when the clan channel last (re)loaded; joins right after are the channel filling in, not logins */
	private long clanChannelAt;
	/** the last game state, to tell a login (from the login screen) from a world hop */
	private GameState lastGameState;
	/** we just logged in from the login screen; announce ourselves once our name is known */
	private boolean ownLoginPending;
	/** the clan rank title on the panel's rank card, to redraw it when your rank loads or changes */
	private String panelShowsRank;
	private int locationTick;
	/** what the clan map is doing; announced in chat when it changes */
	private MapState mapState = MapState.OFF;
	/** clanmates on the map at the last redraw, so the panel only refreshes when it changes */
	private int mapCount;
	private CorLocation selfLocation;
	private long lastLocationSent;
	private final Map<Long, CorLocation> partyLocations = new HashMap<>();
	private final Map<Long, String> partyNames = new HashMap<>();
	private final Map<Long, Long> partySeen = new HashMap<>();

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
		tracker.loadRecentGz(loadRecentGz(), System.currentTimeMillis());
		if (tracker.rollWeek(System.currentTimeMillis()))
		{
			persistStats();
		}
		for (String key : RETIRED_KEYS)
		{
			configManager.unsetConfiguration(CorClanConfig.GROUP, key);
		}
		started = true;
		clientThread.invokeLater(() ->
		{
			updateGzView();
			if (config.syncEnabled() && client.getGameState() == GameState.LOGGED_IN)
			{
				pullFromServer();
			}
		});
		rebuildCosmetics();
		iconService.ensureRegistered();
		// the party only carries the clan map
		wsClient.registerMessage(CorLocation.class);

		panel = new CorClanPanel(config, itemManager, this::resetStats,
			(glow, on) -> configManager.setConfiguration(CorClanConfig.GROUP, glow.configKey, on),
			(glow, on) -> configManager.setConfiguration(CorClanConfig.GROUP, glow.configKey, on),
			(glow, on) -> configManager.setConfiguration(CorClanConfig.GROUP, glow.configKey, on),
			this::toggleCorParty,
			this::editAnnouncement,
			(key, on) -> configManager.setConfiguration(CorClanConfig.GROUP, key, on),
			new RaidFinderCard.Actions()
			{
				@Override
				public void post(RaidAd ad)
				{
					postRaid(ad);
				}

				@Override
				public void stop()
				{
					stopRaid("CoR: your raid group is taken down");
				}

				@Override
				public void ping()
				{
					pingRaid();
				}
			});
		navButton = NavigationButton.builder()
			.tooltip("CoR Clan")
			.icon(ImageUtil.loadImageResource(CorClanPlugin.class, "panel_icon.png"))
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(overlay);
		overlayManager.add(rankGlowOverlay);
		overlayManager.add(signatureGlowOverlay);
		rankCommands.startUp(name -> view.givenOf(name), () -> rankCosmetics);
		// the CoR party is only ever joined from the panel button: never automatically
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
		started = false;
		clearServerState();
		stopSharingLocation();
		if (inCorParty())
		{
			partyService.changeParty(null);
		}
		wsClient.unregisterMessage(CorLocation.class);
		clearPartyLocations();
		overlayManager.remove(overlay);
		overlayManager.remove(rankGlowOverlay);
		overlayManager.remove(signatureGlowOverlay);
		rankCommands.shutDown();
		clientThread.invoke(holyAura::clear);
		clientThread.invoke(lavaAura::clear);
		clientThread.invoke(daylightAura::clear);
		clientThread.invoke(natureAura::clear);
		clientThread.invoke(shameAura::clearShames);
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
		if (!CorClanConfig.GROUP.equals(event.getGroup()) || STATS_KEYS.contains(event.getKey())
			|| RETIRED_KEYS.contains(event.getKey()))
		{
			return;
		}
		String key = event.getKey();
		if ("syncEnabled".equals(key))
		{
			// off: forget everything from the server and stop all traffic. On: start clean and load right away
			clearServerState();
			if (config.syncEnabled())
			{
				clientThread.invokeLater(() ->
				{
					if (client.getGameState() == GameState.LOGGED_IN)
					{
						pullFromServer();
					}
				});
			}
		}
		if ("shareLocation".equals(key) || "shareInWilderness".equals(key))
		{
			clientThread.invokeLater(() ->
			{
				stopSharingLocation();
				if (!config.shareLocation())
				{
					setMapState(MapState.OFF);
				}
			});
		}
		if ("rankGlow".equals(event.getKey()) && !config.rankGlow())
		{
			clientThread.invoke(lavaAura::clear);
			clientThread.invoke(daylightAura::clear);
			clientThread.invoke(natureAura::clear);
			clientThread.invoke(holyAura::clear);
			clientThread.invoke(shameAura::clear);
		}
		rebuildCosmetics();
		clientThread.invokeLater(client::refreshChat);
		refreshPanel();
	}

	// ---------------------------------------------------------------- gz tracking

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType type = event.getType();
		if (addingRaidLine)
		{
			return;
		}
		if (type == ChatMessageType.CLAN_CHAT || type == ChatMessageType.CLAN_MESSAGE || type == ChatMessageType.GAMEMESSAGE)
		{
			chatSinceRaidLine = true;
		}
		long now = System.currentTimeMillis();
		// the daily gm bonus works whether or not the gz tracker is on; only our own line is ever sent
		if (type == ChatMessageType.CLAN_CHAT && isMe(displayName(event.getName())))
		{
			String said = Text.removeTags(event.getMessage());
			if (DailyGm.isGm(said))
			{
				claimDailyGm(said, now);
			}
		}
		if (!config.gzTrackingEnabled())
		{
			return;
		}

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
			updateGzView();
			refreshPanel();
			// only a broadcast about our own character is reported, never one about someone else
			if (type == ChatMessageType.CLAN_MESSAGE && isMe(subject))
			{
				queueSync(SyncModels.Event.broadcast(text, now));
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
				updateGzView();
				refreshPanel();
				tagGzCount(event.getMessageNode(), sender);
				announce(sender, tracker.getLastGzSubject());
				// only a gz our own character said is reported, never one somebody else said
				if (type == ChatMessageType.CLAN_CHAT && isMe(sender))
				{
					queueSync(SyncModels.Event.gz(message, now));
				}
			}
		}
	}

	/** Appends "[GZ count: N]" to the chat line on this client, N being the sender's all-time gz total (the clan server's with clan sync on). */
	private void tagGzCount(MessageNode node, String sender)
	{
		if (!config.showGzCount() || node == null)
		{
			return;
		}
		int count = view.givenOf(sender);
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
				+ " (" + view.receivedOf(subject) + " total)";
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

	/** The top gz giver: clan-wide with clan sync on, otherwise as counted on this client. */
	private String gzKing()
	{
		return gzKing;
	}

	// ---------------------------------------------------------------- ::cor / ::test / Owner storm effects

	/**
	 * ::glowzap, ::glowshock and ::glowstrike &lt;id&gt; play a game graphic (spot anim
	 * id) on your own character and use it for that layer of the Owner's storm this session, to try
	 * out effects. Local only, like every :: command.
	 */
	private void previewGlowFx(HolyAura.Effect effect, String[] args)
	{
		int id;
		try
		{
			id = Integer.parseInt(args.length > 0 ? args[0] : "");
		}
		catch (NumberFormatException e)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				"Usage: ::" + effect.command + " <spot anim id>, e.g. ::" + effect.command + " " + effect.defaultSpotAnimId, null);
			return;
		}
		holyAura.preview(effect, id);
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
			"CoR: Owner " + effect.label + " set to " + id + " for this session", null);
	}

	/**
	 * ::myglow lists the Owner's effects; ::myglow &lt;name&gt; [on|off] switches one (toggles without
	 * on/off). Their settings are hidden so the rest of the clan never sees them; for anyone but the
	 * clan Owner the command does nothing.
	 */
	private void myGlow(String[] args)
	{
		ClanRank rank = glowPicks.localRank();
		if (!ClanRank.OWNER.equals(rank))
		{
			return;
		}
		if (args.length == 0)
		{
			StringBuilder list = new StringBuilder("CoR: your glow -");
			for (GlowEffect glow : GlowEffect.values())
			{
				if (glow.unlockedBy(rank))
				{
					list.append(' ').append(glow.shortName).append(GlowPicks.picked(config, glow) ? " on," : " off,");
				}
			}
			list.setLength(list.length() - 1);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", list + ". ::myglow <name> [on|off]", null);
			return;
		}
		GlowEffect glow = GlowEffect.byShortName(args[0]);
		String state = args.length > 1 ? args[1].toLowerCase() : "";
		if (glow == null || !glow.unlockedBy(rank) || !(state.isEmpty() || state.equals("on") || state.equals("off")))
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Usage: ::myglow <name> [on|off]. ::myglow lists the names", null);
			return;
		}
		boolean on = state.isEmpty() ? !GlowPicks.picked(config, glow) : state.equals("on");
		configManager.setConfiguration(CorClanConfig.GROUP, glow.configKey, on);
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CoR: " + glow.shortName + (on ? " on" : " off"), null);
	}

	/** True when you are logged in as a wearer of the Molten Lord aura (Lavasockz). Client thread. */
	private boolean isDev()
	{
		String me = localPlayerName();
		return me != null && LavaAura.WEARERS.contains(MemberCosmetics.key(me));
	}

	/**
	 * ::devglow lists Lavasockz's aura parts; ::devglow &lt;name&gt; [on|off] switches one (toggles without
	 * on/off). The settings are hidden, and for anyone else the command does nothing.
	 */
	private void devGlow(String[] args)
	{
		if (!isDev())
		{
			return;
		}
		if (args.length == 0)
		{
			StringBuilder list = new StringBuilder("CoR: your dev glow -");
			for (DevGlow glow : DevGlow.values())
			{
				list.append(' ').append(glow.shortName).append(DevGlow.picked(config, glow) ? " on," : " off,");
			}
			list.setLength(list.length() - 1);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", list + ". ::devglow <name> [on|off]", null);
			return;
		}
		DevGlow glow = DevGlow.byShortName(args[0]);
		String state = args.length > 1 ? args[1].toLowerCase() : "";
		if (glow == null || !(state.isEmpty() || state.equals("on") || state.equals("off")))
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Usage: ::devglow <name> [on|off]. ::devglow lists the names", null);
			return;
		}
		boolean on = state.isEmpty() ? !DevGlow.picked(config, glow) : state.equals("on");
		configManager.setConfiguration(CorClanConfig.GROUP, glow.configKey, on);
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CoR: " + glow.shortName + (on ? " on" : " off"), null);
	}

	/**
	 * ::shame &lt;name&gt; drops the Kephri dung on a clanmate's head for five minutes, for every plugin user with clan
	 * sync on; ::unshame &lt;name&gt; takes it off. Only the shamers can, and the clan server checks it is really them.
	 * For anyone else the command does nothing. Client thread.
	 */
	private void shame(String[] args, boolean clear)
	{
		String me = localPlayerName();
		if (!ClanLeaders.isLeader(me))
		{
			return;
		}
		String command = clear ? "::unshame" : "::shame";
		String target = displayName(String.join(" ", args));
		if (target.isEmpty())
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Usage: " + command + " <player name>", null);
			return;
		}
		if (!config.syncEnabled())
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CoR: " + command + " needs Sync with clan server turned on", null);
			return;
		}
		if (clanRankOf(target) == null)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CoR: " + target + " is not in the clan", null);
			return;
		}
		SyncModels.Reporter r = currentReporter();
		if (r == null || r.equals(refusedReporter))
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CoR: the clan server isn't taking anything from you right now", null);
			return;
		}
		clanApi.sendShame(new SyncModels.ShamePayload(r, target, clear), outcome -> clientThread.invokeLater(() ->
		{
			if (!started || !config.syncEnabled())
			{
				return;
			}
			String done;
			if (outcome == ClanApi.Outcome.OK)
			{
				// show it right away; the next refresh confirms it for everyone else
				long now = System.currentTimeMillis();
				shameAura.setShames(clear ? shameAura.getShames().without(target) : shameAura.getShames().with(target, now));
				if (clear)
				{
					shameAura.clear();
				}
				done = clear ? "CoR: " + target + " is unshamed" : "CoR: " + target + " has been shamed";
			}
			else if (outcome == ClanApi.Outcome.RETRY)
			{
				done = "CoR: the clan server isn't answering, try " + command + " again";
			}
			else
			{
				done = "CoR: the clan server refused " + command;
				if (outcome.blocksReporter())
				{
					onRefused(r, outcome);
				}
			}
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", done, null);
		}));
	}

	/** True when you are logged in as a wearer of the founder aura (DAYLlGHT). Client thread. */
	private boolean isFounder()
	{
		String me = localPlayerName();
		return me != null && DaylightAura.WEARERS.contains(MemberCosmetics.key(me));
	}

	/**
	 * ::founderglow lists DAYLlGHT's aura parts; ::founderglow &lt;name&gt; [on|off] switches one (toggles
	 * without on/off). The settings are hidden, and for anyone else the command does nothing.
	 */
	private void founderGlow(String[] args)
	{
		if (!isFounder())
		{
			return;
		}
		if (args.length == 0)
		{
			StringBuilder list = new StringBuilder("CoR: your founder glow -");
			for (FounderGlow glow : FounderGlow.values())
			{
				list.append(' ').append(glow.shortName).append(FounderGlow.picked(config, glow) ? " on," : " off,");
			}
			list.setLength(list.length() - 1);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", list + ". ::founderglow <name> [on|off]", null);
			return;
		}
		FounderGlow glow = FounderGlow.byShortName(args[0]);
		String state = args.length > 1 ? args[1].toLowerCase() : "";
		if (glow == null || !(state.isEmpty() || state.equals("on") || state.equals("off")))
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Usage: ::founderglow <name> [on|off]. ::founderglow lists the names", null);
			return;
		}
		boolean on = state.isEmpty() ? !FounderGlow.picked(config, glow) : state.equals("on");
		configManager.setConfiguration(CorClanConfig.GROUP, glow.configKey, on);
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CoR: " + glow.shortName + (on ? " on" : " off"), null);
	}

	/** True when you are logged in as a wearer of the nature aura (JoeBoob). Client thread. */
	private boolean isNature()
	{
		String me = localPlayerName();
		return me != null && NatureAura.WEARERS.contains(MemberCosmetics.key(me));
	}

	/**
	 * ::natureglow lists JoeBoob's aura parts; ::natureglow &lt;name&gt; [on|off] switches one (toggles
	 * without on/off). The settings are hidden, and for anyone else the command does nothing.
	 */
	private void natureGlow(String[] args)
	{
		if (!isNature())
		{
			return;
		}
		if (args.length == 0)
		{
			StringBuilder list = new StringBuilder("CoR: your nature glow -");
			for (NatureGlow glow : NatureGlow.values())
			{
				list.append(' ').append(glow.shortName).append(NatureGlow.picked(config, glow) ? " on," : " off,");
			}
			list.setLength(list.length() - 1);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", list + ". ::natureglow <name> [on|off]", null);
			return;
		}
		NatureGlow glow = NatureGlow.byShortName(args[0]);
		String state = args.length > 1 ? args[1].toLowerCase() : "";
		if (glow == null || !(state.isEmpty() || state.equals("on") || state.equals("off")))
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Usage: ::natureglow <name> [on|off]. ::natureglow lists the names", null);
			return;
		}
		boolean on = state.isEmpty() ? !NatureGlow.picked(config, glow) : state.equals("on");
		configManager.setConfiguration(CorClanConfig.GROUP, glow.configKey, on);
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CoR: " + glow.shortName + (on ? " on" : " off"), null);
	}

	/**
	 * ::naturefx ring|leaves|swirl|bloom &lt;spot anim id&gt; [rise or height] switches a part of JoeBoob's aura to another
	 * game graphic this session, like ::dayfx. Local only.
	 */
	private void previewNatureFx(String[] args)
	{
		try
		{
			int id = Integer.parseInt(args.length > 1 ? args[1] : "");
			boolean swirl = NatureGlow.LEAF_SWIRL.shortName.equalsIgnoreCase(args[0]);
			int extra = args.length > 2 ? Integer.parseInt(args[2]) : swirl ? DaylightAura.DEFAULT_SWIRL_RISE : 0;
			String done = natureAura.preview(args[0], id, extra);
			if (done != null)
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", done, null);
				return;
			}
		}
		catch (NumberFormatException e)
		{
			// fall through to the usage line
		}
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
			"Usage: ::naturefx ring|leaves|swirl|bloom <spot anim id> [swirl rise / bloom height], e.g. ::naturefx ring 2540", null);
	}

	/**
	 * ::dayfx flames|swirl|smoke &lt;spot anim id&gt; [rise or height] switches a part of DAYLlGHT's aura to
	 * another game graphic this session, to try out effects. Local only, like every :: command.
	 */
	private void previewDaylightFx(String[] args)
	{
		try
		{
			int id = Integer.parseInt(args.length > 1 ? args[1] : "");
			// the swirl climbs this far each loop unless told otherwise; the smoke plays at the feet
			boolean swirl = FounderGlow.BLACK_SWIRL.shortName.equalsIgnoreCase(args[0]);
			int extra = args.length > 2 ? Integer.parseInt(args[2]) : swirl ? DaylightAura.DEFAULT_SWIRL_RISE : 0;
			String done = daylightAura.preview(args[0], id, extra);
			if (done != null)
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", done, null);
				return;
			}
		}
		catch (NumberFormatException e)
		{
			// fall through to the usage line
		}
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
			"Usage: ::dayfx flames|swirl|smoke <spot anim id> [swirl rise / smoke height], e.g. ::dayfx swirl 1296 250", null);
	}

	/**
	 * Double-colon commands are handled inside the client and never sent to the game server. This one
	 * prints a local CoR banner with the gz leaderboard. Only the player who typed it sees it.
	 * Colours come from RuneLite's chat colour types so they adapt to the opaque / transparent chatbox.
	 */
	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		String cmd = event.getCommand().toLowerCase();
		HolyAura.Effect effect = holyAura.forCommand(cmd);
		if (effect != null)
		{
			previewGlowFx(effect, event.getArguments());
			return;
		}
		if (cmd.equals("lavafx"))
		{
			// local preview of Lavasockz's aura on your own character
			lavaAura.preview();
			return;
		}
		if (cmd.equals("devglow"))
		{
			devGlow(event.getArguments());
			return;
		}
		if (cmd.equals("dayfx"))
		{
			previewDaylightFx(event.getArguments());
			return;
		}
		if (cmd.equals("naturefx"))
		{
			previewNatureFx(event.getArguments());
			return;
		}
		if (cmd.equals("natureglow"))
		{
			natureGlow(event.getArguments());
			return;
		}
		if (cmd.equals("founderglow"))
		{
			founderGlow(event.getArguments());
			return;
		}
		if (cmd.equals("myglow"))
		{
			myGlow(event.getArguments());
			return;
		}
		if (cmd.equals("dungfx"))
		{
			// local preview of the Kephri dung on your own character, dropping like a real shame for 30 seconds
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", shameAura.preview()
				? "CoR: dropping the Kephri dung on you for 30 seconds, like a shame. ::dungfx again stops it"
				: "CoR: dung preview stopped", null);
			return;
		}
		if (cmd.equals("shame") || cmd.equals("unshame"))
		{
			shame(event.getArguments(), cmd.equals("unshame"));
			return;
		}
		if (glowPicks.gemPartCommand(cmd, event.getArguments()))
		{
			return;
		}
		if (!cmd.equals("cor") && !cmd.equals("test"))
		{
			return;
		}
		GzStats stats = tracker.getAllTime();
		String scope = view.clanWide ? "clan" : "this client";
		int rhino = iconService.indexFor(ClanIconService.KEY_STAFF);
		int gzIcon = iconService.indexFor(ClanIconService.KEY_GZ_KING);
		int crown = iconService.indexFor(ClanIconService.KEY_FOUNDER);

		ChatMessageBuilder banner = new ChatMessageBuilder();
		icon(banner, rhino);
		banner.append(ChatColorType.HIGHLIGHT).append("CoR Clan").append(ChatColorType.NORMAL)
			.append(" - gz leaderboard (" + scope + ") ");
		icon(banner, rhino);
		say(banner);

		List<Map.Entry<String, Integer>> givers = GzStats.top(view.given, 1);
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

		List<Map.Entry<String, Integer>> top = GzStats.top(view.received, 3);
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
		ClanChannel channel = type == ChatMessageType.CLAN_CHAT ? client.getClanChannel() : client.getGuestClanChannel();
		ClanChannelMember member = channel == null || key.isEmpty() ? null : findMember(channel, key);
		// rank icons only apply in our own clan's chat, where the rank titles are ours
		String rankKey = type == ChatMessageType.CLAN_CHAT ? rankTitleKey(member) : null;
		MemberCosmetics current = cosmetics;
		String tags = iconTagsFor(key, member, rankKey, current);
		String title = current.titleFor(key);
		if (title == null && rankKey != null)
		{
			title = rankCosmetics.titleFor(rankKey);
		}
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
	 * Icons stack, left to right: member icons (built-in, clan server or local clan list, own config; or else the
	 * icons for their clan rank), the GZ King badge, this week's trophy, then the rank icon. Returns "" when there
	 * is nothing to show.
	 */
	private String iconTagsFor(String key, ClanChannelMember member, String rankKey, MemberCosmetics current)
	{
		if (key.isEmpty())
		{
			return "";
		}
		StringBuilder sb = new StringBuilder();

		List<String> icons = current.iconsFor(key);
		if (icons.isEmpty() && rankKey != null)
		{
			icons = rankCosmetics.iconsFor(rankKey);
		}
		for (String icon : icons)
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

		if (config.weeklyTrophies())
		{
			append(sb, iconService.tagFor(weeklyTrophies.get(key)));
		}

		if (config.replaceRankIcons() && member != null)
		{
			append(sb, iconService.tagFor(iconService.iconForRank(member.getRank())));
		}
		return sb.toString();
	}

	/** Lookup key of a clan chat member's in-game rank title, e.g. "gnome child", or null. Client thread. */
	private String rankTitleKey(ClanChannelMember member)
	{
		ClanSettings settings = client.getClanSettings();
		if (member == null || member.getRank() == null || settings == null)
		{
			return null;
		}
		ClanTitle title = settings.titleForRank(member.getRank());
		return title == null || title.getName() == null ? null : MemberCosmetics.key(title.getName());
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
	 * Safe from any thread: builds new immutable snapshots and swaps them in. Layers, later wins per player:
	 * built-in defaults, the local "Clan member icons" list, then your own "Member icons". Once the clan server's
	 * list has loaded it replaces the first two (the admin page decides; it was seeded with the built-ins), and
	 * its icon per clan rank replaces the local "Clan rank icons" list.
	 */
	private void rebuildCosmetics()
	{
		ClanSnapshot server = config.syncEnabled() ? serverClan : null;
		boolean fromServer = server != null;
		cosmetics = MemberCosmetics.build(
			fromServer ? Collections.emptyMap() : BUILTIN_MEMBER_ICONS,
			fromServer ? Collections.emptyMap() : BUILTIN_MEMBER_TITLES,
			fromServer ? server.getPlayers() : Collections.emptyList(),
			(fromServer ? "" : config.clanMemberIcons() + "\n") + config.memberIcons(),
			iconService::isMemberKey);
		rankCosmetics = config.syncEnabled() && serverIconsLoaded
			? MemberCosmetics.EMPTY
			: MemberCosmetics.build(
				Collections.emptyMap(),
				Collections.emptyMap(),
				Collections.emptyList(),
				config.clanRankIcons(),
				iconService::isMemberKey);
	}

	/**
	 * Client thread: recomputes the counts shown everywhere (the clan server's with clan sync on and loaded,
	 * otherwise this client's), the GZ King and the weekly trophies, and redraws chat if a badge moved.
	 */
	private void updateGzView()
	{
		view = GzView.choose(config.syncEnabled() ? serverClan : null, serverClanAt, System.currentTimeMillis(),
			tracker.getAllTime(), tracker.getWeekly(), tracker.getWeekResults(), tracker.getWeekStart());

		List<Map.Entry<String, Integer>> top = GzStats.top(view.given, 1);
		String king = top.isEmpty() ? null : top.get(0).getKey();
		GzStats weekly = new GzStats();
		weekly.getGiven().putAll(view.weekly);
		Map<String, String> trophies = WeeklyTrophies.of(weekly);
		if (!trophies.equals(weeklyTrophies) || !Objects.equals(king, gzKing))
		{
			weeklyTrophies = trophies;
			gzKing = king;
			client.refreshChat();
		}
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
		configManager.setConfiguration(CorClanConfig.GROUP, RECENT_GZ_KEY,
			gson.toJson(tracker.getRecentGz(System.currentTimeMillis()), RECENT_GZ_TYPE));
	}

	private Map<String, List<Long>> loadRecentGz()
	{
		String json = configManager.getConfiguration(CorClanConfig.GROUP, RECENT_GZ_KEY);
		if (json == null || json.isEmpty())
		{
			return Collections.emptyMap();
		}
		try
		{
			Map<String, List<Long>> saved = gson.fromJson(json, RECENT_GZ_TYPE);
			return saved != null ? saved : Collections.emptyMap();
		}
		catch (JsonSyntaxException ex)
		{
			log.debug("Discarding unreadable recent gz times", ex);
			return Collections.emptyMap();
		}
	}

	private void resetStats()
	{
		clientThread.invokeLater(() ->
		{
			tracker.resetAllTime();
			tracker.resetSession();
			persistStats();
			updateGzView();
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
			updateGzView();
		}
		GzStats allTime = tracker.getAllTime();
		GzStats session = tracker.getSession();
		String me = localPlayerName();

		String mine = me == null
			? "Log in to see your own counts"
			: String.format("You: gave %d (%d today), got %d (%d today)",
				view.givenOf(me), session.getGiven().getOrDefault(me, 0),
				view.receivedOf(me), session.getReceived().getOrDefault(me, 0));
		String summary = String.format("This client: %d given, %d received all time",
			allTime.totalGiven(), allTime.totalReceived());

		List<Map.Entry<String, Integer>> givers = GzStats.top(view.given, PANEL_GIVERS_SIZE);
		List<Map.Entry<String, Integer>> receivers = GzStats.top(view.received, PANEL_LEADERBOARD_SIZE);
		List<BroadcastRecord> recent = new ArrayList<>(allTime.getRecent());

		List<Map.Entry<String, Integer>> weeklyGivers = GzStats.top(view.weekly, PANEL_LEADERBOARD_SIZE);

		// every giver counted (by the clan server, or this client)
		List<Map.Entry<String, Integer>> allGivers = GzStats.top(view.given, Integer.MAX_VALUE);

		panelShowsOwner = ClanRank.OWNER.equals(glowPicks.localRank());
		panelShowsDev = isDev();
		panelShowsFounder = isFounder();
		panelShowsNature = isNature();
		panelShowsLeader = config.syncEnabled() && ClanLeaders.isLeader(me);
		panelShowsRank = myRankTitle();
		return new PanelData(mine, summary, syncStatus(), partyStatus(), mapState.status(partyLocations.size()), view.clanWide,
			givers, receivers, recent, weeklyGivers, tracker.getWeekStart(),
			Streaks.longest(view.weekResults, GzTracker.WEEK_ZONE),
			Streaks.current(view.weekResults, tracker.getWeekStart(), GzTracker.WEEK_ZONE), allGivers,
			clanRoster(), clanRankTitles(), panelShowsOwner, panelShowsDev, panelShowsFounder, inCorParty(),
			config.syncEnabled() && serverClan != null ? serverClan.getAnnouncement() : null,
			announcementHint(),
			panelShowsLeader, me, myRankTitle(), myGem(),
			me == null ? 0 : view.givenOf(me), me == null ? 0 : session.getGiven().getOrDefault(me, 0),
			me == null ? 0 : view.receivedOf(me), me == null ? 0 : session.getReceived().getOrDefault(me, 0),
			panelShowsNature, GlowPicks.isStaff(glowPicks.localRank()), promotions(),
			config.syncEnabled() && serverClan != null ? serverClan.getRecentPromotions() : Collections.emptyList(),
			config.syncEnabled() ? raidBoard : Collections.emptyList(), myRaid, raidProblem());
	}

	/**
	 * The Gem League promotion assistant, for clan staff only (administrators up to the Owner, by your in-game rank).
	 * Join dates and titles come from the game's clan data; gz's given from the same numbers the panel shows (the clan
	 * server's with clan sync on). Cached for {@link #PROMOTIONS_MILLIS}. Client thread.
	 * @return null when you are not staff or not in a clan
	 */
	private Promotions.Result promotions()
	{
		ClanRank myRank = glowPicks.localRank();
		ClanSettings settings = client.getClanSettings();
		if (settings == null || !(GlowPicks.isStaff(myRank) || ClanRank.OWNER.equals(myRank)))
		{
			promotionsCache = null;
			return null;
		}
		long now = System.currentTimeMillis();
		if (promotionsCache != null && myRank.equals(promotionsRank) && now - promotionsAt < PROMOTIONS_MILLIS)
		{
			return promotionsCache;
		}
		ClanChannel channel = client.getClanChannel();
		List<Promotions.Member> members = new ArrayList<>();
		for (ClanMember member : settings.getMembers())
		{
			ClanRank rank = member.getRank();
			if (member.getName() == null || rank == null)
			{
				continue;
			}
			ClanTitle title = settings.titleForRank(rank);
			members.add(new Promotions.Member(displayName(member.getName()), title != null ? title.getName() : null,
				member.getJoinDate(), channel != null && channel.findMember(member.getName()) != null));
		}
		Map<String, Integer> gz = new HashMap<>();
		view.given.forEach((name, count) -> gz.merge(MemberCosmetics.key(name), count, Integer::sum));
		promotionsCache = Promotions.check(members, LocalDate.now(ZoneOffset.UTC), gz);
		promotionsRank = myRank;
		promotionsAt = now;
		return promotionsCache;
	}

	/** The banner's line while there is no announcement to show. Client thread. */
	/** Your clan rank's title ("Rank N" when it has none), or null when not in a clan or not loaded yet. Client thread. */
	private String myRankTitle()
	{
		ClanRank rank = glowPicks.localRank();
		if (rank == null)
		{
			return null;
		}
		String title = clanRankTitles().get(rank.getRank());
		return title == null ? "Rank " + rank.getRank() : title;
	}

	/** The highest Gem League glow your rank unlocks, or null. Client thread. */
	private GemGlow myGem()
	{
		ClanRank rank = glowPicks.localRank();
		return rank == null ? null : GemGlow.highestUnlocked(rank, clanRankTitles());
	}

	private String announcementHint()
	{
		if (!config.syncEnabled())
		{
			return "Turn on Sync with clan server in the CoR Clan settings to see clan announcements.";
		}
		if (serverClan == null)
		{
			return client.getGameState() == GameState.LOGGED_IN
				? "Loading clan announcements..."
				: "Clan announcements load when you log in.";
		}
		return "No announcements right now.";
	}

	/** One line for the panel about clan sync. Client thread. */
	private String syncStatus()
	{
		if (!config.syncEnabled())
		{
			return "Clan sync: off (turn on in settings)";
		}
		SyncModels.Reporter r = currentReporter();
		if (r != null && r.equals(refusedReporter))
		{
			return syncRefusal == ClanApi.Outcome.NAME_TAKEN
				? "Clan sync: your name is linked to another account, ask CoR staff"
				: "Clan sync: only members of the CoR clan can report";
		}
		if (client.getGameState() != GameState.LOGGED_IN && !view.clanWide)
		{
			return "Clan sync: on, loads when you log in";
		}
		if (serverClan == null)
		{
			return "Clan sync: on, waiting for the clan server";
		}
		if (!view.clanWide)
		{
			return "Clan sync: on, the clan server is not answering";
		}
		return "Clan sync: on, updated " + new SimpleDateFormat("HH:mm").format(new Date(serverClanAt));
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
		if (!event.isGuest())
		{
			clanChannelAt = System.currentTimeMillis();
			promotionsCache = null;
		}
		refreshPanel();
	}

	@Subscribe
	public void onClanMemberJoined(ClanMemberJoined event)
	{
		refreshPanel();
		String name = event.getClanMember() == null ? null : event.getClanMember().getName();
		long now = System.currentTimeMillis();
		// our own clan only, not the channel filling in as we arrive, and never ourselves (that is ownLoginPending)
		if (event.getClanChannel() != client.getClanChannel() || now - clanChannelAt < CHANNEL_SETTLE_MILLIS
			|| name == null || MemberCosmetics.key(name).equals(MemberCosmetics.key(localPlayerName())))
		{
			return;
		}
		if (legion.isLogin(name, now))
		{
			announceLegion(name);
			if (InfernoReminder.isTarget(name) && InfernoReminder.isWatcher(localPlayerName()))
			{
				remindInferno(name, false);
			}
		}
	}

	@Subscribe
	public void onClanMemberLeft(ClanMemberLeft event)
	{
		refreshPanel();
		if (event.getClanChannel() == client.getClanChannel() && event.getClanMember() != null)
		{
			legion.left(event.getClanMember().getName(), System.currentTimeMillis());
		}
	}

	/**
	 * JoeBoob's Infernal cape reminder (an inside joke between him and Ray): on every login, with the number of days he
	 * has been reminded, raised once per day. Each client counts on its own. Client thread.
	 * @param own true on JoeBoob's own client, false on Ray's
	 */
	private void remindInferno(String name, boolean own)
	{
		String key = own ? INFERNO_SELF_KEY : INFERNO_SEEN_KEY;
		LocalDate today = LocalDate.now();
		int day = InfernoReminder.countAfter(configManager.getConfiguration(CorClanConfig.GROUP, key), today);
		configManager.setConfiguration(CorClanConfig.GROUP, key, InfernoReminder.store(today, day));
		String icon = iconService.tagFor("fire");
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
			own ? InfernoReminder.targetLine(icon, day) : InfernoReminder.watcherLine(icon, name, day), null);
	}

	/**
	 * Our first gm of the day (UTC) in clan chat earns a pack's worth of TCG points from the clan server, with clan sync
	 * on. Asked once a day; after a network error the next gm asks again. Gm's are not counted anywhere. Client thread.
	 */
	private void claimDailyGm(String said, long now)
	{
		long day = DailyGm.utcDay(now);
		SyncModels.Reporter r = currentReporter();
		if (!config.syncEnabled() || gmInFlight || day == gmAnsweredDay || r == null || r.equals(refusedReporter))
		{
			return;
		}
		gmInFlight = true;
		clanApi.sendGm(new SyncModels.GmPayload(r, said), (outcome, result) -> clientThread.invokeLater(() ->
		{
			gmInFlight = false;
			if (!started || !config.syncEnabled())
			{
				return;
			}
			if (outcome == ClanApi.Outcome.RETRY)
			{
				return;
			}
			gmAnsweredDay = day;
			if (outcome.blocksReporter())
			{
				onRefused(r, outcome);
				return;
			}
			if (outcome == ClanApi.Outcome.OK && result != null && result.isAwarded())
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "<col=ffc832>gm! +" + String.format("%,d", result.getPointsEarned())
					+ " TCG points for your daily gm. Next one after 00:00 UTC.</col>", null);
			}
		}));
	}

	/**
	 * A player in view died in the Chambers of Xeric: a chicken's death squawk plays on this client, if we are in a
	 * clan. It is the game's own sound effect, so nothing is bundled. Every CoR plugin user in the raid sees the death
	 * too, so they all hear it; nothing is sent anywhere. Client thread.
	 */
	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (!config.coxDeathSound() || !(event.getActor() instanceof Player)
			|| client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1 || client.getClanChannel() == null)
		{
			return;
		}
		int volume = RaidSounds.volume(config.coxDeathVolume());
		if (volume > 0)
		{
			client.playSoundEffect(RaidSounds.CHICKEN_DEATH, volume);
		}
	}

	/**
	 * A leader logged in: a line in chat on this client. The Spartan (Ray) lands in gold with a crown on each side,
	 * a fanfare and the words over his head if he is in view. Client thread.
	 */
	private void announceLegion(String name)
	{
		if (!config.legionLogins())
		{
			return;
		}
		boolean spartan = Legion.isSpartan(name);
		String icon = iconService.tagFor(spartan ? "crown" : "star");
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", Legion.line(name, icon), null);
		if (!spartan)
		{
			return;
		}
		client.playSoundEffect(SoundEffectID.GE_ADD_OFFER_DINGALING);
		for (Player p : client.getTopLevelWorldView().players())
		{
			if (p != null && p.getName() != null && Legion.isSpartan(p.getName()))
			{
				p.setOverheadText("A Spartan has landed!");
				p.setOverheadCycle(SPARTAN_OVERHEAD_CYCLES);
			}
		}
	}

	// ---------------------------------------------------------------- clan sync (opt-in)

	/** The name of the clan our character is in, or null. Client thread. */
	private String clanName()
	{
		ClanSettings settings = client.getClanSettings();
		if (settings != null && settings.getName() != null && !settings.getName().isEmpty())
		{
			return settings.getName();
		}
		ClanChannel channel = client.getClanChannel();
		return channel == null || channel.getName() == null || channel.getName().isEmpty() ? null : channel.getName();
	}

	/** Who we report as, or null when not logged in or not in a clan. Client thread. */
	private SyncModels.Reporter currentReporter()
	{
		long hash = client.getAccountHash();
		String me = localPlayerName();
		String clan = clanName();
		if (client.getGameState() != GameState.LOGGED_IN || hash == -1 || me == null || me.isEmpty() || clan == null)
		{
			return null;
		}
		return new SyncModels.Reporter(Long.toString(hash), me, clan);
	}

	/** True when {@code name} is our own character. Client thread. */
	private boolean isMe(String name)
	{
		String me = localPlayerName();
		return me != null && !me.isEmpty() && MemberCosmetics.key(me).equals(MemberCosmetics.key(name));
	}

	/**
	 * Client thread: queues something our own character did for the next batch. Callers only pass a gz we said
	 * or a broadcast about us: RuneLite does not allow crowdsourcing data about other players.
	 */
	private void queueSync(SyncModels.Event event)
	{
		SyncModels.Reporter r = config.syncEnabled() ? currentReporter() : null;
		if (r == null || r.equals(refusedReporter))
		{
			return;
		}
		synchronized (syncQueue)
		{
			if (!r.equals(reporter))
			{
				// another character logged in: what the last one had queued is never sent under this name
				syncQueue.clear();
				reporter = r;
			}
			syncQueue.add(event);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGGING_IN)
		{
			// from the login screen it is a real login; a world hop goes through HOPPING instead
			ownLoginPending = lastGameState == GameState.LOGIN_SCREEN || lastGameState == GameState.LOGIN_SCREEN_AUTHENTICATOR;
		}
		else if (state == GameState.HOPPING || state == GameState.LOGIN_SCREEN)
		{
			ownLoginPending = false;
		}
		if (state == GameState.LOGIN_SCREEN)
		{
			// our raid group stays up (the server drops it an hour after our last resend); the next login looks at
			// the board quietly first
			raidBoardSeen = false;
			// the next login advertises what is open straight away, on a fresh line
			raidLine = null;
			raidLineAt = 0;
			legion.clear();
			// another account may log in next; it gets its own daily gm
			gmAnsweredDay = -1;
		}
		lastGameState = state;
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			// the next login reports its rank names and profile again, and gets one more try if it was refused
			ranksReported = false;
			sentProfile = null;
			refusedReporter = null;
			syncRefusal = null;
		}
		else if (event.getGameState() == GameState.LOGGED_IN && config.syncEnabled()
			&& System.currentTimeMillis() - lastPull >= PULL_MIN_MILLIS)
		{
			pullFromServer();
		}
	}

	/**
	 * Once per login (with sync on) sends the clan's rank numbers and their titles, e.g. 126 "Owner",
	 * 5 "Captain", so admins can pick an icon per rank. No player names are sent. Client thread.
	 */
	private void reportRanks()
	{
		if (ranksReported || refusedReporter != null || !config.syncEnabled() || client.getClanSettings() == null)
		{
			return;
		}
		SyncModels.Reporter r = currentReporter();
		if (r == null)
		{
			return;
		}
		ranksReported = true;

		Map<Integer, String> titles = clanRankTitles();
		ClanTitle guest = client.getClanSettings().titleForRank(ClanRank.GUEST);
		if (guest != null && guest.getName() != null && !guest.getName().isEmpty())
		{
			titles.put(ClanRank.GUEST.getRank(), guest.getName());
		}
		if (titles.isEmpty())
		{
			return;
		}
		List<SyncModels.RankTitle> payload = new ArrayList<>();
		titles.forEach((rank, title) -> payload.add(new SyncModels.RankTitle(rank, title)));
		clanApi.sendRanks(new SyncModels.RanksPayload(r, payload), outcome ->
		{
			if (outcome.blocksReporter())
			{
				onRefused(r, outcome);
			}
		});
	}

	/** Sends our queued gz's and broadcasts to the clan server in one batch. Runs off the client thread. */
	@Schedule(period = 30, unit = ChronoUnit.SECONDS, asynchronous = true)
	public void flushSync()
	{
		if (!config.syncEnabled() || !reportInFlight.compareAndSet(false, true))
		{
			return;
		}
		long now = System.currentTimeMillis();
		SyncModels.Reporter r;
		List<SyncModels.Event> batch;
		synchronized (syncQueue)
		{
			r = reporter;
			syncQueue.dropOlderThan(now - MAX_EVENT_AGE_MILLIS);
			batch = r == null || r.equals(refusedReporter) ? Collections.emptyList() : syncQueue.drain(SyncQueue.MAX_BATCH);
		}
		if (batch.isEmpty())
		{
			reportInFlight.set(false);
			return;
		}
		clanApi.sendReport(new SyncModels.ReportPayload(r, now, batch), outcome ->
		{
			if (outcome == ClanApi.Outcome.RETRY)
			{
				synchronized (syncQueue)
				{
					// goes out again with the next batch, unless another character took over meanwhile
					if (config.syncEnabled() && r.equals(reporter))
					{
						syncQueue.requeue(batch);
					}
				}
			}
			else if (outcome.blocksReporter())
			{
				onRefused(r, outcome);
			}
			reportInFlight.set(false);
		});
	}

	/** Every 10 seconds: our own clan rank and glow picks go to the clan server if they changed. */
	@Schedule(period = 10, unit = ChronoUnit.SECONDS)
	public void profileTimer()
	{
		if (config.syncEnabled())
		{
			clientThread.invokeLater(this::sendProfileIfChanged);
		}
	}

	/**
	 * Client thread: sends our rank and glow picks on login and whenever they differ from what the server last
	 * took, at most every {@link #PROFILE_MIN_MILLIS}. Rank, dev and founder glow picks travel in one list; each
	 * viewer ignores the ids that are not for it.
	 */
	private void sendProfileIfChanged()
	{
		long now = System.currentTimeMillis();
		SyncModels.Reporter r = config.syncEnabled() && !profileInFlight && now >= nextProfileAt ? currentReporter() : null;
		if (r == null || r.equals(refusedReporter))
		{
			return;
		}
		ClanRank rank = glowPicks.localRank();
		if (rank == null)
		{
			rank = clanRankOf(r.getRsn());
		}
		if (rank == null)
		{
			// the clan data has not loaded yet
			return;
		}
		List<String> glows = new ArrayList<>(glowPicks.localPicks());
		if (isDev())
		{
			glows.addAll(lavaAura.localPicks());
		}
		if (isFounder())
		{
			glows.addAll(daylightAura.localPicks());
		}
		if (isNature())
		{
			glows.addAll(natureAura.localPicks());
		}
		List<Object> profile = Arrays.asList(r, rank.getRank(), glows);
		if (profile.equals(sentProfile))
		{
			return;
		}
		profileInFlight = true;
		nextProfileAt = now + PROFILE_MIN_MILLIS;
		clanApi.sendProfile(new SyncModels.ProfilePayload(r, rank.getRank(), glows), outcome -> clientThread.invokeLater(() ->
		{
			profileInFlight = false;
			if (outcome == ClanApi.Outcome.RETRY)
			{
				nextProfileAt = System.currentTimeMillis() + PROFILE_RETRY_MILLIS;
				return;
			}
			// taken or refused for good: either way this exact profile is not sent again
			sentProfile = profile;
			if (outcome.blocksReporter())
			{
				onRefused(r, outcome);
			}
		}));
	}

	/**
	 * The server refuses everything from this character (its name belongs to another account, or it is not in
	 * CoR). Nothing more is sent for it until the next login, and a taken name is explained in chat once.
	 */
	private void onRefused(SyncModels.Reporter r, ClanApi.Outcome outcome)
	{
		clientThread.invokeLater(() ->
		{
			if (!started || !config.syncEnabled() || r.equals(refusedReporter))
			{
				return;
			}
			log.debug("CoR sync: the clan server refuses {}: {}", r.getRsn(), outcome);
			refusedReporter = r;
			syncRefusal = outcome;
			synchronized (syncQueue)
			{
				if (r.equals(reporter))
				{
					syncQueue.clear();
				}
			}
			if (outcome == ClanApi.Outcome.NAME_TAKEN && !r.equals(nameTakenTold)
				&& client.getGameState() == GameState.LOGGED_IN)
			{
				nameTakenTold = r;
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"CoR: clan sync can't report for " + r.getRsn() + ": the clan server has that name linked to "
						+ "another account. Ask CoR staff to unlink it, then log in again.", null);
			}
			refreshPanel();
		});
	}

	/** Every 2 minutes while logged in: refreshes the clan's gz counts, icons, titles and glow picks. */
	@Schedule(period = 2, unit = ChronoUnit.MINUTES)
	public void scheduledPull()
	{
		if (!config.syncEnabled())
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				pullFromServer();
			}
			// an answer that got too old hands the leaderboards back to this client's own counts
			boolean wasClanWide = view.clanWide;
			updateGzView();
			if (wasClanWide != view.clanWide)
			{
				refreshPanel();
			}
		});
	}

	/** Why we can't post a raid finder group right now, or null. Client thread. */
	private String raidProblem()
	{
		if (!config.syncEnabled())
		{
			return "Turn on Clan sync in the plugin settings to see and post groups.";
		}
		if (client.getGameState() != GameState.LOGGED_IN || client.getClanChannel() == null)
		{
			return "Log in and join the clan chat to post a group.";
		}
		return null;
	}

	/** Puts up (or updates) our own raid finder group. From the panel, so it hops to the client thread. */
	private void postRaid(RaidAd ad)
	{
		clientThread.invokeLater(() ->
		{
			SyncModels.Reporter r = currentReporter();
			if (raidProblem() != null || r == null || r.equals(refusedReporter))
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CoR: can't post a raid group right now", null);
				return;
			}
			sendRaid(r, ad, true);
		});
	}

	/** @param tell print a chat line with the result (a click), not for the quiet resends */
	private void sendRaid(SyncModels.Reporter r, RaidAd ad, boolean tell)
	{
		int world = client.getWorld();
		raidSentAt = System.currentTimeMillis();
		raidSentWorld = world;
		boolean raiding = inRaid();
		raidSentRaiding = raiding;
		clanApi.sendRaid(new SyncModels.RaidPayload(r, ad.raid.id, ad.filled, ad.size, world > 0 ? world : null, ad.note, raiding),
			outcome -> clientThread.invokeLater(() ->
			{
				if (!started || !config.syncEnabled())
				{
					return;
				}
				if (outcome == ClanApi.Outcome.OK)
				{
					myRaid = ad;
					myRaidReporter = r;
					pullRaids();
					if (tell)
					{
						client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CoR: your " + ad.raid.shortName + " group ("
							+ ad.slots() + ") is up on the raid finder", null);
					}
					refreshPanel();
					return;
				}
				if (outcome.blocksReporter())
				{
					onRefused(r, outcome);
				}
				if (tell)
				{
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", outcome == ClanApi.Outcome.RETRY
						? "CoR: the clan server isn't answering, try posting your raid group again"
						: "CoR: the clan server refused the raid group", null);
				}
			}));
	}

	/**
	 * Takes our raid finder group down: from the panel, or when we enter the raid.
	 * Runs at once on the client thread (so a tick never queues it twice), else hops there.
	 * @param done the chat line once the server took it down
	 */
	private void stopRaid(String done)
	{
		clientThread.invoke(() ->
		{
			SyncModels.Reporter r = myRaidReporter;
			myRaid = null;
			myRaidReporter = null;
			raidStoppedAt = System.currentTimeMillis();
			refreshPanel();
			if (r == null || !config.syncEnabled())
			{
				return;
			}
			clanApi.stopRaid(new SyncModels.RaidStopPayload(r), outcome -> clientThread.invokeLater(() ->
			{
				if (started && config.syncEnabled())
				{
					pullRaids();
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", outcome == ClanApi.Outcome.OK
						? done
						: "CoR: couldn't reach the clan server to take your raid group down; it goes by itself within the hour", null);
				}
			}));
		});
	}

	/** "Ping Discord": the clan server posts our open group in the Discord's looking-for-group channel. */
	private void pingRaid()
	{
		clientThread.invokeLater(() ->
		{
			SyncModels.Reporter r = currentReporter();
			if (myRaid == null || raidProblem() != null || r == null || !r.equals(myRaidReporter))
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CoR: put your raid group up first", null);
				return;
			}
			clanApi.pingRaid(new SyncModels.RaidStopPayload(r), problem -> clientThread.invokeLater(() ->
			{
				if (started)
				{
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", problem == null
						? "CoR: your " + myRaidName() + " group is posted in the clan Discord"
						: "CoR: " + problem, null);
				}
			}));
		});
	}

	private String myRaidName()
	{
		return myRaid == null ? "raid" : myRaid.raid.shortName;
	}

	/**
	 * We are inside a raid: the Chambers (in the dungeon), or the Theatre / Tombs instance with a party. The parties
	 * are formed outside the instances, so a group only shows as raiding once we are actually in.
	 */
	private boolean inRaid()
	{
		if (client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1)
		{
			return true;
		}
		return client.isInInstancedRegion() && (client.getVarbitValue(VarbitID.TOB_CLIENT_PARTYSTATUS) > 0
			|| client.getVarbitValue(VarbitID.TOA_CLIENT_PARTYSTATUS) > 0);
	}

	/**
	 * Every game tick while we have a raid group up: entering or leaving the raid sends it again at once, so the board
	 * (and everyone's clan chat) shows the party as raiding while we are inside. The group stays listed either way.
	 */
	private void checkRaidEntry()
	{
		SyncModels.Reporter r = myRaid == null || client.getGameState() != GameState.LOGGED_IN ? null : currentReporter();
		if (r == null || !r.equals(myRaidReporter) || r.equals(refusedReporter) || inRaid() == raidSentRaiding)
		{
			return;
		}
		sendRaid(r, myRaid, false);
		if (raidSentRaiding)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CoR: good luck in there! Your " + myRaidName()
				+ " group shows as raiding on the raid finder", null);
		}
	}

	/**
	 * Every 30 seconds with clan sync on and logged in: the raid finder board, and our own group sent again every few
	 * minutes (or at once after a world hop) so it stays up while we play.
	 */
	@Schedule(period = 30, unit = ChronoUnit.SECONDS)
	public void raidTimer()
	{
		if (!config.syncEnabled())
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() != GameState.LOGGED_IN)
			{
				return;
			}
			pullRaids();
			SyncModels.Reporter r = currentReporter();
			if (myRaid != null && r != null && r.equals(myRaidReporter) && !r.equals(refusedReporter)
				&& (System.currentTimeMillis() - raidSentAt >= RAID_RESEND_MILLIS || client.getWorld() != raidSentWorld))
			{
				sendRaid(r, myRaid, false);
			}
		});
	}

	private void pullRaids()
	{
		clanApi.fetchRaids(response ->
		{
			List<RaidListing> board = RaidListing.board(response);
			clientThread.invokeLater(() ->
			{
				if (!started || !config.syncEnabled())
				{
					return;
				}
				adoptOwnRaid(board);
				boolean announced = announceNewRaids(board);
				if (!board.equals(raidBoard))
				{
					raidBoard = board;
					refreshPanel();
				}
				if (!announced && !board.isEmpty() && System.currentTimeMillis() - raidLineAt >= RAID_AD_MILLIS)
				{
					showRaidLine(RaidLines.advert(board));
				}
			});
		});
	}

	/**
	 * Our group is on the board but this client doesn't know it (RuneLite was restarted while it was up): take it
	 * back, so it is resent while we play and the panel can update or take it down.
	 */
	private void adoptOwnRaid(List<RaidListing> board)
	{
		SyncModels.Reporter r = currentReporter();
		if (r == null || System.currentTimeMillis() - raidStoppedAt < RAID_ADOPT_PAUSE_MILLIS)
		{
			return;
		}
		if (myRaid != null && !r.equals(myRaidReporter))
		{
			// a different account logged in; the old one's group runs out on the server
			myRaid = null;
			myRaidReporter = null;
		}
		if (myRaid != null)
		{
			return;
		}
		String me = localPlayerName();
		for (RaidListing l : board)
		{
			if (l.isHost(me))
			{
				myRaid = new RaidAd(l.raid, l.filled, l.size, l.note);
				myRaidReporter = r;
				raidSentAt = 0;
				refreshPanel();
				return;
			}
		}
	}

	/**
	 * A raid finder line (a game message) for a group put up since the last look, or a party that just went into the raid (not our own).
	 * The first look after login is quiet: the advert that follows lists what is already open.
	 * @return true if a line was shown
	 */
	private boolean announceNewRaids(List<RaidListing> board)
	{
		String me = localPlayerName();
		List<RaidListing> fresh = new ArrayList<>();
		for (RaidListing l : board)
		{
			if (seenRaids.add(l.key()) && raidBoardSeen && !l.isHost(me))
			{
				fresh.add(l);
			}
		}
		// a party that went into the raid: once per raid trip
		List<RaidListing> raiding = new ArrayList<>();
		for (RaidListing l : board)
		{
			if (!l.raiding)
			{
				raidingSeen.remove(l.key());
			}
			else if (raidingSeen.add(l.key()) && raidBoardSeen && !l.isHost(me) && !fresh.contains(l))
			{
				raiding.add(l);
			}
		}
		raidBoardSeen = true;
		if (seenRaids.size() > 500)
		{
			seenRaids.clear();
			board.forEach(l -> seenRaids.add(l.key()));
		}
		if (raidingSeen.size() > 500)
		{
			raidingSeen.clear();
		}
		if (fresh.size() + raiding.size() > 1)
		{
			showRaidLine(RaidLines.advert(board));
		}
		else if (fresh.size() == 1)
		{
			showRaidLine(RaidLines.newGroup(fresh.get(0)));
		}
		else if (raiding.size() == 1)
		{
			showRaidLine(RaidLines.raiding(raiding.get(0)));
		}
		return !fresh.isEmpty() || !raiding.isEmpty();
	}

	/**
	 * Shows a raid finder line as a game message, on this client only (plugins may not send chat, and a game message
	 * can't be mistaken for something a clanmate said). If no clan chat or other game message came in since our last
	 * raid line, that line is updated in place instead, so the ads stack into one line rather than filling the chat.
	 * Client thread.
	 */
	private void showRaidLine(String text)
	{
		ClanChannel channel = client.getClanChannel();
		if (!config.raidFinderAlerts() || channel == null)
		{
			return;
		}
		long now = System.currentTimeMillis();
		raidLineAt = now;
		String line = "<col=" + RAID_ALERT_COLOR + ">" + text + "</col>";
		if (raidLine != null && !chatSinceRaidLine && stillInChat(raidLine))
		{
			raidLine.setValue(line);
			raidLine.setTimestamp((int) (now / 1000));
			client.refreshChat();
			return;
		}
		addingRaidLine = true;
		try
		{
			raidLine = client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", line, null);
		}
		finally
		{
			addingRaidLine = false;
		}
		chatSinceRaidLine = false;
	}

	/** The line is still in the game message history (old lines drop out of it). */
	private boolean stillInChat(MessageNode node)
	{
		ChatLineBuffer buffer = client.getChatLineMap().get(ChatMessageType.GAMEMESSAGE.getType());
		if (buffer == null)
		{
			return false;
		}
		for (MessageNode line : buffer.getLines())
		{
			if (line == node)
			{
				return true;
			}
		}
		return false;
	}

	/** Every 10 seconds while logged in: who wears the Kephri dung, so a ::shame shows up fast. */
	@Schedule(period = 10, unit = ChronoUnit.SECONDS)
	public void shameTimer()
	{
		if (config.syncEnabled())
		{
			clientThread.invokeLater(() ->
			{
				if (client.getGameState() == GameState.LOGGED_IN)
				{
					pullShames();
				}
			});
		}
	}

	private void pullShames()
	{
		clanApi.fetchShames(response ->
		{
			Shames shames = Shames.of(response, System.currentTimeMillis());
			clientThread.invokeLater(() ->
			{
				if (started && config.syncEnabled())
				{
					shameAura.setShames(shames);
				}
			});
		});
	}

	/** Asks the clan server for everyone's data and its chat icons. The answers arrive on OkHttp threads. */
	private void pullFromServer()
	{
		lastPull = System.currentTimeMillis();
		pullShames();
		pullRaids();
		clanApi.fetchClan(this::onClanResponse);
		clanApi.fetchIcons(response -> clientThread.invokeLater(() ->
		{
			if (!started || !config.syncEnabled())
			{
				return;
			}
			boolean first = !serverIconsLoaded;
			serverIconsLoaded = true;
			boolean iconsChanged = iconService.applyServerIcons(response.getIcons());
			boolean ranksChanged = iconService.applyRankIcons(response.getRankIcons());
			if (first || iconsChanged || ranksChanged)
			{
				// new icon names can now be given to players, and chat shows the new images
				rebuildCosmetics();
				client.refreshChat();
			}
		}));
	}

	/** {@code GET /v1/clan} answered (on an OkHttp thread): everyone's counts, icons, glows and the announcement. */
	private void onClanResponse(SyncModels.ClanResponse response)
	{
		ClanSnapshot snapshot = ClanSnapshot.of(response);
		clientThread.invokeLater(() ->
		{
			if (!started || !config.syncEnabled())
			{
				return;
			}
			serverClan = snapshot;
			serverClanAt = System.currentTimeMillis();
			// other players' picks; each is still only drawn as far as their clan rank, as we see it, allows
			glowPicks.setSharedPicks(snapshot.getGlows());
			lavaAura.setSharedPicks(snapshot.getGlows());
			daylightAura.setSharedPicks(snapshot.getGlows());
			natureAura.setSharedPicks(snapshot.getGlows());
			rebuildCosmetics();
			updateGzView();
			client.refreshChat();
			refreshPanel();
		});
	}

	/**
	 * Panel Edit button (clan leaders only): sends the new announcement banner, or clears it when {@code text} is
	 * empty, then reloads the clan data so it shows straight away. Everyone else gets it on their next refresh.
	 */
	private void editAnnouncement(String text)
	{
		clientThread.invokeLater(() ->
		{
			if (!ClanLeaders.isLeader(localPlayerName()) || !config.syncEnabled())
			{
				return;
			}
			SyncModels.Reporter r = currentReporter();
			if (r == null || r.equals(refusedReporter))
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CoR: the clan server isn't taking anything from you right now", null);
				return;
			}
			clanApi.sendAnnouncement(new SyncModels.AnnouncementPayload(r, text), outcome -> clientThread.invokeLater(() ->
			{
				if (!started || !config.syncEnabled())
				{
					return;
				}
				String done;
				if (outcome == ClanApi.Outcome.OK)
				{
					clanApi.fetchClanNow(this::onClanResponse);
					done = text.isEmpty() ? "CoR: announcement taken down" : "CoR: announcement posted";
				}
				else if (outcome == ClanApi.Outcome.RETRY)
				{
					done = "CoR: the clan server isn't answering, try posting the announcement again";
				}
				else
				{
					done = "CoR: the clan server refused the announcement";
					if (outcome.blocksReporter())
					{
						onRefused(r, outcome);
					}
				}
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", done, null);
			}));
		});
	}

	/** Clan sync was turned off (or the plugin stopped): forgets the server's data and puts the bundled icons back. */
	private void clearServerState()
	{
		synchronized (syncQueue)
		{
			syncQueue.clear();
			reporter = null;
		}
		clientThread.invokeLater(() ->
		{
			serverClan = null;
			serverClanAt = 0;
			serverIconsLoaded = false;
			refusedReporter = null;
			syncRefusal = null;
			ranksReported = false;
			sentProfile = null;
			nextProfileAt = 0;
			lastPull = 0;
			glowPicks.clearSharedPicks();
			lavaAura.clearSharedPicks();
			daylightAura.clearSharedPicks();
			natureAura.clearSharedPicks();
			shameAura.clearShames();
			raidBoard = Collections.emptyList();
			raidBoardSeen = false;
			myRaid = null;
			myRaidReporter = null;
			iconService.clearServerIcons();
			rebuildCosmetics();
			updateGzView();
			client.refreshChat();
			refreshPanel();
		});
	}

	// ---------------------------------------------------------------- CoR party (opt-in, clan map only)

	private String passphrase()
	{
		String p = config.partyPassphrase() == null ? "" : config.partyPassphrase().trim();
		return p.isEmpty() ? CorClanConfig.DEFAULT_PARTY_PASSPHRASE : p;
	}

	/** In RuneLite's party with our passphrase. */
	private boolean inCorParty()
	{
		return partyService.isInParty() && passphrase().equals(partyService.getPartyPassphrase());
	}

	private boolean partyActive()
	{
		return inCorParty() && partyService.getLocalMember() != null;
	}

	/**
	 * Panel button: joins the CoR party, or leaves it if we are in it. The only way the plugin ever changes
	 * the party, so it never joins on its own (RuneLite asks plugins not to auto-join parties).
	 */
	private void toggleCorParty()
	{
		clientThread.invokeLater(() ->
		{
			if (inCorParty())
			{
				leaveCorParty();
			}
			else
			{
				log.debug("Joining the CoR party");
				partyService.changeParty(passphrase());
			}
			refreshPanel();
		});
	}

	private void leaveCorParty()
	{
		stopSharingLocation();
		clearPartyLocations();
		if (inCorParty())
		{
			partyService.changeParty(null);
		}
	}

	private String partyStatus()
	{
		if (!partyService.isInParty())
		{
			return "CoR party: not joined";
		}
		if (!inCorParty())
		{
			return "CoR party: you are in another party";
		}
		if (partyService.getLocalMember() == null)
		{
			return "CoR party: connecting";
		}
		int others = Math.max(0, partyService.getMembers().size() - 1);
		return "CoR party: on, " + others + (others == 1 ? " other member" : " other members");
	}

	/** The sender of a CoR message, or null if it is us, unknown, or we are not in the CoR party. */
	private PartyMember sender(long memberId)
	{
		if (!partyActive())
		{
			return null;
		}
		PartyMember member = partyService.getMemberById(memberId);
		PartyMember local = partyService.getLocalMember();
		if (member == null || member.getDisplayName() == null || (local != null && local.getMemberId() == memberId))
		{
			return null;
		}
		return member;
	}

	/** Our clan's rank for a name, or null if they are not in it. Client thread. */
	private ClanRank clanRankOf(String name)
	{
		ClanSettings settings = client.getClanSettings();
		if (settings == null || name == null)
		{
			return null;
		}
		String key = MemberCosmetics.key(name);
		for (ClanMember m : settings.getMembers())
		{
			if (key.equals(MemberCosmetics.key(m.getName())))
			{
				return m.getRank();
			}
		}
		return null;
	}

	@Subscribe
	public void onPartyChanged(PartyChanged event)
	{
		clientThread.invokeLater(() ->
		{
			clearPartyLocations();
			refreshPanel();
		});
	}

	// the panel shows how many members the party has
	@Subscribe
	public void onUserJoin(UserJoin event)
	{
		refreshPanel();
	}

	@Subscribe
	public void onUserPart(UserPart event)
	{
		clientThread.invokeLater(() ->
		{
			if (partyLocations.remove(event.getMemberId()) != null)
			{
				redrawMap();
			}
			partyNames.remove(event.getMemberId());
			partySeen.remove(event.getMemberId());
			refreshPanel();
		});
	}

	// party messages arrive on the websocket thread; all state lives on the client thread

	@Subscribe
	public void onCorLocation(CorLocation msg)
	{
		clientThread.invokeLater(() ->
		{
			PartyMember from = sender(msg.getMemberId());
			if (from == null || clanRankOf(from.getDisplayName()) == null)
			{
				return;
			}
			long id = msg.getMemberId();
			if (msg.isStopped())
			{
				partyLocations.remove(id);
				partySeen.remove(id);
			}
			else
			{
				partyLocations.put(id, msg);
				partyNames.put(id, from.getDisplayName());
				partySeen.put(id, System.currentTimeMillis());
			}
			redrawMap();
		});
	}

	// ---------------------------------------------------------------- clan map (through the CoR party)

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		holyAura.onClientTick();
		lavaAura.onClientTick();
		daylightAura.onClientTick();
		natureAura.onClientTick();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		holyAura.onGameTick();
		shameAura.onGameTick();
		checkRaidEntry();
		if (ownLoginPending && localPlayerName() != null)
		{
			ownLoginPending = false;
			if (Legion.isLegionnaire(localPlayerName()))
			{
				announceLegion(localPlayerName());
			}
			if (InfernoReminder.isTarget(localPlayerName()))
			{
				remindInferno(localPlayerName(), true);
			}
		}
		tickLocation();
		reportRanks();
		// the Owner glow section appears once your clan rank has loaded (and goes if it changes)
		if (panelShowsOwner != ClanRank.OWNER.equals(glowPicks.localRank()) || panelShowsDev != isDev()
			|| panelShowsFounder != isFounder() || panelShowsNature != isNature()
			|| panelShowsLeader != (config.syncEnabled() && ClanLeaders.isLeader(localPlayerName()))
			|| !Objects.equals(panelShowsRank, myRankTitle()))
		{
			refreshPanel();
		}
	}

	/** Client thread, every game tick: shares our position with the CoR party while "Share my location" is on. */
	private void tickLocation()
	{
		Player me = client.getLocalPlayer();
		if (client.getGameState() != GameState.LOGGED_IN || me == null)
		{
			return;
		}
		boolean inWilderness = client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 1;
		LocationRules.Decision decision = LocationRules.decide(inWilderness, client.isInInstancedRegion(), config.shareInWilderness());
		setMapState(MapState.of(config.shareLocation(), inCorParty(), partyActive(), decision));
		if (mapState != MapState.SHARING)
		{
			stopSharingLocation();
			return;
		}

		int every = Math.max(LOCATION_TICKS, partyService.getMembers().size() / 2);
		// the first position goes out right away, so your own marker appears as soon as you turn sharing on
		if (selfLocation != null && ++locationTick < every)
		{
			return;
		}
		locationTick = 0;
		expirePartyLocations();

		WorldPoint here = me.getWorldLocation();
		CorLocation loc = new CorLocation(client.getWorld(), here.getX(), here.getY(), here.getPlane(), inWilderness, false);
		long now = System.currentTimeMillis();
		boolean moved = !loc.equals(selfLocation);
		if (moved || now - lastLocationSent >= LOCATION_HEARTBEAT_MILLIS)
		{
			partyService.send(loc);
			lastLocationSent = now;
		}
		selfLocation = loc;
		if (moved)
		{
			redrawMap();
		}
	}

	/** Client thread: remembers the clan map's state, and says in chat when it starts sharing or pauses. */
	private void setMapState(MapState next)
	{
		if (next == mapState)
		{
			return;
		}
		mapState = next;
		if (next.announcement != null)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", next.announcement, null);
		}
		refreshPanel();
	}

	/** Client thread: tells the party we stopped and hides the map. Safe to call when not sharing. */
	private void stopSharingLocation()
	{
		if (selfLocation == null)
		{
			return;
		}
		selfLocation = null;
		locationTick = 0;
		lastLocationSent = 0;
		if (partyActive())
		{
			partyService.send(new CorLocation(0, 0, 0, 0, false, true));
		}
		mapPoints.clear();
	}

	private void expirePartyLocations()
	{
		long cutoff = System.currentTimeMillis() - LOCATION_EXPIRY_MILLIS;
		if (partySeen.entrySet().removeIf(e -> e.getValue() < cutoff))
		{
			partyLocations.keySet().retainAll(partySeen.keySet());
			redrawMap();
		}
	}

	/** Only people who share can see others. */
	private void redrawMap()
	{
		if (selfLocation == null)
		{
			mapPoints.clear();
			return;
		}
		Map<String, CorLocation> others = new TreeMap<>();
		partyLocations.forEach((id, loc) -> others.put(partyNames.getOrDefault(id, "Clanmate"), loc));
		mapPoints.update(others, selfLocation);
		if (others.size() != mapCount)
		{
			mapCount = others.size();
			refreshPanel();
		}
	}

	private void clearPartyLocations()
	{
		partyLocations.clear();
		partyNames.clear();
		partySeen.clear();
		mapPoints.clear();
	}
}

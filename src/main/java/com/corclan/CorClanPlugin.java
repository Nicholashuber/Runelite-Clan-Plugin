package com.corclan;

import com.corclan.chat.RankCommands;
import com.corclan.clan.ClanRoster;
import com.corclan.glow.DevGlow;
import com.corclan.glow.GlowEffect;
import com.corclan.glow.GlowPicks;
import com.corclan.glow.HolyAura;
import com.corclan.glow.LavaAura;
import com.corclan.glow.RankGlowOverlay;
import com.corclan.glow.SignatureGlowOverlay;
import com.corclan.gz.BroadcastParser;
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
import com.corclan.party.CorGzCounts;
import com.corclan.party.CorLocation;
import com.corclan.party.CorGlowPicks;
import com.corclan.party.CorStaffSettings;
import com.corclan.party.PartyGzBook;
import com.corclan.ui.CorClanOverlay;
import com.corclan.ui.CorClanPanel;
import com.corclan.ui.PanelData;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.lang.reflect.Type;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MessageNode;
import net.runelite.api.Player;
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
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
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
	description = "Clan sidebar, custom clan chat icons, a gz tracker and an opt-in clan party with a clan map for the C o R clan.",
	tags = {"clan", "cor", "gz", "chat", "icons", "social", "party", "map"}
)
public class CorClanPlugin extends Plugin
{
	static final String STATS_KEY = "gzStats";
	static final String WEEKLY_KEY = "gzWeekly";
	static final String WEEK_START_KEY = "gzWeekStart";
	static final String WEEK_RESULTS_KEY = "gzWeekResults";
	static final String PARTY_GZ_KEY = "partyGz";
	/** recent gz times per player, so the hourly cap survives a restart */
	static final String RECENT_GZ_KEY = "gzRecent";
	private static final Type RECENT_GZ_TYPE = new TypeToken<Map<String, List<Long>>>(){}.getType();
	static final String STAFF_UPDATED_KEY = "clanSettingsUpdatedAt";
	private static final Type WEEK_RESULTS_TYPE = new TypeToken<List<WeekResult>>(){}.getType();
	/** Saved stats, not settings: changing them must not trigger a config refresh. */
	private static final List<String> STATS_KEYS = Arrays.asList(STATS_KEY, WEEKLY_KEY, WEEK_START_KEY, WEEK_RESULTS_KEY,
		PARTY_GZ_KEY, STAFF_UPDATED_KEY, RECENT_GZ_KEY);
	private static final String CHAT_BUILD_CALLBACK = "chatMessageBuilding";
	/** Position of the name string relative to the top of the object stack in that callback. */
	private static final int NAME_STACK_OFFSET = 3;
	/** Placeholder default from the first build; replaced by the real invite once seen. */
	private static final String OLD_DISCORD_PLACEHOLDER = "https://discord.gg/";
	private static final Pattern IMG_TAG = Pattern.compile("<img=\\d+>");
	private static final int PANEL_LEADERBOARD_SIZE = 5;
	private static final int PANEL_GIVERS_SIZE = 10;

	// CoR party
	/** Position updates at most every 5 game ticks (~3s), slower in big parties to spare RuneLite's party server. */
	private static final int LOCATION_TICKS = 5;
	/** Resend an unchanged position this often so others know we are still here. */
	private static final long LOCATION_HEARTBEAT_MILLIS = 30_000L;
	/** A clanmate who sent nothing for this long drops off the map. */
	private static final long LOCATION_EXPIRY_MILLIS = 90_000L;
	/** Our own gz totals go to the party at most this often when they changed. */
	private static final long GZ_SEND_MILLIS = 60_000L;
	/** Staff lists longer than this are ignored, so nobody can fill everyone's config. */
	private static final int MAX_STAFF_TEXT = 4000;
	/** Staff edits dated further ahead than this (wrong clock) are ignored. */
	private static final long MAX_CLOCK_SKEW_MILLIS = 600_000L;

	/** Defaults that apply without any config. The "Member icons" config box overrides these. */
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

	private final GzTracker tracker = new GzTracker();
	/** gz counts shared by the CoR party; client thread only */
	private PartyGzBook partyGz = new PartyGzBook();

	/** Rebuilt whenever config changes; read by chat rendering on the client thread. */
	private volatile MemberCosmetics cosmetics = MemberCosmetics.EMPTY;
	/** icons and titles per in-game rank title, from the staff "Clan rank icons" list */
	private volatile MemberCosmetics rankCosmetics = MemberCosmetics.EMPTY;
	/** this week's top 3 givers -> trophy icon; replaced whole when the weekly counts change */
	private volatile Map<String, String> weeklyTrophies = Collections.emptyMap();

	// gz counts shown everywhere: this client's, merged with the CoR party's while in it (client thread)
	private Map<String, Integer> viewGiven = Collections.emptyMap();
	private Map<String, Integer> viewReceived = Collections.emptyMap();
	private Map<String, Integer> viewWeekly = Collections.emptyMap();
	private volatile String gzKing;
	private boolean viewShared;

	// CoR party state, client thread only
	private boolean gzDirty;
	private long lastGzSent;
	/** someone joined, or we did: send our gz counts on the next tick of the party timer */
	private boolean sendGzSoon;
	/** send the staff lists on the next tick (only staff actually send) */
	private boolean sendStaffSoon;
	/** send our glow picks on the next tick (we or someone else joined, or we changed them) */
	private boolean sendGlowSoon;
	/** the side panel currently shows the Owner glow section */
	private boolean panelShowsOwner;
	/** the side panel currently shows the Dev glow section (you are Lavasockz) */
	private boolean panelShowsDev;
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
	/** staff lists as last received, to tell our own writes apart from a staff member's edits */
	private String lastRemoteMemberIcons;
	private String lastRemoteRankIcons;

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
		partyGz = loadPartyGz();
		partyGz.rollWeek(tracker.getWeekStart());
		clientThread.invokeLater(this::updateGzView);
		rebuildCosmetics();
		iconService.ensureRegistered();
		wsClient.registerMessage(CorLocation.class);
		wsClient.registerMessage(CorGzCounts.class);
		wsClient.registerMessage(CorStaffSettings.class);
		wsClient.registerMessage(CorGlowPicks.class);

		panel = new CorClanPanel(config, itemManager, this::resetStats,
			(glow, on) -> configManager.setConfiguration(CorClanConfig.GROUP, glow.configKey, on),
			(glow, on) -> configManager.setConfiguration(CorClanConfig.GROUP, glow.configKey, on),
			this::toggleCorParty);
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
		rankCommands.startUp(() -> viewGiven, () -> rankCosmetics);
		// the CoR party is only ever joined from the panel button: never automatically
		refreshPanel();
		log.debug("CoR Clan started");
	}

	@Override
	protected void shutDown()
	{
		stopSharingLocation();
		if (inCorParty())
		{
			partyService.changeParty(null);
		}
		wsClient.unregisterMessage(CorLocation.class);
		wsClient.unregisterMessage(CorGzCounts.class);
		wsClient.unregisterMessage(CorStaffSettings.class);
		wsClient.unregisterMessage(CorGlowPicks.class);
		clientThread.invoke(glowPicks::clearPartyPicks);
		clientThread.invoke(lavaAura::clearPartyPicks);
		clearPartyLocations();
		overlayManager.remove(overlay);
		overlayManager.remove(rankGlowOverlay);
		overlayManager.remove(signatureGlowOverlay);
		rankCommands.shutDown();
		clientThread.invoke(holyAura::clear);
		clientThread.invoke(lavaAura::clear);
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
		String key = event.getKey();
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
		if ("clanMemberIcons".equals(key) || "clanRankIcons".equals(key))
		{
			String value = normalizeStaffText(event.getNewValue());
			String remote = "clanMemberIcons".equals(key) ? lastRemoteMemberIcons : lastRemoteRankIcons;
			if (!value.equals(remote))
			{
				clientThread.invokeLater(this::onStaffEdit);
			}
		}
		if (GlowEffect.isConfigKey(event.getKey()) || DevGlow.isConfigKey(event.getKey()))
		{
			clientThread.invokeLater(() -> sendGlowSoon = true);
		}
		if ("rankGlow".equals(event.getKey()) && !config.rankGlow())
		{
			clientThread.invoke(lavaAura::clear);
			clientThread.invoke(holyAura::clear);
		}
		rebuildCosmetics();
		clientThread.invokeLater(client::refreshChat);
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
			gzDirty = true;
			updateGzView();
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
				gzDirty = true;
				updateGzView();
				refreshPanel();
				tagGzCount(event.getMessageNode(), sender);
				announce(sender, tracker.getLastGzSubject());
			}
		}
	}

	/** Appends "[GZ count: N]" to the chat line on this client, N being the sender's all-time gz total (party-wide in the CoR party). */
	private void tagGzCount(MessageNode node, String sender)
	{
		if (!config.showGzCount() || node == null)
		{
			return;
		}
		int count = viewGiven.getOrDefault(sender, 0);
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
				+ " (" + viewReceived.getOrDefault(subject, 0) + " total)";
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

	/** The top gz giver: as counted on this client, or across the CoR party when in it. */
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
		if (cmd.equals("myglow"))
		{
			myGlow(event.getArguments());
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
		String scope = viewShared ? "CoR party" : "this client";
		int rhino = iconService.indexFor(ClanIconService.KEY_STAFF);
		int gzIcon = iconService.indexFor(ClanIconService.KEY_GZ_KING);
		int crown = iconService.indexFor(ClanIconService.KEY_FOUNDER);

		ChatMessageBuilder banner = new ChatMessageBuilder();
		icon(banner, rhino);
		banner.append(ChatColorType.HIGHLIGHT).append("CoR Clan").append(ChatColorType.NORMAL)
			.append(" - gz leaderboard (" + scope + ") ");
		icon(banner, rhino);
		say(banner);

		List<Map.Entry<String, Integer>> givers = GzStats.top(viewGiven, 1);
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

		List<Map.Entry<String, Integer>> top = GzStats.top(viewReceived, 3);
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
	 * Icons stack, left to right: member icons (built-in, staff list, own config; or else the icons for their
	 * clan rank), the GZ King badge, this week's trophy, then the rank rhino. Returns "" when there is nothing to show.
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
	 * built-in defaults, the staff "Clan member icons" list, then your own "Member icons".
	 */
	private void rebuildCosmetics()
	{
		cosmetics = MemberCosmetics.build(
			BUILTIN_MEMBER_ICONS,
			BUILTIN_MEMBER_TITLES,
			config.clanMemberIcons() + "\n" + config.memberIcons(),
			iconService::isMemberKey);
		rankCosmetics = MemberCosmetics.build(
			Collections.emptyMap(),
			Collections.emptyMap(),
			config.clanRankIcons(),
			iconService::isMemberKey);
	}

	/**
	 * Client thread: recomputes the counts shown everywhere (this client's, merged with the party's while in the
	 * CoR party), the GZ King and the weekly trophies, and redraws chat if a badge moved.
	 */
	private void updateGzView()
	{
		boolean shared = inCorParty() && !partyGz.isEmpty();
		GzStats allTime = tracker.getAllTime();
		Map<String, Integer> localWeekly = tracker.getWeekly().getGiven();
		viewShared = shared;
		viewGiven = shared ? partyGz.given(allTime.getGiven()) : new HashMap<>(allTime.getGiven());
		viewReceived = shared ? partyGz.received(allTime.getReceived()) : new HashMap<>(allTime.getReceived());
		viewWeekly = shared ? partyGz.weekly(localWeekly) : new HashMap<>(localWeekly);

		List<Map.Entry<String, Integer>> top = GzStats.top(viewGiven, 1);
		String king = top.isEmpty() ? null : top.get(0).getKey();
		GzStats weekly = new GzStats();
		weekly.getGiven().putAll(viewWeekly);
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
		configManager.setConfiguration(CorClanConfig.GROUP, PARTY_GZ_KEY, gson.toJson(partyGz));
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

	private PartyGzBook loadPartyGz()
	{
		String json = configManager.getConfiguration(CorClanConfig.GROUP, PARTY_GZ_KEY);
		if (json == null || json.isEmpty())
		{
			return new PartyGzBook();
		}
		try
		{
			PartyGzBook book = gson.fromJson(json, PartyGzBook.class);
			return book != null ? book.normalized() : new PartyGzBook();
		}
		catch (JsonSyntaxException ex)
		{
			log.debug("Discarding unreadable party gz counts", ex);
			return new PartyGzBook();
		}
	}

	private void resetStats()
	{
		clientThread.invokeLater(() ->
		{
			tracker.resetAllTime();
			tracker.resetSession();
			partyGz.clear();
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
			partyGz.rollWeek(tracker.getWeekStart());
			persistStats();
			updateGzView();
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

		List<Map.Entry<String, Integer>> givers = GzStats.top(viewGiven, PANEL_GIVERS_SIZE);
		List<Map.Entry<String, Integer>> receivers = GzStats.top(viewReceived, PANEL_LEADERBOARD_SIZE);
		List<BroadcastRecord> recent = new ArrayList<>(allTime.getRecent());

		List<Map.Entry<String, Integer>> weeklyGivers = GzStats.top(viewWeekly, PANEL_LEADERBOARD_SIZE);

		// every giver counted (by this client, or the party)
		List<Map.Entry<String, Integer>> allGivers = GzStats.top(viewGiven, Integer.MAX_VALUE);

		String scope = viewShared ? " (CoR party)" : " (this client)";
		panelShowsOwner = ClanRank.OWNER.equals(glowPicks.localRank());
		panelShowsDev = isDev();
		return new PanelData(mine, summary, partyStatus(), mapState.status(partyLocations.size()), scope, givers, receivers, recent,
			weeklyGivers, tracker.getWeekStart(), tracker.longestStreak(), tracker.currentStreak(), allGivers,
			clanRoster(), clanRankTitles(), panelShowsOwner, panelShowsDev, inCorParty());
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

	// ---------------------------------------------------------------- CoR party (opt-in)

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
			updateGzView();
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

	private static boolean isStaff(ClanRank rank)
	{
		return rank != null && rank.getRank() >= ClanRank.ADMINISTRATOR.getRank();
	}

	@Subscribe
	public void onPartyChanged(PartyChanged event)
	{
		clientThread.invokeLater(() ->
		{
			clearPartyLocations();
			glowPicks.clearPartyPicks();
			lavaAura.clearPartyPicks();
			updateGzView();
			// we just joined: the party gets our counts and glow, and staff their lists
			sendGzSoon = true;
			sendStaffSoon = true;
			sendGlowSoon = true;
			refreshPanel();
		});
	}

	@Subscribe
	public void onUserJoin(UserJoin event)
	{
		// the newcomer needs the party's counts and the staff settings
		clientThread.invokeLater(() ->
		{
			PartyMember local = partyService.getLocalMember();
			if (local != null && local.getMemberId() != event.getMemberId())
			{
				sendGzSoon = true;
				sendStaffSoon = true;
				sendGlowSoon = true;
			}
			refreshPanel();
		});
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
			glowPicks.removePartyMember(event.getMemberId());
			lavaAura.removePartyMember(event.getMemberId());
			partySeen.remove(event.getMemberId());
			refreshPanel();
		});
	}

	/**
	 * Every 10 seconds: sends our gz counts when someone joined, or when they changed and the last send was
	 * {@link #GZ_SEND_MILLIS} ago, and the staff lists when asked.
	 */
	@Schedule(period = 10, unit = ChronoUnit.SECONDS)
	public void partyTimer()
	{
		clientThread.invokeLater(() ->
		{
			if (!partyActive())
			{
				return;
			}
			long now = System.currentTimeMillis();
			String me = localPlayerName();
			if (me != null && (sendGzSoon || (gzDirty && now - lastGzSent >= GZ_SEND_MILLIS)))
			{
				// only our own numbers: RuneLite does not allow crowdsourcing data about other players
				partyGz.rollWeek(tracker.getWeekStart());
				partyService.send(partyGz.message(me, tracker.getWeekly(), tracker.getAllTime()));
				gzDirty = false;
				sendGzSoon = false;
				lastGzSent = now;
			}
			if (sendStaffSoon)
			{
				sendStaffSettings();
				sendStaffSoon = false;
			}
			if (sendGlowSoon)
			{
				// rank glow picks and dev glow picks travel together; each side ignores the other's ids
				List<String> picks = new ArrayList<>(glowPicks.localPicks());
				picks.addAll(lavaAura.localPicks());
				partyService.send(new CorGlowPicks(picks));
				sendGlowSoon = false;
			}
		});
	}

	// party messages arrive on the websocket thread; all state lives on the client thread

	@Subscribe
	public void onCorGzCounts(CorGzCounts msg)
	{
		clientThread.invokeLater(() ->
		{
			PartyMember from = sender(msg.getMemberId());
			if (from == null || clanRankOf(from.getDisplayName()) == null)
			{
				return;
			}
			partyGz.rollWeek(tracker.getWeekStart());
			if (partyGz.merge(displayName(from.getDisplayName()), msg, System.currentTimeMillis()))
			{
				persistStats();
				updateGzView();
				refreshPanel();
			}
		});
	}

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

	/** A party member's glow picks; drawn only as far as their clan rank (as we see it) allows. */
	@Subscribe
	public void onCorGlowPicks(CorGlowPicks msg)
	{
		clientThread.invokeLater(() ->
		{
			PartyMember from = sender(msg.getMemberId());
			if (from == null || clanRankOf(from.getDisplayName()) == null)
			{
				return;
			}
			glowPicks.setPartyPicks(msg.getMemberId(), from.getDisplayName(), msg.getGlows());
			lavaAura.setPartyPicks(msg.getMemberId(), from.getDisplayName(), msg.getGlows());
		});
	}

	@Subscribe
	public void onCorStaffSettings(CorStaffSettings msg)
	{
		clientThread.invokeLater(() ->
		{
			PartyMember from = sender(msg.getMemberId());
			if (from == null || !isStaff(clanRankOf(from.getDisplayName())))
			{
				return;
			}
			String members = msg.getMemberIcons() == null ? "" : msg.getMemberIcons();
			String ranks = msg.getRankIcons() == null ? "" : msg.getRankIcons();
			if (members.length() > MAX_STAFF_TEXT || ranks.length() > MAX_STAFF_TEXT
				|| msg.getUpdatedAt() > System.currentTimeMillis() + MAX_CLOCK_SKEW_MILLIS)
			{
				return;
			}
			long ours = staffUpdatedAt();
			if (msg.getUpdatedAt() > ours)
			{
				log.debug("Staff icon settings from {}", from.getDisplayName());
				lastRemoteMemberIcons = normalizeStaffText(members);
				lastRemoteRankIcons = normalizeStaffText(ranks);
				configManager.setConfiguration(CorClanConfig.GROUP, STAFF_UPDATED_KEY, String.valueOf(msg.getUpdatedAt()));
				configManager.setConfiguration(CorClanConfig.GROUP, "clanMemberIcons", members);
				configManager.setConfiguration(CorClanConfig.GROUP, "clanRankIcons", ranks);
				rebuildCosmetics();
				client.refreshChat();
			}
			else if (msg.getUpdatedAt() < ours)
			{
				// they have an older list: if we are staff, ours goes out on the next timer tick
				sendStaffSoon = true;
			}
		});
	}

	/** Client thread: a staff list was edited in this client's settings. */
	private void onStaffEdit()
	{
		if (!isStaff(clanRankOf(localPlayerName())))
		{
			return;
		}
		configManager.setConfiguration(CorClanConfig.GROUP, STAFF_UPDATED_KEY, String.valueOf(System.currentTimeMillis()));
		sendStaffSettings();
	}

	/** Client thread: staff send their lists to the party. */
	private void sendStaffSettings()
	{
		long updatedAt = staffUpdatedAt();
		if (!partyActive() || updatedAt == 0 || !isStaff(clanRankOf(localPlayerName())))
		{
			return;
		}
		partyService.send(new CorStaffSettings(updatedAt, config.clanMemberIcons(), config.clanRankIcons()));
	}

	private long staffUpdatedAt()
	{
		String raw = configManager.getConfiguration(CorClanConfig.GROUP, STAFF_UPDATED_KEY);
		try
		{
			return raw == null ? 0L : Long.parseLong(raw);
		}
		catch (NumberFormatException ex)
		{
			return 0L;
		}
	}

	private static String normalizeStaffText(String text)
	{
		return text == null ? "" : text.replace("\r\n", "\n").trim();
	}

	// ---------------------------------------------------------------- clan map (through the CoR party)

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		holyAura.onClientTick();
		lavaAura.onClientTick();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		holyAura.onGameTick();
		tickLocation();
		// the Owner glow section appears once your clan rank has loaded (and goes if it changes)
		if (panelShowsOwner != ClanRank.OWNER.equals(glowPicks.localRank()) || panelShowsDev != isDev())
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

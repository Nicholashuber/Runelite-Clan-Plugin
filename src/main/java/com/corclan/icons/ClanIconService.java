package com.corclan.icons;

import com.corclan.sync.SyncModels;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.clan.ClanRank;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.util.ImageUtil;

/**
 * Loads the CoR chat icons from resources, registers them with RuneLite's {@link ChatIconManager}
 * exactly once, and maps clan ranks / config keys to the {@code <img=N>} index used in chat.
 *
 * With clan sync on, icons set on the clan server's admin page replace the bundled images in place
 * (or add new icons), and turning sync off puts the bundled images back.
 */
@Slf4j
@Singleton
public class ClanIconService
{
	/** Chat icons are drawn at this size by the game. */
	private static final int ICON_SIZE = 11;

	/** Red rhino: owner, deputy owner and administrators. */
	public static final String KEY_STAFF = "staff";
	/** Blue rhino: every other member and guests. */
	public static final String KEY_MEMBER = "member";
	public static final String KEY_GZ_KING = "gzking";

	private static final List<String> RANK_KEYS = Arrays.asList(KEY_STAFF, KEY_MEMBER);

	public static final String KEY_FOUNDER = "founder";
	public static final String KEY_DEV = "dev";

	/** Built-in keys that can be given to players. Server icons with new names are added to these. */
	public static final List<String> MEMBER_KEYS = Collections.unmodifiableList(Arrays.asList(
		"crown", "trophy", "star", "skull", "gem", "fire", KEY_FOUNDER, KEY_DEV));

	/** Same rule the server enforces for icon names. */
	private static final Pattern ICON_KEY = Pattern.compile("[a-z0-9][a-z0-9_-]{0,23}");
	/** Caps how many extra icons the server can register on this client. */
	private static final int MAX_CUSTOM_ICONS = 64;

	/** Absolute path: this class sits in com.corclan.icons but the PNGs live in com/corclan/. */
	private static final String RESOURCE_DIR = "/com/corclan/";

	private final ChatIconManager chatIconManager;
	/** icon key -> ChatIconManager id; read on the client thread while the server icons are applied */
	private final Map<String, Integer> iconIds = new ConcurrentHashMap<>();
	private final Map<String, BufferedImage> bundled = new HashMap<>();
	/** icon key -> hash of the server image currently shown instead of the bundled one */
	private final Map<String, String> serverHashes = new HashMap<>();
	/** server icons with new names, which players can be given */
	private final Set<String> customKeys = ConcurrentHashMap.newKeySet();
	/** clan rank -> icon picked on the clan server; replaced whole, read on the client thread */
	private volatile Map<Integer, String> rankOverrides = Collections.emptyMap();

	@Inject
	public ClanIconService(ChatIconManager chatIconManager)
	{
		this.chatIconManager = chatIconManager;
	}

	/**
	 * Registers all bundled icons with the chat icon manager. Safe to call repeatedly; the ids are cached
	 * for the lifetime of the client so toggling the plugin never registers duplicates.
	 */
	public synchronized void ensureRegistered()
	{
		if (!bundled.isEmpty())
		{
			return;
		}
		for (String key : RANK_KEYS)
		{
			register(key, "rank_" + key + ".png");
		}
		for (String key : MEMBER_KEYS)
		{
			register(key, "member_" + key + ".png");
		}
		register(KEY_GZ_KING, "member_gzking.png");
		for (String key : WeeklyTrophies.KEYS)
		{
			register(key, key + ".png");
		}
		log.debug("Registered {} CoR chat icons", iconIds.size());
	}

	private void register(String key, String resource)
	{
		try
		{
			BufferedImage img = fit(ImageUtil.loadImageResource(ClanIconService.class, RESOURCE_DIR + resource));
			bundled.put(key, img);
			iconIds.put(key, chatIconManager.registerChatIcon(img));
		}
		catch (RuntimeException ex)
		{
			log.warn("Could not load CoR icon {}", resource, ex);
		}
	}

	/** Scales to the chat icon height, keeping the width proportional (wordmarks are wider). */
	private static BufferedImage fit(BufferedImage img)
	{
		if (img.getHeight() == ICON_SIZE)
		{
			return img;
		}
		int width = Math.max(1, Math.round(img.getWidth() * (float) ICON_SIZE / img.getHeight()));
		return ImageUtil.resizeImage(img, width, ICON_SIZE);
	}

	/**
	 * Applies the icon images from the clan server. Call on the client thread.
	 *
	 * @return true if any icon changed, so chat should be redrawn
	 */
	public synchronized boolean applyServerIcons(List<SyncModels.IconData> icons)
	{
		boolean changed = false;
		Set<String> seen = new HashSet<>();
		for (SyncModels.IconData icon : icons)
		{
			String key = icon.getKey();
			if (key == null || !ICON_KEY.matcher(key).matches() || !seen.add(key))
			{
				continue;
			}
			if (icon.getHash() != null && icon.getHash().equals(serverHashes.get(key)))
			{
				continue;
			}
			boolean isNew = !iconIds.containsKey(key);
			if (isNew && customKeys.size() >= MAX_CUSTOM_ICONS)
			{
				continue;
			}
			BufferedImage img = IconDecoder.decode(icon.getPng());
			if (img == null)
			{
				log.debug("CoR sync: skipped unreadable icon {}", key);
				continue;
			}
			img = fit(img);
			if (isNew)
			{
				iconIds.put(key, chatIconManager.registerChatIcon(img));
			}
			else
			{
				chatIconManager.updateChatIcon(iconIds.get(key), img);
			}
			if (!bundled.containsKey(key))
			{
				customKeys.add(key);
			}
			serverHashes.put(key, icon.getHash() == null ? "" : icon.getHash());
			changed = true;
		}

		// icons removed on the server: bundled ones get their image back, custom ones stop being usable
		for (String key : new HashSet<>(serverHashes.keySet()))
		{
			if (!seen.contains(key))
			{
				restore(key);
				changed = true;
			}
		}
		return changed;
	}

	/** Puts every bundled image back and forgets the server's icons. Call on the client thread. */
	public synchronized boolean clearServerIcons()
	{
		boolean changed = !serverHashes.isEmpty() || !rankOverrides.isEmpty();
		rankOverrides = Collections.emptyMap();
		for (String key : new HashSet<>(serverHashes.keySet()))
		{
			restore(key);
		}
		return changed;
	}

	private void restore(String key)
	{
		serverHashes.remove(key);
		BufferedImage original = bundled.get(key);
		Integer id = iconIds.get(key);
		if (original != null && id != null)
		{
			chatIconManager.updateChatIcon(id, original);
		}
		customKeys.remove(key);
	}

	/**
	 * @return the mod icon index for an icon key, or -1 if that icon is unknown or the client has
	 * not finished loading its chat icons yet
	 */
	public int indexFor(String key)
	{
		if (key == null)
		{
			return -1;
		}
		Integer id = iconIds.get(key);
		if (id == null || (!bundled.containsKey(key) && !customKeys.contains(key)))
		{
			return -1;
		}
		return chatIconManager.chatIconIndex(id);
	}

	/** @return the {@code <img=N>} tag for an icon key, or null when {@link #indexFor} is -1 */
	public String tagFor(String key)
	{
		int index = indexFor(key);
		return index < 0 ? null : "<img=" + index + ">";
	}

	/** Built-in member icons plus any new icons from the clan server. */
	public boolean isMemberKey(String key)
	{
		return key != null && (MEMBER_KEYS.contains(key) || customKeys.contains(key));
	}

	/**
	 * Applies the icon picked per clan rank on the clan server. Ranks not listed use the default rule.
	 *
	 * @return true if anything changed
	 */
	public boolean applyRankIcons(List<SyncModels.RankIcon> picks)
	{
		Map<Integer, String> next = new HashMap<>();
		for (SyncModels.RankIcon pick : picks)
		{
			String icon = pick.getIcon();
			if (icon != null && (RankIcons.NO_ICON.equals(icon) || ICON_KEY.matcher(icon).matches()))
			{
				next.put(pick.getRank(), icon);
			}
		}
		if (next.equals(rankOverrides))
		{
			return false;
		}
		rankOverrides = Collections.unmodifiableMap(next);
		return true;
	}

	/** Icon key to draw for a clan rank, or null for none. J-Mods keep their own icon unless the server says otherwise. */
	public String iconForRank(ClanRank rank)
	{
		if (rank == null)
		{
			return null;
		}
		return RankIcons.resolve(rank.getRank(), rankOverrides, k -> bundled.containsKey(k) || customKeys.contains(k));
	}
}

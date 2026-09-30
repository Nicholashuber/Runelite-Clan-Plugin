package com.corclan.icons;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.clan.ClanRank;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.util.ImageUtil;

/**
 * Loads the CoR chat icons from resources, registers them with RuneLite's {@link ChatIconManager}
 * exactly once, and maps clan ranks / config keys to the {@code <img=N>} index used in chat.
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

	/** Built-in keys that can be given to players in the "Member icons" config. */
	public static final List<String> MEMBER_KEYS = Collections.unmodifiableList(Arrays.asList(
		"crown", "trophy", "star", "skull", "gem", "fire", KEY_FOUNDER, KEY_DEV));

	/** Absolute path: this class sits in com.corclan.icons but the PNGs live in com/corclan/. */
	private static final String RESOURCE_DIR = "/com/corclan/";

	private final ChatIconManager chatIconManager;
	/** icon key -> ChatIconManager id */
	private final Map<String, Integer> iconIds = new ConcurrentHashMap<>();
	private final Map<String, BufferedImage> bundled = new HashMap<>();

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
		if (id == null)
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

	public boolean isMemberKey(String key)
	{
		return key != null && MEMBER_KEYS.contains(key);
	}

	/** Icon key to draw for a clan rank, or null for none. J-Mods keep their own icon. */
	public String iconForRank(ClanRank rank)
	{
		if (rank == null)
		{
			return null;
		}
		return RankIcons.defaultIcon(rank.getRank());
	}
}

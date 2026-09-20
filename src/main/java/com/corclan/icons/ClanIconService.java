package com.corclan.icons;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.clan.ClanRank;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.util.ImageUtil;

/**
 * Loads the CoR chat icons from resources, registers them with RuneLite's {@link ChatIconManager}
 * exactly once, and maps clan ranks / config keys to the {@code <img=N>} index used in chat.
 * Everything here is local to this client; nothing is sent anywhere.
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

	/** Keys a clan admin can use in the "Member icons" config box. */
	public static final List<String> MEMBER_KEYS = Collections.unmodifiableList(Arrays.asList(
		"crown", "trophy", "star", "skull", "gem", "fire", KEY_FOUNDER));

	private final ChatIconManager chatIconManager;
	private final Map<String, Integer> iconIds = new HashMap<>();

	@Inject
	public ClanIconService(ChatIconManager chatIconManager)
	{
		this.chatIconManager = chatIconManager;
	}

	/**
	 * Registers all icons with the chat icon manager. Safe to call repeatedly; the ids are cached
	 * for the lifetime of the client so toggling the plugin never registers duplicates.
	 */
	public synchronized void ensureRegistered()
	{
		if (!iconIds.isEmpty())
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
		log.debug("Registered {} CoR chat icons", iconIds.size());
	}

	/** Absolute path: this class sits in com.corclan.icons but the PNGs live in com/corclan/. */
	private static final String RESOURCE_DIR = "/com/corclan/";

	private void register(String key, String resource)
	{
		try
		{
			BufferedImage img = ImageUtil.loadImageResource(ClanIconService.class, RESOURCE_DIR + resource);
			if (img.getHeight() != ICON_SIZE)
			{
				// keep width proportional: wordmark icons may be wider than they are tall
				int width = Math.max(1, Math.round(img.getWidth() * (float) ICON_SIZE / img.getHeight()));
				img = ImageUtil.resizeImage(img, width, ICON_SIZE);
			}
			iconIds.put(key, chatIconManager.registerChatIcon(img));
		}
		catch (RuntimeException ex)
		{
			log.warn("Could not load CoR icon {}", resource, ex);
		}
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

	/**
	 * Maps a clan rank to a rank icon key. Clan ranks are ints: guests are negative, normal member
	 * ranks count up from 0, administrators are 100+, deputy owner 125, owner 126. J-Mods (127)
	 * keep their own icon.
	 */
	public static String rankKey(ClanRank rank)
	{
		if (rank == null || ClanRank.JMOD.equals(rank))
		{
			return null;
		}
		return rank.getRank() >= ClanRank.ADMINISTRATOR.getRank() ? KEY_STAFF : KEY_MEMBER;
	}
}

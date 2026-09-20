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

	public static final String KEY_OWNER = "owner";
	public static final String KEY_DEPUTY_OWNER = "deputy_owner";
	public static final String KEY_ADMINISTRATOR = "administrator";
	public static final String KEY_HIGH = "high";
	public static final String KEY_MEDIUM = "medium";
	public static final String KEY_LOW = "low";
	public static final String KEY_GUEST = "guest";
	public static final String KEY_GZ_KING = "gzking";

	private static final List<String> RANK_KEYS = Arrays.asList(
		KEY_OWNER, KEY_DEPUTY_OWNER, KEY_ADMINISTRATOR, KEY_HIGH, KEY_MEDIUM, KEY_LOW, KEY_GUEST);

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

	private void register(String key, String resource)
	{
		try
		{
			BufferedImage img = ImageUtil.loadImageResource(ClanIconService.class, resource);
			if (img.getWidth() != ICON_SIZE || img.getHeight() != ICON_SIZE)
			{
				img = ImageUtil.resizeImage(img, ICON_SIZE, ICON_SIZE);
			}
			iconIds.put(key, chatIconManager.registerChatIcon(img));
		}
		catch (RuntimeException ex)
		{
			log.debug("Could not load CoR icon {}", resource, ex);
		}
	}

	/**
	 * @return the {@code <img=N>} tag for an icon key, or null if that icon is unknown or the
	 * client has not finished loading its chat icons yet
	 */
	public String tagFor(String key)
	{
		if (key == null)
		{
			return null;
		}
		Integer id = iconIds.get(key);
		if (id == null)
		{
			return null;
		}
		int index = chatIconManager.chatIconIndex(id);
		if (index < 0)
		{
			return null;
		}
		return "<img=" + index + ">";
	}

	public boolean isMemberKey(String key)
	{
		return key != null && MEMBER_KEYS.contains(key);
	}

	/**
	 * Maps a clan rank to one of the rank icon keys. Clan ranks are ints: guests are negative,
	 * normal member ranks count up from 0, administrators are 100+, deputy owner 125, owner 126.
	 * J-Mods (127) keep their own icon.
	 */
	public static String rankKey(ClanRank rank)
	{
		if (rank == null)
		{
			return null;
		}
		if (ClanRank.JMOD.equals(rank))
		{
			return null;
		}
		if (ClanRank.OWNER.equals(rank))
		{
			return KEY_OWNER;
		}
		if (ClanRank.DEPUTY_OWNER.equals(rank))
		{
			return KEY_DEPUTY_OWNER;
		}
		int r = rank.getRank();
		if (r >= ClanRank.ADMINISTRATOR.getRank())
		{
			return KEY_ADMINISTRATOR;
		}
		if (r < 0)
		{
			return KEY_GUEST;
		}
		if (r >= 6)
		{
			return KEY_HIGH;
		}
		if (r >= 3)
		{
			return KEY_MEDIUM;
		}
		return KEY_LOW;
	}
}

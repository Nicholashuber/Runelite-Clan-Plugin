package com.corclan.sync;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * JSON shapes exchanged with the CoR clan server (see the cor-clan-api README, "API for the plugins").
 * Serialized with RuneLite's Gson (nulls are omitted). Everything a client sends is about its own player.
 */
public final class SyncModels
{
	private SyncModels()
	{
	}

	/** Who is reporting: RuneLite's account hash, the character's name and the clan it is in. */
	public static final class Reporter
	{
		private final String accountHash;
		private final String rsn;
		private final String clan;

		public Reporter(String accountHash, String rsn, String clan)
		{
			this.accountHash = accountHash;
			this.rsn = rsn;
			this.clan = clan;
		}

		public String getRsn()
		{
			return rsn;
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof Reporter))
			{
				return false;
			}
			Reporter other = (Reporter) o;
			return Objects.equals(accountHash, other.accountHash) && Objects.equals(rsn, other.rsn)
				&& Objects.equals(clan, other.clan);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(accountHash, rsn, clan);
		}
	}

	/**
	 * A gz the reporter said, or a clan broadcast about the reporter. The server takes the player from the
	 * reporter, so no name is sent with the event.
	 */
	public static final class Event
	{
		private final String type;
		private final String text;
		private final long at;

		private Event(String type, String text, long at)
		{
			this.type = type;
			this.text = text;
			this.at = at;
		}

		public static Event broadcast(String text, long at)
		{
			return new Event("broadcast", text, at);
		}

		public static Event gz(String text, long at)
		{
			return new Event("gz", text, at);
		}

		public String getType()
		{
			return type;
		}

		public long getAt()
		{
			return at;
		}
	}

	public static final class ReportPayload
	{
		private final Reporter reporter;
		/** this client's clock when sending; the server uses it to correct event times */
		private final long sentAt;
		private final List<Event> events;

		public ReportPayload(Reporter reporter, long sentAt, List<Event> events)
		{
			this.reporter = reporter;
			this.sentAt = sentAt;
			this.events = events;
		}
	}

	/** The reporter's own clan rank and glow picks. A null field is left out, which keeps what the server has. */
	public static final class ProfilePayload
	{
		private final Reporter reporter;
		private final Integer rank;
		private final List<String> glows;
		/** our own "Let leaders shame me" */
		private final Boolean allowShame;

		public ProfilePayload(Reporter reporter, Integer rank, List<String> glows, Boolean allowShame)
		{
			this.reporter = reporter;
			this.rank = rank;
			this.glows = glows;
			this.allowShame = allowShame;
		}
	}

	/** A clan rank number and the clan's own name for it. No player names. */
	public static final class RankTitle
	{
		private final int rank;
		private final String title;

		public RankTitle(int rank, String title)
		{
			this.rank = rank;
			this.title = title;
		}
	}

	public static final class RanksPayload
	{
		private final Reporter reporter;
		private final List<RankTitle> ranks;

		public RanksPayload(Reporter reporter, List<RankTitle> ranks)
		{
			this.reporter = reporter;
			this.ranks = ranks;
		}
	}

	/** A clan leader sets the announcement banner; an empty text clears it. */
	public static final class AnnouncementPayload
	{
		private final Reporter reporter;
		private final String text;

		public AnnouncementPayload(Reporter reporter, String text)
		{
			this.reporter = reporter;
			this.text = text;
		}
	}

	/** The banner in {@code GET /v1/clan}, as the clan server sends it. */
	public static final class AnnouncementData
	{
		private String text;
		private String by;
		/** ISO-8601 instant */
		private String updatedAt;

		public String getText()
		{
			return text;
		}

		public String getBy()
		{
			return by;
		}

		public String getUpdatedAt()
		{
			return updatedAt;
		}
	}

	/** One of the latest promotions in {@code GET /v1/clan}, as the clan server sends it. */
	public static final class PromotionData
	{
		private String rsn;
		/** the clan's rank titles */
		private String from;
		private String to;
		/** ISO-8601 instant */
		private String at;

		public String getRsn()
		{
			return rsn;
		}

		public String getFrom()
		{
			return from;
		}

		public String getTo()
		{
			return to;
		}

		public String getAt()
		{
			return at;
		}
	}

	/** Our own character's daily gm line, for the gm bonus. */
	public static final class GmPayload
	{
		private final Reporter reporter;
		private final String text;

		public GmPayload(Reporter reporter, String text)
		{
			this.reporter = reporter;
			this.text = text;
		}
	}

	/** The clan server's answer to a daily gm. */
	public static final class GmResult
	{
		private boolean awarded;
		private int pointsEarned;
		private int points;

		/** true when this gm earned the day's bonus */
		public boolean isAwarded()
		{
			return awarded;
		}

		public int getPointsEarned()
		{
			return pointsEarned;
		}

		/** our TCG points afterwards */
		public int getPoints()
		{
			return points;
		}
	}

	/** ::shame (or with {@code clear}, ::unshame) from one of the clan leaders, naming the clanmate to shame. */
	public static final class ShamePayload
	{
		private final Reporter reporter;
		private final String target;
		private final boolean clear;

		public ShamePayload(Reporter reporter, String target, boolean clear)
		{
			this.reporter = reporter;
			this.target = target;
			this.clear = clear;
		}
	}

	/** One player wearing the Kephri dung in {@code GET /v1/shames}. */
	public static final class ActiveShame
	{
		private String rsn;
		private String by;

		public ActiveShame()
		{
		}

		public ActiveShame(String rsn, String by)
		{
			this.rsn = rsn;
			this.by = by;
		}

		public String getRsn()
		{
			return rsn;
		}

		public String getBy()
		{
			return by;
		}
	}

	public static final class ShamesResponse
	{
		private List<ActiveShame> shames;

		public List<ActiveShame> getShames()
		{
			return shames == null ? Collections.emptyList() : shames;
		}
	}

	/** Our own raid finder listing: one raid, slots filled out of the party size, our world and a short note. */
	public static final class RaidPayload
	{
		private final Reporter reporter;
		private final String raid;
		private final int filled;
		private final int size;
		/** null when unknown */
		private final Integer world;
		private final String note;
		/** we are inside the raid with our party */
		private final boolean raiding;

		public RaidPayload(Reporter reporter, String raid, int filled, int size, Integer world, String note, boolean raiding)
		{
			this.raiding = raiding;
			this.reporter = reporter;
			this.raid = raid;
			this.filled = filled;
			this.size = size;
			this.world = world;
			this.note = note;
		}
	}

	/** Takes our own raid finder listing down. */
	public static final class RaidStopPayload
	{
		private final Reporter reporter;

		public RaidStopPayload(Reporter reporter)
		{
			this.reporter = reporter;
		}
	}

	/** One open group in {@code GET /v1/raids}. */
	public static final class RaidListingData
	{
		private String rsn;
		private String raid;
		private int filled;
		private int size;
		private Integer world;
		private String note;
		/** ISO-8601 instant the group went up */
		private String postedAt;
		/** ISO-8601 instant the host last sent it */
		private String updatedAt;
		/** the host is inside the raid with their party */
		private boolean raiding;

		public boolean isRaiding()
		{
			return raiding;
		}

		public String getRsn()
		{
			return rsn;
		}

		public String getRaid()
		{
			return raid;
		}

		public int getFilled()
		{
			return filled;
		}

		public int getSize()
		{
			return size;
		}

		public Integer getWorld()
		{
			return world;
		}

		public String getNote()
		{
			return note;
		}

		public String getPostedAt()
		{
			return postedAt;
		}

		public String getUpdatedAt()
		{
			return updatedAt;
		}
	}

	public static final class RaidsResponse
	{
		private List<RaidListingData> raids;

		public List<RaidListingData> getRaids()
		{
			return raids == null ? Collections.emptyList() : raids;
		}
	}

	/** The body of a refused request, e.g. {@code { "error": "name_taken" }}. */
	public static final class ErrorBody
	{
		private String error;
		/** with {@code ping_cooldown}: how long until the next Discord ping */
		private Integer retryAfterSeconds;

		public String getError()
		{
			return error;
		}

		public int getRetryAfterSeconds()
		{
			return retryAfterSeconds == null ? 0 : retryAfterSeconds;
		}
	}

	/** One player in {@code GET /v1/clan}: what their own client reported, plus what admins gave them. */
	public static final class ClanPlayer
	{
		private String rsn;
		/** null until that player's own client has reported it */
		private Integer rank;
		private int given;
		private int received;
		private int weeklyGiven;
		private int weeklyReceived;
		private List<String> icons;
		private String title;
		private List<String> glows;

		public ClanPlayer()
		{
		}

		public ClanPlayer(String rsn, List<String> icons, String title)
		{
			this.rsn = rsn;
			this.icons = icons;
			this.title = title;
		}

		public String getRsn()
		{
			return rsn;
		}

		public Integer getRank()
		{
			return rank;
		}

		public int getGiven()
		{
			return given;
		}

		public int getReceived()
		{
			return received;
		}

		public int getWeeklyGiven()
		{
			return weeklyGiven;
		}

		public List<String> getIcons()
		{
			return icons == null ? Collections.emptyList() : icons;
		}

		public String getTitle()
		{
			return title;
		}

		/** @return the glow ids they picked, or null when the server sent none */
		public List<String> getGlows()
		{
			return glows;
		}
	}

	public static final class Entry
	{
		private String rsn;
		private int count;

		public String getRsn()
		{
			return rsn;
		}

		public int getCount()
		{
			return count;
		}
	}

	/** A closed week and its top three gz givers, in order. */
	public static final class Week
	{
		/** ISO-8601 instant of the Sunday 00:00 UTC that began the week */
		private String weekStart;
		private List<Entry> top;

		public String getWeekStart()
		{
			return weekStart;
		}

		public List<Entry> getTop()
		{
			return top == null ? Collections.emptyList() : top;
		}
	}

	public static final class ClanResponse
	{
		/** ISO-8601 instant of the Sunday 00:00 UTC that began the week the weekly counts belong to */
		private String weekStart;
		private List<ClanPlayer> players;
		private List<Week> weeks;
		/** the banner clan leaders set, or null */
		private AnnouncementData announcement;
		/** the newest few promotions, newest first; missing on older servers */
		private List<PromotionData> promotions;

		public AnnouncementData getAnnouncement()
		{
			return announcement;
		}

		public List<PromotionData> getPromotions()
		{
			return promotions == null ? Collections.emptyList() : promotions;
		}

		public String getWeekStart()
		{
			return weekStart;
		}

		public List<ClanPlayer> getPlayers()
		{
			return players == null ? Collections.emptyList() : players;
		}

		public List<Week> getWeeks()
		{
			return weeks == null ? Collections.emptyList() : weeks;
		}
	}

	/** A chat icon image from the clan server's admin page: a small PNG sent as base64. */
	public static final class IconData
	{
		private String key;
		private String hash;
		private String png;

		public IconData()
		{
		}

		public IconData(String key, String hash, String png)
		{
			this.key = key;
			this.hash = hash;
			this.png = png;
		}

		public String getKey()
		{
			return key;
		}

		public String getHash()
		{
			return hash;
		}

		public String getPng()
		{
			return png;
		}
	}

	/** The icon picked on the admin page for one clan rank ("none" = no icon). */
	public static final class RankIcon
	{
		private int rank;
		private String icon;

		public RankIcon()
		{
		}

		public RankIcon(int rank, String icon)
		{
			this.rank = rank;
			this.icon = icon;
		}

		public int getRank()
		{
			return rank;
		}

		public String getIcon()
		{
			return icon;
		}
	}

	public static final class IconsResponse
	{
		private List<IconData> icons;
		private List<RankIcon> rankIcons;

		public List<IconData> getIcons()
		{
			return icons == null ? Collections.emptyList() : icons;
		}

		public List<RankIcon> getRankIcons()
		{
			return rankIcons == null ? Collections.emptyList() : rankIcons;
		}
	}
}

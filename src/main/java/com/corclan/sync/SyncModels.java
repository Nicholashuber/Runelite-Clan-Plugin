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

		public ProfilePayload(Reporter reporter, Integer rank, List<String> glows)
		{
			this.reporter = reporter;
			this.rank = rank;
			this.glows = glows;
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

	/** The body of a refused request, e.g. {@code { "error": "name_taken" }}. */
	public static final class ErrorBody
	{
		private String error;

		public String getError()
		{
			return error;
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

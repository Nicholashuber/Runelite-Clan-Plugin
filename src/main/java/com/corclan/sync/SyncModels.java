package com.corclan.sync;

import java.util.Collections;
import java.util.List;

/** JSON shapes exchanged with the CoR clan server. Serialized with RuneLite's Gson (nulls are omitted). */
public final class SyncModels
{
	private SyncModels()
	{
	}

	/** Who is reporting. The account hash lets the server merge reports from the same client. */
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

		public String getAccountHash()
		{
			return accountHash;
		}
	}

	/** A clan broadcast or a gz line seen in clan chat. Only one of subject / giver is set. */
	public static final class Event
	{
		private final String type;
		private final String subject;
		private final String giver;
		private final String text;
		private final long at;

		private Event(String type, String subject, String giver, String text, long at)
		{
			this.type = type;
			this.subject = subject;
			this.giver = giver;
			this.text = text;
			this.at = at;
		}

		public static Event broadcast(String subject, String text, long at)
		{
			return new Event("broadcast", subject, null, text, at);
		}

		public static Event gz(String giver, String text, long at)
		{
			return new Event("gz", null, giver, text, at);
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

	public static final class Entry
	{
		private String rsn;
		private int count;

		public Entry()
		{
		}

		public Entry(String rsn, int count)
		{
			this.rsn = rsn;
			this.count = count;
		}

		public String getRsn()
		{
			return rsn;
		}

		public int getCount()
		{
			return count;
		}
	}

	public static final class Leaderboard
	{
		private List<Entry> givers;
		private List<Entry> receivers;

		public List<Entry> getGivers()
		{
			return givers == null ? Collections.emptyList() : givers;
		}

		public List<Entry> getReceivers()
		{
			return receivers == null ? Collections.emptyList() : receivers;
		}
	}

	public static final class Cosmetic
	{
		private String rsn;
		private List<String> icons;
		private String title;

		public Cosmetic()
		{
		}

		public Cosmetic(String rsn, List<String> icons, String title)
		{
			this.rsn = rsn;
			this.icons = icons;
			this.title = title;
		}

		public String getRsn()
		{
			return rsn;
		}

		public List<String> getIcons()
		{
			return icons == null ? Collections.emptyList() : icons;
		}

		public String getTitle()
		{
			return title;
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

	public static final class IconsResponse
	{
		private List<IconData> icons;

		public List<IconData> getIcons()
		{
			return icons == null ? Collections.emptyList() : icons;
		}
	}

	public static final class CosmeticsResponse
	{
		private List<Cosmetic> players;

		public List<Cosmetic> getPlayers()
		{
			return players == null ? Collections.emptyList() : players;
		}
	}
}

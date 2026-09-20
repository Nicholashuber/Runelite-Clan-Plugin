package com.corclan.gz;

/**
 * Attributes gz messages to the most recent clan broadcast while its window is open.
 * Pure Java: the plugin feeds it chat events and persists {@link #getAllTime()} when
 * {@link #onClanChat} reports a change.
 */
public class GzTracker
{
	private GzStats allTime = new GzStats();
	private GzStats session = new GzStats();
	private BroadcastRecord current;

	// last counted gz, for the on-screen indicator
	private long lastGzTime;
	private String lastGzGiver;
	private String lastGzSubject;

	public long getLastGzTime()
	{
		return lastGzTime;
	}

	public String getLastGzGiver()
	{
		return lastGzGiver;
	}

	/** Who the last gz was credited to, or null if no broadcast window was open. */
	public String getLastGzSubject()
	{
		return lastGzSubject;
	}

	public void load(GzStats stats)
	{
		allTime = stats != null ? stats : new GzStats();
		session = new GzStats();
		current = null;
	}

	public GzStats getAllTime()
	{
		return allTime;
	}

	public GzStats getSession()
	{
		return session;
	}

	public void resetAllTime()
	{
		allTime = new GzStats();
		current = null;
	}

	public void resetSession()
	{
		session = new GzStats();
	}

	/** @return the broadcast currently accepting gz's, or null if none / expired */
	public BroadcastRecord currentWindow(long now, long windowMillis)
	{
		if (current != null && current.isOpen(now, windowMillis))
		{
			return current;
		}
		return null;
	}

	/**
	 * @param subject the member being congratulated (from {@link BroadcastParser#subjectOf})
	 * @return the record that now accepts gz's
	 */
	public BroadcastRecord onBroadcast(String subject, String text, long now)
	{
		BroadcastRecord record = new BroadcastRecord(subject, text, now);
		current = record;
		allTime.addRecent(record);
		session.addRecent(record);
		return record;
	}

	/**
	 * @return true if any counter changed (caller should persist + refresh UI)
	 */
	public boolean onClanChat(String sender, String message, long now, Settings settings)
	{
		if (sender == null || sender.isEmpty() || !GzDetector.isGz(message, settings.maxMessageLength))
		{
			return false;
		}
		allTime.addGiven(sender);
		session.addGiven(sender);
		lastGzTime = now;
		lastGzGiver = sender;
		lastGzSubject = null;

		BroadcastRecord window = currentWindow(now, settings.windowMillis);
		if (window != null
			&& !sender.equalsIgnoreCase(window.getSubject())
			&& (!settings.oneGzPerPersonPerBroadcast || !window.getGivers().contains(sender)))
		{
			window.addGz(sender);
			allTime.addReceived(window.getSubject());
			session.addReceived(window.getSubject());
			lastGzSubject = window.getSubject();
		}
		return true;
	}

	/** Snapshot of the config values the tracker needs. */
	public static final class Settings
	{
		public final long windowMillis;
		public final boolean oneGzPerPersonPerBroadcast;
		public final int maxMessageLength;

		public Settings(long windowMillis, boolean oneGzPerPersonPerBroadcast, int maxMessageLength)
		{
			this.windowMillis = windowMillis;
			this.oneGzPerPersonPerBroadcast = oneGzPerPersonPerBroadcast;
			this.maxMessageLength = maxMessageLength;
		}
	}
}

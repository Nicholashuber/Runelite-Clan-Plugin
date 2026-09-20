package com.corclan.gz;

import java.util.HashSet;
import java.util.Set;

/** One clan broadcast and the gz's that were attributed to it. Serialized with Gson. */
public class BroadcastRecord
{
	private String subject;
	private String text;
	private long timestamp;
	private int gzCount;
	private Set<String> givers = new HashSet<>();

	public BroadcastRecord()
	{
	}

	public BroadcastRecord(String subject, String text, long timestamp)
	{
		this.subject = subject;
		this.text = text;
		this.timestamp = timestamp;
	}

	public String getSubject()
	{
		return subject;
	}

	public String getText()
	{
		return text;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public int getGzCount()
	{
		return gzCount;
	}

	public Set<String> getGivers()
	{
		if (givers == null)
		{
			givers = new HashSet<>();
		}
		return givers;
	}

	void addGz(String giver)
	{
		gzCount++;
		getGivers().add(giver);
	}

	public boolean isOpen(long now, long windowMillis)
	{
		return now - timestamp <= windowMillis;
	}
}

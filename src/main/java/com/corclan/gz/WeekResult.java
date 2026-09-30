package com.corclan.gz;

import java.util.ArrayList;
import java.util.List;

/** The #1 gz giver(s) of one finished week. Serialized with Gson. */
public class WeekResult
{
	private long weekStart;
	private List<String> winners = new ArrayList<>();

	public WeekResult()
	{
	}

	public WeekResult(long weekStart, List<String> winners)
	{
		this.weekStart = weekStart;
		this.winners = new ArrayList<>(winners);
	}

	/** Epoch millis of the Sunday 00:00 that began the week. */
	public long getWeekStart()
	{
		return weekStart;
	}

	/** Everyone tied for the most gz's given that week. */
	public List<String> getWinners()
	{
		if (winners == null)
		{
			winners = new ArrayList<>();
		}
		return winners;
	}
}

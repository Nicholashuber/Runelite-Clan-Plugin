package com.corclan.gz;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Counters for gz's given and received. Serialized to RuneLite config as JSON with Gson. */
public class GzStats
{
	public static final int MAX_RECENT = 10;

	private Map<String, Integer> given = new HashMap<>();
	private Map<String, Integer> received = new HashMap<>();
	private List<BroadcastRecord> recent = new ArrayList<>();

	public Map<String, Integer> getGiven()
	{
		if (given == null)
		{
			given = new HashMap<>();
		}
		return given;
	}

	public Map<String, Integer> getReceived()
	{
		if (received == null)
		{
			received = new HashMap<>();
		}
		return received;
	}

	public List<BroadcastRecord> getRecent()
	{
		if (recent == null)
		{
			recent = new ArrayList<>();
		}
		return recent;
	}

	void addGiven(String name)
	{
		getGiven().merge(name, 1, Integer::sum);
	}

	void addReceived(String name)
	{
		getReceived().merge(name, 1, Integer::sum);
	}

	void addRecent(BroadcastRecord record)
	{
		List<BroadcastRecord> list = getRecent();
		list.add(0, record);
		while (list.size() > MAX_RECENT)
		{
			list.remove(list.size() - 1);
		}
	}

	public int totalGiven()
	{
		return getGiven().values().stream().mapToInt(Integer::intValue).sum();
	}

	public int totalReceived()
	{
		return getReceived().values().stream().mapToInt(Integer::intValue).sum();
	}

	/** Top entries, highest first, ties broken by name. */
	public static List<Map.Entry<String, Integer>> top(Map<String, Integer> counts, int limit)
	{
		return counts.entrySet().stream()
			.sorted(Comparator.comparing((Map.Entry<String, Integer> e) -> e.getValue()).reversed()
				.thenComparing(Map.Entry::getKey))
			.limit(limit)
			.collect(Collectors.toList());
	}

	public String topGiver()
	{
		List<Map.Entry<String, Integer>> top = top(getGiven(), 1);
		return top.isEmpty() ? null : top.get(0).getKey();
	}
}

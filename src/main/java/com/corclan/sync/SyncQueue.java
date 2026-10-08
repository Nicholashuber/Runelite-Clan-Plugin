package com.corclan.sync;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The player's own gz's and broadcasts waiting to be sent to the clan server. Bounded so a long outage
 * cannot grow memory; the oldest events are dropped first (the server refuses events older than 10 minutes
 * anyway). Thread-safe: filled on the client thread, drained on the scheduler, re-filled from OkHttp callbacks.
 */
public final class SyncQueue
{
	public static final int MAX_PENDING = 500;
	public static final int MAX_BATCH = 100;

	private final Deque<SyncModels.Event> pending = new ArrayDeque<>();

	public synchronized void add(SyncModels.Event event)
	{
		pending.addLast(event);
		trim();
	}

	/** Removes and returns up to {@code max} of the oldest events. */
	public synchronized List<SyncModels.Event> drain(int max)
	{
		List<SyncModels.Event> batch = new ArrayList<>(Math.min(max, pending.size()));
		while (batch.size() < max && !pending.isEmpty())
		{
			batch.add(pending.removeFirst());
		}
		return batch;
	}

	/** Puts a batch that failed to send back at the front, in its original order. */
	public synchronized void requeue(List<SyncModels.Event> batch)
	{
		for (int i = batch.size() - 1; i >= 0; i--)
		{
			pending.addFirst(batch.get(i));
		}
		trim();
	}

	/** Forgets events from before {@code cutoff} (epoch millis): the server would refuse them as too old. */
	public synchronized void dropOlderThan(long cutoff)
	{
		pending.removeIf(event -> event.getAt() < cutoff);
	}

	public synchronized void clear()
	{
		pending.clear();
	}

	public synchronized int size()
	{
		return pending.size();
	}

	private void trim()
	{
		while (pending.size() > MAX_PENDING)
		{
			pending.removeFirst();
		}
	}
}

package com.corclan.sync;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class SyncQueueTest
{
	private static SyncModels.Event gz(long at)
	{
		return SyncModels.Event.gz("gz", at);
	}

	@Test
	public void drainsOldestFirstUpToTheBatchSize()
	{
		SyncQueue q = new SyncQueue();
		for (int i = 0; i < 5; i++)
		{
			q.add(gz(i));
		}
		List<SyncModels.Event> batch = q.drain(3);
		assertEquals(3, batch.size());
		assertEquals(0L, batch.get(0).getAt());
		assertEquals(2L, batch.get(2).getAt());
		assertEquals(2, q.size());
	}

	@Test
	public void requeuePutsAFailedBatchBackInFront()
	{
		SyncQueue q = new SyncQueue();
		q.add(gz(1));
		q.add(gz(2));
		List<SyncModels.Event> batch = q.drain(2);
		q.add(gz(3));
		q.requeue(batch);
		List<SyncModels.Event> all = q.drain(10);
		assertEquals(Arrays.asList(1L, 2L, 3L), Arrays.asList(all.get(0).getAt(), all.get(1).getAt(), all.get(2).getAt()));
	}

	@Test
	public void isBoundedAndDropsTheOldest()
	{
		SyncQueue q = new SyncQueue();
		for (int i = 0; i < SyncQueue.MAX_PENDING + 50; i++)
		{
			q.add(gz(i));
		}
		assertEquals(SyncQueue.MAX_PENDING, q.size());
		assertEquals(50L, q.drain(1).get(0).getAt());
	}

	@Test
	public void dropsEventsTheServerWouldRefuseAsTooOld()
	{
		SyncQueue q = new SyncQueue();
		q.add(gz(100));
		q.add(gz(200));
		q.add(gz(300));
		q.dropOlderThan(200);
		List<SyncModels.Event> left = q.drain(10);
		assertEquals(2, left.size());
		assertEquals(200L, left.get(0).getAt());
	}

	@Test
	public void clearEmptiesTheQueue()
	{
		SyncQueue q = new SyncQueue();
		q.add(gz(1));
		q.clear();
		assertEquals(0, q.drain(10).size());
	}
}

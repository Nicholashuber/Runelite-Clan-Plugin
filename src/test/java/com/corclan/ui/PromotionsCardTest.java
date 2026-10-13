package com.corclan.ui;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PromotionsCardTest
{
	@Test
	public void agoIsShort()
	{
		assertEquals("just now", PromotionsCard.ago(30_000L));
		assertEquals("5m ago", PromotionsCard.ago(5 * 60_000L));
		assertEquals("3h ago", PromotionsCard.ago(3 * 3_600_000L));
		assertEquals("2d ago", PromotionsCard.ago(50 * 3_600_000L));
		assertEquals("just now", PromotionsCard.ago(-1000L));
	}
}

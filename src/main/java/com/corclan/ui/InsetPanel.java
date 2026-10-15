package com.corclan.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * A dark, see-through inset with a thin rim, for things sitting on an {@link ArtPlaque} (the gz counters, the
 * trophies): the plaque shows through around it.
 */
class InsetPanel extends JPanel
{
	private static final Color RIM = PanelArt.STEEL_DIM;

	InsetPanel(LayoutManager layout)
	{
		super(layout);
		setOpaque(false);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		try
		{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(PanelArt.alpha(Color.BLACK, 120));
			g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
			g2.setColor(PanelArt.alpha(RIM, 140));
			g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
		}
		finally
		{
			g2.dispose();
		}
	}
}

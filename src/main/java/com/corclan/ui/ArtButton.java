package com.corclan.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;

/**
 * A button in the rhino logo's colours, for the side panel's art: an iron-grey face with a steel rim that turns
 * horn red, with a faint red glow, under the mouse.
 */
class ArtButton extends JButton
{
	private static final int ARC = 8;
	/** the label colour while enabled */
	private Color textColor = Color.WHITE;

	ArtButton(String text)
	{
		super(text);
		setFont(FontManager.getRunescapeBoldFont());
		setForeground(Color.WHITE);
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
		setFocusable(false);
		setOpaque(false);
		setRolloverEnabled(true);
		setBorder(new EmptyBorder(5, 10, 5, 10));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	/** The label colour while enabled (white by default). */
	void setTextColor(Color color)
	{
		textColor = color;
		repaint();
	}

	@Override
	public Dimension getPreferredSize()
	{
		Dimension size = super.getPreferredSize();
		return new Dimension(size.width, Math.max(size.height, 26));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		try
		{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			ButtonModel model = getModel();
			boolean hot = isEnabled() && model.isRollover();
			boolean down = isEnabled() && model.isArmed() && model.isPressed();
			int w = getWidth() - 1;
			int h = getHeight() - 1;

			if (hot)
			{
				for (int i = 3; i >= 1; i--)
				{
					g2.setColor(PanelArt.alpha(PanelArt.HORN, 40 / i));
					g2.setStroke(new BasicStroke(i * 2f));
					g2.drawRoundRect(1, 1, w - 2, h - 2, ARC, ARC);
				}
			}
			Color top = down ? PanelArt.IRON_BOTTOM : hot ? new Color(62, 62, 62) : PanelArt.IRON_TOP;
			Color bottom = down ? PanelArt.IRON_TOP : PanelArt.IRON_BOTTOM;
			g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
			g2.fillRoundRect(1, 1, w - 2, h - 2, ARC, ARC);
			// a light line along the top edge, like the logo's face plates
			g2.setColor(PanelArt.alpha(Color.WHITE, down ? 10 : 28));
			g2.drawLine(ARC / 2 + 1, 2, w - ARC / 2 - 1, 2);
			g2.setStroke(new BasicStroke(1.5f));
			g2.setColor(!isEnabled() ? PanelArt.STEEL_DIM : hot ? PanelArt.HORN : PanelArt.STEEL);
			g2.drawRoundRect(1, 1, w - 2, h - 2, ARC, ARC);
		}
		finally
		{
			g2.dispose();
		}
		setForeground(isEnabled() ? textColor : PanelArt.STEEL_DIM);
		super.paintComponent(g);
	}
}

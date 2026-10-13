package com.corclan.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.LinearGradientPaint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

/**
 * The side panel's art frame (announcement banner, rank card), in the rhino logo's colours: a charcoal plaque with a
 * steel rim and rivets. While live it comes alive: the rim brightens and an outer glow in horn red pulses, a light sweep crosses it
 * now and then and sparkles twinkle in the corners. It only repaints while on screen, and stays still when not live.
 */
final class ArtPlaque extends JPanel
{
	private static final Color RIM = PanelArt.STEEL_DIM;
	private static final Color PLAQUE_TOP = new Color(46, 46, 46);
	private static final Color PLAQUE_BOTTOM = PanelArt.IRON_BOTTOM;
	private static final Color RIVET = new Color(130, 130, 130);

	/** room around the plaque for the glow */
	private static final int MARGIN = 4;
	private static final int ARC = 12;
	private static final int FRAME_MILLIS = 50;
	private static final double PULSE_MILLIS = 2400;
	private static final long SWEEP_MILLIS = 5000;
	/** share of each sweep cycle the light band is moving; the rest is a pause */
	private static final float SWEEP_SHARE = 0.35f;
	private static final double TWINKLE_MILLIS = 1800;
	/** sparkle spots: x and y as a share of the plaque, then the phase offset */
	private static final float[][] TWINKLES = {
		{0.05f, 0.10f, 0.00f},
		{0.95f, 0.12f, 0.35f},
		{0.93f, 0.90f, 0.70f},
		{0.07f, 0.88f, 0.50f},
		{0.62f, 0.05f, 0.20f},
	};

	private final Timer timer = new Timer(FRAME_MILLIS, e ->
	{
		if (isShowing())
		{
			repaint();
		}
	});
	private final long start = System.currentTimeMillis();
	private boolean live;
	private Color accent = PanelArt.HORN;
	private Color sparkle = PanelArt.mix(PanelArt.HORN, Color.WHITE, 0.45f);

	ArtPlaque(LayoutManager layout)
	{
		super(layout);
		setOpaque(false);
		setBorder(new EmptyBorder(MARGIN + 8, MARGIN + 10, MARGIN + 8, MARGIN + 10));
	}

	/** @param live true to animate; false for a still, dimmer plaque */
	void setLive(boolean live)
	{
		this.live = live;
		if (live && isDisplayable())
		{
			timer.start();
		}
		else
		{
			timer.stop();
		}
		repaint();
	}

	/** The colour of the outer glow; the sparkles get a lighter version of it. */
	void setAccent(Color accent)
	{
		this.accent = accent;
		this.sparkle = PanelArt.mix(accent, Color.WHITE, 0.45f);
		repaint();
	}

	@Override
	public void addNotify()
	{
		super.addNotify();
		if (live)
		{
			timer.start();
		}
	}

	@Override
	public void removeNotify()
	{
		timer.stop();
		super.removeNotify();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		try
		{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			long t = System.currentTimeMillis() - start;
			float pulse = live ? (float) (0.5 + 0.5 * Math.sin(t * 2 * Math.PI / PULSE_MILLIS)) : 0f;
			int x = MARGIN;
			int y = MARGIN;
			int w = getWidth() - 2 * MARGIN - 1;
			int h = getHeight() - 2 * MARGIN - 1;
			if (w <= 0 || h <= 0)
			{
				return;
			}

			// soft glow outside the rim
			if (live)
			{
				for (int i = MARGIN; i >= 1; i--)
				{
					g2.setColor(PanelArt.alpha(accent, (int) ((30 + 70 * pulse) / i)));
					g2.setStroke(new BasicStroke(i * 2f));
					g2.drawRoundRect(x, y, w, h, ARC, ARC);
				}
			}

			RoundRectangle2D plaque = new RoundRectangle2D.Float(x, y, w, h, ARC, ARC);
			g2.setPaint(new GradientPaint(0, y, PLAQUE_TOP, 0, y + h, PLAQUE_BOTTOM));
			g2.fill(plaque);

			if (live)
			{
				paintSweep(g2, plaque, t);
			}

			// steel rim, a darker inner line and a rivet in each corner
			g2.setStroke(new BasicStroke(2f));
			g2.setColor(live ? PanelArt.mix(RIM, PanelArt.STEEL_LIGHT, 0.3f + 0.7f * pulse) : RIM);
			g2.draw(plaque);
			g2.setStroke(new BasicStroke(1f));
			g2.setColor(PanelArt.alpha(Color.BLACK, 110));
			g2.drawRoundRect(x + 3, y + 3, w - 6, h - 6, ARC - 4, ARC - 4);
			int r = 3;
			g2.setColor(RIVET);
			g2.fillOval(x + 5, y + 5, r, r);
			g2.fillOval(x + w - 5 - r, y + 5, r, r);
			g2.fillOval(x + 5, y + h - 5 - r, r, r);
			g2.fillOval(x + w - 5 - r, y + h - 5 - r, r, r);

			if (live)
			{
				paintTwinkles(g2, x, y, w, h, t);
			}
		}
		finally
		{
			g2.dispose();
		}
	}

	/** A slanted band of light that crosses the plaque, then pauses. */
	private void paintSweep(Graphics2D g2, RoundRectangle2D plaque, long t)
	{
		float phase = (t % SWEEP_MILLIS) / (float) SWEEP_MILLIS / SWEEP_SHARE;
		if (phase >= 1f)
		{
			return;
		}
		float band = 46f;
		float width = (float) plaque.getWidth();
		float left = (float) plaque.getX() - band * 2 + phase * (width + band * 3);
		float top = (float) plaque.getY();
		LinearGradientPaint light = new LinearGradientPaint(left, top, left + band, top + band * 0.6f,
			new float[]{0f, 0.5f, 1f},
			new Color[]{PanelArt.alpha(Color.WHITE, 0), PanelArt.alpha(new Color(235, 235, 235), 50), PanelArt.alpha(Color.WHITE, 0)});
		g2.setPaint(light);
		g2.fill(plaque);
	}

	/** Small four-point stars that fade in and out, each on its own beat. */
	private void paintTwinkles(Graphics2D g2, int x, int y, int w, int h, long t)
	{
		for (float[] spot : TWINKLES)
		{
			double wave = Math.sin((t / TWINKLE_MILLIS + spot[2]) * 2 * Math.PI);
			if (wave <= 0)
			{
				continue;
			}
			float bright = (float) (wave * wave);
			int cx = x + Math.round(spot[0] * w);
			int cy = y + Math.round(spot[1] * h);
			int arm = 2 + Math.round(4 * bright);
			g2.setColor(PanelArt.alpha(sparkle, Math.round(255 * bright)));
			g2.fill(star(cx, cy, arm));
			g2.setColor(PanelArt.alpha(Color.WHITE, Math.round(220 * bright)));
			g2.fillRect(cx, cy, 1, 1);
		}
	}

	static Polygon star(int cx, int cy, int arm)
	{
		Polygon p = new Polygon();
		p.addPoint(cx, cy - arm);
		p.addPoint(cx + 1, cy - 1);
		p.addPoint(cx + arm, cy);
		p.addPoint(cx + 1, cy + 1);
		p.addPoint(cx, cy + arm);
		p.addPoint(cx - 1, cy + 1);
		p.addPoint(cx - arm, cy);
		p.addPoint(cx - 1, cy - 1);
		return p;
	}
}

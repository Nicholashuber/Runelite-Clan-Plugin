package com.corclan.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/** Small pictures for the side panel, drawn in code so there are no image files to ship. */
final class PanelArt
{
	// the rhino logo's palette: a red horn, steel-grey face plates, black outlines
	/** the horn */
	static final Color HORN = new Color(240, 0, 0);
	static final Color HORN_DARK = new Color(140, 0, 0);
	/** face plates, light to dark */
	static final Color STEEL_LIGHT = new Color(205, 205, 205);
	static final Color STEEL = new Color(150, 150, 150);
	static final Color STEEL_DIM = new Color(95, 95, 95);
	/** the dark backgrounds the art sits on */
	static final Color IRON_TOP = new Color(50, 50, 50);
	static final Color IRON_BOTTOM = new Color(17, 17, 17);
	private static final Color OUTLINE = new Color(8, 8, 8);

	/** What the rank card's shield shows. */
	enum Emblem
	{
		/** clan Owner */
		CROWN,
		/** Lavasockz's Molten Lord aura */
		FLAME,
		/** DAYLlGHT's founder aura */
		SUN,
		/** JoeBoob's nature aura */
		LEAF,
		/** a Gem League rank */
		GEM,
		/** any other rank */
		STAR,
		/** not logged in or not in the clan */
		NONE
	}

	private PanelArt()
	{
	}

	/** A small steel megaphone with red sound waves, for the announcement banner's title. */
	static BufferedImage megaphone()
	{
		BufferedImage img = new BufferedImage(22, 22, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = start(img);
		try
		{
			// handle
			g.setColor(STEEL_DIM);
			g.fillRect(6, 13, 3, 6);
			g.setColor(OUTLINE);
			g.drawRect(6, 13, 3, 6);

			// mouthpiece and horn
			Polygon horn = new Polygon(new int[]{5, 14, 14, 5}, new int[]{8, 3, 18, 13}, 4);
			g.setPaint(new GradientPaint(0, 3, STEEL_LIGHT, 0, 18, STEEL_DIM));
			g.fillRect(1, 8, 5, 5);
			g.fill(horn);
			g.setColor(OUTLINE);
			g.drawRect(1, 8, 5, 5);
			g.draw(horn);
			g.setColor(Color.WHITE);
			g.drawLine(6, 9, 13, 5);

			// sound waves
			g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(HORN);
			g.draw(new Arc2D.Float(12, 6, 6, 9, -55, 110, Arc2D.OPEN));
			g.draw(new Arc2D.Float(12, 3, 9, 15, -55, 110, Arc2D.OPEN));
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	/** parties bigger than this show their slots as text only */
	static final int MAX_PIPS = 8;

	/** One dot per party slot (up to {@link #MAX_PIPS}): filled ones in the raid's colour, open ones hollow steel. */
	static BufferedImage pips(int filled, int size, Color color)
	{
		int n = Math.max(1, Math.min(MAX_PIPS, size));
		BufferedImage img = new BufferedImage(n * 8, 8, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = start(img);
		try
		{
			for (int i = 0; i < n; i++)
			{
				int x = i * 8 + 1;
				if (i < filled)
				{
					g.setColor(color);
					g.fillOval(x, 1, 6, 6);
					g.setColor(mix(color, Color.WHITE, 0.5f));
					g.fillRect(x + 2, 2, 1, 1);
				}
				else
				{
					g.setColor(STEEL_DIM);
					g.drawOval(x, 1, 5, 5);
				}
			}
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	/** A steel skull with horn-red eyes (22x22), for the raid finder's title. */
	static BufferedImage skull()
	{
		BufferedImage img = new BufferedImage(22, 22, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = start(img);
		try
		{
			java.awt.geom.Area head = new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Float(3, 1, 16, 14));
			head.add(new java.awt.geom.Area(new java.awt.geom.RoundRectangle2D.Float(6, 10, 10, 9, 3, 3)));
			g.setPaint(new GradientPaint(0, 1, STEEL_LIGHT, 0, 20, STEEL_DIM));
			g.fill(head);
			g.setColor(OUTLINE);
			g.draw(head);
			g.setColor(HORN);
			g.fillOval(6, 7, 4, 4);
			g.fillOval(12, 7, 4, 4);
			g.setColor(OUTLINE);
			g.fillPolygon(new int[]{11, 10, 12}, new int[]{12, 14, 14}, 3);
			g.drawLine(9, 16, 9, 19);
			g.drawLine(11, 16, 11, 19);
			g.drawLine(13, 16, 13, 19);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	/** A small cut gem (18x16) in the gem's colour, for the promotion assistant's rows. */
	static BufferedImage gemIcon(Color accent)
	{
		BufferedImage img = new BufferedImage(18, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = start(img);
		try
		{
			g.scale(0.6, 0.6);
			gem(g, 15, 12, accent);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	/** Two stacked chevrons pointing up, horn red over steel, for the promotion assistant's title. */
	static BufferedImage promote()
	{
		BufferedImage img = new BufferedImage(22, 22, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = start(img);
		try
		{
			Polygon low = chevron(11, 12);
			Polygon high = chevron(11, 4);
			g.setPaint(new GradientPaint(0, 12, STEEL_LIGHT, 0, 21, STEEL_DIM));
			g.fill(low);
			g.setColor(OUTLINE);
			g.draw(low);
			g.setPaint(new GradientPaint(0, 4, mix(HORN, Color.WHITE, 0.3f), 0, 13, HORN_DARK));
			g.fill(high);
			g.setColor(OUTLINE);
			g.draw(high);
			g.setColor(Color.WHITE);
			g.fill(ArtPlaque.star(17, 4, 2));
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	/** an upward chevron 18 wide and 8 tall, its tip at (cx, top) */
	private static Polygon chevron(int cx, int top)
	{
		return new Polygon(
			new int[]{cx, cx + 9, cx + 9, cx, cx - 9, cx - 9},
			new int[]{top, top + 6, top + 9, top + 3, top + 9, top + 6}, 6);
	}

	/** Three fanned trading cards in the logo's steel, red and black, for the CoR TCG mention. */
	static BufferedImage cardFan()
	{
		BufferedImage img = new BufferedImage(26, 26, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = start(img);
		try
		{
			double[] angles = {-0.35, 0, 0.35};
			Color[] faces = {STEEL, HORN, IRON_TOP};
			for (int i = 0; i < angles.length; i++)
			{
				Graphics2D c = (Graphics2D) g.create();
				c.rotate(angles[i], 13, 22);
				RoundRectangle2D card = new RoundRectangle2D.Float(7, 4, 12, 17, 3, 3);
				c.setColor(faces[i]);
				c.fill(card);
				c.setColor(STEEL_LIGHT);
				c.setStroke(new BasicStroke(1.2f));
				c.draw(card);
				c.dispose();
			}
			g.setColor(Color.WHITE);
			g.fill(ArtPlaque.star(13, 12, 4));
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	/** A heraldic shield (44x50) with a steel rim, an accent-coloured inner line and the rank's symbol. */
	static BufferedImage emblem(Emblem kind, Color accent)
	{
		int w = 44;
		int h = 50;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = start(img);
		try
		{
			Path2D shield = shield(2, 2, w - 4, h - 4);
			g.setPaint(new GradientPaint(0, 2, IRON_TOP, 0, h, IRON_BOTTOM));
			g.fill(shield);
			g.setColor(PanelArt.alpha(accent, 150));
			g.setStroke(new BasicStroke(1f));
			g.draw(shield(5, 5, w - 10, h - 10));
			g.setStroke(new BasicStroke(2f));
			g.setColor(kind == Emblem.NONE ? STEEL_DIM : STEEL_LIGHT);
			g.draw(shield);

			int cx = w / 2;
			int cy = 22;
			switch (kind)
			{
				case CROWN:
					crown(g, cx, cy, accent);
					break;
				case FLAME:
					flame(g, cx, cy, accent);
					break;
				case SUN:
					sun(g, cx, cy, accent);
					break;
				case LEAF:
					leaf(g, cx, cy, accent);
					break;
				case GEM:
					gem(g, cx, cy, accent);
					break;
				case STAR:
					star(g, cx, cy, accent);
					break;
				default:
					g.setColor(new Color(120, 120, 120));
					g.setFont(g.getFont().deriveFont(java.awt.Font.BOLD, 18f));
					g.drawString("?", cx - 5, cy + 7);
					break;
			}
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	/**
	 * The glow switch drawn as the checkbox's own box: a big pill that lights up horn red with the knob on the right
	 * when on, dark with the knob on the left when off.
	 */
	static BufferedImage glowSwitch(boolean on, boolean enabled)
	{
		Color accent = HORN;
		int w = 50;
		int h = 26;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = start(img);
		try
		{
			RoundRectangle2D track = new RoundRectangle2D.Float(2, 3, w - 4, h - 6, h - 6, h - 6);
			if (on && enabled)
			{
				for (int i = 3; i >= 1; i--)
				{
					g.setColor(alpha(accent, 60 / i));
					g.setStroke(new BasicStroke(i * 2f));
					g.draw(track);
				}
				g.setPaint(new GradientPaint(0, 3, mix(accent, Color.WHITE, 0.25f), 0, h - 3, mix(accent, Color.BLACK, 0.35f)));
			}
			else
			{
				g.setPaint(new GradientPaint(0, 3, new Color(40, 40, 40), 0, h - 3, new Color(20, 20, 20)));
			}
			g.fill(track);
			g.setStroke(new BasicStroke(1.5f));
			g.setColor(on && enabled ? STEEL_LIGHT : STEEL_DIM);
			g.draw(track);

			int knob = h - 10;
			int kx = on ? w - 5 - knob : 5;
			Ellipse2D ball = new Ellipse2D.Float(kx, 5, knob, knob);
			g.setPaint(new GradientPaint(0, 5, on && enabled ? Color.WHITE : new Color(160, 160, 160),
				0, 5 + knob, on && enabled ? STEEL : new Color(80, 80, 80)));
			g.fill(ball);
			g.setColor(OUTLINE);
			g.setStroke(new BasicStroke(1f));
			g.draw(ball);
			if (on && enabled)
			{
				g.setColor(Color.WHITE);
				g.fill(ArtPlaque.star(kx + knob / 2, 5 + knob / 2, 4));
			}
		}
		finally
		{
			g.dispose();
		}
		if (!enabled)
		{
			return faded(img);
		}
		return img;
	}

	private static Path2D shield(float x, float y, float w, float h)
	{
		Path2D p = new Path2D.Float();
		p.moveTo(x + 4, y);
		p.lineTo(x + w - 4, y);
		p.quadTo(x + w, y, x + w, y + 4);
		p.lineTo(x + w, y + h * 0.48f);
		p.quadTo(x + w, y + h * 0.80f, x + w / 2, y + h);
		p.quadTo(x, y + h * 0.80f, x, y + h * 0.48f);
		p.lineTo(x, y + 4);
		p.quadTo(x, y, x + 4, y);
		p.closePath();
		return p;
	}

	private static void crown(Graphics2D g, int cx, int cy, Color accent)
	{
		Polygon crown = new Polygon(
			new int[]{cx - 12, cx - 12, cx - 6, cx, cx + 6, cx + 12, cx + 12},
			new int[]{cy + 8, cy - 7, cy, cy - 10, cy, cy - 7, cy + 8}, 7);
		g.setPaint(new GradientPaint(0, cy - 10, new Color(255, 236, 150), 0, cy + 8, accent));
		g.fill(crown);
		g.setColor(OUTLINE);
		g.draw(crown);
		g.setColor(new Color(200, 30, 40));
		g.fillOval(cx - 2, cy + 1, 5, 5);
		g.setColor(new Color(40, 90, 220));
		g.fillOval(cx - 9, cy + 2, 4, 4);
		g.fillOval(cx + 6, cy + 2, 4, 4);
	}

	private static void flame(Graphics2D g, int cx, int cy, Color accent)
	{
		g.setPaint(new GradientPaint(0, cy - 13, new Color(255, 120, 30), 0, cy + 11, accent));
		g.fill(flameShape(cx, cy + 11, 11, 24));
		g.setPaint(new GradientPaint(0, cy - 4, new Color(255, 250, 200), 0, cy + 11, new Color(255, 200, 60)));
		g.fill(flameShape(cx, cy + 11, 6, 13));
		g.setColor(OUTLINE);
		g.draw(flameShape(cx, cy + 11, 11, 24));
	}

	private static Path2D flameShape(float cx, float bottom, float halfWidth, float height)
	{
		Path2D p = new Path2D.Float();
		p.moveTo(cx, bottom);
		p.curveTo(cx - halfWidth * 1.4f, bottom - height * 0.15f, cx - halfWidth * 0.6f, bottom - height * 0.6f, cx, bottom - height);
		p.curveTo(cx + halfWidth * 0.3f, bottom - height * 0.6f, cx + halfWidth * 1.4f, bottom - height * 0.35f, cx, bottom);
		p.closePath();
		return p;
	}

	private static void sun(Graphics2D g, int cx, int cy, Color accent)
	{
		g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(accent);
		for (int i = 0; i < 12; i++)
		{
			double a = i * Math.PI / 6;
			int inner = 9;
			int outer = i % 2 == 0 ? 14 : 12;
			g.drawLine(cx + (int) Math.round(Math.cos(a) * inner), cy + (int) Math.round(Math.sin(a) * inner),
				cx + (int) Math.round(Math.cos(a) * outer), cy + (int) Math.round(Math.sin(a) * outer));
		}
		g.setPaint(new GradientPaint(0, cy - 7, new Color(255, 240, 170), 0, cy + 7, accent));
		g.fillOval(cx - 7, cy - 7, 14, 14);
		g.setStroke(new BasicStroke(1f));
		g.setColor(OUTLINE);
		g.drawOval(cx - 7, cy - 7, 14, 14);
	}

	/** Two crossed leaves with veins and a little sparkle, for the nature aura. */
	private static void leaf(Graphics2D g, int cx, int cy, Color accent)
	{
		for (int side = -1; side <= 1; side += 2)
		{
			Graphics2D l = (Graphics2D) g.create();
			l.rotate(side * 0.55, cx, cy + 10);
			Path2D blade = new Path2D.Float();
			blade.moveTo(cx, cy + 10);
			blade.quadTo(cx - 9, cy - 2, cx, cy - 13);
			blade.quadTo(cx + 9, cy - 2, cx, cy + 10);
			blade.closePath();
			l.setPaint(new GradientPaint(0, cy - 13, mix(accent, Color.WHITE, 0.35f), 0, cy + 10, mix(accent, Color.BLACK, 0.3f)));
			l.fill(blade);
			l.setColor(OUTLINE);
			l.draw(blade);
			l.setColor(alpha(new Color(220, 255, 200), 170));
			l.drawLine(cx, cy + 8, cx, cy - 10);
			l.drawLine(cx, cy - 1, cx - 4, cy - 5);
			l.drawLine(cx, cy + 3, cx + 4, cy - 1);
			l.dispose();
		}
		g.setColor(new Color(120, 80, 40));
		g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.drawLine(cx, cy + 9, cx, cy + 14);
		g.setStroke(new BasicStroke(1f));
		g.setColor(new Color(220, 255, 200));
		g.fill(ArtPlaque.star(cx + 9, cy - 9, 3));
	}

	/** A cut gem seen from the side: crown facets on top, a pointed pavilion below, a white glint. */
	private static void gem(Graphics2D g, int cx, int cy, Color accent)
	{
		int top = cy - 9;
		int girdle = cy - 3;
		int tip = cy + 12;
		Polygon whole = new Polygon(
			new int[]{cx - 7, cx + 7, cx + 13, cx, cx - 13},
			new int[]{top, top, girdle, tip, girdle}, 5);
		g.setColor(mix(accent, Color.BLACK, 0.25f));
		g.fill(whole);
		// lighter crown facets and the left pavilion half
		g.setColor(mix(accent, Color.WHITE, 0.35f));
		g.fill(new Polygon(new int[]{cx - 7, cx + 7, cx + 4, cx - 4}, new int[]{top, top, girdle, girdle}, 4));
		g.setColor(accent);
		g.fill(new Polygon(new int[]{cx - 13, cx - 4, cx}, new int[]{girdle, girdle, tip}, 3));
		g.setColor(mix(accent, Color.BLACK, 0.45f));
		g.fill(new Polygon(new int[]{cx + 4, cx + 13, cx}, new int[]{girdle, girdle, tip}, 3));
		g.setColor(alpha(Color.WHITE, 120));
		g.drawLine(cx - 13, girdle, cx + 13, girdle);
		g.drawLine(cx - 4, girdle, cx, tip);
		g.drawLine(cx + 4, girdle, cx, tip);
		g.setColor(OUTLINE);
		g.draw(whole);
		g.setColor(Color.WHITE);
		g.fill(ArtPlaque.star(cx - 4, top + 3, 3));
	}

	private static void star(Graphics2D g, int cx, int cy, Color accent)
	{
		Polygon star = new Polygon();
		for (int i = 0; i < 10; i++)
		{
			double a = -Math.PI / 2 + i * Math.PI / 5;
			int r = i % 2 == 0 ? 13 : 5;
			star.addPoint(cx + (int) Math.round(Math.cos(a) * r), cy + 1 + (int) Math.round(Math.sin(a) * r));
		}
		g.setPaint(new GradientPaint(0, cy - 12, mix(accent, Color.WHITE, 0.4f), 0, cy + 12, accent));
		g.fill(star);
		g.setColor(OUTLINE);
		g.draw(star);
	}

	private static Graphics2D start(BufferedImage img)
	{
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		return g;
	}

	/** The same picture at 40% opacity, for a switch that cannot be used. */
	private static BufferedImage faded(BufferedImage img)
	{
		BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		try
		{
			g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.4f));
			g.drawImage(img, 0, 0, null);
		}
		finally
		{
			g.dispose();
		}
		return out;
	}

	static Color alpha(Color c, int a)
	{
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
	}

	static Color mix(Color a, Color b, float k)
	{
		return new Color(
			Math.round(a.getRed() + (b.getRed() - a.getRed()) * k),
			Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * k),
			Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * k));
	}
}

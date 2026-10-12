package com.corclan.ui;

import com.corclan.CorClanConfig;
import com.corclan.glow.DevGlow;
import com.corclan.glow.FounderGlow;
import com.corclan.glow.GemGlow;
import com.corclan.glow.GlowEffect;
import com.corclan.glow.GlowPicks;
import com.corclan.glow.NatureGlow;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The player's own clan rank on a plaque, with their rank's emblem and one big glow switch. The switch flips the same
 * settings as the per-effect checkboxes further down (Owner, Dev, Founder, Nature glow, gem outline + sparkles), so it is a
 * shortcut, not a new setting. The plaque animates in the rank's colour while the glow is on.
 */
final class RankCard extends JPanel
{
	private static final Color STEEL = new Color(140, 160, 190);
	private static final Color OWNER = new Color(255, 190, 40);
	private static final Color MOLTEN = new Color(235, 70, 20);
	private static final Color FOUNDER = new Color(220, 40, 60);
	private static final Color NATURE = new Color(50, 205, 80);
	/** the status lines' wrap width inside the plaque */
	private static final int STATUS_WIDTH = 165;

	private final CorClanConfig config;
	/** config key, new value: the same setter the per-effect checkboxes use */
	private final BiConsumer<String, Boolean> setGlow;

	private final ArtPlaque plaque = new ArtPlaque(new BorderLayout(10, 8));
	private final JLabel emblem = new JLabel();
	private final JLabel caption = new JLabel("YOUR RANK");
	private final JLabel title = new JLabel();
	private final JLabel name = new JLabel();
	private final JCheckBox glow = new JCheckBox("Glow");
	private final JLabel glowNote = new JLabel();
	private final StatTile given = new StatTile("GZ GIVEN");
	private final StatTile received = new StatTile("GZ'D");
	/** this client's totals, then clan sync, the CoR party and the clan map, one line each */
	private final JLabel status = new JLabel();

	/** config key to its current value, for every glow setting this player's switch controls */
	private Map<String, Boolean> controlled = new LinkedHashMap<>();
	/** which of them were on when the switch last turned the glow off, so turning it on again restores them */
	private Set<String> remembered = new HashSet<>();
	private PanelArt.Emblem shownEmblem;
	private Color accent = STEEL;

	RankCard(CorClanConfig config, BiConsumer<String, Boolean> setGlow)
	{
		super(new BorderLayout());
		this.config = config;
		this.setGlow = setGlow;
		setOpaque(false);

		JPanel words = new JPanel();
		words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
		words.setOpaque(false);
		caption.setFont(FontManager.getRunescapeSmallFont());
		caption.setForeground(PanelArt.DIM_GOLD);
		title.setFont(FontManager.getRunescapeBoldFont().deriveFont(22f));
		name.setFont(FontManager.getRunescapeSmallFont());
		name.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		words.add(caption);
		words.add(title);
		words.add(name);

		glow.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
		glow.setOpaque(false);
		glow.setFocusable(false);
		glow.setIconTextGap(8);
		glow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		glow.addActionListener(e -> flip(glow.isSelected()));
		glowNote.setFont(FontManager.getRunescapeSmallFont());
		glowNote.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		JPanel switchRow = new JPanel(new BorderLayout(0, 2));
		switchRow.setOpaque(false);
		switchRow.add(glow, BorderLayout.NORTH);
		switchRow.add(glowNote, BorderLayout.CENTER);

		// the GZ tracker, built into the card: two counters, then the status lines
		JPanel tiles = new JPanel(new GridLayout(1, 2, 6, 0));
		tiles.setOpaque(false);
		tiles.add(given);
		tiles.add(received);
		status.setFont(FontManager.getRunescapeSmallFont());
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		JPanel gz = new JPanel(new BorderLayout(0, 6));
		gz.setOpaque(false);
		gz.add(tiles, BorderLayout.NORTH);
		gz.add(status, BorderLayout.CENTER);

		JPanel lower = new JPanel(new BorderLayout(0, 8));
		lower.setOpaque(false);
		lower.add(gz, BorderLayout.NORTH);
		lower.add(switchRow, BorderLayout.CENTER);

		plaque.add(emblem, BorderLayout.WEST);
		plaque.add(words, BorderLayout.CENTER);
		plaque.add(lower, BorderLayout.SOUTH);
		add(plaque, BorderLayout.CENTER);
		setBorder(new EmptyBorder(8, 0, 0, 0));
	}

	void fill(PanelData data)
	{
		PanelArt.Emblem kind;
		if (data.myName == null)
		{
			kind = PanelArt.Emblem.NONE;
			title.setText("Log in");
			name.setText("to see your clan rank");
		}
		else if (data.myRankTitle == null)
		{
			kind = PanelArt.Emblem.NONE;
			title.setText("No clan");
			name.setText(data.myName + " is not in a clan");
		}
		else
		{
			kind = data.dev ? PanelArt.Emblem.FLAME
				: data.founder ? PanelArt.Emblem.SUN
				: data.nature ? PanelArt.Emblem.LEAF
				: data.owner ? PanelArt.Emblem.CROWN
				: data.myGem != null ? PanelArt.Emblem.GEM
				: PanelArt.Emblem.STAR;
			title.setText(data.myRankTitle);
			name.setText(data.myName);
		}
		boolean in = data.myName != null;
		given.set(in ? String.valueOf(data.myGiven) : "-", in ? "+" + data.myGivenToday + " today" : "log in");
		received.set(in ? String.valueOf(data.myReceived) : "-", in ? "+" + data.myReceivedToday + " today" : "log in");
		status.setText("<html><div style='width:" + STATUS_WIDTH + "px'>" + CorClanPanel.escape(data.summary)
			+ "<br>" + CorClanPanel.escape(data.syncStatus) + "<br>" + CorClanPanel.escape(data.partyStatus)
			+ "<br>" + CorClanPanel.escape(data.mapStatus) + "</div></html>");

		Color newAccent = accentOf(kind, data.myGem);
		if (kind != shownEmblem || !newAccent.equals(accent))
		{
			shownEmblem = kind;
			accent = newAccent;
			emblem.setIcon(new ImageIcon(PanelArt.emblem(kind, accent)));
			plaque.setAccent(accent);
			given.setAccent(accent);
			received.setAccent(accent);
		}
		title.setForeground(kind == PanelArt.Emblem.NONE ? ColorScheme.LIGHT_GRAY_COLOR : PanelArt.mix(accent, Color.WHITE, 0.35f));

		controlled = controlledSettings(data);
		boolean any = controlled.containsValue(true);
		boolean shown = any && config.rankGlow();
		glow.setVisible(kind != PanelArt.Emblem.NONE);
		glowNote.setVisible(kind != PanelArt.Emblem.NONE);
		glow.setEnabled(!controlled.isEmpty());
		glow.setSelected(shown);
		glow.setText(controlled.isEmpty() ? "Glow" : shown ? "Glow ON" : "Glow OFF");
		glow.setForeground(shown ? PanelArt.GOLD : ColorScheme.LIGHT_GRAY_COLOR);
		glow.setIcon(new ImageIcon(PanelArt.glowSwitch(false, !controlled.isEmpty(), accent)));
		glow.setSelectedIcon(new ImageIcon(PanelArt.glowSwitch(true, true, accent)));
		glow.setDisabledIcon(new ImageIcon(PanelArt.glowSwitch(false, false, accent)));
		glow.setToolTipText(controlled.isEmpty() ? null
			: "Switches your " + String.join(", ", labels(data)).toLowerCase() + ". Fine-tune each part further down");
		glowNote.setText(controlled.isEmpty() ? "No glow at your rank yet"
			: !config.rankGlow() ? "Glows are hidden in the CoR Clan settings"
			: any ? "Other CoR plugin users see it on you" : "Tick to show your rank's glow");
		plaque.setLive(shown);
	}

	/** Turns every glow setting this switch controls on or off. Turning on also unhides glows if they were hidden. */
	private void flip(boolean on)
	{
		if (controlled.isEmpty())
		{
			return;
		}
		if (on)
		{
			if (!config.rankGlow())
			{
				setGlow.accept("rankGlow", true);
			}
			boolean restore = !remembered.isEmpty();
			for (String key : controlled.keySet())
			{
				setGlow.accept(key, !restore || remembered.contains(key));
			}
		}
		else
		{
			remembered = new HashSet<>();
			for (Map.Entry<String, Boolean> e : controlled.entrySet())
			{
				if (e.getValue())
				{
					remembered.add(e.getKey());
				}
				setGlow.accept(e.getKey(), false);
			}
		}
	}

	/** Every glow setting that is this player's own, with its current value. Empty when their rank has no glow. */
	private Map<String, Boolean> controlledSettings(PanelData data)
	{
		Map<String, Boolean> keys = new LinkedHashMap<>();
		if (data.myRankTitle == null)
		{
			return keys;
		}
		if (data.owner)
		{
			for (GlowEffect e : GlowEffect.values())
			{
				keys.put(e.configKey, GlowPicks.picked(config, e));
			}
		}
		if (data.dev)
		{
			for (DevGlow e : DevGlow.values())
			{
				keys.put(e.configKey, DevGlow.picked(config, e));
			}
		}
		if (data.founder)
		{
			for (FounderGlow e : FounderGlow.values())
			{
				keys.put(e.configKey, FounderGlow.picked(config, e));
			}
		}
		if (data.nature)
		{
			for (NatureGlow e : NatureGlow.values())
			{
				keys.put(e.configKey, NatureGlow.picked(config, e));
			}
		}
		// a molten-outline wearer shows no gem glow on top, so the gem parts are not theirs to switch
		if (data.myGem != null && !data.dev)
		{
			for (GemGlow.Part p : GemGlow.Part.values())
			{
				keys.put(p.configKey, GemGlow.Part.picked(config, p));
			}
		}
		return keys;
	}

	private static List<String> labels(PanelData data)
	{
		List<String> out = new ArrayList<>();
		if (data.owner)
		{
			out.add("Owner glow");
		}
		if (data.dev)
		{
			out.add("Dev glow");
		}
		if (data.founder)
		{
			out.add("Founder glow");
		}
		if (data.nature)
		{
			out.add("Nature glow");
		}
		if (data.myGem != null && !data.dev)
		{
			out.add(data.myGem.title + " glow");
		}
		return out;
	}

	private static Color accentOf(PanelArt.Emblem kind, GemGlow gem)
	{
		switch (kind)
		{
			case CROWN:
				return OWNER;
			case FLAME:
				return MOLTEN;
			case SUN:
				return FOUNDER;
			case LEAF:
				return NATURE;
			case GEM:
				return gemColor(gem);
			case NONE:
				return new Color(110, 110, 110);
			default:
				return STEEL;
		}
	}

	/** A gz counter: a dark inset with a thin rim in the rank's colour, a big gold number, a caption and today's gain. */
	private static final class StatTile extends JPanel
	{
		private final JLabel number = new JLabel("-", SwingConstants.CENTER);
		private final JLabel today = new JLabel(" ", SwingConstants.CENTER);
		private Color accent = STEEL;

		StatTile(String caption)
		{
			super(new BorderLayout());
			setOpaque(false);
			setBorder(new EmptyBorder(4, 4, 4, 4));
			JLabel top = new JLabel(caption, SwingConstants.CENTER);
			top.setFont(FontManager.getRunescapeSmallFont());
			top.setForeground(PanelArt.DIM_GOLD);
			number.setFont(FontManager.getRunescapeBoldFont().deriveFont(20f));
			number.setForeground(PanelArt.GOLD);
			today.setFont(FontManager.getRunescapeSmallFont());
			today.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			add(top, BorderLayout.NORTH);
			add(number, BorderLayout.CENTER);
			add(today, BorderLayout.SOUTH);
		}

		void set(String value, String gain)
		{
			number.setText(value);
			today.setText(gain);
		}

		void setAccent(Color accent)
		{
			this.accent = accent;
			repaint();
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
				g2.setColor(PanelArt.alpha(accent, 140));
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
			}
			finally
			{
				g2.dispose();
			}
		}
	}

	static Color gemColor(GemGlow gem)
	{
		switch (gem)
		{
			case ZENYTE:
				return new Color(255, 150, 40);
			case ONYX:
				return new Color(120, 110, 130);
			case DRAGONSTONE:
				return new Color(175, 80, 225);
			case DIAMOND:
				return new Color(215, 240, 255);
			case RUBY:
				return new Color(225, 35, 55);
			case EMERALD:
				return new Color(40, 195, 95);
			case SAPPHIRE:
				return new Color(55, 100, 235);
			default:
				return new Color(235, 215, 235);
		}
	}
}

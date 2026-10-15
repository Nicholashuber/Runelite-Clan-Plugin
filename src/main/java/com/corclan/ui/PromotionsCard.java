package com.corclan.ui;

import com.corclan.clan.Promotions;
import com.corclan.glow.GemGlow;
import com.corclan.sync.RecentPromotion;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Promotions, under the rank card, on the same art plaque as the banner. Everyone (clan sync on) sees the clan's three
 * latest promotions. Clan staff also get the Gem League assistant: who has earned a higher gem and who is close; it
 * pulses while someone is due. Click the title to open or close it. It only suggests: staff change ranks in the game.
 */
final class PromotionsCard extends JPanel
{
	/** at most this many "coming up" rows; the rest are counted */
	private static final int SOON_ROWS = 8;
	private static final Color ONLINE = new Color(60, 210, 90);

	private final ArtPlaque plaque = new ArtPlaque(new BorderLayout(0, 6));
	private final JLabel badge = new JLabel();
	private final JLabel marker = new JLabel();
	private final WrapText summary = new WrapText(FontManager.getRunescapeSmallFont(), ColorScheme.LIGHT_GRAY_COLOR);
	private final JPanel body = new JPanel();
	private final JPanel ladder = new JPanel();
	private final JLabel ladderToggle = new JLabel();
	private Promotions.Result shown;
	private List<RecentPromotion> shownRecent = Collections.emptyList();
	private boolean shownClanWide;

	PromotionsCard()
	{
		super(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(Box.createVerticalStrut(8), BorderLayout.NORTH);
		add(plaque, BorderLayout.CENTER);

		JPanel header = new JPanel(new BorderLayout(0, 2));
		header.setOpaque(false);
		JPanel titleRow = new JPanel(new BorderLayout(6, 0));
		titleRow.setOpaque(false);
		JLabel title = new JLabel("Promotions", new ImageIcon(PanelArt.promote()), SwingConstants.LEFT);
		title.setIconTextGap(6);
		title.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
		title.setForeground(PanelArt.HORN);
		titleRow.add(title, BorderLayout.CENTER);

		JPanel right = new JPanel();
		right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
		right.setOpaque(false);
		badge.setFont(FontManager.getRunescapeSmallFont());
		badge.setForeground(Color.WHITE);
		badge.setBackground(PanelArt.HORN);
		badge.setOpaque(true);
		badge.setBorder(new EmptyBorder(1, 4, 1, 4));
		right.add(badge);
		right.add(Box.createHorizontalStrut(6));
		marker.setFont(FontManager.getRunescapeSmallFont());
		marker.setForeground(PanelArt.STEEL_LIGHT);
		right.add(marker);
		JPanel rightHolder = new JPanel(new GridBagLayout());
		rightHolder.setOpaque(false);
		rightHolder.add(right);
		titleRow.add(rightHolder, BorderLayout.EAST);
		header.add(titleRow, BorderLayout.NORTH);
		header.add(summary, BorderLayout.SOUTH);
		plaque.add(header, BorderLayout.NORTH);

		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setOpaque(false);
		body.setVisible(false);
		plaque.add(body, BorderLayout.CENTER);

		ladder.setLayout(new BoxLayout(ladder, BoxLayout.Y_AXIS));
		ladder.setOpaque(false);
		ladder.setVisible(false);
		for (Promotions.Step step : Promotions.LADDER)
		{
			String need = step.days + " days" + (step.gz > 0 ? ", " + step.gz + " gz" : "");
			JPanel row = new JPanel(new BorderLayout(4, 0));
			row.setOpaque(false);
			row.add(small(gems(step.from, step.to)), BorderLayout.CENTER);
			JLabel needLabel = small(need);
			needLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			row.add(needLabel, BorderLayout.EAST);
			row.setAlignmentX(LEFT_ALIGNMENT);
			ladder.add(row);
		}
		ladderToggle.setFont(FontManager.getRunescapeSmallFont());
		ladderToggle.setForeground(PanelArt.STEEL_LIGHT);
		ladderToggle.setAlignmentX(LEFT_ALIGNMENT);
		ladderToggle.setToolTipText("What each step takes");
		clickable(ladderToggle, () -> ladder.setVisible(!ladder.isVisible()));

		header.setToolTipText("Click to show or hide");
		clickable(header, () -> body.setVisible(!body.isVisible()));
		updateMarkers();
		setVisible(false);
	}

	/**
	 * @param result the staff assistant, or null when you are not staff
	 * @param recent the clan's latest promotions, for everyone (empty with clan sync off)
	 * @param clanWide the gz's are the clan server's, not just this client's
	 */
	void fill(Promotions.Result result, List<RecentPromotion> recent, boolean clanWide)
	{
		setVisible(result != null || !recent.isEmpty());
		if (!isVisible() || (Objects.equals(result, shown) && recent.equals(shownRecent) && clanWide == shownClanWide
			&& body.getComponentCount() > 0))
		{
			return;
		}
		shown = result;
		shownRecent = recent;
		shownClanWide = clanWide;
		body.removeAll();

		if (result == null)
		{
			// everyone else: just the latest few
			badge.setVisible(false);
			RecentPromotion newest = recent.get(0);
			summary.setText("Latest: " + newest.name + ", now " + newest.to + ".");
			plaque.setLive(false);
			addRecent(recent);
			body.revalidate();
			body.repaint();
			return;
		}

		int due = result.due.size();
		badge.setText(due + " due");
		badge.setVisible(due > 0);
		summary.setText("Staff only. " + (due == 0 ? "Nobody is due" : due == 1 ? "1 member is due" : due + " members are due")
			+ ", " + result.soon.size() + " coming up.");
		plaque.setLive(due > 0);

		addToBody(RankCard.heading("Ready for promotion"));
		if (due == 0)
		{
			addToBody(note("Nobody is due right now."));
		}
		for (Promotions.Candidate c : result.due)
		{
			addToBody(dueRow(c));
		}

		body.add(Box.createVerticalStrut(8));
		addToBody(RankCard.heading("Coming up"));
		if (result.soon.isEmpty())
		{
			addToBody(note("Nobody is within a week of their next gem."));
		}
		List<Promotions.Candidate> soon = result.soon;
		for (int i = 0; i < Math.min(SOON_ROWS, soon.size()); i++)
		{
			addToBody(soonRow(soon.get(i)));
		}
		if (soon.size() > SOON_ROWS)
		{
			addToBody(note("and " + (soon.size() - SOON_ROWS) + " more"));
		}

		if (!recent.isEmpty())
		{
			body.add(Box.createVerticalStrut(8));
			addRecent(recent);
		}

		body.add(Box.createVerticalStrut(8));
		addToBody(ladderToggle);
		body.add(Box.createVerticalStrut(2));
		ladder.setAlignmentX(LEFT_ALIGNMENT);
		body.add(ladder);
		body.add(Box.createVerticalStrut(6));
		addToBody(note("Only staff see the list above; everyone sees the recent promotions. Change ranks in the game: "
			+ "Clan Settings > Members. Time is from each member's join date. "
			+ (clanWide
				? "Gz's are all-time given, counted by the clan server."
				: "Gz's are only this client's counts; turn on Clan sync for the clan's.")
			+ " Members whose gz's aren't counted (no plugin) count as 0 gz's."));
		body.revalidate();
		body.repaint();
	}

	private void addRecent(List<RecentPromotion> recent)
	{
		addToBody(RankCard.heading("Recently promoted"));
		long now = System.currentTimeMillis();
		for (RecentPromotion p : recent)
		{
			addToBody(recentRow(p, now));
		}
	}

	/** gem icon, name and how long ago, "Opal > Sapphire" */
	private static JPanel recentRow(RecentPromotion p, long now)
	{
		InsetPanel row = new InsetPanel(new BorderLayout(6, 0));
		row.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
		row.add(gemLabel(p.to), BorderLayout.WEST);
		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		JPanel line = new JPanel(new BorderLayout(4, 0));
		line.setOpaque(false);
		line.setAlignmentX(LEFT_ALIGNMENT);
		JLabel name = new JLabel(p.name);
		name.setFont(FontManager.getRunescapeBoldFont());
		name.setForeground(Color.WHITE);
		line.add(name, BorderLayout.CENTER);
		if (p.at > 0)
		{
			JLabel when = small(ago(now - p.at));
			when.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			line.add(when, BorderLayout.EAST);
		}
		text.add(line);
		text.add(small(gems(CorClanPanel.escape(p.from), CorClanPanel.escape(p.to))));
		row.add(text, BorderLayout.CENTER);
		return row;
	}

	/** "just now", "5m ago", "3h ago", "2d ago" */
	static String ago(long millis)
	{
		long minutes = Math.max(0, millis) / 60_000L;
		if (minutes < 1)
		{
			return "just now";
		}
		if (minutes < 60)
		{
			return minutes + "m ago";
		}
		long hours = minutes / 60;
		return hours < 24 ? hours + "h ago" : hours / 24 + "d ago";
	}

	private void addToBody(JComponent component)
	{
		if (body.getComponentCount() > 0 && component instanceof InsetPanel)
		{
			body.add(Box.createVerticalStrut(4));
		}
		component.setAlignmentX(LEFT_ALIGNMENT);
		body.add(component);
	}

	/** gem icon, name (+ online), "Opal > Emerald" in the gems' colours, then the time and gz's */
	private static JPanel dueRow(Promotions.Candidate c)
	{
		InsetPanel row = new InsetPanel(new BorderLayout(6, 0));
		row.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		row.add(gemLabel(c.to), BorderLayout.WEST);

		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		text.add(nameLine(c));
		text.add(small(gems(c.from, c.to)));
		JLabel detail = small(days(c.days) + " in clan, " + (c.gz == null ? "no gz's counted" : c.gz + " gz given"));
		detail.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		text.add(detail);
		row.add(text, BorderLayout.CENTER);
		return row;
	}

	/** gem icon, name (+ online), "Sapphire in 3 days" */
	private static JPanel soonRow(Promotions.Candidate c)
	{
		InsetPanel row = new InsetPanel(new BorderLayout(6, 0));
		row.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
		row.add(gemLabel(c.to), BorderLayout.WEST);
		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		text.add(nameLine(c));
		JLabel still = small("<html><font color='" + hex(colorOf(c.to)) + "'>" + c.to + "</font> in "
			+ CorClanPanel.escape(c.still()) + "</html>");
		still.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		text.add(still);
		row.add(text, BorderLayout.CENTER);
		return row;
	}

	private static JPanel nameLine(Promotions.Candidate c)
	{
		JPanel line = new JPanel(new BorderLayout(4, 0));
		line.setOpaque(false);
		line.setAlignmentX(LEFT_ALIGNMENT);
		JLabel name = new JLabel(c.name);
		name.setFont(FontManager.getRunescapeBoldFont());
		name.setForeground(Color.WHITE);
		line.add(name, BorderLayout.CENTER);
		if (c.online)
		{
			JLabel online = small("online");
			online.setForeground(ONLINE);
			line.add(online, BorderLayout.EAST);
		}
		return line;
	}

	private static JLabel gemLabel(String title)
	{
		JLabel icon = new JLabel(new ImageIcon(PanelArt.gemIcon(colorOf(title))));
		icon.setVerticalAlignment(SwingConstants.TOP);
		return icon;
	}

	/** "Opal > Emerald", each in its gem's colour (plain ASCII: the RuneScape font has no arrows) */
	private static String gems(String from, String to)
	{
		return "<html><font color='" + hex(colorOf(from)) + "'>" + from + "</font> &gt; <font color='"
			+ hex(colorOf(to)) + "'>" + to + "</font></html>";
	}

	private static Color colorOf(String title)
	{
		for (GemGlow gem : GemGlow.values())
		{
			if (gem.title.equalsIgnoreCase(title.trim()))
			{
				return RankCard.gemColor(gem);
			}
		}
		return Color.WHITE;
	}

	private static String hex(Color c)
	{
		return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
	}

	private static String days(int n)
	{
		return n == 1 ? "1 day" : n + " days";
	}

	private static JLabel small(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(Color.WHITE);
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private static WrapText note(String text)
	{
		WrapText note = new WrapText(FontManager.getRunescapeSmallFont(), ColorScheme.LIGHT_GRAY_COLOR);
		note.setText(text);
		return note;
	}

	/** a hand cursor and a toggle on click, for the component and everything in it */
	private void clickable(Component component, Runnable toggle)
	{
		MouseAdapter click = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				toggle.run();
				updateMarkers();
				revalidate();
				repaint();
			}
		};
		hook(component, click);
	}

	private static void hook(Component component, MouseAdapter click)
	{
		component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		component.addMouseListener(click);
		if (component instanceof java.awt.Container)
		{
			for (Component child : ((java.awt.Container) component).getComponents())
			{
				hook(child, click);
			}
		}
	}

	/** plain ASCII markers: the RuneScape font has no arrow glyphs */
	private void updateMarkers()
	{
		marker.setText(body.isVisible() ? "[-]" : "[+]");
		ladderToggle.setText((ladder.isVisible() ? "[-] " : "[+] ") + "The ladder");
	}
}

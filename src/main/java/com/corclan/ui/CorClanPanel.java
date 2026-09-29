package com.corclan.ui;

import com.corclan.CorClanConfig;
import com.corclan.gz.BroadcastRecord;
import com.corclan.gz.Streaks;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

/**
 * The CoR sidebar: links, gz leaderboards (clan-wide when sync is on, otherwise this client's own
 * counts), recent clan broadcasts, the gz podiums and weekly #1 streaks.
 */
public class CorClanPanel extends PluginPanel
{
	private static final int LEADERBOARD_SIZE = 5;
	private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm");
	private static final SimpleDateFormat WEEK_DAY = new SimpleDateFormat("EEE d MMM");

	/** In-game item shown next to 1st, 2nd and 3rd place gz givers. Change here to use other items. */
	private static final int[] PODIUM_ITEMS = {
		ItemID.TWISTED_DRAGON_TROPHY,
		ItemID.TWISTED_RUNE_TROPHY,
		ItemID.TWISTED_ADAMANT_TROPHY,
	};

	private final CorClanConfig config;
	private final Runnable onReset;

	// each podium needs its own icon labels: a Swing component can only sit in one place
	private final JLabel[] podiumIcons;
	private final JLabel podiumTitle = new JLabel("GZ podium");
	private final JPanel podiumPanel = new JPanel();
	private final JLabel[] weeklyIcons;
	private final JPanel weeklyPanel = new JPanel();
	private final JPanel streakPanel = new JPanel();

	private final JLabel summaryLabel = new JLabel();
	private final JLabel giversTitle = new JLabel("Top gz givers");
	private final JLabel receiversTitle = new JLabel("Most gz'd");
	private final JPanel giversPanel = new JPanel();
	private final JPanel receiversPanel = new JPanel();
	private final JPanel broadcastsPanel = new JPanel();

	public CorClanPanel(CorClanConfig config, ItemManager itemManager, Runnable onReset)
	{
		super();
		this.config = config;
		this.onReset = onReset;
		podiumIcons = podiumIcons(itemManager);
		weeklyIcons = podiumIcons(itemManager);

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.setBorder(new EmptyBorder(0, 0, 0, 0));

		content.add(buildHeader());
		content.add(Box.createVerticalStrut(8));
		content.add(buildLinks());
		content.add(Box.createVerticalStrut(12));

		summaryLabel.setForeground(Color.WHITE);
		summaryLabel.setFont(FontManager.getRunescapeSmallFont());
		content.add(section("GZ tracker", summaryLabel));
		content.add(Box.createVerticalStrut(8));

		content.add(section(giversTitle, giversPanel));
		content.add(Box.createVerticalStrut(8));
		content.add(section(receiversTitle, receiversPanel));
		content.add(Box.createVerticalStrut(8));
		content.add(section("Recent broadcasts", broadcastsPanel));
		content.add(Box.createVerticalStrut(12));

		JButton reset = new JButton("Reset all-time stats");
		reset.setFocusable(false);
		reset.addActionListener(e ->
		{
			int choice = JOptionPane.showConfirmDialog(this,
				"Clear all gz counts stored on this client?", "CoR Clan",
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (choice == JOptionPane.YES_OPTION)
			{
				onReset.run();
			}
		});
		content.add(reset);
		content.add(Box.createVerticalStrut(12));

		content.add(section(podiumTitle, podiumPanel));
		content.add(Box.createVerticalStrut(8));
		content.add(section("Weekly GZ podium", weeklyPanel));
		content.add(Box.createVerticalStrut(8));
		content.add(section("Streak", streakPanel));

		add(content, BorderLayout.NORTH);
	}

	private JPanel buildHeader()
	{
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);

		BufferedImage logo = ImageUtil.loadImageResource(CorClanPanel.class, "/com/corclan/logo.png");
		JLabel logoLabel = new JLabel(new ImageIcon(ImageUtil.resizeImage(logo, 64, 64)));
		logoLabel.setAlignmentX(CENTER_ALIGNMENT);
		header.add(logoLabel);

		JLabel title = new JLabel("C o R");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.BRAND_ORANGE);
		title.setAlignmentX(CENTER_ALIGNMENT);
		header.add(title);

		JLabel sub = new JLabel("Clan plugin");
		sub.setFont(FontManager.getRunescapeSmallFont());
		sub.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		sub.setAlignmentX(CENTER_ALIGNMENT);
		header.add(sub);
		return header;
	}

	private JPanel buildLinks()
	{
		JPanel links = new JPanel(new GridLayout(1, 2, 6, 0));
		links.setBackground(ColorScheme.DARK_GRAY_COLOR);
		links.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

		JButton discord = new JButton("Discord");
		discord.setFocusable(false);
		discord.addActionListener(e -> open(config.discordUrl()));
		links.add(discord);

		JButton site = new JButton("Website");
		site.setFocusable(false);
		site.addActionListener(e -> open(config.websiteUrl()));
		links.add(site);
		return links;
	}

	private static void open(String url)
	{
		if (url != null && !url.trim().isEmpty())
		{
			LinkBrowser.browse(url.trim());
		}
	}

	private static JPanel section(String title, JPanel body)
	{
		return section(new JLabel(title), body);
	}

	private static JPanel section(JLabel label, JPanel body)
	{
		JPanel wrapper = new JPanel(new BorderLayout(0, 4));
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);

		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(Color.WHITE);
		wrapper.add(label, BorderLayout.NORTH);

		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		body.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		wrapper.add(body, BorderLayout.CENTER);
		return wrapper;
	}

	private static JPanel section(String title, JLabel body)
	{
		JPanel panel = new JPanel();
		panel.add(body);
		return section(title, panel);
	}

	/** Must be called on the Swing thread with a snapshot built on the client thread. */
	public void refresh(PanelData data)
	{
		summaryLabel.setText("<html>" + escape(data.mine) + "<br>" + escape(data.summary) + "<br>" + escape(data.syncStatus) + "</html>");

		String scope = data.clanWide ? " (clan)" : " (this client)";
		giversTitle.setText("Top gz givers" + scope);
		receiversTitle.setText("Most gz'd" + scope);
		fillLeaderboard(giversPanel, data.givers.subList(0, Math.min(LEADERBOARD_SIZE, data.givers.size())), "No gz's counted yet");
		fillLeaderboard(receiversPanel, data.receivers.subList(0, Math.min(LEADERBOARD_SIZE, data.receivers.size())), "Nobody gz'd yet");
		fillBroadcasts(data.recent);

		podiumTitle.setText("GZ podium" + scope);
		fillPodium(podiumPanel, podiumIcons, data.givers);
		fillPodium(weeklyPanel, weeklyIcons, data.weeklyGivers);
		weeklyPanel.add(muted("Since " + WEEK_DAY.format(new Date(data.weekStart)) + ", resets Sunday"), 0);
		fillStreaks(data.longestStreak, data.currentStreak);

		revalidate();
		repaint();
	}

	/** Player names go into an HTML label; never let them inject markup. */
	private static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static void fillLeaderboard(JPanel panel, List<Map.Entry<String, Integer>> rows, String emptyText)
	{
		panel.removeAll();
		if (rows.isEmpty())
		{
			panel.add(muted(emptyText));
			return;
		}
		int place = 1;
		for (Map.Entry<String, Integer> row : rows)
		{
			panel.add(line(place + ". " + row.getKey(), String.valueOf(row.getValue())));
			place++;
		}
	}

	private static JLabel[] podiumIcons(ItemManager itemManager)
	{
		JLabel[] icons = new JLabel[PODIUM_ITEMS.length];
		for (int i = 0; i < icons.length; i++)
		{
			icons[i] = new JLabel();
			icons[i].setPreferredSize(new Dimension(36, 32));
			// sprites load from the game cache once the client is ready; addTo sets the icon when they arrive
			itemManager.getImage(PODIUM_ITEMS[i]).addTo(icons[i]);
		}
		return icons;
	}

	/** Top {@code icons.length} of an already sorted leaderboard. */
	private static void fillPodium(JPanel panel, JLabel[] icons, List<Map.Entry<String, Integer>> ranked)
	{
		panel.removeAll();
		if (ranked.isEmpty())
		{
			panel.add(muted("No gz's counted yet"));
			return;
		}
		for (int i = 0; i < Math.min(icons.length, ranked.size()); i++)
		{
			Map.Entry<String, Integer> entry = ranked.get(i);
			JPanel row = line((i + 1) + ". " + entry.getKey(), entry.getValue() + " gz");
			row.add(icons[i], BorderLayout.WEST);
			panel.add(row);
		}
	}

	private void fillStreaks(Streaks.Streak longest, Streaks.Streak current)
	{
		streakPanel.removeAll();
		if (longest == null)
		{
			streakPanel.add(muted("<html>Weeks in a row as weekly #1 gz giver.<br>The first winner is recorded when this week ends.</html>"));
			return;
		}
		streakPanel.add(line("Record: " + String.join(", ", longest.players), weeks(longest.weeks)));
		streakPanel.add(current == null
			? muted("Current: nobody, last week had no #1")
			: line("Current: " + String.join(", ", current.players), weeks(current.weeks)));
	}

	private static String weeks(int n)
	{
		return n == 1 ? "1 week" : n + " weeks";
	}

	private void fillBroadcasts(List<BroadcastRecord> recent)
	{
		broadcastsPanel.removeAll();
		if (recent.isEmpty())
		{
			broadcastsPanel.add(muted("No clan broadcasts seen yet"));
			return;
		}
		for (BroadcastRecord record : recent)
		{
			String when = TIME.format(new Date(record.getTimestamp()));
			broadcastsPanel.add(line(when + "  " + record.getSubject(), record.getGzCount() + " gz"));
		}
	}

	private static JLabel muted(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	private static JPanel line(String left, String right)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		JLabel l = new JLabel(left);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(Color.WHITE);
		JLabel r = new JLabel(right, SwingConstants.RIGHT);
		r.setFont(FontManager.getRunescapeSmallFont());
		r.setForeground(ColorScheme.BRAND_ORANGE);
		row.add(l, BorderLayout.CENTER);
		row.add(r, BorderLayout.EAST);
		return row;
	}
}

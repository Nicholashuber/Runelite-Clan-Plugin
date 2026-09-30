package com.corclan.ui;

import com.corclan.CorClanConfig;
import com.corclan.clan.ClanRoster;
import com.corclan.clan.OrgChart;
import com.corclan.gz.BroadcastRecord;
import com.corclan.gz.Streaks;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

/**
 * The CoR sidebar: links, gz leaderboards (clan-wide when sync is on, otherwise this client's own
 * counts), recent clan broadcasts, the gz podiums and weekly #1 streaks.
 */
public class CorClanPanel extends PluginPanel
{
	private static final int LEADERBOARD_SIZE = 5;
	private static final int GIVERS_SIZE = 10;
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
	private final JLabel allGiversHeader = new JLabel();
	// collapsible body: search box on top, (filtered) list below
	private final JPanel allGiversPanel = new JPanel();
	private final IconTextField allGiversSearch = new IconTextField();
	private final JPanel allGiversList = new JPanel();
	private List<Map.Entry<String, Integer>> allGivers = Collections.emptyList();
	// clan members by in-game rank; collapsible like All gz givers
	private final JLabel clanHeader = new JLabel();
	private final JPanel clanPanel = new JPanel();
	private List<ClanRoster.RankGroup> clanRoster;
	private Map<Integer, String> clanRankTitles = Collections.emptyMap();
	private Map<String, Integer> clanGzCounts = Collections.emptyMap();

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
		content.add(Box.createVerticalStrut(8));
		content.add(collapsible(allGiversHeader, allGiversPanel, this::updateAllGiversHeader));
		buildAllGiversSearch();
		content.add(Box.createVerticalStrut(8));
		content.add(collapsible(clanHeader, clanPanel, this::updateClanHeader));

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

	/**
	 * A section whose body is hidden until the header is clicked. Starts collapsed.
	 * @param updateHeader redraws the header text (with its [+]/[-] marker) after a toggle
	 */
	private JPanel collapsible(JLabel header, JPanel body, Runnable updateHeader)
	{
		JPanel wrapper = section(header, body);
		body.setVisible(false);
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		header.setToolTipText("Click to show or hide");
		header.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				body.setVisible(!body.isVisible());
				updateHeader.run();
				revalidate();
				repaint();
			}
		});
		return wrapper;
	}

	/** plain ASCII: the RuneScape font has no arrow glyphs */
	private static String marker(JPanel body)
	{
		return body.isVisible() ? "[-] " : "[+] ";
	}

	private void updateAllGiversHeader()
	{
		allGiversHeader.setText(marker(allGiversPanel) + "All gz givers (this client, " + allGivers.size() + ")");
	}

	private void updateClanHeader()
	{
		String counts = clanRoster == null
			? ""
			: " (" + ClanRoster.total(clanRoster) + ", " + ClanRoster.online(clanRoster) + " online)";
		clanHeader.setText(marker(clanPanel) + "Clan members" + counts);
	}

	/**
	 * Rebuilds the chart only when something it shows changed; a big clan is hundreds of rows.
	 * @param gzGiven all-time gz's given per player on this client, shown in the competitive trees
	 */
	private void fillClanRoster(List<ClanRoster.RankGroup> roster, Map<Integer, String> rankTitles, Map<String, Integer> gzGiven)
	{
		// only the counts of players in the chart matter; ignore gz's from people outside the clan
		Map<String, Integer> counts = new HashMap<>();
		if (roster != null)
		{
			for (ClanRoster.RankGroup g : roster)
			{
				for (ClanRoster.Member m : g.members)
				{
					counts.put(m.name, gzGiven.getOrDefault(m.name, 0));
				}
			}
		}
		if (Objects.equals(roster, clanRoster) && rankTitles.equals(clanRankTitles) && counts.equals(clanGzCounts)
			&& clanPanel.getComponentCount() > 0)
		{
			return;
		}
		clanRoster = roster;
		clanRankTitles = rankTitles;
		clanGzCounts = counts;
		clanPanel.removeAll();
		if (roster == null)
		{
			clanPanel.add(muted("Log in and join a clan to see its members"));
		}
		else
		{
			OrgChart.Chart chart = OrgChart.build(roster, rankTitles);
			addChain(chart.staff, null);
			clanPanel.add(Box.createVerticalStrut(12));
			addChain(chart.competitive, counts);
			if (!chart.others.isEmpty())
			{
				clanPanel.add(Box.createVerticalStrut(12));
				JPanel box = tierBox("Other ranks");
				for (ClanRoster.RankGroup group : chart.others)
				{
					addRank(box, null, group.title, group.members, group.onlineCount(), null);
				}
				clanPanel.add(box);
			}
		}
		clanPanel.revalidate();
		clanPanel.repaint();
	}

	private static Map<String, Integer> toMap(List<Map.Entry<String, Integer>> entries)
	{
		Map<String, Integer> map = new HashMap<>();
		for (Map.Entry<String, Integer> e : entries)
		{
			map.put(e.getKey(), e.getValue());
		}
		return map;
	}

	/**
	 * Tier boxes joined by connector lines.
	 * @param gzCounts shown next to each member when non-null (the competitive trees)
	 */
	private void addChain(List<OrgChart.Tier> tiers, Map<String, Integer> gzCounts)
	{
		for (int i = 0; i < tiers.size(); i++)
		{
			if (i > 0)
			{
				clanPanel.add(connector());
			}
			OrgChart.Tier tier = tiers.get(i);
			JPanel box = tierBox(tier.name);
			for (OrgChart.Slot slot : tier.slots)
			{
				addRank(box, slot.grade, slot.title, slot.members, slot.onlineCount(), gzCounts);
			}
			clanPanel.add(box);
		}
	}

	/** A bordered box for one tier of the org chart, labelled at the top. */
	private static JPanel tierBox(String name)
	{
		JPanel box = new JPanel();
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		box.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(3, 5, 4, 5)));
		// wrapped in a row like everything else in the box, so BoxLayout aligns them all the same way
		JPanel label = new JPanel(new BorderLayout());
		label.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		label.add(muted(name), BorderLayout.CENTER);
		box.add(label);
		return box;
	}

	/** The short vertical line joining one tier's box to the next. */
	private static JPanel connector()
	{
		JPanel line = new JPanel();
		line.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
		line.setPreferredSize(new Dimension(2, 10));
		line.setMaximumSize(new Dimension(2, 10));
		line.setAlignmentX(CENTER_ALIGNMENT);
		JPanel holder = new JPanel();
		holder.setLayout(new BoxLayout(holder, BoxLayout.Y_AXIS));
		holder.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		holder.add(line);
		return holder;
	}

	/**
	 * A rank heading followed by its members, or "vacant" when nobody holds it.
	 * @param grade    letter grade shown before the title (competitive tiers), or null
	 * @param gzCounts when non-null, each member's gz count replaces the "online" label
	 */
	private static void addRank(JPanel box, String grade, String title, List<ClanRoster.Member> members, int online,
		Map<String, Integer> gzCounts)
	{
		box.add(Box.createVerticalStrut(3));
		box.add(rankRow(grade == null ? title : grade + "  " + title, members.size(), online));
		if (members.isEmpty())
		{
			JLabel vacant = muted("vacant");
			vacant.setBorder(new EmptyBorder(0, 10, 0, 0));
			JPanel row = new JPanel(new BorderLayout());
			row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			row.add(vacant, BorderLayout.CENTER);
			box.add(row);
		}
		for (ClanRoster.Member member : members)
		{
			box.add(memberRow(member, gzCounts == null ? null : gzCounts.getOrDefault(member.name, 0)));
		}
	}

	/** Rank title in bold, member count (and how many are online) on the right. */
	private static JPanel rankRow(String rankTitle, int size, int online)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		JLabel title = new JLabel(rankTitle);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.BRAND_ORANGE);
		JLabel count = new JLabel(size == 0 ? "" : size + (online > 0 ? " (" + online + " online)" : ""), SwingConstants.RIGHT);
		count.setFont(FontManager.getRunescapeSmallFont());
		count.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		row.add(title, BorderLayout.CENTER);
		row.add(count, BorderLayout.EAST);
		return row;
	}

	/**
	 * Indented name, green when online. On the right: their gz count when {@code gz} is given
	 * (competitive trees), otherwise "online" for online members.
	 */
	private static JPanel memberRow(ClanRoster.Member member, Integer gz)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(0, 10, 0, 0));
		JLabel name = new JLabel(member.name);
		name.setFont(FontManager.getRunescapeSmallFont());
		name.setForeground(member.online ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
		row.add(name, BorderLayout.CENTER);
		String right = gz != null ? gz + " gz" : member.online ? "online" : null;
		if (right != null)
		{
			JLabel status = new JLabel(right, SwingConstants.RIGHT);
			status.setFont(FontManager.getRunescapeSmallFont());
			status.setForeground(gz != null ? ColorScheme.BRAND_ORANGE : ColorScheme.PROGRESS_COMPLETE_COLOR);
			row.add(status, BorderLayout.EAST);
		}
		return row;
	}

	private void buildAllGiversSearch()
	{
		allGiversSearch.setIcon(IconTextField.Icon.SEARCH);
		allGiversSearch.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
		allGiversSearch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		allGiversSearch.setBackground(ColorScheme.DARK_GRAY_COLOR);
		allGiversSearch.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		allGiversSearch.setToolTipText("Search by name");
		allGiversSearch.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				fillAllGivers();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				fillAllGivers();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				fillAllGivers();
			}
		});

		allGiversList.setLayout(new BoxLayout(allGiversList, BoxLayout.Y_AXIS));
		allGiversList.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		allGiversPanel.add(allGiversSearch);
		allGiversPanel.add(Box.createVerticalStrut(4));
		allGiversPanel.add(allGiversList);
	}

	/** Every giver matching the search box (case-insensitive, any part of the name), keeping their real rank. */
	private void fillAllGivers()
	{
		allGiversList.removeAll();
		String query = allGiversSearch.getText().trim().toLowerCase(Locale.ROOT);
		if (allGivers.isEmpty())
		{
			allGiversList.add(muted("No gz's counted yet"));
		}
		else
		{
			int shown = 0;
			for (int i = 0; i < allGivers.size(); i++)
			{
				Map.Entry<String, Integer> entry = allGivers.get(i);
				if (query.isEmpty() || entry.getKey().toLowerCase(Locale.ROOT).contains(query))
				{
					allGiversList.add(line((i + 1) + ". " + entry.getKey(), String.valueOf(entry.getValue())));
					shown++;
				}
			}
			if (shown == 0)
			{
				// don't echo the query: Swing would render typed "<html>..." as markup
				allGiversList.add(muted("No one matches your search"));
			}
		}
		allGiversList.revalidate();
		allGiversList.repaint();
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
		fillLeaderboard(giversPanel, data.givers.subList(0, Math.min(GIVERS_SIZE, data.givers.size())), "No gz's counted yet");
		fillLeaderboard(receiversPanel, data.receivers.subList(0, Math.min(LEADERBOARD_SIZE, data.receivers.size())), "Nobody gz'd yet");
		fillBroadcasts(data.recent);

		podiumTitle.setText("GZ podium" + scope);
		fillPodium(podiumPanel, podiumIcons, data.givers);
		fillPodium(weeklyPanel, weeklyIcons, data.weeklyGivers);
		weeklyPanel.add(muted("Since " + WEEK_DAY.format(new Date(data.weekStart)) + ", resets Sunday"), 0);
		fillStreaks(data.longestStreak, data.currentStreak);
		allGivers = data.allGivers;
		updateAllGiversHeader();
		fillAllGivers();
		fillClanRoster(data.clanRoster, data.clanRankTitles, toMap(data.allGivers));
		updateClanHeader();

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

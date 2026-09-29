package com.corclan.ui;

import com.corclan.CorClanConfig;
import com.corclan.gz.BroadcastRecord;
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
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

/**
 * The CoR sidebar: links, gz leaderboards (clan-wide when sync is on, otherwise this client's own
 * counts) and recent clan broadcasts.
 */
public class CorClanPanel extends PluginPanel
{
	private static final int LEADERBOARD_SIZE = 5;
	private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm");

	private final CorClanConfig config;
	private final Runnable onReset;

	private final JLabel summaryLabel = new JLabel();
	private final JLabel giversTitle = new JLabel("Top gz givers");
	private final JLabel receiversTitle = new JLabel("Most gz'd");
	private final JPanel giversPanel = new JPanel();
	private final JPanel receiversPanel = new JPanel();
	private final JPanel broadcastsPanel = new JPanel();

	public CorClanPanel(CorClanConfig config, Runnable onReset)
	{
		super();
		this.config = config;
		this.onReset = onReset;

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

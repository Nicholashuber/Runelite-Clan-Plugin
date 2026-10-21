package com.corclan.ui;

import com.corclan.raid.Raid;
import com.corclan.raid.RaidAd;
import com.corclan.raid.RaidListing;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The raid group finder, under the rank card: clanmates looking for a party for CoX, ToB or ToA, with how many slots
 * are filled, on the same art plaque as the banner. Anyone with clan sync on can put up one group of their own and
 * update or take it down. Click the title to open or close it.
 */
public final class RaidFinderCard extends JPanel
{
	/** What the card asks the plugin to do (on the Swing thread). */
	public interface Actions
	{
		void post(RaidAd ad);

		void stop();

		/** posts our group in the clan Discord's looking-for-group channel */
		void ping();
	}

	private final ArtPlaque plaque = new ArtPlaque(new BorderLayout(0, 6));
	private final JLabel badge = new JLabel();
	private final JLabel marker = new JLabel();
	private final WrapText summary = new WrapText(FontManager.getRunescapeSmallFont(), ColorScheme.LIGHT_GRAY_COLOR);
	private final JPanel body = new JPanel();
	private final JPanel listings = new JPanel();
	private final WrapText hint = new WrapText(FontManager.getRunescapeSmallFont(), ColorScheme.LIGHT_GRAY_COLOR);
	private final JLabel mine = new JLabel();

	private final JComboBox<Raid> raidBox = new JComboBox<>(Raid.values());
	private final SpinnerNumberModel filledModel = new SpinnerNumberModel(1, 1, Raid.COX.defaultSize, 1);
	private final SpinnerNumberModel sizeModel = new SpinnerNumberModel(Raid.COX.defaultSize, 2, Raid.COX.maxSize, 1);
	private final JTextField note = new JTextField();
	private final JButton postButton = new ArtButton("Post group");
	private final JButton stopButton = new ArtButton("Take down");
	private final JButton pingButton = new ArtButton("Ping Discord");

	private List<RaidListing> shown = Collections.emptyList();
	private String shownMe;
	private RaidAd shownAd;
	private long shownMinute = -1;

	RaidFinderCard(Actions actions)
	{
		super(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(Box.createVerticalStrut(8), BorderLayout.NORTH);
		add(plaque, BorderLayout.CENTER);

		JPanel header = new JPanel(new BorderLayout(0, 2));
		header.setOpaque(false);
		JPanel titleRow = new JPanel(new BorderLayout(6, 0));
		titleRow.setOpaque(false);
		JLabel title = new JLabel("Raid finder", new ImageIcon(PanelArt.skull()), SwingConstants.LEFT);
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

		listings.setLayout(new BoxLayout(listings, BoxLayout.Y_AXIS));
		listings.setOpaque(false);
		listings.setAlignmentX(LEFT_ALIGNMENT);
		body.add(listings);
		body.add(Box.createVerticalStrut(8));
		JLabel yours = RankCard.heading("Your group");
		yours.setAlignmentX(LEFT_ALIGNMENT);
		body.add(yours);
		mine.setFont(FontManager.getRunescapeSmallFont());
		mine.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		mine.setAlignmentX(LEFT_ALIGNMENT);
		body.add(mine);
		body.add(Box.createVerticalStrut(4));
		JPanel form = buildForm(actions);
		form.setAlignmentX(LEFT_ALIGNMENT);
		body.add(form);
		hint.setAlignmentX(LEFT_ALIGNMENT);
		body.add(hint);

		header.setToolTipText("Click to show or hide");
		hookClicks(header, new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				body.setVisible(!body.isVisible());
				updateMarker();
				revalidate();
				repaint();
			}
		});
		updateMarker();
	}

	/** Raid, filled of size, a note, then Post / Take down. */
	private JPanel buildForm(Actions actions)
	{
		InsetPanel form = new InsetPanel(new BorderLayout(0, 4));
		form.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

		JPanel grid = new JPanel(new GridLayout(0, 2, 4, 4));
		grid.setOpaque(false);
		grid.add(caption("Raid"));
		raidBox.setFocusable(false);
		raidBox.setSelectedItem(Raid.COX);
		grid.add(raidBox);
		grid.add(caption("Slots filled"));
		grid.add(new JSpinner(filledModel));
		grid.add(caption("Party size"));
		grid.add(new JSpinner(sizeModel));
		form.add(grid, BorderLayout.NORTH);

		JPanel noteRow = new JPanel(new BorderLayout(0, 2));
		noteRow.setOpaque(false);
		noteRow.add(caption("Note (optional, e.g. \"need a tank, 300+ kc\")"), BorderLayout.NORTH);
		note.setToolTipText("Up to " + RaidAd.MAX_NOTE + " characters");
		noteRow.add(note, BorderLayout.CENTER);
		form.add(noteRow, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
		buttons.setOpaque(false);
		buttons.add(postButton);
		buttons.add(stopButton);
		JPanel actionsPanel = new JPanel(new BorderLayout(0, 4));
		actionsPanel.setOpaque(false);
		actionsPanel.add(buttons, BorderLayout.NORTH);
		pingButton.setToolTipText("Post your group in the looking-for-group channel of the clan Discord (once every 10 minutes)");
		actionsPanel.add(pingButton, BorderLayout.SOUTH);
		form.add(actionsPanel, BorderLayout.SOUTH);

		// party size follows the raid; filled can never pass the size
		raidBox.addActionListener(e ->
		{
			Raid raid = (Raid) raidBox.getSelectedItem();
			if (raid != null)
			{
				sizeModel.setMaximum(raid.maxSize);
				sizeModel.setValue(raid.defaultSize);
			}
		});
		sizeModel.addChangeListener(e ->
		{
			int size = sizeModel.getNumber().intValue();
			filledModel.setMaximum(size);
			if (filledModel.getNumber().intValue() > size)
			{
				filledModel.setValue(size);
			}
		});
		postButton.addActionListener(e ->
		{
			Raid raid = (Raid) raidBox.getSelectedItem();
			if (raid != null)
			{
				actions.post(new RaidAd(raid, filledModel.getNumber().intValue(), sizeModel.getNumber().intValue(), note.getText()));
			}
		});
		stopButton.addActionListener(e -> actions.stop());
		pingButton.addActionListener(e -> actions.ping());
		return form;
	}

	/**
	 * @param board every open group, newest first
	 * @param me our character name, or null when logged out
	 * @param myAd our own group as last posted, or null
	 * @param problem why we can't post (clan sync off, logged out), or null
	 */
	void fill(List<RaidListing> board, String me, RaidAd myAd, String problem)
	{
		badge.setText(board.size() + (board.stream().allMatch(l -> l.raiding) ? " raiding" : " open"));
		badge.setVisible(!board.isEmpty());
		summary.setText(summary(board));
		plaque.setLive(!board.isEmpty());

		boolean canPost = problem == null;
		hint.setText(canPost ? "Your group shows as raiding while you're in the raid, and comes down 30 minutes after you"
			+ " log out." : problem);
		postButton.setEnabled(canPost);
		postButton.setText(myAd == null ? "Post group" : "Update");
		stopButton.setEnabled(canPost && myAd != null);
		pingButton.setEnabled(canPost && myAd != null);
		mine.setText(myAd == null ? "Not looking right now." : "Looking for " + myAd.raid.fullName + ", " + myAd.slots() + " filled.");
		if (myAd != null && !myAd.equals(shownAd))
		{
			// show what is posted (after a login the form starts fresh)
			raidBox.setSelectedItem(myAd.raid);
			sizeModel.setValue(myAd.size);
			filledModel.setValue(myAd.filled);
			note.setText(myAd.note);
		}
		shownAd = myAd;

		long minute = System.currentTimeMillis() / 60_000L;
		if (board.equals(shown) && Objects.equals(me, shownMe) && minute == shownMinute && listings.getComponentCount() > 0)
		{
			return;
		}
		shown = board;
		shownMe = me;
		shownMinute = minute;
		listings.removeAll();
		JLabel open = RankCard.heading("Looking for a party");
		open.setAlignmentX(LEFT_ALIGNMENT);
		listings.add(open);
		if (board.isEmpty())
		{
			WrapText none = new WrapText(FontManager.getRunescapeSmallFont(), ColorScheme.LIGHT_GRAY_COLOR);
			none.setText("No groups looking right now. Put yours up below.");
			none.setAlignmentX(LEFT_ALIGNMENT);
			listings.add(none);
		}
		long now = System.currentTimeMillis();
		for (RaidListing listing : board)
		{
			listings.add(Box.createVerticalStrut(4));
			JPanel row = row(listing, listing.isHost(me), now);
			row.setAlignmentX(LEFT_ALIGNMENT);
			listings.add(row);
		}
		listings.revalidate();
		listings.repaint();
	}

	/** "ToB 3/5, CoX 2/5 and 1 more" or a nudge when the board is empty */
	static String summary(List<RaidListing> board)
	{
		if (board.isEmpty())
		{
			return "Nobody is looking for a raid party right now.";
		}
		StringBuilder text = new StringBuilder("Looking: ");
		int listed = Math.min(2, board.size());
		for (int i = 0; i < listed; i++)
		{
			RaidListing l = board.get(i);
			text.append(i > 0 ? ", " : "").append(l.raid.shortName).append(' ').append(l.slots())
				.append(l.raiding ? " raiding" : "");
		}
		if (board.size() > listed)
		{
			text.append(" and ").append(board.size() - listed).append(" more");
		}
		return text.append('.').toString();
	}

	/** raid tag | host (you) and world, slots pips and age, the note */
	private static JPanel row(RaidListing l, boolean mine, long now)
	{
		InsetPanel row = new InsetPanel(new BorderLayout(6, 0));
		row.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

		JLabel tag = new JLabel(l.raid.shortName, SwingConstants.CENTER);
		tag.setFont(FontManager.getRunescapeBoldFont());
		tag.setForeground(Color.WHITE);
		tag.setBackground(PanelArt.mix(l.raid.color, Color.BLACK, 0.25f));
		tag.setOpaque(true);
		tag.setBorder(new EmptyBorder(2, 4, 2, 4));
		tag.setToolTipText(l.raid.fullName);
		JPanel tagHolder = new JPanel(new BorderLayout());
		tagHolder.setOpaque(false);
		tagHolder.add(tag, BorderLayout.NORTH);
		row.add(tagHolder, BorderLayout.WEST);

		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);

		JPanel top = new JPanel(new BorderLayout(4, 0));
		top.setOpaque(false);
		top.setAlignmentX(LEFT_ALIGNMENT);
		boolean away = l.isAway(now);
		JLabel name = new JLabel(mine ? l.name + " (you)" : away ? l.name + " (away)" : l.name);
		if (away)
		{
			name.setToolTipText("Logged out; the group stays up for an hour");
		}
		name.setFont(FontManager.getRunescapeBoldFont());
		name.setForeground(mine ? PanelArt.mix(l.raid.color, Color.WHITE, 0.4f) : away ? ColorScheme.LIGHT_GRAY_COLOR : Color.WHITE);
		top.add(name, BorderLayout.CENTER);
		if (l.world > 0)
		{
			JLabel world = small("W" + l.world, PanelArt.STEEL_LIGHT);
			world.setToolTipText("World " + l.world);
			top.add(world, BorderLayout.EAST);
		}
		text.add(top);

		JPanel slots = new JPanel(new BorderLayout(4, 0));
		slots.setOpaque(false);
		slots.setAlignmentX(LEFT_ALIGNMENT);
		JLabel filled = small(l.raiding ? l.slots() + " raiding now" : l.slots() + " filled", l.raid.color);
		if (l.size <= PanelArt.MAX_PIPS)
		{
			filled.setIcon(new ImageIcon(PanelArt.pips(l.filled, l.size, l.raid.color)));
			filled.setIconTextGap(5);
		}
		slots.add(filled, BorderLayout.CENTER);
		if (l.postedAt > 0)
		{
			slots.add(small(PromotionsCard.ago(now - l.postedAt), ColorScheme.LIGHT_GRAY_COLOR), BorderLayout.EAST);
		}
		text.add(slots);

		if (!l.room.isEmpty())
		{
			JLabel room = small(l.roomLabel(), PanelArt.mix(l.raid.color, Color.WHITE, 0.4f));
			room.setAlignmentX(LEFT_ALIGNMENT);
			room.setToolTipText("Where the party is in " + l.raid.fullName + " right now");
			text.add(room);
		}

		if (!l.note.isEmpty())
		{
			WrapText note = new WrapText(FontManager.getRunescapeSmallFont(), ColorScheme.LIGHT_GRAY_COLOR);
			note.setText(l.note);
			note.setAlignmentX(LEFT_ALIGNMENT);
			text.add(note);
		}
		row.add(text, BorderLayout.CENTER);
		return row;
	}

	private static JLabel caption(String text)
	{
		return small(text, ColorScheme.LIGHT_GRAY_COLOR);
	}

	private static JLabel small(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(color);
		return label;
	}

	/** plain ASCII: the RuneScape font has no arrow glyphs */
	private void updateMarker()
	{
		marker.setText(body.isVisible() ? "[-]" : "[+]");
	}

	private static void hookClicks(Component component, MouseAdapter click)
	{
		component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		component.addMouseListener(click);
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				hookClicks(child, click);
			}
		}
	}
}

package com.corclan.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

/**
 * Read-only text that wraps at word breaks to whatever width its container gives it, so nothing is ever cut off.
 * (An HTML label with a fixed width does not fit reliably: Swing scales its CSS pixels with the screen.) Plain text,
 * so nothing in it is ever read as markup; new lines start new lines. It never scrolls the panel: a text area's caret
 * normally asks the scroll pane to show it whenever the text changes, which jumped the side panel to the top on every
 * refresh.
 */
final class WrapText extends JTextArea
{
	WrapText(Font font, Color color)
	{
		setLineWrap(true);
		setWrapStyleWord(true);
		setEditable(false);
		setFocusable(false);
		setOpaque(false);
		setBorder(null);
		setHighlighter(null);
		setFont(font);
		setForeground(color);
		setAlignmentX(LEFT_ALIGNMENT);
		((DefaultCaret) getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
	}

	/** Only touches the text when it changed, so a refresh with the same text does nothing at all. */
	@Override
	public void setText(String text)
	{
		if (!java.util.Objects.equals(text, getText()))
		{
			super.setText(text);
		}
	}

	/** Read-only text never needs to be scrolled to. */
	@Override
	public void scrollRectToVisible(Rectangle rect)
	{
	}

	/** As tall as the wrapped text, never stretched by a box layout. */
	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}
}

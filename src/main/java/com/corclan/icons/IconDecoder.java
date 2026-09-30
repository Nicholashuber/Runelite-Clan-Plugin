package com.corclan.icons;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Turns a chat icon downloaded from the clan server into an image, safely. The size is read from the
 * PNG header before any pixels are decoded, so an image that claims to be huge is refused without
 * allocating memory for it.
 */
public final class IconDecoder
{
	public static final int MAX_WIDTH = 32;
	public static final int MAX_HEIGHT = 16;
	/** base64 of the server's 16 KB PNG limit, with a little slack */
	static final int MAX_BASE64_LENGTH = 22_000;

	private IconDecoder()
	{
	}

	/** @return the decoded icon, or null if the data is not a small, valid PNG */
	public static BufferedImage decode(String base64)
	{
		if (base64 == null || base64.isEmpty() || base64.length() > MAX_BASE64_LENGTH)
		{
			return null;
		}
		byte[] bytes;
		try
		{
			bytes = Base64.getDecoder().decode(base64);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}

		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("png");
		if (!readers.hasNext())
		{
			return null;
		}
		ImageReader reader = readers.next();
		try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes)))
		{
			if (in == null)
			{
				return null;
			}
			reader.setInput(in, true, true);
			int width = reader.getWidth(0);
			int height = reader.getHeight(0);
			if (width < 1 || height < 1 || width > MAX_WIDTH || height > MAX_HEIGHT)
			{
				return null;
			}
			return reader.read(0);
		}
		catch (IOException | RuntimeException e)
		{
			return null;
		}
		finally
		{
			reader.dispose();
		}
	}
}

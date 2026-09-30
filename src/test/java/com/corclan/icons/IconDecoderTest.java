package com.corclan.icons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.Test;

public class IconDecoderTest
{
	private static byte[] png(int w, int h) throws Exception
	{
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		img.setRGB(0, 0, 0xFFD32F2F);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(img, "png", out);
		return out.toByteArray();
	}

	private static String b64(byte[] bytes)
	{
		return Base64.getEncoder().encodeToString(bytes);
	}

	@Test
	public void decodesChatSizedPngs() throws Exception
	{
		BufferedImage img = IconDecoder.decode(b64(png(13, 11)));
		assertNotNull(img);
		assertEquals(13, img.getWidth());
		assertEquals(11, img.getHeight());
		assertNotNull(IconDecoder.decode(b64(png(32, 16))));
	}

	@Test
	public void refusesOversizedImages() throws Exception
	{
		assertNull(IconDecoder.decode(b64(png(33, 11))));
		assertNull(IconDecoder.decode(b64(png(64, 64))));
	}

	@Test
	public void refusesAHeaderThatClaimsAHugeImage() throws Exception
	{
		byte[] bytes = png(1, 1);
		// IHDR width and height live at bytes 16-23
		bytes[16] = 0; bytes[17] = 0; bytes[18] = (byte) 0xC3; bytes[19] = 0x50;
		bytes[20] = 0; bytes[21] = 0; bytes[22] = (byte) 0xC3; bytes[23] = 0x50;
		assertNull(IconDecoder.decode(b64(bytes)));
	}

	@Test
	public void refusesJunk()
	{
		assertNull(IconDecoder.decode(null));
		assertNull(IconDecoder.decode(""));
		assertNull(IconDecoder.decode("not base64 !!!"));
		assertNull(IconDecoder.decode(b64("GIF89a not a png".getBytes())));
		StringBuilder big = new StringBuilder();
		for (int i = 0; i < IconDecoder.MAX_BASE64_LENGTH + 10; i++)
		{
			big.append('A');
		}
		assertNull(IconDecoder.decode(big.toString()));
	}
}

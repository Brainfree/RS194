package pre194;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class Sprite extends pre194.Graphics2D {

	private static final Logger logger = Logger.getLogger(Sprite.class.toString());

	public static final Sprite load(File f) throws IOException {
		BufferedImage image = ImageIO.read(f);
		Sprite s = new Sprite(image.getWidth(), image.getHeight());
		image.getRGB(0, 0, s.width, s.height, s.pixels, 0, s.width);
		for (int i = 0; i < s.pixels.length; i++) {
			s.pixels[i] &= ~(0xFF000000);
		}
		return s;
	}

	public int[] pixels;
	public int width;
	public int height;
	public int clipX;
	public int clipY;
	public int clipWidth;
	public int clipHeight;

	public Sprite(int w, int h) {
		pixels = new int[w * h];
		width = clipWidth = w;
		height = clipHeight = h;
		clipX = clipY = 0;
	}

	public Sprite(byte[] src, Component c) {
		try {
			Image i = Toolkit.getDefaultToolkit().createImage(src);
			MediaTracker mt = new MediaTracker(c);
			mt.addImage(i, 0);
			mt.waitForAll();
			width = i.getWidth(c);
			height = i.getHeight(c);
			clipWidth = width;
			clipHeight = height;
			clipX = 0;
			clipY = 0;
			pixels = new int[width * height];
			PixelGrabber pg = new PixelGrabber(i, 0, 0, width, height, pixels, 0, width);
			pg.grabPixels();
		} catch (Exception e) {
			System.out.println("Error converting jpg");
		}
	}

	public Sprite(Archive archive, String name, int index) {
		Buffer dat = new Buffer(archive.get(name + ".dat", null));
		Buffer idx = new Buffer(archive.get("index.dat", null));
		idx.position = dat.getUShort();

		clipWidth = idx.getUShort();
		clipHeight = idx.getUShort();

		int[] palette = new int[idx.getUByte()];

		for (int i = 0; i < palette.length - 1; i++) {
			palette[i + 1] = idx.getInt24();
			if (palette[i + 1] == 0) {
				palette[i + 1] = 1;
			}
		}

		for (int i = 0; i < index; i++) {
			idx.position += 2;
			dat.position += (idx.getUShort() * idx.getUShort());
			idx.position++;
		}

		clipX = idx.getUByte();
		clipY = idx.getUByte();
		width = idx.getUShort();
		height = idx.getUShort();

		int type = idx.getUByte();
		int len = width * height;
		pixels = new int[len];

		if (type == 0) {
			for (int i = 0; i < len; i++) {
				pixels[i] = palette[dat.getUByte()];
			}
		} else if (type == 1) {
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					pixels[x + y * width] = palette[dat.getUByte()];
				}
			}
		}
	}

	public void prepare() {
		pre194.Graphics2D.prepare(pixels, width, height);
	}

	public void replace(int rgbA, int rgbB) {
		for (int i = 0; i < pixels.length; i++) {
			if (pixels[i] == rgbA) {
				pixels[i] = rgbB;
			}
		}
	}

	public void drawOpaque(int x, int y) {
		x += clipX;
		y += clipY;

		int dstOff = x + y * pre194.Graphics2D.targetWidth;
		int srcOff = 0;
		int h = height;
		int w = width;
		int dstStep = pre194.Graphics2D.targetWidth - w;
		int srcStep = 0;

		if (y < pre194.Graphics2D.top) {
			int cutoff = pre194.Graphics2D.top - y;
			h -= cutoff;
			y = pre194.Graphics2D.top;
			srcOff += cutoff * w;
			dstOff += cutoff * pre194.Graphics2D.targetWidth;
		}

		if (y + h > pre194.Graphics2D.bottom) {
			h -= y + h - pre194.Graphics2D.bottom;
		}

		if (x < pre194.Graphics2D.left) {
			int cutoff = pre194.Graphics2D.left - x;
			w -= cutoff;
			x = pre194.Graphics2D.left;
			srcOff += cutoff;
			dstOff += cutoff;
			srcStep += cutoff;
			dstStep += cutoff;
		}
		if (x + w > pre194.Graphics2D.right) {
			int i_22_ = x + w - pre194.Graphics2D.right;
			w -= i_22_;
			srcStep += i_22_;
			dstStep += i_22_;
		}

		if (w > 0 && h > 0) {
			copyImage(w, h, pixels, srcOff, srcStep, pre194.Graphics2D.target, dstOff, dstStep);
		}
	}

	private void copyImage(int w, int h, int[] src, int srcOff, int srcStep, int[] dst, int dstOff, int dstStep) {
		int hw = -(w >> 2);
		w = -(w & 0x3);

		for (int y = -h; y < 0; y++) {
			for (int x = hw; x < 0; x++) {
				dst[dstOff++] = src[srcOff++];
				dst[dstOff++] = src[srcOff++];
				dst[dstOff++] = src[srcOff++];
				dst[dstOff++] = src[srcOff++];
			}

			for (int x = w; x < 0; x++) {
				dst[dstOff++] = src[srcOff++];
			}

			dstOff += dstStep;
			srcOff += srcStep;
		}
	}

	public void draw(int x, int y) {
		x += clipX;
		y += clipY;

		int dstOff = x + y * pre194.Graphics2D.targetWidth;
		int srcOff = 0;
		int w = width;
		int h = height;
		int dstStep = pre194.Graphics2D.targetWidth - w;
		int srcStep = 0;

		if (y < pre194.Graphics2D.top) {
			int cutoff = pre194.Graphics2D.top - y;
			h -= cutoff;
			y = pre194.Graphics2D.top;
			srcOff += cutoff * w;
			dstOff += cutoff * pre194.Graphics2D.targetWidth;
		}

		if (y + h > pre194.Graphics2D.bottom) {
			h -= y + h - pre194.Graphics2D.bottom;
		}

		if (x < pre194.Graphics2D.left) {
			int cutoff = pre194.Graphics2D.left - x;
			w -= cutoff;
			x = pre194.Graphics2D.left;
			srcOff += cutoff;
			dstOff += cutoff;
			srcStep += cutoff;
			dstStep += cutoff;
		}

		if (x + w > pre194.Graphics2D.right) {
			int cutoff = x + w - pre194.Graphics2D.right;
			w -= cutoff;
			srcStep += cutoff;
			dstStep += cutoff;
		}

		if (w > 0 && h > 0) {
			copyImage(h, w, pixels, srcOff, srcStep, pre194.Graphics2D.target, dstOff, dstStep, 0);
		}
	}

	public void copyImage(int h, int w, int[] src, int srcOff, int srcStep, int[] dst, int dstOff, int dstStep, int rgb) {
		int hw = -(w >> 2);
		w = -(w & 0x3);
		for (int x = -h; x < 0; x++) {
			for (int y = hw; y < 0; y++) {
				rgb = src[srcOff++];

				if (rgb != 0) {
					dst[dstOff++] = rgb;
				} else {
					dstOff++;
				}

				rgb = src[srcOff++];

				if (rgb != 0) {
					dst[dstOff++] = rgb;
				} else {
					dstOff++;
				}

				rgb = src[srcOff++];

				if (rgb != 0) {
					dst[dstOff++] = rgb;
				} else {
					dstOff++;
				}

				rgb = src[srcOff++];

				if (rgb != 0) {
					dst[dstOff++] = rgb;
				} else {
					dstOff++;
				}
			}

			for (int y = w; y < 0; y++) {
				rgb = src[srcOff++];

				if (rgb != 0) {
					dst[dstOff++] = rgb;
				} else {
					dstOff++;
				}
			}

			dstOff += dstStep;
			srcOff += srcStep;
		}
	}

	public void draw(int x, int y, int alpha) {
		x += clipX;
		y += clipY;

		int dstOff = x + y * pre194.Graphics2D.targetWidth;
		int srcOff = 0;
		int w = width;
		int h = height;
		int dstStep = pre194.Graphics2D.targetWidth - w;
		int srcStep = 0;

		if (y < pre194.Graphics2D.top) {
			int cutoff = pre194.Graphics2D.top - y;
			h -= cutoff;
			y = pre194.Graphics2D.top;
			srcOff += cutoff * w;
			dstOff += cutoff * pre194.Graphics2D.targetWidth;
		}

		if (y + h > pre194.Graphics2D.bottom) {
			h -= y + h - pre194.Graphics2D.bottom;
		}

		if (x < pre194.Graphics2D.left) {
			int cutoff = pre194.Graphics2D.left - x;
			w -= cutoff;
			x = pre194.Graphics2D.left;
			srcOff += cutoff;
			dstOff += cutoff;
			srcStep += cutoff;
			dstStep += cutoff;
		}

		if (x + w > pre194.Graphics2D.right) {
			int cutoff = x + w - pre194.Graphics2D.right;
			w -= cutoff;
			srcStep += cutoff;
			dstStep += cutoff;
		}

		if (w > 0 && h > 0) {
			copyImage(w, h, pixels, srcOff, srcStep, pre194.Graphics2D.target, dstOff, dstStep, alpha, 0);
		}
	}

	private void copyImage(int w, int h, int[] src, int srcOff, int srcStep, int[] dst, int dstOff, int dstStep, int alpha, int rgb) {
		int opacity = 256 - alpha;
		for (int y = -h; y < 0; y++) {
			for (int x = -w; x < 0; x++) {
				rgb = src[srcOff++];
				if (rgb != 0) {
					int dstRGB = dst[dstOff];
					dst[dstOff++] = ((((rgb & 0xff00ff) * alpha + (dstRGB & 0xff00ff) * opacity) & ~0xff00ff) + (((rgb & 0xff00) * alpha + (dstRGB & 0xff00) * opacity) & 0xff0000)) >> 8;
				} else {
					dstOff++;
				}
			}
			dstOff += dstStep;
			srcOff += srcStep;
		}
	}

	public void draw(int x, int y, int w, int h, int pivotX, int pivotY, int theta, int[] lineStart, int[] lineWidth) {
		try {
			int cx = w / 2;
			int cy = h / 2;

			int sin = (int) (Math.sin(theta / 326.11) * 65536.0);
			int cos = (int) (Math.cos(theta / 326.11) * 65536.0);

			int originX = (pivotX << 16) - ((cy * sin) + (cx * cos));
			int originY = (pivotY << 16) - ((cy * cos) - (cx * sin));

			int origin = x + (y * pre194.Graphics2D.targetWidth);

			for (y = 0; y < h; y++) {
				int start = lineStart[y];
				int dstOff = origin + start;

				int srcX = originX + (cos * start);
				int srcY = originY - (sin * start);

				for (x = 0; x < lineWidth[y]; x++) {
					pre194.Graphics2D.target[dstOff++] = pixels[(srcX >> 16) + (srcY >> 16) * width];
					srcX += cos;
					srcY -= sin;
				}

				originX += sin;
				originY += cos;
				origin += pre194.Graphics2D.targetWidth;
			}
		} catch (Exception ignored) {
		}
	}

	public void draw(int x, int y, int w, int h, int anchorx, int anchory, int theta) {
		try {
			int centerX = -w / 2;
			int centerY = -h / 2;

			int sin = (int) (Math.sin(theta / 326.11) * 65536.0);
			int cos = (int) (Math.cos(theta / 326.11) * 65536.0);

			int originX = (anchorx << 16) + ((centerY * sin) + (centerX * cos));
			int originY = (anchory << 16) + ((centerY * cos) - (centerX * sin));
			int origin = x + (y * pre194.Graphics2D.targetWidth);

			for (y = 0; y < h; y++) {
				int dstOff = origin;
				int srcX = originX + cos;
				int srcY = originY - sin;

				for (x = 0; x < w; x++) {
					int rgb = pixels[(srcX >> 16) + (srcY >> 16) * width];

					if (rgb != 0) {
						pre194.Graphics2D.target[dstOff++] = rgb;
					} else {
						dstOff++;
					}
					srcX += cos;
					srcY -= sin;
				}
				originX += sin;
				originY += cos;
				origin += pre194.Graphics2D.targetWidth;
			}
		} catch (Exception ignored) {
		}
	}

	public void draw(IndexedSprite mask, int x, int y) {
		x += clipX;
		y += clipY;

		int dstOff = x + y * pre194.Graphics2D.targetWidth;
		int srcOff = 0;

		int h = height;
		int w = width;

		int dstStep = pre194.Graphics2D.targetWidth - w;
		int srcStep = 0;

		if (y < pre194.Graphics2D.top) {
			int i = pre194.Graphics2D.top - y;
			h -= i;
			y = pre194.Graphics2D.top;
			srcOff += i * w;
			dstOff += i * pre194.Graphics2D.targetWidth;
		}

		if (y + h > pre194.Graphics2D.bottom) {
			h -= y + h - pre194.Graphics2D.bottom;
		}

		if (x < pre194.Graphics2D.left) {
			int i = pre194.Graphics2D.left - x;
			w -= i;
			x = pre194.Graphics2D.left;
			srcOff += i;
			dstOff += i;
			srcStep += i;
			dstStep += i;
		}
		if (x + w > pre194.Graphics2D.right) {
			int i = x + w - pre194.Graphics2D.right;
			w -= i;
			srcStep += i;
			dstStep += i;
		}

		if (w > 0 && h > 0) {
			copyImage(pre194.Graphics2D.target, srcOff, 0, h, srcStep, dstOff, dstStep, pixels, mask.data, w);
		}
	}

	private void copyImage(int[] is, int i, int i_111_, int i_112_, int i_113_, int i_114_, int i_115_, int[] is_116_, byte[] is_117_, int i_118_) {
		int i_119_ = -(i_118_ >> 2);
		i_118_ = -(i_118_ & 0x3);
		for (int i_120_ = -i_112_; i_120_ < 0; i_120_++) {
			for (int i_121_ = i_119_; i_121_ < 0; i_121_++) {
				i_111_ = is_116_[i++];
				if (i_111_ != 0 && is_117_[i_114_] == 0) {
					is[i_114_++] = i_111_;
				} else {
					i_114_++;
				}
				i_111_ = is_116_[i++];
				if (i_111_ != 0 && is_117_[i_114_] == 0) {
					is[i_114_++] = i_111_;
				} else {
					i_114_++;
				}
				i_111_ = is_116_[i++];
				if (i_111_ != 0 && is_117_[i_114_] == 0) {
					is[i_114_++] = i_111_;
				} else {
					i_114_++;
				}
				i_111_ = is_116_[i++];
				if (i_111_ != 0 && is_117_[i_114_] == 0) {
					is[i_114_++] = i_111_;
				} else {
					i_114_++;
				}
			}
			for (int i_122_ = i_118_; i_122_ < 0; i_122_++) {
				i_111_ = is_116_[i++];
				if (i_111_ != 0 && is_117_[i_114_] == 0) {
					is[i_114_++] = i_111_;
				} else {
					i_114_++;
				}
			}
			i_114_ += i_115_;
			i += i_113_;
		}
	}


}

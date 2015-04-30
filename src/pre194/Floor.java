package pre194;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains information on how a tile should appear on the scene, and minimap.
 *
 * @author Dane
 */
public class Floor {

	private static final Logger logger = Logger.getLogger(Floor.class.getName());

	/**
	 * The floor instance count.
	 */
	public static int count;

	/**
	 * The floor instances.
	 */
	public static Floor[] instances;

	/**
	 * The color in INT24_RGB format.
	 */
	public int rgb;

	/**
	 * The texture pointer this floor uses.
	 */
	public int textureIndex = -1;

	/**
	 * Whether this floor hides the underlay or not.
	 */
	public boolean occlude = true;

	/**
	 * The name of the floor.
	 */
	public String name;

	/**
	 * The hue of the floor.
	 */
	public int hue;

	/**
	 * The saturation of the floor.
	 */
	public int saturation;

	/**
	 * The lightness of the floor.
	 */
	public int lightness;

	/**
	 * The hue used for blending on the landscape.
	 */
	public int blendHue;

	/**
	 * The 16-bit value containg the hue, saturation and lightness.
	 */
	public int hsl16;

	/**
	 * The hue multiplier used for blending on the landscape.
	 */
	public int blendHueMultiplier;

	/**
	 * The index of the floor.
	 */
	public int index;

	/**
	 * Unpacks the floors and stores them in a static array.
	 *
	 * @param archive the archive containing the floor data file.
	 */
	public static void unpack(Archive archive) {
		Buffer b;
		byte[] data = Signlink.loadFile("flo.dat");

		if (data != null) {
			b = new Buffer(data);
		} else {
			b = new Buffer(archive.get("flo.dat", null));
		}

		count = b.getUShort();

		if (instances == null) {
			instances = new Floor[count];
		}

		for (int n = 0; n < count; n++) {
			if (instances[n] == null) {
				instances[n] = new Floor();
			}
			instances[n].index = n;
			instances[n].read(b);
		}
	}

	/**
	 * Sets the color of the floor.
	 *
	 * @param rgb the color. (INT24_RGB format)
	 */
	private void setColor(int rgb) {
		double r = (double) (rgb >> 16 & 0xff) / 256.0;
		double g = (double) (rgb >> 8 & 0xff) / 256.0;
		double b = (double) (rgb & 0xff) / 256.0;

		double min = Math.min(Math.min(r, g), b);
		double max = Math.max(Math.max(r, g), b);

		double h = 0.0;
		double s = 0.0;
		double l = (min + max) / 2.0;

		if (min != max) {
			if (l < 0.5) {
				s = (max - min) / (max + min);
			}
			if (l >= 0.5) {
				s = (max - min) / (2.0 - max - min);
			}

			if (r == max) {
				h = (g - b) / (max - min);
			} else if (g == max) {
				h = 2.0 + (b - r) / (max - min);
			} else if (b == max) {
				h = 4.0 + (r - g) / (max - min);
			}
		}

		h /= 6.0;

		this.hue = (int) (h * 256.0);
		this.saturation = (int) (s * 256.0);
		this.lightness = (int) (l * 256.0);

		if (this.saturation < 0) {
			this.saturation = 0;
		} else if (this.saturation > 255) {
			this.saturation = 255;
		}

		if (this.lightness < 0) {
			this.lightness = 0;
		} else if (this.lightness > 255) {
			this.lightness = 255;
		}

		if (l > 0.5) {
			this.blendHueMultiplier = (int) ((1.0 - l) * s * 512.0);
		} else {
			this.blendHueMultiplier = (int) (l * s * 512.0);
		}

		if (this.blendHueMultiplier < 1) {
			this.blendHueMultiplier = 1;
		}

		this.blendHue = (int) (h * (double) this.blendHueMultiplier);

		// XXX: from 317
		setHSL16();
	}

	private void setHSL16() {
		int h0 = (hue + (int) (Math.random() * 16D)) - 8;

		if (h0 < 0) {
			h0 = 0;
		} else if (h0 > 255) {
			h0 = 255;
		}

		int s0 = (saturation + (int) (Math.random() * 48D)) - 24;

		if (s0 < 0) {
			s0 = 0;
		} else if (s0 > 255) {
			s0 = 255;
		}

		int l0 = (lightness + (int) (Math.random() * 48D)) - 24;

		if (l0 < 0) {
			l0 = 0;
		} else if (l0 > 255) {
			l0 = 255;
		}

		hsl16 = hsl24to16(h0, s0, l0);
	}

	public int hsl24to16(int h, int s, int l) {
		if (l > 179) {
			s /= 2;
		}
		if (l > 192) {
			s /= 2;
		}
		if (l > 217) {
			s /= 2;
		}
		if (l > 243) {
			s /= 2;
		}
		return (h / 4 << 10) + (s / 32 << 7) + l / 2;
	}

	/**
	 * Reads the floor data from the provided buffer.
	 *
	 * @param buffer the buffer.
	 */
	private void read(Buffer buffer) {
		for (; ; ) {
			int opcode = buffer.getUByte();

			if (opcode == 0) {
				break;
			}

			if (opcode == 1) {
				this.setColor(rgb = buffer.getInt24());
			} else if (opcode == 2) {
				this.textureIndex = buffer.getUByte();
			} else if (opcode == 3) {
				// dummy
			} else if (opcode == 5) {
				this.occlude = false;
			} else if (opcode == 6) {
				this.name = buffer.getString();
			} else {
				logger.log(Level.WARNING, "Error unrecognized config code: {0}", opcode);
			}
		}
	}

}

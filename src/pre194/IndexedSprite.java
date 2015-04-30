package pre194;

public final class IndexedSprite extends Graphics2D {

	public byte[] data;
	public int[] palette;
	public int width;
	public int height;
	public int clipX;
	public int clipY;
	public int clipWidth;
	public int clipHeight;

	public IndexedSprite(Archive archive, String name, int index) {
		Buffer dat = new Buffer(archive.get(name + ".dat", null));
		Buffer idx = new Buffer(archive.get("index.dat", null));

		idx.position = dat.getUShort();

		clipWidth = idx.getUShort();
		clipHeight = idx.getUShort();

		palette = new int[idx.getUByte()];

		for (int n = 0; n < palette.length - 1; n++) {
			palette[n + 1] = idx.getInt24();
		}

		for (int n = 0; n < index; n++) {
			idx.position += 2;
			dat.position += (idx.getUShort() * idx.getUShort());
			idx.position++;
		}

		clipX = idx.getUByte();
		clipY = idx.getUByte();
		width = idx.getUShort();
		height = idx.getUShort();

		int type = idx.getUByte();
		data = new byte[width * height];

		if (type == 0) {
			for (int n = 0; n < data.length; n++) {
				data[n] = dat.getByte();
			}
		} else if (type == 1) {
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					data[x + y * width] = dat.getByte();
				}
			}
		}
	}

	public void shrink() {
		clipWidth /= 2;
		clipHeight /= 2;

		byte[] newPixels = new byte[clipWidth * clipHeight];
		int off = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				newPixels[((x + clipX >> 1) + (y + clipY >> 1) * clipWidth)] = data[off++];
			}
		}
		data = newPixels;
		width = clipWidth;
		height = clipHeight;
		clipX = 0;
		clipY = 0;
	}

	public void crop() {
		if (width != clipWidth || height != clipHeight) {
			byte[] newPixels = new byte[clipWidth * clipHeight];
			int off = 0;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					newPixels[x + clipX + (y + clipY) * clipWidth] = data[off++];
				}
			}
			data = newPixels;
			width = clipWidth;
			height = clipHeight;
			clipX = 0;
			clipY = 0;
		}
	}

	public void flipHorizontally() {
		byte[] flipped = new byte[width * height];
		int off = 0;
		for (int y = 0; y < height; y++) {
			for (int x = width - 1; x >= 0; x--) {
				flipped[off++] = data[x + y * width];
			}
		}
		data = flipped;
		clipX = clipWidth - width - clipX;
	}

	public void flipVertically() {
		byte[] flipped = new byte[width * height];
		int off = 0;
		for (int y = height - 1; y >= 0; y--) {
			for (int x = 0; x < width; x++) {
				flipped[off++] = data[x + y * width];
			}
		}
		data = flipped;
		clipY = clipHeight - height - clipY;
	}

	public void draw(int x, int y) {
		x += clipX;
		y += clipY;
		int dstOff = x + y * targetWidth;
		int srcOff = 0;
		int h = height;
		int w = width;
		int dstStep = targetWidth - w;
		int srcStep = 0;
		if (y < top) {
			int trim = top - y;
			h -= trim;
			y = top;
			srcOff += trim * w;
			dstOff += trim * targetWidth;
		}
		if (y + h > bottom) {
			h -= y + h - bottom;
		}
		if (x < left) {
			int trim = left - x;
			w -= trim;
			x = left;
			srcOff += trim;
			dstOff += trim;
			srcStep += trim;
			dstStep += trim;
		}
		if (x + w > right) {
			int trim = x + w - right;
			w -= trim;
			srcStep += trim;
			dstStep += trim;
		}
		if (w > 0 && h > 0) {
			copyImage(h, data, palette, w, dstOff, srcOff, target, dstStep, srcStep);
		}
	}

	private void copyImage(int h, byte[] src, int[] palette, int w, int dstOff, int srcOff, int[] dst, int dstStep, int srcstep) {
		int hw = -(w >> 2);
		w = -(w & 0x3);
		for (int y = -h; y < 0; y++) {
			for (int x = hw; x < 0; x++) {
				int p = src[srcOff++];
				if (p != 0) {
					dst[dstOff++] = palette[p & 0xff];
				} else {
					dstOff++;
				}
				p = src[srcOff++];
				if (p != 0) {
					dst[dstOff++] = palette[p & 0xff];
				} else {
					dstOff++;
				}
				p = src[srcOff++];
				if (p != 0) {
					dst[dstOff++] = palette[p & 0xff];
				} else {
					dstOff++;
				}
				p = src[srcOff++];
				if (p != 0) {
					dst[dstOff++] = palette[p & 0xff];
				} else {
					dstOff++;
				}
			}
			for (int x = w; x < 0; x++) {
				int p = src[srcOff++];
				if (p != 0) {
					dst[dstOff++] = palette[p & 0xff];
				} else {
					dstOff++;
				}
			}
			dstOff += dstStep;
			srcOff += srcstep;
		}
	}
}

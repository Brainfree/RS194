package pre194;

public class NPCInfo {

	public static int count;
	private static int[] pointers;
	private static Buffer data;
	private static NPCInfo[] cache;
	private static int cachePosition;
	public static LinkedList models = new LinkedList(30);

	public int index;
	public String name;
	public byte[] description;
	public byte size = 1;
	public int[] modelIndices;
	private int[] headModelIndices;
	public int animStandIndex = -1;
	public int animWalkIndex = -1;
	public int animRunIndex = -1;
	public int animTurnRightIndex = -1;
	public int animTurnLeftIndex = -1;
	private boolean disposeAlpha = false;
	public int[] oldColors;
	public int[] newColors;
	public String[] actions;
	public boolean showOnMinimap = true;
	public int level = -1;
	private int scaleX = 128;
	private int scaleY = 128;

	public static final int getCount() {
		return count;
	}

	public static final void load(Archive a) {
		data = new Buffer(a.get("npc.dat", null));
		Buffer idx = new Buffer(a.get("npc.idx", null));
		count = idx.getUShort();
		pointers = new int[count];

		int off = 2;
		for (int n = 0; n < count; n++) {
			pointers[n] = off;
			off += idx.getUShort();
		}

		cache = new NPCInfo[20];
		for (int n = 0; n < 20; n++) {
			cache[n] = new NPCInfo();
		}
	}

	public static final void unload() {
		models = null;
		pointers = null;
		cache = null;
		data = null;
	}

	public static final NPCInfo get(int index) {
		for (int n = 0; n < 20; n++) {
			if (cache[n].index == index) {
				return cache[n];
			}
		}
		cachePosition = (cachePosition + 1) % 20;
		NPCInfo i = cache[cachePosition] = new NPCInfo();
		data.position = pointers[index];
		i.index = index;
		i.read(data);
		return i;
	}

	public NPCInfo() {
		this.index = -1;
	}

	private void read(Buffer b) {
		for (;;) {
			int opcode = b.getUByte();
			
			if (opcode == 0) {
				break;
			}
			
			if (opcode == 1) {
				int count = b.getUByte();
				modelIndices = new int[count];
				for (int n = 0; n < count; n++) {
					modelIndices[n] = b.getUShort();
				}
			} else if (opcode == 2) {
				name = b.getString();
			} else if (opcode == 3) {
				description = b.getStringBytes();
			} else if (opcode == 12) {
				size = b.getByte();
			} else if (opcode == 13) {
				animStandIndex = b.getUShort();
			} else if (opcode == 14) {
				animWalkIndex = b.getUShort();
			} else if (opcode == 16) {
				disposeAlpha = true;
			} else if (opcode == 17) {
				animWalkIndex = b.getUShort();
				animRunIndex = b.getUShort();
				animTurnRightIndex = b.getUShort();
				animTurnLeftIndex = b.getUShort();
			} else if (opcode >= 30 && opcode < 40) {
				if (actions == null) {
					actions = new String[5];
				}
				actions[opcode - 30] = b.getString();
			} else if (opcode == 40) {
				int count = b.getUByte();
				oldColors = new int[count];
				newColors = new int[count];
				for (int n = 0; n < count; n++) {
					oldColors[n] = b.getUShort();
					newColors[n] = b.getUShort();
				}
			} else if (opcode == 60) {
				int n = b.getUByte();
				headModelIndices = new int[n];
				for (int m = 0; m < n; m++) {
					headModelIndices[m] = b.getUShort();
				}
			} else if (opcode == 90) {
				b.getUShort();
			} else if (opcode == 91) {
				b.getUShort();
			} else if (opcode == 92) {
				b.getUShort();
			} else if (opcode == 93) {
				showOnMinimap = false;
			} else if (opcode == 95) {
				level = b.getUShort();
			} else if (opcode == 97) {
				scaleX = b.getUShort();
			} else if (opcode == 98) {
				scaleY = b.getUShort();
			}
		}
	}

	public final Model getModel(int primaryFrame, int secondaryFrame, int[] labelGroups) {
		Model m = (Model) models.get(index);

		if (m == null) {
			Model[] models = new Model[modelIndices.length];

			for (int n = 0; n < modelIndices.length; n++) {
				models[n] = new Model(modelIndices[n]);
			}

			if (models.length == 1) {
				m = models[0];
			} else {
				m = new Model(models, models.length);
			}

			if (oldColors != null) {
				for (int n = 0; n < oldColors.length; n++) {
					m.recolor(oldColors[n], newColors[n]);
				}
			}

			m.applyGroups();
			m.applyLighting(64, 850, -30, -50, -30, true);
			NPCInfo.models.put(m, index);
		}

		m = new Model(m, !disposeAlpha);

		if (primaryFrame != -1 && secondaryFrame != -1) {
			m.applyFrames(primaryFrame, secondaryFrame, labelGroups);
		} else if (primaryFrame != -1) {
			m.applyFrame(primaryFrame);
		}

		if (scaleX != 128 || scaleY != 128) {
			m.scale(scaleX, scaleY, scaleX);
		}

		m.calculateYBoundaries();
		m.skinTriangle = null;
		m.labelVertices = null;
		return m;
	}

	public final Model getHeadModel() {
		if (headModelIndices == null) {
			return null;
		}

		Model[] models = new Model[headModelIndices.length];

		for (int n = 0; n < headModelIndices.length; n++) {
			models[n] = new Model(headModelIndices[n]);
		}

		Model m;

		if (models.length == 1) {
			m = models[0];
		} else {
			m = new Model(models, models.length);
		}

		if (oldColors != null) {
			for (int n = 0; n < oldColors.length; n++) {
				m.recolor(oldColors[n], newColors[n]);
			}
		}
		return m;
	}

}

package pre194;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class Widget {

	public static final int TYPE_PARENT = 0;
	public static final int TYPE_UNUSED = 1;
	public static final int TYPE_INVENTORY = 2;
	public static final int TYPE_RECT = 3;
	public static final int TYPE_TEXT = 4;
	public static final int TYPE_IMAGE = 5;
	public static final int TYPE_MODEL = 6;
	public static final int TYPE_INVENTORY_TEXT = 7;

	public static final int OPTIONTYPE_OK = 1;
	public static final int OPTIONTYPE_SELECT = 4;
	public static final int OPTIONTYPE_SELECT2 = 5;
	public static final int OPTIONTYPE_CONTINUE = 6;

	public static Widget[] instances;

	private static LinkedList spriteCache;
	private static LinkedList modelCache;

	private static Sprite getSprite(String name, Archive media, int index) {
		long uid = (StringUtil.getHash(name) << 4) + (long) index;
		Sprite s = (Sprite) spriteCache.get(uid);

		if (s != null) {
			return s;
		}

		s = new Sprite(media, name, index);
		spriteCache.put(s, uid);
		return s;
	}

	private static Model getModel(int index) {
		Model m = (Model) modelCache.get((long) index);

		if (m != null) {
			return m;
		}

		m = new Model(index);
		modelCache.put(m, (long) index);
		return m;
	}

	public static void load(BitmapFont[] fonts, Archive media, Archive interfaces) {
		spriteCache = new LinkedList(50000);
		modelCache = new LinkedList(50000);

		Buffer b = new Buffer(interfaces.get("data", null));
		instances = new Widget[b.getUShort()];

		int parent = -1;
		while (b.position < b.data.length) {
			int index = b.getUShort();

			if (index == 65535) {
				parent = b.getUShort();
				index = b.getUShort();
			}

			Widget w = instances[index] = new Widget();
			w.index = index;
			w.parent = parent;
			w.type = b.getUByte();
			w.optionType = b.getUByte();
			w.action = b.getUShort();
			w.width = b.getUShort();
			w.height = b.getUShort();
			w.hoverParentIndex = b.getUByte();

			if (w.hoverParentIndex != 0) {
				w.hoverParentIndex = ((w.hoverParentIndex - 1 << 8) + b.getUByte());
			} else {
				w.hoverParentIndex = -1;
			}

			int comparatorCount = b.getUByte();

			if (comparatorCount > 0) {
				w.scriptCompareType = new int[comparatorCount];
				w.scriptCompareValue = new int[comparatorCount];

				for (int n = 0; n < comparatorCount; n++) {
					w.scriptCompareType[n] = b.getUByte();
					w.scriptCompareValue[n] = b.getUShort();
				}
			}

			int scriptCount = b.getUByte();

			if (scriptCount > 0) {
				w.script = new int[scriptCount][];
				for (int script = 0; script < scriptCount; script++) {
					int opcodes = b.getUShort();
					w.script[script] = new int[opcodes];
					for (int opcode = 0; opcode < opcodes; opcode++) {
						w.script[script][opcode] = b.getUShort();
					}
				}
			}

			if (w.type == TYPE_PARENT) {
				w.scrollHeight = b.getUShort();
				w.hidden = b.getUByte() == 1;

				int n = b.getUByte();
				w.children = new int[n];
				w.childX = new int[n];
				w.childY = new int[n];

				for (int m = 0; m < n; m++) {
					w.children[m] = b.getUShort();
					w.childX[m] = b.getShort();
					w.childY[m] = b.getShort();
				}
			}

			if (w.type == TYPE_UNUSED) {
				w.unusedInt = b.getUShort();
				w.unusedBool = b.getUByte() == 1;
			}

			if (w.type == TYPE_INVENTORY) {
				w.inventoryIndices = new int[w.width * w.height];
				w.inventoryAmount = new int[w.width * w.height];

				w.inventoryDummy = b.getUByte() == 1;
				w.inventoryHasActions = b.getUByte() == 1;
				w.inventoryIsUsable = b.getUByte() == 1;
				w.inventoryMarginX = b.getUByte();
				w.inventoryMarginY = b.getUByte();
				w.inventoryOffsetX = new int[20];
				w.inventoryOffsetY = new int[20];
				w.inventorySprite = new Sprite[20];

				for (int n = 0; n < 20; n++) {
					if (b.getUByte() == 1) {
						w.inventoryOffsetX[n] = b.getShort();
						w.inventoryOffsetY[n] = b.getShort();

						String s = b.getString();

						if (media != null && s.length() > 0) {
							int j = s.lastIndexOf(",");
							w.inventorySprite[n] = getSprite(s.substring(0, j), media, (Integer.parseInt(s.substring(j + 1))));
						}
					}
				}

				w.inventoryActions = new String[5];

				for (int n = 0; n < 5; n++) {
					w.inventoryActions[n] = b.getString();

					if (w.inventoryActions[n].length() == 0) {
						w.inventoryActions[n] = null;
					}
				}
			}

			if (w.type == TYPE_RECT) {
				w.fill = b.getUByte() == 1;
			}

			if (w.type == TYPE_TEXT || w.type == TYPE_UNUSED) {
				w.centered = b.getUByte() == 1;
				w.font = fonts[b.getUByte()];
				w.shadow = b.getUByte() == 1;
			}

			if (w.type == TYPE_TEXT) {
				w.messageDisabled = b.getString();
				w.messageEnabled = b.getString();
			}

			if (w.type == TYPE_UNUSED || w.type == TYPE_RECT || w.type == TYPE_TEXT) {
				w.colorDisabled = b.getInt();
			}

			if (w.type == TYPE_RECT || w.type == TYPE_TEXT) {
				w.colorEnabled = b.getInt();
				w.hoverColor = b.getInt();
			}

			if (w.type == TYPE_IMAGE) {
				String s = b.getString();

				if (media != null && s.length() > 0) {
					int j = s.lastIndexOf(",");
					w.spriteDisabled = getSprite(s.substring(0, j), media, Integer.parseInt(s.substring(j + 1)));
				}

				s = b.getString();

				if (media != null && s.length() > 0) {
					int j = s.lastIndexOf(",");
					w.spriteEnabled = getSprite(s.substring(0, j), media, Integer.parseInt(s.substring(j + 1)));
				}
			}

			if (w.type == TYPE_MODEL) {
				index = b.getUByte();

				if (index != 0) {
					w.modelDisabled = getModel(((index - 1 << 8) + b.getUByte()));
				}

				index = b.getUByte();

				if (index != 0) {
					w.modelEnabled = getModel(((index - 1 << 8) + b.getUByte()));
				}

				index = b.getUByte();

				if (index != 0) {
					w.animIndexDisabled = (index - 1 << 8) + b.getUByte();
				} else {
					w.animIndexDisabled = -1;
				}

				index = b.getUByte();

				if (index != 0) {
					w.seqIndexEnabled = (index - 1 << 8) + b.getUByte();
				} else {
					w.seqIndexEnabled = -1;
				}

				w.modelZoom = b.getUShort();
				w.modelCameraPitch = b.getUShort();
				w.modelYaw = b.getUShort();
			}

			if (w.type == TYPE_INVENTORY_TEXT) {
				w.inventoryIndices = new int[w.width * w.height];
				w.inventoryAmount = new int[w.width * w.height];
				w.centered = b.getUByte() == 1;

				int font = b.getUByte();

				if (fonts != null) {
					w.font = fonts[font];
				}

				w.shadow = b.getUByte() == 1;
				w.colorDisabled = b.getInt();
				w.inventoryMarginX = b.getShort();
				w.inventoryMarginY = b.getShort();
				w.inventoryHasActions = b.getUByte() == 1;
				w.inventoryActions = new String[5];

				for (int n = 0; n < 5; n++) {
					w.inventoryActions[n] = b.getString();

					if (w.inventoryActions[n].length() == 0) {
						w.inventoryActions[n] = null;
					}
				}
			}

			if (w.optionType == 2 || w.type == TYPE_INVENTORY) {
				w.optionCircumfix = b.getString();
				w.optionSuffix = b.getString();
				w.optionFlags = b.getUShort();
			}

			if (w.optionType == 1 || w.optionType == 4 || w.optionType == 5 || w.optionType == 6) {
				w.option = b.getString();

				if (w.option.length() == 0) {
					if (w.optionType == OPTIONTYPE_OK) {
						w.option = "Ok";
					}

					if (w.optionType == OPTIONTYPE_SELECT || w.optionType == OPTIONTYPE_SELECT2) {
						w.option = "Select";
					}

					if (w.optionType == OPTIONTYPE_CONTINUE) {
						w.option = "Continue";
					}
				}
			}
		}
		spriteCache = null;
	}

	public int[] inventoryIndices;
	public int[] inventoryAmount;
	public int animFrame;
	public int animCycle;
	public int index;
	public int parent;
	public int type;
	public int optionType;
	public int action;
	public int width;
	public int height;
	public int[][] script;
	public int[] scriptCompareType;
	public int[] scriptCompareValue;
	public int hoverParentIndex = -1;
	public int scrollHeight;
	public int scrollAmount;
	public boolean hidden;
	public int[] children;
	public int[] childX;
	public int[] childY;
	public int unusedInt;
	public boolean unusedBool;
	public boolean inventoryDummy;
	public boolean inventoryHasActions;
	public boolean inventoryIsUsable;
	public int inventoryMarginX;
	public int inventoryMarginY;
	public Sprite[] inventorySprite;
	public int[] inventoryOffsetX;
	public int[] inventoryOffsetY;
	public String[] inventoryActions;
	public boolean fill;
	public boolean centered;
	public boolean shadow;
	public BitmapFont font;
	public String messageDisabled;
	public String messageEnabled;
	public int colorDisabled;
	public int colorEnabled;
	public int hoverColor;
	public Sprite spriteDisabled;
	public Sprite spriteEnabled;
	public Model modelDisabled;
	public Model modelEnabled;
	public int animIndexDisabled;
	public int seqIndexEnabled;
	public int modelZoom;
	public int modelCameraPitch;
	public int modelYaw;
	public String optionCircumfix;
	public String optionSuffix;
	public int optionFlags;
	public String option;

	public Model getModel(int primaryFrame, int secondaryFrame, boolean enabled) {
		Model m = modelDisabled;

		if (enabled) {
			m = modelEnabled;
		}

		if (m == null) {
			return null;
		}

		if (primaryFrame == -1 && secondaryFrame == -1 && m.unmodifiedTriangleColor == null) {
			return m;
		}

		m = new Model(m, false, true, true, true);

		if (primaryFrame != -1 || secondaryFrame != -1) {
			m.applyGroups();
		}

		if (primaryFrame != -1) {
			m.applyFrame(primaryFrame);
		}

		if (secondaryFrame != -1) {
			m.applyFrame(secondaryFrame);
		}

		m.applyLighting(64, 768, -50, -10, -50, true);
		return m;
	}
}

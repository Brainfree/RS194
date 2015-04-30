package pre194;

public class Identikit {

	/**
	 * Appearance colors for each part of your Identity. Their format is in HSL and are used with
	 * <code>Canvas3D.palette</code>.
	 */
	public static final int[][] APPEARANCE_COLORS = new int[][]{
			{6798, 107, 10283, 16, 4797, 7744, 5799},
			{8741, 12, 64030, 43162, 7735, 8404, 1701, 38430, 24094, 10153, 56621, 4783, 1341, 16578, 35003, 25239},
			{25238, 8742, 12, 64030, 43162, 7735, 8404, 1701, 38430, 24094, 10153, 56621, 4783, 1341, 16578, 35003},
			{4626, 11146, 6439, 12, 4758, 10270},
			{4550, 4537, 5681, 5673, 5790, 6806, 8076, 4574}
	};
	public static final int[] BEARD_COLORS = {9104, 10275, 7595, 3610, 7975, 8526, 918, 38802, 24466, 10145, 58654, 5027, 1457, 16565, 34991, 25486};

	public static int count;
	public static Identikit[] instance;
	public int type = -1;
	public int[] modelIndices;
	public int[] oldColors = new int[6];
	public int[] newColors = new int[6];
	public int[] headModelIndices = {-1, -1, -1, -1, -1};

	public static void load(Archive a) {
		Buffer b = new Buffer(a.get("idk.dat", null));
		count = b.getUShort();

		if (instance == null) {
			instance = new Identikit[count];
		}

		for (int n = 0; n < count; n++) {
			if (instance[n] == null) {
				instance[n] = new Identikit();
			}
			instance[n].read(b);
		}
	}

	public void read(Buffer b) {
		for (;;) {
			int opcode = b.getUByte();

			if (opcode == 0) {
				break;
			}

			if (opcode == 1) {
				type = b.getUByte();
			} else if (opcode == 2) {
				int n = b.getUByte();
				modelIndices = new int[n];
				for (int m = 0; m < n; m++) {
					modelIndices[m] = b.getUShort();
				}
			} else if (opcode >= 40 && opcode < 50) {
				oldColors[opcode - 40] = b.getUShort();
			} else if (opcode >= 50 && opcode < 60) {
				newColors[opcode - 50] = b.getUShort();
			} else if (opcode >= 60 && opcode < 70) {
				headModelIndices[opcode - 60] = b.getUShort();
			} else {
				System.out.println("Error unrecognised config code: " + opcode);
			}
		}
	}

	public Model getModel() {
		if (modelIndices == null) {
			return null;
		}

		Model[] models = new Model[modelIndices.length];

		for (int i = 0; i < modelIndices.length; i++) {
			models[i] = new Model(modelIndices[i]);
		}

		Model m;

		if (models.length == 1) {
			m = models[0];
		} else {
			m = new Model(models, models.length);
		}

		for (int i = 0; i < 6; i++) {
			if (oldColors[i] == 0) {
				break;
			}
			m.recolor(oldColors[i], newColors[i]);
		}
		return m;
	}

	public Model getHeadModel() {
		Model[] models = new Model[5];
		int modelCount = 0;

		for (int n = 0; n < 5; n++) {
			if (headModelIndices[n] != -1) {
				models[modelCount++] = new Model(headModelIndices[n]);
			}
		}

		Model m = new Model(models, modelCount);

		for (int n = 0; n < 6; n++) {
			if (oldColors[n] == 0) {
				break;
			}
			m.recolor(oldColors[n], newColors[n]);
		}
		return m;
	}
}

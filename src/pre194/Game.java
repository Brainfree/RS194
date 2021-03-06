package pre194;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import net.burtleburtle.bob.rand.IsaacRandom;
import rs317.ModelExtension;

/**
 * The RuneScape game class for revision #194.
 *
 * @author Dane
 */
public class Game extends GameShell {

	private static final Logger logger = Logger.getLogger(Game.class.getName());

	/* Constants */
	public static final long serialVersionUID = -1412785310365267985L;

	public static final int[] SPOKEN_COLORS = {0xFFFF00, 0xFF0000, 0xFF00, 0xFFFF, 0xFF00FF, 0xFFFFFF};

	public static final int SCROLLBAR_TRACK_COLOR = 0x23201B;
	public static final int SCROLLBAR_GRIP_LOWLIGHT = 0x332D25;
	public static final int SCROLLBAR_GRIP_HIGHLIGHT = 0x766654;
	public static final int SCROLLBAR_GRIP_FOREGROUND = 0x4D4233;

	private static final BigInteger RSA_EXPONENT = new BigInteger("65537");
	private static final BigInteger RSA_MODULUS = new BigInteger("118864839396833161133277295108759594878091760480449050972427371143944653378606502852179891832913336184499656858177875574679796153895987835111782780500397033108690142008149540462791311216350792149909539866285095935906624469838539471628409143101360641327529747825355089296681933929964075178157444376735277489229");

	public static final int LOCALPLAYER_INDEX = 2047;
	public static final int MAX_ENTITY_COUNT = 2048;

	public static final int[] EXPERIENCE_TABLE = new int[99];

	public static int cycle;

	/**
	 * Static initializer
	 */
	static {
		int i = 0;

		for (int n = 0; n < 99; n++) {
			int level = n + 1;
			int exp = (int) ((double) level + 300.0 * Math.pow(2.0, (double) level / 7.0));
			i += exp;
			EXPERIENCE_TABLE[n] = i / 4;
		}
	}

	/* CRC */
	public int[] archiveCRC = new int[8];
	public CRC32 crc32 = new CRC32();

	/* Game */
	public int allowChatEffects;
	public int wildernessLevel;
	public int systemUpdate;
	public int inMultizone;
	public long sessionId;

	public int[] characterDesigns = new int[7];
	public int[] characterDesignColors = new int[5];

	/* Input */
	public int dragCycle;
	public int hoveredInterfaceIndex;
	public int scrollGripInputPadding;

	/* Settings */
	public int portoff;
	public int nodeid = 10;
	public int[] variables = new int[2000];

	/* Skills */
	public int[] skillExperience = new int[50];
	public int[] skillLevel = new int[50];
	public int[] skillLevelReal = new int[50];

	/* Scene */
	public int currentLevel;
	public int drawCycle;
	public int sceneDelta;
	public int sceneState;

	public byte[][][] levelRenderFlags;
	public int[][][] levelHeightMaps;

	public int[][] tileCycle = new int[104][104];
	public CollisionMap[] collisions = new CollisionMap[4];
	public SceneGraph graph;

	// used for updating textures
	public byte[] tmpTexels = new byte[16384];

	/* Map/Region Loading */
	public int mapBaseX;
	public int mapBaseY;
	public int centerSectorX;
	public int centerSectorY;
	public int[] mapIndices;
	public byte[][] mapLandData;
	public int mapLastBaseX;
	public int mapLastBaseZ;
	public byte[][] mapLocData;

	/* Camera */
	public int cameraMaxY;
	public int cameraOffsetCycle;
	public int cameraOffsetX;
	public int cameraOffsetZ;
	public int cameraOffsetYaw;
	public int cameraOffsetXModifier = 2;
	public int cameraOffsetZModifier = 2;
	public int cameraOffsetYawModifier = 1;
	public int cameraOrbitX;
	public int cameraOrbitZ;
	public int cameraOrbitPitch = 128;
	public int cameraOrbitYaw;
	public int cameraX;
	public int cameraY;
	public int cameraZ;
	public int cameraPitch;
	public int cameraPitchModifier;
	public int cameraYaw;
	public int cameraYawModifier;

	/* Entities */
	public int lastSceneLevel = -1;

	public int entityCount;
	public int playerCount;

	private NPC[] npcs = new NPC[MAX_ENTITY_COUNT];
	private Player[] players = new Player[MAX_ENTITY_COUNT];

	public int[] npcIndices = new int[MAX_ENTITY_COUNT];
	public int[] playerIndices = new int[MAX_ENTITY_COUNT];

	public Buffer[] playerBuffers = new Buffer[MAX_ENTITY_COUNT];

	private Player localPlayer;
	public int localPlayerIndex = -1;

	public int deadEntityCount;
	public int[] entityUpdateIndices = new int[MAX_ENTITY_COUNT];
	public int[] deadEntityIndices = new int[1000];

	/* Linked Entites */
	public LinkedQueue[][][] levelObjects = new LinkedQueue[4][104][104];
	public LinkedQueue projectiles = new LinkedQueue();
	public LinkedQueue animatedLocations = new LinkedQueue();
	public LinkedQueue spawntLocs = new LinkedQueue();
	public LinkedQueue spotanims = new LinkedQueue();
	public LinkedQueue temporaryLocs = new LinkedQueue();

	/* Networking */
	public int netHeartbeatCycle;
	public int netIdleCycles;
	public IsaacRandom isaac;
	public int packetSize;
	public int packetType;
	public int lastPacketType;

	public int outRate, inRate;
	public int outBytes, inBytes;
	public long nextRateUpdate = System.currentTimeMillis();

	// used for spawning stuff
	public int netTileX, netTileZ;

	public Buffer in = Buffer.get(1);
	public Buffer out = Buffer.get(1);
	public Buffer login = Buffer.get(1);
	public BufferedStream stream;

	/* Fonts */
	public BitmapFont fontSmall;
	public BitmapFont fontNormal;
	public BitmapFont fontBold;
	public BitmapFont fontFancy;

	/* Bitmaps */
	public Sprite buttonDisabled;
	public Sprite buttonEnabled;
	public Sprite[] headicons = new Sprite[20];
	public Sprite[] hitmarks = new Sprite[20];

	/* Indexed Bitmaps */
	public IndexedSprite backbase1;
	public IndexedSprite backbase2;
	public IndexedSprite backhmid1;
	public IndexedSprite invback;
	public IndexedSprite mapback;
	public IndexedSprite[] mapscenes = new IndexedSprite[50];
	public IndexedSprite redstone1;
	public IndexedSprite redstone1h;
	public IndexedSprite redstone1hv;
	public IndexedSprite redstone1v;
	public IndexedSprite redstone2;
	public IndexedSprite redstone2h;
	public IndexedSprite redstone2hv;
	public IndexedSprite redstone2v;
	public IndexedSprite redstone3;
	public IndexedSprite redstone3v;
	public IndexedSprite[] runes;
	public IndexedSprite scrollbar1;
	public IndexedSprite scrollbar2;
	public IndexedSprite sideicons1;
	public IndexedSprite sideicons2;

	/* Image Producers */
	// gameframe
	public ImageProducer backhmid2;
	public ImageProducer backleft1;
	public ImageProducer backleft2;
	public ImageProducer backright1;
	public ImageProducer backright2;
	public ImageProducer backtop1;
	public ImageProducer backtop2;
	public ImageProducer backvmid1;
	public ImageProducer backvmid2;
	public ImageProducer backvmid3;

	// title
	public ImageProducer titleBottom;
	public ImageProducer titleBottomLeft;
	public ImageProducer titleBottomRight;
	public ImageProducer titleCenter;
	public ImageProducer titleLeft;
	public ImageProducer titleLeftSpace;
	public ImageProducer titleRight;
	public ImageProducer titleRightSpace;
	public ImageProducer titleTop;

	/* Minimap Area */
	public ImageProducer maparea;

	public Sprite compass;
	public int[] compassLeft = new int[33];
	public int[] compassLineWidth = new int[33];

	public Sprite mapdot1;
	public Sprite mapdot2;
	public Sprite mapdot3;
	public Sprite[] mapfunctions = new Sprite[50];
	public Sprite minimap;
	public int minimapDrawPhase;
	public int minimapFunctionCount;
	public Sprite[] minimapFunctions = new Sprite[1000];
	public int[] minimapFunctionX = new int[1000];
	public int[] minimapFunctionY = new int[1000];
	public int minimapLastUpdateCycle;
	public int[] minimapLeft = new int[151];
	public int[] minimapLineWidth = new int[151];

	/* Sidebar */
	public ImageProducer sidebar;
	public int sidebarHoveredInterfaceIndex;
	public int sidebarWidgetIndex = -1;
	public int[] sidebarOffsets;
	public boolean sidebarRedraw = false;
	public boolean sidebarRedrawIcons = false;
	public int[] tabWidgetIndex = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
	public ImageProducer sideiconsBottom;
	public ImageProducer sideiconsTop;

	/* Viewport */
	public ImageProducer viewport;
	public int drawX = -1;
	public int drawY = -1;
	public int viewportHoveredInterfaceIndex;
	public int viewportWidgetIndex = -1;
	public int[] viewportOffsets;

	/* Strings */
	public String address = "127.0.0.1";
	public String midi;

	/* Errors */
	public boolean errorAlreadyStarted = false;
	public boolean errorInvalidHost = false;
	public boolean errorLoading = false;

	/* Booleans */
	public boolean scrollGripHeld = false;
	public boolean ingame = false;
	public static boolean lowmemory = true;
	public boolean characterDesignIsMale = true;
	public boolean midiPlaying = true;
	public boolean mouseOneButton;
	public boolean scrollButtonHeld = false;
	public boolean redraw = false;
	public static boolean started;
	public boolean characterDesignUpdate = false;

	/* Friends/Ignore List */
	public int friendCount;
	public String[] friendName = new String[100];
	public int[] friendWorld = new int[100];
	public int ignoreCount;
	public long[] ignoreNameLong = new long[100];

	/* Chat */
	public ImageProducer chatarea;
	public IndexedSprite chatback;
	public Widget chatbox = new Widget();
	public int chatHeight = 78;
	public int chatScrollAmount;
	public int chatDialogueInputType;
	public int chatSendFriendMessageIndex;
	public boolean chatContinuingDialogue = false;
	public String chatDialogueInput = "";
	public String chatDialogueMessage = "";
	public String chatTransferInput = "";
	public int chatHoveredInterfaceIndex;
	public String chatInput = "";
	public int chatWidgetIndex = -1;
	public String[] chatMessage = new String[100];
	public String[] chatMessagePrefix = new String[100];
	public int[] chatMessageType = new int[100];
	public int[] chatOffsets;
	public int chatPrivateSetting;
	public int chatPublicSetting;
	public boolean chatRedraw = false;
	public boolean chatRedrawSettings = false;
	public ImageProducer chatsettings;
	public boolean chatShowDialogueInput = false;
	public boolean chatShowTransferInput = false;
	public int chatTradeDuelSetting;

	/* Private Messages */
	public int privateMessageCount;
	public int[] privateMessageIndex = new int[100];

	/* Login/Title Screen */
	public int loginFocusedLine;
	public String loginMessage1 = "";
	public String loginMessage2 = "";
	public String loginPassword = "";
	public String loginPasswordConfirm = "";
	public String loginUsername = "";

	public Archive titleArchive;
	public IndexedSprite titlebox;
	public IndexedSprite titlebutton;
	public int titleState;

	/* Option Menu */
	public boolean optionMenuVisible = false;
	public int optionMenuX;
	public int optionMenuY;
	public int optionMenuWidth;
	public int optionMenuHeight;
	public int optionMenuArea;
	public String[] options = new String[500];
	public int[] optionType = new int[500];
	public int[] optionParamA = new int[500];
	public int[] optionParamB = new int[500];
	public int[] optionParamC = new int[500];
	public int optionCount;

	/* Title Flames */
	public int[] flameBuffer1;
	public int[] flameBuffer2;
	public int flameCycle1;
	public int flameCycle2;
	public int[] flameGradient;
	public int[] flameGradientGreen;
	public int[] flameGradientRed;
	public int[] flameGradientViolet;
	public int[] flameIntensity;
	public int[] flameIntensityBuffer;
	public Sprite flameLeft;
	public int flameOffset;
	public Sprite flameRight;
	public int[] flameShiftX = new int[256];
	public boolean runFlames = false;
	public boolean flameThreadActive = false;
	public boolean flamesActive = false;

	/* Selection Fields */
	public int selectedArea;
	public int selectedCycle;
	public int selectedFlags;
	public int selectedInterfaceIndex;
	public int selectedInterfaceSlot;
	public boolean selectedObject;
	public int selectedObjIndex;
	public int selectedObjInterface;
	public String selectedObjName;
	public int selectedObjSlot;
	public boolean selectedSpell;
	public int selectedSpellIndex;
	public String selectedSpellPrefix;
	public int selectedTab = 3;

	/* Pathfinding */
	public int[] waypointX = new int[4000];
	public int[] waypointY = new int[4000];
	public int[][] pathDistance = new int[104][104];
	public int[][] pathWaypoint = new int[104][104];

	/* Mouse Cross */
	public int crossCycle;
	public Sprite[] crosses = new Sprite[8];
	public int crossType;
	public int crossX;
	public int crossY;

	/**
	 * Application entry point ;)
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Signlink.startPrivate(InetAddress.getLocalHost());
			Game g = new Game();

			if (args.length > 0) {
				g.nodeid = Integer.parseInt(args[0]);
			}

			if (args.length > 1) {
				g.portoff = Integer.parseInt(args[1]);
			}

			if (args.length > 2 && args[2].equalsIgnoreCase("lowmem")) {
				setLowMemory();
			} else {
				setHighMemory();
			}

			if (args.length > 3) {

			}

			g.initFrame(789, 532);
		} catch (UnknownHostException | NumberFormatException e) {
			logger.log(Level.SEVERE, "Error creating game instance", e);
		}
	}

	@Override
	public void init() {
		portoff = Integer.parseInt(getParameter("portoff"));
		nodeid = Integer.parseInt(getParameter("nodeid"));
		String lowmem = getParameter("lowmem");

		if (lowmem != null && lowmem.equals("1")) {
			setLowMemory();
		} else {
			setHighMemory();
		}

		initApplet(789, 532);
	}

	public static final void setLowMemory() {
		SceneGraph.lowmemory = true;
		Graphics3D.lowmemory = true;
		lowmemory = true;
		Scene.lowmemory = true;
	}

	public static final void setHighMemory() {
		SceneGraph.lowmemory = false;
		Graphics3D.lowmemory = false;
		lowmemory = false;
		Scene.lowmemory = false;
	}

	public boolean isValidHost(String host) {
		boolean valid = false;
		if (host.endsWith("jagex.com")) {
			valid = true;
		} else if (host.endsWith("runescape.com")) {
			valid = true;
		} else if (host.endsWith("192.168.1.252")) {
			valid = true;
		} else if (host.endsWith("192.168.1.2")) {
			valid = true;
		} else if (host.endsWith("69.1.68.43")) {
			valid = true;
		} else if (host.endsWith("127.0.0.1")) {
			valid = true;
		}
		return valid;
	}

	public void getArchiveChecksums() {
		int wait = 5;
		archiveCRC[7] = 0;

		while (archiveCRC[7] == 0) {
			drawProgress("Connecting to fileserver", 10);
			try {
				try (DataInputStream dis = openURL("crc" + (int) (Math.random() * 99999999))) {
					Buffer b = new Buffer(new byte[32]);
					dis.readFully(b.data, 0, 32);

					for (int n = 0; n < 8; n++) {
						archiveCRC[n] = b.getInt();
					}
				}
			} catch (IOException e) {
				logger.log(Level.WARNING, "Error reading CRC", e);

				for (int s = wait; s > 0; s--) {
					drawProgress(("Error loading - Will retry in " + s + " secs."), 10);
					try {
						Thread.sleep(1000L);
					} catch (Exception exception) {
						/* empty */
					}
				}

				wait *= 2;

				if (wait > 60) {
					wait = 60;
				}
			}
		}
	}

	public void loadFonts(Archive title) {
		fontSmall = new BitmapFont("p11", title);
		fontNormal = new BitmapFont("p12", title);
		fontBold = new BitmapFont("b12", title);
		fontFancy = new BitmapFont("q8", title);
	}

	public void initSceneComponents() {
		levelRenderFlags = new byte[4][104][104];
		levelHeightMaps = new int[4][105][105];

		graph = new SceneGraph(104, 104, 4, levelHeightMaps);

		for (int n = 0; n < 4; n++) {
			collisions[n] = new CollisionMap(104, 104);
		}
	}

	public void initAudio() {
		Signlink.audio = new Audio();
		Signlink.startThread(Signlink.audio, Thread.MAX_PRIORITY);
	}

	public void loadMedia(Archive media) {
		drawProgress("Unpacking media", 80);
		invback = new IndexedSprite(media, "invback", 0);
		chatback = new IndexedSprite(media, "chatback", 0);
		mapback = new IndexedSprite(media, "mapback", 0);
		backbase1 = new IndexedSprite(media, "backbase1", 0);
		backbase2 = new IndexedSprite(media, "backbase2", 0);
		backhmid1 = new IndexedSprite(media, "backhmid1", 0);
		sideicons1 = new IndexedSprite(media, "sideicons1", 0);
		sideicons2 = new IndexedSprite(media, "sideicons2", 0);
		compass = new Sprite(media, "compass", 0);

		try {
			for (int i = 0; i < 50; i++) {
				mapscenes[i] = new IndexedSprite(media, "mapscene", i);
			}
		} catch (Exception e) {
			/* empty */
		}
		try {
			for (int i = 0; i < 50; i++) {
				mapfunctions[i] = new Sprite(media, "mapfunction", i);
			}
		} catch (Exception exception) {
			/* empty */
		}
		try {
			for (int i = 0; i < 20; i++) {
				hitmarks[i] = new Sprite(media, "hitmarks", i);
			}
		} catch (Exception exception) {
			/* empty */
		}
		try {
			for (int i = 0; i < 20; i++) {
				headicons[i] = new Sprite(media, "headicons", i);
			}
		} catch (Exception exception) {
			/* empty */
		}

		for (int i = 0; i < 8; i++) {
			crosses[i] = new Sprite(media, "cross", i);
		}

		mapdot1 = new Sprite(media, "mapdots", 0);
		mapdot2 = new Sprite(media, "mapdots", 1);
		mapdot3 = new Sprite(media, "mapdots", 2);

		scrollbar1 = new IndexedSprite(media, "scrollbar", 0);
		scrollbar2 = new IndexedSprite(media, "scrollbar", 1);

		redstone1 = new IndexedSprite(media, "redstone1", 0);
		redstone2 = new IndexedSprite(media, "redstone2", 0);
		redstone3 = new IndexedSprite(media, "redstone3", 0);

		redstone1h = new IndexedSprite(media, "redstone1", 0);
		redstone1h.flipHorizontally();
		redstone2h = new IndexedSprite(media, "redstone2", 0);
		redstone2h.flipHorizontally();

		redstone1v = new IndexedSprite(media, "redstone1", 0);
		redstone1v.flipVertically();
		redstone2v = new IndexedSprite(media, "redstone2", 0);
		redstone2v.flipVertically();
		redstone3v = new IndexedSprite(media, "redstone3", 0);
		redstone3v.flipVertically();

		redstone1hv = new IndexedSprite(media, "redstone1", 0);
		redstone1hv.flipHorizontally();
		redstone1hv.flipVertically();
		redstone2hv = new IndexedSprite(media, "redstone2", 0);
		redstone2hv.flipHorizontally();
		redstone2hv.flipVertically();

		Sprite s = new Sprite(media, "backleft1", 0);
		backleft1 = new ImageProducer(s.width, s.height);
		s.drawOpaque(0, 0);

		s = new Sprite(media, "backleft2", 0);
		backleft2 = new ImageProducer(s.width, s.height);
		s.drawOpaque(0, 0);

		s = new Sprite(media, "backright1", 0);
		backright1 = new ImageProducer(s.width, s.height);
		s.drawOpaque(0, 0);

		s = new Sprite(media, "backright2", 0);
		backright2 = new ImageProducer(s.width, s.height);
		s.drawOpaque(0, 0);

		s = new Sprite(media, "backtop1", 0);
		backtop1 = new ImageProducer(s.width, s.height);
		s.drawOpaque(0, 0);

		s = new Sprite(media, "backtop2", 0);
		backtop2 = new ImageProducer(s.width, s.height);
		s.drawOpaque(0, 0);

		s = new Sprite(media, "backvmid1", 0);
		backvmid1 = new ImageProducer(s.width, s.height);
		s.drawOpaque(0, 0);

		s = new Sprite(media, "backvmid2", 0);
		backvmid2 = new ImageProducer(s.width, s.height);
		s.drawOpaque(0, 0);

		s = new Sprite(media, "backvmid3", 0);
		backvmid3 = new ImageProducer(s.width, s.height);
		s.drawOpaque(0, 0);

		s = new Sprite(media, "backhmid2", 0);
		backhmid2 = new ImageProducer(s.width, s.height);
		s.drawOpaque(0, 0);
	}

	public void loadTextures(Archive textures) {
		drawProgress("Unpacking textures", 85);
		Graphics3D.unpackTextures(textures);
		Graphics3D.generatePalette(0.8);
		Graphics3D.setupPools(20);
	}

	public void loadModels(Archive models) {
		drawProgress("Unpacking models", 85);
		Model.load(models);
		ModelExtension.unpack();
		AnimationTransform.load(models);
		AnimationFrame.load(models);
	}

	public void loadConfigs(Archive config) {
		drawProgress("Unpacking config", 85);
		Animation.load(config);
		LocationInfo.load(config);
		Floor.unpack(config);
		ObjectInfo.load(config);
		NPCInfo.load(config);
		Identikit.load(config);
		SpotAnimation.load(config);
		Varp.load(config);
	}

	public void prepareRotatables() {
		for (int y = 0; y < 33; y++) {
			int min = 999;
			int max = 0;
			for (int x = 0; x < 35; x++) {
				if ((mapback.data[x + y * (mapback.width)]) == 0) {
					if (min == 999) {
						min = x;
					}
				} else if (min != 999) {
					max = x;
					break;
				}
			}
			compassLeft[y] = min;
			compassLineWidth[y] = max - min;
		}

		for (int y = 9; y < 160; y++) {
			int min = 999;
			int max = 0;
			for (int x = 10; x < 168; x++) {
				if ((mapback.data[x + y * (mapback.width)]) == 0 && (x > 34 || y > 34)) {
					if (min == 999) {
						min = x;
					}
				} else if (min != 999) {
					max = x;
					break;
				}
			}
			minimapLeft[y - 9] = min - 21;
			minimapLineWidth[y - 9] = max - min;
		}
	}

	@Override
	public void load() {
		if (Signlink.sunjava) {
			minDelay = 5;
		}

		Signlink.midi = "450";

		if (started) {
			errorAlreadyStarted = true;
			return;
		}

		started = true;

		if (!isValidHost(getDocumentHost())) {
			errorInvalidHost = true;
			return;
		}

		try {
			//getArchiveChecksums();

			titleArchive = loadArchive("title screen", "title", archiveCRC[1], 10);

			loadFonts(titleArchive);
			loadTitleBackground();
			loadTitleForeground();

			Archive config = loadArchive("config", "config", archiveCRC[2], 20);
			Archive widget = loadArchive("interface", "interface", archiveCRC[3], 30);
			Archive media = loadArchive("2d graphics", "media", archiveCRC[4], 40);
			Archive models = loadArchive("3d graphics", "models", archiveCRC[5], 50);
			Archive textures = loadArchive("textures", "textures", archiveCRC[6], 60);

			Censor.load(loadArchive("chat system", "wordenc", archiveCRC[7], 70));

			initSceneComponents();
			initAudio();

			minimap = new Sprite(512, 512);

			loadMedia(media);
			loadTextures(textures);
			loadModels(models);
			loadConfigs(config);

			drawProgress("Unpacking interfaces", 90);
			Widget.load(new BitmapFont[]{fontSmall, fontNormal, fontBold, fontFancy}, media, widget);

			drawProgress("Preparing game engine", 95);
			prepareRotatables();

			Graphics3D.prepareOffsets(479, 96);
			chatOffsets = Graphics3D.offsets;

			Graphics3D.prepareOffsets(190, 261);
			sidebarOffsets = Graphics3D.offsets;

			Graphics3D.prepareOffsets(512, 334);
			viewportOffsets = Graphics3D.offsets;

			SceneGraph.init(512, 334, 500, 800);
		} catch (Exception e) {
			errorLoading = true;
			logger.log(Level.SEVERE, "Error starting game", e);
		}
	}

	/**
	 * Attempts to load the archive data locally. If there is no local data stored, it will open a connection to the URL
	 * of the archive and download it.
	 *
	 * @param archiveName the archive name.
	 * @param archiveFile the archive file.
	 * @param crc the archive crc.
	 * @param percent the load percentage.
	 * @return the archive file.
	 */
	public Archive loadArchive(String archiveName, String archiveFile, int crc, int percent) {
		int wait = 5;
		byte[] data = Signlink.loadFile(archiveFile);

		if (data != null) {
			crc32.reset();
			crc32.update(data);

			int readcrc = (int) crc32.getValue();

			if (readcrc != crc) {
				// data = null;
			}
		}

		if (data != null) {
			return new Archive(data);
		}

		while (data == null) {
			drawProgress("Requesting " + archiveName, percent);

			try {
				int lastPercent = 0;

				try (DataInputStream dis = openURL(archiveFile + crc)) {
					byte[] header = new byte[6];
					dis.readFully(header, 0, 6);

					Buffer b = new Buffer(header);
					b.position = 3;

					int size = b.getInt24() + 6;
					int read = 6;

					data = new byte[size];

					System.arraycopy(header, 0, data, 0, 6);

					while (read < size) {
						int available = size - read;

						if (available > 1000) {
							available = 1000;
						}

						read += dis.read(data, read, available);

						int currentPercent = read * 100 / size;

						if (currentPercent != lastPercent) {
							drawProgress("Loading " + archiveName + " - " + currentPercent + "%", currentPercent);
						}

						lastPercent = currentPercent;
					}
				}
			} catch (IOException e) {
				data = null;

				logger.log(Level.WARNING, "Error loading archive", e);

				for (int s = wait; s > 0; s--) {
					drawProgress(("Error loading - Will retry in " + s + " secs."), percent);
					try {
						Thread.sleep(1000L);
					} catch (Exception ex) {
					}
				}

				wait *= 2;

				if (wait > 60) {
					wait = 60;
				}
			}
		}
		Signlink.saveFile(archiveFile, data);
		return new Archive(data);
	}

	public final void loadTitleBackground() {
		Sprite s = new Sprite(titleArchive.get("title.dat"), this);
		titleLeft.prepare();
		s.drawOpaque(0, 0);

		titleRight.prepare();
		s.drawOpaque(-661, 0);

		titleTop.prepare();
		s.drawOpaque(-128, 0);

		titleBottom.prepare();
		s.drawOpaque(-214, -386);

		titleCenter.prepare();
		s.drawOpaque(-214, -186);

		titleBottomLeft.prepare();
		s.drawOpaque(0, -265);

		titleBottomRight.prepare();
		s.drawOpaque(-574, -265);

		titleLeftSpace.prepare();
		s.drawOpaque(-128, -186);

		titleRightSpace.prepare();
		s.drawOpaque(-574, -186);

		int[] line = new int[s.width];
		for (int y = 0; y < s.height; y++) {
			for (int x = 0; x < s.width; x++) {
				line[x] = (s.pixels[(s.width - x - 1 + s.width * y)]);
			}
			System.arraycopy(line, 0, s.pixels, s.width * y, s.width);
		}

		titleLeft.prepare();
		s.drawOpaque(394, 0);

		titleRight.prepare();
		s.drawOpaque(-267, 0);

		titleTop.prepare();
		s.drawOpaque(266, 0);

		titleBottom.prepare();
		s.drawOpaque(180, -386);

		titleCenter.prepare();
		s.drawOpaque(180, -186);

		titleBottomLeft.prepare();
		s.drawOpaque(394, -265);

		titleBottomRight.prepare();
		s.drawOpaque(-180, -265);

		titleLeftSpace.prepare();
		s.drawOpaque(212, -186);

		titleRightSpace.prepare();
		s.drawOpaque(-180, -186);

		s = new Sprite(titleArchive, "logo", 0);
		titleTop.prepare();
		s.draw((width / 2) - (s.width / 2) - 128, 18);
		System.gc();
	}

	@Override
	public void drawProgress(String caption, int percent) {
		loadTitle();

		if (titleArchive == null) {
			super.drawProgress(caption, percent);
		} else {
			titleCenter.prepare();

			final int centerX = titleCenter.width / 2;
			final int centerY = titleCenter.height / 2;

			final int w = 304;
			final int h = 34;

			int x = centerX;
			int y = centerY;

			fontBold.drawCentered("RuneScape is loading - please wait...", x, y - 46, 0xFFFFFF);

			x -= w / 2;
			y -= (h / 2);
			y -= 21; // titleCenter isn't even perfectly centered

			Graphics2D.fillRect(x, y, w, h, 0);
			Graphics2D.drawRect(x, y, w, h, 0x8C1111);
			Graphics2D.fillRect(x + 2, y + 2, ((w - 4) * percent) / 100, h - 4, 0x8C1111);

			fontBold.drawCentered(caption, centerX, centerY - (h / 2), 0xFFFFFF);

			titleCenter.draw(graphics, 214, 186);

			if (redraw) {
				redraw = false;

				if (!flamesActive) {
					titleLeft.draw(graphics, 0, 0);
					titleRight.draw(graphics, 661, 0);
				}

				titleTop.draw(graphics, 128, 0);
				titleBottom.draw(graphics, 214, 386);
				titleBottomLeft.draw(graphics, 0, 265);
				titleBottomRight.draw(graphics, 574, 265);
				titleLeftSpace.draw(graphics, 128, 186);
				titleRightSpace.draw(graphics, 574, 186);
			}
		}
	}

	@Override
	public void update() {
		if (errorAlreadyStarted || errorLoading || errorInvalidHost) {
			return;
		}

		cycle++;

		if (!ingame) {
			updateTitle();
		} else {
			updateGame();
		}
	}

	@Override
	public void draw() {
		if (errorAlreadyStarted || errorLoading || errorInvalidHost) {
			drawErrorScreen();
		} else {
			if (!ingame) {
				drawTitle();
			} else {
				drawGame();
			}
			dragCycle = 0;
		}
	}

	public final void drawErrorScreen() {
		Graphics g = graphics;
		g.setColor(Color.black);
		g.fillRect(0, 0, 789, 532);

		setLoopRate(1);

		if (errorLoading) {
			flamesActive = false;
			g.setFont(new Font("Helvetica", 1, 16));
			g.setColor(Color.yellow);
			int y = 35;
			g.drawString("Sorry, an error has occured whilst loading RuneScape", 30, y);
			y += 50;
			g.setColor(Color.white);
			g.drawString("To fix this try the following (in order):", 30, y);
			y += 50;
			g.setColor(Color.white);
			g.setFont(new Font("Helvetica", 1, 12));
			g.drawString("1: Try closing ALL open web-browser windows, and reloading", 30, y);
			y += 30;
			g.drawString("2: Try clearing your web-browsers cache from tools->internet options", 30, y);
			y += 30;
			g.drawString("3: Try using a different game-world", 30, y);
			y += 30;
			g.drawString("4: Try rebooting your computer", 30, y);
			y += 30;
			g.drawString("5: Try selecting a different version of Java from the play-game menu", 30, y);
		}
		if (errorInvalidHost) {
			flamesActive = false;
			g.setFont(new Font("Helvetica", 1, 20));
			g.setColor(Color.white);
			g.drawString("Error - unable to load game!", 50, 50);
			g.drawString("To play RuneScape make sure you play from", 50, 100);
			g.drawString("http://www.runescape.com", 50, 150);
		}
		if (errorAlreadyStarted) {
			flamesActive = false;
			g.setColor(Color.yellow);
			int y = 35;
			g.drawString("Error a copy of RuneScape already appears to be loaded", 30, y);
			y += 50;
			g.setColor(Color.white);
			g.drawString("To fix this try the following (in order):", 30, y);
			y += 50;
			g.setColor(Color.white);
			g.setFont(new Font("Helvetica", 1, 12));
			g.drawString("1: Try closing ALL open web-browser windows, and reloading", 30, y);
			y += 30;
			g.drawString("2: Try rebooting your computer, and reloading", 30, y);
			y += 30;
		}
	}

	@Override
	public void refresh() {
		redraw = true;
	}

	public final void loadIngame() {
		if (chatarea == null) {
			unloadTitle();
			titleTop = null;
			titleBottom = null;
			titleCenter = null;
			titleLeft = null;
			titleRight = null;
			titleBottomLeft = null;
			titleBottomRight = null;
			titleLeftSpace = null;
			titleRightSpace = null;

			chatarea = new ImageProducer(479, 96);
			chatsettings = new ImageProducer(501, 61);

			sideiconsTop = new ImageProducer(269, 66);
			sidebar = new ImageProducer(190, 261);
			sideiconsBottom = new ImageProducer(288, 40);

			maparea = new ImageProducer(168, 160);
			Graphics2D.clear();
			mapback.draw(0, 0);

			viewport = new ImageProducer(512, 334);
			Graphics2D.clear();

			redraw = true;
		}
	}

	public final void loadTitle() {
		if (titleTop == null) {
			chatarea = null;
			maparea = null;
			sidebar = null;
			viewport = null;
			chatsettings = null;
			sideiconsBottom = null;
			sideiconsTop = null;

			titleLeft = new ImageProducer(128, 265);
			Graphics2D.clear();

			titleRight = new ImageProducer(128, 265);
			Graphics2D.clear();

			titleTop = new ImageProducer(533, 186);
			Graphics2D.clear();

			titleBottom = new ImageProducer(360, 146);
			Graphics2D.clear();

			titleCenter = new ImageProducer(360, 200);
			Graphics2D.clear();

			titleBottomLeft = new ImageProducer(214, 267);
			Graphics2D.clear();

			titleBottomRight = new ImageProducer(215, 267);
			Graphics2D.clear();

			titleLeftSpace = new ImageProducer(86, 79);
			Graphics2D.clear();

			titleRightSpace = new ImageProducer(87, 79);
			Graphics2D.clear();

			if (titleArchive != null) {
				loadTitleBackground();
				loadTitleForeground();
			}
			redraw = true;
		}
	}

	public final void updateTitle() {
		if (titleState == 0) {
			int x = width / 2;
			int y = height / 2 + 90;

			if (mouseButton == 1 && clickX >= x - 75 && clickX <= x + 75 && clickY >= y - 20 && clickY <= y + 20) {
				loginMessage1 = "";
				loginMessage2 = "Enter your username & password.";
				titleState = 2;
				loginFocusedLine = 0;
			}
		} else if (titleState == 1 || titleState == 2) {
			int y = height / 2 - 30;
			y += 30;

			if (mouseButton == 1 && clickY >= y - 15 && clickY < y) {
				loginFocusedLine = 0;
			}

			y += 15;

			if (mouseButton == 1 && clickY >= y - 15 && clickY < y) {
				loginFocusedLine = 1;
			}

			y += 15;

			if (mouseButton == 1 && clickY >= y - 15 && clickY < y && titleState == 1) {
				loginFocusedLine = 2;
			}

			int x = width / 2 - 80;
			y = height / 2 + 60;

			if (mouseButton == 1 && clickX >= x - 75 && clickX <= x + 75 && clickY >= y - 20 && clickY <= y + 20) {
				if (titleState == 1) {
					register(0, loginUsername, loginPassword, 0);
				} else {
					login(loginUsername, loginPassword, false);
				}
			}
			x = width / 2 + 80;

			if (mouseButton == 1 && clickX >= x - 75 && clickX <= x + 75 && clickY >= y - 20 && clickY <= y + 20) {
				titleState = 0;
			}

			for (;;) {
				int c = pollKey();

				if (c == -1) {
					break;
				}

				boolean isAscii = StringUtil.isASCII((char) c);

				if (loginFocusedLine == 0) {
					if (c == 8 && loginUsername.length() > 0) {
						loginUsername = loginUsername.substring(0, loginUsername.length() - 1);
					}

					if (c == KeyEvent.VK_TAB || c == KeyEvent.VK_ENTER) {
						loginFocusedLine = 1;
					}

					if (isAscii) {
						loginUsername += (char) c;
					}

					if (loginUsername.length() > 12) {
						loginUsername = loginUsername.substring(0, 12);
					}
				} else if (loginFocusedLine == 1) {
					if (c == 8 && loginPassword.length() > 0) {
						loginPassword = loginPassword.substring(0, loginPassword.length() - 1);
					}

					if (c == KeyEvent.VK_TAB || c == KeyEvent.VK_ENTER) {
						if (titleState == 1) {
							loginFocusedLine = 2;
						} else {
							loginFocusedLine = 0;
						}
					}

					if (isAscii) {
						loginPassword += (char) c;
					}

					if (loginPassword.length() > 20) {
						loginPassword = loginPassword.substring(0, 20);
					}
				} else if (loginFocusedLine == 2) {
					if (c == 8 && loginPasswordConfirm.length() > 0) {
						loginPasswordConfirm = loginPasswordConfirm.substring(0, loginPasswordConfirm.length() - 1);
					}

					if (c == KeyEvent.VK_TAB || c == KeyEvent.VK_ENTER) {
						loginFocusedLine = 0;
					}

					if (isAscii) {
						loginPasswordConfirm += (char) c;
					}

					if (loginPasswordConfirm.length() > 20) {
						loginPasswordConfirm = loginPasswordConfirm.substring(0, 20);
					}
				}
			}
		}
	}

	public final void loadTitleForeground() {
		titlebox = new IndexedSprite(titleArchive, "titlebox", 0);
		titlebutton = new IndexedSprite(titleArchive, "titlebutton", 0);
		runes = new IndexedSprite[12];

		for (int i = 0; i < 12; i++) {
			runes[i] = new IndexedSprite(titleArchive, "runes", i);
		}

		flameLeft = new Sprite(128, 265);
		flameRight = new Sprite(128, 265);

		System.arraycopy(titleLeft.pixels, 0, flameLeft.pixels, 0, 33920);
		System.arraycopy(titleRight.pixels, 0, flameRight.pixels, 0, 33920);

		flameGradientRed = new int[256];

		for (int i = 0; i < 64; i++) {
			flameGradientRed[i] = i * 0x40000;
		}

		for (int i = 0; i < 64; i++) {
			flameGradientRed[i + 64] = 0xFF0000 + i * 0x400;
		}

		for (int i = 0; i < 64; i++) {
			flameGradientRed[i + 128] = 0xFFFF00 + i * 0x4;
		}

		for (int i = 0; i < 64; i++) {
			flameGradientRed[i + 192] = 0xFFFFFF;
		}

		flameGradientGreen = new int[256];

		for (int i = 0; i < 64; i++) {
			flameGradientGreen[i] = i * 0x400;
		}

		for (int i = 0; i < 64; i++) {
			flameGradientGreen[i + 64] = 0xFF00 + i * 4;
		}

		for (int i = 0; i < 64; i++) {
			flameGradientGreen[i + 128] = 0xFFFF + i * 0x40000;
		}

		for (int i = 0; i < 64; i++) {
			flameGradientGreen[i + 192] = 0xFFFFFF;
		}

		flameGradientViolet = new int[256];

		for (int i = 0; i < 64; i++) {
			flameGradientViolet[i] = i * 4;
		}

		for (int i = 0; i < 64; i++) {
			flameGradientViolet[i + 64] = 0xFF + i * 0x40000;
		}

		for (int i = 0; i < 64; i++) {
			flameGradientViolet[i + 128] = 0xFF00FF + i * 0x400;
		}

		for (int i = 0; i < 64; i++) {
			flameGradientViolet[i + 192] = 0xFFFFFF;
		}

		flameGradient = new int[256];
		flameBuffer1 = new int[128 * 256];
		flameBuffer2 = new int[128 * 256];
		updateFlameDissolve(null);
		flameIntensity = new int[128 * 256];
		flameIntensityBuffer = new int[128 * 256];

		drawProgress("Connecting to fileserver", 10);

		if (!flamesActive) {
			runFlames = true;
			flamesActive = true;
			startThread(this, 2);
		}
	}

	@Override
	public final void run() {
		if (runFlames) {
			flameThreadActive = true;
			try {
				long lastTime = System.currentTimeMillis();
				int i = 0;
				int interval = 20;

				while (flamesActive) {
					updateFlames();
					updateFlames();
					drawFlames();

					if (++i > 10) {
						long time = System.currentTimeMillis();
						int delay = (int) ((time - lastTime) / 10) - interval;
						interval = 40 - delay;

						if (interval < 5) {
							interval = 5;
						}

						i = 0;
						lastTime = time;
					}

					try {
						Thread.sleep((long) interval);
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
			flameThreadActive = false;
		} else {
			super.run();
		}
	}

	public final void unloadTitle() {
		flamesActive = false;
		while (flameThreadActive) {
			flamesActive = false;
			try {
				Thread.sleep(50L);
			} catch (Exception exception) {
				/* empty */
			}
		}
		titlebox = null;
		titlebutton = null;
		runes = null;
		flameGradient = null;
		flameGradientRed = null;
		flameGradientGreen = null;
		flameGradientViolet = null;
		flameBuffer1 = null;
		flameBuffer2 = null;
		flameIntensity = null;
		flameIntensityBuffer = null;
		flameLeft = null;
		flameRight = null;
	}

	public final void updateFlameDissolve(IndexedSprite image) {
		int flameHeight = 256;

		for (int n = 0; n < flameBuffer1.length; n++) {
			flameBuffer1[n] = 0;
		}

		// toss 5000 disolves into that map yo
		for (int n = 0; n < 5000; n++) {
			int i = (int) (Math.random() * 128.0 * (double) flameHeight);
			flameBuffer1[i] = (int) (Math.random() * 256.0);
		}

		// blur dissolve map in 20 iterations
		for (int n = 0; n < 20; n++) {
			for (int y = 1; y < flameHeight - 1; y++) {
				for (int x = 1; x < 127; x++) {
					int i = x + (y << 7);
					flameBuffer2[i] = (flameBuffer1[i - 1] + flameBuffer1[i + 1] + flameBuffer1[i - 128] + flameBuffer1[i + 128]) / 4;
				}
			}

			int[] last = flameBuffer1;
			flameBuffer1 = flameBuffer2;
			flameBuffer2 = last;
		}

		if (image != null) {
			int off = 0;
			for (int y = 0; y < image.height; y++) {
				for (int x = 0; x < image.width; x++) {
					if (image.data[off++] != 0) {
						int x0 = x + 16 + image.clipX;
						int y0 = y + 16 + image.clipY;
						flameBuffer1[x0 + (y0 << 7)] = 0;
					}
				}
			}
		}
	}

	public final void updateFlames() {
		int flameHeight = 256;

		// generate more flame
		for (int x = 10; x < 117; x++) {
			int n = (int) (Math.random() * 100.0);
			if (n < 50) {
				flameIntensity[x + (flameHeight - 2 << 7)] = 0xFF;
			}
		}

		// throws some sparkles in there
		for (int n = 0; n < 100; n++) {
			int x = (int) (Math.random() * 124.0) + 2;
			int y = (int) (Math.random() * 128.0) + 128;
			int i = x + (y << 7);
			flameIntensity[i] = 192;
		}

		// blur flame intensity
		for (int y = 1; y < flameHeight - 1; y++) {
			for (int x = 1; x < 127; x++) {
				int i = x + (y << 7);
				flameIntensityBuffer[i] = (flameIntensity[i - 1] + flameIntensity[i + 1] + flameIntensity[i - 128] + flameIntensity[i + 128]) / 4;
			}
		}

		flameOffset += 128;

		if (flameOffset > flameBuffer1.length) {
			flameOffset -= flameBuffer1.length;
			updateFlameDissolve(runes[(int) (Math.random() * 12.0)]);
		}

		// shift flame pixels up and dissolve
		for (int y = 1; y < flameHeight - 1; y++) {
			for (int x = 1; x < 127; x++) {
				int i = x + (y << 7);
				int n = flameIntensityBuffer[i + 128] - (flameBuffer1[i + flameOffset & flameBuffer1.length - 1] / 5);

				if (n < 0) {
					n = 0;
				}

				flameIntensity[i] = n;
			}
		}

		for (int y = 0; y < flameHeight - 1; y++) {
			flameShiftX[y] = flameShiftX[y + 1];
		}

		flameShiftX[flameHeight - 1] = (int) (Math.sin((double) cycle / 14.0) * 16.0 + Math.sin((double) cycle / 15.0) * 14.0 + Math.sin((double) cycle / 16.0) * 12.0);

		if (flameCycle1 > 0) {
			flameCycle1 -= 2;
		}

		if (flameCycle2 > 0) {
			flameCycle2 -= 2;
		}

		if (flameCycle1 == 0 && flameCycle2 == 0) {
			int i = (int) (Math.random() * 2000.0);

			if (i == 0) {
				flameCycle1 = 1024;
			}

			if (i == 1) {
				flameCycle2 = 1024;
			}
		}
	}

	public final int getColorMixed(int a, int b, int alpha) {
		int srcA = 256 - alpha;
		return ((b & 0xff00ff) * srcA + (a & 0xff00ff) * alpha & 0xff00ff00) + ((b & 0xff00) * srcA + (a & 0xff00) * alpha & 0xff0000) >> 8;
	}

	public final void drawFlames() {
		int flameHeight = 256;

		if (flameCycle1 > 0) {
			for (int n = 0; n < 256; n++) {
				if (flameCycle1 > 768) {
					flameGradient[n] = getColorMixed(flameGradientGreen[n], flameGradientRed[n], 1024 - flameCycle1);
				} else if (flameCycle1 > 256) {
					flameGradient[n] = flameGradientGreen[n];
				} else {
					flameGradient[n] = getColorMixed(flameGradientRed[n], flameGradientGreen[n], 256 - flameCycle1);
				}
			}
		} else if (flameCycle2 > 0) {
			for (int n = 0; n < 256; n++) {
				if (flameCycle2 > 768) {
					flameGradient[n] = getColorMixed(flameGradientViolet[n], flameGradientRed[n], 1024 - flameCycle2);
				} else if (flameCycle2 > 256) {
					flameGradient[n] = flameGradientViolet[n];
				} else {
					flameGradient[n] = getColorMixed(flameGradientRed[n], flameGradientViolet[n], 256 - flameCycle2);
				}
			}
		} else {
			System.arraycopy(flameGradientRed, 0, flameGradient, 0, 256);
		}

		System.arraycopy(flameLeft.pixels, 0, titleLeft.pixels, 0, 33920);

		int srcOff = 0;
		int dstOff = 0 + (9 * 128);

		for (int y = 1; y < flameHeight - 1; y++) {
			int shiftx = flameShiftX[y] * (flameHeight - y) / flameHeight;
			int dstStep = shiftx + 22;

			if (dstStep < 0) {
				dstStep = 0;
			}

			srcOff += dstStep;

			for (int x = dstStep; x < 128; x++) {
				int src = flameIntensity[srcOff++];
				if (src != 0) {
					int opacity = src;
					int alpha = 0x100 - src;
					src = flameGradient[src];
					int dst = titleLeft.pixels[dstOff];
					titleLeft.pixels[dstOff++] = ((((src & 0xff00ff) * opacity + (dst & 0xff00ff) * alpha) & ~0xff00ff) + (((src & 0xff00) * opacity + (dst & 0xff00) * alpha) & 0xff0000)) >> 8;
				} else {
					dstOff++;
				}
			}
			dstOff += dstStep;
		}
		titleLeft.draw(graphics, 0, 0);

		System.arraycopy(flameRight.pixels, 0, titleRight.pixels, 0, 33920);

		srcOff = 0;
		dstOff = 24 + (9 * 128);

		for (int y = 1; y < flameHeight - 1; y++) {
			int shiftX = flameShiftX[y] * (flameHeight - y) / flameHeight;
			int dstStep = 103 - shiftX;

			dstOff += shiftX;

			for (int n = 0; n < dstStep; n++) {
				int src = flameIntensity[srcOff++];
				if (src != 0) {
					int opacity = src;
					int alpha = 0x100 - src;
					src = flameGradient[src];
					int dst = titleRight.pixels[dstOff];
					titleRight.pixels[dstOff++] = ((((src & 0xff00ff) * opacity + (dst & 0xff00ff) * alpha) & ~0xff00ff) + (((src & 0xff00) * opacity + (dst & 0xff00) * alpha) & 0xff0000)) >> 8;
				} else {
					dstOff++;
				}
			}
			srcOff += 128 - dstStep;
			dstOff += 128 - dstStep - shiftX;
		}

		titleRight.draw(graphics, 661, 0);
	}

	public final void drawTitle() {
		loadTitle();
		titleCenter.prepare();
		titlebox.draw(0, 0);

		int w = 360;
		int h = 200;

		if (titleState == 0) {
			int y = h / 2 - 80;

			fontBold.drawTaggableCentered("Welcome to the RuneScape-2 BETA test.", w / 2, y, 0xFFFF00, true);
			y += 15;
			y += 15;

			fontSmall.drawTaggableCentered("Please note this test version of the game is provided for ", w / 2, y, 0xFFFFFF, true);
			y += 15;

			fontSmall.drawTaggableCentered("testing/preview purposes only. As such please bear in mind that:", w / 2, y, 0xFFFFFF, true);
			y += 15;
			y += 10;

			fontSmall.drawTaggableCentered("a) Everything you do/gain here will be forgotten when the beta ends.", w / 2, y, 0xFFFFFF, true);
			y += 15;

			fontSmall.drawTaggableCentered("b) No customer support is available for the beta.", w / 2, y, 0xFFFFFF, true);
			y += 15;

			fontSmall.drawTaggableCentered("c) The beta may be incomplete/buggy, we're still working on it.", w / 2, y, 0xFFFFFF, true);
			y += 15;

			fontSmall.drawTaggableCentered("d) The beta may be totally unavailable at times.", w / 2, y, 0xFFFFFF, true);
			y += 15;

			int x = w / 2;
			y = h / 2 + 65;

			titlebutton.draw(x - 73, y - 20);
			fontBold.drawTaggableCentered("Click here to login.", w / 2, y + 5, 0xFFFFFF, true);
		}

		if (titleState == 1 || titleState == 2) {
			int y = h / 2 - 50;

			if (loginMessage1.length() > 0) {
				fontBold.drawTaggableCentered(loginMessage1, w / 2, y - 15, 0xFFFF00, true);
				fontBold.drawTaggableCentered(loginMessage2, w / 2, y, 0xFFFF00, true);
				y += 30;
			} else {
				fontBold.drawTaggableCentered(loginMessage2, w / 2, y - 7, 0xFFFF00, true);
				y += 30;
			}

			fontBold.drawTaggable(("Username: " + loginUsername + ((loginFocusedLine == 0 & cycle % 40 < 20) ? "@yel@|" : "")), w / 2 - 90, y, 0xFFFFFF, true);
			y += 15;

			fontBold.drawTaggable(("Password: " + StringUtil.toAsterisks(loginPassword) + (loginFocusedLine == 1 & cycle % 40 < 20 ? "@yel@|" : "")), w / 2 - 88, y, 0xFFFFFF, true);
			y += 15;

			if (titleState == 1) {
				fontBold.drawTaggable(("Confirm Password: " + StringUtil.toAsterisks(loginPasswordConfirm) + (loginFocusedLine == 2 & cycle % 40 < 20 ? "@yel@|" : "")), w / 2 - 143, y, 0xFFFFFF, true);
			}

			int x = w / 2 - 80;
			y = h / 2 + 40;

			titlebutton.draw(x - 73, y - 20);

			if (titleState == 1) {
				fontBold.drawTaggableCentered("Create", x, y + 5, 0xFFFFFF, true);
			} else {
				fontBold.drawTaggableCentered("Login", x, y + 5, 0xFFFFFF, true);
			}

			x = w / 2 + 80;
			titlebutton.draw(x - 73, y - 20);
			fontBold.drawTaggableCentered("Cancel", x, y + 5, 0xFFFFFF, true);
		}

		titleCenter.draw(graphics, 214, 186);
		if (redraw) {
			redraw = false;
			titleTop.draw(graphics, 128, 0);
			titleBottom.draw(graphics, 214, 386);
			titleBottomLeft.draw(graphics, 0, 265);
			titleBottomRight.draw(graphics, 574, 265);
			titleLeftSpace.draw(graphics, 128, 186);
			titleRightSpace.draw(graphics, 574, 186);
		}
	}

	public final void login(String username, String password, boolean reconnect) {
		try {
			if (!reconnect) {
				loginMessage1 = "";
				loginMessage2 = "Connecting to server...";
				drawTitle();
			}

			stream = new BufferedStream(openSocket(portoff + 43594));
			stream.read(in.data, 0, 8);
			in.position = 0;
			sessionId = in.getLong();

			int[] seed = new int[4];
			seed[0] = (int) (Math.random() * 99999999);
			seed[1] = (int) (Math.random() * 99999999);
			seed[2] = (int) (sessionId >> 32);
			seed[3] = (int) (sessionId);

			out.position = 0;
			out.putByte(10); // this motherfucker
			out.putInt(seed[0]);
			out.putInt(seed[1]);
			out.putInt(seed[2]);
			out.putInt(seed[3]);
			out.putInt(8008135);
			out.putString(username);
			out.putString(password);
			out.encode(RSA_EXPONENT, RSA_MODULUS);

			login.position = 0;
			login.putByte(reconnect ? 18 : 16);
			login.putByte(out.position + 32);

			for (int n = 0; n < 8; n++) {
				login.putInt(archiveCRC[n]);
			}

			login.putBytes((out.data), 0, (out.position));

			out.isaac = new IsaacRandom(seed);

			for (int n = 0; n < 4; n++) {
				seed[n] += 50;
			}

			isaac = new IsaacRandom(seed);
			stream.write(login.data, 0, login.position);

			int response = stream.read();

			if (response == 1) {
				try {
					Thread.sleep(2000L);
				} catch (Exception e) {
				}
				login(username, password, reconnect);
			} else if (response == 2) {
				ingame = true;
				out.position = 0;
				in.position = 0;
				packetType = -1;
				packetSize = 0;
				netIdleCycles = 0;
				systemUpdate = 0;

				if (!reconnect) {
					idleCycles = 0;

					for (int n = 0; n < 100; n++) {
						chatMessage[n] = null;
					}

					selectedObject = false;
					selectedSpell = false;
					sceneState = 0;
					lastSceneLevel = -1;
					playerCount = 0;
					entityCount = 0;

					for (int n = 0; n < MAX_ENTITY_COUNT; n++) {
						players[n] = null;
						playerBuffers[n] = null;
					}

					for (int n = 0; n < MAX_ENTITY_COUNT; n++) {
						npcs[n] = null;
					}

					localPlayer = players[LOCALPLAYER_INDEX] = new Player();
					projectiles.clear();
					spotanims.clear();
					temporaryLocs.clear();

					for (int level = 0; level < 4; level++) {
						for (int x = 0; x < 104; x++) {
							for (int z = 0; z < 104; z++) {
								levelObjects[level][x][z] = null;
							}
						}
					}

					spawntLocs = new LinkedQueue();
					friendCount = 0;
					chatWidgetIndex = -1;
					viewportWidgetIndex = -1;
					sidebarWidgetIndex = -1;
					chatContinuingDialogue = false;
					selectedTab = 3;
					chatShowTransferInput = false;
					optionMenuVisible = false;
					chatShowDialogueInput = false;
					inMultizone = 0;
					characterDesignIsMale = true;
					resetCharacterDesign();
					for (int n = 0; n < 5; n++) {
						characterDesignColors[n] = 0;
					}
					loadIngame();
				}
			} else if (response == 3) {
				loginMessage1 = "";
				loginMessage2 = "Invalid username or password.";
			} else if (response == 4) {
				loginMessage1 = "Your account has been disabled.";
				loginMessage2 = "Please check your message-centre for details.";
			} else if (response == 5) {
				loginMessage1 = "Your account is already logged in.";
				loginMessage2 = "Try again in 60 secs...";
			} else if (response == 6) {
				loginMessage1 = "RuneScape has been updated!";
				loginMessage2 = "Please reload this page.";
			} else if (response == 7) {
				loginMessage1 = "This world is full.";
				loginMessage2 = "Please use a different world.";
			} else if (response == 8) {
				loginMessage1 = "Unable to connect.";
				loginMessage2 = "Login server offline.";
			} else if (response == 9) {
				loginMessage1 = "Login limit exceeded.";
				loginMessage2 = "Too many connections from your address.";
			} else if (response == 10) {
				loginMessage1 = "Unable to connect.";
				loginMessage2 = "Bad session id.";
			} else if (response == 11) {
				loginMessage1 = "Unable to connect.";
				loginMessage2 = "Login server rejected session.";
			} else if (response == 12) {
				loginMessage1 = "You need a members account to beta-test";
				loginMessage2 = "Please subscribe, or play RS1 instead";
			} else if (response == 13) {
				loginMessage1 = "Could not complete login";
				loginMessage2 = "Please try using a different world";
			} else if (response == 14) {
				loginMessage1 = "The server is being updated";
				loginMessage2 = "Please wait 1 minute and try again";
			} else if (response == -1) {
				throw new EOFException();
			}
		} catch (IOException e) {
			loginMessage1 = "";
			loginMessage2 = "Error connecting to server.";
		}
	}

	public final void register(int uid, String user, String pass, int i_141_) {
		try {
			loginMessage1 = "";
			loginMessage2 = "Connecting to server...";

			drawTitle();

			stream = new BufferedStream(openSocket(portoff + 43594));
			stream.read(in.data, 0, 8);

			in.position = 0;
			sessionId = in.getLong();

			out.position = 0;
			out.putByte(10);
			out.putInt((int) (Math.random() * 99999999));
			out.putInt((int) (Math.random() * 99999999));
			out.putLong(sessionId);
			out.putInt(uid);
			out.putString(user);
			out.putString(pass);
			out.encode(RSA_EXPONENT, RSA_MODULUS);

			login.position = 0;
			login.putByte(17);
			login.putByte(out.position);
			login.putBytes((out.data), 0, out.position);

			stream.write(login.data, 0, login.position);

			int response = stream.read();

			if (response == 1) {
				try {
					Thread.sleep(2000L);
				} catch (Exception exception) {
					/* empty */
				}
				register(uid, user, pass, 0);
			} else if (response == 2) {
				loginMessage1 = "Username already taken.";
				loginMessage2 = "Please choose a different name.";
			} else if (response == 3) {
				titleState = 2;
				login(loginUsername, loginPassword, false);
			}
		} catch (IOException e) {
			loginMessage1 = "";
			loginMessage2 = "Error connecting to server.";
		}
	}

	@Override
	public void shutdown() {
		try {
			if (stream != null) {
				stream.close();
			}
		} catch (Exception exception) {
			/* empty */
		}
		stream = null;
		Signlink.midi = null;
		out = null;
		login = null;
		in = null;
		mapIndices = null;
		mapLandData = null;
		mapLocData = null;
		levelHeightMaps = null;
		levelRenderFlags = null;
		graph = null;
		collisions = null;
		pathWaypoint = null;
		pathDistance = null;
		waypointX = null;
		waypointY = null;
		tmpTexels = null;
		sidebar = null;
		maparea = null;
		viewport = null;
		chatarea = null;
		chatsettings = null;
		sideiconsBottom = null;
		sideiconsTop = null;
		backleft1 = null;
		backleft2 = null;
		backright1 = null;
		backright2 = null;
		backtop1 = null;
		backtop2 = null;
		backvmid1 = null;
		backvmid2 = null;
		backvmid3 = null;
		backhmid2 = null;
		invback = null;
		mapback = null;
		chatback = null;
		backbase1 = null;
		backbase2 = null;
		backhmid1 = null;
		sideicons1 = null;
		sideicons2 = null;
		redstone1 = null;
		redstone2 = null;
		redstone3 = null;
		redstone1h = null;
		redstone2h = null;
		redstone1v = null;
		redstone2v = null;
		redstone3v = null;
		redstone1hv = null;
		redstone2hv = null;
		compass = null;
		hitmarks = null;
		headicons = null;
		crosses = null;
		mapdot1 = null;
		mapdot2 = null;
		mapdot3 = null;
		mapscenes = null;
		mapfunctions = null;
		tileCycle = null;
		players = null;
		playerIndices = null;
		entityUpdateIndices = null;
		playerBuffers = null;
		deadEntityIndices = null;
		npcs = null;
		npcIndices = null;
		levelObjects = null;
		spawntLocs = null;
		temporaryLocs = null;
		projectiles = null;
		spotanims = null;
		animatedLocations = null;
		optionParamB = null;
		optionParamC = null;
		optionType = null;
		optionParamA = null;
		options = null;
		variables = null;
		minimapFunctionX = null;
		minimapFunctionY = null;
		minimapFunctions = null;
		minimap = null;
		friendName = null;
		friendWorld = null;
		titleLeft = null;
		titleRight = null;
		titleTop = null;
		titleBottom = null;
		titleCenter = null;
		titleBottomLeft = null;
		titleBottomRight = null;
		titleLeftSpace = null;
		titleRightSpace = null;
		unloadTitle();
		LocationInfo.unload();
		NPCInfo.unload();
		ObjectInfo.unload();
		Floor.instances = null;
		Identikit.instance = null;
		Widget.instances = null;
		Animation.instance = null;
		SpotAnimation.instance = null;
		SpotAnimation.models = null;
		Varp.instance = null;
		Player.models = null;
		Graphics3D.unload();
		SceneGraph.unload();
		Model.unload();
		AnimationTransform.instance = null;
		AnimationFrame.instance = null;
		System.gc();
	}

	public final void clearCaches() {
		LocationInfo.models.clear();
		LocationInfo.builtModels.clear();
		NPCInfo.models.clear();
		ObjectInfo.models.clear();
		ObjectInfo.sprites.clear();
		Player.models.clear();
		SpotAnimation.models.clear();
	}

	public final void logout() {
		try {
			if (stream != null) {
				stream.close();
			}
		} catch (Exception exception) {
			/* empty */
		}

		stream = null;
		ingame = false;
		titleState = 0;

		loginUsername = "";
		loginPassword = "";
		loginPasswordConfirm = "";

		clearCaches();
		graph.reset();

		for (int p = 0; p < 4; p++) {
			collisions[p].reset();
		}

		System.gc();

		if (midiPlaying) {
			Signlink.midi = null;
		}

		midi = null;
	}

	public final void reconnect() {
		viewport.prepare();
		fontNormal.drawCentered("Connection lost", 257, 144, 0);
		fontNormal.drawCentered("Connection lost", 0x100, 143, 0xFFFFFF);
		fontNormal.drawCentered("Please wait - attempting to reestablish", 257, 159, 0);
		fontNormal.drawCentered("Please wait - attempting to reestablish", 0x100, 158, 0xFFFFFF);
		viewport.draw(graphics, 8, 11);

		ingame = false;
		login(loginUsername, loginPassword, true);

		if (!ingame) {
			logout();
		}
	}

	public final int getInterfaceParent(int index) {
		int parent = Widget.instances[index].parent;

		if (parent == index) {
			return index;
		}

		if (parent >= 0) {
			return getInterfaceParent(parent);
		}

		return index;
	}

	public void updateIdleCycle() {
		idleCycles++;

		if (idleCycles > 4500) {
			idleCycles -= 500;
			out.putOpcode(Packet.IDLE);
		}
	}

	public void updateHeartbeat() {
		netHeartbeatCycle++;

		if (netHeartbeatCycle > 50) {
			out.putOpcode(223);
			netHeartbeatCycle = 0;
		}
	}

	public void flushConnection() {
		if (stream == null || out.position <= 0) {
			return;
		}

		try {
			stream.write(out.data, 0, out.position);
			outBytes += out.position;
			out.position = 0;
			netHeartbeatCycle = 0;
		} catch (IOException e) {
			reconnect();
			logger.log(Level.WARNING, "IO Error flushing connection", e);
		} catch (Exception e) {
			logout();
			logger.log(Level.WARNING, "Error flushing connection", e);
		}
	}

	public final void updateGame() {
		if (systemUpdate > 1) {
			systemUpdate--;
		}

		long time = System.currentTimeMillis();

		// purposely avoided using cycle count
		if (time >= nextRateUpdate) {
			inRate = inBytes;
			inBytes = 0;

			outRate = outBytes;
			outBytes = 0;

			nextRateUpdate = time + 1000;
		}

		updateConnection();
		updatePlayers();
		updateNPCs();
		updateEntityVoices();
		updateTemporaryLocs();

		sceneDelta++;

		// Organized
		updateCross();
		updateSelectCycle();
		updateLandscapeClick();
		updateOptionMenu();
		updateMinimapInput();
		updateSidebarTabInput();
		updateChatSettingInput();

		if (dragButton == 1 || mouseButton == 1) {
			dragCycle++;
		}

		if (sceneState == 2) {
			updateOrbitCamera();
		}

		updateKeyboard();
		updateIdleCycle();
		updateCameraAnticheat();
		updateHeartbeat();

		flushConnection();
	}

	public void updateConnection() {
		for (int n = 0; n < 5; n++) {
			if (!readStream()) {
				break;
			}
		}

		netIdleCycles++;

		if (netIdleCycles > 750) {
			reconnect();
		}
	}

	public void updateEntityVoices() {
		for (int i = -1; i < playerCount; i++) {
			int index;

			if (i == -1) {
				index = LOCALPLAYER_INDEX;
			} else {
				index = playerIndices[i];
			}

			Player p = players[index];

			if (p == null) {
				continue;
			}

			if (p.spokenLife > 0) {
				p.spokenLife--;
				if (p.spokenLife == 0) {
					p.spoken = null;
				}
			}
		}

		for (int n = 0; n < entityCount; n++) {
			NPC npc = npcs[npcIndices[n]];

			if (npc.spokenLife > 0) {
				npc.spokenLife--;
				if (npc.spokenLife == 0) {
					npc.spoken = null;
				}
			}
		}
	}

	public void updateTemporaryLocs() {
		if (sceneState == 2) {
			for (TemporaryLocation l = (TemporaryLocation) temporaryLocs.peekLast(); l != null; l = (TemporaryLocation) temporaryLocs.getPrevious()) {
				if (cycle >= l.lastCycle) {
					addLoc(l.locIndex, l.level, l.tileX, l.tileZ, l.type, l.classtype, l.rotation);
					l.unlink();
				}
			}
		}
	}

	public void updateMinimapInput() {
		if (mouseButton == 1) {
			int x = clickX - 21 - 561;
			int y = clickY - 9 - 5;

			if (x >= 0 && y >= 0 && x < 146 && y < 151) {
				x -= 73;
				y -= 75;

				int yawsin = Graphics3D.sin[cameraOrbitYaw];
				int yawcos = Graphics3D.cos[cameraOrbitYaw];

				int rotatedX = y * yawsin + x * yawcos >> 11;
				int rotatedY = y * yawcos - x * yawsin >> 11;

				int dstTileX = localPlayer.sceneX + rotatedX >> 7;
				int dstTileZ = localPlayer.sceneZ - rotatedY >> 7; // y -> z

				moveTo(localPlayer.pathX[0], localPlayer.pathY[0], dstTileX, dstTileZ, 0, 0, 0, 0, 0, true);
			}
		}
	}

	public void updateSidebarTabInput() {
		if (mouseButton == 1) {
			if (clickX >= 549 && clickX <= 583 && clickY >= 195 && clickY < 231) {
				sidebarRedraw = true;
				selectedTab = 0;
				sidebarRedrawIcons = true;
			}
			if (clickX >= 579 && clickX <= 609 && clickY >= 194 && clickY < 231) {
				sidebarRedraw = true;
				selectedTab = 1;
				sidebarRedrawIcons = true;
			}
			if (clickX >= 607 && clickX <= 637 && clickY >= 194 && clickY < 231) {
				sidebarRedraw = true;
				selectedTab = 2;
				sidebarRedrawIcons = true;
			}
			if (clickX >= 635 && clickX <= 679 && clickY >= 194 && clickY < 229) {
				sidebarRedraw = true;
				selectedTab = 3;
				sidebarRedrawIcons = true;
			}
			if (clickX >= 676 && clickX <= 706 && clickY >= 194 && clickY < 231) {
				sidebarRedraw = true;
				selectedTab = 4;
				sidebarRedrawIcons = true;
			}
			if (clickX >= 704 && clickX <= 734 && clickY >= 194 && clickY < 231) {
				sidebarRedraw = true;
				selectedTab = 5;
				sidebarRedrawIcons = true;
			}
			if (clickX >= 732 && clickX <= 766 && clickY >= 195 && clickY < 231) {
				sidebarRedraw = true;
				selectedTab = 6;
				sidebarRedrawIcons = true;
			}
			if (clickX >= 582 && clickX <= 612 && clickY >= 492 && clickY < 529) {
				sidebarRedraw = true;
				selectedTab = 8;
				sidebarRedrawIcons = true;
			}
			if (clickX >= 609 && clickX <= 639 && clickY >= 492 && clickY < 529) {
				sidebarRedraw = true;
				selectedTab = 9;
				sidebarRedrawIcons = true;
			}
			if (clickX >= 637 && clickX <= 681 && clickY >= 493 && clickY < 528) {
				sidebarRedraw = true;
				selectedTab = 10;
				sidebarRedrawIcons = true;
			}
			if (clickX >= 679 && clickX <= 709 && clickY >= 492 && clickY < 529) {
				sidebarRedraw = true;
				selectedTab = 11;
				sidebarRedrawIcons = true;
			}
			if (clickX >= 706 && clickX <= 737 && clickY >= 491 && clickY < 529) {
				sidebarRedraw = true;
				selectedTab = 12;
				sidebarRedrawIcons = true;
			}
		}
	}

	public void updateCross() {
		if (crossType != 0) {
			crossCycle += 20;

			// finish cross after 400ms
			if (crossCycle >= 400) {
				crossType = 0;
			}
		}
	}

	public void updateSelectCycle() {
		if (selectedArea != 0) {
			selectedCycle++;

			// redraw after 15 ticks (300ms)
			if (selectedCycle >= 15) {
				if (selectedArea == 2) {
					sidebarRedraw = true;
				}

				if (selectedArea == 3) {
					chatRedraw = true;
				}
				selectedArea = 0;
			}
		}
	}

	public void updateLandscapeClick() {
		if (Scene.clickedTileX != -1) {
			int tileX = Scene.clickedTileX;
			int tileZ = Scene.clickedTileZ;
			boolean canMove = moveTo(localPlayer.pathX[0], localPlayer.pathY[0], tileX, tileZ, 0, 0, 0, 0, 0, true);
			Scene.clickedTileX = -1;

			if (canMove) {
				crossX = clickX;
				crossY = clickY;
				crossType = 1;
				crossCycle = 0;
			}
		}
	}

	public void updateChatSettingInput() {
		if (mouseButton == 1) {
			if (clickX >= 8 && clickX <= 108 && clickY >= 490 && clickY <= 522) {
				chatPublicSetting = (chatPublicSetting + 1) % 3;
				chatRedrawSettings = true;
				chatRedraw = true;
				out.putOpcode(173);
				out.putByte(chatPublicSetting);
				out.putByte(chatPrivateSetting);
				out.putByte(chatTradeDuelSetting);
			}
			if (clickX >= 137 && clickX <= 237 && clickY >= 490 && clickY <= 522) {
				chatPrivateSetting = (chatPrivateSetting + 1) % 3;
				chatRedrawSettings = true;
				chatRedraw = true;
				out.putOpcode(173);
				out.putByte(chatPublicSetting);
				out.putByte(chatPrivateSetting);
				out.putByte(chatTradeDuelSetting);
			}
			if (clickX >= 275 && clickX <= 375 && clickY >= 490 && clickY <= 522) {
				chatTradeDuelSetting = (chatTradeDuelSetting + 1) % 3;
				chatRedrawSettings = true;
				chatRedraw = true;
				out.putOpcode(173);
				out.putByte(chatPublicSetting);
				out.putByte(chatPrivateSetting);
				out.putByte(chatTradeDuelSetting);
			}
			if (clickX >= 416 && clickX <= 516 && clickY >= 490 && clickY <= 522) {
				/* empty */
			}
		}
	}

	public void updateKeyboard() {
		for (;;) {
			int key = pollKey();

			if (key == -1) {
				break;
			}

			if (chatShowDialogueInput) {
				if (key >= ' ' && key <= 'z' && chatDialogueInput.length() < 80) {
					chatDialogueInput += (char) key;
					chatRedraw = true;
				}

				if (key == KeyEvent.VK_BACK_SPACE && chatDialogueInput.length() > 0) {
					chatDialogueInput = chatDialogueInput.substring(0, chatDialogueInput.length() - 1);
					chatRedraw = true;
				}

				if (key == KeyEvent.VK_ENTER) {
					chatShowDialogueInput = false;
					chatRedraw = true;

					// add friend
					if (chatDialogueInputType == 1 && friendCount < 100) {
						chatDialogueInput = StringUtil.getFormatted(StringUtil.getSafe(chatDialogueInput));

						if (chatDialogueInput.length() > 0) {
							boolean contains = false;
							for (int n = 0; n < friendCount; n++) {
								if (friendName[n].equals(chatDialogueInput)) {
									contains = true;
									break;
								}
							}

							if (chatDialogueInput.equals(localPlayer.name)) {
								contains = true;
							}

							if (!contains) {
								friendName[friendCount] = chatDialogueInput;
								friendWorld[friendCount] = 0;
								friendCount++;
								sidebarRedraw = true;
								out.putOpcode(150);
								out.putLong(StringUtil.toBase37(chatDialogueInput));
							}
						}
					}

					// remove friend
					if (chatDialogueInputType == 2 && friendCount > 0) {
						chatDialogueInput = StringUtil.getFormatted(StringUtil.getSafe(chatDialogueInput));

						if (chatDialogueInput.length() > 0) {
							for (int n = 0; n < friendCount; n++) {
								if (friendName[n].equals(chatDialogueInput)) {
									friendCount--;
									sidebarRedraw = true;

									for (int i = n; i < friendCount; i++) {
										friendName[i] = friendName[i + 1];
										friendWorld[i] = friendWorld[i + 1];
									}

									out.putOpcode(234);
									out.putLong(StringUtil.toBase37(chatDialogueInput));
									break;
								}
							}
						}
					}

					// send message
					if (chatDialogueInputType == 3 && chatDialogueInput.length() > 0 && chatSendFriendMessageIndex >= 0 && chatSendFriendMessageIndex < friendCount) {
						out.putOpcode(12);
						out.putByte(0); // size placehold

						int start = out.position;
						out.putLong(StringUtil.toBase37(friendName[chatSendFriendMessageIndex]));
						StringBuffer.write(out, chatDialogueInput);
						out.putByteLength(out.position - start);

						chatDialogueInput = StringUtil.getPunctuated(chatDialogueInput);
						chatDialogueInput = Censor.getFiltered(chatDialogueInput);

						addMessage(6, friendName[chatSendFriendMessageIndex], chatDialogueInput);

						if (chatPrivateSetting == 2) {
							chatPrivateSetting = 1;
							chatRedrawSettings = true;
							out.putOpcode(173);
							out.putByte(chatPublicSetting);
							out.putByte(chatPrivateSetting);
							out.putByte(chatTradeDuelSetting);
						}
					}

					// add ignore
					if (chatDialogueInputType == 4 && ignoreCount < 100 && chatDialogueInput.length() > 0) {
						long name = StringUtil.toBase37(chatDialogueInput);

						boolean contains = false;
						for (int n = 0; n < ignoreCount; n++) {
							if (ignoreNameLong[n] == name) {
								contains = true;
								break;
							}
						}

						if (!contains) {
							ignoreNameLong[ignoreCount++] = name;
							sidebarRedraw = true;
							out.putOpcode(105);
							out.putLong(name);
						}
					}

					// remove ignore
					if (chatDialogueInputType == 5 && ignoreCount > 0 && chatDialogueInput.length() > 0) {
						long name = StringUtil.toBase37(chatDialogueInput);

						for (int n = 0; n < ignoreCount; n++) {
							if (ignoreNameLong[n] == name) {
								ignoreCount--;
								sidebarRedraw = true;

								for (int i = n; i < ignoreCount; i++) {
									ignoreNameLong[i] = ignoreNameLong[i + 1];
								}

								out.putOpcode(92);
								out.putLong(name);
								break;
							}
						}
					}
				}
			} else if (chatShowTransferInput) {
				if (key >= '0' && key <= '9' && chatTransferInput.length() < 10) {
					chatTransferInput += (char) key;
					chatRedraw = true;
				}

				if (key == KeyEvent.VK_BACK_SPACE && chatTransferInput.length() > 0) {
					chatTransferInput = chatTransferInput.substring(0, chatTransferInput.length() - 1);
					chatRedraw = true;
				}

				if (key == KeyEvent.VK_ENTER) {
					if (chatTransferInput.length() > 0) {
						int v = 0;
						try {
							v = Integer.parseInt(chatTransferInput);
						} catch (Exception e) {
						}
						out.putOpcode(217);
						out.putInt(v);
					}
					chatShowTransferInput = false;
					chatRedraw = true;
				}
			} else if (chatWidgetIndex == -1) {
				if (key >= ' ' && key <= 'z' && chatInput.length() < 80) {
					chatInput += (char) key;
					chatRedraw = true;
				}
				if (key == KeyEvent.VK_BACK_SPACE && chatInput.length() > 0) {
					chatInput = chatInput.substring(0, chatInput.length() - 1);
					chatRedraw = true;
				}

				if (key == KeyEvent.VK_ENTER && chatInput.length() > 0) {
					if (chatInput.startsWith("::")) {
						out.putOpcode(37);
						out.putByte(chatInput.length() - 1);
						out.putString(chatInput.substring(2));
					} else {
						int color = 0;
						int effect = 0;

						if (chatInput.startsWith("yellow:")) {
							color = 0;
							chatInput = chatInput.substring(7);
						} else if (chatInput.startsWith("red:")) {
							color = 1;
							chatInput = chatInput.substring(4);
						} else if (chatInput.startsWith("green:")) {
							color = 2;
							chatInput = chatInput.substring(6);
						} else if (chatInput.startsWith("cyan:")) {
							color = 3;
							chatInput = chatInput.substring(5);
						} else if (chatInput.startsWith("purple:")) {
							color = 4;
							chatInput = chatInput.substring(7);
						} else if (chatInput.startsWith("white:")) {
							color = 5;
							chatInput = chatInput.substring(6);
						} else if (chatInput.startsWith("flash1:")) {
							color = 6;
							chatInput = chatInput.substring(7);
						} else if (chatInput.startsWith("flash2:")) {
							color = 7;
							chatInput = chatInput.substring(7);
						} else if (chatInput.startsWith("flash3:")) {
							color = 8;
							chatInput = chatInput.substring(7);
						} else if (chatInput.startsWith("glow1:")) {
							color = 9;
							chatInput = chatInput.substring(6);
						} else if (chatInput.startsWith("glow2:")) {
							color = 10;
							chatInput = chatInput.substring(6);
						} else if (chatInput.startsWith("glow3:")) {
							color = 11;
							chatInput = chatInput.substring(6);
						} else if (chatInput.startsWith("rainbow:")) {
							color = 12;
							chatInput = chatInput.substring(8);
						}

						if (chatInput.startsWith("wave:")) {
							effect = 1;
							chatInput = chatInput.substring(5);
						} else if (chatInput.startsWith("scroll:")) {
							effect = 2;
							chatInput = chatInput.substring(7);
						}

						out.putOpcode(18);
						out.putByte(0); // size placeholder
						int start = out.position;
						out.putByte(color);
						out.putByte(effect);
						StringBuffer.write(out, chatInput);
						out.putByteLength(out.position - start);

						chatInput = StringUtil.getPunctuated(chatInput);
						chatInput = Censor.getFiltered(chatInput);

						localPlayer.spoken = chatInput;
						localPlayer.spokenColor = color;
						localPlayer.spokenEffect = effect;
						localPlayer.spokenLife = 150;

						addMessage(2, localPlayer.name, localPlayer.spoken);

						if (chatPublicSetting == 2) {
							chatPublicSetting = 1;
							chatRedrawSettings = true;
							out.putOpcode(173);
							out.putByte(chatPublicSetting);
							out.putByte(chatPrivateSetting);
							out.putByte(chatTradeDuelSetting);
						}
					}
					chatInput = "";
					chatRedraw = true;
				}
			}
		}
	}

	public void updateCameraAnticheat() {
		cameraOffsetCycle++;

		if (cameraOffsetCycle > 500) {
			cameraOffsetCycle = 0;
			int i = (int) (Math.random() * 8.0);

			if ((i & 0x1) == 1) {
				cameraOffsetX += cameraOffsetXModifier;
			}

			if ((i & 0x2) == 2) {
				cameraOffsetZ += cameraOffsetZModifier;
			}

			if ((i & 0x4) == 4) {
				cameraOffsetYaw += cameraOffsetYawModifier;
			}
		}

		if (cameraOffsetX < -50) {
			cameraOffsetXModifier = 2;
		}

		if (cameraOffsetX > 50) {
			cameraOffsetXModifier = -2;
		}

		if (cameraOffsetZ < -55) {
			cameraOffsetZModifier = 2;
		}

		if (cameraOffsetZ > 55) {
			cameraOffsetZModifier = -2;
		}

		if (cameraOffsetYaw < -40) {
			cameraOffsetYawModifier = 1;
		}

		if (cameraOffsetYaw > 40) {
			cameraOffsetYawModifier = -1;
		}
	}

	public int getTopLevel(int x, int y) {
		int top = 3;

		if (cameraPitch < 310) {
			int dstX = cameraX >> 7;
			int dstY = cameraZ >> 7;

			if ((levelRenderFlags[currentLevel][dstX][dstY] & 0x4) != 0) {
				top = currentLevel;
			}

			int dx;

			if (x > dstX) {
				dx = x - dstX;
			} else {
				dx = dstX - x;
			}

			int dy;

			if (y > dstY) {
				dy = y - dstY;
			} else {
				dy = dstY - y;
			}

			if (dx > dy) {
				int slope = (dy * 65536) / dx;
				int error = 32768;

				while (dstX != x) {
					if (dstX < x) {
						dstX++;
					} else if (dstX > x) {
						dstX--;
					}

					if ((levelRenderFlags[currentLevel][dstX][dstY] & 0x4) != 0) {
						top = currentLevel;
					}

					error += slope;

					if (error >= 65536) {
						error -= 65536;

						if (dstY < y) {
							dstY++;
						} else if (dstY > y) {
							dstY--;
						}

						if ((levelRenderFlags[currentLevel][dstX][dstY] & 0x4) != 0) {
							top = currentLevel;
						}
					}
				}
			} else {
				int slope = (dx * 65536) / dy;
				int error = 32768;

				while (dstY != y) {
					if (dstY < y) {
						dstY++;
					} else if (dstY > y) {
						dstY--;
					}

					if ((levelRenderFlags[currentLevel][dstX][dstY] & 0x4) != 0) {
						top = currentLevel;
					}

					error += slope;

					if (error >= 65536) {
						error -= 65536;

						if (dstX < x) {
							dstX++;
						} else if (dstX > x) {
							dstX--;
						}

						if ((levelRenderFlags[currentLevel][dstX][dstY] & 0x4) != 0) {
							top = currentLevel;
						}
					}
				}
			}
		}
		return top;
	}

	public void updateOrbitCamera() {
		int x = localPlayer.sceneX + cameraOffsetX;
		int z = localPlayer.sceneZ + cameraOffsetZ;

		if (cameraOrbitX - x < -500 || cameraOrbitX - x > 500 || cameraOrbitZ - z < -500 || cameraOrbitZ - z > 500) {
			cameraOrbitX = x;
			cameraOrbitZ = z;
		}

		// rate = distance / time
		if (cameraOrbitX != x) {
			cameraOrbitX += (x - cameraOrbitX) / 16;
		}

		if (cameraOrbitZ != z) {
			cameraOrbitZ += (z - cameraOrbitZ) / 16;
		}

		if (keyDown[1]) {
			cameraYawModifier += (-24 - cameraYawModifier) / 2;
		} else if (keyDown[2]) {
			cameraYawModifier += (24 - cameraYawModifier) / 2;
		} else {
			cameraYawModifier /= 2;
		}

		if (keyDown[3]) {
			cameraPitchModifier += (12 - cameraPitchModifier) / 2;
		} else if (keyDown[4]) {
			cameraPitchModifier += (-12 - cameraPitchModifier) / 2;
		} else {
			cameraPitchModifier /= 2;
		}

		cameraYaw = cameraYaw + cameraYawModifier / 2 & 0x7ff;

		cameraOrbitPitch += cameraPitchModifier / 2;
		cameraOrbitYaw = cameraYaw + cameraOffsetYaw & 0x7ff;

		if (cameraOrbitPitch < 128) {
			cameraOrbitPitch = 128;
		}

		if (cameraOrbitPitch > 383) {
			cameraOrbitPitch = 383;
		}

		int tileX = cameraOrbitX >> 7;
		int tileZ = cameraOrbitZ >> 7;
		int landY = getLandY(cameraOrbitX, cameraOrbitZ, currentLevel);
		int maxY = 0;

		if (tileX > 3 && tileZ > 3 && tileX < 100 && tileZ < 100) {
			for (int tx = tileX - 4; tx <= tileX + 4; tx++) {
				for (int ty = tileZ - 4; ty <= tileZ + 4; ty++) {
					int p = currentLevel;

					// is bridge
					if (p < 3 && ((levelRenderFlags[1][tx][ty] & 0x2) == 2)) {
						p++;
					}

					int y = landY - levelHeightMaps[p][tx][ty];

					if (y > maxY) {
						maxY = y;
					}
				}
			}
		}

		int y = maxY * 192;

		if (y > 98048) {
			y = 98048;
		}

		if (y < 32768) {
			y = 32768;
		}

		if (y > cameraMaxY) {
			cameraMaxY += (y - cameraMaxY) / 24;
		} else if (y < cameraMaxY) {
			cameraMaxY += (y - cameraMaxY) / 80;
		}
	}

	public final void drawGame() {
		if (redraw) {
			redraw = false;

			backleft1.draw(graphics, 0, 11);
			backleft2.draw(graphics, 0, 375);
			backright1.draw(graphics, 729, 5);
			backright2.draw(graphics, 752, 231);
			backtop1.draw(graphics, 0, 0);
			backtop2.draw(graphics, 561, 0);
			backvmid1.draw(graphics, 520, 11);
			backvmid2.draw(graphics, 520, 231);
			backvmid3.draw(graphics, 501, 375);
			backhmid2.draw(graphics, 0, 345);

			sidebarRedraw = true;
			sidebarRedrawIcons = true;
			chatRedraw = true;
			chatRedrawSettings = true;

			if (sceneState != 2) {
				viewport.draw(graphics, 8, 11);
				maparea.draw(graphics, 561, 5);
			}
		}

		if (sceneState == 2) {
			drawViewport();
		}

		if (optionMenuVisible && optionMenuArea == 1) {
			sidebarRedraw = true;
		}

		if (sidebarWidgetIndex != -1) {
			if (updateWidgetAnimations(sidebarWidgetIndex, sceneDelta)) {
				sidebarRedraw = true;
			}
		}

		if (selectedArea == 2) {
			sidebarRedraw = true;
		}

		if (sidebarRedraw) {
			drawSidebar();
			sidebarRedraw = false;
		}

		if (chatWidgetIndex == -1) {
			chatbox.scrollAmount = chatHeight - chatScrollAmount - 77;

			if (mouseX > 453 && mouseX < 565 && mouseY > 350) {
				updateScrollbar(chatbox, 463, 0, 77, chatHeight, mouseX - 22, mouseY - 375, false);
			}

			int scrollAmount = chatHeight - 77 - chatbox.scrollAmount;

			if (scrollAmount < 0) {
				scrollAmount = 0;
			}

			if (scrollAmount > chatHeight - 77) {
				scrollAmount = chatHeight - 77;
			}

			if (chatScrollAmount != scrollAmount) {
				chatScrollAmount = scrollAmount;
				chatRedraw = true;
			}
		}

		if (chatWidgetIndex != -1) {
			if (updateWidgetAnimations(chatWidgetIndex, sceneDelta)) {
				chatRedraw = true;
			}
		}

		if (selectedArea == 3) {
			chatRedraw = true;
		}

		if (chatRedraw) {
			drawChat();
			chatRedraw = false;
		}

		if (sceneState == 2) {
			if (lowmemory && cameraYawModifier == 0 && cycle - minimapLastUpdateCycle > 25) {
				minimapLastUpdateCycle = cycle;
				minimapDrawPhase = 1 - minimapDrawPhase;

				if (minimapDrawPhase == 0) {
					drawMinimap();
				} else {
					maparea.draw(graphics, 561, 5);
				}
			}

			if (!lowmemory) {
				drawMinimap();
				maparea.draw(graphics, 561, 5);
			}
		}

		if (sidebarRedrawIcons) {
			sidebarRedrawIcons = false;
			drawSideicons();
			viewport.prepare();
		}

		if (chatRedrawSettings) {
			chatRedrawSettings = false;
			drawChatSettings();
			viewport.prepare();
		}
		sceneDelta = 0;
	}

	public void drawSideicons() {
		sideiconsTop.prepare();
		backhmid1.draw(0, 0);
		if (sidebarWidgetIndex == -1) {
			if (selectedTab == 0) {
				redstone1.draw(29, 30);
			} else if (selectedTab == 1) {
				redstone2.draw(59, 29);
			} else if (selectedTab == 2) {
				redstone2.draw(87, 29);
			} else if (selectedTab == 3) {
				redstone3.draw(115, 29);
			} else if (selectedTab == 4) {
				redstone2h.draw(156, 29);
			} else if (selectedTab == 5) {
				redstone2h.draw(184, 29);
			} else if (selectedTab == 6) {
				redstone1h.draw(212, 30);
			}
			sideicons1.draw(39, 33);
		}
		sideiconsTop.draw(graphics, 520, 165);

		sideiconsBottom.prepare();
		backbase2.draw(0, 0);

		if (sidebarWidgetIndex == -1) {
			if (selectedTab == 7) {
				redstone1v.draw(49, 0);
			} else if (selectedTab == 8) {
				redstone2v.draw(81, 0);
			} else if (selectedTab == 9) {
				redstone2v.draw(108, 0);
			} else if (selectedTab == 10) {
				redstone3v.draw(136, 1);
			} else if (selectedTab == 11) {
				redstone2hv.draw(178, 0);
			} else if (selectedTab == 12) {
				redstone2hv.draw(205, 0);
			} else if (selectedTab == 13) {
				redstone1hv.draw(233, 0);
			}
			sideicons2.draw(83, 4);
		}
		sideiconsBottom.draw(graphics, 501, 492);
	}

	public void drawChatSettings() {
		chatsettings.prepare();
		backbase1.draw(0, 0);

		fontNormal.drawTaggableCentered("Public chat", 57, 33, 0xFFFFFF, true);

		if (chatPublicSetting == 0) {
			fontNormal.drawTaggableCentered("On", 57, 46, 0xFF00, true);
		} else if (chatPublicSetting == 1) {
			fontNormal.drawTaggableCentered("Friends", 57, 46, 0xFFFF00, true);
		} else if (chatPublicSetting == 2) {
			fontNormal.drawTaggableCentered("Off", 57, 46, 0xFF0000, true);
		}

		fontNormal.drawTaggableCentered("Private chat", 186, 33, 0xFFFFFF, true);

		if (chatPrivateSetting == 0) {
			fontNormal.drawTaggableCentered("On", 186, 46, 0xFF00, true);
		} else if (chatPrivateSetting == 1) {
			fontNormal.drawTaggableCentered("Friends", 186, 46, 0xFFFF00, true);
		} else if (chatPrivateSetting == 2) {
			fontNormal.drawTaggableCentered("Off", 186, 46, 0xFF0000, true);
		}

		fontNormal.drawTaggableCentered("Trade/duel", 326, 33, 0xFFFFFF, true);

		if (chatTradeDuelSetting == 0) {
			fontNormal.drawTaggableCentered("On", 326, 46, 0xFF00, true);
		} else if (chatTradeDuelSetting == 1) {
			fontNormal.drawTaggableCentered("Friends", 326, 46, 0xFFFF00, true);
		} else if (chatTradeDuelSetting == 2) {
			fontNormal.drawTaggableCentered("Off", 326, 46, 0xFF0000, true);
		}

		chatsettings.draw(graphics, 0, 471);
	}

	public int updateCamera(int tileX, int tileZ) {
		int landY = getLandY(cameraOrbitX, cameraOrbitZ, currentLevel);
		cameraPitch = cameraOrbitPitch;

		if (cameraMaxY / 256 > cameraPitch) {
			cameraPitch = cameraMaxY / 256;
		}

		updateCameraOrbit(cameraOrbitX, landY - 50, cameraOrbitZ, cameraOrbitYaw, cameraPitch, cameraPitch * 3 + 600);

		if ((levelRenderFlags[currentLevel][tileX][tileZ] & 0x4) != 0) {
			return currentLevel;
		}

		return getTopLevel(tileX, tileZ);
	}

	public void drawViewport() {
		drawCycle++;

		drawPlayers();
		drawNPCs();
		drawProjectiles();
		drawSpotAnimations();
		drawAnimatedLocations();

		int topLevel = updateCamera(localPlayer.sceneX >> 7, localPlayer.sceneZ >> 7);

		int startCycle = Graphics3D.cycle;
		Model.allowInput = true;
		Model.hoverCount = 0;
		Model.mouseX = mouseX - 8;
		Model.mouseY = mouseY - 11;

		Graphics2D.clear();

		Scene.mouseX = Model.mouseX;
		Scene.mouseY = Model.mouseY;

		graph.draw(cameraX, cameraY, cameraZ, cameraPitch, cameraOrbitYaw, topLevel);
		graph.clearFrameLocs();

		drawViewport2d();
		drawCross();

		if (viewportWidgetIndex != -1) {
			drawWidget(Widget.instances[viewportWidgetIndex], 0, 0, 0);
		}

		if (!optionMenuVisible) {
			updateInput();
			drawTooltip();
		} else if (optionMenuArea == 0) {
			drawOptionMenu();
		}

		updateAnimatedTextures(startCycle);
		drawMultiZone();
		drawWildyLevel();
		drawSystemUpdate();
		drawDebug();

		viewport.draw(graphics, 8, 11);
	}

	public void drawPlayers() {
		for (int n = -1; n < playerCount; n++) {
			Player p;
			int index;

			if (n == -1) {
				p = localPlayer;
				index = LOCALPLAYER_INDEX << 14;
			} else {
				p = players[playerIndices[n]];
				index = playerIndices[n] << 14;
			}

			if (p == null) {
				continue;
			}

			p.lowmemory = false;

			if ((lowmemory && playerCount > 50 || playerCount > 200) && n != -1 && (p.secondaryAnimIndex == p.animStand)) {
				p.lowmemory = true;
			}

			if (p.isVisible()) {
				int tileX = p.sceneX >> 7;
				int tileZ = p.sceneZ >> 7;

				if (tileX >= 0 && tileX < 104 && tileZ >= 0 && tileZ < 104) {
					// if we have a loc model, render our player by the loc bounds
					if (p.locModel != null && cycle >= p.locFirstCycle && cycle < p.locLastCycle) {
						p.lowmemory = false;
						p.sceneY = getLandY(p.sceneX, p.sceneZ, currentLevel);
						graph.add(p, null, p.sceneX, p.sceneY, p.sceneZ, p.locMinTileX, p.locMinTileZ, p.locMaxTileX, p.locMaxTileZ, currentLevel, p.yaw, index);
					} // render normally
					else {
						if ((p.sceneX & 0x7f) == 64 && ((p.sceneZ & 0x7f) == 64)) {
							if (tileCycle[tileX][tileZ] == drawCycle) {
								continue;
							}
							tileCycle[tileX][tileZ] = drawCycle;
						}
						p.sceneY = getLandY(p.sceneX, p.sceneZ, currentLevel);
						graph.add(p, null, p.sceneX, p.sceneY, p.sceneZ, currentLevel, p.yaw, 60, index, p.renderPadding);
					}
				}
			}
		}
	}

	public void drawNPCs() {
		for (int n = 0; n < entityCount; n++) {
			NPC npc = npcs[npcIndices[n]];
			int bitset = (npcIndices[n] << 14) + 0x20000000;

			if (npc.isValid()) {
				int x = npc.sceneX >> 7;
				int y = npc.sceneZ >> 7;

				if (x >= 0 && x < 104 && y >= 0 && y < 104) {
					if (npc.size == 1 && (npc.sceneX & 0x7f) == 64 && (npc.sceneZ & 0x7f) == 64) {
						if (tileCycle[x][y] == drawCycle) {
							continue;
						}
						tileCycle[x][y] = drawCycle;
					}
					graph.add(npc, null, npc.sceneX, getLandY(npc.sceneX, npc.sceneZ, currentLevel), npc.sceneZ, currentLevel, npc.yaw, (npc.size - 1) * 64 + 60, bitset, npc.renderPadding);
				}
			}
		}
	}

	public void drawProjectiles() {
		for (Projectile p = (Projectile) projectiles.peekLast(); p != null; p = (Projectile) projectiles.getPrevious()) {
			if (p.level != currentLevel || cycle > p.lastCycle) {
				p.unlink();
			} else if (cycle >= p.firstCycle) {
				if (p.targetIndex > 0) {
					NPC n = (npcs[p.targetIndex - 1]);
					if (n != null) {
						p.setTarget(n.sceneX, n.sceneZ, (getLandY(n.sceneX, n.sceneZ, p.level) - p.baseZ), cycle);
					}
				}

				if (p.targetIndex < 0) {
					int index = -p.targetIndex - 1;
					Player pl;

					if (index == localPlayerIndex) {
						pl = localPlayer;
					} else {
						pl = players[index];
					}

					if (pl != null) {
						p.setTarget(pl.sceneX, pl.sceneZ, (getLandY(pl.sceneX, pl.sceneZ, p.level) - p.baseZ), cycle);
					}
				}

				p.update(sceneDelta);
				graph.add(p, null, (int) p.x, (int) p.z, (int) p.y, currentLevel, p.yaw, 60, -1, 0);
			}
		}
	}

	public void drawSpotAnimations() {
		for (SpotAnimationEntity e = (SpotAnimationEntity) spotanims.peekLast(); e != null; e = (SpotAnimationEntity) spotanims.getPrevious()) {
			if (e.level != currentLevel || e.finished) {
				e.unlink();
			} else if (cycle >= e.firstCycle) {
				e.update(sceneDelta);

				if (e.finished) {
					e.unlink();
				} else {
					graph.add(e, null, e.x, e.y, e.z, e.level, 0, 60, -1, 0);
				}
			}
		}
	}

	public void drawAnimatedLocations() {
		for (AnimatedLocation l = (AnimatedLocation) animatedLocations.peekLast(); l != null; l = (AnimatedLocation) animatedLocations.getPrevious()) {
			boolean append = false;
			l.animCycle += sceneDelta;

			if (l.animFrame == -1) {
				l.animFrame = 0;
				append = true;
			}

			while (l.animCycle > (l.animation.frameDuration[l.animFrame])) {
				l.animCycle -= (l.animation.frameDuration[l.animFrame]) + 1;
				l.animFrame++;

				append = true;

				if (l.animFrame >= l.animation.frameCount) {
					l.animFrame -= l.animation.delta;

					if (l.animFrame < 0 || (l.animFrame >= l.animation.frameCount)) {
						l.unlink();
						append = false;
						break;
					}
				}
			}

			if (append) {
				int bitset = 0;

				if (l.classtype == 1) {
					bitset = graph.getWallDecorationBitset(l.tileX, l.tileZ, l.level);
				}
				if (l.classtype == 2) {
					bitset = graph.getLocationBitset(l.tileX, l.tileZ, l.level);
				}

				if (bitset == 0 || (bitset >> 14 & 0x7fff) != l.locIndex) {
					l.unlink();
				} else {
					LocationInfo c = LocationInfo.get(l.locIndex);
					int animFrame = -1;

					if (l.animFrame != -1) {
						animFrame = l.animation.primaryFrames[l.animFrame];
					}

					if (l.classtype == 2) {
						int rotation = graph.getInfo(l.tileX, l.tileZ, l.level, bitset) >> 6;
						Model m = c.getModel(10, rotation, 0, 0, 0, 0, animFrame);
						graph.setLocModel(m, l.tileX, l.tileZ, l.level);
					} else if (l.classtype == 1) {
						Model m = c.getModel(4, 0, 0, 0, 0, 0, animFrame);
						graph.setWallDecorationModel(m, l.tileX, l.tileZ, l.level);
					}
				}
			}
		}
	}

	public void drawViewport2d() {
		for (int i = -1; i < playerCount + entityCount; i++) {
			Entity e;

			if (i == -1) {
				e = localPlayer;
			} else if (i < playerCount) {
				e = players[playerIndices[i]];
			} else {
				e = npcs[npcIndices[i - playerCount]];
			}

			if (e == null) {
				continue;
			}

			if (i < playerCount) {
				Player p = (Player) e;
				if (p.headicons != 0) {
					setDrawPos(e, e.height + 15);

					if (drawX > -1) {
						int y = 28;

						for (int n = 0; n < 8; n++) {
							if ((p.headicons & 1 << n) != 0) {
								headicons[n].draw(drawX - 12, drawY - y);
								y -= 25;
							}
						}
					}
				}
			}

			if (e.spoken != null && (i >= playerCount || chatPublicSetting == 0 || chatPublicSetting == 1 && isFriend(((Player) e).name))) {
				setDrawPos(e, e.height);

				if (drawX > -1) {
					if (allowChatEffects == 0) {
						int rgb = 0xFFFF00;

						if (e.spokenColor < 6) {
							rgb = SPOKEN_COLORS[e.spokenColor];
						} else if (e.spokenColor == 6) {
							rgb = drawCycle % 20 < 10 ? 0xFF0000 : 0xFFFF00;
						} else if (e.spokenColor == 7) {
							rgb = drawCycle % 20 < 10 ? 0xFF : 0xFFFF;
						} else if (e.spokenColor == 8) {
							rgb = drawCycle % 20 < 10 ? 0xB000 : 0x80FF80;
						} else if (e.spokenColor == 9) {
							int phase = 150 - e.spokenLife;

							if (phase < 50) {
								rgb = 0xFF0000 + phase * 0x500;
							} else if (phase < 100) {
								rgb = 0xFFFF00 - (phase - 50) * 0x50000;
							} else if (phase < 150) {
								rgb = 0xFF00 + (phase - 100) * 5;
							}
						} else if (e.spokenColor == 10) {
							int phase = 150 - e.spokenLife;

							if (phase < 50) {
								rgb = 0xFF0000 + (phase * 5);
							} else if (phase < 100) {
								rgb = 0xFF00FF - (phase - 50) * 0x50000;
							} else if (phase < 150) {
								rgb = (0xFF + (phase - 100) * 0x50000 - (phase - 100) * 5);
							}
						} else if (e.spokenColor == 11) {
							int phase = 150 - e.spokenLife;

							if (phase < 50) {
								rgb = 0xFFFFFF - phase * 0x50005;
							} else if (phase < 100) {
								rgb = 0xFF00 + (phase - 50) * 0x50005;
							} else if (phase < 150) {
								rgb = 0xFFFFFF - (phase - 100) * 0x50000;
							}
						} else if (e.spokenColor == 12) {
							rgb = (int) (Math.random() * 0xFFFFFF);
						}

						if (e.spokenEffect == 0) { // colored
							fontBold.drawCentered(e.spoken, drawX, drawY + 1, 0);
							fontBold.drawCentered(e.spoken, drawX, drawY, rgb);
						} else if (e.spokenEffect == 1) { // wavy
							fontBold.drawCenteredWave(e.spoken, drawX, drawY + 1, 0, drawCycle);
							fontBold.drawCenteredWave(e.spoken, drawX, drawY, rgb, drawCycle);
						} else if (e.spokenEffect == 2) { // scroll
							int w = fontBold.stringWidth(e.spoken);
							int x = ((150 - e.spokenLife) * (w + 100)) / 150;

							Graphics2D.setBounds(drawX - 50, 0, drawX + 50, 334);
							fontBold.draw(e.spoken, drawX + 50 - x, drawY + 1, 0);
							fontBold.draw(e.spoken, drawX + 50 - x, drawY, rgb);
							Graphics2D.resetBounds();
						}
					} else {
						fontBold.drawCentered(e.spoken, drawX, drawY + 1, 0);
						fontBold.drawCentered(e.spoken, drawX, drawY, 0xFFFF00);
					}
				}
			}

			if (e.lastCombatCycle > cycle + 100) {
				setDrawPos(e, e.height + 15);
				if (drawX > -1) {
					int w = (e.currentHealth * 30 / e.maxHealth);

					if (w > 30) {
						w = 30;
					}

					Graphics2D.fillRect(drawX - 15, drawY - 3, w, 5, 0xFF00);
					Graphics2D.fillRect(drawX - 15 + w, drawY - 3, 30 - w, 5, 0xFF0000);
				}
			}

			if (e.lastCombatCycle > cycle + 330) {
				setDrawPos(e, e.height / 2);

				if (drawX > -1) {
					hitmarks[e.damageType].draw(drawX - 12, drawY - 12);
					fontSmall.drawCentered(String.valueOf(e.damageTaken), drawX, drawY + 4, 0);
					fontSmall.drawCentered(String.valueOf(e.damageTaken), drawX - 1, drawY + 3, 0xFFFFFF);
				}
			}
		}
	}

	public void drawCross() {
		if (crossType == 1) {
			crosses[crossCycle / 100].draw(crossX - 8 - 9, crossY - 8 - 11);
		}

		if (crossType == 2) {
			crosses[crossCycle / 100 + 4].draw(crossX - 8 - 9, crossY - 8 - 11);
		}
	}

	public void updateAnimatedTextures(int cycle) {
		if (lowmemory) {
			return;
		}

		if (Graphics3D.textureCycles[17] >= cycle) {
			IndexedSprite i = (Graphics3D.textures[17]);
			int len = ((i.width * i.height) - 1);
			int shift = i.width * (sceneDelta * 2);
			byte[] pixels = i.data;
			byte[] tmp = tmpTexels;

			for (int j = 0; j <= len; j++) {
				tmp[j] = pixels[j - shift & len];
			}

			i.data = tmp;
			tmpTexels = pixels;
			Graphics3D.updateTexture(17);
		}
	}

	public void drawMultiZone() {
		if (inMultizone == 1) {
			headicons[1].draw(5, 296);
		}
	}

	public void drawWildyLevel() {
		if (localPlayer == null) {
			return;
		}

		int x = (localPlayer.sceneX >> 7) + mapBaseX;
		int y = (localPlayer.sceneZ >> 7) + mapBaseY;

		if (x >= 2944 && x < 3392 && y >= 3520 && y < 6400) {
			wildernessLevel = (y - 3520) / 8 + 1;
		} else {
			wildernessLevel = 0;
		}

		if (wildernessLevel > 0) {
			headicons[0].draw(472, 296);
			fontNormal.drawCentered("Level: " + wildernessLevel, 484, 329, 0xFFFF00);
		}
	}

	public void drawSystemUpdate() {
		if (systemUpdate != 0) {
			int seconds = systemUpdate / 50;
			int minutes = seconds / 60;
			seconds %= 60;

			if (seconds < 10) {
				fontNormal.drawCentered("System update in: " + minutes + ":0" + seconds, 256, 329, 0xFFFF00);
			} else {
				fontNormal.drawCentered("System update in: " + minutes + ":" + seconds, 256, 329, 0xFFFF00);
			}
		}
	}

	public void drawDebug() {
		fontNormal.drawRightAligned(outRate + "b out/s", 504, 16, 0xFFFFFF);
		fontNormal.drawRightAligned(inRate + "b in/s", 504, 32, 0xFFFFFF);
		fontNormal.drawRightAligned("Widget: " + viewportWidgetIndex, 504, 48, 0xFFFFFF);
	}

	public final void setDrawPos(Entity e, int offset) {
		int sceneX = e.sceneX;
		int sceneY = getLandY(e.sceneX, e.sceneZ, currentLevel) - offset;
		int sceneZ = e.sceneZ;

		cameraPitch = cameraOrbitPitch;

		if (cameraMaxY / 256 > cameraPitch) {
			cameraPitch = cameraMaxY / 256;
		}

		setDrawPos(sceneX, sceneY, sceneZ);
	}

	public final void setDrawPos(int sceneX, int sceneY, int sceneZ) {
		sceneX -= cameraX;
		sceneY -= cameraY;
		sceneZ -= cameraZ;

		int psin = Model.sin[cameraPitch];
		int pcos = Model.cos[cameraPitch];
		int ysin = Model.sin[cameraOrbitYaw];
		int ycos = Model.cos[cameraOrbitYaw];

		int w = sceneZ * ysin + sceneX * ycos >> 16;
		sceneZ = sceneZ * ycos - sceneX * ysin >> 16;
		sceneX = w;

		w = sceneY * pcos - sceneZ * psin >> 16;
		sceneZ = sceneY * psin + sceneZ * pcos >> 16;
		sceneY = w;

		if (sceneZ >= 50) {
			drawX = Graphics3D.centerX + (sceneX << 9) / sceneZ;
			drawY = Graphics3D.centerY + (sceneY << 9) / sceneZ;
		} else {
			drawX = -1;
			drawY = -1;
		}
	}

	public final int getAverageTileY(int level, int x, int z) {
		return (levelHeightMaps[level][x][z] + levelHeightMaps[level][x + 1][z] + levelHeightMaps[level][x][z + 1] + levelHeightMaps[level][x + 1][z + 1]) / 4;
	}

	public final int getLandY(int sceneX, int sceneZ, int level) {
		int tileX = sceneX >> 7;
		int tileZ = sceneZ >> 7;

		if (tileX < 0 || tileX > 103 || tileZ < 0 || tileZ > 103) {
			return -1;
		}

		// this tile is a bridge
		if (level < 3 && (levelRenderFlags[1][tileX][tileZ] & 0x2) == 2) {
			level++;
		}

		int tileLocalX = sceneX & 0x7F;
		int tileLocalZ = sceneZ & 0x7F;

		int southY = ((levelHeightMaps[level][tileX][tileZ] * (128 - tileLocalX) + levelHeightMaps[level][tileX + 1][tileZ] * tileLocalX) >> 7);
		int northY = (levelHeightMaps[level][tileX][tileZ + 1] * (128 - tileLocalX) + (levelHeightMaps[level][tileX + 1][tileZ + 1] * tileLocalX)) >> 7;

		// (l * min) / max;
		return southY * (128 - tileLocalZ) + northY * tileLocalZ >> 7;
	}

	public final void updateCameraOrbit(int x, int y, int z, int cameraYaw, int cameraPitch, int distance) {
		int pitch = 2048 - cameraPitch & 0x7ff;
		int yaw = 2048 - cameraYaw & 0x7ff;

		int offsetX = 0;
		int offsetY = 0;
		int offsetZ = distance;

		if (pitch != 0) {
			int pitchSin = Model.sin[pitch];
			int pitchCos = Model.cos[pitch];
			int w = offsetY * pitchCos - offsetZ * pitchSin >> 16;
			offsetZ = offsetY * pitchSin + offsetZ * pitchCos >> 16;
			offsetY = w;
		}

		if (yaw != 0) {
			int yawSin = Model.sin[yaw];
			int yawCos = Model.cos[yaw];
			int w = offsetZ * yawSin + offsetX * yawCos >> 16;
			offsetZ = offsetZ * yawCos - offsetX * yawSin >> 16;
			offsetX = w;
		}

		cameraX = x - offsetX;
		cameraY = y - offsetY;
		cameraZ = z - offsetZ;
	}

	public void clearScene() {
		lastSceneLevel = -1;
		temporaryLocs.clear();
		animatedLocations.clear();
		spotanims.clear();
		projectiles.clear();
		Graphics3D.clearPools();
		clearCaches();
		graph.reset();

		for (int z = 0; z < 4; z++) {
			collisions[z].reset();
		}

		System.gc();
	}

	public void readLandscape(Scene s, byte[] src, int baseTileX, int baseTileZ) {
		s.readLandscape(src, baseTileX, baseTileZ, (centerSectorX - 6) * 8, (centerSectorY - 6) * 8);
	}

	public void readLocscape(Scene s, byte[] src, int baseTileX, int baseTileZ) {
		s.readLocs(src, baseTileX, baseTileZ, graph, collisions, animatedLocations);
	}

	public Scene createScene() {
		Scene s = null;

		try {
			clearScene();

			s = new Scene(104, 104, levelRenderFlags, levelHeightMaps);
			int mapCount = mapLandData.length;

			if (lowmemory) {
				graph.setup(currentLevel);
			} else {
				graph.setup(0);
			}

			for (int n = 0; n < mapCount; n++) {
				int baseTileX = ((mapIndices[n] >> 8) * 64) - mapBaseX;
				int baseTileY = ((mapIndices[n] & 0xff) * 64) - mapBaseY;
				byte[] src = mapLandData[n];

				if (src != null) {
					readLandscape(s, Signlink.getDecompressed(src), baseTileX, baseTileY);
				} else if (centerSectorY < 800) {
					s.clearLandscape(baseTileX, baseTileY, 64, 64);
				}
			}

			for (int n = 0; n < mapCount; n++) {
				int baseTileX = ((mapIndices[n] >> 8) * 64) - mapBaseX;
				int baseTileY = ((mapIndices[n] & 0xff) * 64) - mapBaseY;
				byte[] src = mapLocData[n];

				if (src != null) {
					readLocscape(s, Signlink.getDecompressed(src), baseTileX, baseTileY);
				}
			}

			s.buildLandscape(collisions, graph);
			viewport.prepare();

			updateObjectStacks();

			for (SpawnedLocation l = (SpawnedLocation) spawntLocs.peekLast(); l != null; l = (SpawnedLocation) spawntLocs.getPrevious()) {
				addLoc(l.locIndex, l.level, l.tileX, l.tileZ, l.type, l.classtype, l.rotation);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error creating scene", e);
		}

		LocationInfo.models.clear();
		System.gc();
		Graphics3D.setupPools(20);
		return s;
	}

	public final void updateObjectStacks() {
		for (int x = 0; x < 104; x++) {
			for (int y = 0; y < 104; y++) {
				updateObjectStack(x, y);
			}
		}
	}

	public final void createMinimap(int level) {
		int[] pixels = minimap.pixels;
		int pixelLength = pixels.length;

		for (int n = 0; n < pixelLength; n++) {
			pixels[n] = 0;
		}

		for (int y = 1; y < 103; y++) {
			int off = (103 - y) * (minimap.width * 4) + (52 + (48 * minimap.width));

			for (int x = 1; x < 103; x++) {
				if ((levelRenderFlags[level][x][y] & 0x18) == 0) {
					graph.drawMinimapTile(pixels, off, minimap.width, level, x, y);
				}

				// tile draws on minimap ignoring level
				if (level < 3 && (levelRenderFlags[level + 1][x][y] & 0x8) != 0) {
					graph.drawMinimapTile(pixels, off, minimap.width, level + 1, x, y);
				}
				off += 4;
			}
		}

		minimap.prepare();

		for (int y = 1; y < 103; y++) {
			for (int x = 1; x < 103; x++) {
				if ((levelRenderFlags[level][x][y] & 0x18) == 0) {
					drawMinimapLoc(x, y, level);
				}

				if (level < 3 && (levelRenderFlags[level + 1][x][y] & 0x8) != 0) {
					drawMinimapLoc(x, y, level + 1);
				}
			}
		}

		viewport.prepare();

		minimapFunctionCount = 0;

		for (int x = 0; x < 104; x++) {
			for (int y = 0; y < 104; y++) {
				int index = graph.getGroundDecorationBitset(x, y, currentLevel);

				if (index != 0) {
					index = index >> 14 & 0x7fff;
					int mapfunction = LocationInfo.get(index).mapfunction;

					if (mapfunction >= mapfunctions.length) {
						continue;
					}

					if (mapfunction >= 0) {
						minimapFunctions[minimapFunctionCount] = mapfunctions[mapfunction];
						minimapFunctionX[minimapFunctionCount] = x;
						minimapFunctionY[minimapFunctionCount] = y;
						minimapFunctionCount++;
					}
				}
			}
		}
	}

	public final void drawMinimapLoc(int x, int y, int level) {
		int bitset = graph.getWallBitset(x, y, level);

		if (bitset != 0) {
			int info = graph.getInfo(x, y, level, bitset);
			int rotation = info >> 6 & 0x3;
			int type = info & 0x1f;
			int rgb = 0xEEEEEE;

			if (bitset > 0) {
				rgb = 0xEE0000;
			}

			int[] dst = minimap.pixels;
			int off = x * 4 + 24624 + (103 - y) * 512 * 4;
			int locIndex = bitset >> 14 & 0x7fff;
			LocationInfo c = LocationInfo.get(locIndex);

			if (c.mapfunction != -1) {
				Sprite s = minimapFunctions[c.mapfunction];

				if (s != null) {
					int x0 = ((c.sizeX * 4) - s.width) / 2;
					int y0 = ((c.sizeZ * 4) - s.height) / 2;
					s.draw(48 + (x * 4) + x0, 48 + (104 - y - c.sizeZ) * 4 + y0);
				}
			} else {
				if (type == 0 || type == 2) {
					if (rotation == 0) {
						dst[off] = rgb;
						dst[off + 512] = rgb;
						dst[off + 1024] = rgb;
						dst[off + 1536] = rgb;
					} else if (rotation == 1) {
						dst[off] = rgb;
						dst[off + 1] = rgb;
						dst[off + 2] = rgb;
						dst[off + 3] = rgb;
					} else if (rotation == 2) {
						dst[off + 3] = rgb;
						dst[off + 3 + 512] = rgb;
						dst[off + 3 + 1024] = rgb;
						dst[off + 3 + 1536] = rgb;
					} else if (rotation == 3) {
						dst[off + 1536] = rgb;
						dst[off + 1536 + 1] = rgb;
						dst[off + 1536 + 2] = rgb;
						dst[off + 1536 + 3] = rgb;
					}
				}

				if (type == 3) {
					if (rotation == 0) {
						dst[off] = rgb;
					} else if (rotation == 1) {
						dst[off + 3] = rgb;
					} else if (rotation == 2) {
						dst[off + 3 + 1536] = rgb;
					} else if (rotation == 3) {
						dst[off + 1536] = rgb;
					}
				}

				if (type == 2) {
					if (rotation == 3) {
						dst[off] = rgb;
						dst[off + 512] = rgb;
						dst[off + 1024] = rgb;
						dst[off + 1536] = rgb;
					} else if (rotation == 0) {
						dst[off] = rgb;
						dst[off + 1] = rgb;
						dst[off + 2] = rgb;
						dst[off + 3] = rgb;
					} else if (rotation == 1) {
						dst[off + 3] = rgb;
						dst[off + 3 + 512] = rgb;
						dst[off + 3 + 1024] = rgb;
						dst[off + 3 + 1536] = rgb;
					} else if (rotation == 2) {
						dst[off + 1536] = rgb;
						dst[off + 1536 + 1] = rgb;
						dst[off + 1536 + 2] = rgb;
						dst[off + 1536 + 3] = rgb;
					}
				}
			}
		}

		bitset = graph.getLocationBitset(x, y, level);

		if (bitset != 0) {
			int info = graph.getInfo(x, y, level, bitset);
			int rotation = info >> 6 & 0x3;
			int type = info & 0x1f;

			if (type == 9) {
				int rgb = 0xEEEEEE;

				if (bitset > 0) {
					rgb = 0xEE0000;
				}

				int[] dst = minimap.pixels;
				int i = (x * 4) + (48 + (48 * minimap.width)) + (103 - y) * (minimap.width * 4);

				if (rotation == 0 || rotation == 2) {
					dst[i + 1536] = rgb;
					dst[i + 1024 + 1] = rgb;
					dst[i + 512 + 2] = rgb;
					dst[i + 3] = rgb;
				} else {
					dst[i] = rgb;
					dst[i + 512 + 1] = rgb;
					dst[i + 1024 + 2] = rgb;
					dst[i + 1536 + 3] = rgb;
				}
			}

			if (type == 10 || type == 11) {
				int locIndex = bitset >> 14 & 0x7fff;
				LocationInfo c = LocationInfo.get(locIndex);

				if (c.mapscene != -1) {
					IndexedSprite mapscene = mapscenes[c.mapscene];

					if (mapscene != null) {
						int dx = ((c.sizeX * 4 - mapscene.width) / 2);
						int dy = ((c.sizeZ * 4 - mapscene.height) / 2);
						mapscene.draw(x * 4 + 48 + dx, 48 + (((104 - y) - c.sizeZ) * 4) + dy);
					}
				}
			}
		}
	}

	public final void updateEntity(Entity e) {
		if (e == null) {
			return;
		}

		if (e.sceneX < 0 || e.sceneZ < 0 || e.sceneX >= 13312 || e.sceneZ >= 13312) {
			e.primaryAnimIndex = -1;
			e.spotanimIndex = -1;
			e.firstMoveCycle = 0;
			e.lastMoveCycle = 0;
			e.sceneX = (e.pathX[0] * 128) + (e.size * 64);
			e.sceneZ = (e.pathY[0] * 128) + (e.size * 64);
			e.pathStepCount = 0;
		}

		boolean noLabels = false;

		if (e.primaryAnimIndex != -1 && e.primaryAnimDelay == 0) {
			try {
				Animation s = Animation.instance[e.primaryAnimIndex];

				if (s.labelGroups == null) {
					noLabels = true;
					e.catchupCycles++;
				}
			} catch (Exception ex) {
				System.out.println("e2: " + e.primaryAnimIndex);
			}
		}

		if (e.firstMoveCycle > cycle) {
			int dt = e.firstMoveCycle - cycle;
			int dstX = (e.srcTileX * 128) + (e.size * 64);
			int dstY = (e.srcTileY * 128) + (e.size * 64);

			// rate = distance / time
			e.sceneX += (dstX - e.sceneX) / dt;
			e.sceneZ += (dstY - e.sceneZ) / dt;

			e.catchupCycles = 0;

			if (e.faceDirection == 0) {
				e.dstYaw = 1024;
			}

			if (e.faceDirection == 1) {
				e.dstYaw = 1536;
			}

			if (e.faceDirection == 2) {
				e.dstYaw = 0;
			}

			if (e.faceDirection == 3) {
				e.dstYaw = 512;
			}
		} else if (e.lastMoveCycle >= cycle) {
			try {
				if (e.lastMoveCycle == cycle || !noLabels || e.primaryAnimCycle + 1 > Animation.instance[e.primaryAnimIndex].frameDuration[e.primaryAnimFrame]) {
					// total move time in cycles (20ms/cycle)
					int duration = e.lastMoveCycle - e.firstMoveCycle;

					// time delta (time left to finish moving)
					int dt = cycle - e.firstMoveCycle;

					int srcX = (e.srcTileX * 128) + (e.size * 64);
					int srcZ = (e.srcTileY * 128) + (e.size * 64);
					int dstX = (e.dstTileX * 128) + (e.size * 64);
					int dstZ = (e.dstTileY * 128) + (e.size * 64);

					e.sceneX = (srcX * (duration - dt) + dstX * dt) / duration;
					e.sceneZ = (srcZ * (duration - dt) + dstZ * dt) / duration;
				}

				e.catchupCycles = 0;

				if (e.faceDirection == 0) {
					e.dstYaw = 1024;
				}

				if (e.faceDirection == 1) {
					e.dstYaw = 1536;
				}

				if (e.faceDirection == 2) {
					e.dstYaw = 0;
				}

				if (e.faceDirection == 3) {
					e.dstYaw = 512;
				}

				e.yaw = e.dstYaw;
			} catch (Exception ex) {
				System.out.println("e4: " + e.primaryAnimIndex);
			}
		} else {
			try {
				if (e.pathStepCount == 0) {
					e.catchupCycles = 0;
				}

				if (e.pathStepCount > 0 && !noLabels) {
					int x = e.sceneX;
					int y = e.sceneZ;
					int dstX = ((e.pathX[e.pathStepCount - 1]) * 128 + e.size * 64);
					int dstY = ((e.pathY[e.pathStepCount - 1]) * 128 + e.size * 64);

					if (dstX - x > 0x100 || dstX - x < -0x100 || dstY - y > 0x100 || dstY - y < -0x100) {
						e.sceneX = dstX;
						e.sceneZ = dstY;
					} else {
						if (x < dstX) {
							if (y < dstY) {
								e.dstYaw = 1280;
							} else if (y > dstY) {
								e.dstYaw = 1792;
							} else {
								e.dstYaw = 1536;
							}
						} else if (x > dstX) {
							if (y < dstY) {
								e.dstYaw = 768;
							} else if (y > dstY) {
								e.dstYaw = 256;
							} else {
								e.dstYaw = 512;
							}
						} else if (y < dstY) {
							e.dstYaw = 1024;
						} else {
							e.dstYaw = 0;
						}

						int deltaYaw = ((e.dstYaw - e.yaw) & 0x7ff);

						if (deltaYaw > 1024) {
							deltaYaw -= 2048;
						}

						int animIndex = e.animTurnBackIndex;

						if (deltaYaw >= -256 && deltaYaw <= 256) {
							animIndex = e.animWalkIndex;
						} else if (deltaYaw >= 256 && deltaYaw < 768) {
							animIndex = e.animTurnLeftIndex;
						} else if (deltaYaw >= -768 && deltaYaw <= -256) {
							animIndex = e.animTurnRightIndex;
						}

						if (animIndex == -1) {
							animIndex = e.animWalkIndex;
						}

						if (animIndex != e.secondaryAnimIndex) {
							e.secondaryAnimIndex = animIndex;
							e.secondaryAnimFrame = 0;
							e.secondaryAnimCycle = 0;
						}

						int speed = 4;

						if ((e.yaw != e.dstYaw) && e.targetEntity == -1) {
							speed = 2;
						}

						if (e.pathStepCount > 2) {
							speed = 6;
						}

						if (e.pathStepCount > 3) {
							speed = 8;
						}

						if (e.catchupCycles > 0 && e.pathStepCount > 1) {
							speed = 8;
							e.catchupCycles--;
						}

						if (x < dstX) {
							e.sceneX += speed;

							if (e.sceneX > dstX) {
								e.sceneX = dstX;
							}
						} else if (x > dstX) {
							e.sceneX -= speed;

							if (e.sceneX < dstX) {
								e.sceneX = dstX;
							}
						}

						if (y < dstY) {
							e.sceneZ += speed;

							if (e.sceneZ > dstY) {
								e.sceneZ = dstY;
							}
						} else if (y > dstY) {
							e.sceneZ -= speed;

							if (e.sceneZ < dstY) {
								e.sceneZ = dstY;
							}
						}

						if (e.sceneX == dstX && e.sceneZ == dstY) {
							e.pathStepCount--;
						}
					}
				} else {
					e.secondaryAnimIndex = e.animStand;
				}
			} catch (Exception exception) {
				System.out.println("e5: " + e.pathStepCount);
			}
		}

		if (e.targetEntity != -1 && e.targetEntity < 32768) {
			try {
				NPC n = (npcs[e.targetEntity]);
				if (n != null) {
					int dx = (e.sceneX - n.sceneX);
					int dy = (e.sceneZ - n.sceneZ);
					if (dx != 0 || dy != 0) {
						e.dstYaw = (int) (Math.atan2((double) dx, (double) dy) * 325.949) & 0x7ff;
					}
				}
			} catch (Exception ex) {
				System.out.println("e6: " + e.targetEntity);
			}
		}

		if (e.targetEntity >= 32768) {
			try {
				int target = e.targetEntity - 32768;

				if (target == localPlayerIndex) {
					target = LOCALPLAYER_INDEX;
				}

				Player p = players[target];

				if (p != null) {
					int dx = (e.sceneX - p.sceneX);
					int dy = (e.sceneZ - p.sceneZ);

					if (dx != 0 || dy != 0) {
						e.dstYaw = (int) (Math.atan2((double) dx, (double) dy) * 325.949) & 0x7ff;
					}
				}
			} catch (Exception ex) {
				System.out.println("e7: " + e.targetEntity);
			}
		}

		if ((e.focusX != 0 || e.focusY != 0) && (e.pathStepCount == 0 || e.catchupCycles > 0)) {
			int dx = (e.sceneX - (e.focusX - mapBaseX - mapBaseX) * 64);
			int dy = (e.sceneZ - (e.focusY - mapBaseY - mapBaseY) * 64);

			if (dx != 0 || dy != 0) {
				e.dstYaw = (int) (Math.atan2((double) dx, (double) dy) * 325.949) & 0x7ff;
			}

			e.focusX = 0;
			e.focusY = 0;
		}

		int deltaYaw = (e.dstYaw - e.yaw & 0x7ff);

		if (deltaYaw != 0) {

			if (deltaYaw < 32 || deltaYaw > 2016) {
				e.yaw = e.dstYaw;
			} else if (deltaYaw > 1024) {
				e.yaw -= 32;
			} else {
				e.yaw += 32;
			}

			e.yaw &= 0x7ff;

			if (e.secondaryAnimIndex == e.animStand) {
				if (e.animTurnIndex != -1) {
					e.secondaryAnimIndex = e.animTurnIndex;
				} else {
					e.secondaryAnimIndex = e.animWalkIndex;
				}
			}
		}

		e.renderPadding = 0;

		if (e.secondaryAnimIndex != -1) {
			try {
				Animation a = Animation.instance[e.secondaryAnimIndex];
				e.secondaryAnimCycle++;

				if (e.secondaryAnimFrame < a.frameCount && (e.secondaryAnimCycle > (a.frameDuration[e.secondaryAnimFrame]))) {
					e.secondaryAnimCycle = 0;
					e.secondaryAnimFrame++;
				}

				if (e.secondaryAnimFrame >= a.frameCount) {
					e.secondaryAnimCycle = 0;
					e.secondaryAnimFrame = 0;
				}
			} catch (Exception ex) {
				System.out.println("e8: " + e.secondaryAnimIndex);
			}
		}

		if (e.primaryAnimIndex != -1 && e.primaryAnimDelay == 0) {
			try {
				Animation a = Animation.instance[e.primaryAnimIndex];

				for (e.primaryAnimCycle++; (e.primaryAnimFrame < a.frameCount && (e.primaryAnimCycle > (a.frameDuration[e.primaryAnimFrame]))); e.primaryAnimFrame++) {
					e.primaryAnimCycle -= (a.frameDuration[e.primaryAnimFrame]);
				}

				if (e.primaryAnimFrame >= a.frameCount) {
					e.primaryAnimFrame -= a.delta;
					e.primaryAnimDelta++;

					if (e.primaryAnimDelta >= a.length) {
						e.primaryAnimIndex = -1;
					}

					if (e.primaryAnimFrame < 0 || e.primaryAnimFrame >= a.frameCount) {
						e.primaryAnimIndex = -1;
					}
				}

				e.renderPadding = a.renderPadding;
			} catch (Exception ex) {
				System.out.println("e9: " + e.primaryAnimIndex);
			}
		}

		if (e.primaryAnimDelay > 0) {
			e.primaryAnimDelay--;
		}

		do {
			if (e.spotanimIndex != -1 && cycle >= e.lastSpotanimCycle) {
				try {
					if (e.spotanimFrame < 0) {
						e.spotanimFrame = 0;
					}

					Animation a = SpotAnimation.instance[e.spotanimIndex].animation;

					for (e.spotanimCycle++; (e.spotanimFrame < a.frameCount && (e.spotanimCycle > (a.frameDuration[e.spotanimFrame]))); e.spotanimFrame++) {
						e.spotanimCycle -= (a.frameDuration[e.spotanimFrame]);
					}

					if (e.spotanimFrame < a.frameCount) {
						break;
					}

					e.spotanimIndex = -1;
				} catch (Exception exception) {
					System.out.println("e10: " + e.spotanimIndex);
				}
				break;
			}
		} while (false);
	}

	public final void updatePlayers() {
		for (int n = -1; n < playerCount; n++) {
			int index;

			if (n == -1) {
				index = LOCALPLAYER_INDEX;
			} else {
				index = playerIndices[n];
			}

			updateEntity(players[index]);
		}
	}

	public final void updateNPCs() {
		for (int n = 0; n < entityCount; n++) {
			updateEntity(npcs[npcIndices[n]]);
		}
	}

	public void interactWithLocation(int bitset, int x, int y, int opcode) {
		int locIndex = bitset >> 14 & 0x7fff;
		int info = graph.getInfo(x, y, currentLevel, bitset);

		if (info != -1) {
			int type = info & 0x1f;
			int rotation = info >> 6 & 0x3;

			if (type == 10 || type == 11 || type == 22) {
				LocationInfo c = LocationInfo.get(locIndex);
				int sizeX;
				int sizeY;

				if (rotation == 0 || rotation == 2) {
					sizeX = c.sizeX;
					sizeY = c.sizeZ;
				} else {
					sizeX = c.sizeZ;
					sizeY = c.sizeX;
				}

				int interactionSide = c.interactionSideFlags;

				if (rotation != 0) {
					interactionSide = (interactionSide << rotation & 0xf) + (interactionSide >> 4 - rotation);
				}

				moveTo(localPlayer.pathX[0], localPlayer.pathY[0], x, y, sizeX, sizeY, 0, interactionSide, 0, false);
			} else {
				moveTo(localPlayer.pathX[0], localPlayer.pathY[0], x, y, 0, 0, type + 1, 0, rotation, false);
			}

			crossX = clickX;
			crossY = clickY;
			crossType = 2;
			crossCycle = 0;

			out.putOpcode(opcode);
			out.putShort(x + mapBaseX);
			out.putShort(y + mapBaseY);
			out.putShort(locIndex);
		}
	}

	public final boolean moveTo(int srcX, int srcZ, int dstX, int dstZ, int width, int length, int type, int faceflags, int interactionSide, boolean arbitrary) {
		for (int tileX = 0; tileX < 104; tileX++) {
			for (int tileZ = 0; tileZ < 104; tileZ++) {
				pathWaypoint[tileX][tileZ] = 0;
				pathDistance[tileX][tileZ] = 99999999;
			}
		}

		int x = srcX;
		int z = srcZ;
		pathWaypoint[srcX][srcZ] = 99;
		pathDistance[srcX][srcZ] = 0;

		int count = 0;
		int current = 0;
		waypointX[count] = srcX;
		waypointY[count++] = srcZ;

		boolean reached = false;
		int pathStepCount = waypointX.length;
		int[][] flags = collisions[currentLevel].flags;

		while (current != count) {
			x = waypointX[current];
			z = waypointY[current];
			current = (current + 1) % pathStepCount;

			if (x == dstX && z == dstZ) {
				reached = true;
				break;
			}

			if (type != 0) {
				if ((type < 5 || type == 10) && collisions[currentLevel].method101(x, z, dstZ, dstX, type - 1, interactionSide)) {
					reached = true;
					break;
				}
				if (type < 10 && collisions[currentLevel].method102(x, z, dstX, dstZ, type - 1, interactionSide)) {
					reached = true;
					break;
				}
			}

			if (width != 0 && length != 0 && collisions[currentLevel].method103(x, z, width, length, dstX, dstZ, faceflags)) {
				reached = true;
				break;
			}

			int distance = pathDistance[x][z] + 1;

			if (x > 0 && pathWaypoint[x - 1][z] == 0 && (flags[x - 1][z] & 0x280108) == 0) {
				waypointX[count] = x - 1;
				waypointY[count] = z;
				count = (count + 1) % pathStepCount;
				pathWaypoint[x - 1][z] = 2;
				pathDistance[x - 1][z] = distance;
			}

			if (x < 104 - 1 && pathWaypoint[x + 1][z] == 0 && (flags[x + 1][z] & 0x280180) == 0) {
				waypointX[count] = x + 1;
				waypointY[count] = z;
				count = (count + 1) % pathStepCount;
				pathWaypoint[x + 1][z] = 8;
				pathDistance[x + 1][z] = distance;
			}

			if (z > 0 && pathWaypoint[x][z - 1] == 0 && (flags[x][z - 1] & 0x280102) == 0) {
				waypointX[count] = x;
				waypointY[count] = z - 1;
				count = (count + 1) % pathStepCount;
				pathWaypoint[x][z - 1] = 1;
				pathDistance[x][z - 1] = distance;
			}

			if (z < 104 - 1 && pathWaypoint[x][z + 1] == 0 && (flags[x][z + 1] & 0x280120) == 0) {
				waypointX[count] = x;
				waypointY[count] = z + 1;
				count = (count + 1) % pathStepCount;
				pathWaypoint[x][z + 1] = 4;
				pathDistance[x][z + 1] = distance;
			}

			if (x > 0 && z > 0 && pathWaypoint[x - 1][z - 1] == 0 && (flags[x - 1][z - 1] & 0x28010e) == 0 && (flags[x - 1][z] & 0x280108) == 0 && (flags[x][z - 1] & 0x280102) == 0) {
				waypointX[count] = x - 1;
				waypointY[count] = z - 1;
				count = (count + 1) % pathStepCount;
				pathWaypoint[x - 1][z - 1] = 3;
				pathDistance[x - 1][z - 1] = distance;
			}

			if (x < 104 - 1 && z > 0 && pathWaypoint[x + 1][z - 1] == 0 && (flags[x + 1][z - 1] & 0x280183) == 0 && (flags[x + 1][z] & 0x280180) == 0 && (flags[x][z - 1] & 0x280102) == 0) {
				waypointX[count] = x + 1;
				waypointY[count] = z - 1;
				count = (count + 1) % pathStepCount;
				pathWaypoint[x + 1][z - 1] = 9;
				pathDistance[x + 1][z - 1] = distance;
			}

			if (x > 0 && z < 104 - 1 && pathWaypoint[x - 1][z + 1] == 0 && (flags[x - 1][z + 1] & 0x280138) == 0 && (flags[x - 1][z] & 0x280108) == 0 && (flags[x][z + 1] & 0x280120) == 0) {
				waypointX[count] = x - 1;
				waypointY[count] = z + 1;
				count = (count + 1) % pathStepCount;
				pathWaypoint[x - 1][z + 1] = 6;
				pathDistance[x - 1][z + 1] = distance;
			}

			if (x < 104 - 1 && z < 104 - 1 && pathWaypoint[x + 1][z + 1] == 0 && (flags[x + 1][z + 1] & 0x2801e0) == 0 && (flags[x + 1][z] & 0x280180) == 0 && (flags[x][z + 1] & 0x280120) == 0) {
				waypointX[count] = x + 1;
				waypointY[count] = z + 1;
				count = (count + 1) % pathStepCount;
				pathWaypoint[x + 1][z + 1] = 12;
				pathDistance[x + 1][z + 1] = distance;
			}
		}

		if (!reached) {
			if (arbitrary) {
				int maxDistance = 100;
				for (int n = 1; n < 2; n++) {
					for (int dx = dstX - n; dx <= dstX + n; dx++) {
						for (int dy = dstZ - n; dy <= dstZ + n; dy++) {
							if (dx >= 0 && dy >= 0 && dx < 104 && dy < 104 && (pathDistance[dx][dy] < maxDistance)) {
								maxDistance = pathDistance[dx][dy];
								x = dx;
								z = dy;
								reached = true;
							}
						}
					}
					if (reached) {
						break;
					}
				}
			}
			if (!reached) {
				return false;
			}
		}

		current = 0;
		waypointX[current] = x;
		waypointY[current++] = z;
		int lastWaypoint;
		int waypoint = lastWaypoint = pathWaypoint[x][z];

		while (x != srcX || z != srcZ) {
			if (waypoint != lastWaypoint) {
				lastWaypoint = waypoint;
				waypointX[current] = x;
				waypointY[current++] = z;
			}

			if ((waypoint & 0x2) != 0) {
				x++;
			} else if ((waypoint & 0x8) != 0) {
				x--;
			}

			if ((waypoint & 0x1) != 0) {
				z++;
			} else if ((waypoint & 0x4) != 0) {
				z--;
			}

			waypoint = pathWaypoint[x][z];
		}

		if (current > 0) {
			pathStepCount = current;

			if (pathStepCount > 25) {
				pathStepCount = 25;
			}

			current--;
			int px = waypointX[current];
			int py = waypointY[current];

			out.putOpcode(Packet.WALK);
			out.putByte((pathStepCount * 2) + 2);
			out.putShort(px + mapBaseX);
			out.putShort(py + mapBaseY);

			System.out.println(pathStepCount);

			for (int n = 1; n < pathStepCount; n++) {
				current--;
				out.putByte(waypointX[current] - px);
				out.putByte(waypointY[current] - py);
			}
		}
		return true;
	}

	public final boolean readStream() {
		if (stream == null) {
			return false;
		}

		try {
			int available = stream.available();

			if (available == 0) {
				return false;
			}

			System.out.println(available);

			if (packetType == -1) {
				stream.read(in.data, 0, 1);
				packetType = in.data[0] & 0xff;

				if (isaac != null) {
					packetType = (packetType - isaac.nextInt()) & 0xFF;
				}

				inBytes++;
				packetSize = Packet.SIZE[packetType];
				available--;
			}

			// byte sized payload
			if (packetSize == -1) {
				if (available > 0) {
					stream.read((in.data), 0, 1);
					packetSize = in.data[0] & 0xff;
					inBytes++;
					available--;
				} else {
					return false;
				}
			}

			// short sized payload
			if (packetSize == -2) {
				if (available > 1) {
					stream.read((in.data), 0, 2);
					in.position = 0;
					packetSize = in.getUShort();
					inBytes += 2;
					available -= 2;
				} else {
					return false;
				}
			}

			if (available < packetSize) {
				return false;
			}

			in.position = 0;
			stream.read(in.data, 0, packetSize);
			netIdleCycles = 0;
			lastPacketType = packetType;

			inBytes += packetSize;

			if (packetType == Packet.SECTOR_CHANGE) {
				viewport.prepare();
				fontNormal.drawCentered("Loading - please wait.", 257, 151, 0);
				fontNormal.drawCentered("Loading - please wait.", 256, 150, 0xFFFFFF);

				viewport.draw(graphics, 8, 11);

				sceneState = 1;

				centerSectorX = in.getUShort();
				centerSectorY = in.getUShort();

				mapBaseX = (centerSectorX - 6) * 8;
				mapBaseY = (centerSectorY - 6) * 8;

				Signlink.setLoopRate(5);

				int minRegionX = (centerSectorX - 6) / 8;
				int minRegionY = (centerSectorY - 6) / 8;
				int maxRegionX = (centerSectorX + 6) / 8;
				int maxRegionY = (centerSectorY + 6) / 8;

				// = (packetSize - 2) / 10;
				int mapCount = ((maxRegionX - minRegionX) + 1) * ((maxRegionY - minRegionY) + 1);

				mapLandData = new byte[mapCount][];
				mapLocData = new byte[mapCount][];
				mapIndices = new int[mapCount];

				mapCount = 0;

				// sector change response
				out.startVarSize(Packet.SECTOR_CHANGE_RESPONSE, 1);
				for (int x = minRegionX; x <= maxRegionX; x++) {
					for (int y = minRegionY; y <= maxRegionY; y++) {
						mapIndices[mapCount] = (x << 8) | y;

						byte[] data = Signlink.loadFile(String.format("maps/m%s_%s", x, y));

						if (data != null) {
							mapLandData[mapCount] = data;
						} else {
							out.putByte(0);
							out.putByte(x);
							out.putByte(y);
						}

						data = Signlink.loadFile(String.format("maps/l%s_%s", x, y));

						if (data != null) {
							mapLocData[mapCount] = data;
						} else {
							out.putByte(1);
							out.putByte(x);
							out.putByte(y);
						}

						mapCount++;
					}
				}
				out.endVarSize();

				/*for (int n = 0; n < mapCount; n++) {
				 int x = in.getUByte();
				 int y = in.getUByte();
				 int mapcrc32 = in.getInt();
				 int loccrc32 = in.getInt();
				 mapIndices[n] = (x << 8) + y;

				 if (mapcrc32 != 0) {
				 byte[] data = Signlink.loadFile("maps/m" + x + "_" + y);

				 if (data != null) {
				 crc32.reset();
				 crc32.update(data);
				 if ((int) crc32.getValue() != mapcrc32) {
				 // data = null;
				 }
				 }

				 if (data != null) {
				 mapLandData[n] = data;
				 } else {
				 sceneState = 0;
				 out.putByte(0);
				 out.putByte(x);
				 out.putByte(y);
				 length += 3;
				 }

				 }

				 if (loccrc32 != 0) {
				 byte[] data = Signlink.loadFile("maps/l" + x + "_" + y);

				 if (data != null) {
				 crc32.reset();
				 crc32.update(data);
				 if ((int) crc32.getValue() != loccrc32) {
				 // data = null;
				 }
				 }

				 if (data != null) {
				 mapLocData[n] = data;
				 } else {
				 sceneState = 0;
				 out.putByte(1);
				 out.putByte(x);
				 out.putByte(y);
				 length += 3;
				 }
				 }
				 }*/
				Signlink.setLoopRate(100);

				int deltaX = mapBaseX - mapLastBaseX;
				int deltaY = mapBaseY - mapLastBaseZ;
				mapLastBaseX = mapBaseX;
				mapLastBaseZ = mapBaseY;

				for (int i = 0; i < MAX_ENTITY_COUNT; i++) {
					NPC npc = npcs[i];

					if (npc != null) {
						for (int j = 0; j < 10; j++) {
							npc.pathX[j] -= deltaX;
							npc.pathY[j] -= deltaY;
						}

						npc.sceneX -= deltaX * 128;
						npc.sceneZ -= deltaY * 128;
					}
				}

				for (int n = 0; n < MAX_ENTITY_COUNT; n++) {
					Player p = players[n];

					if (p != null) {
						for (int j = 0; j < 10; j++) {
							p.pathX[j] -= deltaX;
							p.pathY[j] -= deltaY;
						}

						p.sceneX -= deltaX * 128;
						p.sceneZ -= deltaY * 128;
					}
				}

				int startTileX = 0;
				int endTileX = 104;
				int dirX = 1;

				if (deltaX < 0) {
					startTileX = 103;
					endTileX = -1;
					dirX = -1;
				}

				int startTileY = 0;
				int endTileY = 104;
				int dirY = 1;

				if (deltaY < 0) {
					startTileY = 103;
					endTileY = -1;
					dirY = -1;
				}

				for (int tileX = startTileX; tileX != endTileX; tileX += dirX) {
					for (int tileZ = startTileY; tileZ != endTileY; tileZ += dirY) {
						int lastX = tileX + deltaX;
						int lastZ = tileZ + deltaY;
						for (int level = 0; level < 4; level++) {
							if (lastX >= 0 && lastZ >= 0 && lastX < 104 && lastZ < 104) {
								levelObjects[level][tileX][tileZ] = (levelObjects[level][lastX][lastZ]);
							} else {
								levelObjects[level][tileX][tileZ] = null;
							}
						}
					}
				}

				for (SpawnedLocation l = (SpawnedLocation) spawntLocs.peekLast(); l != null; l = (SpawnedLocation) spawntLocs.getPrevious()) {
					l.tileX -= deltaX;
					l.tileZ -= deltaY;

					if (l.tileX < 0 || l.tileZ < 0 || l.tileX >= 104 || l.tileZ >= 104) {
						l.unlink();
					}
				}
			} else if (packetType == Packet.SET_MULTIZONE) {
				inMultizone = in.getUByte();
			} else if (packetType == Packet.SET_DISABLED_WIDGET_COLOR) {
				int index = in.getUShort();
				int rgb = in.getUShort();
				int red = rgb >> 10 & 0x1f;
				int green = rgb >> 5 & 0x1f;
				int blue = rgb & 0x1f;
				Widget.instances[index].colorDisabled = (red << 19) + (green << 11) + (blue << 3);
			} else if (packetType == Packet.UPDATE_SKILL) {
				sidebarRedraw = true;
				int skill = in.getUByte();
				int experience = in.getInt();
				int level = in.getUByte();
				skillExperience[skill] = experience;
				skillLevelReal[skill] = level;
				skillLevel[skill] = 1;
				for (int n = 0; n < 98; n++) {
					if (experience >= EXPERIENCE_TABLE[n]) {
						skillLevel[skill] = n + 2;
					}
				}
			} else if (packetType == Packet.RESET_ENTITY_ANIMATIONS) {
				for (Player player : players) {
					if (player != null) {
						player.primaryAnimIndex = -1;
					}
				}
				for (NPC npc : npcs) {
					if (npc != null) {
						npc.primaryAnimIndex = -1;
					}
				}
			} else if (packetType == Packet.UPDATE_SCENE) {
				updatePlayers(in, packetSize);

				if (sceneState == 1) {
					sceneState = 2;
					Scene.levelBuilt = currentLevel;
					createScene();
				}

				if (lowmemory && sceneState == 2 && Scene.levelBuilt != currentLevel) {
					viewport.prepare();
					fontNormal.drawCentered("Loading - please wait.", 257, 151, 0);
					fontNormal.drawCentered("Loading - please wait.", 0x100, 150, 0xFFFFFF);
					viewport.draw(graphics, 8, 11);
					Scene.levelBuilt = currentLevel;
					createScene();
				}

				if (currentLevel != lastSceneLevel && sceneState == 2) {
					lastSceneLevel = currentLevel;
					createMinimap(currentLevel);
				}
			} else if (packetType == Packet.SET_TAB_WIDGET) {
				int widgetIndex = in.getUShort();
				int tabIndex = in.getUByte();

				if (widgetIndex == 0xFFFF) {
					widgetIndex = -1;
				}

				tabWidgetIndex[tabIndex] = widgetIndex;
				sidebarRedraw = true;
			} else if (packetType == Packet.ADD_MESSAGE) {
				String s = in.getString();

				if (s.endsWith(":tradereq:")) {
					String name = s.substring(0, s.indexOf(":"));
					long l = StringUtil.toBase37(name);
					boolean ignore = false;
					for (int n = 0; n < ignoreCount; n++) {
						if (ignoreNameLong[n] == l) {
							ignore = true;
							break;
						}
					}
					if (!ignore) {
						addMessage(4, name, "wishes to trade with you.");
					}
				} else {
					addMessage(0, "", s);
				}
			} else if (packetType == Packet.SET_CHAT_SETTINGS) {
				chatPublicSetting = in.getUByte();
				chatPrivateSetting = in.getUByte();
				chatTradeDuelSetting = in.getUByte();
				chatRedrawSettings = true;
				chatRedraw = true;
			} else if (packetType == Packet.SET_DISABLED_WIDGET_ANIMATION) {
				Widget.instances[in.getUShort()].animIndexDisabled = in.getUShort();
			} else if (packetType == Packet.SET_TEMPORARY_POSITION) {
				netTileX = in.getUByte();
				netTileZ = in.getUByte();
			} else if (packetType == Packet.SET_SHORT_VARIABLE) {
				sidebarRedraw = true;

				int key = in.getUShort();
				int value = in.getByte();

				if (variables[key] != value) {
					variables[key] = value;
					updateVarp(key);
				}
			} else if (packetType == Packet.SET_WIDGET_MODEL_TO_PLAYER_HEAD) {
				Widget.instances[in.getUShort()].modelDisabled = localPlayer.getHeadModel();
			} else if (packetType == Packet.SAVE_LOCATION_MAPS) {
				int x = in.getUByte();
				int y = in.getUByte();
				int index = -1;

				for (int n = 0; n < mapIndices.length; n++) {
					if (mapIndices[n] == (x << 8) + y) {
						index = n;
					}
				}

				if (index != -1) {
					Signlink.saveFile("l" + x + "_" + y, mapLocData[index]);
					sceneState = 1;
				}
			} else if (packetType == Packet.SET_VIEWPORT_WIDGET) {
				int widgetIndex = in.getUShort();
				resetWidgetAnimations(widgetIndex);

				if (sidebarWidgetIndex != -1) {
					sidebarWidgetIndex = -1;
					sidebarRedraw = true;
					sidebarRedrawIcons = true;
				}

				if (chatWidgetIndex != -1) {
					chatWidgetIndex = -1;
					chatRedraw = true;
				}

				if (chatShowTransferInput) {
					chatShowTransferInput = false;
					chatRedraw = true;
				}

				viewportWidgetIndex = widgetIndex;
				chatContinuingDialogue = false;
			} else if (packetType == Packet.READ_LANDSCAPE_MAPS) {
				int x = in.getUByte();
				int y = in.getUByte();
				int off = in.getUShort();
				int length = in.getUShort();
				int index = -1;

				for (int n = 0; n < mapIndices.length; n++) {
					if (mapIndices[n] == (x << 8) + y) {
						index = n;
					}
				}

				if (index != -1) {
					if (mapLandData[index] == null || mapLandData[index].length != length) {
						mapLandData[index] = new byte[length];
					}
					in.getBytes(mapLandData[index], off, packetSize - 6);
				}
			} else if (packetType == Packet.SET_WIDGET_MODEL_TO_NPC_HEAD) {
				Widget.instances[in.getUShort()].modelDisabled = NPCInfo.get(in.getUShort()).getHeadModel();
			} else if (packetType == Packet.READ_WIDGET_INVENTORY) {
				sidebarRedraw = true;
				Widget w = Widget.instances[in.getUShort()];

				while (in.position < packetSize) {
					int slot = in.getUByte();
					int index = in.getUShort();
					int amount = in.getUByte();

					if (amount == 0xFF) {
						amount = in.getInt();
					}

					if (slot >= 0 && slot < w.inventoryIndices.length) {
						w.inventoryIndices[slot] = index;
						w.inventoryAmount[slot] = amount;
					}
				}
			} else if (packetType == Packet.ATTACH_TEMPORARY_LOCATION_TO_PLAYER || packetType == Packet.ADD_PRIVATE_OBJECT_STACK || packetType == Packet.ADD_SPOT_ANIMATION || packetType == Packet.ADD_PROJECTILE || packetType == Packet.REMOVE_OBJECT_STACK || packetType == Packet.ADD_OBJECT_STACK || packetType == Packet.ADD_ANIMATED_LOCATION || packetType == Packet.REMOVE_LOCATION || packetType == Packet.ADD_LOCATION) {
				readSecondaryPacket(in, packetType);
			} else if (packetType == Packet.SET_WIDGET_VISIBILITY) {
				Widget.instances[in.getUShort()].hidden = in.getUByte() == 1;
			} else if (packetType == Packet.CLEAR_WIDGETS_AND_INPUTS) {
				if (sidebarWidgetIndex != -1) {
					sidebarWidgetIndex = -1;
					sidebarRedraw = true;
					sidebarRedrawIcons = true;
				}

				if (chatWidgetIndex != -1) {
					chatWidgetIndex = -1;
					chatRedraw = true;
				}

				if (chatShowTransferInput) {
					chatShowTransferInput = false;
					chatRedraw = true;
				}

				viewportWidgetIndex = -1;
				chatContinuingDialogue = false;
			} else if (packetType == Packet.READ_LOCATION_MAPS) {
				int x = in.getUByte();
				int y = in.getUByte();
				int off = in.getUShort();
				int length = in.getUShort();
				int index = -1;

				for (int n = 0; n < mapIndices.length; n++) {
					if (mapIndices[n] == (x << 8) + y) {
						index = n;
					}
				}

				if (index != -1) {
					if (mapLocData[index] == null || mapLocData[index].length != length) {
						mapLocData[index] = new byte[length];
					}
					in.getBytes(mapLocData[index], off, packetSize - 6);
				}
			} else if (packetType == Packet.SAVE_LANDSCAPE_MAPS) {
				int x = in.getUByte();
				int y = in.getUByte();
				int index = -1;
				for (int n = 0; n < mapIndices.length; n++) {
					if (mapIndices[n] == (x << 8) + y) {
						index = n;
					}
				}

				if (index != -1) {
					Signlink.saveFile("m" + x + "_" + y, mapLandData[index]);
					sceneState = 1;
				}
			} else if (packetType == Packet.UPDATE_NPCS) {
				updateNPCs(in, packetSize);
			} else if (packetType == Packet.CLEAR_WIDGET_INVENTORY) {
				Widget w = Widget.instances[in.getUShort()];

				for (int n = 0; n < w.inventoryIndices.length; n++) {
					w.inventoryIndices[n] = -1;
					w.inventoryIndices[n] = 0;
				}
			} else if (packetType == Packet.SET_DISABLED_WIDGET_MODEL) {
				Widget.instances[in.getUShort()].modelDisabled = new Model(in.getUShort());
			} else if (packetType == Packet.SET_SELECTED_TAB) {
				selectedTab = in.getUByte();
				sidebarRedraw = true;
				sidebarRedrawIcons = true;
			} else if (packetType == Packet.NOTIFY_FRIEND_STATUS) {
				long nameLong = in.getLong();
				int world = in.getUByte();
				String name = StringUtil.getFormatted(StringUtil.fromBase37(nameLong));

				for (int n = 0; n < friendCount; n++) {
					if (name.equals(friendName[n])) {
						if (friendWorld[n] != world) {
							friendWorld[n] = world;
							sidebarRedraw = true;

							if (world > 0) {
								addMessage(5, "", name + " has logged in.");
							} else if (world == 0) {
								addMessage(5, "", name + " has logged out.");
							}
						}
						name = null;
						break;
					}
				}

				if (name != null && friendCount < 100) {
					friendName[friendCount] = name;
					friendWorld[friendCount] = world;
					friendCount++;
					sidebarRedraw = true;
				}

				boolean sorted = false;
				while (!sorted) {
					sorted = true;

					for (int n = 0; n < friendCount - 1; n++) {
						if ((friendWorld[n] != nodeid && friendWorld[n + 1] == nodeid) || (friendWorld[n] == 0 && friendWorld[n + 1] != 0)) {
							int i = friendWorld[n];
							friendWorld[n] = friendWorld[n + 1];
							friendWorld[n + 1] = i;

							String s = friendName[n];
							friendName[n] = friendName[n + 1];
							friendName[n + 1] = s;
							sidebarRedraw = true;
							sorted = false;
						}
					}
				}
			} else if (packetType == Packet.SET_MIDI) {
				String s = in.getString();
				if (!s.equals(midi)) {
					midi = s;

					if (midiPlaying) {
						Signlink.midi = s;
					}
				}
			} else if (packetType == Packet.SET_INTEGER_VARIABLE) {
				sidebarRedraw = true;

				int key = in.getUShort();
				int value = in.getInt();

				if (variables[key] != value) {
					variables[key] = value;
					updateVarp(key);
				}
			} else if (packetType == Packet.SET_VIEWPORT_AND_SIDEBAR_WIDGETS) {
				if (chatWidgetIndex != -1) {
					chatWidgetIndex = -1;
					chatRedraw = true;
				}

				if (chatShowTransferInput) {
					chatShowTransferInput = false;
					chatRedraw = true;
				}

				viewportWidgetIndex = in.getUShort();
				sidebarWidgetIndex = in.getUShort();
				sidebarRedraw = true;
				sidebarRedrawIcons = true;
				chatContinuingDialogue = false;
			} else if (packetType == Packet.READ_SECONDARY_PACKETS) {
				netTileX = in.getUByte();
				netTileZ = in.getUByte();

				while (in.position < packetSize) {
					readSecondaryPacket(in, in.getUByte());
				}
			} else if (packetType == Packet.SET_LOCAL_PLAYER_INDEX) {
				localPlayerIndex = in.getUShort();
			} else if (packetType == Packet.SET_JINGLE) {
				if (midiPlaying) {
					Signlink.jingle = in.getString();
					Signlink.jinglelen = in.getUShort();
				}
			} else if (packetType == Packet.SET_WIDGET_INVENTORY) {
				sidebarRedraw = true;
				Widget w = Widget.instances[in.getUShort()];
				int length = in.getUByte();

				for (int n = 0; n < length; n++) {
					w.inventoryIndices[n] = in.getUShort();
					int amount = in.getUByte();

					if (amount == 0xFF) {
						amount = in.getInt();
					}

					w.inventoryAmount[n] = amount;
				}

				for (int n = length; n < w.inventoryIndices.length; n++) {
					w.inventoryIndices[n] = 0;
					w.inventoryAmount[n] = 0;
				}
			} else if (packetType == Packet.CLEAR_8X8_OBJECTS_LOCS) {
				netTileX = in.getUByte();
				netTileZ = in.getUByte();

				for (int x = netTileX; x < netTileX + 8; x++) {
					for (int z = netTileZ; z < netTileZ + 8; z++) {
						if (levelObjects[currentLevel][x][z] != null) {
							levelObjects[currentLevel][x][z] = null;
							updateObjectStack(x, z);
						}
					}
				}

				for (SpawnedLocation l = (SpawnedLocation) spawntLocs.peekLast(); l != null; l = (SpawnedLocation) spawntLocs.getPrevious()) {
					if (l.tileX >= netTileX && l.tileX < netTileX + 8 && l.tileZ >= netTileZ && l.tileZ < netTileZ + 8 && l.level == currentLevel) {
						addLoc(l.lastLocIndex, l.level, l.tileX, l.tileZ, l.lastType, l.classtype, l.lastRotation);
						l.unlink();
					}
				}
			} else if (packetType == Packet.SET_DISABLED_WIDGET_MESSAGE) {
				Widget.instances[in.getUShort()].messageDisabled = in.getString();
			} else {
				if (packetType == Packet.PERSUADE_LOGOUT) {
					logout();
					return false;
				}
				if (packetType == Packet.ADD_PRIVATE_MESSAGE) {
					long name = in.getLong();
					int uid = in.getInt();

					boolean ignore = false;
					for (int n = 0; n < 100; n++) {
						if (privateMessageIndex[n] == uid) {
							ignore = true;
							break;
						}
					}

					for (int n = 0; n < ignoreCount; n++) {
						if (ignoreNameLong[n] == name) {
							ignore = true;
							break;
						}
					}

					if (!ignore) {
						privateMessageIndex[privateMessageCount] = uid;
						privateMessageCount = (privateMessageCount + 1) % 100;
						String s = StringBuffer.read(in, packetSize - 12);
						s = Censor.getFiltered(s);
						addMessage(3, StringUtil.getFormatted(StringUtil.fromBase37(name)), s);
					}
				} else if (packetType == Packet.READ_IGNORE_LIST) {
					ignoreCount = packetSize / 8;
					for (int n = 0; n < ignoreCount; n++) {
						ignoreNameLong[n] = in.getLong();
					}
				} else if (packetType == Packet.START_SYSTEM_UPDATE_TIMER) {
					systemUpdate = in.getUShort() * 30;
				} else if (packetType == Packet.SET_CHATBOX_WIDGET) {
					int widgetIndex = in.getUShort();
					resetWidgetAnimations(widgetIndex);

					if (sidebarWidgetIndex != -1) {
						sidebarWidgetIndex = -1;
						sidebarRedraw = true;
						sidebarRedrawIcons = true;
					}

					chatWidgetIndex = widgetIndex;
					chatRedraw = true;
					viewportWidgetIndex = -1;
					chatContinuingDialogue = false;
				} else if (packetType == Packet.SET_DISABLED_WIDGET_MODEL_TO_OBJECT) {
					int widgetIndex = in.getUShort();
					int objectIndex = in.getUShort();
					int zoomModifier = in.getUShort();
					ObjectInfo o = ObjectInfo.get(objectIndex);
					Widget.instances[widgetIndex].modelDisabled = o.getModel();
					Widget.instances[widgetIndex].modelCameraPitch = o.iconCameraPitch;
					Widget.instances[widgetIndex].modelYaw = o.iconYaw;
					Widget.instances[widgetIndex].modelZoom = (o.iconZoom * 100) / zoomModifier;
				} else if (packetType == Packet.SHOW_TRANSFER_INPUT) {
					chatShowDialogueInput = false;
					chatShowTransferInput = true;
					chatTransferInput = "";
					chatRedraw = true;
				} else if (packetType == Packet.SET_SIDEBAR_WIDGET) {
					int widgetIndex = in.getUShort();

					resetWidgetAnimations(widgetIndex);

					if (chatWidgetIndex != -1) {
						chatWidgetIndex = -1;
						chatRedraw = true;
					}

					if (chatShowTransferInput) {
						chatShowTransferInput = false;
						chatRedraw = true;
					}

					sidebarWidgetIndex = widgetIndex;
					sidebarRedraw = true;
					sidebarRedrawIcons = true;
					viewportWidgetIndex = -1;
					chatContinuingDialogue = false;
				}
			}
			packetType = -1;
		} catch (IOException e) {
			reconnect();
			logger.log(Level.SEVERE, "IO error handling packet", e);
		} catch (Exception e) {
			logout();
			logger.log(Level.SEVERE, "Error handling packet: " + lastPacketType, e);
		}
		return true;
	}

	/**
	 * Appends a Location to the landscape. This does not change the shadow map. Using a locIndex of <b>-1</b> removes
	 * any location of the same
	 * <b>type</b>.
	 *
	 * @param index the location index.
	 * @param level the level the location is on.
	 * @param tileX the tile x the location is on.
	 * @param tileZ the tile z the location is on.
	 * @param type the type of the location.
	 * @param classtype the class type of the location.
	 * @param rotation the rotation of the location.
	 */
	public final void addLoc(int index, int level, int tileX, int tileZ, int type, int classtype, int rotation) {
		if (!lowmemory || level == currentLevel) {
			int bitset = 0;
			int lastIndex;

			if (classtype == LocationInfo.CLASS_WALL) {
				bitset = graph.getWallBitset(tileX, tileZ, level);
			}

			if (classtype == LocationInfo.CLASS_WALL_DECORATION) {
				bitset = graph.getWallDecorationBitset(tileX, tileZ, level);
			}

			if (classtype == LocationInfo.CLASS_NORMAL) {
				bitset = graph.getLocationBitset(tileX, tileZ, level);
			}

			if (classtype == LocationInfo.CLASS_GROUND_DECORATION) {
				bitset = graph.getGroundDecorationBitset(tileX, tileZ, level);
			}

			if (bitset != 0) {
				int info = graph.getInfo(tileX, tileZ, level, bitset);
				lastIndex = bitset >> 14 & 0x7fff;

				if (classtype == 0) {
					graph.removeWall(tileX, tileZ, level);
					LocationInfo c = LocationInfo.get(lastIndex);

					if (c.hasCollision) {
						collisions[level].removeWall(tileX, tileZ, info & 0x1F, info >> 6, c.isSolid);
					}
				}

				if (classtype == LocationInfo.CLASS_WALL_DECORATION) {
					graph.removeWallDecoration(tileX, tileZ, level);
				}

				if (classtype == LocationInfo.CLASS_NORMAL) {
					graph.removeLocations(tileX, tileZ, level);
					LocationInfo c = LocationInfo.get(lastIndex);

					if (c.hasCollision) {
						collisions[level].removeLoc(tileX, tileZ, c.sizeX, c.sizeZ, info >> 6, c.isSolid);
					}
				}

				if (classtype == LocationInfo.CLASS_GROUND_DECORATION) {
					graph.removeGroundDecoration(tileX, tileZ, level);
					LocationInfo c = LocationInfo.get(lastIndex);

					if (c.hasCollision && c.interactable) {
						collisions[level].removeBlock(tileX, tileZ);
					}
				}
			}

			if (index >= 0) {
				int l = level;

				// increase level if a bridge tile
				if (l < 3 && (levelRenderFlags[1][tileX][tileZ] & 0x2) == 2) {
					l++;
				}

				Scene.addLoc(type, index, tileX, tileZ, level, l, rotation, levelHeightMaps, graph, collisions[level], animatedLocations);
			}
		}
	}

	public final void readSecondaryPacket(Buffer b, int type) {
		if (type == Packet.ADD_LOCATION || type == Packet.REMOVE_LOCATION) {
			int tile = b.getUByte();
			int x = netTileX + (tile >> 4 & 0x7);
			int z = netTileZ + (tile & 0x7);
			int locInfo = b.getUByte();
			int locType = locInfo >> 2;
			int locRotation = locInfo & 0x3;
			int locClass = LocationInfo.TYPE_TO_CLASS[locType];
			int locIndex;

			if (type == Packet.REMOVE_LOCATION) {
				locIndex = -1;
			} else {
				locIndex = b.getUShort();
			}

			if (x >= 0 && z >= 0 && x < 104 && z < 104) {
				SpawnedLocation loc = null;

				for (SpawnedLocation l = (SpawnedLocation) spawntLocs.peekLast(); l != null; l = (SpawnedLocation) spawntLocs.getPrevious()) {
					if (l.level == currentLevel && l.tileX == x && l.tileZ == z && l.classtype == locClass) {
						loc = l;
						break;
					}
				}

				// if we didn't find any spawnt locs that are the same as our
				// new one
				if (loc == null) {
					int bitset = 0;
					int lastIndex = -1;
					int lastType = 0;
					int lastRotation = 0;

					if (locClass == 0) {
						bitset = graph.getWallBitset(x, z, currentLevel);
					}

					if (locClass == 1) {
						bitset = graph.getWallDecorationBitset(x, z, currentLevel);
					}

					if (locClass == 2) {
						bitset = graph.getLocationBitset(x, z, currentLevel);
					}

					if (locClass == 3) {
						bitset = graph.getGroundDecorationBitset(x, z, currentLevel);
					}

					if (bitset != 0) {
						// save this information for later
						int info = graph.getInfo(x, z, currentLevel, bitset);
						lastIndex = bitset >> 14 & 0x7fff;
						lastType = info & 0x1F;
						lastRotation = info >> 6;
					}

					loc = new SpawnedLocation();
					loc.level = currentLevel;
					loc.classtype = locClass;
					loc.tileX = x;
					loc.tileZ = z;
					loc.lastLocIndex = lastIndex;
					loc.lastType = lastType;
					loc.lastRotation = lastRotation;
					spawntLocs.push(loc);
				}

				loc.locIndex = locIndex;
				loc.type = locType;
				loc.rotation = locRotation;
				addLoc(locIndex, currentLevel, x, z, locType, locClass, locRotation);
			}
		} else if (type == Packet.ADD_ANIMATED_LOCATION) {
			int tile = b.getUByte();
			int x = netTileX + (tile >> 4 & 0x7);
			int z = netTileZ + (tile & 0x7);
			int locInfo = b.getUByte();
			int locType = locInfo >> 2;
			int locClassType = LocationInfo.TYPE_TO_CLASS[locType];
			int animationIndex = b.getUShort();

			if (x >= 0 && z >= 0 && x < 104 && z < 104) {
				int bitset = 0;

				if (locClassType == LocationInfo.CLASS_WALL_DECORATION) {
					bitset = graph.getWallDecorationBitset(x, z, currentLevel);
				}

				if (locClassType == LocationInfo.CLASS_NORMAL) {
					bitset = graph.getLocationBitset(x, z, currentLevel);
				}

				if (bitset != 0) {
					animatedLocations.push(new AnimatedLocation(Animation.instance[animationIndex], bitset >> 14 & 0x7fff, locClassType, x, z, currentLevel));
				}
			}
		} else if (type == Packet.ADD_OBJECT_STACK) {
			int tile = b.getUByte();
			int x = netTileX + (tile >> 4 & 0x7);
			int z = netTileZ + (tile & 0x7);
			int objectIndex = b.getUShort();

			if (x >= 0 && z >= 0 && x < 104 && z < 104) {
				ObjectStack o = new ObjectStack();
				o.index = objectIndex;

				if (levelObjects[currentLevel][x][z] == null) {
					levelObjects[currentLevel][x][z] = new LinkedQueue();
				}

				levelObjects[currentLevel][x][z].push(o);
				updateObjectStack(x, z);
			}
		} else if (type == Packet.REMOVE_OBJECT_STACK) {
			int tile = b.getUByte();
			int x = netTileX + (tile >> 4 & 0x7);
			int z = netTileZ + (tile & 0x7);
			int objectIndex = b.getUShort();

			if (x >= 0 && z >= 0 && x < 104 && z < 104) {
				LinkedQueue stack = levelObjects[currentLevel][x][z];

				if (stack != null) {
					for (ObjectStack s = (ObjectStack) stack.peekLast(); s != null; s = (ObjectStack) stack.getPrevious()) {
						if (s.index == (objectIndex & 0x7fff)) {
							s.unlink();
							break;
						}
					}

					if (stack.peekLast() == null) {
						levelObjects[currentLevel][x][z] = null;
					}
					updateObjectStack(x, z);
				}
			}
		} else if (type == Packet.ADD_PROJECTILE) {
			int tile = b.getUByte();
			int x = netTileX + (tile >> 4 & 0x7);
			int z = netTileZ + (tile & 0x7);
			int dstX = x + b.getByte();
			int dstY = z + b.getByte();
			int targetIndex = b.getShort();
			int spotanimIndex = b.getUShort();
			int offsetY = b.getUByte();
			int baseY = b.getUByte();
			int startCycle = b.getUShort();
			int endCycle = b.getUShort();
			int elevationPitch = b.getUByte();
			int arcScale = b.getUByte();

			if (x >= 0 && z >= 0 && x < 104 && z < 104 && dstX >= 0 && dstY >= 0 && dstX < 104 && dstY < 104) {
				x = x * 128 + 64;
				z = z * 128 + 64;
				dstX = dstX * 128 + 64;
				dstY = dstY * 128 + 64;
				Projectile p = new Projectile(spotanimIndex, targetIndex, x, z, getLandY(x, z, currentLevel) - offsetY, currentLevel, startCycle + cycle, endCycle + cycle, arcScale, elevationPitch, baseY);
				p.setTarget(dstX, dstY, getLandY(dstX, dstY, currentLevel) - baseY, startCycle + cycle);
				projectiles.push(p);
			}
		} else if (type == Packet.ADD_SPOT_ANIMATION) {
			int tile = b.getUByte();
			int x = netTileX + (tile >> 4 & 0x7);
			int z = netTileZ + (tile & 0x7);
			int spotanimIndex = b.getUShort();
			int baseY = b.getUByte();
			int duration = b.getUShort();

			if (x >= 0 && z >= 0 && x < 104 && z < 104) {
				x = x * 128 + 64;
				z = z * 128 + 64;
				SpotAnimationEntity e = new SpotAnimationEntity(x, getLandY(x, z, currentLevel) - baseY, z, currentLevel, spotanimIndex, cycle, duration);
				spotanims.push(e);
			}
		} else if (type == Packet.ADD_PRIVATE_OBJECT_STACK) {
			int tile = b.getUByte();
			int x = netTileX + (tile >> 4 & 0x7);
			int z = netTileZ + (tile & 0x7);
			int objectIndex = b.getUShort();
			int playerIndex = b.getUShort();

			// this baffles me. why is the local player receiving this packet,
			// but not allowed to see the item if it is theirs?
			if (x >= 0 && z >= 0 && x < 104 && z < 104 && playerIndex != localPlayerIndex) {
				ObjectStack stack = new ObjectStack();
				stack.index = objectIndex;

				if (levelObjects[currentLevel][x][z] == null) {
					levelObjects[currentLevel][x][z] = new LinkedQueue();
				}

				levelObjects[currentLevel][x][z].push(stack);
				updateObjectStack(x, z);
			}
		} else if (type == Packet.ATTACH_TEMPORARY_LOCATION_TO_PLAYER) {
			int tile = b.getUByte();
			int x = netTileX + (tile >> 4 & 0x7);
			int z = netTileZ + (tile & 0x7);
			int locInfo = b.getUByte();
			int locType = locInfo >> 2;
			int locRotation = locInfo & 0x3;
			int locClass = LocationInfo.TYPE_TO_CLASS[locType];
			int locIndex = b.getUShort();
			int locStartCycle = b.getUShort();
			int locEndCycle = b.getUShort();
			int playerIndex = b.getUShort();
			int minTileX = b.getByte();
			int minTileZ = b.getByte();
			int maxTileX = b.getByte();
			int maxTileZ = b.getByte();

			Player p;

			if (playerIndex == localPlayerIndex) {
				p = localPlayer;
			} else {
				p = players[playerIndex];
			}

			if (p != null) {
				temporaryLocs.push(new TemporaryLocation(-1, x, z, currentLevel, locType, locRotation, locClass, locStartCycle + cycle));
				temporaryLocs.push(new TemporaryLocation(locIndex, x, z, currentLevel, locType, locRotation, locClass, locEndCycle + cycle));

				int southwestY = levelHeightMaps[currentLevel][x][z];
				int southeastY = levelHeightMaps[currentLevel][x + 1][z];
				int northeastY = levelHeightMaps[currentLevel][x + 1][z + 1];
				int northwestY = levelHeightMaps[currentLevel][x][z + 1];

				LocationInfo l = LocationInfo.get(locIndex);
				p.locFirstCycle = locStartCycle + cycle;
				p.locLastCycle = locEndCycle + cycle;
				p.locModel = l.getModel(locType, locRotation, southwestY, southeastY, northeastY, northwestY, -1);

				int sizeX = l.sizeX;
				int sizeZ = l.sizeZ;

				if (locRotation == 1 || locRotation == 3) {
					sizeX = l.sizeZ;
					sizeZ = l.sizeX;
				}

				p.locSceneX = x * 128 + sizeX * 64;
				p.locSceneZ = z * 128 + sizeZ * 64;
				p.locSceneY = getLandY(p.locSceneX, p.locSceneZ, currentLevel);

				if (minTileX > maxTileX) {
					int tx = minTileX;
					minTileX = maxTileX;
					maxTileX = tx;
				}

				if (minTileZ > maxTileZ) {
					int tz = minTileZ;
					minTileZ = maxTileZ;
					maxTileZ = tz;
				}

				p.locMinTileX = x + minTileX;
				p.locMaxTileX = x + maxTileX;
				p.locMinTileZ = z + minTileZ;
				p.locMaxTileZ = z + maxTileZ;
			}
		}
	}

	public final void updateObjectStack(int x, int y) {
		LinkedQueue stacks = levelObjects[currentLevel][x][y];

		if (stacks == null) {
			graph.removeObject(x, y, currentLevel);
		} else {
			int maxPriority = -99999999;
			ObjectInfo topObject = null;

			for (ObjectStack stack = (ObjectStack) stacks.peekLast(); stack != null; stack = (ObjectStack) stacks.getPrevious()) {
				ObjectInfo object = ObjectInfo.get(stack.index);
				if (object.priority > maxPriority) {
					maxPriority = object.priority;
					topObject = object;
				}
			}

			if (topObject != null) {
				int bitset = x + (y << 7) + 0x60000000;
				graph.addObject(topObject.getModel(), currentLevel, x, y, getLandY(x * 128 + 64, y * 128 + 64, currentLevel), bitset);
			}
		}
	}

	public int updateCount;

	public final void updateLocalPlayer(Buffer b) {
		b.startBitAccess();
		currentLevel = b.getBits(2);
		int tileX = b.getBits(7);
		int tileZ = b.getBits(7);

		if (b.getBits(1) == 1) {
			entityUpdateIndices[updateCount++] = LOCALPLAYER_INDEX;
		}

		localPlayer.setPosition(tileX, tileZ);
	}

	public final void updatePlayers(Buffer b) {
		deadEntityCount = 0;

		int count = b.getBits(8);

		if (count < entityCount) {
			for (int n = count; n < playerCount; n++) {
				deadEntityIndices[deadEntityCount++] = playerIndices[n];
				players[playerIndices[n]].remove = true;
			}
		}

		playerCount = 0;

		for (int n = 0; n < count; n++) {
			int index = playerIndices[n];
			Player p = players[index];

			boolean noUpdate = b.getBits(1) == 0;

			if (noUpdate) {
				playerIndices[playerCount++] = index;
			} else {
				int type = b.getBits(2);

				if (type == 0) {
					playerIndices[playerCount++] = index;
					entityUpdateIndices[updateCount++] = index;
				} else if (type == 1 || type == 2) {
					if (type == 2) {
						entityUpdateIndices[updateCount++] = index;
					}

					playerIndices[playerCount++] = index;
					int direction = b.getBits(3);

					if (direction == 0) {
						p.moveBy(-1, 1);
					} else if (direction == 1) {
						p.moveBy(0, 1);
					} else if (direction == 2) {
						p.moveBy(1, 1);
					} else if (direction == 3) {
						p.moveBy(-1, 0);
					} else if (direction == 4) {
						p.moveBy(1, 0);
					} else if (direction == 5) {
						p.moveBy(-1, -1);
					} else if (direction == 6) {
						p.moveBy(0, -1);
					} else if (direction == 7) {
						p.moveBy(1, -1);
					}
				} else if (type == 3) {
					deadEntityIndices[deadEntityCount++] = index;
					p.remove = true;
				}
			}
		}
	}

	public void updateNewPlayers(Buffer b, int size) {
		for (;;) {
			int index = b.getBits(11);

			if (index == 2047 || (b.bitPosition + 10 >= size * 8)) {
				break;
			}

			if (players[index] == null) {
				players[index] = new Player();

				if (playerBuffers[index] != null) {
					players[index].read(playerBuffers[index]);
				}
			}

			playerIndices[playerCount++] = index;
			Player p = players[index];
			p.remove = false;
			int x = b.getBits(5);

			if (x > 15) {
				x -= 32;
			}

			int y = b.getBits(5);

			if (y > 15) {
				y -= 32;
			}

			p.setPosition(localPlayer.pathX[0] + x, localPlayer.pathY[0] + y);
			entityUpdateIndices[updateCount++] = index;
		}
	}

	public void updatePlayerMasks(Buffer b) {
		b.startByteAccess();

		for (int n = 0; n < updateCount; n++) {
			int index = entityUpdateIndices[n];
			Player p = players[index];

			int mask = b.getUByte();

			if ((mask & 0x80) == 0x80) {
				mask |= b.getUByte() << 8;
			}

			if ((mask & 0x1) == 0x1) {
				Buffer buffer = new Buffer(new byte[b.getUByte()]);
				b.getBytes(buffer.data, 0, buffer.data.length);
				playerBuffers[index] = buffer;
				p.read(buffer);
			}

			if ((mask & 0x2) == 0x2) {
				int animIndex = b.getUShort();

				if (animIndex == 0xFFFF) {
					animIndex = -1;
				}

				if (animIndex == p.primaryAnimIndex) {
					p.primaryAnimDelta = 0;
				}

				int delay = b.getUByte();

				if (animIndex == -1 || p.primaryAnimIndex == -1 || Animation.instance[animIndex].priority > Animation.instance[p.primaryAnimIndex].priority) {
					p.primaryAnimIndex = animIndex;
					p.primaryAnimFrame = 0;
					p.primaryAnimCycle = 0;
					p.primaryAnimDelay = delay;
					p.primaryAnimDelta = 0;
				}
			}

			if ((mask & 0x4) == 0x4) {
				p.targetEntity = b.getUShort();

				if (p.targetEntity == 0xFFFF) {
					p.targetEntity = -1;
				}
			}

			if ((mask & 0x8) == 0x8) {
				p.spoken = b.getString();
				p.spokenColor = 0;
				p.spokenEffect = 0;
				p.spokenLife = 150;
				addMessage(2, p.name, p.spoken);
			}

			if ((mask & 0x10) == 0x10) {
				p.damageTaken = b.getUByte();
				p.damageType = b.getUByte();
				p.lastCombatCycle = cycle + 400;
				p.currentHealth = b.getUByte();
				p.maxHealth = b.getUByte();
			}

			if ((mask & 0x20) == 0x20) {
				p.focusX = b.getUShort();
				p.focusY = b.getUShort();
			}

			if ((mask & 0x40) == 0x40) {
				int info = b.getUShort();
				int length = b.getUByte();
				long longName = StringUtil.toBase37(p.name);

				boolean ignore = false;
				for (int m = 0; m < ignoreCount; m++) {
					if (ignoreNameLong[m] == longName) {
						ignore = true;
						break;
					}
				}

				if (!ignore) {
					String s = StringBuffer.read(b, length);
					s = Censor.getFiltered(s);
					p.spoken = s;
					p.spokenColor = info >> 8;
					p.spokenEffect = info & 0xff;
					p.spokenLife = 150;
					addMessage(2, p.name, s);
				}
			}

			if ((mask & 0x100) == 0x100) {
				p.spotanimIndex = b.getUShort();
				int info = b.getInt();
				p.spotanimOffsetY = info >> 16;
				p.lastSpotanimCycle = cycle + (info & 0xFFFF);
				p.spotanimFrame = 0;
				p.spotanimCycle = 0;

				if (p.lastSpotanimCycle > cycle) {
					p.spotanimFrame = -1;
				}

				if (p.spotanimIndex == 0xFFFF) {
					p.spotanimIndex = -1;
				}
			}

			if ((mask & 0x200) == 0x200) {
				p.srcTileX = b.getUByte();
				p.srcTileY = b.getUByte();
				p.dstTileX = b.getUByte();
				p.dstTileY = b.getUByte();
				p.firstMoveCycle = b.getUShort() + cycle;
				p.lastMoveCycle = b.getUShort() + cycle;
				p.faceDirection = b.getUByte();
				p.pathStepCount = 0;
				p.pathX[0] = p.dstTileX;
				p.pathY[0] = p.dstTileY;
			}
		}
	}

	public final void updatePlayers(Buffer b, int size) {
		updateCount = 0;

		updateLocalPlayer(b);
		updatePlayers(b);
		updateNewPlayers(b, size);
		updatePlayerMasks(b);

		for (int n = 0; n < deadEntityCount; n++) {
			int i = deadEntityIndices[n];

			if (players[i].remove) {
				players[i] = null;
			}
		}
	}

	public final void updateNPCs(Buffer b, int packetSize) {
		deadEntityCount = 0;
		b.startBitAccess();

		int count = b.getBits(8);

		if (count < entityCount) {
			for (int n = count; n < entityCount; n++) {
				deadEntityIndices[deadEntityCount++] = npcIndices[n];
				npcs[npcIndices[n]].remove = true;
			}
		}

		updateCount = 0;
		entityCount = 0;

		for (int n = 0; n < count; n++) {
			int index = npcIndices[n];
			NPC npc = npcs[index];

			if (b.getBits(1) == 0) {
				npcIndices[entityCount++] = index;
			} else {
				int type = b.getBits(2);

				if (type == 3) {
					deadEntityIndices[deadEntityCount++] = index;
					npc.remove = true;
				} else {
					npcIndices[entityCount++] = index;

					if (type == 0) {
						entityUpdateIndices[updateCount++] = index;
					} else {
						if (type == 2) {
							entityUpdateIndices[updateCount++] = index;
						}
						int dir = b.getBits(3);

						if (dir == 0) {
							npc.moveBy(-1, 1);
						} else if (dir == 1) {
							npc.moveBy(0, 1);
						} else if (dir == 2) {
							npc.moveBy(1, 1);
						} else if (dir == 3) {
							npc.moveBy(-1, 0);
						} else if (dir == 4) {
							npc.moveBy(1, 0);
						} else if (dir == 5) {
							npc.moveBy(-1, -1);
						} else if (dir == 6) {
							npc.moveBy(0, -1);
						} else if (dir == 7) {
							npc.moveBy(1, -1);
						}
					}
				}
			}
		}

		for (;;) {
			int index = b.getBits(13);

			if (index == 8191 || b.bitPosition + 21 >= packetSize * 8) {
				break;
			}

			npcIndices[entityCount++] = index;

			if (npcs[index] == null) {
				npcs[index] = new NPC();
			}

			NPC n = npcs[index];
			n.remove = false;
			n.config = NPCInfo.get(b.getBits(11));
			n.size = n.config.size;
			n.animWalkIndex = n.config.animWalkIndex;
			n.animTurnBackIndex = n.config.animRunIndex;
			n.animTurnRightIndex = n.config.animTurnRightIndex;
			n.animTurnLeftIndex = n.config.animTurnLeftIndex;
			n.animStand = n.config.animStandIndex;

			int x = b.getBits(5);

			if (x > 15) {
				x -= 32;
			}

			int y = b.getBits(5);

			if (y > 15) {
				y -= 32;
			}

			n.setPosition(localPlayer.pathX[0] + x, localPlayer.pathY[0] + y);

			if (b.getBits(1) == 1) {
				entityUpdateIndices[updateCount++] = index;
			}
		}

		b.startByteAccess();

		for (int n = 0; n < updateCount; n++) {
			int index = entityUpdateIndices[n];
			NPC npc = npcs[index];

			int mask = b.getUByte();

			if ((mask & 0x2) == 2) {
				int animIndex = b.getUShort();

				if (animIndex == 0xFFFF) {
					animIndex = -1;
				}

				if (animIndex == npc.primaryAnimIndex) {
					npc.primaryAnimDelta = 0;
				}

				int delay = b.getUByte();

				if (animIndex == -1 || npc.primaryAnimIndex == -1 || (Animation.instance[animIndex].priority > (Animation.instance[npc.primaryAnimIndex].priority))) {
					npc.primaryAnimIndex = animIndex;
					npc.primaryAnimFrame = 0;
					npc.primaryAnimCycle = 0;
					npc.primaryAnimDelay = delay;
					npc.primaryAnimDelta = 0;
				}
			}

			if ((mask & 0x4) == 4) {
				npc.targetEntity = b.getUShort();

				if (npc.targetEntity == 0xFFFF) {
					npc.targetEntity = -1;
				}
			}

			if ((mask & 0x8) == 8) {
				npc.spoken = b.getString();
				npc.spokenLife = 100;
			}

			if ((mask & 0x10) == 16) {
				npc.damageTaken = b.getUByte();
				npc.damageType = b.getUByte();
				npc.lastCombatCycle = cycle + 400;
				npc.currentHealth = b.getUByte();
				npc.maxHealth = b.getUByte();
			}

			if ((mask & 0x20) == 32) {
				npc.config = NPCInfo.get(b.getUShort());
				npc.animWalkIndex = npc.config.animWalkIndex;
				npc.animTurnBackIndex = npc.config.animRunIndex;
				npc.animTurnRightIndex = npc.config.animTurnRightIndex;
				npc.animTurnLeftIndex = npc.config.animTurnLeftIndex;
				npc.animStand = npc.config.animStandIndex;
			}

			if ((mask & 0x40) == 64) {
				npc.spotanimIndex = b.getUShort();
				int bitset = b.getInt();
				npc.spotanimOffsetY = bitset >> 16;
				npc.lastSpotanimCycle = cycle + (bitset & 0xffff);
				npc.spotanimFrame = 0;
				npc.spotanimCycle = 0;

				if (npc.lastSpotanimCycle > cycle) {
					npc.spotanimFrame = -1;
				}

				if (npc.spotanimIndex == 0xFFFF) {
					npc.spotanimIndex = -1;
				}
			}
		}

		for (int n = 0; n < deadEntityCount; n++) {
			int index = deadEntityIndices[n];

			if (npcs[index].remove) {
				npcs[index].config = null;
				npcs[index] = null;
			}
		}
	}

	public void drawTooltip() {
		if (optionCount >= 2 || selectedObject || selectedSpell) {
			String s;

			if (selectedObject && optionCount < 2) {
				s = "Use " + selectedObjName + " with...";
			} else if (selectedSpell && optionCount < 2) {
				s = selectedSpellPrefix + "...";
			} else {
				s = options[optionCount - 1];
			}

			if (optionCount > 2) {
				s += "@whi@ / " + (optionCount - 2) + " more options";
			}

			fontBold.drawTaggable(s, 4, 15, 0xFFFFFF, true);
		}
	}

	public void drawOptionMenu() {
		int x = optionMenuX;
		int y = optionMenuY;
		int w = optionMenuWidth;
		int h = optionMenuHeight;
		int bgrgb = 0x5D5447;

		Graphics2D.fillRect(x, y, w, h, bgrgb);
		Graphics2D.fillRect(x + 1, y + 1, w - 2, 16, 0);
		Graphics2D.drawRect(x + 1, y + 18, w - 2, h - 19, 0);
		fontBold.draw("Choose Option", x + 3, y + 14, bgrgb);

		int mx = mouseX;
		int my = mouseY;

		if (optionMenuArea == 0) {
			mx -= 8;
			my -= 11;
		}

		if (optionMenuArea == 1) {
			mx -= 562;
			my -= 231;
		}

		for (int n = 0; n < optionCount; n++) {
			int optionY = y + 31 + (optionCount - 1 - n) * 15;
			int fontrgb = 0xFFFFFF;

			if (mx > x && mx < x + w && my > optionY - 13 && my < optionY + 3) {
				fontrgb = 0xFFFF00;
			}

			fontBold.drawTaggable(options[n], x + 3, optionY, fontrgb, true);
		}
	}

	public void updateOptionMenu() {
		int button = mouseButton;

		if (selectedSpell && clickX >= 520 && clickY >= 165 && clickX <= 788 && clickY <= 230) {
			button = 0;
		}

		if (optionMenuVisible) {
			if (button != 1) {
				int mx = mouseX;
				int my = mouseY;

				if (optionMenuArea == 0) {
					mx -= 8;
					my -= 11;
				}

				if (optionMenuArea == 1) {
					mx -= 562;
					my -= 231;
				}

				if (mx < optionMenuX - 10 || mx > optionMenuX + optionMenuWidth + 10 || my < optionMenuY - 10 || my > optionMenuY + optionMenuHeight + 10) {
					optionMenuVisible = false;

					if (optionMenuArea == 1) {
						sidebarRedraw = true;
					}
				}
			}

			if (button == 1) {
				int x = optionMenuX;
				int y = optionMenuY;
				int w = optionMenuWidth;
				int cx = clickX;
				int cy = clickY;

				if (optionMenuArea == 0) {
					cx -= 8;
					cy -= 11;
				}

				if (optionMenuArea == 1) {
					cx -= 562;
					cy -= 231;
				}

				int option = -1;
				for (int n = 0; n < optionCount; n++) {
					int optionY = y + 31 + (optionCount - 1 - n) * 15;

					if (cx > x && cx < x + w && cy > optionY - 13 && cy < optionY + 3) {
						option = n;
					}
				}

				if (option != -1) {
					useOption(option);
				}

				optionMenuVisible = false;

				if (optionMenuArea == 1) {
					sidebarRedraw = true;
				}
			}
		} else {
			if (button == 1 && mouseOneButton && optionCount > 2) {
				button = 2;
			}

			if (button == 1 && optionCount > 0) {
				useOption(optionCount - 1);
			}

			if (button == 2 && optionCount > 0) {
				int maxWidth = fontBold.stringWidth("Choose Option");

				for (int n = 0; n < optionCount; n++) {
					int w = fontBold.stringWidth(options[n]);
					if (w > maxWidth) {
						maxWidth = w;
					}
				}

				maxWidth += 8;

				int h = (optionCount * 15) + 21;

				if (clickX > 8 && clickY > 11 && clickX < 520 && clickY < 345) {
					int x = clickX - 8 - maxWidth / 2;

					if (x + maxWidth > 512) {
						x = 512 - maxWidth;
					}

					if (x < 0) {
						x = 0;
					}

					int y = clickY - 11;

					if (y + h > 334) {
						y = 334 - h;
					}

					if (y < 0) {
						y = 0;
					}

					optionMenuVisible = true;
					optionMenuArea = 0;
					optionMenuX = x;
					optionMenuY = y;
					optionMenuWidth = maxWidth;
					optionMenuHeight = optionCount * 15 + 22;
				}

				if (clickX > 562 && clickY > 231 && clickX < 752 && clickY < 492) {
					int x = clickX - 562 - maxWidth / 2;

					if (x < 0) {
						x = 0;
					} else if (x + maxWidth > 190) {
						x = 190 - maxWidth;
					}

					int y = clickY - 231;

					if (y < 0) {
						y = 0;
					} else if (y + h > 261) {
						y = 261 - h;
					}

					optionMenuVisible = true;
					optionMenuArea = 1;
					optionMenuX = x;
					optionMenuY = y;
					optionMenuWidth = maxWidth;
					optionMenuHeight = optionCount * 15 + 22;
				}
			}
		}
	}

	public void useOption(int option) {
		if (option < 0) {
			return;
		}

		if (chatShowTransferInput) {
			chatShowTransferInput = false;
			chatRedraw = true;
		}

		int a = optionParamA[option];
		int b = optionParamB[option];
		int c = optionParamC[option];
		int type = optionType[option];

		if (type == 636) {
			Player p = players[a];
			if (p != null) {
				moveTo(localPlayer.pathX[0], localPlayer.pathY[0], p.pathX[0], p.pathY[0], 1, 1, 0, 0, 0, false);
				crossX = clickX;
				crossY = clickY;
				crossType = 2;
				crossCycle = 0;
				out.putOpcode(185);
				out.putShort(a);
				out.putShort(selectedObjIndex);
				out.putShort(selectedObjSlot);
				out.putShort(selectedObjInterface);
			}
		}

		if (type == 1294) {
			int locIndex = a >> 14 & 0x7fff;
			LocationInfo l = LocationInfo.get(locIndex);
			String string;

			if (l.description != null) {
				string = new String(l.description);
			} else {
				string = "It's a " + l.name + ".";
			}

			addMessage(0, "", string);
		}

		if (type == 700) {
			out.putOpcode(101);
			out.putShort(c);
			Widget w = Widget.instances[c];

			if (w.script != null && w.script[0][0] == 5) {
				int v = w.script[0][1];
				if (variables[v] != w.scriptCompareValue[0]) {
					variables[v] = w.scriptCompareValue[0];
					updateVarp(v);
					sidebarRedraw = true;
				}
			}
		}

		if (type == 54) {
			interactWithLocation(a, b, c, Packet.THIRD_OBJECT_ACTION);
		}

		if (type == 806) {
			NPC npc = npcs[a];
			if (npc != null) {
				moveTo(localPlayer.pathX[0], localPlayer.pathY[0], npc.pathX[0], npc.pathY[0], 1, 1, 0, 0, 0, false);
				crossX = clickX;
				crossY = clickY;
				crossType = 2;
				crossCycle = 0;
				out.putOpcode(28);
				out.putShort(a);
				out.putShort(selectedObjIndex);
				out.putShort(selectedObjSlot);
				out.putShort(selectedObjInterface);
			}
		}

		if (type == 243) {
			interactWithLocation(a, b, c, 10);
			out.putShort(selectedSpellIndex);
		}

		if (type == 17) {
			Widget w = Widget.instances[c];
			selectedSpell = true;
			selectedSpellIndex = c;
			selectedFlags = w.optionFlags;
			selectedObject = false;

			String prefix = w.optionCircumfix;

			if (prefix.contains(" ")) {
				prefix = prefix.substring(0, prefix.indexOf(" "));
			}

			String suffix = w.optionCircumfix;

			if (suffix.contains(" ")) {
				suffix = suffix.substring(suffix.indexOf(" ") + 1);
			}

			selectedSpellPrefix = prefix + " " + w.optionSuffix + " " + suffix;

			if (selectedFlags == 16) {
				sidebarRedraw = true;
				selectedTab = 3;
				sidebarRedrawIcons = true;
			}
		} else {
			if (type == 284) {
				if (!optionMenuVisible) {
					graph.sendClick(clickX - 8, clickY - 11);
				} else {
					graph.sendClick(b - 8, c - 11);
				}
			}

			if (type == 669) {
				selectedObject = true;
				selectedObjSlot = b;
				selectedObjInterface = c;
				selectedObjIndex = a;
				selectedObjName = ObjectInfo.get(a).name;
				selectedSpell = false;
			} else {
				if (type == 146) {
					interactWithLocation(a, b, c, Packet.FOURTH_OBJECT_ACTION);
				}

				if (type == 237) {
					interactWithLocation(a, b, c, 205);
					out.putShort(selectedObjIndex);
					out.putShort(selectedObjSlot);
					out.putShort(selectedObjInterface);
				}

				if (type == 739) {
					out.putOpcode(101);
					out.putShort(c);
					Widget w = Widget.instances[c];
					if (w.script != null && w.script[0][0] == 5) {
						int j = w.script[0][1];
						variables[j] = 1 - variables[j];
						updateVarp(j);
						sidebarRedraw = true;
					}
				}

				if (type == 710 || type == 301 || type == 328 || type == 498 || type == 74) {
					NPC npc = npcs[a];
					if (npc != null) {
						moveTo((localPlayer.pathX[0]), (localPlayer.pathY[0]), npc.pathX[0], npc.pathY[0], 1, 1, 0, 0, 0, false);
						crossX = clickX;
						crossY = clickY;
						crossType = 2;
						crossCycle = 0;

						if (type == 328) {
							out.putOpcode(107);
						} else if (type == 301) {
							out.putOpcode(152);
						} else if (type == 498) {
							out.putOpcode(119);
						} else if (type == 74) {
							out.putOpcode(8);
						} else if (type == 710) {
							out.putOpcode(41);
						}

						out.putShort(a);
					}
				}

				if (type == 1682 || type == 1930 || type == 1754 || type == 1484) {
					Player p = players[a];
					if (p != null) {
						moveTo((localPlayer.pathX[0]), (localPlayer.pathY[0]), p.pathX[0], p.pathY[0], 1, 1, 0, 0, 0, false);
						crossX = clickX;
						crossY = clickY;
						crossType = 2;
						crossCycle = 0;

						if (type == 1930) {
							out.putOpcode(212);
						} else if (type == 1682) {
							out.putOpcode(192);
						} else if (type == 1484) {
							out.putOpcode(172);
						} else if (type == 1754) {
							out.putOpcode(251);
						}

						out.putShort(a);
					}
				}

				if (type == 462) {
					interactWithLocation(a, b, c, Packet.SECOND_OBJECT_ACTION);
				}

				if (type == 1971 || type == 1258) {
					ObjectInfo o = ObjectInfo.get(a);
					String s;

					if (o.description != null) {
						s = new String(o.description);
					} else {
						s = "It's a " + o.name + ".";
					}

					addMessage(0, "", s);
				}

				if (type == 730) {
					Player p = players[a];

					if (p != null) {
						moveTo((localPlayer.pathX[0]), (localPlayer.pathY[0]), p.pathX[0], p.pathY[0], 1, 1, 0, 0, 0, false);
						crossX = clickX;
						crossY = clickY;
						crossType = 2;
						crossCycle = 0;
						out.putOpcode(252);
						out.putShort(a);
						out.putShort(selectedSpellIndex);
					}
				}

				if (type == 917 || type == 14 || type == 401 || type == 514 || type == 164) {
					if (!moveTo((localPlayer.pathX[0]), (localPlayer.pathY[0]), b, c, 0, 0, 0, 0, 0, false)) {
						moveTo((localPlayer.pathX[0]), (localPlayer.pathY[0]), b, c, 1, 1, 0, 0, 0, false);
					}

					crossX = clickX;
					crossY = clickY;
					crossType = 2;
					crossCycle = 0;

					if (type == 164) {
						out.putOpcode(140);
					} else if (type == 514) {
						out.putOpcode(235);
					} else if (type == 401) {
						out.putOpcode(113);
					} else if (type == 14) {
						out.putOpcode(61);
					} else if (type == 917) {
						out.putOpcode(186);
					}

					out.putShort(b + mapBaseX);
					out.putShort(c + mapBaseY);
					out.putShort(a);
				}

				if (type == 677 || type == 522 || type == 249 || type == 247 || type == 296) {
					if (type == 296) {
						out.putOpcode(38);
					} else if (type == 247) {
						out.putOpcode(155);
					} else if (type == 249) {
						out.putOpcode(146);
					} else if (type == 522) {
						out.putOpcode(240);
					} else if (type == 677) {
						out.putOpcode(121);
					}

					out.putShort(a);
					out.putShort(b);
					out.putShort(c);

					selectedCycle = 0;
					selectedInterfaceIndex = c;
					selectedInterfaceSlot = b;
					selectedArea = 2;

					if (Widget.instances[c].parent == viewportWidgetIndex) {
						selectedArea = 1;
					}
					if (Widget.instances[c].parent == chatWidgetIndex) {
						selectedArea = 3;
					}
				}

				if (type == 883 && !chatContinuingDialogue) {
					out.putOpcode(167);
					out.putShort(c);
					chatContinuingDialogue = true;
				}

				if (type == 754) {
					interactWithLocation(a, b, c, Packet.FIFTH_OBJECT_ACTION);
				}

				if (type == 39) {
					out.putOpcode(168);
					out.putShort(a);
					out.putShort(b);
					out.putShort(c);
					out.putShort(selectedObjIndex);
					out.putShort(selectedObjSlot);
					out.putShort(selectedObjInterface);
					selectedCycle = 0;
					selectedInterfaceIndex = c;
					selectedInterfaceSlot = b;
					selectedArea = 2;

					if (Widget.instances[c].parent == viewportWidgetIndex) {
						selectedArea = 1;
					}

					if (Widget.instances[c].parent == chatWidgetIndex) {
						selectedArea = 3;
					}
				}

				if (type == 981) {
					interactWithLocation(a, b, c, Packet.FIRST_OBJECT_ACTION);
				}

				if (type == 454) {
					out.putOpcode(213);

					if (sidebarWidgetIndex != -1) {
						sidebarWidgetIndex = -1;
						sidebarRedraw = true;
						chatContinuingDialogue = false;
						sidebarRedrawIcons = true;
					}

					if (chatWidgetIndex != -1) {
						chatWidgetIndex = -1;
						chatRedraw = true;
						chatContinuingDialogue = false;
					}

					viewportWidgetIndex = -1;
				}

				if (type == 759) {
					Widget w = Widget.instances[c];
					boolean write = true;

					if (w.action > 0) {
						write = useWidgetAction(w);
					}

					if (write) {
						out.putOpcode(101);
						out.putShort(c);
					}
				}

				if (type == 160) {
					if (!moveTo((localPlayer.pathX[0]), (localPlayer.pathY[0]), b, c, 0, 0, 0, 0, 0, false)) {
						moveTo((localPlayer.pathX[0]), (localPlayer.pathY[0]), b, c, 1, 1, 0, 0, 0, false);
					}

					crossX = clickX;
					crossY = clickY;
					crossType = 2;
					crossCycle = 0;
					out.putOpcode(42);
					out.putShort(b + mapBaseX);
					out.putShort(c + mapBaseY);
					out.putShort(a);
					out.putShort(selectedObjIndex);
					out.putShort(selectedObjSlot);
					out.putShort(selectedObjInterface);
				}

				if (type == 678 || type == 523 || type == 836 || type == 548 || type == 62) {
					if (type == 548) {
						out.putOpcode(21);
					} else if (type == 523) {
						out.putOpcode(181);
					} else if (type == 836) {
						out.putOpcode(145);
					} else if (type == 678) {
						out.putOpcode(175);
					} else if (type == 62) {
						out.putOpcode(47);
					}

					out.putShort(a);
					out.putShort(b);
					out.putShort(c);

					selectedCycle = 0;
					selectedInterfaceIndex = c;
					selectedInterfaceSlot = b;
					selectedArea = 2;

					if (Widget.instances[c].parent == viewportWidgetIndex) {
						selectedArea = 1;
					}

					if (Widget.instances[c].parent == chatWidgetIndex) {
						selectedArea = 3;
					}
				}

				if (type == 130) {
					NPC npc = npcs[a];
					if (npc != null) {
						moveTo((localPlayer.pathX[0]), (localPlayer.pathY[0]), npc.pathX[0], npc.pathY[0], 1, 1, 0, 0, 0, false);
						crossX = clickX;
						crossY = clickY;
						crossType = 2;
						crossCycle = 0;
						out.putOpcode(189);
						out.putShort(a);
						out.putShort(selectedSpellIndex);
					}
				}
				if (type == 1725) {
					NPC npc = npcs[a];

					if (npc != null) {
						String s;
						if (npc.config.description != null) {
							s = new String(npc.config.description);
						} else {
							s = "It's a " + npc.config.name + ".";
						}
						addMessage(0, "", s);
					}
				}

				if (type == 449) {
					out.putOpcode(247);
					out.putShort(a);
					out.putShort(b);
					out.putShort(c);
					out.putShort(selectedSpellIndex);
					selectedCycle = 0;
					selectedInterfaceIndex = c;
					selectedInterfaceSlot = b;
					selectedArea = 2;

					if (Widget.instances[c].parent == viewportWidgetIndex) {
						selectedArea = 1;
					}

					if (Widget.instances[c].parent == chatWidgetIndex) {
						selectedArea = 3;
					}
				}

				if (type == 504) {
					if (!moveTo(localPlayer.pathX[0], localPlayer.pathY[0], b, c, 0, 0, 0, 0, 0, false)) {
						moveTo(localPlayer.pathX[0], localPlayer.pathY[0], b, c, 1, 1, 0, 0, 0, false);
					}

					crossX = clickX;
					crossY = clickY;
					crossType = 2;
					crossCycle = 0;

					out.putOpcode(244);
					out.putShort(b + mapBaseX);
					out.putShort(c + mapBaseY);
					out.putShort(a);
					out.putShort(selectedSpellIndex);
				}

				selectedObject = false;
				selectedSpell = false;
			}
		}
	}

	public void addOption(String s, int type, int a, int b, int c) {
		options[optionCount] = s;
		optionType[optionCount] = type;
		optionParamA[optionCount] = a;
		optionParamB[optionCount] = b;
		optionParamC[optionCount++] = c;
	}

	public void updateInput() {
		options[0] = "Cancel";
		optionType[0] = 1264;
		optionCount = 1;

		if (mouseX > 8 && mouseY > 11 && mouseX < 520 && mouseY < 345) {
			if (viewportWidgetIndex != -1) {
				updateWidget(Widget.instances[viewportWidgetIndex], 8, 11, mouseX, mouseY, 0);
			} else {
				updateViewport();
			}
		}

		if (hoveredInterfaceIndex != viewportHoveredInterfaceIndex) {
			viewportHoveredInterfaceIndex = hoveredInterfaceIndex;
		}

		hoveredInterfaceIndex = 0;

		if (mouseX > 562 && mouseY > 231 && mouseX < 752 && mouseY < 492) {
			if (sidebarWidgetIndex != -1) {
				updateWidget(Widget.instances[sidebarWidgetIndex], 562, 231, mouseX, mouseY, 0);
			} else if (tabWidgetIndex[selectedTab] != -1) {
				updateWidget(Widget.instances[tabWidgetIndex[selectedTab]], 562, 231, mouseX, mouseY, 0);
			}
		}

		if (hoveredInterfaceIndex != sidebarHoveredInterfaceIndex) {
			sidebarRedraw = true;
			sidebarHoveredInterfaceIndex = hoveredInterfaceIndex;
		}

		hoveredInterfaceIndex = 0;

		if (mouseX > 22 && mouseY > 375 && mouseX < 501 && mouseY < 471 && chatWidgetIndex != -1) {
			updateWidget(Widget.instances[chatWidgetIndex], 22, 375, mouseX, mouseY, 0);
		}

		if (chatWidgetIndex != -1 && hoveredInterfaceIndex != chatHoveredInterfaceIndex) {
			chatRedraw = true;
			chatHoveredInterfaceIndex = hoveredInterfaceIndex;
		}

		sortOptions();
	}

	public final void sortOptions() {
		boolean sorted = false;
		while (!sorted) {
			sorted = true;
			for (int n = 0; n < optionCount - 1; n++) {
				if (optionType[n] < 1000 && optionType[n + 1] > 1000) {
					String s = options[n];
					options[n] = options[n + 1];
					options[n + 1] = s;

					int type = optionType[n];
					optionType[n] = optionType[n + 1];
					optionType[n + 1] = type;

					type = optionParamB[n];
					optionParamB[n] = optionParamB[n + 1];
					optionParamB[n + 1] = type;

					type = optionParamC[n];
					optionParamC[n] = optionParamC[n + 1];
					optionParamC[n + 1] = type;

					type = optionParamA[n];
					optionParamA[n] = optionParamA[n + 1];
					optionParamA[n + 1] = type;
					sorted = false;
				}
			}
		}
	}

	public final void updateViewport() {
		if (!selectedObject && !selectedSpell) {
			options[optionCount] = "Walk here";
			optionType[optionCount] = 284;
			optionParamB[optionCount] = mouseX;
			optionParamC[optionCount] = mouseY;
			optionCount++;
		}

		for (int i = 0; i < Model.hoverCount; i++) {
			int bitset = Model.hoveredBitsets[i];
			int x = bitset & 0x7F;
			int z = (bitset >> 7) & 0x7F;
			int index = (bitset >> 14) & 0x7FFF;
			int type = (bitset >> 29) & 0x3;

			if (type == 2 && graph.getInfo(x, z, currentLevel, bitset) >= 0) {
				LocationInfo l = LocationInfo.get(index);

				if (selectedObject) {
					options[optionCount] = "Use " + selectedObjName + " with @cya@" + l.name;
					optionType[optionCount] = 237;
					optionParamA[optionCount] = bitset;
					optionParamB[optionCount] = x;
					optionParamC[optionCount] = z;
					optionCount++;
				} else if (selectedSpell) {
					if ((selectedFlags & 0x4) == 4) {
						options[optionCount] = selectedSpellPrefix + " @cya@" + l.name;
						optionType[optionCount] = 243;
						optionParamA[optionCount] = bitset;
						optionParamB[optionCount] = x;
						optionParamC[optionCount] = z;
						optionCount++;
					}
				} else {
					if (l.actions != null) {
						for (int n = 4; n >= 0; n--) {
							if (l.actions[n] != null) {
								options[optionCount] = (l.actions[n] + " @cya@" + l.name);
								if (n == 0) {
									optionType[optionCount] = 981;
								} else if (n == 1) {
									optionType[optionCount] = 462;
								} else if (n == 2) {
									optionType[optionCount] = 54;
								} else if (n == 3) {
									optionType[optionCount] = 146;
								} else if (n == 4) {
									optionType[optionCount] = 754;
								}
								optionParamA[optionCount] = bitset;
								optionParamB[optionCount] = x;
								optionParamC[optionCount] = z;
								optionCount++;
							}
						}
					}
					options[optionCount] = "Examine @cya@" + l.name;
					optionType[optionCount] = 1294;
					optionParamA[optionCount] = bitset;
					optionParamB[optionCount] = x;
					optionParamC[optionCount] = z;
					optionCount++;
				}
			}

			if (type == 1) {
				NPC npc = npcs[index];

				// if npc is standing in the very center of its tile
				if (npc.config.size == 1 && (npc.sceneX & 0x7f) == 64 && (npc.sceneZ & 0x7f) == 64) {
					for (int n = 0; n < entityCount; n++) {
						NPC npc1 = npcs[npcIndices[n]];
						if (npc1 != null && (npc1 != npc) && (npc1.config.size) == 1 && (npc1.sceneX == npc.sceneX) && (npc1.sceneZ == npc.sceneZ)) {
							parseNPCOptions(npc1.config, x, z, npcIndices[n]);
						}
					}
				}

				parseNPCOptions(npc.config, x, z, index);
			}

			if (type == 0) {
				Player p = players[index];

				if ((p.sceneX & 0x7f) == 64 && (p.sceneZ & 0x7f) == 64) {
					for (int n = 0; n < entityCount; n++) {
						NPC npc = npcs[npcIndices[n]];
						if (npc != null && (npc.config.size == 1) && (npc.sceneX == p.sceneX) && (npc.sceneZ == p.sceneZ)) {
							parseNPCOptions(npc.config, x, z, npcIndices[n]);
						}
					}

					for (int n = 0; n < playerCount; n++) {
						Player p1 = players[playerIndices[n]];

						if (p1 != null && (p1 != p) && (p1.sceneX == p.sceneX) && (p1.sceneZ == p.sceneZ)) {
							parsePlayerOptions(p1, x, z, playerIndices[n]);
						}
					}
				}
				parsePlayerOptions(p, x, z, index);
			}

			if (type == 3) {
				LinkedQueue stack = levelObjects[currentLevel][x][z];

				if (stack != null) {
					for (ObjectStack o = (ObjectStack) stack.peekFirst(); o != null; o = (ObjectStack) stack.getNext()) {
						ObjectInfo c = ObjectInfo.get(o.index);

						if (selectedObject) {
							options[optionCount] = ("Use " + selectedObjName + " with @lre@" + c.name);
							optionType[optionCount] = 160;
							optionParamA[optionCount] = o.index;
							optionParamB[optionCount] = x;
							optionParamC[optionCount] = z;
							optionCount++;
						} else if (selectedSpell) {
							if ((selectedFlags & 0x1) == 1) {
								options[optionCount] = (selectedSpellPrefix + " @lre@" + c.name);
								optionType[optionCount] = 504;
								optionParamA[optionCount] = o.index;
								optionParamB[optionCount] = x;
								optionParamC[optionCount] = z;
								optionCount++;
							}
						} else {
							for (int n = 4; n >= 0; n--) {
								if (c.groundActions != null && (c.groundActions[n] != null)) {
									options[optionCount] = (c.groundActions[n] + " @lre@" + c.name);

									if (n == 0) {
										optionType[optionCount] = 917;
									} else if (n == 1) {
										optionType[optionCount] = 14;
									} else if (n == 2) {
										optionType[optionCount] = 401;
									} else if (n == 3) {
										optionType[optionCount] = 514;
									} else if (n == 4) {
										optionType[optionCount] = 164;
									}

									optionParamA[optionCount] = o.index;
									optionParamB[optionCount] = x;
									optionParamC[optionCount] = z;
									optionCount++;
								} else if (n == 2) {
									options[optionCount] = "Take @lre@" + c.name;
									optionType[optionCount] = 401;
									optionParamA[optionCount] = o.index;
									optionParamB[optionCount] = x;
									optionParamC[optionCount] = z;
									optionCount++;
								}
							}
							options[optionCount] = "Examine @lre@" + c.name;
							optionType[optionCount] = 1971;
							optionParamA[optionCount] = o.index;
							optionParamB[optionCount] = x;
							optionParamC[optionCount] = z;
							optionCount++;
						}
					}
				}
			}
		}
	}

	public final void parseNPCOptions(NPCInfo npc, int x, int z, int info) {
		if (optionCount < 400) {
			String name = npc.name;

			if (npc.level != 0) {
				name += getLevelColorTag(localPlayer.level, npc.level) + " (level-" + npc.level + ")";
			}

			if (selectedObject) {
				options[optionCount] = "Use " + selectedObjName + " with @yel@" + name;
				optionType[optionCount] = 806;
				optionParamA[optionCount] = info;
				optionParamB[optionCount] = x;
				optionParamC[optionCount] = z;
				optionCount++;
			} else if (selectedSpell) {
				if ((selectedFlags & 0x2) == 2) {
					options[optionCount] = selectedSpellPrefix + " @yel@" + name;
					optionType[optionCount] = 130;
					optionParamA[optionCount] = info;
					optionParamB[optionCount] = x;
					optionParamC[optionCount] = z;
					optionCount++;
				}
			} else {
				if (npc.actions != null) {
					for (int n = 4; n >= 0; n--) {
						if (npc.actions[n] != null) {
							options[optionCount] = npc.actions[n] + " @yel@" + name;
							if (n == 0) {
								optionType[optionCount] = 710;
							} else if (n == 1) {
								optionType[optionCount] = 301;
							} else if (n == 2) {
								optionType[optionCount] = 328;
							} else if (n == 3) {
								optionType[optionCount] = 498;
							} else if (n == 4) {
								optionType[optionCount] = 74;
							}
							optionParamA[optionCount] = info;
							optionParamB[optionCount] = x;
							optionParamC[optionCount] = z;
							optionCount++;
						}
					}
				}
				options[optionCount] = "Examine @yel@" + name;
				optionType[optionCount] = 1725;
				optionParamA[optionCount] = info;
				optionParamB[optionCount] = x;
				optionParamC[optionCount] = z;
				optionCount++;
			}
		}
	}

	private void parsePlayerOptions(Player p, int x, int z, int index) {
		if (p != localPlayer && optionCount < 400) {
			String string = p.name + getLevelColorTag(localPlayer.level, p.level) + " (level-" + p.level + ")";

			if (selectedObject) {
				options[optionCount] = "Use " + selectedObjName + " with @whi@" + string;
				optionType[optionCount] = 636;
				optionParamA[optionCount] = index;
				optionParamB[optionCount] = x;
				optionParamC[optionCount] = z;
				optionCount++;
			} else if (selectedSpell) {
				if ((selectedFlags & 0x8) == 8) {
					options[optionCount] = selectedSpellPrefix + " @whi@" + string;
					optionType[optionCount] = 730;
					optionParamA[optionCount] = index;
					optionParamB[optionCount] = x;
					optionParamC[optionCount] = z;
					optionCount++;
				}
			} else {
				options[optionCount] = "Trade with @whi@" + string;
				optionType[optionCount] = 1682;
				optionParamA[optionCount] = index;
				optionParamB[optionCount] = x;
				optionParamC[optionCount] = z;
				optionCount++;

				options[optionCount] = "Follow @whi@" + string;
				optionType[optionCount] = 1930;
				optionParamA[optionCount] = index;
				optionParamB[optionCount] = x;
				optionParamC[optionCount] = z;
				optionCount++;

				if (wildernessLevel > 0) {
					options[optionCount] = "Attack @whi@" + string;
					optionType[optionCount] = 1754;
					optionParamA[optionCount] = index;
					optionParamB[optionCount] = x;
					optionParamC[optionCount] = z;
					optionCount++;
				}
			}
		}
	}

	public static final String getLevelColorTag(int a, int b) {
		int d = a - b;
		if (d < -9) {
			return "@red@";
		} else if (d < -6) {
			return "@or3@";
		} else if (d < -3) {
			return "@or2@";
		} else if (d < 0) {
			return "@or1@";
		} else if (d > 9) {
			return "@gre@";
		} else if (d > 6) {
			return "@gr3@";
		} else if (d > 3) {
			return "@gr2@";
		} else if (d > 0) {
			return "@gr1@";
		}
		return "@yel@";
	}

	public final void drawWidget(Widget parent, int parentX, int parentY, int offsetY) {
		if (parent == null) {
			return;
		}

		if (parent.type == 0 && parent.children != null && (!parent.hidden || viewportHoveredInterfaceIndex == parent.index || sidebarHoveredInterfaceIndex == parent.index || chatHoveredInterfaceIndex == parent.index)) {
			int left = Graphics2D.left;
			int top = Graphics2D.top;
			int right = Graphics2D.right;
			int bottom = Graphics2D.bottom;
			Graphics2D.setBounds(parentX, parentY, parentX + parent.width, parentY + parent.height);

			for (int n = 0; n < parent.children.length; n++) {
				int x = parent.childX[n] + parentX;
				int y = parent.childY[n] + parentY - offsetY;
				Widget w = Widget.instances[parent.children[n]];

				if (w.action > 0) {
					updateWidget(w);
				}

				if (w.type == 0) {
					if (w.scrollAmount > w.scrollHeight - w.height) {
						w.scrollAmount = w.scrollHeight - w.height;
					}

					if (w.scrollAmount < 0) {
						w.scrollAmount = 0;
					}

					drawWidget(w, x, y, w.scrollAmount);

					if (w.scrollHeight > w.height) {
						drawScrollbar(x + w.width, y, w.height, w.scrollHeight, w.scrollAmount);
					}
				} else if (w.type == 2) {
					int slot = 0;

					for (int row = 0; row < w.height; row++) {
						for (int column = 0; column < w.width; column++) {
							int drawX = x + (column * (w.inventoryMarginX + 32));
							int drawY = y + (row * (w.inventoryMarginY + 32));

							if (slot < 20) {
								drawX += w.inventoryOffsetX[slot];
								drawY += w.inventoryOffsetY[slot];
							}

							if (w.inventoryIndices[slot] > 0) {
								int index = w.inventoryIndices[slot] - 1;
								Sprite s = ObjectInfo.getSprite(index);

								if (selectedArea != 0 && selectedInterfaceSlot == slot && selectedInterfaceIndex == w.index) {
									s.draw(drawX, drawY, 128);
								} else {
									s.draw(drawX, drawY);
								}

								if (s.clipWidth == 33 || w.inventoryAmount[slot] != 1) {
									int amount = w.inventoryAmount[slot];
									fontSmall.draw(String.valueOf(amount), drawX + 1, drawY + 10, 0);
									fontSmall.draw(String.valueOf(amount), drawX, drawY + 9, 0xFFFF00);
								}
							} else if (w.inventorySprite != null && slot < 20) {
								Sprite s = w.inventorySprite[slot];

								if (s != null) {
									s.draw(drawX, drawY);
								}
							}
							slot++;
						}
					}
				} else if (w.type == 3) {
					if (w.fill) {
						Graphics2D.fillRect(x, y, w.width, w.height, w.colorDisabled);
					} else {
						Graphics2D.drawRect(x, y, w.width, w.height, w.colorDisabled);
					}
				} else if (w.type == 4) {
					BitmapFont f = w.font;

					if (f == null) {
						continue;
					}

					int color = w.colorDisabled;
					String message = w.messageDisabled;

					if (w.hoverColor != 0 && (chatHoveredInterfaceIndex == w.index || sidebarHoveredInterfaceIndex == w.index || viewportHoveredInterfaceIndex == w.index)) {
						color = w.hoverColor;
					}

					if (isWidgetEnabled(w)) {
						color = w.colorEnabled;

						if (w.messageEnabled.length() > 0) {
							message = w.messageEnabled;
						}
					}

					if (w.optionType == 6 && chatContinuingDialogue) {
						message = "Please wait...";
						color = w.colorDisabled;
					}

					int dy = y + f.height;

					if (message == null) {
						continue;
					}

					while (message.length() > 0) {
						if (message.contains("%")) {
							for (;;) {
								int j = message.indexOf("%1");
								if (j == -1) {
									break;
								}
								message = (message.substring(0, j) + getAmountString(invokeWidgetScript(w, 0)) + message.substring(j + 2));
							}
							for (;;) {
								int j = message.indexOf("%2");
								if (j == -1) {
									break;
								}
								message = (message.substring(0, j) + getAmountString(invokeWidgetScript(w, 1)) + message.substring(j + 2));
							}
							for (;;) {
								int j = message.indexOf("%3");
								if (j == -1) {
									break;
								}
								message = (message.substring(0, j) + getAmountString(invokeWidgetScript(w, 2)) + message.substring(j + 2));
							}
							for (;;) {
								int j = message.indexOf("%4");
								if (j == -1) {
									break;
								}
								message = (message.substring(0, j) + getAmountString(invokeWidgetScript(w, 3)) + message.substring(j + 2));
							}
							for (;;) {
								int j = message.indexOf("%5");
								if (j == -1) {
									break;
								}
								message = (message.substring(0, j) + getAmountString(invokeWidgetScript(w, 4)) + message.substring(j + 2));
							}
						}

						int newline = message.indexOf("\\n");
						String s;

						if (newline != -1) {
							s = message.substring(0, newline);
							message = message.substring(newline + 2);
						} else {
							s = message;
							message = "";
						}

						if (w.centered) {
							f.drawTaggableCentered(s, x + w.width / 2, dy, color, w.shadow);
						} else {
							f.drawTaggable(s, x, dy, color, w.shadow);
						}

						dy += f.height;
					}
				} else if (w.type == 5) {
					Sprite s;

					if (isWidgetEnabled(w)) {
						s = w.spriteEnabled;
					} else {
						s = w.spriteDisabled;
					}

					if (s != null) {
						s.draw(x, y);
					}
				} else if (w.type == 6) {
					int centerX = Graphics3D.centerX;
					int centerY = Graphics3D.centerY;

					Graphics3D.centerX = x + (w.width / 2);
					Graphics3D.centerY = y + (w.height / 2);

					int camY = (Graphics3D.sin[w.modelCameraPitch] * w.modelZoom) >> 16;
					int camZ = (Graphics3D.cos[w.modelCameraPitch] * w.modelZoom) >> 16;
					Model m;

					if (w.animIndexDisabled == -1) {
						m = w.getModel(-1, -1, isWidgetEnabled(w));
					} else {
						Animation a = Animation.instance[w.animIndexDisabled];
						m = w.getModel(a.primaryFrames[w.animFrame], a.secondaryFrames[w.animFrame], isWidgetEnabled(w));
					}

					if (m != null) {
						m.draw(0, w.modelYaw, 0, 0, camY, camZ, w.modelCameraPitch);
					}

					Graphics3D.centerX = centerX;
					Graphics3D.centerY = centerY;
				} else if (w.type == 7) {
					BitmapFont f = w.font;

					int slot = 0;
					for (int row = 0; row < w.height; row++) {
						for (int column = 0; column < w.width; column++) {
							if (w.inventoryIndices[slot] > 0) {
								ObjectInfo c = ObjectInfo.get(w.inventoryIndices[slot] - 1);
								String name = c.name;

								if (c.stackable || w.inventoryAmount[slot] != 1) {
									name = String.valueOf(w.inventoryAmount[slot] + "x " + name);
								}

								int dx = x + column * (w.inventoryMarginX + 115);
								int dy = y + row * (w.inventoryMarginY + 12);

								if (w.centered) {
									if (w.shadow) {
										f.drawCentered(name, (dx + 1 + (w.width / 2)), dy + 1, 0);
									}
									f.drawCentered(name, dx + (w.width / 2), dy, w.colorDisabled);
								} else {
									if (w.shadow) {
										f.draw(name, dx + 1, dy + 1, 0);
									}
									f.draw(name, dx, dy, w.colorDisabled);
								}
							}
							slot++;
						}
					}
				}
			}
			Graphics2D.setBounds(left, top, right, bottom);
		}
	}

	public final void updateScrollbar(Widget w, int x, int y, int h, int scrollHeight, int mouseX, int mouseY, boolean isSidebar) {
		if (scrollGripHeld) {
			scrollGripInputPadding = 32;
		} else {
			scrollGripInputPadding = 0;
		}

		scrollGripHeld = false;
		scrollButtonHeld = false;

		if (dragCycle == 0) {
			return;
		}

		if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
			w.scrollAmount -= dragCycle * 4;
			scrollButtonHeld = true;

			if (isSidebar) {
				sidebarRedraw = true;
			}
		} else if (mouseX >= x && mouseX < x + 16 && mouseY >= y + h - 16 && mouseY < y + h) {
			w.scrollAmount += dragCycle * 4;
			scrollButtonHeld = true;

			if (isSidebar) {
				sidebarRedraw = true;
			}
		} else if (mouseX >= x - scrollGripInputPadding && mouseX < x + 16 + scrollGripInputPadding && mouseY >= y + 16 && mouseY < y + h - 16) {
			int gripHeight = (h - 32) * h / scrollHeight;

			if (gripHeight < 8) {
				gripHeight = 8;
			}

			int gripCenterY = (mouseY - y) - 16 - (gripHeight / 2);
			int trackHeight = h - 32 - gripHeight;

			w.scrollAmount = ((scrollHeight - h) * gripCenterY) / trackHeight;

			if (isSidebar) {
				sidebarRedraw = true;
			}

			scrollGripHeld = true;
		}
	}

	public final void drawScrollbar(int x, int y, int h, int scrollHeight, int scrollAmount) {
		scrollbar1.draw(x, y);
		scrollbar2.draw(x, y + h - 16);
		Graphics2D.fillRect(x, y + 16, 16, h - 32, SCROLLBAR_TRACK_COLOR);

		int gripHeight = ((h - 32) * h) / scrollHeight;

		if (gripHeight < 8) {
			gripHeight = 8;
		}

		int offY = ((h - 32 - gripHeight) * scrollAmount) / (scrollHeight - h);
		Graphics2D.fillRect(x, y + 16 + offY, 16, gripHeight, SCROLLBAR_GRIP_FOREGROUND);
		Graphics2D.drawVerticalLine(x, y + 16 + offY, gripHeight, SCROLLBAR_GRIP_HIGHLIGHT);
		Graphics2D.drawVerticalLine(x + 1, y + 16 + offY, gripHeight, SCROLLBAR_GRIP_HIGHLIGHT);
		Graphics2D.drawHorizontalLine(x, y + 16 + offY, 16, SCROLLBAR_GRIP_HIGHLIGHT);
		Graphics2D.drawHorizontalLine(x, y + 17 + offY, 16, SCROLLBAR_GRIP_HIGHLIGHT);
		Graphics2D.drawVerticalLine(x + 15, y + 16 + offY, gripHeight, SCROLLBAR_GRIP_LOWLIGHT);
		Graphics2D.drawVerticalLine(x + 14, y + 17 + offY, gripHeight - 1, SCROLLBAR_GRIP_LOWLIGHT);
		Graphics2D.drawHorizontalLine(x, y + 15 + offY + gripHeight, 16, SCROLLBAR_GRIP_LOWLIGHT);
		Graphics2D.drawHorizontalLine(x + 1, y + 14 + offY + gripHeight, 15, SCROLLBAR_GRIP_LOWLIGHT);
	}

	public final String getAmountString(int i) {
		if (i < 999999999) {
			return String.valueOf(i);
		}
		return "*";
	}

	public final boolean isWidgetEnabled(Widget w) {
		if (w.scriptCompareType == null) {
			return false;
		}

		for (int n = 0; n < w.scriptCompareType.length; n++) {
			int a = invokeWidgetScript(w, n);
			int b = w.scriptCompareValue[n];

			if (w.scriptCompareType[n] == 2) {
				if (a >= b) {
					return false;
				}
			} else if (w.scriptCompareType[n] == 3) {
				if (a <= b) {
					return false;
				}
			} else if (w.scriptCompareType[n] == 4) {
				if (a == b) {
					return false;
				}
			} else if (a != b) {
				return false;
			}
		}
		return true;
	}

	public final int invokeWidgetScript(Widget parent, int script) {
		if (parent.script == null || script >= parent.script.length) {
			return -2;
		}

		try {
			int[] code = parent.script[script];
			int a = 0;
			int position = 0;

			for (;;) {
				int opcode = code[position++];

				if (opcode == 0) {
					return a;
				}

				if (opcode == 1) {
					a += skillLevelReal[code[position++]];
				}

				if (opcode == 2) {
					a += skillLevel[code[position++]];
				}

				if (opcode == 3) {
					a += skillExperience[code[position++]];
				}

				if (opcode == 4) {
					Widget w = Widget.instances[code[position++]];
					int index = code[position++] + 1;

					for (int n = 0; n < w.inventoryIndices.length; n++) {
						if (w.inventoryIndices[n] == index) {
							a += w.inventoryAmount[n];
						}
					}
				}

				if (opcode == 5) {
					a += variables[code[position++]];
				}

				if (opcode == 6) {
					try {
						a += EXPERIENCE_TABLE[skillLevel[code[position++]] - 1];
					} catch (Exception e) {
					}
				}

				if (opcode == 7) {
					a += (variables[code[position++]] * 100) / 46875;
				}

				if (opcode == 8) {
					a += localPlayer.level;
				}

				if (opcode == 9) {
					for (int n = 0; n < 19; n++) {
						a += skillLevel[n];
					}
				}

				if (opcode == 10) {
					Widget w = Widget.instances[code[position++]];
					int index = code[position++] + 1;

					for (int n = 0; n < w.inventoryIndices.length; n++) {
						if (w.inventoryIndices[n] == index) {
							a += 999999999;
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error executing clientscript", e);
			return -1;
		}
	}

	public void parseWidgetOption(Widget w) {
		if (w.optionType == 1) {
			options[optionCount] = w.option;
			optionType[optionCount] = 759;
			optionParamC[optionCount] = w.index;
			optionCount++;
		} else if (w.optionType == 2) {
			if (!selectedSpell) {
				String s = w.optionCircumfix;

				if (s.contains(" ")) {
					s = s.substring(0, s.indexOf(" "));
				}

				options[optionCount] = s + " @gre@" + w.optionSuffix;
				optionType[optionCount] = 17;
				optionParamC[optionCount] = w.index;
				optionCount++;
			}
		} else if (w.optionType == 3) {
			options[optionCount] = "Close";
			optionType[optionCount] = 454;
			optionParamC[optionCount] = w.index;
			optionCount++;
		} else if (w.optionType == 4) {
			options[optionCount] = w.option;
			optionType[optionCount] = 739;
			optionParamC[optionCount] = w.index;
			optionCount++;
		} else if (w.optionType == 5) {
			options[optionCount] = w.option;
			optionType[optionCount] = 700;
			optionParamC[optionCount] = w.index;
			optionCount++;
		} else if (w.optionType == 6) {
			if (!chatContinuingDialogue) {
				options[optionCount] = w.option;
				optionType[optionCount] = 883;
				optionParamC[optionCount] = w.index;
				optionCount++;
			}
		}
	}

	public final void updateWidget(Widget parent, int parentX, int parentY, int mouseX, int mouseY, int scrollAmount) {
		if (parent == null) {
			return;
		}

		if (parent.type == 0 && parent.children != null && !parent.hidden && (mouseX >= parentX && mouseY >= parentY && mouseX <= parentX + parent.width && mouseY <= parentY + parent.height)) {
			for (int n = 0; n < parent.children.length; n++) {
				int x = parent.childX[n] + parentX;
				int y = parent.childY[n] + parentY - scrollAmount;
				Widget w = Widget.instances[parent.children[n]];

				boolean containsMouse = mouseX >= x && mouseY >= y && mouseX < x + w.width && mouseY < y + w.height;

				if ((w.hoverParentIndex >= 0 || w.hoverColor != 0) && containsMouse) {
					if (w.hoverParentIndex >= 0) {
						hoveredInterfaceIndex = w.hoverParentIndex;
					} else {
						hoveredInterfaceIndex = w.index;
					}
				}

				if (w.type == 0) {
					updateWidget(w, x, y, mouseX, mouseY, w.scrollAmount);

					if (w.scrollHeight > w.height) {
						updateScrollbar(w, x + w.width, y, w.height, w.scrollHeight, mouseX, mouseY, true);
					}
				} else {
					if (containsMouse) {
						parseWidgetOption(w);
					}

					if (w.type == 2) {
						int slot = 0;
						for (int slotY = 0; slotY < w.height; slotY++) {
							for (int slotX = 0; slotX < w.width; slotX++) {
								if (w.inventoryIndices[slot] > 0) {
									int x0 = x + slotX * (w.inventoryMarginX + 32);
									int y0 = y + slotY * (w.inventoryMarginY + 32);

									if (slot < 20) {
										x0 += w.inventoryOffsetX[slot];
										y0 += w.inventoryOffsetY[slot];
									}

									if (mouseX >= x0 && mouseY >= y0 && mouseX < x0 + 32 && mouseY < y0 + 32) {
										ObjectInfo c = ObjectInfo.get(w.inventoryIndices[slot] - 1);

										if (selectedObject && w.inventoryHasActions) {
											if (w.index != selectedObjInterface || slot != selectedObjSlot) {
												options[optionCount] = "Use " + selectedObjName + " with @lre@" + c.name;
												optionType[optionCount] = 39;
												optionParamA[optionCount] = c.index;
												optionParamB[optionCount] = slot;
												optionParamC[optionCount] = w.index;
												optionCount++;
											}
										} else if (selectedSpell && w.inventoryHasActions) {
											if ((selectedFlags & 0x10) == 16) {
												options[optionCount] = selectedSpellPrefix + " @lre@" + c.name;
												optionType[optionCount] = 449;
												optionParamA[optionCount] = c.index;
												optionParamB[optionCount] = slot;
												optionParamC[optionCount] = w.index;
												optionCount++;
											}
										} else {
											if (w.inventoryHasActions) {
												for (int j = 4; j >= 3; j--) {
													if (c.actions != null && c.actions[j] != null) {
														options[optionCount] = c.actions[j] + " @lre@" + c.name;

														if (j == 3) {
															optionType[optionCount] = 247;
														} else if (j == 4) {
															optionType[optionCount] = 296;
														}

														optionParamA[optionCount] = c.index;
														optionParamB[optionCount] = slot;
														optionParamC[optionCount] = w.index;
														optionCount++;
													} else if (j == 4) {
														options[optionCount] = "Drop @lre@" + c.name;
														optionType[optionCount] = 296;
														optionParamA[optionCount] = c.index;
														optionParamB[optionCount] = slot;
														optionParamC[optionCount] = w.index;
														optionCount++;
													}
												}
											}

											if (w.inventoryIsUsable) {
												options[optionCount] = "Use @lre@" + c.name;
												optionType[optionCount] = 669;
												optionParamA[optionCount] = c.index;
												optionParamB[optionCount] = slot;
												optionParamC[optionCount] = w.index;
												optionCount++;
											}

											if (w.inventoryHasActions && c.actions != null) {
												for (int j = 2; j >= 0; j--) {
													if (c.actions[j] != null) {
														options[optionCount] = c.actions[j] + " @lre@" + c.name;

														if (j == 0) {
															optionType[optionCount] = 677;
														} else if (j == 1) {
															optionType[optionCount] = 522;
														} else if (j == 2) {
															optionType[optionCount] = 249;
														}

														optionParamA[optionCount] = c.index;
														optionParamB[optionCount] = slot;
														optionParamC[optionCount] = w.index;
														optionCount++;
													}
												}
											}

											if (w.inventoryActions != null) {
												for (int j = 4; j >= 0; j--) {
													if (w.inventoryActions[j] != null) {
														options[optionCount] = w.inventoryActions[j] + " @lre@" + c.name;

														if (j == 0) {
															optionType[optionCount] = 678;
														} else if (j == 1) {
															optionType[optionCount] = 523;
														} else if (j == 2) {
															optionType[optionCount] = 836;
														} else if (j == 3) {
															optionType[optionCount] = 548;
														} else if (j == 4) {
															optionType[optionCount] = 62;
														}

														optionParamA[optionCount] = c.index;
														optionParamB[optionCount] = slot;
														optionParamC[optionCount] = (w.index);
														optionCount++;
													}
												}
											}

											options[optionCount] = "Examine @lre@" + c.name;
											optionType[optionCount] = 1258;
											optionParamA[optionCount] = c.index;
											optionCount++;
										}
									}
								}
								slot++;
							}
						}
					}
				}
			}
		}
	}

	public final void resetWidgetAnimations(int index) {
		Widget parent = Widget.instances[index];

		for (int n = 0; n < parent.children.length; n++) {
			if (parent.children[n] == -1) {
				break;
			}

			Widget child = Widget.instances[parent.children[n]];

			if (child.type == 1) {
				resetWidgetAnimations(child.index);
			}

			child.animFrame = 0;
			child.animCycle = 0;
		}
	}

	public final boolean updateWidgetAnimations(int index, int cycle) {
		boolean updated = false;
		Widget parent = Widget.instances[index];

		for (int n = 0; n < parent.children.length; n++) {
			if (parent.children[n] == -1) {
				break;
			}

			Widget w = Widget.instances[parent.children[n]];

			if (w.type == 1) {
				updated |= updateWidgetAnimations(w.index, cycle);
			}

			if (w.animIndexDisabled != -1) {
				Animation a = Animation.instance[w.animIndexDisabled];
				w.animCycle += cycle;

				while (w.animCycle > a.frameDuration[w.animFrame]) {
					w.animCycle -= a.frameDuration[w.animFrame] + 1;
					w.animFrame++;

					if (w.animFrame >= a.frameCount) {
						w.animFrame -= a.delta;

						if (w.animFrame < 0 || w.animFrame >= a.frameCount) {
							w.animFrame = 0;
						}
					}
					updated = true;
				}
			}
		}

		return updated;
	}

	public final void updateVarp(int index) {
		int type = Varp.instance[index].type;

		if (type != 0) {
			int v = variables[index];

			if (type == 1) {
				if (v == 1) {
					Graphics3D.generatePalette(0.9);
				} else if (v == 2) {
					Graphics3D.generatePalette(0.8);
				} else if (v == 3) {
					Graphics3D.generatePalette(0.7);
				} else if (v == 4) {
					Graphics3D.generatePalette(0.6);
				}
				ObjectInfo.sprites.clear();
				redraw = true;
			}

			if (type == 3) {
				if (v == 1 && midiPlaying) {
					midiPlaying = false;
					Signlink.midi = null;
				}
				if (v == 0 && !midiPlaying) {
					midiPlaying = true;
					Signlink.midi = midi;
				}
			}

			if (type == 5) {
				mouseOneButton = v == 1;
			}

			if (type == 6) {
				allowChatEffects = v;
			}
		}
	}

	public void updateWidget(Widget w) {
		int action = w.action;

		if (action >= 1 && action <= 100) {
			if (--action >= friendCount) {
				w.messageDisabled = "";
				w.optionType = 0;
			} else {
				w.messageDisabled = friendName[action];
				w.optionType = 1;

				if (w.option == null) {
					w.optionType = 0;
				}
			}
		} else if (action >= 101 && action <= 200) {
			action -= 101;

			if (action >= friendCount) {
				w.messageDisabled = "";
				w.optionType = 0;
			} else {
				if (friendWorld[action] == 0) {
					w.messageDisabled = "@red@Offline";
				} else if (friendWorld[action] == nodeid) {
					w.messageDisabled = "@gre@World-" + (friendWorld[action] - 9);
				} else {
					w.messageDisabled = "@yel@World-" + (friendWorld[action] - 9);
				}

				w.optionType = 1;

				if (w.option == null) {
					w.optionType = 0;
				}
			}
		} else if (action == 203) {
			w.scrollHeight = friendCount * 15 + 20;

			if (w.scrollHeight <= w.height) {
				w.scrollHeight = w.height + 1;
			}
		} else if (action >= 401 && action <= 500) {
			action -= 401;

			if (action >= ignoreCount) {
				w.messageDisabled = "";
				w.optionType = 0;
			} else {
				w.messageDisabled = StringUtil.getFormatted(StringUtil.fromBase37((ignoreNameLong[action])));
			}
		} else if (action == 503) {
			w.scrollHeight = ignoreCount * 15 + 20;

			if (w.scrollHeight <= w.height) {
				w.scrollHeight = w.height + 1;
			}
		} else if (action == 327) {
			w.modelCameraPitch = 150;
			w.modelYaw = (int) (Math.sin((double) cycle / 40.0) * 256.0) & 0x7ff;

			if (characterDesignUpdate) {
				characterDesignUpdate = false;

				Model[] models = new Model[7];
				int n = 0;

				for (int m = 0; m < 7; m++) {
					int d = characterDesigns[m];
					if (d >= 0) {
						models[n++] = Identikit.instance[d].getModel();
					}
				}

				Model m = new Model(models, n);

				for (int d = 0; d < 5; d++) {
					if (characterDesignColors[d] != 0) {
						m.recolor((Identikit.APPEARANCE_COLORS[d][0]), (Identikit.APPEARANCE_COLORS[d][characterDesignColors[d]]));
						if (d == 1) {
							m.recolor(Identikit.BEARD_COLORS[0], (Identikit.BEARD_COLORS[(characterDesignColors[d])]));
						}
					}
				}

				m.applyGroups();
				m.applyFrame(Animation.instance[localPlayer.animStand].primaryFrames[0]);
				m.applyLighting(64, 850, -30, -50, -30, true);
				w.modelDisabled = m;
			}
		} else if (action == 324) {
			if (buttonDisabled == null) {
				buttonDisabled = w.spriteDisabled;
				buttonEnabled = w.spriteEnabled;
			}

			if (characterDesignIsMale) {
				w.spriteDisabled = buttonEnabled;
			} else {
				w.spriteDisabled = buttonDisabled;
			}
		} else if (action == 325) {
			if (buttonDisabled == null) {
				buttonDisabled = w.spriteDisabled;
				buttonEnabled = w.spriteEnabled;
			}

			if (characterDesignIsMale) {
				w.spriteDisabled = buttonDisabled;
			} else {
				w.spriteDisabled = buttonEnabled;
			}
		}
	}

	public boolean useWidgetAction(Widget w) {
		int action = w.action;

		if (action == 201) {
			chatRedraw = true;
			chatShowTransferInput = false;
			chatShowDialogueInput = true;
			chatDialogueInput = "";
			chatDialogueInputType = 1;
			chatDialogueMessage = "Enter name of friend to add to list";
			return false;
		}

		if (action == 202) {
			chatRedraw = true;
			chatShowTransferInput = false;
			chatShowDialogueInput = true;
			chatDialogueInput = "";
			chatDialogueInputType = 2;
			chatDialogueMessage = "Enter name of friend to delete from list";
			return false;
		}

		if (action >= 1 && action <= 200) {
			if (action >= 101) {
				action -= 101;
			} else {
				action--;
			}

			if (friendWorld[action] > 0) {
				chatRedraw = true;
				chatShowTransferInput = false;
				chatShowDialogueInput = true;
				chatDialogueInput = "";
				chatDialogueInputType = 3;
				chatSendFriendMessageIndex = action;
				chatDialogueMessage = "Enter message to send to " + friendName[action];
			}
			return false;
		}

		if (action == 501) {
			chatRedraw = true;
			chatShowTransferInput = false;
			chatShowDialogueInput = true;
			chatDialogueInput = "";
			chatDialogueInputType = 4;
			chatDialogueMessage = "Enter name of player to add to list";
			return false;
		}

		if (action == 502) {
			chatRedraw = true;
			chatShowTransferInput = false;
			chatShowDialogueInput = true;
			chatDialogueInput = "";
			chatDialogueInputType = 5;
			chatDialogueMessage = "Enter name of player to delete from list";
			return false;
		}

		if (action >= 300 && action <= 313) {
			int type = (action - 300) / 2;
			int direction = action & 0x1;
			int design = characterDesigns[type];

			if (design != -1) {
				do {
					if (direction == 0 && --design < 0) {
						design = Identikit.count - 1;
					}

					if (direction == 1 && ++design >= Identikit.count) {
						design = 0;
					}
				} while (Identikit.instance[design].type != type + (characterDesignIsMale ? 0 : 7));

				characterDesigns[type] = design;
				characterDesignUpdate = true;
			}
			return false;
		}

		if (action >= 314 && action <= 323) {
			int type = (action - 314) / 2;
			int direction = action & 0x1;
			int color = characterDesignColors[type];

			if (direction == 0 && --color < 0) {
				color = Identikit.APPEARANCE_COLORS[type].length - 1;
			}

			if (direction == 1 && ++color >= Identikit.APPEARANCE_COLORS[type].length) {
				color = 0;
			}

			characterDesignColors[type] = color;
			characterDesignUpdate = true;
			return false;
		}

		if (action == 324 && !characterDesignIsMale) {
			characterDesignIsMale = true;
			resetCharacterDesign();
			return false;
		}

		if (action == 325 && characterDesignIsMale) {
			characterDesignIsMale = false;
			resetCharacterDesign();
			return false;
		}

		if (action == 326) {
			out.putOpcode(128);
			out.putByte(characterDesignIsMale ? 0 : 1);

			for (int n = 0; n < 7; n++) {
				out.putByte(characterDesigns[n]);
			}

			for (int n = 0; n < 5; n++) {
				out.putByte(characterDesignColors[n]);
			}
			return true;
		}

		return false;
	}

	public final void resetCharacterDesign() {
		characterDesignUpdate = true;

		for (int n = 0; n < 7; n++) {
			characterDesigns[n] = -1;

			for (int m = 0; m < Identikit.count; m++) {
				if (Identikit.instance[m].type == n + (characterDesignIsMale ? 0 : 7)) {
					characterDesigns[n] = m;
					break;
				}
			}
		}
	}

	public final void drawMinimap() {
		maparea.prepare();

		// player minimap position
		int playerX = localPlayer.sceneX / 32;
		int playerY = localPlayer.sceneZ / 32;

		int x = playerX + 48;
		int y = (minimap.height - 48) - playerY;

		minimap.draw(21, 9, 146, 151, x, y, cameraOrbitYaw, minimapLeft, minimapLineWidth);
		compass.draw(0, 0, 33, 33, 25, 25, cameraOrbitYaw, compassLeft, compassLineWidth);

		for (int n = 0; n < minimapFunctionCount; n++) {
			x = (((minimapFunctionX[n] * 4) + 2) - playerX);
			y = (((minimapFunctionY[n] * 4) + 2) - playerY);
			drawOntoMinimap(minimapFunctions[n], x, y);
		}

		for (int tileX = 0; tileX < 104; tileX++) {
			for (int tileY = 0; tileY < 104; tileY++) {
				LinkedQueue c = levelObjects[currentLevel][tileX][tileY];
				if (c != null) {
					x = (((tileX * 4) + 2) - playerX);
					y = (((tileY * 4) + 2) - playerY);
					drawOntoMinimap(mapdot1, x, y);
				}
			}
		}

		for (int n = 0; n < entityCount; n++) {
			NPC npc = npcs[npcIndices[n]];
			if (npc != null && npc.isValid() && npc.config.showOnMinimap) {
				x = ((npc.sceneX / 32) - playerX);
				y = ((npc.sceneZ / 32) - playerY);
				drawOntoMinimap(mapdot2, x, y);
			}
		}

		for (int n = 0; n < playerCount; n++) {
			Player p = players[playerIndices[n]];
			if (p != null && p.isVisible()) {
				x = ((p.sceneX / 32) - playerX);
				y = ((p.sceneZ / 32) - playerY);
				drawOntoMinimap(mapdot3, x, y);
			}
		}

		Graphics2D.fillRect(93, 82, 3, 3, 0xFFFFFF);
		viewport.prepare();
	}

	public final void drawOntoMinimap(Sprite s, int x, int y) {
		if (s == null) {
			return;
		}

		int length = (x * x) + (y * y);

		if (length > 6400) {
			return;
		}

		int sin = Model.sin[cameraOrbitYaw];
		int cos = Model.cos[cameraOrbitYaw];

		int drawX = ((y * sin) + (x * cos)) >> 16;
		int drawY = ((y * cos) - (x * sin)) >> 16;

		drawX -= s.width / 2;
		drawY += s.height / 2;

		if (length > 2500) {
			s.draw(mapback, drawX + 94, 83 - drawY);
		} else {
			s.draw(drawX + 94, 83 - drawY);
		}
	}

	public final void addMessage(int type, String prefix, String suffix) {
		if (chatWidgetIndex == -1) {
			chatRedraw = true;
		}

		for (int i = 99; i > 0; i--) {
			chatMessageType[i] = chatMessageType[i - 1];
			chatMessagePrefix[i] = chatMessagePrefix[i - 1];
			chatMessage[i] = chatMessage[i - 1];
		}

		chatMessageType[0] = type;
		chatMessagePrefix[0] = prefix;
		chatMessage[0] = suffix;
	}

	public final void drawChat() {
		chatarea.prepare();
		Graphics3D.offsets = chatOffsets;
		chatback.draw(0, 0);

		if (chatShowDialogueInput) {
			fontBold.drawCentered(chatDialogueMessage, 239, 40, 0);
			fontBold.drawCentered(chatDialogueInput + "*", 239, 60, 128);
		} else if (chatShowTransferInput) {
			fontBold.drawCentered("Enter amount to transfer:", 239, 40, 0);
			fontBold.drawCentered(chatTransferInput + "*", 239, 60, 128);
		} else if (chatWidgetIndex != -1) {
			drawWidget(Widget.instances[chatWidgetIndex], 0, 0, 0);
		} else {
			int messageCount = 0;
			Graphics2D.setBounds(0, 0, 463, 77);

			for (int n = 0; n < 50; n++) {
				if (chatMessage[n] != null) {
					int type = chatMessageType[n];
					int y = 70 - messageCount * 14 + chatScrollAmount;

					if (type == 0) {
						if (y > 0 && y < 110) {
							fontFancy.draw(chatMessage[n], 4, y, 0);
						}
						messageCount++;
					}

					if (type == 1) {
						if (y > 0 && y < 110) {
							fontFancy.draw(chatMessagePrefix[n] + ":", 4, y, 0xFFFFFF);
							fontFancy.draw(chatMessage[n], 12 + fontFancy.stringWidth(chatMessagePrefix[n]), y, 0xFF);
						}
						messageCount++;
					}

					if (type == 2 && (chatPublicSetting == 0 || (chatPublicSetting == 1 && isFriend(chatMessagePrefix[n])))) {
						if (y > 0 && y < 110) {
							fontFancy.draw(chatMessagePrefix[n] + ":", 4, y, 0);
							fontFancy.draw(chatMessage[n], fontFancy.stringWidth(chatMessagePrefix[n]) + 12, y, 0xFF);
						}
						messageCount++;
					}

					if (type == 3 && (chatPrivateSetting == 0 || (chatPrivateSetting == 1 && isFriend(chatMessagePrefix[n])))) {
						if (y > 0 && y < 110) {
							fontFancy.draw("From " + chatMessagePrefix[n] + ":", 4, y, 0);
							fontFancy.draw(chatMessage[n], 12 + (fontFancy.stringWidth("From " + chatMessagePrefix[n])), y, 0x800000);
						}
						messageCount++;
					}

					if (type == 4 && (chatTradeDuelSetting == 0 || (chatTradeDuelSetting == 1 && isFriend(chatMessagePrefix[n])))) {
						if (y > 0 && y < 110) {
							fontFancy.draw((chatMessagePrefix[n] + " " + chatMessage[n]), 4, y, 0x800080);
						}
						messageCount++;
					}

					if (type == 5 && chatPrivateSetting < 2) {
						if (y > 0 && y < 110) {
							fontFancy.draw(chatMessage[n], 4, y, 0x800000);
						}
						messageCount++;
					}

					if (type == 6 && chatPrivateSetting < 2) {
						if (y > 0 && y < 110) {
							fontFancy.draw("To " + chatMessagePrefix[n] + ":", 4, y, 0);
							fontFancy.draw(chatMessage[n], 12 + (fontFancy.stringWidth("To " + chatMessagePrefix[n])), y, 0x800000);
						}
						messageCount++;
					}
				}
			}
			Graphics2D.resetBounds();
			chatHeight = (messageCount * 14) + 7;

			if (chatHeight < 78) {
				chatHeight = 78;
			}

			drawScrollbar(463, 0, 77, chatHeight, chatHeight - chatScrollAmount - 77);
			fontFancy.draw(chatInput + "*", 3, 90, 0);
			Graphics2D.drawHorizontalLine(0, 77, 479, 0);
		}
		chatarea.draw(graphics, 22, 375);
		viewport.prepare();
		Graphics3D.offsets = viewportOffsets;
	}

	public final boolean isFriend(String s) {
		if (s == null) {
			return false;
		}

		for (int n = 0; n < friendCount; n++) {
			if (s.equalsIgnoreCase(friendName[n])) {
				return true;
			}
		}

		return s.equalsIgnoreCase(localPlayer.name);
	}

	public final void drawSidebar() {
		sidebar.prepare();
		Graphics3D.offsets = sidebarOffsets;
		invback.draw(0, 0);

		if (sidebarWidgetIndex != -1) {
			drawWidget(Widget.instances[sidebarWidgetIndex], 0, 0, 0);
		} else if (tabWidgetIndex[selectedTab] != -1) {
			drawWidget(Widget.instances[tabWidgetIndex[selectedTab]], 0, 0, 0);
		}

		if (optionMenuVisible && optionMenuArea == 1) {
			drawOptionMenu();
		}

		sidebar.draw(graphics, 562, 231);
		viewport.prepare();
		Graphics3D.offsets = viewportOffsets;
	}

	@Override
	public URL getCodeBase() {
		if (Signlink.mainapp != null) {
			return Signlink.mainapp.getCodeBase();
		}

		try {
			return new URL("http://" + address + ":" + (portoff + 80));
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error getting codebase", e);
		}

		return super.getCodeBase();
	}

	public final String getDocumentHost() {
		if (Signlink.mainapp != null) {
			return Signlink.mainapp.getDocumentBase().getHost().toLowerCase();
		}

		if (frame != null) {
			return "runescape.com";
		}

		try {
			return super.getDocumentBase().getHost().toLowerCase();
		} catch (Exception e) {
			return "runescape.com";
		}
	}

	@Override
	public final String getParameter(String s) {
		if (Signlink.mainapp != null) {
			return Signlink.mainapp.getParameter(s);
		}
		return super.getParameter(s);
	}

	@Override
	public final void startThread(Runnable r, int priority) {
		if (Signlink.mainapp != null) {
			Signlink.startThread(r, priority);
		} else {
			super.startThread(r, priority);
		}
	}

	public final DataInputStream openURL(String s) throws IOException {
		if (Signlink.mainapp != null) {
			return Signlink.openURL(s);
		}
		return new DataInputStream(new URL(getCodeBase(), s).openStream());
	}

	public final Socket openSocket(int port) throws IOException {
		if (Signlink.mainapp != null) {
			return Signlink.openSocket(port);
		}
		return new Socket(InetAddress.getByName(getCodeBase().getHost()), port);
	}

	public final String getMidi() {
		if (Signlink.midi == null) {
			return "none";
		}
		String s = Signlink.midi + ".mid.gz";
		Signlink.midi = null;
		return s;
	}

	public final String getJingle() {
		if (Signlink.jingle == null) {
			return "none";
		}
		String s = Signlink.jingle + ".mid.gz";
		Signlink.jingle = null;
		return s;
	}

	public final int getJingleLength() {
		return Signlink.jinglelen;
	}
}

package pre194;

public final class Tile extends Link {

	public int level;
	public int x;
	public int z;
	public int renderLevel;
	public TileUnderlay underlay;
	public TileOverlay overlay;
	public WallLocation wall;
	public WallDecorationLocation wallDecoration;
	public GroundDecorationLocation groundDecoration;
	public ObjectLocation object;
	public int locationCount;
	public Location[] locs = new Location[5];
	public int[] locFlags = new int[5];
	public int flags;
	public int physicalLevel; // who knows??
	public boolean draw;
	public boolean isVisible;
	public boolean drawLocations;
	public int wallCullDirection;
	public int wallUncullDirection;
	public int wallCullOppositeDirection;
	public int wallDrawFlags;
	public Tile bridge;

	public Tile(int level, int x, int y) {
		this.renderLevel = this.level = level;
		this.x = x;
		this.z = y;
	}
}

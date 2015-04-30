package pre194;

public final class AnimatedLocation extends Link {

	public int level;
	public int classtype;
	public int tileX;
	public int tileZ;
	public int locIndex;
	public Animation animation;
	public int animFrame;
	public int animCycle;

	public AnimatedLocation(Animation animation, int locIndex, int type, int tileX, int tileY, int level) {
		this.level = level;
		this.classtype = type;
		this.tileX = tileX;
		this.tileZ = tileY;
		this.locIndex = locIndex;
		this.animation = animation;
		this.animFrame = -1;
		this.animCycle = 0;
	}
}

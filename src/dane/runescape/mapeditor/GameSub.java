/*
 * The MIT License
 *
 * Copyright 2015 Dane.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package dane.runescape.mapeditor;

import dane.runescape.mapeditor.event.*;
import dane.runescape.mapeditor.util.*;
import pre194.*;

import java.util.logging.*;

/**
 * An extension of the {@link Game} class. Used for handling the drawing and
 * updating of the 3d scene.
 *
 * @author Dane
 */
public class GameSub extends Game implements MapPanelEventListener {

	private static final Logger logger = Logger.getLogger(GameSub.class.getName());

	public OrbitCamera camera;

	public GameSub() {
		super();
	}

	@Override
	public Archive loadArchive(String archiveName, String archiveFile, int crc, int percent) {
		((GameShell) this).drawProgress("Requesting " + archiveName, percent);
		return new Archive(Signlink.loadFile(archiveFile));
	}

	@Override
	public void load() {
		try {
			Game.setHighMemory();

			titleArchive = loadArchive("title", "title", 0, 10);

			loadFonts(titleArchive);

			Archive config = loadArchive("config", "config", archiveCRC[2], 20);
			Archive media = loadArchive("2d graphics", "media", archiveCRC[4], 40);
			Archive models = loadArchive("3d graphics", "models", archiveCRC[5], 50);
			Archive textures = loadArchive("textures", "textures", archiveCRC[6], 60);

			initSceneComponents();

			loadMedia(media);
			loadTextures(textures);
			loadModels(models);
			loadConfigs(config);

			drawProgress("Preparing game engine", 95);
			viewport = new ImageProducer(512, 334);
			viewportOffsets = Graphics3D.prepareOffsets();

			SceneGraph.init(512, 334, 500, 800);
			loadRegion(50, 50);
		} catch (Exception e) {
			errorLoading = true;
			logger.log(Level.SEVERE, "Error starting game", e);
		}
	}

	@Override
	public void readLandscape(Scene s, byte[] src, int baseTileX, int baseTileY) {
		s.readLandscape(src, baseTileX, baseTileY, centerSectorX * 8, centerSectorY * 8);
	}

	public void loadRegion(int x, int y) {
		centerSectorX = x << 3;
		centerSectorY = y << 3;

		mapBaseX = centerSectorX << 3;
		mapBaseY = centerSectorY << 3;

		mapLandData = new byte[1][];
		mapLocData = new byte[1][];
		mapIndices = new int[]{(x << 8) | y};

		Signlink.setLoopRate(1);
		mapLandData[0] = Signlink.loadFile("maps/m" + x + "_" + y);
		mapLocData[0] = Signlink.loadFile("maps/l" + x + "_" + y);
		Signlink.setLoopRate(100);

		int deltaX = mapBaseX - mapLastBaseX;
		int deltaY = mapBaseY - mapLastBaseZ;
		mapLastBaseX = mapBaseX;
		mapLastBaseZ = mapBaseY;

		sceneState = 2;
		Scene.levelBuilt = currentLevel;
		createScene();
	}

	@Override
	public void update() {
		updateCross();
		updateOptionMenu();
		updateLandscapeClick();
	}

	@Override
	public int updateCamera(int tileX, int tileY) {
		int landY = getLandY(0, 0, currentLevel);

		cameraPitch = camera.getCurrentPitch();
		cameraOrbitPitch = cameraPitch;

		cameraYaw = camera.getCurrentYaw();
		cameraOrbitYaw = cameraYaw;

		updateCameraOrbit(camera.getCurrentX(), landY - 50, camera.getCurrentZ(), camera.getCurrentYaw(), cameraPitch, (cameraPitch * 3) + 1200);

		try {
			if ((levelRenderFlags[currentLevel][tileX][tileY] & 0x4) != 0) {
				return currentLevel;
			}
		} catch (Exception ignored) {

		}

		try {
			return getTopLevel(tileX, tileY);
		} catch (Exception ignored) {

		}
		return currentLevel;
	}

	@Override
	public void draw() {
		if (sceneState == 2) {
			drawViewport();
		}
	}

	@Override
	public void drawViewport() {
		drawCycle++;

		drawPlayers();
		drawNPCs();
		drawProjectiles();
		drawSpotAnimations();
		drawAnimatedLocations();

		int topLevel = updateCamera(camera.getCurrentX() >> 7, camera.getCurrentY() >> 7);

		int startCycle = Graphics3D.cycle;
		Model.allowInput = true;
		Model.hoverCount = 0;
		Model.mouseX = mouseX;
		Model.mouseY = mouseY;

		Graphics2D.clear();

		Scene.mouseX = Model.mouseX;
		Scene.mouseY = Model.mouseY;

		graph.draw(cameraX, cameraY, cameraZ, cameraPitch, cameraOrbitYaw, topLevel);
		graph.clearFrameLocs();

		for (int i = 0; i < SceneGraph.activeOccluderCount; i++) {
			Occluder o = SceneGraph.activeOccluders[i];

			setDrawPos(o.minX, o.minY, o.minZ);
			Graphics2D.fillRect(drawX - 1, drawY - 1, 3, 3, 0xFF0000);
			fontSmall.draw(o.type + ", " + o.testDirection, drawX, drawY, 0xFF0000);

			setDrawPos(o.maxX, o.maxY, o.maxZ);
			Graphics2D.fillRect(drawX - 1, drawY - 1, 3, 3, 0xFF00);
			fontSmall.draw(o.type + ", " + o.testDirection, drawX, drawY, 0xFF0000);
		}

		drawViewport2d();
		drawCross();

		if (!optionMenuVisible) {
			updateInput();
			drawTooltip();
		} else if (optionMenuArea == 0) {
			drawOptionMenu();
		}

		updateAnimatedTextures(startCycle);

		fontSmall.draw(mouseX + ", " + mouseY + ", " + clickX + ", " + clickY, 16, 32, 0xFFFFFF);
		fontSmall.draw(crossX + ", " + crossY, 16, 48, 0xFFFFFF);
		viewport.draw(graphics, 0, 0);
	}

	@Override
	public void updateInput() {
		options[0] = "Cancel";
		optionType[0] = 1264;
		optionCount = 1;

		updateViewport();
		sortOptions();
	}

	@Override
	public void updateOptionMenu() {
		int button = mouseButton;

		if (optionMenuVisible) {
			if (button != 1) {
				int mx = mouseX;
				int my = mouseY;

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
				int x = clickX - maxWidth / 2;

				if (x + maxWidth > 512) {
					x = 512 - maxWidth;
				}

				if (x < 0) {
					x = 0;
				}

				int y = clickY;

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
		}
	}

	@Override
	public void interactWithLocation(int bitset, int x, int y, int opcode) {
		crossX = clickX;
		crossY = clickY;
		crossType = 2;
		crossCycle = 0;
	}

	@Override
	public void updateLandscapeClick() {
		if (Scene.clickedTileX != -1) {
			int tileX = Scene.clickedTileX;
			int tileZ = Scene.clickedTileZ;

			// do something?
			crossX = clickX;
			crossY = clickY;
			crossType = 1;
			crossCycle = 0;

			Scene.clickedTileX = -1;
		}
	}

	@Override
	public Scene createScene() {
		Scene s = super.createScene();
		this.fireSceneCreated(this.currentLevel, s, this.graph);
		return s;
	}

	protected void fireSceneCreated(int level, Scene s, SceneGraph graph) {
		for (GameListener l : this.listeners.getListeners(GameListener.class)) {
			l.onSceneLoaded(level, s, graph);
		}

		graph.addObject(new Model(0), 0, 21, 26, 400, 123);
	}

	/**
	 * Adds a game listener.
	 *
	 * @param l the listener.
	 */
	public void addGameListener(GameListener l) {
		this.listeners.add(GameListener.class, l);
	}

	/**
	 * Removes a game listener.
	 *
	 * @param l the listener.
	 */
	public void removeGameListener(GameListener l) {
		this.listeners.remove(GameListener.class, l);
	}

	@Override
	public void onMapPanelEvent(MapPanelEvent e) {
		switch (e.getType()) {
			case ANGLE_CHANGE: {
				this.camera.setYaw(Maths.toRuneDegree(e.getAngle()) & 0x7FF);
				break;
			}
			case TILE_CHANGE: {
				this.camera.setX((e.getTileX() * 128) + 64);
				this.camera.setZ((e.getTileY() * 128) + 64);
				break;
			}
			case ZOOM_ADJUST: {
				this.camera.setZoom(this.camera.getZoom() + e.getZoomAdjustment());
				break;
			}
		}
	}
}

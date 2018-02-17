package com.nowbob.slimegame;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;

public class MyGdxGame implements ApplicationListener, GestureListener, InputProcessor{
	public static final int FPS = 60;
	public static final int WIDTH = 1440;
	public static final int HEIGHT = 2560;
	public static final long WORLD_SEED = 9001;
	public static final int REGION_FEATURE_SIZE = 60;		//The feature size for regions in our simplex noise
	public static final int MAP_RADIUS = 10; 				//The radius of a single map area in tiles
	public static final float CAMERA_MAP_CLAMP_MIN = .85f;
	public static final float CAMERA_MAP_CLAMP_MAX = 2.25f;
	public static final float CAMERA_REGION_CLAMP_MIN = 5f;
	public static final float CAMERA_REGION_CLAMP_MAX = 12f;
	public static final long CAMERA_TRANSITION_TIME = 1000;		//The time it takes the camera to zoom in/out during transition
	public static final float FADE_MULTI_MIN = 1.12f;			//These fade multis are multiplied to CAMERA_MAP_CLAMP_MAX to determine when to start/end fading from tiles to regions
	public static final float FADE_MULTI_MAX = 1.4f;

	public enum CamState {
		MAP,
		REGION,
		TRANSITION_TO_MAP,
		TRANSITION_TO_REGION,
	}

	private World world;
	private SlimeHandler slimeHandler;

	private float initialZoom = 1.95f;			//Tracks the starting camera zoom for when zoom gestures are performed
	private int touches = 0;				//Tracks current number of touches on screen
	private Vector3 touch1 = new Vector3();
	private Vector3 touch2 = new Vector3();
	private Vector3 initialTouch1 = new Vector3();
	private Vector3 initialTouch2 = new Vector3();
	private Vector3 initialWorldCenter = new Vector3(-1,-1,-1);		//Used to keep the camera zooming relative to your pinch
	private Vector3 worldCenter = new Vector3();
	private float cameraCpuMillis;
	private CamState camState;
	private boolean isCameraClamped;
	private boolean camTransitionComplete = true;			//Only equals true when camera is not transitioning AND there are no active pinch gestures!

	private OrthographicCamera worldCamera;
	private long cameraTransitionStart;
	private OrthographicCamera uiCamera;
	private SpriteBatch batch;
	private FreeTypeFontGenerator generator;
	private BitmapFont chewy;

	private String message = "No Gesture";

	private Texture blob;

	public AssetManager manager;

	
	@Override
	public void create () {
		Gdx.app.setLogLevel(Application.LOG_INFO);

		batch = new SpriteBatch();

		manager = new AssetManager();
		manager.load("textures/SlimeTextures.atlas", TextureAtlas.class);
		manager.load("textures/RegionTextures.atlas", TextureAtlas.class);
		manager.finishLoading();

		world = new World(manager, batch);
		slimeHandler = new SlimeHandler();
		slimeHandler.spawnSlime(manager, world);


		worldCamera = new OrthographicCamera(WIDTH, HEIGHT);
		worldCamera.zoom = initialZoom;
		worldCamera.update();
		camState = CamState.MAP;
		isCameraClamped = false;
		uiCamera = new OrthographicCamera(WIDTH, HEIGHT);
		uiCamera.position.set(WIDTH/2, HEIGHT/2, 0);


		generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Chewy.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 48;//(int)(18*Gdx.graphics.getDensity());
		chewy = generator.generateFont(parameter);



		InputMultiplexer im = new InputMultiplexer();
		GestureDetector gd = new GestureDetector(this);
		im.addProcessor(gd);
		im.addProcessor(this);


		Gdx.input.setInputProcessor(im);
	}

	@Override
	public void resize(int width, int height) {

	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(.53f, .81f, .92f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		touches = 0;
		for (int i = 0; i < 20; i++) {
			if (Gdx.input.isTouched(i))
				touches++;
		}

		if (camState == CamState.TRANSITION_TO_REGION) {
			worldCamera.zoom = CAMERA_MAP_CLAMP_MAX + (CAMERA_REGION_CLAMP_MIN - CAMERA_MAP_CLAMP_MAX)*((float)(TimeUtils.nanoTime()-cameraTransitionStart)/(1000000 * CAMERA_TRANSITION_TIME));
			if (worldCamera.zoom > CAMERA_REGION_CLAMP_MIN*1.05) {
				camState = CamState.REGION;
				initialZoom = worldCamera.zoom;
			}
		}
		else if (camState == CamState.TRANSITION_TO_MAP) {
			worldCamera.zoom = CAMERA_REGION_CLAMP_MIN - (CAMERA_REGION_CLAMP_MIN - CAMERA_MAP_CLAMP_MAX)*((float)(TimeUtils.nanoTime()-cameraTransitionStart)/(1000000 * CAMERA_TRANSITION_TIME));
			if (worldCamera.zoom < CAMERA_MAP_CLAMP_MAX*.95f) {
				camState = CamState.MAP;
				initialZoom = worldCamera.zoom;
			}
		}
		else if (touches < 1) {
			camTransitionComplete = true;
			initialWorldCenter.set(-1,-1,-1);
		}


		worldCamera.update();
		uiCamera.update();

		batch.setProjectionMatrix(worldCamera.combined);
		batch.begin();

		world.render(batch, worldCamera);
		slimeHandler.render(batch, world);
		//batch.draw(blob, 0, 0);

		batch.end();



		batch.setProjectionMatrix(uiCamera.combined);
		batch.begin();
		chewy.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond() + "\n" +
				"WorldRenderTime: " + world.getCpuTime() + "%\n" +
				"isCameraClamped: " + isCameraClamped + "\n" +
				"camState: " + camState.toString() + "\n" +
				"Zoom: " + worldCamera.zoom + "\n" +
				"Rendered Tiles: " + world.getRenderedTileCount(),
				10, chewy.getLineHeight()*6);
		batch.end();
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose () {
		batch.dispose();
		world.dispose();
		chewy.dispose();
		generator.dispose();
		slimeHandler.dispose();
	}


	@Override
	public boolean touchDown(float x, float y, int pointer, int button) {
		message = "Touch down!";
		//Gdx.app.log("INFO", message);
		return true;
	}

	@Override
	public boolean tap(float x, float y, int count, int button) {
		message = "Tap performed, finger" + Integer.toString(button);
		//Gdx.app.log("INFO", message);
		return false;
	}

	@Override
	public boolean longPress(float x, float y) {
		message = "Long press performed";
		//Gdx.app.log("INFO", message);
		return true;
	}

	@Override
	public boolean fling(float velocityX, float velocityY, int button) {
		message = "Fling performed, velocity:" + Float.toString(velocityX) +
				"," + Float.toString(velocityY);
		//Gdx.app.log("INFO", message);
		return true;
	}

	@Override
	public boolean pan(float x, float y, float deltaX, float deltaY) {
		if (camState == CamState.TRANSITION_TO_MAP || camState == CamState.TRANSITION_TO_REGION)
			return true;

		message = "Pan performed, delta:" + Float.toString(deltaX) +
				"," + Float.toString(deltaY);
		worldCamera.translate(-deltaX*worldCamera.zoom, deltaY*worldCamera.zoom);
		//Gdx.app.log("INFO", message);
		return true;
	}

	@Override
	public boolean panStop(float x, float y, int pointer, int button) {
		return false;
	}

	@Override
	public boolean zoom(float initialDistance, float distance) {
		message = "Zoom performed, initial Distance:" + Float.toString(initialDistance) +
				" Distance: " + Float.toString(distance);
		//Gdx.app.log("INFO", message);
		return true;
	}

	@Override
	public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2,
						 Vector2 pointer1, Vector2 pointer2) {
		long cpuTimer = TimeUtils.nanoTime();

		if (camState == CamState.TRANSITION_TO_MAP || camState == CamState.TRANSITION_TO_REGION || !camTransitionComplete) {
			return true;
		}

		uiCamera.unproject(initialTouch1.set(initialPointer1.x, initialPointer1.y, 0));			//unprojecting all these into our Vector3s just takes screen coordinates and scales them to be the same on all devices
		uiCamera.unproject(initialTouch2.set(initialPointer2.x, initialPointer2.y, 0));			//we use the uicamera instead of the worldcamera because the uicamera doesnt move; we can get just screen coordinates
		uiCamera.unproject(touch1.set(pointer1.x, pointer1.y, 0));
		uiCamera.unproject(touch2.set(pointer2.x, pointer2.y, 0));
		if (initialWorldCenter.z == -1) {
			worldCamera.unproject(initialWorldCenter.set((initialPointer1.x + initialPointer2.x) / 2, (initialPointer1.y + initialPointer2.y) / 2, 0));
		}
		float pinchDistance = (float)Math.sqrt(Math.pow(touch1.x - touch2.x, 2) + Math.pow(touch1.y - touch2.y, 2));  										//Pythagorean theorem
		float initialPinchDistance = (float)(Math.sqrt(Math.pow(initialTouch1.x - initialTouch2.x, 2) + Math.pow(initialTouch1.y - initialTouch2.y, 2)));



		worldCamera.zoom = initialZoom * (initialPinchDistance/pinchDistance);							//Apparently a larger zoom means the camera zooms out :P
		if (camState == CamState.MAP) {
			if (worldCamera.zoom < CAMERA_MAP_CLAMP_MAX*.9f)
				isCameraClamped = false;

			if (isCameraClamped && worldCamera.zoom > CAMERA_MAP_CLAMP_MAX) {
				camState = CamState.TRANSITION_TO_REGION;
				isCameraClamped = false;
				camTransitionComplete = false;
				cameraTransitionStart = TimeUtils.nanoTime();
			}
			else
				worldCamera.zoom = MathUtils.clamp(worldCamera.zoom, CAMERA_MAP_CLAMP_MIN, CAMERA_MAP_CLAMP_MAX);
		}
		else if (camState == CamState.REGION) {
			if (worldCamera.zoom > CAMERA_REGION_CLAMP_MIN*1.1f)
				isCameraClamped = false;

			if (isCameraClamped && worldCamera.zoom < CAMERA_REGION_CLAMP_MIN) {
				camState = CamState.TRANSITION_TO_MAP;
				isCameraClamped = false;
				camTransitionComplete = false;
				cameraTransitionStart = TimeUtils.nanoTime();
			}
			else
				worldCamera.zoom = MathUtils.clamp(worldCamera.zoom, CAMERA_REGION_CLAMP_MIN, CAMERA_REGION_CLAMP_MAX);
		}

		worldCamera.update();



		worldCamera.unproject(worldCenter.set((pointer1.x + pointer2.x)/2, (pointer1.y + pointer2.y)/2, 0));
		worldCenter.sub(initialWorldCenter);
		worldCamera.position.sub(worldCenter);



		message = "Pinch performed, worldCenter: " + worldCenter.toString() + " initialWorldCenter: " + initialWorldCenter.toString();
		//Gdx.app.log("INFO", message);

		cameraCpuMillis = (float)(TimeUtils.nanoTime()-cpuTimer)/1000000;
		cameraCpuMillis = (float)((int)(cameraCpuMillis*1000))/1000;		//To truncate after 3rd decimal

		return true;
	}

	@Override
	public void pinchStop() {
		if (camState == CamState.TRANSITION_TO_MAP || camState == CamState.TRANSITION_TO_REGION)
			return;

		if (camState == CamState.MAP)
			worldCamera.zoom = MathUtils.clamp(worldCamera.zoom, CAMERA_MAP_CLAMP_MIN*1.05f, CAMERA_MAP_CLAMP_MAX*.95f);
		else if (camState == CamState.REGION)
			worldCamera.zoom = MathUtils.clamp(worldCamera.zoom, CAMERA_REGION_CLAMP_MIN*1.05f, CAMERA_REGION_CLAMP_MAX*.95f);

		if (worldCamera.zoom == CAMERA_MAP_CLAMP_MAX*.95f || worldCamera.zoom == CAMERA_REGION_CLAMP_MIN*1.05f) {
			isCameraClamped = true;
		}
		else {
			isCameraClamped = false;
		}
		initialZoom = worldCamera.zoom;
		initialWorldCenter.set(-1,-1,-1);
	}

	@Override
	public boolean keyDown(int keycode) {
		message = "Key Down";
		//Gdx.app.log("INFO", message);
		return true;
	}

	@Override
	public boolean keyUp(int keycode) {
		message = "Key up";
		//Gdx.app.log("INFO", message);
		return true;
	}

	@Override
	public boolean keyTyped(char character) {
		message = "Key typed";
		//Gdx.app.log("INFO", message);
		return true;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		message = "Touch Down";
		//Gdx.app.log("INFO", message);

		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		message = "Touch up";

		//Gdx.app.log("INFO", message);
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		message = "Touch Dragged";
		//Gdx.app.log("INFO", message);
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		message = "Mouse moved";
		//Gdx.app.log("INFO", message);
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		message = "Scrolled";
		//Gdx.app.log("INFO", message);
		return false;
	}

}

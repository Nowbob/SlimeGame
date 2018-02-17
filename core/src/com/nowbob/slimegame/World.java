package com.nowbob.slimegame;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;


/**
 * Created by Nowbob on 6/17/2017.
 */

public class World {
    private TextureAtlas tileAtlas;
    private TextureRegion grassTile;
    private TextureAtlas regionAtlas;
    private TextureRegion grassRegion;
    //private FrameBuffer mapBuffer;
    //private TextureRegion mapTexture;

    private long renderTimer;
    private float timeToRender;
    private float cpuTime;           //Measures percent of a frame (1/60th of a second) spent rendering this class. Is truncated to hundredths.
    private int renderedTileCount = 0;

    private Map[] maps;
    private Vector2 activeMap;
    private OpenSimplexNoise noise;

    public World(AssetManager manager, SpriteBatch batch) {
        this.tileAtlas = manager.get("textures/SlimeTextures.atlas", TextureAtlas.class);
        this.regionAtlas = manager.get("textures/RegionTextures.atlas", TextureAtlas.class);
        this.grassTile = this.tileAtlas.findRegion("GrassTile");
        this.grassRegion = this.regionAtlas.findRegion("GrassRegion");


        /*  THIS WHOLE CHUNK OF CODE IS FOR SAVING REGIONS BASED OFF OF TILES
        float tileW = (float)this.grassTile.getRegionWidth()/10;
        float tileH = (float)this.grassTile.getRegionHeight()/10;
        float tileDeltaW = tileW/2;
        float tileDeltaH = tileH*.75f;


        //mapBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, (int)((MyGdxGame.MAP_RADIUS*2-1)*tileW), (int)((MyGdxGame.MAP_RADIUS*2-1)*tileDeltaH), true);
        //mapBuffer.begin();
        Gdx.gl.glClearColor(0f,0f,0f,0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Matrix4 matrix = batch.getProjectionMatrix();
        //batch.getProjectionMatrix().setToOrtho2D(0,0,500,500);
        batch.begin();

        for (int y = MyGdxGame.MAP_RADIUS*2-2; y >= 0; y--) {
            for (int x = MyGdxGame.MAP_RADIUS*2-2; x >=0; x--) {
                if (x + y < MyGdxGame.MAP_RADIUS-1 || x + y > MyGdxGame.MAP_RADIUS*2-1 + MyGdxGame.MAP_RADIUS - 2) {
                    continue;
                }
                batch.draw(this.grassTile, (x*tileW + y*tileDeltaW) - (MyGdxGame.MAP_RADIUS/2-1)*tileW, y*tileDeltaH, tileW, tileH);
            }
        }

        batch.end();
        //batch.setProjectionMatrix(matrix);
        //mapBuffer.end();

        //mapTexture = new TextureRegion(mapBuffer.getColorBufferTexture());
        //mapTexture.flip(false, true);
        //mapTexture.getTexture().getTextureData().prepare();
        //Gdx.gl20.glBindFramebuffer(Gdx.gl30.GL_READ_FRAMEBUFFER, mapBuffer.getFramebufferHandle());
        //Gdx.gl30.glReadBuffer(Gdx.gl30.GL_COLOR_ATTACHMENT0);
        byte[] pixels = ScreenUtils.getFrameBufferPixels(0,0,Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);
        Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
        BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
        PixmapIO.writePNG(Gdx.files.absolute("C:/Users/Nowbob/Desktop/pic7.png"), pixmap);
        pixmap.dispose();
        */

        this.noise = new OpenSimplexNoise(MyGdxGame.WORLD_SEED);

        this.maps = new Map[7];                                 //This will store the map we're currently viewing, and it's 6 neighbors. [0] is the center, [1] is directly north, and the remaining go clockwise
        loadMaps(0,0);
    }

    public int getRenderedTileCount() {
        return  renderedTileCount;
    }

    float getCpuTime() {
        return cpuTime;
    }

    public Vector2 getActiveMap() {
        return activeMap;
    }

    void loadMaps(int centerX, int centerY) {                   //Loads the map we're viewing and it's neighbors; (0,0) is the center of the game world
        this.activeMap = new Vector2(centerX, centerY);
        this.maps[0] = new Map(centerX,     centerY     , noise);      //An important thing to note: since our tiles are pointy-topped, each map actually appears flat-topped when zoomed out,
        this.maps[1] = new Map(centerX,     centerY+1   , noise);      //so our X axis now goes northeast, and our y axis goes straight north.
        this.maps[2] = new Map(centerX+1,   centerY     , noise);
        this.maps[3] = new Map(centerX+1,   centerY-1   , noise);
        this.maps[4] = new Map(centerX,     centerY-1   , noise);
        this.maps[5] = new Map(centerX-1,   centerY     , noise);
        this.maps[6] = new Map(centerX-1,   centerY+1   , noise);

    }

    void render(SpriteBatch batch, OrthographicCamera camera) {
        this.renderTimer = TimeUtils.nanoTime();

        int tileW = this.grassTile.getRegionWidth();
        int tileH = this.grassTile.getRegionHeight();

        int tileDeltaH = tileH*3/4 - 1;         //Subtract 1 from the Height to get rid of the seam that hex tiles love so much
        int tileDeltaW = tileW/2;

        int radius = MyGdxGame.MAP_RADIUS;

        this.renderedTileCount = 0;
        for (int m = 0; m < this.maps.length; m++) {
            Vector2 mapCenter = this.maps[m].getCenterCoord();          //This tells us the tile coordinate such that (mapCenter, mapCenter) is the middle of our map
            int mapDimension = this.maps[m].getDimension();                 //Give us the number of tiles along one edge of our map
            float originX = ((mapCenter.x-(radius-1)) * tileW) + ((mapCenter.y-(radius-1)) * tileDeltaW) - tileW / 2;       //This gives us the pixel X coord of the origin tile {(0,0) in tile coords} as if the center of the map was at pixel coords (0,0)
            float originY = ((mapCenter.y-(radius-1)) * tileDeltaH) - tileH / 2;                                            //Same as above but for Y coord

            if (m == 0)
                batch.setColor(1,1,1,1);
            else {
                if (m%2 == 1)
                    batch.setColor(.95f, .95f, .95f, 1);
                else
                    batch.setColor(.92f,.92f,.92f,1);
            }


            for (int i = 0; i < this.maps[m].getMapData().length; i++) {
                int tile = this.maps[m].getMapData()[i];
                switch (tile) {
                    case 1:
                        float x = originX + ((i % mapDimension) * tileW) + ((i / mapDimension) * tileDeltaW);
                        float y = originY + ((i / mapDimension) * tileDeltaH);

                        float wMod = ((MyGdxGame.WIDTH/2)*camera.zoom);             //This block of code just culls any tiles that aren't in the current view
                        float hMod = ((MyGdxGame.HEIGHT/2)*camera.zoom);
                        if (    camera.position.x - x + wMod + tileW < tileW ||
                                camera.position.x - x - wMod         > tileW ||
                                camera.position.y - y + hMod + tileH < tileH ||
                                camera.position.y - y - hMod         > tileH)
                            break;

                        if (camera.zoom < MyGdxGame.CAMERA_MAP_CLAMP_MAX*MyGdxGame.FADE_MULTI_MAX)
                            batch.draw(this.grassTile, x, y);
                        this.renderedTileCount++;
                        break;
                }

            }
        }

        //TODO TEMPORARY
        for (int m = 0; m < this.maps.length; m++) {
            Vector2 mapCenter = this.maps[m].getCenterCoord();
            float originX = ((mapCenter.x - (radius - 1)) * tileW) + ((mapCenter.y - (radius - 1)) * tileDeltaW) - tileW / 2;       //This gives us the pixel X coord of the origin tile {(0,0) in tile coords} as if the center of the map was at pixel coords (0,0)
            float originY = ((mapCenter.y - (radius - 1)) * tileDeltaH) - tileH / 2;

            float rWidth = tileW * (MyGdxGame.MAP_RADIUS * 2 - 1);
            float rHeight = tileDeltaH * (MyGdxGame.MAP_RADIUS * 2 - 1) + (tileH * .25f);
            float clamp = MyGdxGame.CAMERA_MAP_CLAMP_MAX;
            float max = MyGdxGame.FADE_MULTI_MAX;
            float min = MyGdxGame.FADE_MULTI_MIN;
            if (camera.zoom > clamp * min) {
                float alpha = MathUtils.clamp((camera.zoom - clamp * min) / (clamp * max - clamp * min), 0f, 1f);
                batch.setColor(1, 1, 1, alpha);
                batch.draw(this.grassRegion, originX + tileW * 4.5f, originY, rWidth, rHeight);

            }
        }
        //TODO TEMPORARY

        batch.setColor(1,1,1,1);

        this.timeToRender = (float)(TimeUtils.nanoTime() - this.renderTimer)/1000000000;
        this.cpuTime = (float)((int)(this.timeToRender/(1f/MyGdxGame.FPS)*10000))/100;

    }

    public boolean isWalkableTile(int tileX, int tileY) {           //THIS ASSUMES THE ACTIVE (CENTER) MAP
        int dimension = this.maps[0].getDimension();

        if (tileX < 0 || tileY < 0 || tileX >= dimension || tileY >= dimension)
            return false;

        int index = tileY*dimension + tileX;
        switch (this.maps[0].getMapData()[index]) {
            case 1:
                return true;
            default:
                return false;
        }
    }

    private Map getMap(int mapX, int mapY) {
        for (int m = 0; m < this.maps.length; m++) {
            if (this.maps[m].getMapX() == mapX && this.maps[m].getMapY() == mapY)
                return this.maps[m];
        }
        return null;
    }

    public Vector2 getPixelCoords(int tileX, int tileY, int mapX, int mapY) {       //Returns the pixel coordinates of the CENTER of the tile, NOT the bottom left corner!
        Map map = getMap(mapX, mapY);
        if (map == null)
            return null;

        int radius = MyGdxGame.MAP_RADIUS;
        int tileW = this.grassTile.getRegionWidth();
        int tileH = this.grassTile.getRegionHeight();

        int tileDeltaH = tileH*3/4 - 1;
        int tileDeltaW = tileW/2;

        Vector2 mapCenter = map.getCenterCoord();

        float originX = ((mapCenter.x-(radius-1))*tileW) + ((mapCenter.y-(radius-1))*tileDeltaW) - tileW/2;
        float originY = ((mapCenter.y-(radius-1))*tileDeltaH) - tileH/2;

        float x = originX + (tileX * tileW) + (tileY * tileDeltaW) + tileW/2;
        float y = originY + (tileY * tileDeltaH) + tileH/2;

        return new Vector2(x, y);
    }

    void dispose() {
        this.tileAtlas.dispose();
    }

}

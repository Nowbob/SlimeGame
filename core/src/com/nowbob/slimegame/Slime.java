package com.nowbob.slimegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;


/**
 * Created by Nowbob on 6/22/2017.
 */

public class Slime {
    private float x, y;
    private int mapX, mapY;
    private float oldX, oldY;       //For keeping track of where to draw a slime while its moving
    private int tileX, tileY;       //Tile coordinates in map
    private int destX, destY;       //Tile coordinates of destination
    private long lastMoveTime;      //Nanotime timestamp of last movement
    private long restTime;          //Nanoseconds between each check for movement
    private long walkTime;          //Nanoseconds to walk one tile
    private TextureAtlas atlas;
    private TextureRegion slimeImg;

    public Slime(AssetManager manager, World world) {
        this.tileX = 9;
        this.tileY = 18;
        this.mapX = 0;
        this.mapY = 0;
        this.destX = this.tileX;
        this.destY = this.tileY;
        this.lastMoveTime = TimeUtils.nanoTime();
        this.restTime = 3L * 1000000000;
        this.walkTime = 1L * 1000000000;
        this.atlas = manager.get("textures/SlimeTextures.atlas", TextureAtlas.class);
        this.slimeImg = this.atlas.findRegion("blob");

        Vector2 pixelCoords = world.getPixelCoords(this.tileX, this.tileY, this.mapX, this.mapY);
        this.x = pixelCoords.x - this.slimeImg.getRegionWidth()/2;
        this.y = pixelCoords.y - this.slimeImg.getRegionHeight()/2;
    }

    public void render(SpriteBatch batch) {
        batch.draw(this.slimeImg, this.x, this.y);
    }

    public void dispose() {
        this.atlas.dispose();
    }

    public void update(World world) {
        if (TimeUtils.nanoTime() - this.restTime > this.lastMoveTime) {
            this.lastMoveTime = TimeUtils.nanoTime();
            this.oldX = this.x;
            this.oldY = this.y;

            int direction = MathUtils.random(6);
            switch (direction) {
                case 0:         //NorthEast
                    if (world.isWalkableTile(this.tileX, this.tileY+1)) {
                        this.destX = this.tileX;
                        this.destY = this.tileY+1;
                    }
                    break;

                case 1:         //East
                    if (world.isWalkableTile(this.tileX+1, this.tileY)) {
                        this.destX = this.tileX+1;
                        this.destY = this.tileY;
                    }
                    break;

                case 2:         //SouthEast
                    if (world.isWalkableTile(this.tileX+1, this.tileY-1)) {
                        this.destX = this.tileX+1;
                        this.destY = this.tileY-1;
                    }
                    break;

                case 3:         //SouthWest
                    if (world.isWalkableTile(this.tileX, this.tileY-1)) {
                        this.destX = this.tileX;
                        this.destY = this.tileY-1;
                    }
                    break;

                case 4:         //West
                    if (world.isWalkableTile(this.tileX-1, this.tileY)) {
                        this.destX = this.tileX-1;
                        this.destY = this.tileY;
                    }
                    break;

                case 5:         //NorthWest
                    if (world.isWalkableTile(this.tileX-1, this.tileY+1)) {
                        this.destX = this.tileX-1;
                        this.destY = this.tileY+1;
                    }
                    break;

                case 6:         //Hold Still
                    this.destX = this.tileX;
                    this.destY = this.tileY;
                    break;
            }
        }

        if (this.destX != this.tileX || this.destY != this.tileY) {
            Vector2 destination = world.getPixelCoords(this.destX, this.destY, this.mapX, this.mapY);
            Vector2 currentPos = world.getPixelCoords(this.tileX, this.tileY, this.mapX, this.mapY);
            float deltaX = destination.x - currentPos.x;
            float deltaY = destination.y - currentPos.y;

            float deltaT = (float)(TimeUtils.nanoTime() - this.lastMoveTime)/this.walkTime;

            this.x = this.oldX + (deltaX*deltaT);
            this.y = this.oldY + (deltaY*deltaT);

            if (TimeUtils.nanoTime() - this.lastMoveTime > this.walkTime) {
                this.x = destination.x - this.slimeImg.getRegionWidth()/2;
                this.y = destination.y - this.slimeImg.getRegionHeight()/2;
                this.tileX = this.destX;
                this.tileY = this.destY;
                this.lastMoveTime = TimeUtils.nanoTime();
            }
        }
    }
}

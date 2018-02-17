package com.nowbob.slimegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Created by Nowbob on 6/17/2017.
 */

public class Map {
    private int[] mapData;
    private int axisLength;
    private int mapX, mapY;             //Map coordinates of this map

    public Map(int mapX, int mapY, OpenSimplexNoise noise) {                //mapX and mapY refer to the map coordinates, where each map has it's own tiles, and the center of each map's tile
        int radius = MyGdxGame.MAP_RADIUS;                                  //is a radius apart in both the x and y directions. i.e. the center map has map coords (0,0) with the center tile being (0,0) in the world,
        this.axisLength = (radius*2)-1;                                     //the map to the northeast has map coords (1,0) and the center tile on that map is (10,10) in the world (assuming a radius of 10)
        this.mapData = new int[this.axisLength*axisLength];
        this.mapX = mapX;
        this.mapY = mapY;
        int featureSize = MyGdxGame.REGION_FEATURE_SIZE;

        if (!checkMapDimensions(this.mapData)) {
            Gdx.app.error("MapClass", "Map was invalid dimensions");
            this.mapData = new int[]{0};
        }
        else {                                                              //Time to generate the map
            int centerTileX = mapX*(radius) - mapY*(radius-1);
            int centerTileY = mapX*(radius-1) + mapY*(radius*2-1);
            for (int y = 0; y < this.axisLength; y++) {
                for (int x = 0; x < this.axisLength; x++) {
                    int mapIndex = x + y*this.axisLength;
                    if (x + y < radius-1 || x + y > this.axisLength + radius - 2) {
                        this.mapData[mapIndex] = 0;
                        continue;
                    }
                    else {
                        int tileX = (centerTileX - (radius-1)) + x;
                        int tileY = (centerTileY - (radius-1)) + y;
                        double n = noise.eval((float)tileX/(featureSize/10), (float)tileY/(featureSize/10));

                        this.mapData[mapIndex] = n > -0.5 ? 1 : 0;  //TODO THIS IS TEMPORARY

                    }
                }
            }
        }

    }

    public int getMapX() {
        return this.mapX;
    }

    public int getMapY() {
        return  this.mapY;
    }

    public int[] getMapData() {
        return this.mapData;
    }

    public Vector2 getCenterCoord() {                   //Returns a vector of the tile coordinates pointing to the center of this map
        int radius = MyGdxGame.MAP_RADIUS;
        return new Vector2(this.mapX*(radius) - this.mapY*(radius-1), this.mapX*(radius-1) + this.mapY*(radius*2-1));

    }

    public int getDimension() {
        return this.axisLength;
    }

    private boolean checkMapDimensions(int[] map) {         //Checks to make sure our mapdata is a perfect square of an odd number (nothing else will fit a hexagon perfectly!)
        int x = (int) Math.sqrt(map.length);
        if (Math.pow(x, 2) == map.length && x%2 == 1) {
            return true;
        }
        else
            return false;
    }
}

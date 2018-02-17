package com.nowbob.slimegame;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;

/**
 * Created by Nowbob on 6/22/2017.
 */

public class SlimeHandler {
    private ArrayList<Slime> allSlimes;
    private Texture slimeImg;

    public SlimeHandler() {
        this.allSlimes = new ArrayList<Slime>();
    }

    public void render(SpriteBatch batch, World world) {
        for (int i = 0; i < this.allSlimes.size(); i++) {
            this.allSlimes.get(i).update(world);
            this.allSlimes.get(i).render(batch);
        }
    }

    public void dispose() {
        for (int i = 0; i < this.allSlimes.size(); i++) {
            this.allSlimes.get(i).dispose();
        }
    }

    public void spawnSlime(AssetManager manager, World world) {
        this.allSlimes.add(new Slime(manager, world));
    }
}

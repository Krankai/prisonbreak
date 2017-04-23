/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prisonbreak.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.prisonbreak.game.entities.Player;

/**
 *
 * @author krankai
 */
public class MapControlRenderer extends OrthogonalTiledMapRenderer implements InputProcessor {
        
    public static final int WORLD_WIDTH = 100 * 32;
    public static final int WORLD_HEIGHT = 100 * 32;
    
    private Player player;
    private OrthographicCamera camera;
    private final int drawSpritesAfterLayer = 4;
    
    public MapControlRenderer(TiledMap map) {
        super(map);
        
        // create new player
        player = new Player();
        
        // create camera
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.setToOrtho(false);
        this.setView(camera);
    }
    
    public Player getPlayer() {
        return player;
    }
    
    // allow camera to move along with player
    public void moveCamera() {
        camera.position.x = MathUtils.clamp(player.x,
                camera.viewportWidth / 2f,
                WORLD_WIDTH - camera.viewportWidth / 2f);
        camera.position.y = MathUtils.clamp(player.y,
                camera.viewportHeight / 2,
                WORLD_HEIGHT - camera.viewportHeight / 2);
        
        camera.update();
        this.setView(camera);
    }
    
    @Override
    public void render() {
        player.update();
        moveCamera();
        
        beginRender();
        
        int currentLayer = 0;
        for (MapLayer layer : map.getLayers()) {
            if (layer.isVisible()) {
                // tile layer
                if (layer instanceof TiledMapTileLayer) {
                    renderTileLayer((TiledMapTileLayer) layer);
                    ++currentLayer;
                    
                    // layer to draw characters
                    if (currentLayer == drawSpritesAfterLayer) {
                        // draw player
                        player.getSprite().draw(this.getBatch());
                    }
                } 
                // object layer
                else {
                    for (MapObject object : layer.getObjects()) {
                        renderObject(object);
                    }
                }
            }
        }
        
        endRender();
    }
    
    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.LEFT:
                player.setLeftMove(true);
                break;
            case Input.Keys.RIGHT:
                player.setRightMove(true);
                break;
            case Input.Keys.UP:
                player.setUpMove(true);
                break;
            case Input.Keys.DOWN:
                player.setDownMove(true);
                break;
            case Input.Keys.SPACE:
                player.sleepWakeup();
                break;
        }
        
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        switch (keycode) {
            case Input.Keys.LEFT:
                player.setLeftMove(false);
                break;
            case Input.Keys.RIGHT:
                player.setRightMove(false);
                break;
            case Input.Keys.UP:
                player.setUpMove(false);
                break;
            case Input.Keys.DOWN:
                player.setDownMove(false);
                break;
        }
        
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
    
    @Override
    public void dispose() {
        player.dispose();
    }
    
}

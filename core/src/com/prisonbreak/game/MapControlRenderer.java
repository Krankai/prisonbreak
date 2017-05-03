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
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.prisonbreak.game.entities.Player;

/**
 *
 * @author krankai
 */
public class MapControlRenderer extends OrthogonalTiledMapRenderer implements InputProcessor {
        
    public static final int WORLD_WIDTH = 100 * 32;
    public static final int WORLD_HEIGHT = 100 * 32;
    
    private final Player player;
    private final OrthographicCamera camera;
    private final int drawSpritesAfterLayer = this.map.getLayers().getIndex("Walls") + 1;
    
    public enum STATE {
        ONGOING, WIN, LOSE
    }
    private STATE state;      
    
    private int[][] blockedWallUnitGrid;
    private final MapObjects staticObjects;
    private final MapObject winningLocation;
    
    public MapControlRenderer(TiledMap map) {
        super(map);
        state = STATE.ONGOING;
        
        // create new player
        player = new Player();
        player.setMapControlRenderer(this);         // add map to player
        setPlayerToSpawm();                         // add Player at spawming point of the map
        
        // extract a grid identifying position of walls -> for detecting collision
        extractBlockedWalls();      // stored in blockedWallUnitGrid
        
        // get list of static (blocked) objects
        staticObjects = map.getLayers().get("ObjectCollision").getObjects();
        
        // extract winning location
        winningLocation = map.getLayers().get("SpecialLocations").getObjects().get("winningPoint");
        
        // create camera
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.translate(player.x, player.y);
    }
    
    public STATE getCurrentState() {
        return state;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public OrthographicCamera getCamera() {
        return camera;
    }
    
    public int[][] getBlockedWallsGrid() {
        return blockedWallUnitGrid;
    }
    
    public MapObjects getStaticObjects() {
        return staticObjects;
    }
    
    public MapObject getWinningLocation() {
        return winningLocation;
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
    
    private void extractBlockedWalls() {
        // get map objects of ObjectLayer "Collision"
        MapLayer layer = map.getLayers().get("WallCollision");
        MapObjects objects = layer.getObjects();
        
        // fill in blockedWallUnitGrid with position of static blocked map objects
        // 1 -> blocked     0 -> not blocked
        blockedWallUnitGrid = new int[100][100];
        boolean isBlocked = false;
        for (int indexX = 0; indexX < 100; ++indexX) {
            for (int indexY = 0; indexY < 100; ++indexY) {
                // convert to position in world map
                Vector2 worldPosition = new Vector2(indexX * 32f + 16f, indexY * 32f + 16f);  // consider center
                
                // check whether that position is "blocked" or not
                for (MapObject o : objects) {
                    // retrieve RectangleMapObject objects that are blocked
                    if (o.isVisible() && o instanceof RectangleMapObject &&
                            o.getProperties().get("blocked") != null &&
                            o.getProperties().get("blocked").equals(new Boolean(true))) {
                        RectangleMapObject rectObject = (RectangleMapObject) o;
                    
                        if (rectObject.getRectangle().contains(worldPosition)) {
                            isBlocked = true;
                            break;
                        }
                    }
                }
                
                // fill 1 / 0 into the corrsponding tile index
                if (isBlocked) {
                    blockedWallUnitGrid[indexX][indexY] = 1;
                } else {
                    blockedWallUnitGrid[indexX][indexY] = 0;
                }
                
                // reset flag
                isBlocked = false;
            }
        }
        
//        Gdx.app.log("Test: ", " " + blockedWallUnitGrid[13][38] + " " + blockedWallUnitGrid[13][39] +
//                " " + blockedWallUnitGrid[12][38] + " " + blockedWallUnitGrid[12][39]);
    }
    
    // set Player position to spawming location
    private void setPlayerToSpawm() {
        MapObject spawmPoint = map.getLayers().get("SpecialLocations").getObjects().get("spawmPoint");
        RectangleMapObject rectSpawmPoint = (RectangleMapObject) spawmPoint;
        
//        player.x = rectSpawmPoint.getRectangle().getX();
//        player.y = rectSpawmPoint.getRectangle().getY();
        player.x = 71 * 32;
        player.y = 82 * 32;
    }
    
    // checking winning condition
    private boolean winGame() {
        RectangleMapObject rectLoc = (RectangleMapObject) winningLocation;
        
        if (rectLoc.getRectangle().contains(player.getSprite().getBoundingRectangle()))
            return true;
        return false;
    }
    
    @Override
    public void render() {
        player.update();
        moveCamera();
        
        // update winning state
        if (winGame()) state = STATE.WIN;
        
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
            case Input.Keys.A:
                player.setLeftMove(true);
                break;
            case Input.Keys.RIGHT:
            case Input.Keys.D:
                player.setRightMove(true);
                break;
            case Input.Keys.UP:
            case Input.Keys.W:
                player.setUpMove(true);
                break;
            case Input.Keys.DOWN:
            case Input.Keys.S:
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
            case Input.Keys.A:
                player.setLeftMove(false);
                break;
            case Input.Keys.RIGHT:
            case Input.Keys.D:
                player.setRightMove(false);
                break;
            case Input.Keys.UP:
            case Input.Keys.W:
                player.setUpMove(false);
                break;
            case Input.Keys.DOWN:
            case Input.Keys.S:
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
        map.dispose();
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prisonbreak.game.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.prisonbreak.game.MapControlRenderer;

/**
 *
 * @author krankai
 */
public class Player {
    
    private static final int FRAME_COLS = 4;
    private static final int FRAME_ROWS = 1;
    
    private final TextureRegion[] playerFrames;   // 0: down/right , 1: up, 2: left, 3: sleep
    private TextureRegion currentTexture;
    private final Texture sheet;      // contain spritesheet for player
    private Sprite sprite;
    private TiledMap map;       // the map player is in
    
    private boolean moveLeft;
    private boolean moveRight;
    private boolean moveUp;
    private boolean moveDown;
    private boolean sleep;      // for fun :)
    
    public float x;         // position x
    public float y;         // position y
    public final int width;
    public final int height;
    public final float velocity = 32.0f * 5;
    
    private int[][] blockedUnitGrid;
    
    public Player() {
        // load sheet image
        sheet = new Texture(Gdx.files.internal("prisoner.png"));
        
        // initialize position and size (and, sleep state)
        x = y = 0f;
        width = sheet.getWidth() / FRAME_COLS;
        height = sheet.getHeight() / FRAME_ROWS;
        sleep = false;
        
        // split sheet image into multiple single images
        TextureRegion[][] tmp = TextureRegion.split(sheet, width, height);
        
        // place all images into a 1D array
        playerFrames = new TextureRegion[FRAME_COLS * FRAME_ROWS];
        int index = 0;
        for (int i = 0; i < FRAME_ROWS; ++i) {
            for (int j = 0; j < FRAME_COLS; ++j) {
                playerFrames[index++] = tmp[i][j];
            }
        }
        
        // initialize the current image
        currentTexture = playerFrames[0];
        
        // initialize direction flags
        moveLeft = moveRight = moveUp = moveDown = false;
        
        // sprite
        sprite = new Sprite(currentTexture);
        sprite.setX(x);
        sprite.setY(y);
    }
    
    public Sprite getSprite() {
        return sprite;
    }
    
    public void extractBlockedMapObjects() {
        // get map objects of ObjectLayer "Collision"
        MapLayer layer = map.getLayers().get("Collision");
        MapObjects objects = layer.getObjects();    
        
        // fill in blockedUnitGrid with position of static blocked map objects
        // 1 -> blocked     0 -> not blocked
        blockedUnitGrid = new int[100][100];
        boolean isBlocked = false;
        for (int indexX = 0; indexX < 100; ++indexX) {
            for (int indexY = 0; indexY < 100; ++indexY) {
                // convert to position in world map
                Vector2 worldPosition = new Vector2(indexX * 32f + 16f, indexY * 32f + 16f);  // consider center
                
                // check whether that position is "blocked" or not
                for (MapObject o : objects) {
                    // retrieve RectangleMapObject objects that are blocked
                    if (o.isVisible() && o instanceof RectangleMapObject &&
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
                    blockedUnitGrid[indexX][indexY] = 1;
                } else {
                    blockedUnitGrid[indexX][indexY] = 0;
                }
                
                // reset flag
                isBlocked = false;
            }
        }
    }
    
    public void setMap(TiledMap map) {
        this.map = map;
    }
    
    public void dispose() {
       sheet.dispose();
    }
    
    public void setLeftMove(boolean t) {
        // set other direction flags to false
        if (moveRight) moveRight = false;
        if (moveUp) moveUp = false;
        if (moveDown) moveDown = false;
        
        // set to move left or stop moving left, based on user signal
        moveLeft = t;
    }
    
    public void setRightMove(boolean t) {
        if (moveLeft) moveLeft = false;
        if (moveUp) moveUp = false;
        if (moveDown) moveDown = false;
        
        moveRight = t;
    }
    
    public void setUpMove(boolean t) {
        if (moveLeft) moveLeft = false;
        if (moveRight) moveRight = false;
        if (moveDown) moveDown = false;
        
        moveUp = t;
    }
    
    public void setDownMove(boolean t) {
        if (moveLeft) moveLeft = false;
        if (moveUp) moveUp = false;
        if (moveRight) moveRight = false;
        
        moveDown = t;
    }
    
    public void sleepWakeup() {
        if (!sleep) {
            if (moveLeft) moveLeft = false;
            if (moveRight) moveRight = false;
            if (moveUp) moveUp = false;
            if (moveDown) moveDown = false;

            sleep = true;
            currentTexture = playerFrames[3];
        } else {
            sleep = false;
            currentTexture = playerFrames[0];
        }
    }
    
    // check various kinds of collision
    //      collision with static objects in the map
    //      ...
    private boolean haveCollision(float newX, float newY) {
        // if out of world map -> no need to check -> stop checking
        if (newX < 0 || newX + width > MapControlRenderer.WORLD_WIDTH ||
                newY < 0 || newY + height > MapControlRenderer.WORLD_HEIGHT) {
            return true;
        }
        
        /* With static objects */
        
        // position of two points that bound the Player's image
        Vector2 lowerLeft = new Vector2((int) (newX / 32f), (int) (newY / 32f));
        Vector2 upperRight =
                new Vector2((int) Math.ceil((newX + width) / 32f) - 1, (int) Math.ceil((newY + height) / 32f) - 1);
        
        // check all the tiles (32x32 pixels) the Player's image accounts for
        for (int i = (int) lowerLeft.x; i <= (int) upperRight.x; ++i) {
            for (int j = (int) lowerLeft.y; j <= (int) upperRight.y; ++j) {
                if (blockedUnitGrid[i][j] == 1) return true;
            }
        }
        
        
        return false;
    }
    
    public void updateMotion() {
        float amountX = velocity * Gdx.graphics.getDeltaTime();
        float amountY = velocity * Gdx.graphics.getDeltaTime();
 
        // move player along setting direction
        if (moveLeft) {
            currentTexture = playerFrames[2];
            x -= amountX;
            
            if (haveCollision(x, y)) {
                x += amountX;
                moveLeft = false;
            }
        }
        if (moveRight) {
            currentTexture = playerFrames[0];
            x += amountX;
            
            if (haveCollision(x, y)) {
                x -= amountX;
                moveRight = false;
            }
        }
        if (moveUp) {
            currentTexture = playerFrames[1];
            y += amountY;
            
            if (haveCollision(x, y)) {
                y -= amountX;
                moveUp = false;
            }
        }
        if (moveDown) {
            currentTexture = playerFrames[0];
            y -= amountY;
            
            if (haveCollision(x, y)) {
                y += amountY;
                moveDown = false;
            } 
       }
        
        // check for out of bounds
        x = MathUtils.clamp(x, 0,
                MapControlRenderer.WORLD_WIDTH - currentTexture.getRegionWidth());
        y = MathUtils.clamp(y, 0,
                MapControlRenderer.WORLD_HEIGHT - currentTexture.getRegionHeight());
    }
    
    public void update() {
        if (!sleep) {
            updateMotion();
        }
        
        // update sprite
        sprite = new Sprite(currentTexture);
        sprite.setX(x);
        sprite.setY(y);
    }
    
}
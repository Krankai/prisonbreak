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
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.prisonbreak.game.MapControlRenderer;

/**
 *
 * @author krankai
 */
public abstract class Character {
    
    protected static final int FRAME_COLS = 5;
    protected static final int FRAME_ROWS = 1;
    
    protected TextureRegion[] characterFrames;   // 0: down , 1: up, 2: left, 3: right, 4: sleep
    protected TextureRegion currentTexture;
    protected Texture sheet;                // contain spritesheet for player
    protected Sprite sprite;
    protected MapControlRenderer renderer; 
    
    protected boolean moveLeft;
    protected boolean moveRight;
    protected boolean moveUp;
    protected boolean moveDown;
    protected boolean sleep;      // for fun :)
    protected String currentDirection;
    
    public float x;         // position x
    public float y;         // position y
    public int width;
    public int height;
    public final float velocity = 32.0f * 6;
    
    public Character(String tileSheetName) {
        // load sheet image
        sheet = new Texture(Gdx.files.internal(tileSheetName));
        
        // initialize position and size (and, sleep state)
        x = y = 0f;
        width = sheet.getWidth() / FRAME_COLS;      // 64
        height = sheet.getHeight() / FRAME_ROWS;    // 64
        sleep = false;
        
        // split sheet image into multiple single images
        TextureRegion[][] tmp = TextureRegion.split(sheet, width, height);
        
        // place all images into a 1D array
        characterFrames = new TextureRegion[FRAME_COLS * FRAME_ROWS];
        int index = 0;
        for (int i = 0; i < FRAME_ROWS; ++i) {
            for (int j = 0; j < FRAME_COLS; ++j) {
                characterFrames[index++] = tmp[i][j];
            }
        }
    }
    
    public Sprite getSprite() {
        return sprite;
    }
    
    public String getCurrentDirection() {
        return currentDirection;
    }
    
    public void setMapControlRenderer(MapControlRenderer renderer) {
        this.renderer = renderer;
    }
    
    public void dispose() {
       sheet.dispose();
    }
    
    // set texture based on current direction
    public void setTexture() {
        if (currentDirection.equalsIgnoreCase("down") || currentDirection.equalsIgnoreCase("none")) {
            currentTexture = characterFrames[0];
        } else if (currentDirection.equalsIgnoreCase("up")) {
            currentTexture = characterFrames[1];
        } else if (currentDirection.equalsIgnoreCase("left")) {
            currentTexture = characterFrames[2];
        } else {
            currentTexture = characterFrames[3];
        }
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
            currentTexture = characterFrames[4];
        } else {
            sleep = false;
            currentTexture = characterFrames[0];
        }
        
        currentDirection = "none";
    }
    
    // check various kinds of collision
    //      collision with other 'Character's (Player/Guard)
    //      collision with static (blocked) objects in the map
    //      collision with locked doors in the map
    //      collision with walls in the map
    //      ...
    private boolean haveCollision(float newX, float newY) {
        // if out of world map -> no need to check -> stop checking
        if (newX < 0 || newX + width > MapControlRenderer.WORLD_WIDTH ||
                newY < 0 || newY + height > MapControlRenderer.WORLD_HEIGHT) {
            return true;
        }
        
        // get bounding rectangle of current Character
        Rectangle characterBounding = new Rectangle(newX, newY, width, height);
        
        /* With Guard (when as Player) */
        
        // if current character == Player
        float offset = 4f;
        if (this instanceof Player) {
            Rectangle rectPlayer = new Rectangle(characterBounding.getX() + offset,
                    characterBounding.getY() + offset,
                    characterBounding.getWidth()-2*offset,
                    characterBounding.getHeight()-2*offset);
            
            // check with Guards
            for (Guard guard : renderer.getListGuards()) {
                Rectangle rectGuard = new Rectangle(guard.getSprite().getBoundingRectangle().getX() + offset,
                    guard.getSprite().getBoundingRectangle().getY() + offset,
                    guard.getSprite().getBoundingRectangle().getWidth()-2*offset,
                    guard.getSprite().getBoundingRectangle().getHeight()-2*offset);
                
                if (rectPlayer.overlaps(rectGuard))
                    return true;
            }
        }
        
        /* End */
        
        /* With static (blocked) objects */
        
        // consider each objects
        for (MapObject object : renderer.getStaticObjects()) {
            // cast to rectangle object
            if (object.isVisible() && object.getProperties().get("blocked", Boolean.class) != null &&
                    object.getProperties().get("blocked", Boolean.class) &&
                    object instanceof RectangleMapObject) {
                RectangleMapObject rectObject = (RectangleMapObject) object;
                
                // check for overlapping ~ collision
                if (rectObject.getRectangle().overlaps(characterBounding)) return true;
            }
        }
        
        /* End */
        
        /* With locked doors */
        
        // for each object
        for (MapObject object : renderer.getDoorObjects()) {
            if (object.isVisible() && (object.getProperties().get("locked", null, Boolean.class) != null)
                    && object.getProperties().get("locked", Boolean.class)
                    && object instanceof RectangleMapObject) {
                RectangleMapObject rectObject = (RectangleMapObject) object;
                
                if (rectObject.getRectangle().overlaps(characterBounding)) return true;
            }
        }
        
        /* End */
        
        /* With walls */
        
        // position of two points that bound the Player's image
        int lowerBoundX, lowerBoundY, upperBoundX, upperBoundY, temp;
        float alpha = (float) 0.25;
        
        temp = (int) (newX / 32f);                  // account for less than 12.5% of the
        if ((newX / 32f - temp) > (1 - alpha)) {    // left tile -> do not consider
            lowerBoundX = temp + 1;
        } else {                                    // otherwise; consider when checking
            lowerBoundX = temp;                     // for collision
        }
        
        temp = (int) (newY / 32f);                  // account for less than 12.5% of the
        if ((newY / 32f - temp) > (1 - alpha)) {    // below tile -> do not consider
            lowerBoundY = temp + 1;
        } else {
            lowerBoundY = temp;
        }
        
        temp = (int) ((newX + width) / 32f);            // account for less than 12.5% of the
        if (((newX + width) / 32f - temp) < alpha) {    // right tile -> do not consider
            upperBoundX =  temp - 1;
        } else {
            upperBoundX = temp;
        }
        
        temp = (int) ((newY + height) / 32f);           // account for less than 12.5% of the
        if (((newY + height) / 32f - temp) < alpha) {   // above tile -> do not consider
            upperBoundY = temp - 1;
        } else {
            upperBoundY = temp;
        }
        
        // check all the tiles (32x32 pixels) the Player's image accounts for
        int[][] grid = renderer.getBlockedWallsGrid();
        for (int i = (int) lowerBoundX; i <= (int) upperBoundX; ++i) {
            for (int j = (int) lowerBoundY; j <= (int) upperBoundY; ++j) {
                if (grid[i][j] == 1) return true;
            }
        }
        
        /* End */
        
        
        return false;
    }
    
    public void updateMotion() {
        float amountX = velocity * Gdx.graphics.getDeltaTime();
        float amountY = velocity * Gdx.graphics.getDeltaTime();
 
        // move player along setting direction
        if (moveLeft) {
//            currentTexture = characterFrames[2];
            currentDirection = "left";
            setTexture();
            x -= amountX;
            
            if (haveCollision(x, y)) {
                x += amountX;
//                moveLeft = false;
            }
        }
        if (moveRight) {
//            currentTexture = characterFrames[3];
            currentDirection = "right";
            setTexture();
            x += amountX;
            
            if (haveCollision(x, y)) {
                x -= amountX;
//                moveRight = false;
            }
        }
        if (moveUp) {
//            currentTexture = characterFrames[1];
            currentDirection = "up";
            setTexture();
            y += amountY;
            
            if (haveCollision(x, y)) {
                y -= amountX;
//                moveUp = false;
            }
        }
        if (moveDown) {
//            currentTexture = characterFrames[0];
            currentDirection = "down";
            setTexture();
            y -= amountY;
            
            if (haveCollision(x, y)) {
                y += amountY;
//                moveDown = false;
            } 
       }
        
        // check for out of bounds
        x = MathUtils.clamp(x, 0,
                MapControlRenderer.WORLD_WIDTH - currentTexture.getRegionWidth());
        y = MathUtils.clamp(y, 0,
                MapControlRenderer.WORLD_HEIGHT - currentTexture.getRegionHeight());
        
//        Gdx.app.log("Player:", x + " " + y + " " + (x + width) + " " + (y + height));
    }
    
    // update position + corresponding image for the Character
    public abstract void update();
    
}

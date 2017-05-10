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
import com.badlogic.gdx.utils.Array;
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
    private final Texture sheet;                // contain spritesheet for player
    private Sprite sprite;
    private MapControlRenderer renderer;               
    private Array<Item> inventory;
    
    private boolean moveLeft;
    private boolean moveRight;
    private boolean moveUp;
    private boolean moveDown;
    private boolean sleep;      // for fun :)
    private String currentDirection;
    
    public float x;         // position x
    public float y;         // position y
    public final int width;
    public final int height;
    public final float velocity = 32.0f * 6;
    
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
        
        // set current direction to "none"
        currentDirection = "none";
        
        // initialize Player's inventory
        inventory = new Array<Item>();
    }
    
    public Sprite getSprite() {
        return sprite;
    }
    
    public String getCurrentDirection() {
        return currentDirection;
    }
    
    public Array<Item> getInventory() {
        return inventory;
    }
    
    public void setMapControlRenderer(MapControlRenderer renderer) {
        this.renderer = renderer;
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
        
        currentDirection = "none";
    }
    
    // add item to inventory
    public void addItem(Item item) {
        inventory.add(item);
    }
    
    // check various kinds of collision
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
        
        // get bounding rectangle of Player
        Rectangle playerBounding = new Rectangle(newX, newY, width, height);
        
        /* With static (blocked) objects */
        
        // consider each objects
        for (MapObject object : renderer.getStaticObjects()) {
            // cast to rectangle object
            if (object.isVisible() && (object.getProperties().get("blocked", Boolean.class) != null)
                    && object instanceof RectangleMapObject) {
                RectangleMapObject rectObject = (RectangleMapObject) object;
                
                // check for overlapping ~ collision
                if (rectObject.getRectangle().overlaps(playerBounding)) return true;
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
                
                if (rectObject.getRectangle().overlaps(playerBounding)) return true;
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
            currentTexture = playerFrames[2];
            currentDirection = "left";
            x -= amountX;
            
            if (haveCollision(x, y)) {
                x += amountX;
//                moveLeft = false;
            }
        }
        if (moveRight) {
            currentTexture = playerFrames[0];
            currentDirection = "right";
            x += amountX;
            
            if (haveCollision(x, y)) {
                x -= amountX;
//                moveRight = false;
            }
        }
        if (moveUp) {
            currentTexture = playerFrames[1];
            currentDirection = "up";
            y += amountY;
            
            if (haveCollision(x, y)) {
                y -= amountX;
//                moveUp = false;
            }
        }
        if (moveDown) {
            currentTexture = playerFrames[0];
            currentDirection = "down";
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
    
    public void update() {
        if (!sleep) {
            updateMotion();
        }
        
//        Gdx.app.log("direction: ", getCurrentDirection());
        
        // update sprite
        sprite = new Sprite(currentTexture);
        sprite.setX(x);
        sprite.setY(y);
    }
    
}

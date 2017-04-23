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
import com.badlogic.gdx.math.MathUtils;
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
    
    private boolean moveLeft;
    private boolean moveRight;
    private boolean moveUp;
    private boolean moveDown;
    private boolean sleep;      // for fun :)
    
    public float x;     // position x
    public float y;     // position y
    public final float velocity = 32.0f * 5;
    
    public Player() {
        // load sheet image
        sheet = new Texture(Gdx.files.internal("prisoner.png"));
        
        // split sheet image into multiple single images
        TextureRegion[][] tmp = TextureRegion.split(sheet, sheet.getWidth() / FRAME_COLS, sheet.getHeight() / FRAME_ROWS);
        
        // place all images into a 1D array
        playerFrames = new TextureRegion[FRAME_COLS * FRAME_ROWS];
        int index = 0;
        for (int i = 0; i < FRAME_ROWS; ++i) {
            for (int j = 0; j < FRAME_COLS; ++j) {
                playerFrames[index++] = tmp[i][j];
            }
        }
        
        // initialize position
        x = y = 0f;
        
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
    
    public void updateMotion() {
        float amountX = velocity * Gdx.graphics.getDeltaTime();
        float amountY = velocity * Gdx.graphics.getDeltaTime();
        
        // move player along setting direction
        if (moveLeft) {
            currentTexture = playerFrames[2];
            x -= amountX;
        }
        if (moveRight) {
            currentTexture = playerFrames[0];
            x += amountX;
        }
        if (moveUp) {
            currentTexture = playerFrames[1];
            y += amountY;
        }
        if (moveDown) {
            currentTexture = playerFrames[0];
            y -= amountY;
        }
        
        // check for out of bounds
        x = MathUtils.clamp(x, 0,
                MapControlRenderer.WORLD_WIDTH - currentTexture.getRegionWidth());
        y = MathUtils.clamp(y, 0,
                MapControlRenderer.WORLD_HEIGHT - currentTexture.getRegionHeight());
    }
    
    public void update() {
        if (sleep) return;
        updateMotion();
        
        // update sprite
        sprite = new Sprite(currentTexture);
        sprite.setX(x);
        sprite.setY(y);
    }
    
}

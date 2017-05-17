/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prisonbreak.game.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;

/**
 *
 * @author krankai
 */
public class StationGuard extends Guard {
    
    public StationGuard(String tileSheetName, String direction, float x, float y) {
        super(tileSheetName);
        
        // set direction flags
        moveLeft = moveRight = moveUp = moveDown = false;
        
        // set (fixed) direction
        currentDirection = direction;
        
        // set (fixed) texture; based on current direction
        setTexture();
        
        // set position
        this.x = x;
        this.y = y;
        
        // set (fixed) sprite
        sprite = new Sprite(currentTexture);
        sprite.setX(this.x);
        sprite.setY(this.y);
    }
    
    @Override
    public void setLeftMove(boolean t) {
        
    }
    
    @Override
    public void setRightMove(boolean t) {
        
    }
    
    @Override
    public void setDownMove(boolean t) {
        
    }
    
    @Override
    public void setUpMove(boolean t) {
        
    }
    
    @Override
    public void sleepWakeup() {
        
    }
    
    @Override
    public void updateMotion() {
        
    }
    
    @Override
    public void update() {
        
    }
    
}

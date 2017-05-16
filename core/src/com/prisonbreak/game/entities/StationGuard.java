/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prisonbreak.game.entities;

/**
 *
 * @author krankai
 */
public class StationGuard extends Guard {
    
    public StationGuard(String tileSheetName, int id, String direction) {
        super(tileSheetName, id);
        
        // set (fixed) direction
        currentDirection = direction;
        
        // set (fixed) texture; based on current direction
        setTexture();
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

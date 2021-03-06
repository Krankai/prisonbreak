/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prisonbreak.game.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Array;

/**
 *
 * @author krankai
 */
public class Player extends Character {
                  
    private final Array<Item> inventory;
    
    public Player() {
        super("prisoner.png");
        
        // initialize the current image
        currentTexture = characterFrames[0];
        
        // sprite
        sprite = new Sprite(currentTexture);
        sprite.setX(x);
        sprite.setY(y);
        
        // initialize direction flags
        moveLeft = moveRight = moveUp = moveDown = false;
        
        // set current direction to "none"
        currentDirection = "none";
        
        // initialize Player's inventory
        inventory = new Array<Item>();
    }
    
    public Array<Item> getInventory() {
        return inventory;
    }
    
    // add item to inventory
    public void addItem(Item item) {
        inventory.add(item);
    }
    
    @Override
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
    
    @Override
    public void dispose() {
        super.dispose();
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prisonbreak.game.entities;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 *
 * @author user
 */
public abstract class Guard extends Character {
    
    private Array<Rectangle> detectAreas;       // arrays holding detection areas for 4 directions
                                                //      1 -> up , 2 -> down, 3 -> left, 4 -> right
                                                // if Player steps in the current detection area -> GAME OVER
    private final int scale = 3;    // scale of the detection area compare to Character's image
    
    public Guard() {
        super();
        
        initializeImages("guard.png");
        
        // initialize detection areas of the current guard (3 times the size of Character's image)
        detectAreas = new Array<Rectangle>();
        detectAreas.add(new Rectangle(x, y + height, width, height * scale));           // up
        detectAreas.add(new Rectangle(x, y - height * scale, width, height * scale));   // down
        detectAreas.add(new Rectangle(x - width * scale, y, width * scale, height));    // left
        detectAreas.add(new Rectangle(x + width, y, width * scale, height));            // right
    }
    
    // return the current detection area, based on the direction the Guard is facing
    public Rectangle getDetectArea(String direction) {
        if ("up".equals(currentDirection)) {
            return detectAreas.get(0);
        } else if ("down".equals(currentDirection)) {
            return detectAreas.get(1);
        } else if ("left".equals(currentDirection)) {
            return detectAreas.get(2);
        } else if ("right".equals(currentDirection)) {
            return detectAreas.get(3);
        } else {
            // default: "none" direction -> return the "down" one
            return detectAreas.get(1);
        }
    }
    
}

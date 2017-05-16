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
    
    private final int guardID;
    private Array<Rectangle> detectAreas;       // arrays holding detection areas for 4 directions
                                                //      1 -> up , 2 -> down, 3 -> left, 4 -> right
                                                // if Player steps in the current detection area -> GAME OVER
    private final int scale = 3;    // scale of the detection area compare to Character's image
    
    public Guard(String tileSheetName, int id) {
        super(tileSheetName);
        
        // set ID
        guardID = id;
        
        // initialize detection areas of the current guard (3 times the size of Character's image)
        detectAreas = new Array<Rectangle>();
        detectAreas.add(new Rectangle(x, y + height, width, height * scale));           // up
        detectAreas.add(new Rectangle(x, y - height * scale, width, height * scale));   // down
        detectAreas.add(new Rectangle(x - width * scale, y, width * scale, height));    // left
        detectAreas.add(new Rectangle(x + width, y, width * scale, height));            // right
    }
    
    public int getGuardID() {
        return guardID;
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
    
    // perform detection - looking for "prisoner" (Player)
    // return true if Player steps in the detection area > 50%
    //        false otherwise
    public boolean detectPlayer() {
        Rectangle playerBounding = renderer.getPlayer().getSprite().getBoundingRectangle();
        Rectangle detectionArea = getDetectArea(currentDirection);
        
        // if Player steps in detection area
        if (playerBounding.overlaps(detectionArea)) {
            float percent = 0.5f;
            
            // four bounding points of playerBounding
            float xmin = playerBounding.getX();
            float ymin = playerBounding.getY();
            float xmax = xmin + playerBounding.getWidth();
            float ymax = ymin + playerBounding.getHeight();
            float width = xmax - xmin;      // width of Player
            float height = ymax - ymin;     // height of Player

            // four bounding points of detectionArea
            float xMin = detectionArea.getX();
            float yMin = detectionArea.getY();
            float xMax = xMin + detectionArea.getWidth();
            float yMax = ymin + detectionArea.getHeight();
            
            // Guard is facing up direction
            if (currentDirection.equalsIgnoreCase("up")) {
                float deltaLeft = Math.abs(xMax - xmin);    // "left" side of Player in the area
                float deltaRight = Math.abs(xmax - xMin);   // "right" side
                float deltaDown = Math.abs(yMax - ymin);    // "down" side
                
                return (deltaLeft >= percent * width) || (deltaRight >= percent * width) ||
                        (deltaDown >= percent * height);
            }
            // Guard is facing down direction (or, "none")
            else if (currentDirection.equalsIgnoreCase("down") ||
                    currentDirection.equalsIgnoreCase("none")) {
                float deltaLeft = Math.abs(xMax - xmin);    // "left" side of Player in the area
                float deltaRight = Math.abs(xmax - xMin);   // "right" side
                float deltaUp = Math.abs(ymax - yMin);      // "up" side
                
                return (deltaLeft >= percent * width) || (deltaRight >= percent * width) ||
                        (deltaUp >= percent * height);
            }
            // Guard is facing left direction
            else if (currentDirection.equalsIgnoreCase("left")) {
                float deltaRight = Math.abs(xmax - xMin);   // "right" side
                float deltaUp = Math.abs(ymax - yMin);      // "up" side
                float deltaDown = Math.abs(yMax - ymin);    // "down" side
                
                return (deltaRight >= percent * width) || (deltaUp >= percent * height) ||
                        (deltaDown >= percent * height);
            }
            // Guard is facing right direction
            else if (currentDirection.equalsIgnoreCase("right")) {
                float deltaLeft = Math.abs(xMax - xmin);    // "left" side
                float deltaUp = Math.abs(ymax - yMin);      // "up" side
                float deltaDown = Math.abs(yMax - ymin);    // "down" side
                
                return (deltaLeft >= percent * width) || (deltaUp >= percent * height) ||
                        (deltaDown >= percent * height);
            }
        }
        
        return false;
    }
    
}

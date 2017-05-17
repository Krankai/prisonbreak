/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prisonbreak.game.entities;

import com.badlogic.gdx.math.Rectangle;

/**
 *
 * @author krankai
 */
public abstract class Guard extends Character {
    
//    private final int guardID;
    
    public Guard(String tileSheetName) {
        super(tileSheetName);
        
        // set ID
//        guardID = id;
    }
    
//    public int getGuardID() {
//        return guardID;
//    }
    
    // return the current detection area, based on the direction the Guard is facing
    public Rectangle getDetectArea() {
        float offset = 0;
        int scale = 4;
        
        if ("up".equals(currentDirection)) {
            return (new Rectangle(x - offset, y + height, width + 2*offset, height * scale + offset));
        } else if ("down".equals(currentDirection)) {
            return (new Rectangle(x, y - height * scale, width, height * scale));
        } else if ("left".equals(currentDirection)) {
            return (new Rectangle(x - width * scale, y, width * scale, height));
        } else if ("right".equals(currentDirection)) {
            return (new Rectangle(x + width, y, width * scale, height));
        } else {
            // default: "none" direction -> return null
            return null;
        }
    }
    
    // perform detection - looking for "prisoner" (Player)
    // return true if Player steps in the detection area > 50%
    //        false otherwise
    public boolean detectPlayer() {
        Rectangle playerBounding = renderer.getPlayer().getSprite().getBoundingRectangle();
        Rectangle detectionArea = getDetectArea();
        
        // NOTE: Use three small rectangles (top, left, right)
        //      -> check whether they belong completely inside the detectiona area
        // four small rectangles represent four "side" of character
        Rectangle upper = new Rectangle(playerBounding.getX() + playerBounding.getWidth()/4,
                playerBounding.getY() + playerBounding.getHeight()/2,
                playerBounding.getWidth() / 2, playerBounding.getHeight() / 2);
        Rectangle lower = new Rectangle(playerBounding.getX() + playerBounding.getWidth()/4,
                playerBounding.getY(),
                playerBounding.getWidth() / 2, playerBounding.getHeight() / 2);
        Rectangle left = new Rectangle(playerBounding.getX(),
                playerBounding.getY() + playerBounding.getHeight()/4,
                playerBounding.getWidth() / 2, playerBounding.getHeight() / 2);
        Rectangle right = new Rectangle(playerBounding.getX() + playerBounding.getWidth()/2,
                playerBounding.getY() + playerBounding.getHeight()/4,
                playerBounding.getWidth() / 2, playerBounding.getHeight() / 2);
        
//        Gdx.app.log("detectionArea: ", detectionArea.toString());
//        Gdx.app.log("upper: ", upper.toString());
//        Gdx.app.log("lower: ", lower.toString());
//        Gdx.app.log("right: ", right.toString());
//        Gdx.app.log("left: ", left.toString());
        
        // check detection
        if (currentDirection.equalsIgnoreCase("up")) {
            return (detectionArea.contains(left) || detectionArea.contains(right) ||
                    detectionArea.contains(lower));
        } else if (currentDirection.equalsIgnoreCase("down")) {
            return (detectionArea.contains(right) || detectionArea.contains(left) ||
                    detectionArea.contains(upper));
        } else if (currentDirection.equalsIgnoreCase("left")) {
            return (detectionArea.contains(upper) || detectionArea.contains(lower) ||
                    detectionArea.contains(right));
        } else if (currentDirection.equalsIgnoreCase("right")) {
            return (detectionArea.contains(upper) || detectionArea.contains(lower) ||
                    detectionArea.contains(left));
        }
        
//        if (detectionArea.contains(playerBounding))
//            return true;
        
//        // if Player steps in detection area
//        if (playerBounding.overlaps(detectionArea)) {
//            Gdx.app.log("Player: ", "inside detection area");
//            Gdx.app.log("Player bounding: ", playerBounding.toString());
//            Gdx.app.log("Detection area: ", detectionArea.toString());
//            
//            float percent = 0.5f;
//            
//            // four bounding points of playerBounding
//            float xmin = playerBounding.getX();
//            float ymin = playerBounding.getY();
//            float xmax = xmin + playerBounding.getWidth();
//            float ymax = ymin + playerBounding.getHeight();
//
//            // four bounding points of detectionArea
//            float xMin = detectionArea.getX();
//            float yMin = detectionArea.getY();
//            float xMax = xMin + detectionArea.getWidth();
//            float yMax = yMin + detectionArea.getHeight();
//            
//            // Guard is facing up direction
//            if (currentDirection.equalsIgnoreCase("up")) {
//                if (renderer.getPlayer().getCurrentDirection().equalsIgnoreCase("left")) {
//                    return (xMax - xmin) >= percent * (xmax - xmin);
//                }
//                else if (renderer.getPlayer().getCurrentDirection().equalsIgnoreCase("right")) {
//                    return (xmax - xMin) >= percent * (xmax - xmin);
//                }
//                else if (renderer.getPlayer().getCurrentDirection().equalsIgnoreCase("down")){
////                    Gdx.app.log("yMax = ", "" + yMax);
////                    Gdx.app.log("ymin = ", "" + ymin);
////                    Gdx.app.log("yMax - ymin = ", "" + (yMax - ymin));
//                    
//                    return (yMax - ymin) >= percent * (ymax - ymin);
//                }
//                else {  // "up" and "none"
//                    
//                }
//            }
//            // Guard is facing down direction (or, "none")
//            else if (currentDirection.equalsIgnoreCase("down") ||
//                    currentDirection.equalsIgnoreCase("none")) {
//                float deltaLeft = Math.abs(xMax - xmin);    // "left" side of Player in the area
//                float deltaRight = Math.abs(xmax - xMin);   // "right" side
//                float deltaUp = Math.abs(ymax - yMin);      // "up" side
//                
//                return (deltaLeft >= percent * width) || (deltaRight >= percent * width) ||
//                        (deltaUp >= percent * height);
//            }
//            // Guard is facing left direction
//            else if (currentDirection.equalsIgnoreCase("left")) {
//                float deltaRight = Math.abs(xmax - xMin);   // "right" side
//                float deltaUp = Math.abs(ymax - yMin);      // "up" side
//                float deltaDown = Math.abs(yMax - ymin);    // "down" side
//                
//                return (deltaRight >= percent * width) || (deltaUp >= percent * height) ||
//                        (deltaDown >= percent * height);
//            }
//            // Guard is facing right direction
//            else if (currentDirection.equalsIgnoreCase("right")) {
//                float deltaLeft = Math.abs(xMax - xmin);    // "left" side
//                float deltaUp = Math.abs(ymax - yMin);      // "up" side
//                float deltaDown = Math.abs(yMax - ymin);    // "down" side
//                
//                return (deltaLeft >= percent * width) || (deltaUp >= percent * height) ||
//                        (deltaDown >= percent * height);
//            }
//        }
        
        return false;
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prisonbreak.game.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 *
 * @author user
 */
public class PatrolGuard extends Guard {
    
    public Array<Vector2> listMarkPoints;      // list of mark points that the Guard
                                                // will patrol through
    private int currentLoopIndex = 0;           // currentLoopIndex = 0 -> moving from
                                                // listMarkPoints[0] to listMarkPoints[1]
    
    // (initx, inity) = the coordinate of the initial position, in pixels
    public PatrolGuard(String tileSheetName, float initx, float inity, Array<Vector2> markPoints) {
        super(tileSheetName);
        
        listMarkPoints = markPoints;
        
        // set initial position
        this.x = initx;
        this.y = inity;
    }
    
    // (initx, inity) = the coordinate of the initial position, in pixels
    public PatrolGuard(String tileSheetName, float initx, float inity) {
        super(tileSheetName);
        
        listMarkPoints = new Array<Vector2>();
        
        // set initial position
        this.x = initx;
        this.y = inity;
        
        // add the initial position into the list
        listMarkPoints.add(new Vector2(this.x, this.y));
    }
    
    // add new mark points; return true if the new point is added successfully
    public boolean addMarkPoint(Vector2 newMarkPoint) {
        // get the latest mark point
        Vector2 latest = listMarkPoints.get(listMarkPoints.size - 1);
        
        // compate this with the newly added mark point:
        //      if same x-coor or y-coor -> accept
        //      otherwise -> reject
        if (latest.x == newMarkPoint.x || latest.y == newMarkPoint.y) {
            listMarkPoints.add(newMarkPoint);
            
            return true;
        } else {
            return false;
        }
    }
    
    // let the guard patrol through all the mark points
    public void patrol() {
        // update direction of the guard
        Vector2 from = listMarkPoints.get(currentLoopIndex);
        Vector2 to = listMarkPoints.get((currentLoopIndex + 1) % listMarkPoints.size);
        
//        Gdx.app.log("from: ", from.toString());
//        Gdx.app.log("to: ", to.toString());
        
        if (from.y == to.y) {
            // move to the right
            if (to.x > from.x) {
                setRightMove(true);
            }
            // move to the left
            else {
                setLeftMove(true);
            }
        } else if (from.x == to.x) {
            // move up
            if (to.y > from.y) {
                setUpMove(true);
            }
            // move down
            else {
                setDownMove(true);
            }
        } else {
            Gdx.app.log("Error: ", "moving in diagonal");
            return;
        }
        
        // update motion
        updateMotion();
        
        // check new position (adjust if neccessary), and set new value of currentLoopIndex
        if (moveLeft) {
            if (this.x < to.x) {
                this.x = to.x;
                currentLoopIndex = (currentLoopIndex + 1) % listMarkPoints.size;
            }
        } else if (moveRight) {
            if (this.x > to.x) {
                this.x = to.x;
                currentLoopIndex = (currentLoopIndex + 1) % listMarkPoints.size;
            }
        } else if (moveUp) {
            if (this.y > to.y) {
                this.y = to.y;
                currentLoopIndex = (currentLoopIndex + 1) % listMarkPoints.size;
            }
        } else if (moveDown) {
            if (this.y < to.y) {
                this.y = to.y;
                currentLoopIndex = (currentLoopIndex + 1) % listMarkPoints.size;
            }
        } else {
            Gdx.app.log("Error: ", "patrol guard is stationary");
            return;
        }   
    }
    
    @Override
    public void update() {
        if (!sleep) {
            patrol();
        }
        
        // update new Sprite
        sprite = new Sprite(currentTexture);
        sprite.setX(x);
        sprite.setY(y);
    }
    
}

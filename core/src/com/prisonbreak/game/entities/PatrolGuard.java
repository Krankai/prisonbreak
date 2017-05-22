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
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

/**
 *
 * @author user
 */
public class PatrolGuard extends Guard {
    
    public Array<Vector2> listMarkPoints;       // list of mark points that the Guard
                                                // will patrol through
    private int currentLoopIndex = 0;           // currentLoopIndex = 0 -> moving from
                                                // listMarkPoints[0] to listMarkPoints[1]
    private float secondDelay = 0;                // seconds to delay at each mark point
    private boolean indexChange = false;        // notify when currentLoopIndex changes
    private boolean firstLoop = true;           // true only when first loop
    private boolean patrolNow = true;           // determine when should call patrol, or wait for .. seconds then call
    private boolean timerHappen = false;        // true if Timer has been initialized; reset when finish waiting
    
    // (initx, inity) = the coordinate of the initial position, in pixels
    public PatrolGuard(String tileSheetName, float initx, float inity, float secondDelay, Array<Vector2> markPoints) {
        super(tileSheetName);
        
        listMarkPoints = markPoints;
        this.secondDelay = secondDelay;
        
        // set initial position
        this.x = initx;
        this.y = inity;
    }
    
    // (initx, inity) = the coordinate of the initial position, in pixels
    public PatrolGuard(String tileSheetName, float initx, float inity, float secondDelay) {
        super(tileSheetName);
        
        listMarkPoints = new Array<Vector2>();
        this.secondDelay = secondDelay;
        
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
        
        if (firstLoop || indexChange) {
            if (firstLoop) firstLoop = false;
            
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
            indexChange = false;
        }
        
        // update motion
        updateMotion();
        
        // check new position (adjust if neccessary), and set new value of currentLoopIndex
        if (moveLeft) {
            if (this.x <= to.x) {
                this.x = to.x;
                currentLoopIndex = (currentLoopIndex + 1) % listMarkPoints.size;
                indexChange = true;
                patrolNow = false;
            }
        } else if (moveRight) {
            if (this.x >= to.x) {
                this.x = to.x;
                currentLoopIndex = (currentLoopIndex + 1) % listMarkPoints.size;
                indexChange = true;
                patrolNow = false;
            }
        } else if (moveUp) {
            if (this.y >= to.y) {
                this.y = to.y;
                currentLoopIndex = (currentLoopIndex + 1) % listMarkPoints.size;
                indexChange = true;
                patrolNow = false;
            }
        } else if (moveDown) {
            if (this.y <= to.y) {
                this.y = to.y;
                currentLoopIndex = (currentLoopIndex + 1) % listMarkPoints.size;
                indexChange = true;
                patrolNow = false;
            }
        } else {
            Gdx.app.log("Error: ", "patrol guard is stationary");
        }   
    }
    
    @Override
    public void update() {
        if (!sleep) {
//            Gdx.app.log("indexChange: ", "" + indexChange);
//            Gdx.app.log("change: ", "" + change);
//            Gdx.app.log("patrolNow: ", "" + patrolNow);
//            if (indexChange && !change) {
//                patrolNow = false;
//            }
            if (patrolNow) {
                patrol();
                timerHappen = false;
            } else {
                if (!timerHappen) {
                    Timer.schedule(new Task() {
                        @Override
                        public void run() {
                            patrolNow = true;
    //                        change = true;
                            Gdx.app.log("Timer patrolNow: ", "" + patrolNow);
                        }
                    }, secondDelay);
                    timerHappen = true;
                }
            }
        }
        
        // update new Sprite
        sprite = new Sprite(currentTexture);
        sprite.setX(x);
        sprite.setY(y);
    }
    
    @Override
    public void dispose() {
        super.dispose();
    }
    
}

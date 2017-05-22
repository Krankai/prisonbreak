/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prisonbreak.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.ParallelAction;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.prisonbreak.game.MapControlRenderer;
import com.prisonbreak.game.PrisonBreakGame;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author user
 */
public class GameScreen implements Screen {
    
    private final Game game;
    private final TiledMap map;
    private final MapControlRenderer mapRenderer;
    private final Stage stage;
    private final Label label;
    private final Group labelContainer;
    private final ScheduledExecutorService executorService; 
    
    public GameScreen(Game aGame) {
        game = aGame;
        
        // import tiledMap
        map = new TmxMapLoader().load("tiledmap/map.tmx");
        mapRenderer = new MapControlRenderer(map);      // create map-control renderer
        mapRenderer.setView(mapRenderer.getCamera());   // set camera using with the renderer
        
        // create new stage
        stage = new Stage(new ScreenViewport());
        
        // create label for displaying message, initialize to winning message
        label = new Label("CONGRATULATIONS\nYOU HAVE ESCAPED THE PRISON\nPress ESC to return to the Main Menu",
                PrisonBreakGame.gameSkin, "title-plain");
        label.setColor(Color.RED);
        label.setPosition(0, 0);
        label.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        label.setAlignment(Align.center);
       
        // create a Group container that contains the label and perform some actions
        labelContainer = new Group();
        labelContainer.addActor(label);
        labelContainer.setOrigin(label.getWidth() / 2, label.getHeight() / 2);
        stage.addActor(labelContainer);
        
        // initalize actions for winning state
        labelContainer.setScale(0);
        
        ParallelAction parallelAction = new ParallelAction();
        parallelAction.addAction(Actions.rotateBy(360, 2, Interpolation.smooth));
        parallelAction.addAction(Actions.scaleBy((float) 2, (float) 2, 3, Interpolation.smooth));
        parallelAction.addAction(Actions.fadeIn(1));
        
        labelContainer.addAction(parallelAction);   // add actions to container
        
        executorService = Executors.newSingleThreadScheduledExecutor();
    }
    
    @Override
    public void show() {
//        Gdx.app.log("MainScreen", "show");
        Gdx.input.setInputProcessor(mapRenderer);
    }
    
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        mapRenderer.render();
        
        // check winning state
        if (mapRenderer.getCurrentState() == MapControlRenderer.STATE.WIN) {
//            Gdx.app.log("Player ", "wins");
//            Gdx.app.log("Curernt state: ", mapRenderer.getCurrentState().toString());
//            game.setScreen(new WinningScreen(game));
            stage.act();
            stage.draw();
            
            // wait for 4 seconds, then set state of the game to END
            executorService.schedule(new Runnable() {
                @Override
                public void run() {
                    mapRenderer.setCurrentState(MapControlRenderer.STATE.END);
                }
            }, 4, TimeUnit.SECONDS);
            
        }
        // check losing state
        else if (mapRenderer.getCurrentState() == MapControlRenderer.STATE.LOSE) {
            // set the appropriate message to display
            label.setText("GAME OVER. YOU LOSE\nPress ESC to return to the Main Menu");
            
            // display
            stage.act();
            stage.draw();
            
            // wait for 4 seconds, then set state of the game to END
            executorService.schedule(new Runnable() {
                @Override
                public void run() {
                    mapRenderer.setCurrentState(MapControlRenderer.STATE.END);
                }
            }, 4, TimeUnit.SECONDS);
        }
        
        // if game is paused (due to winning/losing the game) -> still, display message
        if (mapRenderer.getCurrentState() == MapControlRenderer.STATE.END) {
            if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
                dispose();
                game.setScreen(new IntroScreen(game));
            }
            stage.act();
            stage.draw();
        }
    }
    
    @Override
    public void resize(int width, int height) {
        
    }
    
    @Override
    public void pause() {
        
    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        stage.dispose();
        map.dispose();
        mapRenderer.dispose();
    }
    
}

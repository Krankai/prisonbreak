/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prisonbreak.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.prisonbreak.game.PrisonBreakGame;

/**
 *
 * @author user
 */
public class IntroScreen implements Screen {
    private Stage stage;
    private Game game;
    
    public IntroScreen(Game aGame) {
        game = aGame;
        stage = new Stage(new ScreenViewport());
        
        // Add background image
        Image backgroundImage = new Image(new Texture(Gdx.files.internal("background.jpg")));
        backgroundImage.setSize(PrisonBreakGame.BACKGROUND_WIDTH, PrisonBreakGame.BACKGROUND_HEIGHT);
        backgroundImage.setPosition(0, 0);
        stage.addActor(backgroundImage);
        
        // Create title panel
        Image titleImage = new Image(new Texture(Gdx.files.internal("title_prison_break.jpg")));
        titleImage.setSize(401, 175);
        titleImage.setPosition(Gdx.graphics.getWidth()/2 - titleImage.getWidth()/2, Gdx.graphics.getHeight()*2/3 - titleImage.getHeight()/2);
        stage.addActor(titleImage);
        
        // Create "Play" button
        TextButton playButton = new TextButton("Play", PrisonBreakGame.gameSkin, "round");
        playButton.setWidth(Gdx.graphics.getWidth() / 4);
        playButton.setPosition(Gdx.graphics.getWidth()/2 - playButton.getWidth()/2, Gdx.graphics.getHeight()*2/5 - playButton.getHeight()/2);
        playButton.addListener(new InputListener() {
            
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                game.setScreen(new GameScreen(game));
            }
            
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }
        });
        stage.addActor(playButton);
        
        // Create "Exit" button
        TextButton exitButton = new TextButton("Exit", PrisonBreakGame.gameSkin, "round");
        exitButton.setWidth(Gdx.graphics.getWidth() / 4);
        exitButton.setPosition(Gdx.graphics.getWidth()/2 - exitButton.getWidth()/2, Gdx.graphics.getHeight()*3/10 - exitButton.getHeight()/2);
        exitButton.addListener(new InputListener() {
           
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                System.exit(0);
            }
            
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }
        });
        stage.addActor(exitButton);
    }
    
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }
    
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act();
        stage.draw();
    }
    
    @Override
    public void resize(int width, int height) {
        
    }
    
    @Override
    public void pause() {
        
    }
    
    @Override
    public void hide() {
        
    }
    
    @Override
    public void resume() {
        
    }
    
    @Override
    public void dispose() {
        stage.dispose();
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prisonbreak.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.prisonbreak.game.MapControlRenderer;
import com.prisonbreak.game.PrisonBreakGame;

/**
 *
 * @author user
 */
public class IntroScreen implements Screen {
    
    private Stage stage;
    private Game game;
    private final Label instruct;
    
    public IntroScreen(Game aGame) {
        game = aGame;
        stage = new Stage(new ScreenViewport());
        
        // add background image
        Image backgroundImage = new Image(new Texture(Gdx.files.internal("background.jpg")));
        backgroundImage.setSize(PrisonBreakGame.BACKGROUND_WIDTH, PrisonBreakGame.BACKGROUND_HEIGHT);
        backgroundImage.setPosition(0, 0);
        stage.addActor(backgroundImage);
        
        // add title
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        BitmapFont myFont = new BitmapFont(Gdx.files.internal("font/Octin-Prison-80.fnt"));
        labelStyle.font = myFont;
        labelStyle.fontColor = Color.WHITE;
        
        Label title = new Label("PRISON BREAK", labelStyle);
        title.setPosition(Gdx.graphics.getWidth()/2 - title.getWidth()/2, Gdx.graphics.getHeight()*2/3 - title.getHeight()/2);
        title.setAlignment(Align.center);
        stage.addActor(title);
        
        // create label for displaying instructions to play the game
        String instructions = "Use arrow keys or W S D A to move the character.\n"
                + "Press F to interact with objects.\nPress I to show/hide player's inventory.\n\n"
                + "Go around and find the necessary items and information to get out.\n"
                + "Remember, avoid the security guards at all cost. Good luck, prisoner!\n\n"
                + "Press ENTER to play the game.";
        instruct = new Label(instructions, PrisonBreakGame.gameSkin, "custom");
        instruct.setPosition(Gdx.graphics.getWidth() / 6, Gdx.graphics.getHeight() / 6);
        instruct.setSize(Gdx.graphics.getWidth() * 2/3, Gdx.graphics.getHeight() * 2/3);
        instruct.setAlignment(Align.left);
        instruct.setWrap(true);
        instruct.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    instruct.remove();
                    dispose();
                    game.setScreen(new GameScreen(game));
                    return true;
                } else {
                    return false;
                }
            }
        });
        
        // create "Play" button
        TextButton playButton = new TextButton("Play", PrisonBreakGame.gameSkin, "round");
        playButton.setWidth(Gdx.graphics.getWidth() / 4);
        playButton.setPosition(Gdx.graphics.getWidth()/2 - playButton.getWidth()/2, Gdx.graphics.getHeight()/2 - playButton.getHeight()/2);
        playButton.addListener(new InputListener() {
            
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
//                dispose();
//                game.setScreen(new GameScreen(game));
                stage.addActor(instruct);
                stage.setKeyboardFocus(instruct);
            }
            
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }
            
        });
        stage.addActor(playButton);
        
        // create "Exit" button
        TextButton exitButton = new TextButton("Exit", PrisonBreakGame.gameSkin, "round");
        exitButton.setWidth(Gdx.graphics.getWidth() / 4);
        exitButton.setPosition(Gdx.graphics.getWidth()/2 - exitButton.getWidth()/2, Gdx.graphics.getHeight()*2/5 - exitButton.getHeight()/2);
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

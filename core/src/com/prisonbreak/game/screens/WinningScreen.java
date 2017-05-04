/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prisonbreak.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.ParallelAction;
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.prisonbreak.game.PrisonBreakGame;

/**
 *
 * @author krankai
 */
public class WinningScreen implements Screen {
    
    private Stage stage;
    private Game game;
    private final Skin skin;
    private Label titleLabel;
    
    public WinningScreen(Game aGame) {
        game = aGame;
        stage = new Stage(new ScreenViewport());
        
        // import skin
        skin = new Skin(Gdx.files.internal("skin/uiskin.json"));
        
        // add background image
        Image backgroundImage = new Image(new Texture(Gdx.files.internal("background.jpg")));
        backgroundImage.setSize(PrisonBreakGame.BACKGROUND_WIDTH, PrisonBreakGame.BACKGROUND_HEIGHT);
        backgroundImage.setPosition(0, 0);
//        stage.addActor(backgroundImage);
        
        // add title (~ congratulation message)
        titleLabel = new Label("CONGRATULATIONS", skin, "title-plain");
        titleLabel.setPosition(0, 0);
        titleLabel.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        titleLabel.setAlignment(Align.center);
        titleLabel.setColor(Color.RED);
        
        Group titleLabelContainer = new Group();
        titleLabelContainer.addActor(titleLabel);
        titleLabelContainer.setOrigin(titleLabel.getWidth()/2, titleLabel.getHeight()/2);
        stage.addActor(titleLabelContainer);
        
        titleLabelContainer.setScale(0);          // hide the message at first
        
        // rotate + zoom in the message when displaying it
        ParallelAction parallelAction = new ParallelAction();
        parallelAction.addAction(Actions.rotateBy(360, 3));
        parallelAction.addAction(Actions.scaleTo((float) 2, (float) 2, 3, Interpolation.smooth));
        parallelAction.addAction(Actions.fadeIn(1));
        
        titleLabelContainer.addAction(parallelAction);
        
        // create "Main Menu" button, and add to stage
        TextButton menuButton = new TextButton("Main Menu", skin, "round");
        menuButton.setWidth(Gdx.graphics.getWidth() / 4);
        menuButton.setPosition(Gdx.graphics.getWidth()/2 - menuButton.getWidth()/2, Gdx.graphics.getHeight()/2 - menuButton.getHeight()/2);
        menuButton.addListener(new InputListener() {
           
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                game.setScreen(new IntroScreen(game));
            }
            
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }
            
        });
//        stage.addActor(menuButton);
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
    public void resume() {
        
    }

    @Override
    public void hide() {
        
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
    
}

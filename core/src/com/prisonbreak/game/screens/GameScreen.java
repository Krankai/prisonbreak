/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prisonbreak.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.prisonbreak.game.entities.Player;

/**
 *
 * @author user
 */
public class GameScreen implements Screen, InputProcessor {
    
    private Game game;
    private SpriteBatch batch;
    private Player player;
    private OrthographicCamera camera;
    
    public GameScreen(Game aGame) {
        game = aGame;
        batch = new SpriteBatch();
        player = new Player();
        camera = new OrthographicCamera();
        camera.setToOrtho(false);
    }
    
    @Override
    public void show() {
        Gdx.app.log("MainScreen", "show");
        Gdx.input.setInputProcessor(this);
    }
    
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        camera.update();
        player.update();
        
        batch.begin();
        batch.draw(player.getImage(), player.x, player.y);
        batch.end();
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
//        player.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Keys.LEFT:
                player.setLeftMove(true);
                break;
            case Keys.RIGHT:
                player.setRightMove(true);
                break;
            case Keys.UP:
                player.setUpMove(true);
                break;
            case Keys.DOWN:
                player.setDownMove(true);
                break;
            case Keys.SPACE:
                player.sleepWakeup();
                break;
        }
        
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        switch (keycode) {
            case Keys.LEFT:
                player.setLeftMove(false);
                break;
            case Keys.RIGHT:
                player.setRightMove(false);
                break;
            case Keys.UP:
                player.setUpMove(false);
                break;
            case Keys.DOWN:
                player.setDownMove(false);
                break;
        }
        
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
}

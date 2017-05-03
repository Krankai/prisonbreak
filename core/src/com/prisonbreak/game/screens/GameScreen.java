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
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.prisonbreak.game.MapControlRenderer;

/**
 *
 * @author user
 */
public class GameScreen implements Screen {
    
    private final Game game;
    private final TiledMap map;
    private final MapControlRenderer mapRenderer;
    
    public GameScreen(Game aGame) {
        game = aGame;
        
        // import tiledMap
        map = new TmxMapLoader().load("tiledmap/map.tmx");
        mapRenderer = new MapControlRenderer(map);    // create map-control renderer
        
        mapRenderer.setView(mapRenderer.getCamera());
    }
    
    @Override
    public void show() {
//        Gdx.app.log("MainScreen", "show");
        Gdx.input.setInputProcessor(mapRenderer);
    }
    
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        mapRenderer.render();
        
        // check winning condition
        if (mapRenderer.getCurrentState() == MapControlRenderer.STATE.WIN) {
//            Gdx.app.log("Player ", "wins");
            game.setScreen(new IntroScreen(game));
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
        
    }
    
}

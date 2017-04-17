package com.prisonbreak.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.prisonbreak.game.screens.IntroScreen;

public class PrisonBreakGame extends Game {
	public static Skin gameSkin;
        public final static float BACKGROUND_WIDTH = 1920 / 2;
        public final static float BACKGROUND_HEIGHT = (float) (1243 / 2.0);
	
	@Override
	public void create() {
            gameSkin = new Skin(Gdx.files.internal("skin/uiskin.json"));
            this.setScreen(new IntroScreen(this));
	}

	@Override
	public void render() {
            super.render();
	}
	
	@Override
	public void dispose() {
            gameSkin.dispose();
	}
}

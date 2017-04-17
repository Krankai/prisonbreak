package com.prisonbreak.game.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.prisonbreak.game.PrisonBreakGame;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
                config.title = "Prison Break 2D Game";
                config.width = (int) PrisonBreakGame.BACKGROUND_WIDTH;
                config.height = (int) PrisonBreakGame.BACKGROUND_HEIGHT;
                config.resizable = false;
                config.x = 200;
                config.y = 30;
		new LwjglApplication(new PrisonBreakGame(), config);
	}
}

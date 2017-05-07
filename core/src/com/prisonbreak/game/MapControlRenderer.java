/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prisonbreak.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.prisonbreak.game.entities.Item;
import com.prisonbreak.game.entities.Player;

/**
 *
 * @author krankai
 */
public class MapControlRenderer extends OrthogonalTiledMapRenderer implements InputProcessor {
        
    public static final int WORLD_WIDTH = 100 * 32;
    public static final int WORLD_HEIGHT = 100 * 32;
    
    private final Player player;
    private final OrthographicCamera camera;
    private final int drawSpritesAfterLayer = this.map.getLayers().getIndex("Walls") + 1;
    private Array<Array<Item>> listItems;       // array holds objectItems of objectItems items of all objects
    private Array<Array<String>> messageTree;   // array holds the message tree (for all objects)
    private Array<Item> objectItems;            // current objectItems of items for one object
    
    private int indexForMessageTree = 0;
    private boolean interactHappen = false;
    
    public enum STATE {
        ONGOING, WIN, LOSE, PAUSE, END
    }
    private STATE state;      
    
    private int[][] blockedWallUnitGrid;
    private final MapObjects interactionObjects;
    private final MapObjects staticObjects;
    private final MapObject winningLocation;
    
    public Stage stage;
    public Label dialogBoxLabel;
    public List itemList;
    public final Skin skin;
    
    public MapControlRenderer(TiledMap map) {
        super(map);
        state = STATE.ONGOING;
        
        // ui components for "interaction conversation"
        stage = new Stage();
        skin = new Skin(Gdx.files.internal("skin/uiskin.json"));
        dialogBoxLabel = null;
        itemList = null;
        
        // create new player
        player = new Player();
        player.setMapControlRenderer(this);         // add map to player
        setPlayerToSpawm();                         // add Player at spawming point of the map
        
        // extract a grid identifying position of walls -> for detecting collision
        extractBlockedWalls();      // stored in blockedWallUnitGrid
        
        // get objectItems of static (blocked) objects
        staticObjects = map.getLayers().get("ObjectCollision").getObjects();
        
        // get objectItems of interaction objects
        interactionObjects = map.getLayers().get("ObjectInteraction").getObjects();
        
        // extract winning location
        winningLocation = map.getLayers().get("SpecialLocations").getObjects().get("winningPoint");
        
        // initialize objectItems of items, and message tree
        initializeListItems();
        initializeMessageTree();
        
        // create camera
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.translate(player.x, player.y);
    }
    
    public STATE getCurrentState() {
        return state;
    }
    
    public void setCurrentState(STATE state) {
        this.state = state;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public OrthographicCamera getCamera() {
        return camera;
    }
    
    public int[][] getBlockedWallsGrid() {
        return blockedWallUnitGrid;
    }
    
    public MapObjects getStaticObjects() {
        return staticObjects;
    }
    
    public MapObject getWinningLocation() {
        return winningLocation;
    }
    
    // allow camera to move along with player
    public void moveCamera() {
        camera.position.x = MathUtils.clamp(player.x,
                camera.viewportWidth / 2f,
                WORLD_WIDTH - camera.viewportWidth / 2f);
        camera.position.y = MathUtils.clamp(player.y,
                camera.viewportHeight / 2,
                WORLD_HEIGHT - camera.viewportHeight / 2);
        
        camera.update();
        this.setView(camera);
    }
    
    // initialize objectItems of items in the map
    private void initializeListItems() {
        Item key, trash;
        
        // the overall objectItems
        listItems = new Array<Array<Item>>();
        
        // add a key into the garbageBin1
        key = new Item("Key", 1);
        trash = new Item("Trash", 2);
        Array<Item> garbageBin1 = new Array<Item>();
        garbageBin1.add(key);
        garbageBin1.add(trash);
        listItems.add(garbageBin1);
    }
    
    // initialize message tree
    private void initializeMessageTree() {
        messageTree = new Array<Array<String>>();
        
        // message for the first case - index = 0 -> Found nothing
        Array<String> messages = new Array<String>();
        messages.add("There seems to be nothing here ...");
        messageTree.add(messages);
        
        // message for the second case - index = 1 -> There are (many) items here
        messages = new Array<String>();
        messages.add("There are many items here. Which should I choose?");
        messages.add("Obtained 1 x ");
        messageTree.add(messages);
    }
    
    // trigger the interaction with the next-to object (if possible)
    // obtain the objectItems of items contained in that object, or null if none
    // Note: flip the interactHappen flag if Player is in appropriate position
    private Array<Item> triggerInteraction() {
        for (MapObject object : interactionObjects) {
            RectangleMapObject rectObject = (RectangleMapObject) object;
//            Gdx.app.log("object: ", rectObject.getRectangle().toString());
//            Gdx.app.log("player: ", player.getSprite().getBoundingRectangle().toString());
            
            // search and find for the object in interaction with Player
            if (rectObject.getRectangle().overlaps(player.getSprite().getBoundingRectangle())) {
                String directionCheck =
                        rectObject.getProperties().get("contactDirection", "none", String.class);
//                Gdx.app.log("Contact direction: ", directionCheck);
//                Gdx.app.log("Player direction: ", player.getCurrentDirection());
                
                // check if user faces object in correct direction
                if (!player.getCurrentDirection().equalsIgnoreCase("none") &&
                        player.getCurrentDirection().equalsIgnoreCase(directionCheck)) {
                    interactHappen = true;

                    boolean haveItems = rectObject.getProperties().get("haveItems", false, Boolean.class);
                    int listID = rectObject.getProperties().get("listItemID", -1, Integer.class);

                    // if object does contain items
                    if (haveItems) {
                        // get the item objectItems ID
                        if (listID == -1)
                            break;
                        else 
                            return listItems.get(listID);
                    }
                }
            }
        }
        
        return null;
    }
    
    private void extractBlockedWalls() {
        // get map objects of ObjectLayer "Collision"
        MapLayer layer = map.getLayers().get("WallCollision");
        MapObjects objects = layer.getObjects();
        
        // fill in blockedWallUnitGrid with position of static blocked map objects
        // 1 -> blocked     0 -> not blocked
        blockedWallUnitGrid = new int[100][100];
        boolean isBlocked = false;
        for (int indexX = 0; indexX < 100; ++indexX) {
            for (int indexY = 0; indexY < 100; ++indexY) {
                // convert to position in world map
                Vector2 worldPosition = new Vector2(indexX * 32f + 16f, indexY * 32f + 16f);  // consider center
                
                // check whether that position is "blocked" or not
                for (MapObject o : objects) {
                    // retrieve RectangleMapObject objects that are blocked
                    if (o.isVisible() && o instanceof RectangleMapObject &&
                            o.getProperties().get("blocked") != null &&
                            o.getProperties().get("blocked", Boolean.class)) {
                        RectangleMapObject rectObject = (RectangleMapObject) o;
                    
                        if (rectObject.getRectangle().contains(worldPosition)) {
                            isBlocked = true;
                            break;
                        }
                    }
                }
                
                // fill 1 / 0 into the corrsponding tile index
                if (isBlocked) {
                    blockedWallUnitGrid[indexX][indexY] = 1;
                } else {
                    blockedWallUnitGrid[indexX][indexY] = 0;
                }
                
                // reset flag
                isBlocked = false;
            }
        }
        
//        Gdx.app.log("Test: ", " " + blockedWallUnitGrid[13][38] + " " + blockedWallUnitGrid[13][39] +
//                " " + blockedWallUnitGrid[12][38] + " " + blockedWallUnitGrid[12][39]);
    }
    
    // set Player position to spawming location
    private void setPlayerToSpawm() {
        MapObject spawmPoint = map.getLayers().get("SpecialLocations").getObjects().get("spawmPoint");
        RectangleMapObject rectSpawmPoint = (RectangleMapObject) spawmPoint;
        
        player.x = rectSpawmPoint.getRectangle().getX();
        player.y = rectSpawmPoint.getRectangle().getY();
//        player.x = 71 * 32;
//        player.y = 82 * 32;
    }
    
    // checking winning condition
    private boolean winGame() {
        RectangleMapObject rectLoc = (RectangleMapObject) winningLocation;
        
        if (state != STATE.END && 
                rectLoc.getRectangle().contains(player.getSprite().getBoundingRectangle()))
            return true;
        return false;
    }
    
    // reset the InputProcessor to this (MapControlRenderer) (from stage)
    private void resetInputProcessor() {
        Gdx.input.setInputProcessor(this);
    }
    
    @Override
    public void render() {
        if (state != STATE.END && state != STATE.PAUSE) {
            player.update();
            moveCamera();
            
            // update winning state
            if (winGame()) state = STATE.WIN;
        }
        
        beginRender();
        
        int currentLayer = 0;
        for (MapLayer layer : map.getLayers()) {
            if (layer.isVisible()) {
                // tile layer
                if (layer instanceof TiledMapTileLayer) {
                    renderTileLayer((TiledMapTileLayer) layer);
                    ++currentLayer;
                    
                    // layer to draw characters
                    if (currentLayer == drawSpritesAfterLayer) {
                        // draw player
                        player.getSprite().draw(this.getBatch());
                    }
                } 
                // object layer
                else {
                    for (MapObject object : layer.getObjects()) {
                        renderObject(object);
                    }
                }
            }
        }
        
        endRender();
        
        stage.act();
        stage.draw();
    }
    
    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.LEFT:
            case Input.Keys.A:
                player.setLeftMove(true);
                break;
            case Input.Keys.RIGHT:
            case Input.Keys.D:
                player.setRightMove(true);
                break;
            case Input.Keys.UP:
            case Input.Keys.W:
                player.setUpMove(true);
                break;
            case Input.Keys.DOWN:
            case Input.Keys.S:
                player.setDownMove(true);
                break;
            case Input.Keys.SPACE:
                player.sleepWakeup();
                break;
        }
        
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {        
        switch (keycode) {
            case Input.Keys.LEFT:
            case Input.Keys.A:
                player.setLeftMove(false);
                break;
            case Input.Keys.RIGHT:
            case Input.Keys.D:
                player.setRightMove(false);
                break;
            case Input.Keys.UP:
            case Input.Keys.W:
                player.setUpMove(false);
                break;
            case Input.Keys.DOWN:
            case Input.Keys.S:
                player.setDownMove(false);
                break;
            case Input.Keys.ESCAPE:
                break;
            case Input.Keys.F:
//                Gdx.app.log("Interaction flag: ", "" + interactHappen);
                // if first time
                if (indexForMessageTree == 0) {
                    objectItems = triggerInteraction();    // get objectItems of items (if any) at the current object
                }
                
                // if no interactions with any objects -> break
                // otherwise; pause the game
                if (!interactHappen) {
                    break;
                } else {
                    state = STATE.PAUSE;            // temporarily "pause" the game -> Player can't move
                }
                
                // if no items
                if (objectItems == null) {
                    if (indexForMessageTree == messageTree.get(0).size) {
                        state = STATE.ONGOING;      // unpause the game
                        interactHappen = false;     // flip the flag again
                        indexForMessageTree = 0;
                        dialogBoxLabel.remove();
                        break;
                    }
                    
                    // create conversation box
                    // for the first message 
                    if (indexForMessageTree == 0) {
                        dialogBoxLabel = new Label(messageTree.get(0).get(0), skin, "custom");
                        dialogBoxLabel.setPosition(0, 0);
                        dialogBoxLabel.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()/5);
                        dialogBoxLabel.setAlignment(Align.topLeft);
//                        dialogBoxLabel.setFontScale(2f);
                        stage.addActor(dialogBoxLabel);
                    }
                    
                    // for every next message
                    else {
                        dialogBoxLabel.setText(messageTree.get(0).get(indexForMessageTree));
                    }
                    
                    ++indexForMessageTree;  // increment index
                    
                } 
                
                // if it does contain items
                else {
//                    Gdx.app.log("class: ", "" + objectItems.getClass());
                    
                    if (indexForMessageTree == messageTree.get(1).size) {
                        state = STATE.ONGOING;      // unpause the game
                        interactHappen = false;     // flip the flag again
                        indexForMessageTree = 0;
                        dialogBoxLabel.remove();
                        break;
                    }
                    
                    // in case Player choose "Nothing" -> does not pick anything
                    if (indexForMessageTree > messageTree.get(1).size) {
                        indexForMessageTree = messageTree.get(1).size;
                        break;
                    }
                    
                    // for the first message 
                    if (indexForMessageTree == 0) {
                        // create a String array of items' names
                        Array<String> itemNames = new Array<String>();
                        for (Item item : objectItems) {
                            itemNames.add(item.getItemName());
                        }
                        itemNames.add("Nothing");   // add additional value - for not choosing any items
                        
                        // create conversation box
                        dialogBoxLabel = new Label(messageTree.get(1).get(0), skin, "custom");
                        dialogBoxLabel.setPosition(0, 0);
                        dialogBoxLabel.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()/5);
                        dialogBoxLabel.setAlignment(Align.topLeft);
//                        dialogBoxLabel.setFontScale(2f);
                        stage.addActor(dialogBoxLabel);
                        
                        // create objectItems of items
                        itemList = new List(skin, "dimmed");
                        itemList.setItems(itemNames);
                        itemList.setSelectedIndex(0);
                        itemList.setSize(Gdx.graphics.getWidth()/4, Gdx.graphics.getHeight()/6);
                        itemList.setPosition(Gdx.graphics.getWidth() - itemList.getWidth(), dialogBoxLabel.getHeight());
                        itemList.addListener(new InputListener() {
                            @Override
                            public boolean keyDown(InputEvent event, int keycode) {
                                int currentIndex = itemList.getSelectedIndex();
                                int upIndex = currentIndex - 1;
                                int downIndex = currentIndex + 1;
                                
                                if (upIndex < 0) upIndex = itemList.getItems().size - 1;
                                if (downIndex >= itemList.getItems().size) downIndex = 0;
                                
//                                Gdx.app.log("currentIndex: ", "" + currentIndex);
//                                Gdx.app.log("upIndex: ", "" + upIndex);
//                                Gdx.app.log("downIndex: ", "" + downIndex);
                                
                                switch (keycode) {
                                    case Keys.UP:
                                        // scroll up the item objectItems
                                        itemList.setSelectedIndex(upIndex);
                                        break;
                                    case Keys.DOWN:
                                        // scroll down the item objectItems
                                        itemList.setSelectedIndex(downIndex);
                                        break;
                                    case Keys.F:
                                        int index = itemList.getSelectedIndex();
                                        
                                        // if Player chooses "Nothing"
                                        if ("Nothing".equals((String) itemList.getSelected())) {
                                            // remove list of items from stage
                                            itemList.remove();
                                            
                                            // confirm the selection
                                            dialogBoxLabel.setText("You choose nothing.");
                                            
                                            // end the "conversation"
                                            indexForMessageTree =
                                                    messageTree.get(1).size + 1;
                                            resetInputProcessor();
                                            break;
                                        }
                                        
                                        // otherwise;
                                        // add the chosen item into Player's objectItems of items
                                        player.addItem(objectItems.get(index));
                                        
                                        // remove that item from the overall listItems
                                        objectItems.removeIndex(index);
                                        
                                        // remove list of items from stage
                                        itemList.remove();
                                        
                                        // reconfirm message
                                        dialogBoxLabel.setText(messageTree.get(1).get(indexForMessageTree));
                                        resetInputProcessor();
                                        break;
                                    default:
                                        break;
                                }
                                
                                return true;
                            }
                        });
                        stage.addActor(itemList);
                        stage.setKeyboardFocus(itemList);
                        
                        // set InputProcessor to stage
                        Gdx.input.setInputProcessor(stage);
                    }
                    
                    // for every next message
                    else {
                        dialogBoxLabel.setText(messageTree.get(1).get(indexForMessageTree)
                            + player.getInventory().get(player.getInventory().size - 1).getItemName() + ".");
                    }
                    
                    ++indexForMessageTree;  // increment index
                    
                }
                
                break;
            default:
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
    
    @Override
    public void dispose() {
        player.dispose();
        map.dispose();
    }
    
}

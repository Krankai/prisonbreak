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
    public enum STATE {
        ONGOING, WIN, LOSE, PAUSE, END
    }
    private STATE state;
    public enum TYPE_INTERACTION {
        NO_INTERACTION, SEARCH_INTERACTION, OPEN_LOCK_INTERACTION
    }
    private TYPE_INTERACTION typeInteraction = TYPE_INTERACTION.NO_INTERACTION;
    
    private final Player player;
    private final OrthographicCamera camera;
    private final int drawSpritesAfterLayer = this.map.getLayers().getIndex("Walls") + 1;
    private Array<Array<Item>> listItems;       // array holds the overall list of list items of all objects
    private Array<Array<String>> messageTree;   // array holds the message tree (for all objects)
    private Array<Item> objectItems;            // current list of items for one object
    private Item neededKey;                     // the required key to unlock an object (door)
    
    private int indexForMessageTree = 0;
    private String currentDoorName = "";        // name of the current door in interaction
    private boolean interactHappen = false;     // flag -> indicate whether interaction happens
    private boolean inventoryOpen = false;      // flag -> indicate whether the inventory is currenly opened
    private boolean currentDoorLocked = false;  // flag -> indicate whether the current door in interaction
                                                // with Player is locked or not
    
    private int[][] blockedWallUnitGrid;
    private final MapObjects interactionObjects;
    private final MapObjects staticObjects;
    private final MapObjects doorObjects;
    private final MapObject winningLocation;
    
    public Stage stage;
    public Label dialogBoxLabel;
    public Label descLabel;             // description label, for inventory
    public Label titleLabel;
    public List itemList;
    public List inventory;
    public final Skin skin;
    
    public MapControlRenderer(TiledMap map) {
        super(map);
        state = STATE.ONGOING;
        
        // ui components for "interaction conversation"
        stage = new Stage();
        skin = new Skin(Gdx.files.internal("skin/uiskin.json"));
        
        // create new player
        player = new Player();
        player.setMapControlRenderer(this);         // add map to player
        setPlayerToSpawm();                         // add Player at spawming point of the map
        
        // extract a grid identifying position of walls -> for detecting collision
        extractBlockedWalls();      // stored in blockedWallUnitGrid
        
        // get list of static (blocked) objects
        staticObjects = map.getLayers().get("ObjectCollision").getObjects();
        
        // get list of door objects
        doorObjects = map.getLayers().get("DoorCollision").getObjects();
        
        // get list of interaction objects
        interactionObjects = map.getLayers().get("ObjectInteraction").getObjects();
        
        // extract winning location
        winningLocation = map.getLayers().get("SpecialLocations").getObjects().get("winningPoint");
        
        // initialize list of items, and message tree
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
    
    public MapObjects getDoorObjects() {
        return doorObjects;
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
        Item item;
        Array<Item> list;
        
        // the overall objectItems
        listItems = new Array<Array<Item>>();
        
        // add an old key + trash into the garbageBin1 ; listItemID = 0
        list = new Array<Item>();
        item = new Item("Old Key", 1);
        item.setDescription("An old key. Maybe it can be used to open a cell ...");
        list.add(item);
        item = new Item("Trash", 2);
        item.setDescription("Filthy trash.");
        list.add(item);
        listItems.add(list);
        
        // add a key into wardrobe1 ; listItemID = 1
        list = new Array<Item>();
        item = new Item("Uniform", 3);
        item.setDescription("A guard uniform.");
        list.add(item);
        listItems.add(list);
        
        // add a modern key below the pillow ; listItemID = 2
        list = new Array<Item>();
        item = new Item("Modern Key", 4);
        item.setDescription("A large key. It seems that this key is used to open a big door.");
        list.add(item);
        listItems.add(list);
        
        // add an ice cream + frozen meat + a bottle into the fride ; listItemID = 3
        list = new Array<Item>();
        item = new Item("Ice cream", 5);
        item.setDescription("A chocolate ice cream. Very yummy.");
        list.add(item);
        item = new Item("Frozen meat", 6);
        item.setDescription("A frozen meat. Need to be thawed if you want to cook it.");
        list.add(item);
        item = new Item("Bottle of drink", 7);
        item.setDescription("A bottle full of cold water. Best for hot days.");
        list.add(item);
        listItems.add(list);
    }
    
    // initialize message tree
    private void initializeMessageTree() {
        messageTree = new Array<Array<String>>();
        
        // for search_interaction
        // message for the first case - index = 0 -> Found nothing
        Array<String> messages = new Array<String>();
        messages.add("There seems to be nothing here ...");
        messageTree.add(messages);
        
        // message for the second case - index = 1 -> There are (many) items here
        messages = new Array<String>();
        messages.add("You found something. Which item do you pick?");
        messages.add("Obtained 1 x ");
        messageTree.add(messages);
        
        // for open_lock_interaction
        // message for the third case - index = 2 -> The door is locked
        messages = new Array<String>();
        messages.add("The door is locked.");
        messages.add("You can unlock it using");
        messages.add("Will you unlock the door?");
        messages.add("The door is unlocked.");
        messageTree.add(messages);
        
        // message for the fourth case - index = 3 -> The door is unlocked
        messages = new Array<String>();
        messages.add("The door is unlocked.");
        messages.add("You can lock it using");
        messages.add("Will you lock the door?");
        messages.add("The door is locked.");
        messageTree.add(messages);
    }
    
    // trigger the interaction with the next-to object (if possible)
    // Note: flip the interactHappen flag if Player is in appropriate position
    // for search_interaction: return the list of items in the object (if any)
    //     open_lock_interaction: return the list contain (one) the key to open (if can be opened)
    private Array<Item> triggerInteraction() {
        for (MapObject object : interactionObjects) {
            // all interaction objects are guaranteed to be rectangle
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
                    
                    // retrieve type of interaction
                    String type = rectObject.getProperties().get("type", "", String.class);

                    // if searchable objects
                    if (type.equalsIgnoreCase("search_interaction")) {
                        interactHappen = true;
                        
                        // set the flag for type of interaction
                        typeInteraction = TYPE_INTERACTION.SEARCH_INTERACTION;
                        
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
                    
                    // if door object (open_lock_interaction)
                    else if (type.equalsIgnoreCase("open_lock_interaction")) {
                        // set the flag for type of interaction
                        typeInteraction = TYPE_INTERACTION.OPEN_LOCK_INTERACTION;
                        
                        boolean locked = rectObject.getProperties().get("locked", false, Boolean.class);
                        int keyID = rectObject.getProperties().get("keyID", -1, Integer.class);
                        Array<Item> needKey = new Array<Item>();
                        Item key = new Item("Needed key", keyID);
                        needKey.add(key);
                        
                        // extract the name of current door in interaction with Player
                        currentDoorName = rectObject.getName();
                        
                        // set the status of current door in interaction with Player
                        currentDoorLocked = locked;
//                        Gdx.app.log("locked: ", currentDoorLocked + "");
                        
                        // if the door is not locked
                        //      check the position of Player before invoking interaction
                        //      -> return the required key to lock it
                        if (!currentDoorLocked) {
                            MapObject o = doorObjects.get(currentDoorName);
                            RectangleMapObject r = (RectangleMapObject) o;
                            
                            if (!r.getRectangle().overlaps(player.getSprite().getBoundingRectangle())) {
                                interactHappen = true;
                                return needKey;
                            }
                        }
                        // if the door is locked
                        //      -> return the required key to unlock it, or
                        //                the key with keyID = -1 ; indicate
                        //                   that the door CANNOT be opened
                        else {
                            interactHappen = true;
                            return needKey;
                        }
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
    
    // show/hide the Player's inventory based on its current status
    private void showHideInventory() {
        // if inventory is currently closed
        if (!inventoryOpen) {
            inventoryOpen = true;       // set status to "opened"
            state = STATE.PAUSE;        // pause the game temporarily
            
            // create an array of names of Player's items
            Array<String> names = new Array<String>();
            if (player.getInventory().size == 0) {
                names.add("Nothing");
            } else {
                for (Item item : player.getInventory()) {
                    names.add(item.getItemName());
                }
            }
            
            // create the List panel to display the inventory
            inventory = new List(skin, "dimmed");
            inventory.setItems(names);
            inventory.setSelectedIndex(0);
            inventory.setSize(Gdx.graphics.getWidth()/6, Gdx.graphics.getHeight()/2);
            inventory.setPosition(Gdx.graphics.getWidth()*3/4 - inventory.getWidth()/2,
                    Gdx.graphics.getHeight()/2 - inventory.getHeight()/2);
            
            // create the description Label to display description of each item
            descLabel = new Label("", skin, "custom-small");
            descLabel.setSize(Gdx.graphics.getWidth()/6, Gdx.graphics.getHeight()/4);
            descLabel.setPosition(inventory.getX() + inventory.getWidth(),
                    inventory.getY() + inventory.getHeight() - descLabel.getHeight());
            descLabel.setAlignment(Align.topLeft);
            descLabel.setWrap(true);
            if (player.getInventory().size > 0) {
                descLabel.setText(player.getInventory().get(inventory.getSelectedIndex()).getDescription());
            }
            stage.addActor(descLabel);
            
            // create title: Inventory
            titleLabel = new Label("INVENTORY", skin, "subtitle");
            titleLabel.setSize(inventory.getWidth(), titleLabel.getHeight() * 3/2);
            titleLabel.setPosition(inventory.getX(), inventory.getY() + inventory.getHeight());
            titleLabel.setAlignment(Align.center);
            stage.addActor(titleLabel);
            
            // add InputListener to inventory
            inventory.addListener(new InputListener() {
                @Override
                public boolean keyUp(InputEvent event, int keycode) {
                    int currentIndex = inventory.getSelectedIndex();
                    int upIndex = currentIndex - 1;
                    int downIndex = currentIndex + 1;
                    String desc = "";

                    if (upIndex < 0) upIndex = inventory.getItems().size - 1;
                    if (downIndex >= inventory.getItems().size) downIndex = 0;

                    switch (keycode) {
                        case Keys.UP:
                            // scroll up the item objectItems
                            inventory.setSelectedIndex(upIndex);
                            
                            // display the corresponding description in descLabel
                            if (player.getInventory().size > 0)
                                desc = player.getInventory().get(upIndex).getDescription();
                            descLabel.setText(desc);
                            
                            break;
                        case Keys.DOWN:
                            // scroll down the item objectItems
                            inventory.setSelectedIndex(downIndex);
                            
                            // display the corresponding description in descLabel
                            if (player.getInventory().size > 0)
                                desc = player.getInventory().get(downIndex).getDescription();
                            descLabel.setText(desc);
                            
                            break;
                        case Keys.I:
                            // if the inventory is currently opened, press I again
                            // to close
                            
                            inventoryOpen = false;      // set status to "closed"
            
                            descLabel.remove();         // remove actors
                            inventory.remove();
                            titleLabel.remove();

                            state = STATE.ONGOING;      // unpause the game

                            resetInputProcessor();      // reset InputProcessor to this MapControlRenderer
                            break;
                        default:
                            break;
                    }

                    return true;
                }
            });
            stage.addActor(inventory);
            stage.setKeyboardFocus(inventory);
            
            Gdx.input.setInputProcessor(stage);     // change InputProcessor to stage
        }
    }
    
    // carry out search interaction
    private void searchInteraction() {
        // if no items
        if (objectItems == null) {
            if (indexForMessageTree == messageTree.get(0).size) {
                state = STATE.ONGOING;      // unpause the game
                interactHappen = false;     // flip the flag again
                typeInteraction = TYPE_INTERACTION.NO_INTERACTION;  // reset type of interaction to
                                                                    // NO_INTERACTION
                indexForMessageTree = 0;
                dialogBoxLabel.remove();
                return;
            }

            // create conversation box
            // for the first message 
            if (indexForMessageTree == 0) {
                dialogBoxLabel = new Label(messageTree.get(0).get(0), skin, "custom");
                dialogBoxLabel.setPosition(0, 0);
                dialogBoxLabel.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()/5);
                dialogBoxLabel.setAlignment(Align.topLeft);
//                dialogBoxLabel.setFontScale(2f);
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
//            Gdx.app.log("class: ", "" + objectItems.getClass());

            if (indexForMessageTree == messageTree.get(1).size) {
                state = STATE.ONGOING;      // unpause the game
                interactHappen = false;     // flip the flag again
                typeInteraction = TYPE_INTERACTION.NO_INTERACTION;  // reset type of interaction to
                                                                    // NO_INTERACTION
                indexForMessageTree = 0;
                dialogBoxLabel.remove();
                return;
            }

            // in case Player choose "Nothing" -> does not pick anything
            if (indexForMessageTree > messageTree.get(1).size) {
                indexForMessageTree = messageTree.get(1).size;
                return;
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
//                dialogBoxLabel.setFontScale(2f);
                stage.addActor(dialogBoxLabel);

                // create objectItems of items
                itemList = new List(skin, "custom");
                itemList.setItems(itemNames);
                itemList.setSelectedIndex(0);
                itemList.setSize(Gdx.graphics.getWidth()/4, Gdx.graphics.getHeight()/3);
                itemList.setPosition(Gdx.graphics.getWidth() - itemList.getWidth(), dialogBoxLabel.getHeight());
                itemList.addListener(new InputListener() {
                    @Override
                    public boolean keyDown(InputEvent event, int keycode) {
                        int currentIndex = itemList.getSelectedIndex();
                        int upIndex = currentIndex - 1;
                        int downIndex = currentIndex + 1;

                        if (upIndex < 0) upIndex = itemList.getItems().size - 1;
                        if (downIndex >= itemList.getItems().size) downIndex = 0;

//                        Gdx.app.log("currentIndex: ", "" + currentIndex);
//                        Gdx.app.log("upIndex: ", "" + upIndex);
//                        Gdx.app.log("downIndex: ", "" + downIndex);

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
    }
    
    // carry out open/lock interaction
    private void openLockInteraction() {
        if (objectItems == null) {
            Gdx.app.log("Error in locked door: ", "objectItems == null");
            return;
        }
        
        // if the door is locked
        if (currentDoorLocked) {
            // end of "conversation" -> reset 
            if (indexForMessageTree == messageTree.get(2).size) {
                state = STATE.ONGOING;
                interactHappen = false;
                typeInteraction = TYPE_INTERACTION.NO_INTERACTION;
                indexForMessageTree = 0;
                
                neededKey = null;
                currentDoorLocked = false;
                currentDoorName = "";
                
                dialogBoxLabel.remove();
                return;
            }

            // first
            if (indexForMessageTree == 0) {
                // create conversation box
                dialogBoxLabel = new Label(messageTree.get(2).get(0), skin, "custom");
                dialogBoxLabel.setPosition(0, 0);
                dialogBoxLabel.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()/5);
                dialogBoxLabel.setAlignment(Align.topLeft);
                stage.addActor(dialogBoxLabel);

                // check if Player have the needed "key" to unlock
                boolean haveIt = false;
                for (Item item : player.getInventory()) {
                    if (item.getItemID() == objectItems.get(0).getItemID()) {
                        haveIt = true;
                        neededKey = item;
                    }
                }
//                Gdx.app.log("haveIt: ", "" + haveIt);

                // if Player DOEST NOT have the neccessary key -> end the "conversation" dialog
                // otherwise; let it go on
                if (!haveIt) {       
                    indexForMessageTree = messageTree.get(2).size - 1;
                }
            }
            // second
            else if (indexForMessageTree == 1) {
                dialogBoxLabel.setText(messageTree.get(2).get(1) + " " + neededKey.getItemName());
            }
            // third
            else if (indexForMessageTree == 2) {
                dialogBoxLabel.setText(messageTree.get(2).get(2));

                // create a List contain Yes/No options
                Array<String> options = new Array<String>();
                options.add("Yes");
                options.add("No");
                itemList = new List(skin, "custom");
                itemList.setItems(options);
                itemList.setSelectedIndex(0);
                itemList.setSize(Gdx.graphics.getWidth()/4, Gdx.graphics.getHeight()/3);
                itemList.setPosition(Gdx.graphics.getWidth() - itemList.getWidth(), dialogBoxLabel.getHeight());
                itemList.addListener(new InputListener() {
                    @Override
                    public boolean keyDown(InputEvent event, int keycode) {
                        int currentIndex = itemList.getSelectedIndex();
                        int upIndex = currentIndex - 1;
                        int downIndex = currentIndex + 1;

                        if (upIndex < 0) upIndex = itemList.getItems().size - 1;
                        if (downIndex >= itemList.getItems().size) downIndex = 0;

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
                                // if Player choose "Yes" -> unlock the door
                                if (itemList.getSelectedIndex() == 0) {
                                    // set "locked" to false in object of DoorCollision
                                    MapObject object = doorObjects.get(currentDoorName);
                                    object.getProperties().put("locked", false);
                                    
                                    // set "locked" to false in object of ObjectInteration
                                    object = interactionObjects.get(currentDoorName);
                                    object.getProperties().put("locked", false);
                                    
                                    // set "locked" to false in object of ...
                                }
                                // if "No" -> end dialog "conversation"
                                else {
                                    indexForMessageTree = messageTree.get(2).size;  // end the dialog
                                }

                                // remove list of items from stage
                                itemList.remove();

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
            // others
            else {
                dialogBoxLabel.setText(messageTree.get(2).get(indexForMessageTree));
            }

            ++indexForMessageTree;
        }
        // if current door is not locked
        else {
            // end of "conversation" -> reset 
            if (indexForMessageTree == messageTree.get(3).size) {
                state = STATE.ONGOING;
                interactHappen = false;
                typeInteraction = TYPE_INTERACTION.NO_INTERACTION;
                indexForMessageTree = 0;
                
                neededKey = null;
                currentDoorLocked = false;
                currentDoorName = "";
                
                dialogBoxLabel.remove();
                return;
            }

            // first
            if (indexForMessageTree == 0) {
                // create conversation box
                dialogBoxLabel = new Label(messageTree.get(3).get(0), skin, "custom");
                dialogBoxLabel.setPosition(0, 0);
                dialogBoxLabel.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()/5);
                dialogBoxLabel.setAlignment(Align.topLeft);
                stage.addActor(dialogBoxLabel);

                // check if Player have the requied key to lock the door
                boolean haveIt = false;
                for (Item item : player.getInventory()) {
                    if (item.getItemID() == objectItems.get(0).getItemID()) {
                        haveIt = true;
                        neededKey = item;
                    }
                }

                // if Player DOEST NOT have the neccessary key -> end the "conversation" dialog
                // otherwise; let it go on
                if (!haveIt) {       
                    indexForMessageTree = messageTree.get(3).size - 1;
                }
            }
            // second
            else if (indexForMessageTree == 1) {
                dialogBoxLabel.setText(messageTree.get(3).get(1) + " " + neededKey.getItemName());
            }
            // third
            else if (indexForMessageTree == 2) {
                dialogBoxLabel.setText(messageTree.get(3).get(2));

                // create a List contain Yes/No options
                Array<String> options = new Array<String>();
                options.add("Yes");
                options.add("No");
                itemList = new List(skin, "custom");
                itemList.setItems(options);
                itemList.setSelectedIndex(0);
                itemList.setSize(Gdx.graphics.getWidth()/4, Gdx.graphics.getHeight()/3);
                itemList.setPosition(Gdx.graphics.getWidth() - itemList.getWidth(), dialogBoxLabel.getHeight());
                itemList.addListener(new InputListener() {
                    @Override
                    public boolean keyDown(InputEvent event, int keycode) {
                        int currentIndex = itemList.getSelectedIndex();
                        int upIndex = currentIndex - 1;
                        int downIndex = currentIndex + 1;

                        if (upIndex < 0) upIndex = itemList.getItems().size - 1;
                        if (downIndex >= itemList.getItems().size) downIndex = 0;

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
                                // if Player choose "Yes" -> lock the door
                                if (itemList.getSelectedIndex() == 0) {
                                    // set "locked" to true in object of DoorCollision
                                    MapObject object = doorObjects.get(currentDoorName);
                                    object.getProperties().put("locked", true);
                                    
                                    // set "locked" to true in object of ObjectInteration
                                    object = interactionObjects.get(currentDoorName);
                                    object.getProperties().put("locked", true);
                                    
                                    // set "locked" to true in object of ...
                                }
                                // if "No" -> end dialog "conversation"
                                else {
                                    indexForMessageTree = messageTree.get(3).size;  // end the dialog
                                }

                                // remove list of items from stage
                                itemList.remove();

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
            // others
            else {
                dialogBoxLabel.setText(messageTree.get(3).get(indexForMessageTree));
            }

            ++indexForMessageTree;
        }
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
                    objectItems = triggerInteraction(); // trigger interaction with the object   
                                                        // get list of items (if any) at the current object
                }
                
                // if no interactions with any objects -> break
                // otherwise; pause the game
                if (!interactHappen) {
                    break;
                } else {
                    state = STATE.PAUSE;            // temporarily "pause" the game -> Player can't move
                }
                
                // for search_interaction
                if (typeInteraction == TYPE_INTERACTION.SEARCH_INTERACTION) {
                    searchInteraction();
                }
                
                // for open_lock_interaction (with door)
                else if (typeInteraction == TYPE_INTERACTION.OPEN_LOCK_INTERACTION) {
                    openLockInteraction();
                }
                break;
            case Input.Keys.I:
                showHideInventory();
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
        stage.dispose();
        skin.dispose();
    }
    
}

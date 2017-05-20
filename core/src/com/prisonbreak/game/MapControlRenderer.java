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
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
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
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.prisonbreak.game.entities.Guard;
import com.prisonbreak.game.entities.Item;
import com.prisonbreak.game.entities.PatrolGuard;
import com.prisonbreak.game.entities.Player;
import com.prisonbreak.game.entities.StationGuard;

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
        NO_INTERACTION, SEARCH_INTERACTION, OPEN_LOCK_INTERACTION,
        READ_INTERACTION, PASS_UNLOCK_INTERACTION, BREAK_INTERACTION
    }
    private TYPE_INTERACTION typeInteraction = TYPE_INTERACTION.NO_INTERACTION;
    
    private final Player player;
    private final OrthographicCamera camera;
    private final ShapeRenderer shapeRenderer;
    private final int drawSpritesAfterLayer = this.map.getLayers().getIndex("Walls") + 1;
    private final int drawDetectionAreaAfterLayer = this.map.getLayers().getIndex("Tiles") + 1;
    private Array<Array<Item>> listItems;       // array holds the overall list of list items of all objects
    private Array<Array<String>> messageTree;   // array holds the message tree (for all objects)
    private Array<Item> objectItems;            // current list of items for one object
    private Item neededKey;                     // the required key to unlock an object (door)
    private Array<Guard> guards;
    
    private int indexForMessageTree = 0;
    private int indexRemainingItems = 1;        // for safe locker (pass_unlock_interaction)
    private String latestObjectName = "";       // name of the lastest object in interaction with Player
    private boolean interactHappen = false;     // flag -> indicate whether interaction happens
    private boolean inventoryOpen = false;      // flag -> indicate whether the inventory is currenly opened
    private boolean latestDoorLocked = false;   // flag -> indicate whether the latest door in interaction
                                                // with Player is locked or not
    private boolean doorHidden = false;         // indicate whether the current is hidden
    
    private int[][] blockedWallUnitGrid;
    private final MapObjects interactionObjects;
    private final MapObjects staticObjects;
    private final MapObjects doorObjects;
    private final MapObject winningLocation;
    private RectangleMapObject currentDoor;      // current door in contact, for hiding/showing unlocked door
    
    private TiledMapTile tile1;         // tiles that, together, represent a door object
    private TiledMapTile tile2;         // tile1 -> lower left cell ; tile2 -> lower right cell
    private TiledMapTile tile3;         // tile3 -> upper left cell ; tile4 -> upper right cell
    private TiledMapTile tile4;
//    private Array<TiledMapTile> tiles;
    
    public Stage stage;
    public Label dialogBoxLabel;
    public Label descLabel;             // description label, for inventory and read_interaction
    public Label titleLabel;
    public List itemList;
    public List inventory;
    public TextField passField;         // to enter password -> unlock objects
    public TextButton txtButton;
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
        
        // initialize tiles
//        tiles = new Array<TiledMapTile>();

        // initialize list of guards in the map
        initializeGuards();
        
        // create camera
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.translate(player.x, player.y);
        
        // create shape renderer
        shapeRenderer = new ShapeRenderer();
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
    
    public Array<Guard> getListGuards() {
        return guards;
    }
    
    public OrthographicCamera getCamera() {
        return camera;
    }
    
    public int[][] getBlockedWallsGrid() {
        return blockedWallUnitGrid;
    }
    
    public MapObjects getInteractionObjects() {
        return interactionObjects;
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
        item.setDescription("Karvick's uniform. There is an identity card in his pocket. Maybe you can make use of it ...");
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
        item.setDescription("Ordered meat for Henry Karvick.");
        list.add(item);
        item = new Item("Bottle of drink", 7);
        item.setDescription("A bottle full of cold water. Best for hot days.");
        list.add(item);
        listItems.add(list);
        
        // add a crowbar into the racks ; listItemID = 4
        list = new Array<Item>();
        item = new Item("Crowbar", 8);
        item.setDescription("A steel crowbar. Very strong and sturdy.");
        list.add(item);
        listItems.add(list);
        
        // add a crinkle letter into the draining holes (in bathroom) ; listItemID = 5
        list = new Array<Item>();
        item = new Item("Crinkle Letter", 9);
        item.setDescription("The letter is in bad condition. "
                + "You can hardly recognize any words at all, except a number '6'");
        list.add(item);
        listItems.add(list);
        
        // add the final key + an airplane ticket into the safe locker ; listItemID = 6
        list = new Array<Item>();
        item = new Item("Final Key", 10);
        item.setDescription("THIS IS THE KEY TO FREEDOM. Now, you only need to find the door to use it in.");
        list.add(item);
        item = new Item("Airplane Ticket", 11);
        item.setDescription("XXX Airlines.\nDATE: 12 Mar, 2025\nFLIGHT HOUR: 16:30\nFROM: Lanbagana, Homoca\nTO: Malag, Facas");
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
        messages.add("You found something. Which will you pick up?");
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
        
        
        // for read interaction
        // message for the fifth case - index = 4 -> Any readable objects
        messages = new Array<String>();
        messages.add("Something is written on the ");
        messages.add("...");
        messageTree.add(messages);
        
        
        // for pass-unlock interaction (use password to unlock objects)
        // message for the sixth case - index = 5 -> Door with password lock
        messages = new Array<String>();
        messages.add("The door is locked.");
        messages.add("There is a card holder at the handle. Use the card obtained with the uniform?");
        messages.add("A voice prompts for your identity ...");
        messages.add("The door is unlocked.");
        messageTree.add(messages);
        
        // message for the seventh case - index = 6 -> Safe locker with passwork lock
        messages = new Array<String>();
        messages.add("Enter the correct password to open.");
        messages.add("The safe locker has been unlocked");
        messages.add("Obtain 1 x ");
        messageTree.add(messages);
        
        
        // for break interaction (with door)
        // message for the eigth case - index = 7 -> Break the (office) door
        messages = new Array<String>();
        messages.add("The door is locked.");
        messages.add("You can break it using ");
        messages.add("Will you break the door?");
        messages.add("The door has been cracked open.");
        messageTree.add(messages);
    }
    
    // initialize list of guards
    private void initializeGuards() {
        String type, sheetName, direction;
        guards = new Array<Guard>();
        
        // extract all guard objects in map
        MapObjects guardObjects = map.getLayers().get("Guards").getObjects();
        
        // for each object
        for (MapObject guard : guardObjects) {
            RectangleMapObject rectGuard = (RectangleMapObject) guard;
            
            // if current guard is "not visible" -> ignore
            if (!rectGuard.isVisible()) {
                continue;
            }
            
            // extract values of attributes of 'guard' object
            type = rectGuard.getProperties().get("type", "none", String.class);
            sheetName = rectGuard.getProperties().get("sheetName", "", String.class);
            if (type.equalsIgnoreCase("none")) {
                Gdx.app.log("Error: ", "'type' attribute not found");
                return;
            }
            
            
            // if stationary guard
            if (type.equalsIgnoreCase("stationary")) {
                direction = rectGuard.getProperties().get("direction", "none", String.class);
                if (direction.equalsIgnoreCase("none")) {
                    Gdx.app.log("Error: ", "invalid value for direction attribute");
                    return;
                }
                
                // create the specified guard
                Guard specifiedGuard = new StationGuard(sheetName, direction,
                        rectGuard.getRectangle().getX(), rectGuard.getRectangle().getY());
                specifiedGuard.setMapControlRenderer(this);
                
                // add the corresponding guard to the array
                guards.add(specifiedGuard);
            }
            // if patrol guard
            else if (type.equalsIgnoreCase("patrol")) {
                // create the specified guard (without setting mark points)
                Guard specifiedGuard = new PatrolGuard(sheetName,
                        rectGuard.getRectangle().getX(), rectGuard.getRectangle().getY());
                specifiedGuard.setMapControlRenderer(this);
                
                // extract list of mark points from the map
                String listPoints = rectGuard.getProperties().get("markPoints", String.class);
                String[] tileIndices = listPoints.split(",");
                for (int i = 0, indexX, indexY; i < tileIndices.length; i += 2) {
                    indexX = Integer.parseInt(tileIndices[i]);
                    indexY = Integer.parseInt(tileIndices[i + 1]);
                    
//                    Gdx.app.log("x: ", "" + indexX);
//                    Gdx.app.log("y: ", "" + indexY);
                    
                    if (!((PatrolGuard) specifiedGuard).addMarkPoint(new Vector2(indexX * 32f, indexY * 32f))) {
                        Gdx.app.log("Cannot add mark point ", "(indeX, indexY)");
                    }
                }
                
//                Gdx.app.log("Mark point 1: ", ((PatrolGuard) specifiedGuard).listMarkPoints.get(0).toString());
//                Gdx.app.log("Mark point 2: ", ((PatrolGuard) specifiedGuard).listMarkPoints.get(1).toString());
                
                // add the corresponding guard to the array
                guards.add(specifiedGuard);
            }
        }
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
                if (directionCheck.equalsIgnoreCase("all") ||
                        (!player.getCurrentDirection().equalsIgnoreCase("none") &&
                        player.getCurrentDirection().equalsIgnoreCase(directionCheck))) {
                    
                    // retrieve type of interaction
                    String type = rectObject.getProperties().get("type", "", String.class);

                    // if searchable objects
                    if (type.equalsIgnoreCase("search_interaction")) {
                        interactHappen = true;
                        
                        // set the flag for type of interaction
                        typeInteraction = TYPE_INTERACTION.SEARCH_INTERACTION;
                        
                        // extract attributes' values
                        boolean haveItems = rectObject.getProperties().get("haveItems", false, Boolean.class);
                        int listID = rectObject.getProperties().get("listItemID", -1, Integer.class);

                        // if object does contain items
                        //      -> return list of those items
                        // otherwise -> return null
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
                        // extract attributes' values
                        boolean locked = rectObject.getProperties().get("locked", false, Boolean.class);
                        int keyID = rectObject.getProperties().get("keyID", -1, Integer.class);
                        Array<Item> needKey = new Array<Item>();
                        Item key = new Item("Needed key", keyID);
                        needKey.add(key);
                        
                        // extract the name of current door in interaction with Player
                        latestObjectName = rectObject.getName();
                        
                        // set the status of current door in interaction with Player
                        latestDoorLocked = locked;
//                        Gdx.app.log("locked: ", latestDoorLocked + "");
                        
                        // if the door is not locked
                        //      check the position of Player before invoking interaction
                        //      -> return the required key to lock it
                        if (!latestDoorLocked) {
                            MapObject o = doorObjects.get(latestObjectName);
                            RectangleMapObject r = (RectangleMapObject) o;
                            
                            if (!r.getRectangle().overlaps(player.getSprite().getBoundingRectangle())) {
                                interactHappen = true;
                                
                                // set the flag for type of interaction
                                typeInteraction = TYPE_INTERACTION.OPEN_LOCK_INTERACTION;
                                
                                return needKey;
                            }
                        }
                        // if the door is locked
                        //      -> return the required key to unlock it, or
                        //                the key with keyID = -1 ; indicate
                        //                   that the door CANNOT be opened
                        else {
                            interactHappen = true;
                            
                            // set the flag for type of interaction
                            typeInteraction = TYPE_INTERACTION.OPEN_LOCK_INTERACTION;
                            
                            return needKey;
                        }
                    }
                    
                    // if readable objects
                    else if (type.equalsIgnoreCase("read_interaction")) {
                        interactHappen = true;
                        
                        // set the flag for type of interaction
                        typeInteraction = TYPE_INTERACTION.READ_INTERACTION;
                        
                        // extract attributes' values
                        String message = rectObject.getProperties().get("message", "", String.class);
                        
                        // set the name of the object
                        latestObjectName = rectObject.getProperties().get("name", "Object", String.class);
                        
                        // return a list with one "item" contain the message; id for item = -2
                        Array<Item> l = new Array<Item>();
                        Item mess = new Item("Written Message", -2);
                        mess.setDescription(message);
                        l.add(mess);
                        
                        return l;
                    }
                    
                    // if password locking objects
                    else if (type.equalsIgnoreCase("pass_unlock_interaction")) {
                        boolean locked = rectObject.getProperties().get("locked", false, Boolean.class);
                        
                        // if the object has already been unlocked
                        //      -> return null; no interaction happens
                        if (!locked) {
                            return null;
                        }
                        // otherwise
                        //      -> return a list with first "item" containing the password,
                        //          and all the remaining items are ones that are inside
                        //          the object (in case of safe locker)
                        else {
                            interactHappen = true;
                            
                            // set the flag indicating the type of interaction
                            typeInteraction = TYPE_INTERACTION.PASS_UNLOCK_INTERACTION;
                            
                            // extract the name of object in interaction
                            latestObjectName = rectObject.getName();
                            
                            // extract attributes
                            String password = rectObject.getProperties().get("pass", "", String.class);
                            Item passItem = new Item("Password", -3);   // item contains password
                            passItem.setDescription(password);
                            
                            // create the list
                            Array<Item> list = new Array<Item>();
                            list.add(passItem);
                            
                            // if safe locker, add all the contained items into the list
                            int listID = rectObject.getProperties().get("listItemID", -1, Integer.class);
                            if (listID == -1) {
                                Gdx.app.log("Error: ", "listItemID attribute not found");
                            } else {
                                list.addAll(listItems.get(listID));
                            }
                            
                            return list;
                        }
                    }
                    
                    // if breakable object (here, break the office door)
                    else if (type.equalsIgnoreCase("break_interaction")) {
                        boolean broken = rectObject.getProperties().get("broken", true, Boolean.class);
                        
                        // if object has been cracked open (broken = true)
                        //      -> do nothing, no interaction happens, return null
                        if (broken) {
                            return null;
                        }
                        // otherwise
                        //      -> return the required item to break the object (door), which is a crowbar in this game
                        else {
                            interactHappen = true;
                            typeInteraction = TYPE_INTERACTION.BREAK_INTERACTION;
                            
                            // extract the name of object in interaction
                            latestObjectName = rectObject.getName();
                            
                            int id = rectObject.getProperties().get("breakItemID", Integer.class);
                            Item item = new Item("Required Item", id);
                            Array<Item> l = new Array<Item>();
                            l.add(item);
                            
                            return l;
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
//        player.x = 73 * 32;
//        player.y = 81 * 32;
    }
    
    // check winning condition
    private boolean winGame() {
        RectangleMapObject rectLoc = (RectangleMapObject) winningLocation;
        
        return state != STATE.END && 
                rectLoc.getRectangle().contains(player.getSprite().getBoundingRectangle());
    }
    
    // perform actions in case PLayer loses the game
    private void loseGame() {
        // set state to PAUSE
        state = STATE.PAUSE;
        
        // notify Player
        dialogBoxLabel = new Label("Ouch!!! You have been found.", skin, "custom");
        dialogBoxLabel.setPosition(0, 0);
        dialogBoxLabel.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()/5);
        dialogBoxLabel.setAlignment(Align.topLeft);
        dialogBoxLabel.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Keys.F) {
                    state = STATE.LOSE;
                    dialogBoxLabel.remove();
                    resetInputProcessor();
                }
                return true;
            }
        });
        stage.addActor(dialogBoxLabel);
        stage.setKeyboardFocus(dialogBoxLabel);
        
        Gdx.input.setInputProcessor(stage);
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
                                // scroll up the item list
                                itemList.setSelectedIndex(upIndex);
                                break;
                            case Keys.DOWN:
                                // scroll down the item list
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
        if (latestDoorLocked) {
            // end of "conversation" -> reset 
            if (indexForMessageTree == messageTree.get(2).size) {
                state = STATE.ONGOING;
                interactHappen = false;
                typeInteraction = TYPE_INTERACTION.NO_INTERACTION;
                indexForMessageTree = 0;
                
                neededKey = null;
                latestDoorLocked = false;
//                latestObjectName = "";
                
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
                        break;
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
                dialogBoxLabel.setText(messageTree.get(2).get(1) + " " + neededKey.getItemName() + ".");
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
                                // scroll up the option list
                                itemList.setSelectedIndex(upIndex);
                                break;
                            case Keys.DOWN:
                                // scroll down the option list
                                itemList.setSelectedIndex(downIndex);
                                break;
                            case Keys.F:
                                // if Player choose "Yes" -> unlock the door
                                if (itemList.getSelectedIndex() == 0) {
                                    // set "locked" to false in object of DoorCollision
                                    MapObject object = doorObjects.get(latestObjectName);
                                    object.getProperties().put("locked", false);
                                    
                                    // set "locked" to false in object of ObjectInteration
                                    object = interactionObjects.get(latestObjectName);
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
                latestDoorLocked = false;
//                latestObjectName = "";
                
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
                dialogBoxLabel.setText(messageTree.get(3).get(1) + " " + neededKey.getItemName() + ".");
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
                                    MapObject object = doorObjects.get(latestObjectName);
                                    object.getProperties().put("locked", true);
                                    
                                    // set "locked" to true in object of ObjectInteration
                                    object = interactionObjects.get(latestObjectName);
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
    
    // carry out read interaction
    private void readInteraction() {
        if (objectItems == null) {
            Gdx.app.log("Error in read_interaction: ", "objectItems == null");
            return;
        }
        
        // end of conversation -> reset
        if (indexForMessageTree == messageTree.get(4).size) {
            state = STATE.ONGOING;
            interactHappen = false;
            typeInteraction = TYPE_INTERACTION.NO_INTERACTION;
            indexForMessageTree = 0;
            
            dialogBoxLabel.remove();    // remove actors from stage
            return;
        }
        
        // first
        if (indexForMessageTree == 0) {
            // create conversation box
            dialogBoxLabel = new Label(messageTree.get(4).get(0) + latestObjectName + ".", skin, "custom");
            dialogBoxLabel.setPosition(0, 0);
            dialogBoxLabel.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()/5);
            dialogBoxLabel.setAlignment(Align.topLeft);
            stage.addActor(dialogBoxLabel);
        }
        // second -> display along the message written on the object
        else if (indexForMessageTree == 1) {
            // set the test of conversation box
            dialogBoxLabel.setText(messageTree.get(4).get(1));
            
            // create description/message box to display the written message
            descLabel = new Label("\"" + objectItems.get(0).getDescription() + "\"", skin, "custom");
            descLabel.setSize(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
            descLabel.setPosition(Gdx.graphics.getWidth()/2 - descLabel.getWidth()/2,
                    Gdx.graphics.getHeight()*3/5 - descLabel.getHeight()/2);
            descLabel.setAlignment(Align.topLeft);
            descLabel.setWrap(true);
            descLabel.addListener(new InputListener() {
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    // if user presses F again, "turn off" the displayed message
                    if (keycode == Keys.F) {
                        descLabel.remove();     // remove the message label
                        resetInputProcessor();  // reset the InputProcessor to MapControlRenderer
                        return true;
                    }
                    
                    return false;
                }
            });
            stage.addActor(descLabel);
            stage.setKeyboardFocus(descLabel);
            
            // set InputProcessor to stage
            Gdx.input.setInputProcessor(stage);
        }
        // others
        else {
            dialogBoxLabel.setText(messageTree.get(4).get(indexForMessageTree));
        }
        
        ++indexForMessageTree;
    }
    
    // carry out password-unlock interaction (using password to unlock objects)
    private void passUnlockInteraction() {
        // for door object
        if (objectItems.size == 1) {
            // end of "conversation" -> reset
            if (indexForMessageTree == messageTree.get(5).size) {
                state = STATE.ONGOING;
                interactHappen = false;
                typeInteraction = TYPE_INTERACTION.NO_INTERACTION;
                indexForMessageTree = 0;
                
                dialogBoxLabel.remove();
                return;
            }
            
            // first
            if (indexForMessageTree == 0) {
                // create conversation box
                dialogBoxLabel = new Label(messageTree.get(5).get(0), skin, "custom");
                dialogBoxLabel.setPosition(0, 0);
                dialogBoxLabel.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()/5);
                dialogBoxLabel.setAlignment(Align.topLeft);
                stage.addActor(dialogBoxLabel);
                
                // check if Player have required card to unlock (obtained from the uniform)
                boolean haveIt = false;
                for (Item item : player.getInventory()) {
                    if (item.getItemID() == 3) {    // the required card in uniform - id = 3
                        haveIt = true;
                        break;
                    }
                }
                
                // if not
                if (!haveIt) {
                    indexForMessageTree = messageTree.get(5).size - 1;
                }
            }
            // second
            else if (indexForMessageTree == 1) {
                dialogBoxLabel.setText(messageTree.get(5).get(1));
                
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
                                // scroll up the option list
                                itemList.setSelectedIndex(upIndex);
                                break;
                            case Keys.DOWN:
                                // scroll down the option list
                                itemList.setSelectedIndex(downIndex);
                                break;
                            case Keys.F:
                                // if Player chooses "No" -> end the dialog
                                // otherwise; let it continue on
                                if (itemList.getSelectedIndex() == 1) {
                                    indexForMessageTree = messageTree.get(5).size;
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
            // third
            else if (indexForMessageTree == 2) {
                dialogBoxLabel.setText(messageTree.get(5).get(2));
                
                // create a TextField for player to enter password
                passField = new TextField("Full name", skin, "default");
                passField.setHeight(25);
                passField.setPosition(Gdx.graphics.getWidth()/2 - passField.getWidth()/2,
                        Gdx.graphics.getHeight()/2 - passField.getHeight()/2);
                stage.addActor(passField);
                
                // create button, goes with the textfield
                txtButton = new TextButton("Enter", skin, "round");
                txtButton.setHeight(passField.getHeight());
                txtButton.setPosition(passField.getWidth() + passField.getX(), passField.getY());
                txtButton.addListener(new ClickListener() {
                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                        // if player enters correct name of any guards -> unlock the door
                        // note: in this case, we only check for the name 'Henry Karvick'
                        String enterName = passField.getText();
                        if (enterName.equalsIgnoreCase(objectItems.first().getDescription())) {
                            // unlock the door
                            // set "locked" to false in object of DoorCollision
                            MapObject object = doorObjects.get(latestObjectName);
                            object.getProperties().put("locked", false);

                            // set "locked" to false in object of ObjectInteration
                            object = interactionObjects.get(latestObjectName);
                            object.getProperties().put("locked", false);
                            
                            dialogBoxLabel.setText("Valid name.");
                        }
                        // enter wrong name
                        else {
                            dialogBoxLabel.setText("Invalid name.");

                            // end the dialog
                            indexForMessageTree = messageTree.get(5).size;
                        }
                        
                        passField.remove();     // remove actors from stage
                        txtButton.remove();
                        
                        resetInputProcessor();  // reset InputProcessor to MapControlRenderer
                        
                        return true;
                    }
                });
                stage.addActor(txtButton);
                
                Gdx.input.setInputProcessor(stage);
            }
            // others
            else {
                dialogBoxLabel.setText(messageTree.get(5).get(indexForMessageTree));
            }
            
            ++indexForMessageTree;
        }
        
        // for safe locker object
        else {
            // end of "conversation" -> reset
            if (indexForMessageTree == messageTree.get(6).size) {
                state = STATE.ONGOING;
                interactHappen = false;
                typeInteraction = TYPE_INTERACTION.NO_INTERACTION;
                indexForMessageTree = 0;
                
                dialogBoxLabel.remove();
                return;
            }
            
            // first
            if (indexForMessageTree == 0) {
                // create conversation box
                dialogBoxLabel = new Label(messageTree.get(6).get(0), skin, "custom");
                dialogBoxLabel.setPosition(0, 0);
                dialogBoxLabel.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()/5);
                dialogBoxLabel.setAlignment(Align.topLeft);
                stage.addActor(dialogBoxLabel);
                
                // create a TextField + TextButton -> prompt for password to unlock the safe locker
                // textfield
                passField = new TextField("", skin, "default");
                passField.setHeight(25);
                passField.setPosition(Gdx.graphics.getWidth()/2 - passField.getWidth()/2,
                        Gdx.graphics.getHeight()/2 - passField.getHeight()/2);
                stage.addActor(passField);
                
                // textbutton
                txtButton = new TextButton("Enter", skin, "round");
                txtButton.setHeight(passField.getHeight());
                txtButton.setPosition(passField.getWidth() + passField.getX(), passField.getY());
                txtButton.addListener(new ClickListener() {
                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                        // if Player enters the correct password -> unlock the safe locker
                        //      -> Player obtains all items in it
                        // password: 3863 - get from object in the map
                        String enterPass = passField.getText();
                        if (enterPass.equalsIgnoreCase(objectItems.first().getDescription())) {
                            // unlock the safe locker
                            // set "locked" to false in object of ObjectInteraction
                            MapObject object = interactionObjects.get(latestObjectName);
                            object.getProperties().put("locked", false);
                            
                            dialogBoxLabel.setText("*Clicked*");
                        }
                        // enter wrong password
                        else {
                            dialogBoxLabel.setText("Wrong password!");

                            // end the dialog
                            indexForMessageTree = messageTree.get(6).size;
                        }
                        
                        passField.remove();     // remove actors from stage
                        txtButton.remove();
                        
                        resetInputProcessor();  // reset InputProcessor to MapControlRenderer
                        
                        return true;
                    }
                });
                stage.addActor(txtButton);
                
                Gdx.input.setInputProcessor(stage);
            }
            // last - message when obtain items
            else if (indexForMessageTree == messageTree.get(6).size - 1) {
                dialogBoxLabel.setText(messageTree.get(6).get(indexForMessageTree) +
                        objectItems.get(indexRemainingItems).getItemName());
                
                // add the corresponding item into Player's inventory
//                player.getInventory().add(objectItems.get(indexRemainingItems++));
                player.addItem(objectItems.get(indexRemainingItems++));
                
                if (indexRemainingItems < objectItems.size) {
                    --indexForMessageTree;
                }
            }
            // others
            else {
                dialogBoxLabel.setText(messageTree.get(6).get(indexForMessageTree));
            }
            
            ++indexForMessageTree;
        }
    }
    
    // carry out break interaction (crack the door open)
    private void breakInteraction() {
        // end of "conversation" -> reset
        if (indexForMessageTree == messageTree.get(7).size) {
            state = STATE.ONGOING;
            interactHappen = false;
            typeInteraction = TYPE_INTERACTION.NO_INTERACTION;
            indexForMessageTree = 0;

            dialogBoxLabel.remove();
            return;
        }
        
        // first
        if (indexForMessageTree == 0) {
            // create conversation box
            dialogBoxLabel = new Label(messageTree.get(7).get(0), skin, "custom");
            dialogBoxLabel.setPosition(0, 0);
            dialogBoxLabel.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()/5);
            dialogBoxLabel.setAlignment(Align.topLeft);
            stage.addActor(dialogBoxLabel);
            
            // check if Player has the required item to break the door
            boolean haveIt = false;
            for (Item item : player.getInventory()) {
                if (item.getItemID() == objectItems.first().getItemID()) {
                    haveIt = true;
                    break;
                }
            }
            
            // if not -> end the dialog
            if (!haveIt) {
                indexForMessageTree = messageTree.get(7).size - 1;
            }
        }
        // third
        else if (indexForMessageTree == 2) {
            dialogBoxLabel.setText(messageTree.get(7).get(2));

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
                            // scroll up the option list
                            itemList.setSelectedIndex(upIndex);
                            break;
                        case Keys.DOWN:
                            // scroll down the option list
                            itemList.setSelectedIndex(downIndex);
                            break;
                        case Keys.F:
                            // if Player choose "Yes" -> crack the door open
                            if (itemList.getSelectedIndex() == 0) {
                                // remove the door
                                // extract neccessary attributes
                                MapObject object = doorObjects.get(latestObjectName);
                                String layerName = object.getProperties().get("tileLayerName", "", String.class);
                                TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(layerName);
                                int x = object.getProperties().get("lowerLeftX", Integer.class);
                                int y = object.getProperties().get("lowerLeftY", Integer.class);
                                
                                // extract the tiles of the door
                                tile1 = layer.getCell(x, y).getTile();            // lower left
                                tile2 = layer.getCell(x + 1, y).getTile();        // lower right
                                tile3 = layer.getCell(x, y + 1).getTile();        // upper left
                                tile4 = layer.getCell(x + 1, y + 1).getTile();    // upper right

                                // "erase" the door
                                layer.getCell(x, y).setTile(null);
                                layer.getCell(x + 1, y).setTile(null);
                                layer.getCell(x, y + 1).setTile(null);
                                layer.getCell(x + 1, y + 1).setTile(null);

                                
                                // set "locked" to false in object of DoorCollision
                                object = doorObjects.get(latestObjectName);
                                object.getProperties().put("locked", false);
                                
                                // set "broken" to true in object of ObjectInteration
                                object = interactionObjects.get(latestObjectName);
                                object.getProperties().put("broken", true);
                            }
                            // if "No" -> end dialog "conversation"
                            else {
                                indexForMessageTree = messageTree.get(7).size;  // end the dialog
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
            dialogBoxLabel.setText(messageTree.get(7).get(indexForMessageTree));
        }
        
        ++indexForMessageTree;
    }
    
    @Override
    public void render() {
        if (state == STATE.ONGOING) {
            player.update();
            moveCamera();
            
            // check if Player is detected by the Guards
            for (Guard guard : guards) {
                if (guard instanceof PatrolGuard) {
                    ((PatrolGuard) guard).update();
                }
                
                if (guard.detectPlayer()) {
//                    Gdx.app.log("Game over", "");
                    loseGame();
                    break;
                }
            }
            
            // search for the door in contact
            for (Object o : doorObjects) {
                RectangleMapObject r = null;
                if (o instanceof RectangleMapObject) {
                    r = (RectangleMapObject) o;
                }
                
                if (r != null && r.getRectangle().overlaps(player.getSprite().getBoundingRectangle())) {
                    currentDoor = r;
                    break;
                }
            }
            
            // the door "disappears" when Player steps in
            // and "showed" again when Player steps out
            if (currentDoor != null) {                
                int x = currentDoor.getProperties().get("lowerLeftX", Integer.class);
                int y = currentDoor.getProperties().get("lowerLeftY", Integer.class);
                String name = currentDoor.getProperties().get("tileLayerName", String.class);
                TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(name);
                
                // if the door has not been hidden, and Player steps in -> hide the door
                if (!currentDoor.getProperties().get("locked", Boolean.class) && !doorHidden &&
                        currentDoor.getRectangle().overlaps(player.getSprite().getBoundingRectangle())) {
                    // extract the tiles
                    tile1 = layer.getCell(x, y).getTile();            // lower left
                    tile2 = layer.getCell(x + 1, y).getTile();        // lower right
                    tile3 = layer.getCell(x, y + 1).getTile();        // upper left
                    tile4 = layer.getCell(x + 1, y + 1).getTile();    // upper right
                    
                    // "hide" the door when Player steps in
                    layer.getCell(x, y).setTile(null);
                    layer.getCell(x + 1, y).setTile(null);
                    layer.getCell(x, y + 1).setTile(null);
                    layer.getCell(x + 1, y + 1).setTile(null);
                    
                    doorHidden = true;      // set the flag
                }
                
                // if the door is currently hidden, and Player steps out -> show it
                if (!currentDoor.getProperties().get("locked", Boolean.class) && doorHidden &&
                        !currentDoor.getRectangle().overlaps(player.getSprite().getBoundingRectangle())) {
                    // "show" the door when Player steps out
                    layer.getCell(x, y).setTile(tile1);
                    layer.getCell(x + 1, y).setTile(tile2);
                    layer.getCell(x, y + 1).setTile(tile3);
                    layer.getCell(x + 1, y + 1).setTile(tile4);
                    
                    doorHidden = false;
                }
            }
            
            
            
            // update winning state
            if (winGame()) state = STATE.WIN;
        }
        
        shapeRenderer.setProjectionMatrix(camera.combined);
        
        beginRender();
        
        int currentLayer = 0;
        for (MapLayer layer : map.getLayers()) {
            if (layer.isVisible()) {
                // tile layer
                if (layer instanceof TiledMapTileLayer) {
                    renderTileLayer((TiledMapTileLayer) layer);
                    ++currentLayer;
                    
                    // layer to draw characters and guards
                    if (currentLayer == drawSpritesAfterLayer) {
                        // draw player
                        player.getSprite().draw(this.getBatch());
                        
                        // draw all the guards
                        for (Guard guard : guards) {
                            guard.getSprite().draw(this.getBatch());
                        }
                    }
                    // layer to draw guard's detection area
                    // note: even though detection area is RECTANGLE
                    //      , draw a POLYGON instead (POLYGON still inside the RECTANGLE)                            
                    if (currentLayer == drawDetectionAreaAfterLayer) {
                        for (Guard guard : guards) {
                            endRender();
                            
                            // first, determine the four vertices
                            float nearLeftX, nearLeftY, nearRightX, nearRightY,     // "nearer" points in Guard's POV
                                    farLeftX, farLeftY, farRightX, farRightY;       // "further" points in Guard's POV
                            if (guard.getCurrentDirection().equalsIgnoreCase("up")) {
                                nearLeftX = guard.getDetectArea().x + guard.getDetectArea().width/4;
                                nearRightX = guard.getDetectArea().x + guard.getDetectArea().width*3/4;
                                nearLeftY = nearRightY = guard.getDetectArea().y;
                                
                                farLeftX = guard.getDetectArea().x;
                                farRightX = guard.getDetectArea().x + guard.getDetectArea().width;
                                farLeftY = farRightY = guard.getDetectArea().y + guard.getDetectArea().height;
                            } else if (guard.getCurrentDirection().equalsIgnoreCase("down")) {
                                nearLeftX = guard.getDetectArea().x + guard.getDetectArea().width*3/4;
                                nearRightX = guard.getDetectArea().x + guard.getDetectArea().width/4;
                                nearLeftY = nearRightY = guard.getDetectArea().y + guard.getDetectArea().height;
                                
                                farLeftX = guard.getDetectArea().x + guard.getDetectArea().width;
                                farRightX = guard.getDetectArea().x;
                                farLeftY = farRightY = guard.getDetectArea().y;
                            } else if (guard.getCurrentDirection().equalsIgnoreCase("right")) {
                                nearLeftY = guard.getDetectArea().y + guard.getDetectArea().height*3/4;
                                nearRightY = guard.getDetectArea().y + guard.getDetectArea().height/4;
                                nearLeftX = nearRightX = guard.getDetectArea().x;
                                
                                farLeftY = guard.getDetectArea().y + guard.getDetectArea().height;
                                farRightY = guard.getDetectArea().y;
                                farLeftX = farRightX = guard.getDetectArea().x + guard.getDetectArea().width;
                            } else if (guard.getCurrentDirection().equalsIgnoreCase("left")) {
                                nearLeftY = guard.getDetectArea().y + guard.getDetectArea().height/4;
                                nearRightY = guard.getDetectArea().y + guard.getDetectArea().height*3/4;
                                nearLeftX = nearRightX = guard.getDetectArea().x + guard.getDetectArea().width;
                                
                                farLeftY = guard.getDetectArea().y;
                                farRightY = guard.getDetectArea().y + guard.getDetectArea().height;
                                farLeftX = farRightX = guard.getDetectArea().x;
                            } else {
                                nearLeftX = nearLeftY = nearRightX = nearRightY = 0;
                                farLeftX = farLeftY = farRightY = farRightX = 0;
                            }
                            
                            // draw detection area
                            Gdx.gl.glEnable(GL20.GL_BLEND);
                            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                            shapeRenderer.setColor(1, 0, 0, 0.5f);
//                            shapeRenderer.rect(guard.getDetectArea().getX(),
//                                    guard.getDetectArea().y,
//                                    guard.getDetectArea().getWidth(),
//                                    guard.getDetectArea().getHeight());
//                            shapeRenderer.triangle(guard.getDetectArea().x + guard.getDetectArea().width/2,
//                                    guard.getDetectArea().y,
//                                    guard.getDetectArea().x,
//                                    guard.getDetectArea().y + guard.getDetectArea().height,
//                                    guard.getDetectArea().x + guard.getDetectArea().width,
//                                    guard.getDetectArea().y + guard.getDetectArea().height);
                            if (guard.getCurrentDirection().equalsIgnoreCase("up")) {
                                // the body (rectangle) of the polygon
                                shapeRenderer.rect(nearLeftX, nearLeftY,
                                        nearRightX - nearLeftX,
                                        farLeftY - nearLeftY);
                                
                                // the left side (triangle) of the polygon
                                shapeRenderer.triangle(nearLeftX, nearLeftY,
                                        farLeftX, farLeftY,
                                        nearLeftX, farLeftY);
                                
                                // the right side (triangle) of the polygon
                                shapeRenderer.triangle(nearRightX, nearRightY,
                                        farRightX, farRightY,
                                        nearRightX, farRightY);
                            } else if (guard.getCurrentDirection().equalsIgnoreCase("down")) {
                                // the body (rectangle) of the polygon
                                shapeRenderer.rect(nearRightX, farRightY,
                                        nearLeftX - nearRightX,
                                        nearRightY - farRightY);
                                
                                // the left side (triangle) of the polygon
                                shapeRenderer.triangle(nearLeftX, nearLeftY,
                                        farLeftX, farLeftY,
                                        nearLeftX, farLeftY);
                                
                                // the right side (triangle) of the polygon
                                shapeRenderer.triangle(nearRightX, nearRightY,
                                        farRightX, farRightY,
                                        nearRightX, farRightY);
                            } else if (guard.getCurrentDirection().equalsIgnoreCase("right")) {
                                // the body (rectangle) of the polygon
                                shapeRenderer.rect(nearRightX, nearRightY,
                                        farRightX - nearRightX,
                                        nearLeftY - nearRightY);
                                
                                // the left side (triangle) of the polygon
                                shapeRenderer.triangle(nearLeftX, nearLeftY,
                                        farLeftX, farLeftY,
                                        farLeftX, nearLeftY);
                                
                                // the right side (triangle) of the polygon
                                shapeRenderer.triangle(nearRightX, nearRightY,
                                        farRightX, farRightY,
                                        farRightX, nearRightY);
                            } else if (guard.getCurrentDirection().equalsIgnoreCase("left")) {
                                // the body (rectangle) of the polygon
                                shapeRenderer.rect(farLeftX, nearLeftY,
                                        nearLeftX - farLeftX,
                                        nearRightY - nearLeftY);
                                
                                // the left side (triangle) of the polygon
                                shapeRenderer.triangle(nearLeftX, nearLeftY,
                                        farLeftX, farLeftY,
                                        farLeftX, nearLeftY);
                                
                                // the right side (triangle) of the polygon
                                shapeRenderer.triangle(nearRightX, nearRightY,
                                        farRightX, farRightY,
                                        farRightX, nearRightY);
                            }
                            
                            shapeRenderer.end();
                            Gdx.gl.glDisable(GL20.GL_BLEND);
                            
                            beginRender();
                        }
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
                
                // for read_interaction
                else if (typeInteraction == TYPE_INTERACTION.READ_INTERACTION) {
                    readInteraction();
                }
                
                // for pass_unlock_interaction
                else if (typeInteraction == TYPE_INTERACTION.PASS_UNLOCK_INTERACTION) {
                    passUnlockInteraction();
                }
                
                // for break_interaction
                else if (typeInteraction == TYPE_INTERACTION.BREAK_INTERACTION) {
                    breakInteraction();
                }
                
                break;
            case Input.Keys.I:
                if (state != STATE.PAUSE)
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

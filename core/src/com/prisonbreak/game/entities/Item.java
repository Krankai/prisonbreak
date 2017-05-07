/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prisonbreak.game.entities;

/**
 *
 * @author krankai
 */
public class Item {
    
    private String itemName;
    private int itemID;
    private String description;
    
    public Item(String name, int id) {
        itemName = name;
        itemID = id;
        description = "No description.";
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public void setItemName(String name) {
        itemName = name;
    }
    
    public int getItemID() {
        return itemID;
    }
    
    public void setItemID(int id) {
        itemID = id;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String desc) {
        description = desc;
    }
    
}

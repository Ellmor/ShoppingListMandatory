package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class ShoppingList {

    private String name;
    private Date createDate;
    private ArrayList<ShoppingItem> shoppingItems;

    public ShoppingList() {
        this.name="";
        this.createDate = new Date();
        this.shoppingItems = new ArrayList<>();
    }

    public ShoppingList(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            ShoppingList sl = mapper.readValue(json, ShoppingList.class);
            this.shoppingItems = sl.getShoppingItems();
            this.createDate = sl.getCreateDate();
            this.name = sl.getName();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ShoppingList(String name, ArrayList<ShoppingItem> shoppingItems) {
        this();
        this.name = name;
        this.shoppingItems = shoppingItems;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public ArrayList<ShoppingItem> getShoppingItems() {
        return shoppingItems;
    }

    public void setShoppingItems(ArrayList<ShoppingItem> shoppingItems) {
        this.shoppingItems = shoppingItems;
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "PARSING ERROR";
        }
    }
}

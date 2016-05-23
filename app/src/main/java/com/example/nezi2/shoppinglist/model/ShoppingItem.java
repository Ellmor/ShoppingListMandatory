package com.example.nezi2.shoppinglist.model;

import java.io.Serializable;

public class ShoppingItem implements Serializable {
    private String name;
    private double quantity;

    public ShoppingItem(){}

    public ShoppingItem(String _name, double _quantity) {
        this.name = _name;
        this.quantity = _quantity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
}

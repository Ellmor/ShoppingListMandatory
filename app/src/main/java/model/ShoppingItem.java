package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.StdJdkSerializers;

import java.io.IOException;
import java.io.Serializable;

public class ShoppingItem implements Serializable {
    private double _price;
    private String _name;
    private String _description;
    private ProductType _productType;

    public ShoppingItem(String json){
        ObjectMapper mapper = new ObjectMapper();
        try {
            ShoppingItem si = mapper.readValue(json, ShoppingItem.class);
            this._description = si.getDescription();
            this._name = si.getName();
            this._price = si.getPrice();
            this._productType = si.getProductType();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ShoppingItem(String name, String description, double price, ProductType type) {
        this._price = price;
        this._name = name;
        this._description = description;
        this._productType = type;
    }

    public double getPrice() {
        return _price;
    }

    public void setPrice(double price) {
        this._price = price;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String description) {
        this._description = description;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        this._name = name;
    }

    public ProductType getProductType() {
        return _productType;
    }

    public void setProductType(ProductType productType) {
        this._productType = productType;
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

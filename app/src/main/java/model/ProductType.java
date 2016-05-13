package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class ProductType {
    private int id;
    private String name;

    public ProductType(){}

//    public ProductType(String json){
//        ObjectMapper mapper = new ObjectMapper();
//        try {
//            ProductType pt = mapper.readValue(json, ProductType.class);
//            this.id = pt.getID();
//            this.name = pt.getName();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public ProductType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

//    @Override
//    public String toString() {
//        ObjectMapper mapper = new ObjectMapper();
//        try {
//            return mapper.writeValueAsString(this);
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//            return "PARSING ERROR";
//        }
//    }
}

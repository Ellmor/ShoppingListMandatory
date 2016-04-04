package model;

public class ProductType {
    private int id;
    private String name;

    public ProductType(String name) {
        this.id = -1;
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
}

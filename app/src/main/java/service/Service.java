package service;

import java.util.ArrayList;

import model.ProductType;
import model.ShoppingItem;

public class Service {
    private static Service instance;

    ArrayList<ShoppingItem> items,deleted;

    ArrayList<ProductType> productTypes;

    private Service() {
        //Initialize
        items = new ArrayList<>();
        deleted = new ArrayList<>();
        productTypes = new ArrayList<>();
        //Insert data
        //ProductTypes
        productTypes.add(new ProductType("Fresh food"));
        productTypes.add(new ProductType("Bakery"));
        productTypes.add(new ProductType("Frozen food"));
        productTypes.add(new ProductType("Drinks"));
        productTypes.add(new ProductType("Milk products"));
        productTypes.add(new ProductType("Meat"));
        productTypes.add(new ProductType("Utilities"));
        productTypes.add(new ProductType("Veg&Fruit"));
        //Items
        items.add(new ShoppingItem("Some item 1", "This is some small item", 20.0, productTypes.get(0)));
        items.add(new ShoppingItem("Some item 2", "This is some other item", 2.95, productTypes.get(0)));
    }

    public static Service getInstance() {
        if (instance == null)
            instance = new Service();
        return instance;
    }

    public ProductType getProductType(String name) {
        ProductType pt = null;
        for (ProductType p :
                productTypes) {
            if (p.getName().contains(name)) {
                pt = p;
                break;
            }
        }
        return pt;
    }

    public ArrayList<ShoppingItem> getItems() {
        return items;
    }
}

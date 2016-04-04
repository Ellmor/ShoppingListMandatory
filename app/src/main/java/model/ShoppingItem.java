package model;

public class ShoppingItem {
    private double _price;
    private String _name;
    private String _description;
    private ProductType _productType;

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
}

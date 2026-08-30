package com.example.finalproject.ui.Cart;

import com.google.firebase.database.IgnoreExtraProperties;


@IgnoreExtraProperties
public class CartItem {


    private String productKey;


    private String nameProduct;


    private String category;


    private int price;


    private int quantity;
    private double lineTotal;
    private double saved;
    public CartItem() {}


    public CartItem(String productKey, String nameProduct, String category, int price, int quantity) {
        this.productKey = productKey;
        this.nameProduct = nameProduct;
        this.category = category;
        this.price = price;
        this.quantity = Math.max(0, quantity);
    }



    public String getProductKey() { return productKey; }
    public void setProductKey(String productKey) { this.productKey = productKey; }

    public String getNameProduct() { return nameProduct; }
    public void setNameProduct(String nameProduct) { this.nameProduct = nameProduct; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = Math.max(0, price); }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = Math.max(0, quantity); }


    @Deprecated
    public int getAmount() { return getQuantity(); }


    @Deprecated
    public void setAmount(int amount) { setQuantity(amount); }




    public double getLineTotal() { return lineTotal; }
    public void setLineTotal(double lineTotal) { this.lineTotal = lineTotal; }

    public double getSaved() { return saved; }
    public void setSaved(double saved) { this.saved = saved; }


    public void addToQuantity(int delta) {
        setQuantity(this.quantity + delta);
    }

    @Override
    public String toString() {
        return "CartItem{key=" + productKey + ", name=" + nameProduct +
                ", price=" + price + ", qty=" + quantity + "}";
    }
}

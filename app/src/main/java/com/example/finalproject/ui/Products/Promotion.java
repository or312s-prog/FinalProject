package com.example.finalproject.ui.Products;

public class Promotion {
    private int buyQty;
    private int freeQty;
    private boolean active;
    private int discountPercent;

    public Promotion() {}

    public Promotion(int buyQty, int freeQty, boolean active) {
        this.buyQty = buyQty;
        this.freeQty = freeQty;
        this.active = active;
        this.discountPercent = 0;
    }

    public Promotion(int buyQty, int freeQty, boolean active, int discountPercent) {
        this.buyQty = buyQty;
        this.freeQty = freeQty;
        this.active = active;
        this.discountPercent = discountPercent;
    }

    public int getBuyQty() { return buyQty; }
    public void setBuyQty(int buyQty) { this.buyQty = buyQty; }

    public int getFreeQty() { return freeQty; }
    public void setFreeQty(int freeQty) { this.freeQty = freeQty; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }
}
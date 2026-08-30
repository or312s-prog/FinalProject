package com.example.finalproject.ui.Orders;

public class Orders {

    private String orderId;
    private String orderDate;
    private String totalAmount;
    private String deliveryDate;
    private String deliveryTime;
    private String status;

    public Orders() {}

    public Orders(String orderId,
                  String orderDate,
                  String totalAmount,
                  String deliveryDate,
                  String deliveryTime,
                  String status) {

        this.orderId = orderId;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.deliveryDate = deliveryDate;
        this.deliveryTime = deliveryTime;
        this.status = status;
    }

    public String getOrderId() { return orderId; }
    public String getOrderDate() { return orderDate; }
    public String getTotalAmount() { return totalAmount; }
    public String getDeliveryDate() { return deliveryDate; }
    public String getDeliveryTime() { return deliveryTime; }
    public String getStatus() { return status; }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setDeliveryDate(String deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public void setDeliveryTime(String deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

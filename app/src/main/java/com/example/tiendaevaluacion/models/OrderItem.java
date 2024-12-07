package com.example.tiendaevaluacion.models;

public class OrderItem {
    private int productId;
    private int quantity;
    private double price;
    private String productName;

    public OrderItem(int productId, int quantity, double price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public int getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public double getTotal() { return price * quantity; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
} 
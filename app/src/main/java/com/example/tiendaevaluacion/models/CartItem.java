package com.example.tiendaevaluacion.models;

public class CartItem {
    private int id;
    private int userId;
    private int productId;
    private int quantity;
    private String productName;
    private double price;
    private String size;

    public CartItem(int id, int userId, int productId, int quantity, String productName, double price, String size) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.productName = productName;
        this.price = price;
        this.size = size;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public int getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getProductName() { return productName; }
    public double getPrice() { return price; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public double getTotal() { return price * quantity; }
} 
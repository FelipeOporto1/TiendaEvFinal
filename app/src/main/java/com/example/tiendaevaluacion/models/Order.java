package com.example.tiendaevaluacion.models;

import java.util.Date;
import java.util.List;

public class Order {
    private int id;
    private int userId;
    private List<OrderItem> items;
    private double totalAmount;
    private String status;
    private String deliveryType;
    private String address;
    private Date orderDate;
    private String customerName;

    public Order(int id, int userId, List<OrderItem> items, double totalAmount, String status, 
                String deliveryType, String address, Date orderDate) {
        this.id = id;
        this.userId = userId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
        this.deliveryType = deliveryType;
        this.address = address;
        this.orderDate = orderDate;
    }

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public List<OrderItem> getItems() { return items; }
    public double getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public String getDeliveryType() { return deliveryType; }
    public String getAddress() { return address; }
    public Date getOrderDate() { return orderDate; }
    public String getCustomerName() { return customerName; }

    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
} 
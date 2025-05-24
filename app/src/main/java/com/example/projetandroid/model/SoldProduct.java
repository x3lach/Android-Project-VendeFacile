package com.example.projetandroid.model;

import java.io.Serializable;

public class SoldProduct implements Serializable {

    private String productId;
    private String name;
    private double price;
    private int quantity;
    
    // Constructeur vide requis pour Firebase
    public SoldProduct() {
    }
    
    public SoldProduct(String productId, String name, double price, int quantity) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }
    
    // Getters et setters
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    // Méthode pratique pour calculer le sous-total
    public double getSubtotal() {
        return price * quantity;
    }
}
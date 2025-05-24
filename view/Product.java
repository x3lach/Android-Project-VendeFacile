package com.example.projetandroid.model;

public class Product {
    public String productId;
    public String name;
    public double price;
    public int quantity; // New field for quantity

    public Product() {
        // Constructeur vide requis pour Firebase
    }

    public Product(String productId, String name, double price, int quantity) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    // Existing getters/setters

    // New getter/setter for quantity
    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    // Utile pour afficher le nom du produit dans un Spinner
    @Override
    public String toString() {
        return name;
    }
}
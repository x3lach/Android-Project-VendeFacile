package com.example.projetandroid.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Invoice implements Serializable {
    
    private String invoiceId;
    private String clientName;
    private String clientPhone; // Nouveau champ pour le numéro de téléphone
    private double totalAmount;
    private long timestamp;

    private Map<String, SoldProduct> soldProducts;
    private List<InvoiceItem> items;

    // Ajouter un champ pour stocker les factures groupées
    private List<Invoice> groupedInvoices;

    private double latitude;
    private double longitude;
    private String locationAddress;

    // Default constructor required for Firebase
    public Invoice() {
        soldProducts = new HashMap<>();
        items = new ArrayList<>();
    }

    // Getters et Setters
    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientPhone() { // Getter pour le numéro de téléphone
        return clientPhone;
    }

    public void setClientPhone(String clientPhone) { // Setter pour le numéro de téléphone
        this.clientPhone = clientPhone;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<InvoiceItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public void setItems(List<InvoiceItem> items) {
        this.items = items;
        calculateTotalAmount();
    }

    // Methods for managing invoice items
    public void addItem(Product product, int quantity) {
        if (items == null) {
            items = new ArrayList<>();
        }

        InvoiceItem newItem = new InvoiceItem(
            product.getProductId(),
            product.getName(),
            product.getPrice(),
            quantity
        );

        items.add(newItem);
        calculateTotalAmount();
    }

    public void removeItem(int position) {
        if (items != null && position >= 0 && position < items.size()) {
            items.remove(position);
            calculateTotalAmount();
        }
    }

    private void calculateTotalAmount() {
        double total = 0;
        if (items != null) {
            for (InvoiceItem item : items) {
                total += item.getSubtotal();
            }
        }
        this.totalAmount = total;
    }

    public Map<String, SoldProduct> getSoldProducts() {
        if (soldProducts == null) {
            soldProducts = new HashMap<>();
        }
        return soldProducts;
    }

    public void setSoldProducts(Map<String, SoldProduct> soldProducts) {
        this.soldProducts = soldProducts;
    }

    public List<Invoice> getGroupedInvoices() {
        return groupedInvoices;
    }

    public void setGroupedInvoices(List<Invoice> groupedInvoices) {
        this.groupedInvoices = groupedInvoices;
    }

    // Méthode helper pour savoir si c'est un groupe
    public boolean isGroup() {
        return groupedInvoices != null && !groupedInvoices.isEmpty();
    }

    // Getters et Setters pour la localisation
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public String getLocationAddress() {
        return locationAddress;
    }
    
    public void setLocationAddress(String locationAddress) {
        this.locationAddress = locationAddress;
    }
}
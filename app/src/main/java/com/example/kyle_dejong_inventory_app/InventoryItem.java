package com.example.kyle_dejong_inventory_app;

public class InventoryItem {

    // Stable SQLite row ID used internally for unambiguous update/delete operations.
    private long id;
    private String name;
    private int quantity;
    private String location;

    public InventoryItem(long id, String name, int quantity, String location) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.location = location;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getLocation() {
        return location;
    }
}
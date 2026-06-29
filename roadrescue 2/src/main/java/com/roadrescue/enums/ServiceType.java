package com.roadrescue.enums;

public enum ServiceType {
    FLAT_TIRE("Flat Tire"),
    BATTERY_JUMP("Battery Jump Start"),
    BATTERY_REPLACEMENT("Battery Replacement"),
    ENGINE_REPAIR("Engine Repair"),
    FUEL_DELIVERY("Fuel Delivery"),
    TOWING("Towing"),
    BRAKE_REPAIR("Brake Repair"),
    LOCKOUT("Lockout Assistance"),
    OIL_CHANGE("Oil Change"),
    GENERAL_REPAIR("General Repair");

    private final String displayName;

    ServiceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
package com.roadrescue.enums;

public enum ServiceType {
    FLAT_TIRE("Flat Tire", 500),
    BATTERY_JUMP("Battery Jump Start", 800),
    BATTERY_REPLACEMENT("Battery Replacement", 3500),
    ENGINE_REPAIR("Engine Repair", 5000),
    FUEL_DELIVERY("Fuel Delivery", 600),
    TOWING("Towing", 2500),
    BRAKE_REPAIR("Brake Repair", 3000),
    LOCKOUT("Lockout Assistance", 1000),
    OIL_CHANGE("Oil Change", 1500),
    GENERAL_REPAIR("General Repair", 2000);

    private final String displayName;
    private final int minimumCharge;

    ServiceType(String displayName, int minimumCharge) {
        this.displayName = displayName;
        this.minimumCharge = minimumCharge;
    }

    public String getDisplayName() { return displayName; }
    public int getMinimumCharge() { return minimumCharge; }
}
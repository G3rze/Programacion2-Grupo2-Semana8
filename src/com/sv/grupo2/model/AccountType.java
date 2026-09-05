package com.sv.grupo2.model;

public enum AccountType {
    AHORROS("Ahorros"),
    CORRIENTE("Corriente");

    private final String displayName;

    AccountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

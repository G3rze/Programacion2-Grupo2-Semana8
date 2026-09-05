package com.sv.grupo2.model;

public enum AccountStatus {
    ACTIVA("Activa"),
    BLOQUEADA("Bloqueada"),
    INACTIVA("Inactiva");

    private final String displayName;

    AccountStatus(String displayName) {
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

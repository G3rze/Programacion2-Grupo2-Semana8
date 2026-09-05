package com.sv.grupo2.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Customer implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String customerId; // DUI o Documento de Identidad
    private String fullName;
    private String email;
    private String phone;
    private final List<String> accountIds; // Cuentas pertenecientes al cliente

    public Customer(String customerId, String fullName, String email, String phone) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.accountIds = new ArrayList<>();
    }

    // Getters
    public String getCustomerId() { return customerId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public List<String> getAccountIds() { return Collections.unmodifiableList(accountIds); }

    // Setters (para el Update)
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }

    // Gestión de relación con Cuentas
    public void addAccount(String accountId) {
        if (!accountIds.contains(accountId)) {
            accountIds.add(accountId);
        }
    }

    public void removeAccount(String accountId) {
        accountIds.remove(accountId);
    }

    @Override
    public String toString() {
        return String.format("Cliente [%s] %s | Tel: %s | Email: %s | Cuentas: %s",
                customerId, fullName, phone, email, accountIds);
    }
}
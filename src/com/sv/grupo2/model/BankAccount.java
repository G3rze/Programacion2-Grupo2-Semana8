package com.sv.grupo2.model;

public class BankAccount {
    private final String accountId;
    private String customerId; // Identificador del cliente / titular (DUI)
    private double balance;

    // Constructor con cliente asignado
    public BankAccount(String accountId, String customerId, double initialBalance) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.balance = initialBalance;
    }

    // Constructor sobrecargado (para mantener compatibilidad si no se pasa cliente al inicio)
    public BankAccount(String accountId, double initialBalance) {
        this(accountId, null, initialBalance);
    }

    public String getAccountId() { 
        return accountId; 
    }

    public String getCustomerId() { 
        return customerId; 
    }

    public void setCustomerId(String customerId) { 
        this.customerId = customerId; 
    }

    public synchronized boolean deposit(double amount) {
        if (amount <= 0) return false;
        balance += amount;
        return true;
    }

    public synchronized boolean withdraw(double amount) {
        if (amount <= 0 || balance < amount) return false;
        balance -= amount;
        return true;
    }

    public synchronized double getBalance() {
        return balance;
    }

    @Override
    public synchronized String toString() {
        return String.format("Cuenta [%s] | Titular: %s | Saldo: $%.2f",
                accountId, (customerId != null ? customerId : "Sin asignar"), balance);
    }
}
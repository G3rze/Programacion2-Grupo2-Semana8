package com.sv.grupo2.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BankAccount {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String accountId;
    private final String bankId;
    private final AccountType accountType;
    private final LocalDateTime createdAt;
    private String customerId;
    private AccountStatus status;
    private double balance;
    private double overdraftLimit;
    private double dailyTransferLimit;
    private double transferredToday;

    public BankAccount(String accountId, String bankId, AccountType accountType, double initialBalance,
                       double overdraftLimit, double dailyTransferLimit) {
        if (accountId == null || accountId.trim().isEmpty() || bankId == null || bankId.trim().isEmpty()) {
            throw new IllegalArgumentException("El identificador de cuenta y banco son obligatorios.");
        }
        if (accountType == null || initialBalance <= 0 || overdraftLimit < 0 || dailyTransferLimit < 0) {
            throw new IllegalArgumentException("Los datos de apertura de la cuenta son invalidos.");
        }
        if (accountType == AccountType.AHORROS && overdraftLimit > 0) {
            throw new IllegalArgumentException("Las cuentas de ahorro no admiten sobregiro.");
        }
        this.accountId = accountId.trim().toUpperCase();
        this.bankId = bankId.trim().toUpperCase();
        this.accountType = accountType;
        this.customerId = null;
        this.status = AccountStatus.ACTIVA;
        this.balance = initialBalance;
        this.overdraftLimit = overdraftLimit;
        this.dailyTransferLimit = dailyTransferLimit;
        this.transferredToday = 0.0;
        this.createdAt = LocalDateTime.now();
    }

    public BankAccount(String accountId, String customerId, double initialBalance) {
        this(accountId, "BANCO_A", AccountType.AHORROS, initialBalance, 0.0, 5000.0);
        this.customerId = customerId;
    }

    public BankAccount(String accountId, double initialBalance) {
        this(accountId, "BANCO_A", AccountType.AHORROS, initialBalance, 0.0, 5000.0);
    }

    public String getAccountId() { return accountId; }
    public String getBankId() { return bankId; }
    public AccountType getAccountType() { return accountType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public synchronized String getCustomerId() { return customerId; }
    public synchronized void setCustomerId(String customerId) { this.customerId = customerId; }
    public synchronized AccountStatus getStatus() { return status; }
    public synchronized void setStatus(AccountStatus status) {
        if (status == null) throw new IllegalArgumentException("El estado no puede ser nulo.");
        this.status = status;
    }
    public synchronized double getBalance() { return balance; }
    public synchronized double getOverdraftLimit() { return overdraftLimit; }
    public synchronized void setOverdraftLimit(double value) {
        if (value < 0 || (accountType == AccountType.AHORROS && value > 0)) {
            throw new IllegalArgumentException("Limite de sobregiro invalido para esta cuenta.");
        }
        overdraftLimit = value;
    }
    public synchronized double getDailyTransferLimit() { return dailyTransferLimit; }
    public synchronized void setDailyTransferLimit(double value) {
        if (value < 0) throw new IllegalArgumentException("El limite diario no puede ser negativo.");
        dailyTransferLimit = value;
    }
    public synchronized double getTransferredToday() { return transferredToday; }
    public synchronized double getAvailableFunds() {
        return accountType == AccountType.CORRIENTE ? balance + overdraftLimit : balance;
    }
    public synchronized boolean deposit(double amount) {
        if (amount <= 0 || status == AccountStatus.INACTIVA) return false;
        balance += amount;
        return true;
    }
    public synchronized boolean withdraw(double amount) {
        if (amount <= 0 || status != AccountStatus.ACTIVA || getAvailableFunds() < amount) return false;
        balance -= amount;
        return true;
    }
    public synchronized boolean canInitiateTransfer(double amount) {
        return amount > 0 && status == AccountStatus.ACTIVA
                && (dailyTransferLimit <= 0 || transferredToday + amount <= dailyTransferLimit)
                && getAvailableFunds() >= amount;
    }
    public synchronized boolean executeTransferDebit(double amount) {
        if (!canInitiateTransfer(amount)) return false;
        balance -= amount;
        transferredToday += amount;
        return true;
    }
    public synchronized void rollbackTransferDebit(double amount) {
        balance += amount;
        transferredToday = Math.max(0.0, transferredToday - amount);
    }
    @Override
    public synchronized String toString() {
        return String.format("[%s | %s] %s (%s) | Titular: %s | Saldo: $%.2f | Disp: $%.2f | Estado: %s",
                bankId, accountId, accountType, createdAt.format(FORMATTER),
                customerId != null ? customerId : "Sin asignar", balance, getAvailableFunds(), status);
    }
    public synchronized String getDetailedInfo() {
        return String.format("ID de Cuenta: %s%nBanco: %s%nTipo de Cuenta: %s%nTitular: %s%nEstado: %s%nSaldo Actual: $%.2f%nLímite Sobregiro: $%.2f%nFondos Disponibles: $%.2f%nLímite Diario: $%.2f%nTransferido Hoy: $%.2f%nFecha de Apertura: %s",
                accountId, bankId, accountType, customerId != null ? customerId : "Sin asignar", status,
                balance, overdraftLimit, getAvailableFunds(), dailyTransferLimit, transferredToday,
                createdAt.format(FORMATTER));
    }
}

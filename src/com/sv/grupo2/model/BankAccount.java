package com.sv.grupo2.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BankAccount {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String accountId;
    private final String bankId;
    private final AccountType accountType;
    private final LocalDateTime createdAt;

    private AccountStatus status;
    private double balance;
    private double overdraftLimit;
    private double dailyTransferLimit;
    private double transferredToday;

    public BankAccount(String accountId, String bankId, AccountType accountType,
                       double initialBalance, double overdraftLimit, double dailyTransferLimit) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("El identificador de cuenta (accountId) no puede estar vacío.");
        }
        if (bankId == null || bankId.trim().isEmpty()) {
            throw new IllegalArgumentException("El banco perteneciente (bankId) no puede estar vacío.");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("El tipo de cuenta no puede ser nulo.");
        }
        if (initialBalance <= 0) {
            throw new IllegalArgumentException(String.format(
                    "Regla de apertura violada: El saldo inicial debe ser estrictamente mayor a 0 ($> 0). Monto recibido: $%.2f",
                    initialBalance));
        }
        if (overdraftLimit < 0) {
            throw new IllegalArgumentException("El límite de sobregiro no puede ser negativo.");
        }
        if (accountType == AccountType.AHORROS && overdraftLimit > 0) {
            throw new IllegalArgumentException("Las cuentas de tipo AHORROS no admiten límite de sobregiro.");
        }
        if (dailyTransferLimit < 0) {
            throw new IllegalArgumentException("El límite diario de transferencias no puede ser negativo.");
        }

        this.accountId = accountId.trim().toUpperCase();
        this.bankId = bankId.trim().toUpperCase();
        this.accountType = accountType;
        this.status = AccountStatus.ACTIVA;
        this.balance = initialBalance;
        this.overdraftLimit = overdraftLimit;
        this.dailyTransferLimit = dailyTransferLimit;
        this.transferredToday = 0.0;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor de compatibilidad para código previo.
     */
    public BankAccount(String accountId, double initialBalance) {
        this(accountId, "BANCO_A", AccountType.AHORROS, initialBalance, 0.0, 5000.0);
    }

    public String getAccountId() {
        return accountId;
    }

    public String getBankId() {
        return bankId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public synchronized AccountStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(AccountStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("El estado de la cuenta no puede ser nulo.");
        }
        this.status = status;
    }

    public synchronized double getBalance() {
        return balance;
    }

    public synchronized double getOverdraftLimit() {
        return overdraftLimit;
    }

    public synchronized void setOverdraftLimit(double overdraftLimit) {
        if (overdraftLimit < 0) {
            throw new IllegalArgumentException("El límite de sobregiro no puede ser negativo.");
        }
        if (accountType == AccountType.AHORROS && overdraftLimit > 0) {
            throw new IllegalArgumentException("Las cuentas de tipo AHORROS no permiten límite de sobregiro.");
        }
        this.overdraftLimit = overdraftLimit;
    }

    public synchronized double getDailyTransferLimit() {
        return dailyTransferLimit;
    }

    public synchronized void setDailyTransferLimit(double dailyTransferLimit) {
        if (dailyTransferLimit < 0) {
            throw new IllegalArgumentException("El límite diario no puede ser negativo.");
        }
        this.dailyTransferLimit = dailyTransferLimit;
    }

    public synchronized double getTransferredToday() {
        return transferredToday;
    }

    public synchronized double getAvailableFunds() {
        if (accountType == AccountType.CORRIENTE) {
            return balance + overdraftLimit;
        }
        return balance;
    }

    public synchronized boolean deposit(double amount) {
        if (amount <= 0) {
            return false;
        }
        if (status == AccountStatus.INACTIVA) {
            return false;
        }
        balance += amount;
        return true;
    }

    public synchronized boolean withdraw(double amount) {
        if (amount <= 0) {
            return false;
        }
        // Las cuentas bloqueadas e inactivas no permiten retiros
        if (status != AccountStatus.ACTIVA) {
            return false;
        }

        double available = getAvailableFunds();
        if (available < amount) {
            return false;
        }

        balance -= amount;
        return true;
    }

    public synchronized boolean canInitiateTransfer(double amount) {
        if (amount <= 0) return false;
        if (status != AccountStatus.ACTIVA) return false;
        if (dailyTransferLimit > 0 && (transferredToday + amount > dailyTransferLimit)) return false;
        return getAvailableFunds() >= amount;
    }

    public synchronized boolean executeTransferDebit(double amount) {
        if (!canInitiateTransfer(amount)) {
            return false;
        }
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
        return String.format("[%s | %s] %s (%s) | Saldo: $%.2f | Disp: $%.2f | Estado: %s",
                bankId, accountId, accountType, createdAt.format(FORMATTER),
                balance, getAvailableFunds(), status);
    }

    public synchronized String getDetailedInfo() {
        return String.format(
                "╔══════════════════════════════════════════════════════════════╗%n" +
                "║                   DETALLE DE CUENTA BANCARIA                 ║%n" +
                "╠══════════════════════════════════════════════════════════════╣%n" +
                "  ID de Cuenta        : %s%n" +
                "  Banco               : %s%n" +
                "  Tipo de Cuenta      : %s%n" +
                "  Estado              : %s%n" +
                "  Saldo Actual        : $%.2f%n" +
                "  Límite Sobregiro    : $%.2f%n" +
                "  Fondos Disponibles  : $%.2f%n" +
                "  Límite Diario Trans : $%.2f%n" +
                "  Transferido Hoy     : $%.2f%n" +
                "  Fecha de Apertura   : %s%n" +
                "╚══════════════════════════════════════════════════════════════╝",
                accountId, bankId, accountType, status,
                balance, overdraftLimit, getAvailableFunds(),
                dailyTransferLimit, transferredToday,
                createdAt.format(FORMATTER));
    }
}
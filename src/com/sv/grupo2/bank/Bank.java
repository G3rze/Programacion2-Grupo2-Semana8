package com.sv.grupo2.bank;

import com.sv.grupo2.model.AccountStatus;
import com.sv.grupo2.model.AccountType;
import com.sv.grupo2.model.BankAccount;
import com.sv.grupo2.model.Receipt;
import com.sv.grupo2.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Bank {

    private final String bankId;
    private final ConcurrentHashMap<String, BankAccount> accounts;
    private final ExecutorService executor;

    public Bank(String bankId, int poolSize) {
        this.bankId = bankId;
        this.accounts = new ConcurrentHashMap<>();
        this.executor = Executors.newFixedThreadPool(poolSize);
    }

    public String getBankId() {
        return bankId;
    }

    /**
     * Abre una nueva cuenta bancaria en tiempo de ejecución validando unicidad de ID y saldo inicial > 0.
     */
    public BankAccount openAccount(String accountId, AccountType type, double initialBalance,
                                   double overdraftLimit, double dailyTransferLimit) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID de cuenta no puede ser nulo ni vacío.");
        }
        String cleanId = accountId.trim().toUpperCase();
        if (accounts.containsKey(cleanId)) {
            throw new IllegalArgumentException(String.format(
                    "El ID de cuenta '%s' ya existe en el banco %s.", cleanId, bankId));
        }

        BankAccount newAccount = new BankAccount(cleanId, bankId, type, initialBalance,
                overdraftLimit, dailyTransferLimit);
        accounts.put(cleanId, newAccount);
        return newAccount;
    }

    /**
     * Método heredado para compatibilidad.
     */
    public void createAccount(String accountId, double initialBalance) {
        openAccount(accountId, AccountType.AHORROS, initialBalance, 0.0, 5000.0);
    }

    public BankAccount getAccount(String accountId) {
        if (accountId == null) return null;
        return accounts.get(accountId.trim().toUpperCase());
    }

    public boolean hasAccount(String accountId) {
        if (accountId == null) return false;
        return accounts.containsKey(accountId.trim().toUpperCase());
    }

    /**
     * Lista todas las cuentas activas del banco.
     */
    public List<BankAccount> getActiveAccounts() {
        List<BankAccount> list = new ArrayList<>();
        for (BankAccount acc : accounts.values()) {
            if (acc.getStatus() == AccountStatus.ACTIVA) {
                list.add(acc);
            }
        }
        return list;
    }

    /**
     * Retorna todas las cuentas del banco (activas, bloqueadas e inactivas).
     */
    public List<BankAccount> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }

    /**
     * Actualiza el estado de una cuenta bancaria.
     */
    public boolean updateAccountStatus(String accountId, AccountStatus newStatus) {
        BankAccount acc = getAccount(accountId);
        if (acc == null) {
            return false;
        }
        acc.setStatus(newStatus);
        return true;
    }

    /**
     * Actualiza el límite de sobregiro (para cuentas corrientes).
     */
    public boolean updateOverdraftLimit(String accountId, double newLimit) {
        BankAccount acc = getAccount(accountId);
        if (acc == null) {
            return false;
        }
        acc.setOverdraftLimit(newLimit);
        return true;
    }

    /**
     * Actualiza el límite diario de transferencias.
     */
    public boolean updateDailyTransferLimit(String accountId, double newLimit) {
        BankAccount acc = getAccount(accountId);
        if (acc == null) {
            return false;
        }
        acc.setDailyTransferLimit(newLimit);
        return true;
    }

    /**
     * Cierre / Cancelación de cuenta.
     * Regla de negocio bancaria: No se puede eliminar una cuenta si tiene saldo pendiente (> 0).
     * Primero debe retirarse o transferirse todo el dinero a otra cuenta.
     */
    public synchronized boolean closeAccount(String accountId) {
        BankAccount acc = getAccount(accountId);
        if (acc == null) {
            throw new IllegalArgumentException("La cuenta no existe en el banco " + bankId + ".");
        }

        synchronized (acc) {
            double currentBalance = acc.getBalance();
            if (currentBalance > 0) {
                throw new IllegalStateException(String.format(
                        "REGLA DE NEGOCIO VIOLADA: No se puede cerrar la cuenta %s porque tiene saldo pendiente ($%.2f).%n" +
                        "Primero debe retirarse o transferirse todo el dinero a otra cuenta.",
                        accountId, currentBalance));
            }
            if (currentBalance < 0) {
                throw new IllegalStateException(String.format(
                        "REGLA DE NEGOCIO VIOLADA: La cuenta %s tiene saldo deudor de sobregiro ($%.2f). Debe saldarse antes del cierre.",
                        accountId, currentBalance));
            }

            acc.setStatus(AccountStatus.INACTIVA);
            accounts.remove(acc.getAccountId());
            return true;
        }
    }

    public Future<Receipt> submitTransaction(Transaction transaction) {
        return executor.submit(transaction);
    }

    public double getTotalBalance() {
        double total = 0;
        for (BankAccount acc : accounts.values()) {
            total += acc.getBalance();
        }
        return total;
    }

    public void shutdown() {
        executor.shutdown();
    }

    public void printAccounts() {
        System.out.printf("[BANCO %s] Catálogo de Cuentas:%n", bankId);
        if (accounts.isEmpty()) {
            System.out.println("  (No hay cuentas registradas)");
            return;
        }
        for (BankAccount acc : accounts.values()) {
            System.out.printf("  %s | %-9s | Saldo: $%9.2f | Disp: $%9.2f | Estado: %-9s%n",
                    acc.getAccountId(), acc.getAccountType(), acc.getBalance(),
                    acc.getAvailableFunds(), acc.getStatus());
        }
    }
}
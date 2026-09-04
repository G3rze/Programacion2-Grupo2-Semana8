package com.sv.grupo2.bank;

import com.sv.grupo2.model.BankAccount;
import com.sv.grupo2.model.Receipt;
import com.sv.grupo2.transaction.Transaction;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Bank {

    private final String bankId;
    private final HashMap<String, BankAccount> accounts;
    private final ExecutorService executor;

    public Bank(String bankId, int poolSize) {
        this.bankId = bankId;
        this.accounts = new HashMap<>();
        this.executor = Executors.newFixedThreadPool(poolSize);
    }

    public String getBankId() { return bankId; }

    public void createAccount(String accountId, double initialBalance) {
        accounts.put(accountId, new BankAccount(accountId, initialBalance));
    }

    public BankAccount getAccount(String accountId) {
        return accounts.get(accountId);
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
        System.out.printf("[BANCO %s] Cuentas:%n", bankId);
        for (BankAccount acc : accounts.values()) {
            System.out.printf("  %s -> $%.2f%n", acc.getAccountId(), acc.getBalance());
        }
    }
}
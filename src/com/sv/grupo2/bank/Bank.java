package com.sv.grupo2.bank;

import com.sv.grupo2.model.AccountStatus;
import com.sv.grupo2.model.AccountType;
import com.sv.grupo2.model.BankAccount;
import com.sv.grupo2.model.Customer;
import com.sv.grupo2.model.Receipt;
import com.sv.grupo2.transaction.Transaction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Bank {
    private final String bankId;
    private final ConcurrentHashMap<String, BankAccount> accounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Customer> customers = new ConcurrentHashMap<>();
    private final ExecutorService executor;

    public Bank(String bankId, int poolSize) {
        this.bankId = bankId;
        this.executor = Executors.newFixedThreadPool(poolSize);
    }
    public String getBankId() { return bankId; }

    public boolean registerCustomer(String id, String name, String email, String phone) {
        return customers.putIfAbsent(id, new Customer(id, name, email, phone)) == null;
    }
    public Customer getCustomer(String id) { return customers.get(id); }
    public Map<String, Customer> getCustomers() { return Collections.unmodifiableMap(customers); }
    public void printCustomers() {
        System.out.printf("%n=== CLIENTES DEL BANCO %s ===%n", bankId);
        if (customers.isEmpty()) { System.out.println("  (No hay clientes registrados)"); return; }
        for (Customer customer : customers.values()) System.out.println("  " + customer);
    }
    public boolean updateCustomerContact(String id, String email, String phone) {
        Customer customer = customers.get(id);
        if (customer == null) return false;
        customer.setEmail(email);
        customer.setPhone(phone);
        return true;
    }
    public boolean deleteCustomer(String id) {
        Customer customer = customers.get(id);
        if (customer == null) return false;
        for (String accountId : customer.getAccountIds()) {
            BankAccount account = accounts.get(accountId);
            if (account != null && account.getBalance() > 0) return false;
        }
        return customers.remove(id, customer);
    }
    public boolean assignAccountToCustomer(String accountId, String customerId) {
        BankAccount account = accounts.get(accountId);
        Customer customer = customers.get(customerId);
        if (account == null || customer == null) return false;
        account.setCustomerId(customerId);
        customer.addAccount(accountId);
        return true;
    }

    public BankAccount openAccount(String accountId, AccountType type, double initialBalance,
                                   double overdraftLimit, double dailyTransferLimit) {
        if (accountId == null || accountId.trim().isEmpty()) throw new IllegalArgumentException("El ID de cuenta es obligatorio.");
        String cleanId = accountId.trim().toUpperCase();
        BankAccount account = new BankAccount(cleanId, bankId, type, initialBalance, overdraftLimit, dailyTransferLimit);
        if (accounts.putIfAbsent(cleanId, account) != null) throw new IllegalArgumentException("El ID de cuenta ya existe.");
        return account;
    }
    public void createAccount(String accountId, String customerId, double initialBalance) {
        BankAccount account = new BankAccount(accountId, bankId, AccountType.AHORROS, initialBalance, 0.0, 5000.0);
        account.setCustomerId(customerId);
        accounts.put(account.getAccountId(), account);
        Customer customer = customers.get(customerId);
        if (customer != null) customer.addAccount(account.getAccountId());
    }
    public void createAccount(String accountId, double initialBalance) { createAccount(accountId, null, initialBalance); }
    public BankAccount getAccount(String id) { return id == null ? null : accounts.get(id.trim().toUpperCase()); }
    public boolean hasAccount(String id) { return getAccount(id) != null; }
    public List<BankAccount> getActiveAccounts() {
        List<BankAccount> result = new ArrayList<>();
        for (BankAccount account : accounts.values()) if (account.getStatus() == AccountStatus.ACTIVA) result.add(account);
        return result;
    }
    public List<BankAccount> getAllAccounts() { return new ArrayList<>(accounts.values()); }
    public boolean updateAccountStatus(String id, AccountStatus status) { BankAccount a = getAccount(id); if (a == null) return false; a.setStatus(status); return true; }
    public boolean updateOverdraftLimit(String id, double value) { BankAccount a = getAccount(id); if (a == null) return false; a.setOverdraftLimit(value); return true; }
    public boolean updateDailyTransferLimit(String id, double value) { BankAccount a = getAccount(id); if (a == null) return false; a.setDailyTransferLimit(value); return true; }
    public synchronized boolean closeAccount(String id) {
        BankAccount account = getAccount(id);
        if (account == null) throw new IllegalArgumentException("La cuenta no existe en el banco " + bankId + ".");
        if (account.getBalance() != 0) throw new IllegalStateException("La cuenta tiene saldo pendiente; debe quedar en cero antes del cierre.");
        account.setStatus(AccountStatus.INACTIVA);
        accounts.remove(account.getAccountId(), account);
        return true;
    }
    public Future<Receipt> submitTransaction(Transaction transaction) { return executor.submit(transaction); }
    public double getTotalBalance() { double total = 0; for (BankAccount a : accounts.values()) total += a.getBalance(); return total; }
    public void shutdown() { executor.shutdown(); }
    public void printAccounts() {
        System.out.printf("[BANCO %s] Catalogo de Cuentas:%n", bankId);
        for (BankAccount account : accounts.values()) System.out.printf("  %s | %-9s | Saldo: $%9.2f | Disp: $%9.2f | Estado: %-9s%n", account.getAccountId(), account.getAccountType(), account.getBalance(), account.getAvailableFunds(), account.getStatus());
    }
}

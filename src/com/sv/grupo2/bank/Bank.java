package com.sv.grupo2.bank;

import com.sv.grupo2.model.BankAccount;
import com.sv.grupo2.model.Customer;
import com.sv.grupo2.model.Receipt;
import com.sv.grupo2.transaction.Transaction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Bank {

    private final String bankId;
    private final HashMap<String, BankAccount> accounts;
    private final HashMap<String, Customer> customers; // Colección de Clientes
    private final ExecutorService executor;

    public Bank(String bankId, int poolSize) {
        this.bankId = bankId;
        this.accounts = new HashMap<>();
        this.customers = new HashMap<>();
        this.executor = Executors.newFixedThreadPool(poolSize);
    }

    public String getBankId() { 
        return bankId; 
    }

    // =========================================================================
    //  CRUD DE CLIENTES (CUSTOMERS)
    // =========================================================================

    // CREATE: Registrar un nuevo cliente
    public boolean registerCustomer(String customerId, String fullName, String email, String phone) {
        if (customers.containsKey(customerId)) {
            System.out.printf("[BANCO %s | ERROR] Cliente con ID %s ya existe.%n", bankId, customerId);
            return false;
        }
        customers.put(customerId, new Customer(customerId, fullName, email, phone));
        System.out.printf("[BANCO %s | CLIENTE CREADO] %s (%s) registrado exitosamente.%n", 
                bankId, fullName, customerId);
        return true;
    }

    // READ: Obtener un cliente específico
    public Customer getCustomer(String customerId) {
        return customers.get(customerId);
    }

    // READ: Obtener mapa de todos los clientes (solo lectura)
    public Map<String, Customer> getCustomers() {
        return Collections.unmodifiableMap(customers);
    }

    // READ: Imprimir lista de clientes
    public void printCustomers() {
        System.out.printf("%n=== CLIENTES DEL BANCO %s ===%n", bankId);
        if (customers.isEmpty()) {
            System.out.println("  (No hay clientes registrados)");
            return;
        }
        for (Customer c : customers.values()) {
            System.out.println("  " + c);
        }
    }

    // UPDATE: Modificar datos de contacto de un cliente
    public boolean updateCustomerContact(String customerId, String newEmail, String newPhone) {
        Customer c = customers.get(customerId);
        if (c == null) {
            System.out.printf("[BANCO %s | ERROR] No se encontró cliente %s para actualizar.%n", bankId, customerId);
            return false;
        }
        c.setEmail(newEmail);
        c.setPhone(newPhone);
        System.out.printf("[BANCO %s | CLIENTE ACTUALIZADO] Datos de %s modificados.%n", bankId, customerId);
        return true;
    }

    // DELETE: Dar de baja a un cliente (Regla: no debe tener cuentas con saldo > 0)
    public boolean deleteCustomer(String customerId) {
        Customer c = customers.get(customerId);
        if (c == null) {
            System.out.printf("[BANCO %s | ERROR] No existe el cliente %s a eliminar.%n", bankId, customerId);
            return false;
        }

        // Validación de regla de negocio
        for (String accId : c.getAccountIds()) {
            BankAccount acc = accounts.get(accId);
            if (acc != null && acc.getBalance() > 0) {
                System.out.printf("[BANCO %s | DENEGADO] No se puede eliminar a %s: La cuenta %s aún tiene saldo $%.2f%n",
                        bankId, customerId, accId, acc.getBalance());
                return false;
            }
        }

        customers.remove(customerId);
        System.out.printf("[BANCO %s | CLIENTE ELIMINADO] Cliente %s dado de baja correctamente.%n", bankId, customerId);
        return true;
    }

    // Asignar una cuenta existente a un cliente
    public boolean assignAccountToCustomer(String accountId, String customerId) {
        BankAccount acc = accounts.get(accountId);
        Customer cust = customers.get(customerId);

        if (acc == null || cust == null) {
            return false;
        }

        acc.setCustomerId(customerId);
        cust.addAccount(accountId);
        return true;
    }

    // =========================================================================
    //  GESTIÓN DE CUENTAS Y TRANSACCIONES
    // =========================================================================

    // Crear cuenta con titular asignado
    public void createAccount(String accountId, String customerId, double initialBalance) {
        BankAccount acc = new BankAccount(accountId, customerId, initialBalance);
        accounts.put(accountId, acc);

        // Si el cliente existe, le agregamos la cuenta a su lista
        Customer cust = customers.get(customerId);
        if (cust != null) {
            cust.addAccount(accountId);
        }
    }

    // Sobrecarga de creación de cuenta (sin titular al inicio)
    public void createAccount(String accountId, double initialBalance) {
        createAccount(accountId, null, initialBalance);
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
            System.out.printf("  %s -> $%.2f (Titular: %s)%n", 
                    acc.getAccountId(), 
                    acc.getBalance(),
                    acc.getCustomerId() != null ? acc.getCustomerId() : "Sin asignar");
        }
    }
}
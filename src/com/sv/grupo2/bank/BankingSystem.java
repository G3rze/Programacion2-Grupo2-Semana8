package com.sv.grupo2.bank;

import com.sv.grupo2.model.AccountStatus;
import com.sv.grupo2.model.AccountType;
import com.sv.grupo2.model.BankAccount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankingSystem {

    private final Map<String, Bank> banks;

    public BankingSystem(int poolSizePerBank) {
        this.banks = new HashMap<>();
        this.banks.put("BANCO_A", new Bank("BANCO_A", poolSizePerBank));
        this.banks.put("BANCO_B", new Bank("BANCO_B", poolSizePerBank));
    }

    public Bank getBank(String bankId) {
        if (bankId == null) return null;
        return banks.get(bankId.trim().toUpperCase());
    }

    public Map<String, Bank> getBanks() {
        return Collections.unmodifiableMap(banks);
    }

    public Bank getBankA() {
        return banks.get("BANCO_A");
    }

    public Bank getBankB() {
        return banks.get("BANCO_B");
    }

    /**
     * Valida la unicidad global del ID de cuenta en todo el sistema multibanco.
     */
    public boolean accountIdExistsAnywhere(String accountId) {
        if (accountId == null) return false;
        String cleanId = accountId.trim().toUpperCase();
        for (Bank b : banks.values()) {
            if (b.hasAccount(cleanId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Busca la cuenta en cualquier banco del sistema.
     */
    public BankAccount findAccount(String accountId) {
        if (accountId == null) return null;
        String cleanId = accountId.trim().toUpperCase();
        for (Bank b : banks.values()) {
            BankAccount acc = b.getAccount(cleanId);
            if (acc != null) {
                return acc;
            }
        }
        return null;
    }

    /**
     * Encuentra el banco al que pertenece una cuenta.
     */
    public Bank findBankForAccount(String accountId) {
        if (accountId == null) return null;
        String cleanId = accountId.trim().toUpperCase();
        for (Bank b : banks.values()) {
            if (b.hasAccount(cleanId)) {
                return b;
            }
        }
        return null;
    }

    /**
     * Create (Apertura): Registra una cuenta asignando un accountId único en el sistema.
     */
    public BankAccount openAccount(String bankId, String accountId, AccountType type,
                                   double initialBalance, double overdraftLimit, double dailyTransferLimit) {
        if (bankId == null || !banks.containsKey(bankId.trim().toUpperCase())) {
            throw new IllegalArgumentException("El banco '" + bankId + "' no existe en el sistema.");
        }
        String cleanAccountId = accountId.trim().toUpperCase();
        if (accountIdExistsAnywhere(cleanAccountId)) {
            throw new IllegalArgumentException(String.format(
                    "El ID de cuenta '%s' ya existe en el sistema bancario.", cleanAccountId));
        }

        Bank targetBank = banks.get(bankId.trim().toUpperCase());
        return targetBank.openAccount(cleanAccountId, type, initialBalance, overdraftLimit, dailyTransferLimit);
    }

    /**
     * Read (Consulta): Lista cuentas activas filtradas por banco o de todo el sistema.
     */
    public List<BankAccount> listActiveAccounts(String bankFilter) {
        List<BankAccount> result = new ArrayList<>();
        if (bankFilter == null || bankFilter.trim().equalsIgnoreCase("ALL") || bankFilter.trim().equalsIgnoreCase("AMBOS")) {
            for (Bank b : banks.values()) {
                result.addAll(b.getActiveAccounts());
            }
        } else {
            Bank b = getBank(bankFilter);
            if (b != null) {
                result.addAll(b.getActiveAccounts());
            }
        }
        return result;
    }

    /**
     * Read (Consulta): Lista todas las cuentas registradas (activas, bloqueadas, inactivas).
     */
    public List<BankAccount> listAllAccounts(String bankFilter) {
        List<BankAccount> result = new ArrayList<>();
        if (bankFilter == null || bankFilter.trim().equalsIgnoreCase("ALL") || bankFilter.trim().equalsIgnoreCase("AMBOS")) {
            for (Bank b : banks.values()) {
                result.addAll(b.getAllAccounts());
            }
        } else {
            Bank b = getBank(bankFilter);
            if (b != null) {
                result.addAll(b.getAllAccounts());
            }
        }
        return result;
    }

    /**
     * Update (Modificación): Cambia el estado de una cuenta.
     */
    public boolean updateAccountStatus(String accountId, AccountStatus newStatus) {
        Bank b = findBankForAccount(accountId);
        if (b == null) return false;
        return b.updateAccountStatus(accountId, newStatus);
    }

    /**
     * Update (Modificación): Actualiza el límite de sobregiro.
     */
    public boolean updateOverdraftLimit(String accountId, double newLimit) {
        Bank b = findBankForAccount(accountId);
        if (b == null) return false;
        return b.updateOverdraftLimit(accountId, newLimit);
    }

    /**
     * Update (Modificación): Actualiza el límite diario de transferencias.
     */
    public boolean updateDailyTransferLimit(String accountId, double newLimit) {
        Bank b = findBankForAccount(accountId);
        if (b == null) return false;
        return b.updateDailyTransferLimit(accountId, newLimit);
    }

    /**
     * Asistente de vaciado de saldo para cumplir la regla bancaria antes del cierre:
     * Retira la totalidad del saldo en efectivo.
     */
    public double withdrawTotalBalance(String accountId) {
        BankAccount acc = findAccount(accountId);
        if (acc == null) {
            throw new IllegalArgumentException("La cuenta no existe.");
        }
        synchronized (acc) {
            double bal = acc.getBalance();
            if (bal <= 0) {
                return 0.0;
            }
            boolean ok = acc.withdraw(bal);
            if (!ok) {
                throw new IllegalStateException("No se pudo retirar el saldo disponible.");
            }
            return bal;
        }
    }

    /**
     * Asistente de vaciado de saldo para cumplir la regla bancaria antes del cierre:
     * Transfiere la totalidad del saldo a otra cuenta de destino.
     */
    public double transferTotalBalance(String fromAccountId, String toAccountId) {
        if (fromAccountId.equalsIgnoreCase(toAccountId)) {
            throw new IllegalArgumentException("La cuenta origen y destino deben ser distintas.");
        }
        BankAccount src = findAccount(fromAccountId);
        BankAccount dst = findAccount(toAccountId);
        if (src == null || dst == null) {
            throw new IllegalArgumentException("Una de las cuentas indicadas no existe.");
        }

        // Orden de bloqueo para evitar deadlocks
        BankAccount firstLock = src.getAccountId().compareTo(dst.getAccountId()) < 0 ? src : dst;
        BankAccount secondLock = firstLock == src ? dst : src;

        synchronized (firstLock) {
            synchronized (secondLock) {
                double amount = src.getBalance();
                if (amount <= 0) {
                    return 0.0;
                }
                if (!src.withdraw(amount)) {
                    throw new IllegalStateException("No se pudo debitar el saldo de origen.");
                }
                if (!dst.deposit(amount)) {
                    src.deposit(amount); // Rollback
                    throw new IllegalStateException("No se pudo acreditar el monto a la cuenta destino.");
                }
                return amount;
            }
        }
    }

    /**
     * Delete (Cierre / Cancelación):
     * Regla bancaria: No se puede eliminar una cuenta con saldo pendiente (> 0).
     */
    public boolean closeAccount(String accountId) {
        Bank b = findBankForAccount(accountId);
        if (b == null) {
            throw new IllegalArgumentException("La cuenta " + accountId + " no existe en ningún banco.");
        }
        return b.closeAccount(accountId);
    }

    public double getTotalSystemBalance() {
        double sum = 0.0;
        for (Bank b : banks.values()) {
            sum += b.getTotalBalance();
        }
        return sum;
    }

    public void shutdown() {
        for (Bank b : banks.values()) {
            b.shutdown();
        }
    }
}

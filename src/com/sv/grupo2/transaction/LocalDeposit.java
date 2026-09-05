package com.sv.grupo2.transaction;

import com.sv.grupo2.bank.Bank;
import com.sv.grupo2.model.AccountStatus;
import com.sv.grupo2.model.BankAccount;
import com.sv.grupo2.model.Receipt;
import com.sv.grupo2.model.TransactionStatus;

public class LocalDeposit extends Transaction {

    public LocalDeposit(String accountId, double amount, Bank bank) {
        super(accountId, amount, bank);
    }

    @Override
    public Receipt call() {
        String threadName = Thread.currentThread().getName();
        System.out.printf("[LOG] %s | INICIO  | LocalDeposit   | cuenta=%s | monto=%.2f%n",
                threadName, accountId, amount);

        BankAccount acc = bank.getAccount(accountId);
        if (acc == null) {
            Receipt r = new Receipt("DEPOSIT", accountId, null, amount,
                    TransactionStatus.FAILED, "Cuenta no encontrada");
            saveReceipt(r);
            System.out.printf("[LOG] %s | FIN     | LocalDeposit   | %s | FALLO: cuenta inexistente%n",
                    threadName, accountId);
            return r;
        }

        TransactionStatus status;
        String msg;
        boolean ok = false;

        synchronized (acc) {
            if (acc.getStatus() == AccountStatus.INACTIVA) {
                status = TransactionStatus.FAILED;
                msg = "Cuenta inactiva: depósitos no permitidos";
            } else {
                ok = acc.deposit(amount);
                status = ok ? TransactionStatus.SUCCESS : TransactionStatus.FAILED;
                msg = ok ? "Deposito exitoso" : "Monto invalido";
            }
        }

        if (ok) {
            System.out.printf("[LOG] %s | FIN     | LocalDeposit   | cuenta=%s | EXITO | saldo=%.2f%n",
                    threadName, accountId, acc.getBalance());
        } else {
            System.out.printf("[LOG] %s | FIN     | LocalDeposit   | cuenta=%s | FALLO: %s%n",
                    threadName, accountId, msg);
        }

        Receipt r = new Receipt("DEPOSIT", accountId, null, amount, status, msg);
        saveReceipt(r);
        return r;
    }
}
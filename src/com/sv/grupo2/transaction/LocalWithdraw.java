package com.sv.grupo2.transaction;

import com.sv.grupo2.bank.Bank;
import com.sv.grupo2.model.BankAccount;
import com.sv.grupo2.model.Receipt;
import com.sv.grupo2.model.TransactionStatus;

public class LocalWithdraw extends Transaction {

    public LocalWithdraw(String accountId, double amount, Bank bank) {
        super(accountId, amount, bank);
    }

    @Override
    public Receipt call() {
        String threadName = Thread.currentThread().getName();
        System.out.printf("[LOG] %s | INICIO  | LocalWithdraw  | cuenta=%s | monto=%.2f%n",
                threadName, accountId, amount);

        BankAccount acc = bank.getAccount(accountId);
        if (acc == null) {
            Receipt r = new Receipt("WITHDRAW", accountId, null, amount,
                    TransactionStatus.FAILED, "Cuenta no encontrada");
            saveReceipt(r);
            System.out.printf("[LOG] %s | FIN     | LocalWithdraw  | %s | FALLO: cuenta inexistente%n",
                    threadName, accountId);
            return r;
        }

        boolean ok = acc.withdraw(amount);
        TransactionStatus status = ok ? TransactionStatus.SUCCESS : TransactionStatus.FAILED;
        String msg = ok ? "Retiro exitoso" : "Saldo insuficiente";
        if (ok) {
            System.out.printf("[LOG] %s | FIN     | LocalWithdraw  | cuenta=%s | EXITO | saldo=%.2f%n",
                    threadName, accountId, acc.getBalance());
        } else {
            System.out.printf("[LOG] %s | FIN     | LocalWithdraw  | cuenta=%s | FALLO: saldo=%.2f < monto=%.2f%n",
                    threadName, accountId, acc.getBalance(), amount);
        }

        Receipt r = new Receipt("WITHDRAW", accountId, null, amount, status, msg);
        saveReceipt(r);
        return r;
    }
}
package com.sv.grupo2.transaction;

import com.sv.grupo2.bank.Bank;
import com.sv.grupo2.model.Receipt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.Callable;

public abstract class Transaction implements Callable<Receipt> {
    protected final String accountId;
    protected final double amount;
    protected final Bank bank;

    public Transaction(String accountId, double amount, Bank bank) {
        this.accountId = accountId;
        this.amount = amount;
        this.bank = bank;
    }

    protected void saveReceipt(Receipt receipt) {
        new File("receipts/").mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("receipts/" + receipt.getTransactionId() + ".dat"))) {
            oos.writeObject(receipt);
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to save receipt " + receipt.getTransactionId() + ": " + e.getMessage());
        }
    }
}
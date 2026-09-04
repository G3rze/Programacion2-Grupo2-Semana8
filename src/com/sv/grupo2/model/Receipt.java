package com.sv.grupo2.model;

import java.io.Serializable;
import java.util.UUID;

public class Receipt implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String transactionId;
    private final String type;
    private final String sourceAccount;
    private final String destAccount;
    private final double amount;
    private final TransactionStatus status;
    private final long timestamp;
    private final String message;

    public Receipt(String type, String sourceAccount, String destAccount,
                   double amount, TransactionStatus status, String message) {
        this.transactionId = UUID.randomUUID().toString();
        this.type = type;
        this.sourceAccount = sourceAccount;
        this.destAccount = destAccount;
        this.amount = amount;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
        this.message = message;
    }

    public String getTransactionId() { return transactionId; }
    public String getType() { return type; }
    public String getSourceAccount() { return sourceAccount; }
    public String getDestAccount() { return destAccount; }
    public double getAmount() { return amount; }
    public TransactionStatus getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return String.format("[%d] %s | %s -> %s | $%.2f | %s | %s",
                timestamp, type, sourceAccount,
                destAccount != null ? destAccount : "N/A",
                amount, status, message);
    }
}
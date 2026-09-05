package com.sv.grupo2.transaction;

import com.sv.grupo2.bank.Bank;
import com.sv.grupo2.model.AccountStatus;
import com.sv.grupo2.model.BankAccount;
import com.sv.grupo2.model.Receipt;
import com.sv.grupo2.model.TransactionStatus;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class InterBankTransfer extends Transaction {

    private static final Random RANDOM = new Random();
    private static final double FAILURE_RATE = 0.20;

    private final Bank destBank;
    private final String destAccountId;

    public InterBankTransfer(String sourceAccountId, double amount, Bank sourceBank,
                             Bank destBank, String destAccountId) {
        super(sourceAccountId, amount, sourceBank);
        this.destBank = destBank;
        this.destAccountId = destAccountId;
    }

    @Override
    public Receipt call() {
        String threadName = Thread.currentThread().getName();
        System.out.printf("[LOG] %s | INICIO  | InterBankTransfer | %s -> %s | monto=%.2f%n",
                threadName, accountId, destAccountId, amount);

        BankAccount srcAcc = bank.getAccount(accountId);
        BankAccount dstAcc = destBank.getAccount(destAccountId);

        if (srcAcc == null || dstAcc == null) {
            String msg = (srcAcc == null ? "Cuenta origen no encontrada" : "Cuenta destino no encontrada");
            Receipt r = new Receipt("TRANSFER", accountId, destAccountId, amount,
                    TransactionStatus.FAILED, msg);
            saveReceipt(r);
            System.out.printf("[LOG] %s | FIN     | InterBankTransfer | %s -> %s | FALLO: %s%n",
                    threadName, accountId, destAccountId, msg);
            return r;
        }

        // Validación de estados
        synchronized (srcAcc) {
            if (srcAcc.getStatus() == AccountStatus.BLOQUEADA) {
                Receipt r = new Receipt("TRANSFER", accountId, destAccountId, amount,
                        TransactionStatus.FAILED, "Cuenta origen bloqueada: transferencias no permitidas");
                saveReceipt(r);
                System.out.printf("[LOG] %s | FIN     | InterBankTransfer | %s | FALLO: origen bloqueada%n",
                        threadName, accountId);
                return r;
            }
            if (srcAcc.getStatus() == AccountStatus.INACTIVA) {
                Receipt r = new Receipt("TRANSFER", accountId, destAccountId, amount,
                        TransactionStatus.FAILED, "Cuenta origen inactiva: transferencias no permitidas");
                saveReceipt(r);
                System.out.printf("[LOG] %s | FIN     | InterBankTransfer | %s | FALLO: origen inactiva%n",
                        threadName, accountId);
                return r;
            }
            if (srcAcc.getDailyTransferLimit() > 0 &&
                    (srcAcc.getTransferredToday() + amount > srcAcc.getDailyTransferLimit())) {
                String msg = String.format("Límite diario de transferencias excedido (Acumulado: $%.2f + Monto: $%.2f > Límite: $%.2f)",
                        srcAcc.getTransferredToday(), amount, srcAcc.getDailyTransferLimit());
                Receipt r = new Receipt("TRANSFER", accountId, destAccountId, amount,
                        TransactionStatus.FAILED, msg);
                saveReceipt(r);
                System.out.printf("[LOG] %s | FIN     | InterBankTransfer | %s | FALLO: límite diario excedido%n",
                        threadName, accountId);
                return r;
            }
        }

        synchronized (dstAcc) {
            if (dstAcc.getStatus() == AccountStatus.INACTIVA) {
                Receipt r = new Receipt("TRANSFER", accountId, destAccountId, amount,
                        TransactionStatus.FAILED, "Cuenta destino inactiva");
                saveReceipt(r);
                System.out.printf("[LOG] %s | FIN     | InterBankTransfer | %s | FALLO: destino inactiva%n",
                        threadName, destAccountId);
                return r;
            }
        }

        boolean withdrawn;
        synchronized (srcAcc) {
            withdrawn = srcAcc.executeTransferDebit(amount);
        }

        if (!withdrawn) {
            Receipt r = new Receipt("TRANSFER", accountId, destAccountId, amount,
                    TransactionStatus.FAILED,
                    String.format("Saldo insuficiente en origen: Disp: $%.2f < Monto: $%.2f",
                            srcAcc.getAvailableFunds(), amount));
            saveReceipt(r);
            System.out.printf("[LOG] %s | FIN     | InterBankTransfer | %s -> %s | FALLO: saldo disp=%.2f < monto=%.2f%n",
                    threadName, accountId, destAccountId, srcAcc.getAvailableFunds(), amount);
            return r;
        }

        System.out.printf("[LOG] %s | DEBITO  | InterBankTransfer | %s -> %.2f | saldo origen=%.2f%n",
                threadName, accountId, amount, srcAcc.getBalance());

        boolean simularFallo = RANDOM.nextDouble() < FAILURE_RATE;
        Receipt depositReceipt;

        if (simularFallo) {
            depositReceipt = new Receipt("DEPOSIT", destAccountId, null, amount,
                    TransactionStatus.FAILED, "SIMULACION: Banco destino no respondio");
            System.out.printf("[LOG] %s | SIMFAIL | InterBankTransfer | %s | fallo simulado en destino%n",
                    threadName, destAccountId);
        } else {
            LocalDeposit depositTask = new LocalDeposit(destAccountId, amount, destBank);
            Future<Receipt> future = destBank.submitTransaction(depositTask);

            try {
                depositReceipt = future.get(5, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                depositReceipt = new Receipt("DEPOSIT", destAccountId, null, amount,
                        TransactionStatus.FAILED, "Timeout en deposito destino");
                future.cancel(true);
            } catch (InterruptedException | ExecutionException e) {
                depositReceipt = new Receipt("DEPOSIT", destAccountId, null, amount,
                        TransactionStatus.FAILED, "Error: " + e.getMessage());
            }
        }

        Receipt finalReceipt;
        if (depositReceipt.getStatus() == TransactionStatus.SUCCESS) {
            finalReceipt = new Receipt("TRANSFER", accountId, destAccountId, amount,
                    TransactionStatus.SUCCESS, "Transferencia exitosa");
            System.out.printf("[LOG] %s | CREDITO | InterBankTransfer | %s <- %.2f | saldo destino=%.2f%n",
                    threadName, destAccountId, amount,
                    destBank.getAccount(destAccountId).getBalance());
        } else {
            synchronized (srcAcc) {
                srcAcc.rollbackTransferDebit(amount);
            }
            finalReceipt = new Receipt("TRANSFER", accountId, destAccountId, amount,
                    TransactionStatus.ROLLED_BACK,
                    "Deposito destino fallo - fondos revertidos a origen");
            System.out.printf("[LOG] %s | ROLLBACK| InterBankTransfer | %s | revertido +%.2f | saldo origen=%.2f%n",
                    threadName, accountId, amount, srcAcc.getBalance());
        }

        saveReceipt(finalReceipt);
        System.out.printf("[LOG] %s | FIN     | InterBankTransfer | %s -> %s | %s%n",
                threadName, accountId, destAccountId, finalReceipt.getStatus());
        return finalReceipt;
    }
}
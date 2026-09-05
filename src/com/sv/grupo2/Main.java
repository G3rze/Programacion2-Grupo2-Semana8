package com.sv.grupo2;

import com.sv.grupo2.bank.Bank;
import com.sv.grupo2.model.Customer;
import com.sv.grupo2.model.Receipt;
import com.sv.grupo2.transaction.InterBankTransfer;
import com.sv.grupo2.transaction.LocalDeposit;
import com.sv.grupo2.transaction.LocalWithdraw;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

public class Main {

    private static final Random RANDOM = new Random(42);
    private static final int NUM_CLIENTS = 55;

    public static void main(String[] args) throws Exception {

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║     SISTEMA BANCARIO CONCURRENTE - MULTIBANCO    ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();

        Bank bancoA = new Bank("BANCO_A", 10);
        Bank bancoB = new Bank("BANCO_B", 10);

        // =========================================================================
        // 1. REGISTRO INICIAL DE CLIENTES (CREATE)
        // =========================================================================
        bancoA.registerCustomer("05123456-7", "Carlos Samayoa", "carlos@mail.com", "7111-2222");
        bancoA.registerCustomer("06123456-8", "Génesis Flores", "genesis@mail.com", "7222-3333");
        bancoA.registerCustomer("07123456-9", "Rodrigo Sánchez", "rodrigo@mail.com", "7333-4444");

        bancoB.registerCustomer("08123456-0", "Gerson Bermúdez", "gerson@mail.com", "7444-5555");
        bancoB.registerCustomer("09123456-1", "Jonathan Amaya", "jonathan@mail.com", "7555-6666");

        // =========================================================================
        // 2. CREACIÓN DE CUENTAS VINCULADAS A TITULARES
        // =========================================================================
        bancoA.createAccount("A001", "05123456-7", 5000);
        bancoA.createAccount("A002", "06123456-8", 3000);
        bancoA.createAccount("A003", "07123456-9", 2000);
        bancoA.createAccount("A004", "05123456-7", 10000);
        bancoA.createAccount("A005", null, 1500); // Cuenta empresarial sin titular individual

        bancoB.createAccount("B001", "08123456-0", 4000);
        bancoB.createAccount("B002", "09123456-1", 7000);
        bancoB.createAccount("B003", "08123456-0", 2500);
        bancoB.createAccount("B004", null, 6000);
        bancoB.createAccount("B005", null, 3500);

        double initialTotal = bancoA.getTotalBalance() + bancoB.getTotalBalance();
        System.out.printf("%nSALDO INICIAL TOTAL: $%.2f (Banco A: $%.2f + Banco B: $%.2f)%n%n",
                initialTotal, bancoA.getTotalBalance(), bancoB.getTotalBalance());

        // =========================================================================
        // 3. SIMULACIÓN DE TRANSACCIONES CONCURRENTES (55 OPERACIONES)
        // =========================================================================
        List<Future<Receipt>> futures = new ArrayList<>();
        String[] accountsA = { "A001", "A002", "A003", "A004", "A005" };
        String[] accountsB = { "B001", "B002", "B003", "B004", "B005" };

        for (int i = 0; i < NUM_CLIENTS; i++) {
            double amount = 100 + RANDOM.nextDouble() * 900;
            int opType = RANDOM.nextInt(3);
            Future<Receipt> future;

            switch (opType) {
                case 0: {
                    String acc = accountsA[RANDOM.nextInt(accountsA.length)];
                    LocalWithdraw w = new LocalWithdraw(acc, amount, bancoA);
                    future = bancoA.submitTransaction(w);
                    break;
                }
                case 1: {
                    String acc = accountsB[RANDOM.nextInt(accountsB.length)];
                    LocalDeposit d = new LocalDeposit(acc, amount, bancoB);
                    future = bancoB.submitTransaction(d);
                    break;
                }
                default: {
                    String src;
                    Bank srcBank;
                    String dst;
                    Bank dstBank;
                    if (RANDOM.nextBoolean()) {
                        src = accountsA[RANDOM.nextInt(accountsA.length)];
                        srcBank = bancoA;
                        dst = accountsB[RANDOM.nextInt(accountsB.length)];
                        dstBank = bancoB;
                    } else {
                        src = accountsA[RANDOM.nextInt(accountsA.length)];
                        srcBank = bancoA;
                        dst = accountsA[RANDOM.nextInt(accountsA.length)];
                        dstBank = bancoA;
                    }
                    if (src.equals(dst)) {
                        src = accountsA[0];
                        dst = accountsA[1];
                    }
                    InterBankTransfer t = new InterBankTransfer(src, amount, srcBank, dstBank, dst);
                    future = srcBank.submitTransaction(t);
                    break;
                }
            }
            futures.add(future);

            Thread.sleep(10 + RANDOM.nextInt(40));
        }

        System.out.println();
        System.out.println("═".repeat(58));
        System.out.println("  ESPERANDO RESULTADOS DE " + futures.size() + " TRANSACCIONES...");
        System.out.println("═".repeat(58));
        System.out.println();

        // =========================================================================
        // 4. PROCESAMIENTO DE RECIBOS Y AUDITORÍA CONTABLE
        // =========================================================================
        int success = 0, failed = 0, rolledBack = 0;
        double totalDeposited = 0, totalWithdrawn = 0;
        for (Future<Receipt> f : futures) {
            Receipt r = f.get();
            switch (r.getStatus()) {
                case SUCCESS -> {
                    success++;
                    switch (r.getType()) {
                        case "DEPOSIT" -> totalDeposited += r.getAmount();
                        case "WITHDRAW" -> totalWithdrawn += r.getAmount();
                    }
                }
                case FAILED -> failed++;
                case ROLLED_BACK -> rolledBack++;
            }
        }

        System.out.println();
        System.out.println("═".repeat(58));
        System.out.println("  RESUMEN DE TRANSACCIONES");
        System.out.println("═".repeat(58));
        System.out.printf("  EXITOS       : %d%n", success);
        System.out.printf("  FALLIDOS     : %d%n", failed);
        System.out.printf("  REVERTIDOS   : %d%n", rolledBack);
        System.out.printf("  TOTAL        : %d%n", futures.size());
        System.out.println();

        bancoA.shutdown();
        bancoB.shutdown();

        double finalTotal = bancoA.getTotalBalance() + bancoB.getTotalBalance();
        double expectedTotal = initialTotal + totalDeposited - totalWithdrawn;
        boolean auditPass = Math.abs(finalTotal - expectedTotal) < 0.01;
        System.out.println("═".repeat(58));
        System.out.println("  AUDITORIA FINAL");
        System.out.println("═".repeat(58));
        System.out.printf("  Saldo inicial total    : $%.2f%n", initialTotal);
        System.out.printf("  Total depositado       : $%.2f%n", totalDeposited);
        System.out.printf("  Total retirado         : $%.2f%n", totalWithdrawn);
        System.out.printf("  Saldo esperado         : $%.2f%n", expectedTotal);
        System.out.printf("  Saldo final total      : $%.2f%n", finalTotal);
        System.out.printf("  Diferencia             : $%.2f%n", finalTotal - expectedTotal);
        System.out.println();
        bancoA.printAccounts();
        System.out.println();
        bancoB.printAccounts();
        System.out.println();

        System.out.println("═".repeat(58));
        System.out.println("  La auditoria " + (auditPass ? "PASO ✓" : "FALLO ✗ - inconsistencia detectada") + "!");
        System.out.println("═".repeat(58));

        // =========================================================================
        // 5. DEMOSTRACIÓN COMPLETA DEL CRUD DE CLIENTES (OPERACIONES C-R-U-D)
        // =========================================================================
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║        PRUEBA DE CRUD DE CLIENTES / TITULARES    ║");
        System.out.println("╚══════════════════════════════════════════════════╝");

        // [C] CREATE: Registrar nuevo cliente
        System.out.println("\n--- 1. CREATE: Registrando nuevo cliente de prueba ---");
        bancoA.registerCustomer("09998888-1", "Cliente Temporal", "temporal@mail.com", "7999-8888");

        // [R] READ: Consultar cliente registrado y listar clientes
        System.out.println("\n--- 2. READ: Consultando cliente recién creado ---");
        Customer temporal = bancoA.getCustomer("09998888-1");
        System.out.println("Resultado de búsqueda: " + temporal);
        bancoA.printCustomers();

        // [U] UPDATE: Modificar datos de contacto
        System.out.println("\n--- 3. UPDATE: Modificando correo y teléfono del cliente ---");
        bancoA.updateCustomerContact("09998888-1", "nuevo.correo@empresa.com", "7000-0000");
        System.out.println("Cliente actualizado: " + bancoA.getCustomer("09998888-1"));

        // [D] DELETE: Prueba de regla de negocio (con y sin saldo)
        System.out.println("\n--- 4. DELETE: Demostración de regla de baja de clientes ---");

        // Caso 4.1: Intentar borrar cliente con cuentas que tienen saldo (Carlos Samayoa tiene cuenta A001 y A004)
        System.out.println("-> Intento 1: Eliminar a Carlos Samayoa (Tiene cuentas con dinero):");
        bancoA.deleteCustomer("05123456-7");

        // Caso 4.2: Borrar cliente temporal que no tiene cuentas con saldo
        System.out.println("\n-> Intento 2: Eliminar a Cliente Temporal (Sin cuentas activas):");
        bancoA.deleteCustomer("09998888-1");

        System.out.println("\nLista final de clientes de Banco A:");
        bancoA.printCustomers();
        System.out.println("═".repeat(58));
    }
}
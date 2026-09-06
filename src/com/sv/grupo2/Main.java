package com.sv.grupo2;

import com.sv.grupo2.bank.Bank;
import com.sv.grupo2.bank.BankingSystem;
import com.sv.grupo2.model.AccountStatus;
import com.sv.grupo2.model.AccountType;
import com.sv.grupo2.model.BankAccount;
import com.sv.grupo2.model.Customer;
import com.sv.grupo2.model.Receipt;
import com.sv.grupo2.transaction.InterBankTransfer;
import com.sv.grupo2.transaction.LocalDeposit;
import com.sv.grupo2.transaction.LocalWithdraw;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Future;

public class Main {

    private static final Random RANDOM = new Random(42);
    private static final int NUM_CLIENTS = 40;

    public static void main(String[] args) throws Exception {
        BankingSystem bankingSystem = new BankingSystem(10);
        initializeSeedAccounts(bankingSystem);

        // Si se pasa el argumento --demo o --non-interactive, ejecutamos la demo automatizada
        if (args.length > 0 && (args[0].equalsIgnoreCase("--demo") || args[0].equalsIgnoreCase("--test"))) {
            runAutomatedDemo(bankingSystem);
            bankingSystem.shutdown();
            return;
        }

        runInteractiveMenu(bankingSystem);
        bankingSystem.shutdown();
    }

    /**
     * Inicializa un catálogo inicial dinámico de cuentas para BANCO_A y BANCO_B.
     */
    public static void initializeSeedAccounts(BankingSystem system) {
        system.openAccount("BANCO_A", "A001", AccountType.CORRIENTE, 5000.0, 1000.0, 3000.0);
        system.openAccount("BANCO_A", "A002", AccountType.AHORROS,   3000.0,    0.0, 2000.0);
        system.openAccount("BANCO_A", "A003", AccountType.AHORROS,   2000.0,    0.0, 1500.0);
        system.openAccount("BANCO_A", "A004", AccountType.CORRIENTE, 10000.0, 2000.0, 5000.0);
        system.openAccount("BANCO_A", "A005", AccountType.AHORROS,   1500.0,    0.0, 1000.0);

        system.openAccount("BANCO_B", "B001", AccountType.AHORROS,   4000.0,    0.0, 2500.0);
        system.openAccount("BANCO_B", "B002", AccountType.CORRIENTE, 7000.0, 1500.0, 4000.0);
        system.openAccount("BANCO_B", "B003", AccountType.AHORROS,   2500.0,    0.0, 1500.0);
        system.openAccount("BANCO_B", "B004", AccountType.CORRIENTE, 6000.0, 1000.0, 3000.0);
        system.openAccount("BANCO_B", "B005", AccountType.AHORROS,   3500.0,    0.0, 2000.0);

        Bank bancoA = system.getBankA();
        Bank bancoB = system.getBankB();
        bancoA.registerCustomer("05123456-7", "Carlos Samayoa", "carlos@mail.com", "7111-2222");
        bancoA.registerCustomer("06123456-8", "Genesis Flores", "genesis@mail.com", "7222-3333");
        bancoA.registerCustomer("07123456-9", "Rodrigo Sanchez", "rodrigo@mail.com", "7333-4444");
        bancoB.registerCustomer("08123456-0", "Gerson Bermudez", "gerson@mail.com", "7444-5555");
        bancoB.registerCustomer("09123456-1", "Jonathan Amaya", "jonathan@mail.com", "7555-6666");
        bancoA.assignAccountToCustomer("A001", "05123456-7");
        bancoA.assignAccountToCustomer("A002", "06123456-8");
        bancoA.assignAccountToCustomer("A003", "07123456-9");
        bancoB.assignAccountToCustomer("B001", "08123456-0");
        bancoB.assignAccountToCustomer("B002", "09123456-1");
    }

    /**
     * Menú interactivo en consola para operaciones CRUD y simulación concurrente.
     */
    private static void runInteractiveMenu(BankingSystem system) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║          SISTEMA BANCARIO CONCURRENTE  - GRUPO 2         ║");
            System.out.println("║                       MENU PRINCIPAL                     ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println("  1. [CREATE] Apertura de nueva cuenta");
            System.out.println("  2. [READ]   Consultar cuenta específica por ID");
            System.out.println("  3. [READ]   Listar cuentas activas (Banco A / B / Ambos)");
            System.out.println("  4. [UPDATE] Modificar estado (Activa / Bloqueada / Inactiva)");
            System.out.println("  5. [UPDATE] Modificar límites (Sobregiro / Límite diario)");
            System.out.println("  6. [ASIST]  Vaciar saldo (Retiro total o transferencia a otra cuenta)");
            System.out.println("  7. [DELETE] Cierre / Cancelación de cuenta (Regla saldo cero)");
            System.out.println("  8. [STRESS] Ejecutar simulación concurrente multihilo");
            System.out.println("  9. [CRUD]   Gestionar clientes");
            System.out.println(" 10. Salir del sistema");
            System.out.print("Seleccione una opción (1-10): ");

            String input = scanner.nextLine().trim();
            switch (input) {
                case "1" -> handleCreateAccount(scanner, system);
                case "2" -> handleReadAccount(scanner, system);
                case "3" -> handleListActiveAccounts(scanner, system);
                case "4" -> handleUpdateStatus(scanner, system);
                case "5" -> handleUpdateLimits(scanner, system);
                case "6" -> handleDrainFunds(scanner, system);
                case "7" -> handleDeleteAccount(scanner, system);
                case "8" -> runConcurrentSimulation(system);
                case "9" -> handleCustomerCrud(scanner, system);
                case "10" -> {
                    System.out.println("\n[SISTEMA] Cerrando operaciones bancarias...");
                    running = false;
                }
                default -> System.out.println("\n[AVISO] Opción inválida. Intente nuevamente.");
            }
        }
    }

    private static void handleCustomerCrud(Scanner scanner, BankingSystem system) {
        Bank bank = system.getBankA();
        System.out.println("\n--- CRUD DE CLIENTES (BANCO_A) ---");
        System.out.println("1. CREATE  2. READ  3. UPDATE  4. DELETE");
        System.out.print("Seleccione una operación: ");
        String option = scanner.nextLine().trim();
        try {
            switch (option) {
                case "1" -> {
                    System.out.print("ID del cliente: ");
                    String id = scanner.nextLine().trim();
                    System.out.print("Nombre completo: ");
                    String name = scanner.nextLine().trim();
                    System.out.print("Correo: ");
                    String email = scanner.nextLine().trim();
                    System.out.print("Teléfono: ");
                    String phone = scanner.nextLine().trim();
                    System.out.println(bank.registerCustomer(id, name, email, phone)
                            ? "Cliente registrado correctamente." : "El cliente ya existe.");
                }
                case "2" -> {
                    System.out.print("ID del cliente (vacío para listar todos): ");
                    String id = scanner.nextLine().trim();
                    if (id.isEmpty()) bank.printCustomers();
                    else System.out.println(bank.getCustomer(id));
                }
                case "3" -> {
                    System.out.print("ID del cliente: ");
                    String id = scanner.nextLine().trim();
                    System.out.print("Nuevo correo: ");
                    String email = scanner.nextLine().trim();
                    System.out.print("Nuevo teléfono: ");
                    String phone = scanner.nextLine().trim();
                    System.out.println(bank.updateCustomerContact(id, email, phone)
                            ? "Cliente actualizado correctamente." : "Cliente no encontrado.");
                }
                case "4" -> {
                    System.out.print("ID del cliente a eliminar: ");
                    String id = scanner.nextLine().trim();
                    System.out.println(bank.deleteCustomer(id)
                            ? "Cliente eliminado correctamente." : "No se puede eliminar: no existe o conserva saldo pendiente.");
                }
                default -> System.out.println("Operación inválida.");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private static void handleCreateAccount(Scanner scanner, BankingSystem system) {
        System.out.println("\n--- APERTURA DE CUENTA BANCARIA (CREATE) ---");
        try {
            System.out.print("Seleccione Banco (1: BANCO_A, 2: BANCO_B): ");
            String bChoice = scanner.nextLine().trim();
            String bankId = bChoice.equals("2") ? "BANCO_B" : "BANCO_A";

            System.out.print("Ingrese el ID de la nueva cuenta (ej: A006, B006): ");
            String accountId = scanner.nextLine().trim().toUpperCase();

            System.out.print("Seleccione Tipo de Cuenta (1: Ahorros, 2: Corriente): ");
            String tChoice = scanner.nextLine().trim();
            AccountType type = tChoice.equals("2") ? AccountType.CORRIENTE : AccountType.AHORROS;

            System.out.print("Ingrese saldo inicial de apertura ($> 0): ");
            double initialBalance = Double.parseDouble(scanner.nextLine().trim());

            double overdraft = 0.0;
            if (type == AccountType.CORRIENTE) {
                System.out.print("Ingrese límite de sobregiro (opcional, default 500.0): ");
                String odStr = scanner.nextLine().trim();
                overdraft = odStr.isEmpty() ? 500.0 : Double.parseDouble(odStr);
            }

            System.out.print("Ingrese límite diario de transferencias (opcional, default 2000.0): ");
            String limitStr = scanner.nextLine().trim();
            double dailyLimit = limitStr.isEmpty() ? 2000.0 : Double.parseDouble(limitStr);

            BankAccount created = system.openAccount(bankId, accountId, type, initialBalance, overdraft, dailyLimit);
            System.out.println("\n[ÉXITO] ¡Cuenta creada exitosamente!");
            System.out.println(created.getDetailedInfo());
        } catch (NumberFormatException e) {
            System.out.println("\n[ERROR] Entrada numérica inválida.");
        } catch (Exception e) {
            System.out.println("\n[ERROR] " + e.getMessage());
        }
    }

    private static void handleReadAccount(Scanner scanner, BankingSystem system) {
        System.out.println("\n--- CONSULTA DE CUENTA POR ID (READ) ---");
        System.out.print("Ingrese el ID de la cuenta a consultar: ");
        String accountId = scanner.nextLine().trim().toUpperCase();
        BankAccount acc = system.findAccount(accountId);
        if (acc == null) {
            System.out.println("\n[AVISO] No se encontró ninguna cuenta con el ID '" + accountId + "'.");
        } else {
            System.out.println("\n" + acc.getDetailedInfo());
        }
    }

    private static void handleListActiveAccounts(Scanner scanner, BankingSystem system) {
        System.out.println("\n--- LISTADO DE CUENTAS ACTIVAS (READ) ---");
        System.out.print("Filtrar por (1: BANCO_A, 2: BANCO_B, 3: Ambos Bancos): ");
        String opt = scanner.nextLine().trim();
        String filter = switch (opt) {
            case "1" -> "BANCO_A";
            case "2" -> "BANCO_B";
            default -> "ALL";
        };

        List<BankAccount> active = system.listActiveAccounts(filter);
        System.out.println("\nCuentas Activas Encontradas: " + active.size());
        System.out.printf("%-10s | %-8s | %-10s | %-12s | %-12s | %-10s%n",
                "BANCO", "ID", "TIPO", "SALDO", "DISPONIBLE", "ESTADO");
        System.out.println("─".repeat(74));
        for (BankAccount acc : active) {
            System.out.printf("%-10s | %-8s | %-10s | $%10.2f | $%10.2f | %-10s%n",
                    acc.getBankId(), acc.getAccountId(), acc.getAccountType(),
                    acc.getBalance(), acc.getAvailableFunds(), acc.getStatus());
        }
    }

    private static void handleUpdateStatus(Scanner scanner, BankingSystem system) {
        System.out.println("\n--- MODIFICACIÓN DE ESTADO (UPDATE) ---");
        System.out.print("Ingrese ID de la cuenta a modificar: ");
        String accountId = scanner.nextLine().trim().toUpperCase();
        BankAccount acc = system.findAccount(accountId);
        if (acc == null) {
            System.out.println("\n[ERROR] Cuenta no encontrada.");
            return;
        }

        System.out.printf("Estado actual de %s: %s%n", accountId, acc.getStatus());
        System.out.println("Seleccione nuevo estado:");
        System.out.println("  1. ACTIVA");
        System.out.println("  2. BLOQUEADA (Impide retiros y transferencias en concurrencia)");
        System.out.println("  3. INACTIVA");
        System.out.print("Opción (1-3): ");
        String choice = scanner.nextLine().trim();

        AccountStatus newStatus = switch (choice) {
            case "2" -> AccountStatus.BLOQUEADA;
            case "3" -> AccountStatus.INACTIVA;
            default -> AccountStatus.ACTIVA;
        };

        boolean ok = system.updateAccountStatus(accountId, newStatus);
        if (ok) {
            System.out.printf("\n[ÉXITO] El estado de la cuenta %s ha sido actualizado a: %s%n",
                    accountId, newStatus);
        } else {
            System.out.println("\n[ERROR] No se pudo actualizar el estado.");
        }
    }

    private static void handleUpdateLimits(Scanner scanner, BankingSystem system) {
        System.out.println("\n--- MODIFICACIÓN DE LÍMITES (UPDATE) ---");
        System.out.print("Ingrese ID de la cuenta: ");
        String accountId = scanner.nextLine().trim().toUpperCase();
        BankAccount acc = system.findAccount(accountId);
        if (acc == null) {
            System.out.println("\n[ERROR] Cuenta no encontrada.");
            return;
        }

        System.out.println("1. Modificar límite de sobregiro (solo Corriente)");
        System.out.println("2. Modificar límite diario de transferencias");
        System.out.print("Seleccione opción (1-2): ");
        String choice = scanner.nextLine().trim();

        try {
            if (choice.equals("1")) {
                if (acc.getAccountType() != AccountType.CORRIENTE) {
                    System.out.println("\n[AVISO] Las cuentas de tipo Ahorros no admiten sobregiro.");
                    return;
                }
                System.out.printf("Límite actual de sobregiro: $%.2f%n", acc.getOverdraftLimit());
                System.out.print("Nuevo límite de sobregiro: ");
                double od = Double.parseDouble(scanner.nextLine().trim());
                system.updateOverdraftLimit(accountId, od);
                System.out.println("\n[ÉXITO] Límite de sobregiro actualizado a $" + od);
            } else {
                System.out.printf("Límite diario actual: $%.2f%n", acc.getDailyTransferLimit());
                System.out.print("Nuevo límite diario: ");
                double limit = Double.parseDouble(scanner.nextLine().trim());
                system.updateDailyTransferLimit(accountId, limit);
                System.out.println("\n[ÉXITO] Límite diario actualizado a $" + limit);
            }
        } catch (Exception e) {
            System.out.println("\n[ERROR] " + e.getMessage());
        }
    }

    private static void handleDrainFunds(Scanner scanner, BankingSystem system) {
        System.out.println("\n--- ASISTENTE PARA VACIAR SALDO (PREPARAR CIERRE) ---");
        System.out.print("Ingrese ID de la cuenta que desea vaciar: ");
        String accountId = scanner.nextLine().trim().toUpperCase();
        BankAccount acc = system.findAccount(accountId);
        if (acc == null) {
            System.out.println("\n[ERROR] Cuenta no encontrada.");
            return;
        }

        System.out.printf("Saldo actual en %s: $%.2f%n", accountId, acc.getBalance());
        if (acc.getBalance() <= 0) {
            System.out.println("\n[AVISO] La cuenta ya tiene saldo $0.00. Lista para cancelarse.");
            return;
        }

        System.out.println("¿Cómo desea vaciar el saldo?");
        System.out.println("  1. Retiro total en efectivo");
        System.out.println("  2. Transferir todo el dinero a otra cuenta");
        System.out.print("Opción (1-2): ");
        String opt = scanner.nextLine().trim();

        try {
            if (opt.equals("2")) {
                System.out.print("Ingrese ID de la cuenta destino: ");
                String dstId = scanner.nextLine().trim().toUpperCase();
                double transferred = system.transferTotalBalance(accountId, dstId);
                System.out.printf("\n[ÉXITO] Se transfirieron $%.2f de %s a %s. Saldo restante en origen: $%.2f%n",
                        transferred, accountId, dstId, acc.getBalance());
            } else {
                double withdrawn = system.withdrawTotalBalance(accountId);
                System.out.printf("\n[ÉXITO] Se retiraron en ventanilla $%.2f. Saldo restante: $%.2f%n",
                        withdrawn, acc.getBalance());
            }
        } catch (Exception e) {
            System.out.println("\n[ERROR] " + e.getMessage());
        }
    }

    private static void handleDeleteAccount(Scanner scanner, BankingSystem system) {
        System.out.println("\n--- CIERRE / CANCELACIÓN DE CUENTA (DELETE) ---");
        System.out.print("Ingrese ID de la cuenta que desea cerrar: ");
        String accountId = scanner.nextLine().trim().toUpperCase();

        try {
            boolean closed = system.closeAccount(accountId);
            if (closed) {
                System.out.println("\n[ÉXITO] La cuenta " + accountId + " ha sido cerrada y retirada del catálogo bancario.");
            }
        } catch (IllegalStateException e) {
            System.out.println("\n[REGLA DE NEGOCIO BANCARIA ACTIVADA]");
            System.out.println(e.getMessage());
            System.out.println("Sugerencia: Use la opción 6 para vaciar o transferir los fondos antes de cerrar.");
        } catch (Exception e) {
            System.out.println("\n[ERROR] " + e.getMessage());
        }
    }

    /**
     * Ejecuta la simulación de transacciones multihilo utilizando el catálogo dinámico de cuentas activas.
     */
    public static void runConcurrentSimulation(BankingSystem system) {
        System.out.println();
        System.out.println("═".repeat(60));
        System.out.println("    INICIANDO SIMULACIÓN CONCURRENTE MULTIHILO (STRESS TEST)");
        System.out.println("═".repeat(60));

        Bank bancoA = system.getBankA();
        Bank bancoB = system.getBankB();

        List<BankAccount> listA = bancoA.getActiveAccounts();
        List<BankAccount> listB = bancoB.getActiveAccounts();

        if (listA.isEmpty() || listB.isEmpty()) {
            System.out.println("[ERROR] Se requieren cuentas activas en ambos bancos para ejecutar la simulación.");
            return;
        }

        double initialTotal = bancoA.getTotalBalance() + bancoB.getTotalBalance();
        System.out.printf("SALDO INICIAL TOTAL: $%.2f (Banco A: $%.2f + Banco B: $%.2f)%n%n",
                initialTotal, bancoA.getTotalBalance(), bancoB.getTotalBalance());

        List<Future<Receipt>> futures = new ArrayList<>();

        for (int i = 0; i < NUM_CLIENTS; i++) {
            double amount = 100 + RANDOM.nextDouble() * 900;
            int opType = RANDOM.nextInt(3);
            Future<Receipt> future;

            switch (opType) {
                case 0 -> {
                    BankAccount acc = listA.get(RANDOM.nextInt(listA.size()));
                    LocalWithdraw w = new LocalWithdraw(acc.getAccountId(), amount, bancoA);
                    future = bancoA.submitTransaction(w);
                }
                case 1 -> {
                    BankAccount acc = listB.get(RANDOM.nextInt(listB.size()));
                    LocalDeposit d = new LocalDeposit(acc.getAccountId(), amount, bancoB);
                    future = bancoB.submitTransaction(d);
                }
                default -> {
                    BankAccount srcAcc;
                    Bank srcBank;
                    BankAccount dstAcc;
                    Bank dstBank;

                    if (RANDOM.nextBoolean()) {
                        srcAcc = listA.get(RANDOM.nextInt(listA.size()));
                        srcBank = bancoA;
                        dstAcc = listB.get(RANDOM.nextInt(listB.size()));
                        dstBank = bancoB;
                    } else {
                        srcAcc = listA.get(RANDOM.nextInt(listA.size()));
                        srcBank = bancoA;
                        dstAcc = listA.get(RANDOM.nextInt(listA.size()));
                        dstBank = bancoA;
                    }
                    if (srcAcc.getAccountId().equals(dstAcc.getAccountId())) {
                        srcAcc = listA.get(0);
                        dstAcc = listA.get(1 % listA.size());
                    }
                    InterBankTransfer t = new InterBankTransfer(
                            srcAcc.getAccountId(), amount, srcBank, dstBank, dstAcc.getAccountId());
                    future = srcBank.submitTransaction(t);
                }
            }
            futures.add(future);

            try {
                Thread.sleep(10 + RANDOM.nextInt(30));
            } catch (InterruptedException ignored) {}
        }

        System.out.println();
        System.out.println("═".repeat(60));
        System.out.println("  ESPERANDO RESULTADOS DE " + futures.size() + " TRANSACCIONES...");
        System.out.println("═".repeat(60));

        int success = 0, failed = 0, rolledBack = 0;
        double totalDeposited = 0, totalWithdrawn = 0;

        for (Future<Receipt> f : futures) {
            try {
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
            } catch (Exception e) {
                System.err.println("[ERROR EN TRANSACCIÓN] " + e.getMessage());
            }
        }

        System.out.println();
        System.out.println("═".repeat(60));
        System.out.println("  RESUMEN DE TRANSACCIONES");
        System.out.println("═".repeat(60));
        System.out.printf("  EXITOS       : %d%n", success);
        System.out.printf("  FALLIDOS     : %d%n", failed);
        System.out.printf("  REVERTIDOS   : %d%n", rolledBack);
        System.out.printf("  TOTAL        : %d%n", futures.size());
        System.out.println();

        double finalTotal = bancoA.getTotalBalance() + bancoB.getTotalBalance();
        double expectedTotal = initialTotal + totalDeposited - totalWithdrawn;
        boolean auditPass = Math.abs(finalTotal - expectedTotal) < 0.01;

        System.out.println("═".repeat(60));
        System.out.println("  AUDITORÍA FINAL");
        System.out.println("═".repeat(60));
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

        System.out.println("═".repeat(60));
        System.out.println("  La auditoría " + (auditPass ? "PASÓ CON ÉXITO [OK]" : "FALLÓ - inconsistencia contable"));
        System.out.println("═".repeat(60));
    }

    /**
     * Demo automatizada no interactiva para validación rápida.
     */
    public static void runAutomatedDemo(BankingSystem system) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║          DEMO AUTOMATIZADA DE CRUD Y CONCURRENCIA        ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        System.out.println("[TEST 1] Create (Apertura de Cuenta):");
        BankAccount accNew = system.openAccount("BANCO_A", "A099", AccountType.CORRIENTE, 1500.0, 500.0, 2000.0);
        System.out.println("  -> Cuenta creada: " + accNew);

        System.out.println("\n[TEST 2] Read (Consulta):");
        System.out.println(accNew.getDetailedInfo());

        System.out.println("\n[TEST 3] Update (Modificación de Estado a BLOQUEADA):");
        system.updateAccountStatus("A099", AccountStatus.BLOQUEADA);
        System.out.println("  -> Estado modificado: " + accNew.getStatus());

        System.out.println("\n[TEST 4] Intento de retiro en cuenta BLOQUEADA:");
        LocalWithdraw w = new LocalWithdraw("A099", 200.0, system.getBankA());
        try {
            Receipt r = system.getBankA().submitTransaction(w).get();
            System.out.printf("  -> Resultado retiro: %s (%s)%n", r.getStatus(), r.getMessage());
        } catch (Exception ignored) {}

        System.out.println("\n[TEST 5] Reactivación y prueba de sobregiro:");
        system.updateAccountStatus("A099", AccountStatus.ACTIVA);
        LocalWithdraw wOverdraft = new LocalWithdraw("A099", 1800.0, system.getBankA());
        try {
            Receipt r = system.getBankA().submitTransaction(wOverdraft).get();
            System.out.printf("  -> Retiro $1800 (Saldo: $1500 + Sobregiro: $500): %s (%s)%n",
                    r.getStatus(), r.getMessage());
            System.out.printf("  -> Saldo resultante en A099: $%.2f%n", accNew.getBalance());
        } catch (Exception ignored) {}

        System.out.println("\n[TEST 6] Delete (Cierre de cuenta) - Regla de saldo pendiente:");
        try {
            system.closeAccount("A099");
            System.out.println("  -> ERROR: Se cerró la cuenta teniendo saldo!");
        } catch (IllegalStateException e) {
            System.out.println("  -> REGLA BANCARIA DETECTADA: " + e.getMessage());
        }

        System.out.println("\n[TEST 7] Vaciado de saldo y Cierre definitivo:");
        try {
            if (accNew.getBalance() > 0) {
                system.withdrawTotalBalance("A099");
            } else if (accNew.getBalance() < 0) {
                // Saldar sobregiro
                accNew.deposit(-accNew.getBalance());
            }
            boolean closed = system.closeAccount("A099");
            System.out.println("  -> Cuenta cerrada con éxito: " + closed);
            System.out.println("  -> Existe A099 en catálogo?: " + (system.findAccount("A099") != null));
        } catch (Exception e) {
            System.out.println("  -> Error: " + e.getMessage());
        }

        System.out.println("\n[TEST 8] CRUD de clientes:");
        Bank bankA = system.getBankA();
        bankA.registerCustomer("09998888-1", "Cliente Temporal", "temporal@mail.com", "7999-8888");
        Customer customer = bankA.getCustomer("09998888-1");
        System.out.println("  -> Cliente creado y consultado: " + customer);
        bankA.updateCustomerContact("09998888-1", "nuevo@mail.com", "7000-0000");
        System.out.println("  -> Cliente actualizado: " + bankA.getCustomer("09998888-1"));
        System.out.println("  -> Cliente eliminado: " + bankA.deleteCustomer("09998888-1"));

        System.out.println("\n[TEST 9] Ejecución de Simulación Concurrente Multihilo:");
        runConcurrentSimulation(system);
    }
}
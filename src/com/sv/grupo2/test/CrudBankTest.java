package com.sv.grupo2.test;

import com.sv.grupo2.bank.Bank;
import com.sv.grupo2.bank.BankingSystem;
import com.sv.grupo2.model.AccountStatus;
import com.sv.grupo2.model.AccountType;
import com.sv.grupo2.model.BankAccount;
import com.sv.grupo2.model.Receipt;
import com.sv.grupo2.model.TransactionStatus;
import com.sv.grupo2.transaction.InterBankTransfer;
import com.sv.grupo2.transaction.LocalWithdraw;

import java.util.List;
import java.util.concurrent.Future;

public class CrudBankTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("══════════════════════════════════════════════════════════════");
        System.out.println("     INICIANDO SUITE DE PRUEBAS AUTOMATIZADAS DE CRUD BANCO   ");
        System.out.println("══════════════════════════════════════════════════════════════\n");

        BankingSystem system = new BankingSystem(5);

        testCreateValidAccount(system);
        testCreateInvalidBalanceRejected(system);
        testCreateDuplicateIdRejected(system);
        testReadAccountDetails(system);
        testReadActiveAccountsFilter(system);
        testUpdateStatusToBloqueada(system);
        testBlockedAccountPreventsWithdrawal(system);
        testBlockedAccountPreventsTransfer(system);
        testUpdateOverdraftAndTransferLimits(system);
        testDeleteAccountWithBalanceRejected(system);
        testDrainFundsAndSuccessfulDelete(system);

        system.shutdown();

        System.out.println("\n══════════════════════════════════════════════════════════════");
        System.out.printf("  RESUMEN DE PRUEBAS: %d PASARON, %d FALLARON.%n", testsPassed, testsFailed);
        System.out.println("══════════════════════════════════════════════════════════════");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void assertEquals(String testName, Object expected, Object actual) {
        if ((expected == null && actual == null) || (expected != null && expected.equals(actual))) {
            System.out.printf("  [PASS] %s%n", testName);
            testsPassed++;
        } else {
            System.err.printf("  [FAIL] %s - Esperado: %s, Obtenido: %s%n", testName, expected, actual);
            testsFailed++;
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        if (condition) {
            System.out.printf("  [PASS] %s%n", testName);
            testsPassed++;
        } else {
            System.err.printf("  [FAIL] %s - Condición evaluó a false%n", testName);
            testsFailed++;
        }
    }

    private static void testCreateValidAccount(BankingSystem system) {
        System.out.println("\n[TEST 1] Create: Apertura válida de cuentas Ahorros y Corriente");
        BankAccount acc1 = system.openAccount("BANCO_A", "T001", AccountType.AHORROS, 500.0, 0.0, 1000.0);
        assertEquals("Account ID correcto", "T001", acc1.getAccountId());
        assertEquals("Banco correcto", "BANCO_A", acc1.getBankId());
        assertEquals("Tipo de cuenta Ahorros", AccountType.AHORROS, acc1.getAccountType());
        assertEquals("Saldo inicial correcto", 500.0, acc1.getBalance());
        assertEquals("Estado inicial ACTIVA", AccountStatus.ACTIVA, acc1.getStatus());
        assertTrue("Fecha de creación no nula", acc1.getCreatedAt() != null);

        BankAccount acc2 = system.openAccount("BANCO_B", "T002", AccountType.CORRIENTE, 1000.0, 300.0, 2500.0);
        assertEquals("Tipo de cuenta Corriente", AccountType.CORRIENTE, acc2.getAccountType());
        assertEquals("Límite de sobregiro", 300.0, acc2.getOverdraftLimit());
        assertEquals("Fondos disponibles (Saldo + Sobregiro)", 1300.0, acc2.getAvailableFunds());
    }

    private static void testCreateInvalidBalanceRejected(BankingSystem system) {
        System.out.println("\n[TEST 2] Create: Rechazo de apertura con saldo <= 0");
        boolean rejectedZero = false;
        try {
            system.openAccount("BANCO_A", "T_ZERO", AccountType.AHORROS, 0.0, 0.0, 1000.0);
        } catch (IllegalArgumentException e) {
            rejectedZero = true;
        }
        assertTrue("Apertura con saldo $0.00 fue rechazada", rejectedZero);

        boolean rejectedNegative = false;
        try {
            system.openAccount("BANCO_A", "T_NEG", AccountType.AHORROS, -100.0, 0.0, 1000.0);
        } catch (IllegalArgumentException e) {
            rejectedNegative = true;
        }
        assertTrue("Apertura con saldo negativo fue rechazada", rejectedNegative);
    }

    private static void testCreateDuplicateIdRejected(BankingSystem system) {
        System.out.println("\n[TEST 3] Create: Rechazo de ID duplicado");
        boolean rejectedDuplicate = false;
        try {
            system.openAccount("BANCO_B", "T001", AccountType.AHORROS, 200.0, 0.0, 1000.0);
        } catch (IllegalArgumentException e) {
            rejectedDuplicate = true;
        }
        assertTrue("Apertura con ID 'T001' duplicado fue rechazada", rejectedDuplicate);
    }

    private static void testReadAccountDetails(BankingSystem system) {
        System.out.println("\n[TEST 4] Read: Consulta de datos detallados de cuenta");
        BankAccount acc = system.findAccount("T001");
        assertTrue("Cuenta encontrada por ID", acc != null);
        String info = acc.getDetailedInfo();
        assertTrue("Info contiene ID", info.contains("T001"));
        assertTrue("Info contiene saldo", info.contains("500.00"));
        assertTrue("Info contiene estado", info.contains("Activa"));
        assertTrue("Info contiene fecha de creación", info.contains("Fecha de Apertura"));
    }

    private static void testReadActiveAccountsFilter(BankingSystem system) {
        System.out.println("\n[TEST 5] Read: Listado de cuentas activas por banco y global");
        List<BankAccount> allActive = system.listActiveAccounts("ALL");
        assertTrue("Hay al menos 2 cuentas activas globales", allActive.size() >= 2);

        List<BankAccount> bankAActive = system.listActiveAccounts("BANCO_A");
        for (BankAccount a : bankAActive) {
            assertEquals("Todas pertenecen a BANCO_A", "BANCO_A", a.getBankId());
            assertEquals("Todas están activas", AccountStatus.ACTIVA, a.getStatus());
        }

        List<BankAccount> bankBActive = system.listActiveAccounts("BANCO_B");
        for (BankAccount b : bankBActive) {
            assertEquals("Todas pertenecen a BANCO_B", "BANCO_B", b.getBankId());
            assertEquals("Todas están activas", AccountStatus.ACTIVA, b.getStatus());
        }
    }

    private static void testUpdateStatusToBloqueada(BankingSystem system) {
        System.out.println("\n[TEST 6] Update: Modificación de estado a BLOQUEADA");
        boolean ok = system.updateAccountStatus("T001", AccountStatus.BLOQUEADA);
        assertTrue("Modificación exitosa", ok);
        BankAccount acc = system.findAccount("T001");
        assertEquals("Estado actualizado a BLOQUEADA", AccountStatus.BLOQUEADA, acc.getStatus());

        // Debe ser excluida del listado de cuentas activas
        List<BankAccount> activeBankA = system.listActiveAccounts("BANCO_A");
        boolean containsT001 = activeBankA.stream().anyMatch(a -> a.getAccountId().equals("T001"));
        assertTrue("Cuenta bloqueada excluida de listado de activas", !containsT001);
    }

    private static void testBlockedAccountPreventsWithdrawal(BankingSystem system) {
        System.out.println("\n[TEST 7] Update/Concurrency: Cuenta BLOQUEADA rechaza retiros");
        Bank bankA = system.getBankA();
        LocalWithdraw w = new LocalWithdraw("T001", 100.0, bankA);
        try {
            Future<Receipt> f = bankA.submitTransaction(w);
            Receipt r = f.get();
            assertEquals("Retiro en cuenta bloqueada FAILED", TransactionStatus.FAILED, r.getStatus());
            assertTrue("Mensaje indica cuenta bloqueada", r.getMessage().contains("bloqueada"));
        } catch (Exception e) {
            assertTrue("Excepción inesperada: " + e.getMessage(), false);
        }
    }

    private static void testBlockedAccountPreventsTransfer(BankingSystem system) {
        System.out.println("\n[TEST 8] Update/Concurrency: Cuenta origen BLOQUEADA rechaza transferencias");
        Bank bankA = system.getBankA();
        Bank bankB = system.getBankB();
        InterBankTransfer t = new InterBankTransfer("T001", 50.0, bankA, bankB, "T002");
        try {
            Future<Receipt> f = bankA.submitTransaction(t);
            Receipt r = f.get();
            assertEquals("Transferencia con origen bloqueado FAILED", TransactionStatus.FAILED, r.getStatus());
            assertTrue("Mensaje indica cuenta bloqueada", r.getMessage().contains("bloqueada"));
        } catch (Exception e) {
            assertTrue("Excepción inesperada: " + e.getMessage(), false);
        }
    }

    private static void testUpdateOverdraftAndTransferLimits(BankingSystem system) {
        System.out.println("\n[TEST 9] Update: Actualización de límites de sobregiro y diario");
        system.updateOverdraftLimit("T002", 700.0);
        BankAccount acc = system.findAccount("T002");
        assertEquals("Nuevo sobregiro", 700.0, acc.getOverdraftLimit());
        assertEquals("Nuevo disponible", 1700.0, acc.getAvailableFunds());

        system.updateDailyTransferLimit("T002", 4000.0);
        assertEquals("Nuevo límite diario", 4000.0, acc.getDailyTransferLimit());
    }

    private static void testDeleteAccountWithBalanceRejected(BankingSystem system) {
        System.out.println("\n[TEST 10] Delete: Regla bancaria - No se puede eliminar cuenta con saldo > 0");
        boolean exceptionThrown = false;
        String errMsg = "";
        try {
            system.closeAccount("T002");
        } catch (IllegalStateException e) {
            exceptionThrown = true;
            errMsg = e.getMessage();
        }
        assertTrue("Intento de eliminar T002 con saldo $1000 fue denegado", exceptionThrown);
        assertTrue("Mensaje menciona regla de saldo pendiente", errMsg.contains("saldo pendiente"));
        assertTrue("Cuenta T002 sigue existiendo en el catálogo", system.findAccount("T002") != null);
    }

    private static void testDrainFundsAndSuccessfulDelete(BankingSystem system) {
        System.out.println("\n[TEST 11] Delete: Vaciado de fondos y cierre exitoso");
        // Creamos una cuenta temporal T003
        BankAccount acc3 = system.openAccount("BANCO_B", "T003", AccountType.AHORROS, 250.0, 0.0, 1000.0);
        assertEquals("Saldo inicial T003", 250.0, acc3.getBalance());

        // Vaciamos el saldo con el asistente
        double withdrawn = system.withdrawTotalBalance("T003");
        assertEquals("Monto retirado", 250.0, withdrawn);
        assertEquals("Saldo queda en $0.00", 0.0, acc3.getBalance());

        // Ahora cerramos la cuenta
        boolean closed = system.closeAccount("T003");
        assertTrue("Cierre de T003 exitoso", closed);
        assertTrue("T003 ya no existe en el catálogo", system.findAccount("T003") == null);
        assertEquals("Estado pasa a INACTIVA", AccountStatus.INACTIVA, acc3.getStatus());
    }
}

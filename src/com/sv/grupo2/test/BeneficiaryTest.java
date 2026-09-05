package com.sv.grupo2.test;

import com.sv.grupo2.bank.BankingSystem;
import com.sv.grupo2.bank.BeneficiaryManager;
import com.sv.grupo2.model.AccountType;
import com.sv.grupo2.model.Beneficiary;

public class BeneficiaryTest {

    public static void main(String[] args) {

        System.out.println("=== PRUEBA CRUD DE BENEFICIARIOS ===");

        // Crear el sistema bancario
        BankingSystem bankingSystem = new BankingSystem(2);

        // Crear dos cuentas para realizar las pruebas
        bankingSystem.openAccount(
                "BANCO_A",
                "A001",
                AccountType.AHORROS,
                1000.00,
                0.00,
                5000.00
        );

        bankingSystem.openAccount(
                "BANCO_B",
                "B002",
                AccountType.AHORROS,
                1500.00,
                0.00,
                5000.00
        );

        // Crear el administrador de beneficiarios
        BeneficiaryManager beneficiaryManager =
                new BeneficiaryManager(bankingSystem);

        System.out.println("\n--- CREATE ---");

        beneficiaryManager.addBeneficiary(
                "A001",
                "Hermano",
                "BANCO_B",
                "B002"
        );

        System.out.println("Beneficiario agregado correctamente.");

        System.out.println("\n--- READ ---");

        for (Beneficiary beneficiary :
                beneficiaryManager.getBeneficiaries("A001")) {

            System.out.println(beneficiary);
        }

        System.out.println("\n--- UPDATE ---");

        boolean updated = beneficiaryManager.updateBeneficiary(
                "A001",
                "Hermano",
                "Mi Hermano",
                "BANCO_B",
                "B002"
        );

        if (updated) {
            System.out.println("Beneficiario actualizado correctamente.");
        } else {
            System.out.println("No se encontró el beneficiario.");
        }

        System.out.println("\n--- READ DESPUES DEL UPDATE ---");

        for (Beneficiary beneficiary :
                beneficiaryManager.getBeneficiaries("A001")) {

            System.out.println(beneficiary);
        }

        System.out.println("\n--- DELETE ---");

        boolean deleted =
                beneficiaryManager.deleteBeneficiary(
                        "A001",
                        "Mi Hermano"
                );

        if (deleted) {
            System.out.println("Beneficiario eliminado correctamente.");
        } else {
            System.out.println("No se encontró el beneficiario.");
        }

        System.out.println("\n--- RESULTADO FINAL ---");

        System.out.println(
                "Beneficiarios restantes: "
                        + beneficiaryManager
                        .getBeneficiaries("A001")
                        .size()
        );

        bankingSystem.shutdown();
    }
}
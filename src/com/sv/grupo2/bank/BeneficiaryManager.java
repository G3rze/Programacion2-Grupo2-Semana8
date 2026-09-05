package com.sv.grupo2.bank;

import com.sv.grupo2.model.BankAccount;
import com.sv.grupo2.model.Beneficiary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeneficiaryManager {

    private final BankingSystem bankingSystem;

    // Guarda los beneficiarios de cada cuenta origen
    private final Map<String, List<Beneficiary>> beneficiaries;

    public BeneficiaryManager(BankingSystem bankingSystem) {
        this.bankingSystem = bankingSystem;
        this.beneficiaries = new HashMap<>();
    }

    // CREATE
    public boolean addBeneficiary(
            String originAccountId,
            String alias,
            String bankId,
            String destinationAccountId) {

        BankAccount originAccount = bankingSystem.findAccount(originAccountId);
        BankAccount destinationAccount = bankingSystem.findAccount(destinationAccountId);

        if (originAccount == null) {
            throw new IllegalArgumentException("La cuenta de origen no existe.");
        }

        if (destinationAccount == null) {
            throw new IllegalArgumentException("La cuenta de destino no existe.");
        }

        if (originAccountId.equalsIgnoreCase(destinationAccountId)) {
            throw new IllegalArgumentException(
                    "No se puede agregar la misma cuenta como beneficiario.");
        }

        Beneficiary beneficiary =
                new Beneficiary(alias, bankId, destinationAccountId);

        beneficiaries
                .computeIfAbsent(originAccountId.toUpperCase(), k -> new ArrayList<>())
                .add(beneficiary);

        return true;
    }

    // READ
    public List<Beneficiary> getBeneficiaries(String originAccountId) {

        return beneficiaries.getOrDefault(
                originAccountId.toUpperCase(),
                new ArrayList<>()
        );
    }

    // UPDATE
    public boolean updateBeneficiary(
            String originAccountId,
            String currentAlias,
            String newAlias,
            String newBankId,
            String newAccountId) {

        List<Beneficiary> list =
                beneficiaries.get(originAccountId.toUpperCase());

        if (list == null) {
            return false;
        }

        BankAccount destinationAccount =
                bankingSystem.findAccount(newAccountId);

        if (destinationAccount == null) {
            throw new IllegalArgumentException(
                    "La nueva cuenta de destino no existe.");
        }

        for (Beneficiary beneficiary : list) {

            if (beneficiary.getAlias().equalsIgnoreCase(currentAlias)) {

                beneficiary.setAlias(newAlias);
                beneficiary.setBankId(newBankId);
                beneficiary.setAccountId(newAccountId);

                return true;
            }
        }

        return false;
    }

    // DELETE
    public boolean deleteBeneficiary(
            String originAccountId,
            String alias) {

        List<Beneficiary> list =
                beneficiaries.get(originAccountId.toUpperCase());

        if (list == null) {
            return false;
        }

        return list.removeIf(
                beneficiary ->
                        beneficiary.getAlias().equalsIgnoreCase(alias)
        );
    }
}
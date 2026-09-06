package com.sv.grupo2.model;

public class Beneficiary {

    private String alias;
    private String bankId;
    private String accountId;

    public Beneficiary(String alias, String bankId, String accountId) {
        this.alias = alias;
        this.bankId = bankId;
        this.accountId = accountId;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getBankId() {
        return bankId;
    }

    public void setBankId(String bankId) {
        this.bankId = bankId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String toString() {
        return "Beneficiario: " + alias
                + " | Banco: " + bankId
                + " | Cuenta: " + accountId;
    }
}
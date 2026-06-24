package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums;

public enum CashTransactionType {
    INCOME("Phiếu thu"),
    EXPENSE("Phiếu chi");

    private final String displayName;

    CashTransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

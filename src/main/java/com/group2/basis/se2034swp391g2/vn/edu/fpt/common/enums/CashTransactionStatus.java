package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums;

public enum CashTransactionStatus {
    COMPLETED("Hoàn tất"),
    CANCELLED("Đã hủy");

    private final String displayName;

    CashTransactionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

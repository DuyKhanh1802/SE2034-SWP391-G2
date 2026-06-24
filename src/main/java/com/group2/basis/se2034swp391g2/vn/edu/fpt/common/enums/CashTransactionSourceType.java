package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums;

public enum CashTransactionSourceType {
    PAYMENT("Thanh toán booking"),
    INVENTORY_RECEIPT("Nhập kho"),
    MANUAL("Thủ công");

    private final String displayName;

    CashTransactionSourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

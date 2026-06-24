package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums;

public enum CashTransactionSourceType {
    PAYMENT("Thanh toán khách hàng"),
    INVENTORY_RECEIPT("Phiếu nhập kho"),
    MANUAL("Phiếu thủ công");

    private final String displayName;

    CashTransactionSourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

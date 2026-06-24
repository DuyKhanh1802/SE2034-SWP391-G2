package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums;

public enum CashTransactionCategory {
    BOOKING_PAYMENT("Thanh toán check-out"),
    DEPOSIT("Đặt cọc"),
    REFUND("Hoàn tiền"),
    CAPITAL_INJECTION("Rót vốn"),
    MANUAL_INCOME("Thu thủ công"),
    MANUAL_EXPENSE("Chi thủ công"),
    INVENTORY_PURCHASE("Chi nhập kho"),
    REVERSAL("Giao dịch đảo chiều");

    private final String displayName;

    CashTransactionCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

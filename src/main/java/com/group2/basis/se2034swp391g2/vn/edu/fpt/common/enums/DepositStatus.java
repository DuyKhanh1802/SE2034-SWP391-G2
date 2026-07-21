package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums;

public enum DepositStatus {
    UNPAID("Chưa thanh toán cọc"),
    PAID("Đã thanh toán cọc"),
    FORFEITED("Đã mất cọc");

    private final String label;

    DepositStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

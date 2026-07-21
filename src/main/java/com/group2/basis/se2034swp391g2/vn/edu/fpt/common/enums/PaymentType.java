package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums;

public enum PaymentType {
    DEPOSIT("Thanh toán cọc"),
    BALANCE("Thanh toán phần còn lại");

    private final String label;

    PaymentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

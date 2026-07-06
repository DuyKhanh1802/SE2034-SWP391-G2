package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums;

public enum PaymentMethod {
    CASH("Tiền mặt"),
    CARD("Thẻ"),
    TRANSFER("Chuyển khoản"),
    VNPAY("Cổng thanh toán VNPAY");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
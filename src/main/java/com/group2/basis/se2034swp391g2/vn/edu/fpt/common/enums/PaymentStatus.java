package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums;

public enum PaymentStatus {
    SUCCESS("Thành công"),
    FAILED("Thất bại"),
    PENDING("Đang chờ xử lý");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums;

public enum BookingDetailStatus {
    RESERVED("Đã đặt"),
    CHECKED_IN("Đã nhận phòng"),
    CHECKED_OUT("Đã trả phòng"),
    CANCELLED("Đã hủy");

    private final String label;

    BookingDetailStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

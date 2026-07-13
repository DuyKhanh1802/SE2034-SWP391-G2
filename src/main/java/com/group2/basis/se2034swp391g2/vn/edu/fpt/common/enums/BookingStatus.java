package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums;

public enum BookingStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    CHECKED_IN("Đã nhận phòng"),
    PARTIALLY_CHECKED_OUT("Trả phòng một phần"),
    CHECKED_OUT("Đã trả phòng"),
    CANCELLED("Đã hủy"),
    NO_SHOW("Khách không đến");

    private final String label;

    BookingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

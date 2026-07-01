package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums;

public enum FolioItemStatus {
    REQUESTED("Chờ phục vụ"),
    COMPLETED("Đã phục vụ"),
    CANCELLED("Đã hủy");

    private final String label;

    FolioItemStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
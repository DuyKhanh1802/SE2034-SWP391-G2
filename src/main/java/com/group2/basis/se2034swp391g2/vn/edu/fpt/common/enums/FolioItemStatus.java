package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums;

public enum FolioItemStatus {
    REQUESTED("Chờ phục vụ"),
    COMPLETED("Đã phục vụ"),
    CANCELLED("Đã hủy"),
    NOT_USED_NO_REFUND("Không sử dụng - không hoàn tiền");

    private final String label;

    FolioItemStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

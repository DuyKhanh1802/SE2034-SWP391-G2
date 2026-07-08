package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums;

public enum RoomMoveFeePolicy {
    NO_CHARGE("Không phát sinh phí"),
    GUEST_UPGRADE_CHARGE("Tính phụ thu nâng hạng");

    private final String label;

    RoomMoveFeePolicy(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
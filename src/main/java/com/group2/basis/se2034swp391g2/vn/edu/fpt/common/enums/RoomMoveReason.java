package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums;

public enum RoomMoveReason {
    HOTEL_FAULT("Lỗi phòng/khách sạn"),
    GUEST_REQUEST("Khách yêu cầu đổi phòng"),
    GUEST_UPGRADE("Khách yêu cầu nâng hạng"),
    NOISY_ROOM("Phòng ồn"),
    OTHER("Khác");

    private final String label;

    RoomMoveReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
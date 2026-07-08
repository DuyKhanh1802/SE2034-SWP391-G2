package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomMoveFeePolicy;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomMoveReason;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import lombok.Data;

@Data
public class RoomMoveRequest {
    private Long bookingId;
    private Long bookingDetailId;
    private Long newRoomId;
    private RoomMoveReason reasonType;
    private RoomMoveFeePolicy feePolicy;
    private RoomStatus oldRoomStatusAfterMove;
    private String reasonNote;
}
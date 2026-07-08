package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomMoveHistoryResponse {
    private Instant movedAt;
    private String oldRoomNumber;
    private String newRoomNumber;
    private String reasonLabel;
    private String feePolicyLabel;
    private BigDecimal extraChargeAmount;
    private RoomStatus oldRoomStatusAfterMove;
    private String movedByName;
}
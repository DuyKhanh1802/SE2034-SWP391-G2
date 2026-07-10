package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomStatusBoardResponse {

    private Long roomId;

    private String roomNumber;

    private Integer floor;

    private RoomStatus status;

    private String roomTypeName;

    private String variantName;

    private String note;

    private Long activeBookingId;

    private Long activeBookingDetailId;
}

package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardRoomItemResponse {
    private Long id;
    private String roomNumber;
    private String roomTypeName;
    private RoomStatus status;
    private String statusLabel;
    private String statusClass;
    private String detailUrl;
}
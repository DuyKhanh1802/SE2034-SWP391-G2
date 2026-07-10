package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardResponse {

    private long totalStaff;
    private long activeStaff;
    private long inactiveStaff;

    private long totalRooms;
    private long availableRooms;
    private long occupiedRooms;
    private long maintenanceRooms;

    private long totalServices;
    private long availableServices;
    private long unavailableServices;

    private List<DashboardRoomItemResponse> roomOverview;
    private List<DashboardAlertResponse> alerts;
}
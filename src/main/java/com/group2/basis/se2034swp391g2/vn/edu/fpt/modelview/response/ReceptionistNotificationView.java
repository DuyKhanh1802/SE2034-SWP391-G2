package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.time.Instant;
import java.util.List;

public record ReceptionistNotificationView(
        long pendingBookingCount,
        long pendingServiceCount,
        List<ReceptionistDashboardView.PendingBookingRow> pendingBookings,
        List<ServiceRequestRow> pendingServices
) {
    public long totalCount() {
        return pendingBookingCount + pendingServiceCount;
    }

    public record ServiceRequestRow(
            Long folioItemId,
            Long bookingId,
            Long bookingDetailId,
            String bookingReference,
            String guestName,
            String roomNumber,
            String serviceName,
            Integer quantity,
            Instant postedAt
    ) {
    }
}

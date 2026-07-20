package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.FolioItemStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ReceptionistNotificationView;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.FolioItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceptionistNotificationService {

    private static final int HEADER_ITEM_LIMIT = 3;

    private final BookingRepository bookingRepository;
    private final FolioItemRepository folioItemRepository;

    @Transactional(readOnly = true)
    public ReceptionistNotificationView getHeaderNotifications() {
        long pendingBookingCount = bookingRepository.countByStatusAndIsDeletedFalse(BookingStatus.PENDING);
        long pendingServiceCount = folioItemRepository.countActiveServiceRequests(FolioItemStatus.REQUESTED);
        PageRequest recentItems = PageRequest.of(0, HEADER_ITEM_LIMIT);

        return new ReceptionistNotificationView(
                pendingBookingCount,
                pendingServiceCount,
                pendingBookingCount == 0
                        ? List.of()
                        : bookingRepository.findRecentPendingBookings(recentItems),
                pendingServiceCount == 0
                        ? List.of()
                        : folioItemRepository.findRecentActiveServiceRequests(FolioItemStatus.REQUESTED, recentItems)
        );
    }
}

package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ReceptionistDashboardView;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingDetailRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomMoveLogRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ReceptionistDashboardService {

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final RoomRepository roomRepository;
    private final RoomMoveLogRepository roomMoveLogRepository;

    public ReceptionistDashboardService(BookingRepository bookingRepository,
                                        BookingDetailRepository bookingDetailRepository,
                                        RoomRepository roomRepository,
                                        RoomMoveLogRepository roomMoveLogRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.roomRepository = roomRepository;
        this.roomMoveLogRepository = roomMoveLogRepository;
    }

    public ReceptionistDashboardView getDashboard() {
        LocalDate today = LocalDate.now();

        long todayArrivals =
                bookingRepository.countByStatusAndCheckInDateAndIsDeletedFalse(
                        BookingStatus.CONFIRMED,
                        today
                );

        long stayingGuests =
                bookingRepository.countByStatusAndIsDeletedFalseAndCheckInDateLessThanEqualAndCheckOutDateAfter(
                        BookingStatus.CHECKED_IN,
                        today,
                        today
                );

        long todayCheckOuts =
                bookingRepository.countByStatusAndCheckOutDateAndIsDeletedFalse(
                        BookingStatus.CHECKED_IN,
                        today
                );

        long pendingBookings =
                bookingRepository.countByStatusAndIsDeletedFalse(
                        BookingStatus.PENDING
                );

        return new ReceptionistDashboardView(
                today,
                todayArrivals,
                stayingGuests,
                todayCheckOuts,
                pendingBookings,
                bookingDetailRepository.findTodayCheckInRows(today, PageRequest.of(0, 5)),
                bookingDetailRepository.findTodayCheckOutRows(today, PageRequest.of(0, 5)),
                roomRepository.findDashboardRoomRows(PageRequest.of(0, 5)),
                bookingRepository.findRecentPendingBookings(PageRequest.of(0, 5)),
                roomMoveLogRepository.findRecentRoomMoveRows(PageRequest.of(0, 5))
        );
    }
}
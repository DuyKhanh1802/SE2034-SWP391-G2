package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.DepositStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.BookingDetail;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Room;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.BookingCreateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingDetailRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final BookingDetailRepository bookingDetailRepository;

    public BookingService(BookingRepository bookingRepository,
                          RoomRepository roomRepository,
                          BookingDetailRepository bookingDetailRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.bookingDetailRepository = bookingDetailRepository;
    }

    public List<BookingResponse> searchBookings(String keyword,
                                                String status,
                                                LocalDate checkIn,
                                                LocalDate checkOut) {
        String searchKeyword = keyword == null ? "" : keyword.trim();
        String searchStatus = status == null ? "" : status.trim();

        return bookingRepository.searchBookingList(searchKeyword, searchStatus, checkIn, checkOut);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRooms(LocalDate checkInDate, LocalDate checkOutDate) {
        validateBookingDates(checkInDate, checkOutDate);

        return roomRepository.findAvailableRooms(
                checkInDate,
                checkOutDate,
                RoomStatus.AVAILABLE,
                List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN)
        );
    }

    @Transactional
    public void addWalkInBooking(BookingCreateRequest request) {
        validateCreateBookingRequest(request);

        LocalDate checkInDate = request.getCheckInDate();
        LocalDate checkOutDate = request.getCheckOutDate();

        List<Room> selectedRooms = roomRepository.findAvailableRoomsByIds(
                request.getRoomIds(),
                checkInDate,
                checkOutDate,
                RoomStatus.AVAILABLE,
                List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN)
        );

        if (selectedRooms.size() != request.getRoomIds().size()) {
            throw new IllegalArgumentException("One or more selected rooms are no longer available.");
        }

        String[] nameParts = splitFullName(request.getFullName());

        BookingStatus bookingStatus;
        if ("create-check-in".equals(request.getAction())) {
            bookingStatus = BookingStatus.CHECKED_IN;
        } else {
            bookingStatus = BookingStatus.CONFIRMED;
        }

        Booking booking = Booking.builder()
                .guestFirstName(nameParts[0])
                .guestLastName(nameParts[1])
                .guestPhone(request.getPhoneNumber())
                .guestEmail(request.getEmail())
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .numAdults(request.getAdults())
                .numChildren(request.getChildren())
                .specialRequests(request.getNotes())
                .bookingReference(generateBookingReference())
                .depositStatus(DepositStatus.UNPAID)
                .status(bookingStatus)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .isDeleted(false)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);

        List<BookingDetail> bookingDetails = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Room room : selectedRooms) {
            BigDecimal pricePerNight = room.getRoomType().getBasePrice();
            BigDecimal subtotal = pricePerNight.multiply(BigDecimal.valueOf(nights));

            BookingDetail detail = BookingDetail.builder()
                    .booking(savedBooking)
                    .room(room)
                    .roomType(room.getRoomType())
                    .checkInDate(checkInDate)
                    .checkOutDate(checkOutDate)
                    .pricePerNight(pricePerNight)
                    .numNights((int) nights)
                    .subtotal(subtotal)
                    .build();

            bookingDetails.add(detail);
            totalAmount = totalAmount.add(subtotal);

            if (bookingStatus == BookingStatus.CHECKED_IN) {
                room.setStatus(RoomStatus.OCCUPIED);
            }
        }

        savedBooking.setTotalAmount(totalAmount);
        bookingRepository.save(savedBooking);

        bookingDetailRepository.saveAll(bookingDetails);

        if (bookingStatus == BookingStatus.CHECKED_IN) {
            roomRepository.saveAll(selectedRooms);
        }
    }

    private void validateCreateBookingRequest(BookingCreateRequest request) {
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Guest full name is required.");
        }

        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Guest phone number is required.");
        }

        validateBookingDates(request.getCheckInDate(), request.getCheckOutDate());

        if (request.getAdults() == null || request.getAdults() < 1) {
            throw new IllegalArgumentException("Adults must be at least 1.");
        }

        if (request.getChildren() == null || request.getChildren() < 0) {
            throw new IllegalArgumentException("Children must be at least 0.");
        }

        if (request.getRoomIds() == null || request.getRoomIds().isEmpty()) {
            throw new IllegalArgumentException("Please select at least one room.");
        }

        if (request.getAction() == null ||
                (!request.getAction().equals("create-only")
                        && !request.getAction().equals("create-check-in"))) {
            throw new IllegalArgumentException("Invalid booking action.");
        }
    }

    private void validateBookingDates(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException("Check-in and check-out dates are required.");
        }

        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }
    }

    private String[] splitFullName(String fullName) {
        String cleanName = fullName.trim().replaceAll("\\s+", " ");
        int lastSpaceIndex = cleanName.lastIndexOf(" ");

        if (lastSpaceIndex == -1) {
            return new String[]{cleanName, ""};
        }

        String firstName = cleanName.substring(0, lastSpaceIndex);
        String lastName = cleanName.substring(lastSpaceIndex + 1);

        return new String[]{firstName, lastName};
    }

    private String generateBookingReference() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String reference;

        do {
            int randomNumber = (int) (Math.random() * 9000) + 1000;
            reference = "BK-" + datePart + randomNumber;
        } while (bookingRepository.existsByBookingReference(reference));

        return reference;
    }
}
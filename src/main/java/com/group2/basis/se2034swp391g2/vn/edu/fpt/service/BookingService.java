package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.DepositStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.BookingDetail;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Room;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.BookingCreateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingDetailResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.CheckInProcedureResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ApprovalStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.IdentityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.UserType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Country;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.time.ZoneId;
import java.time.Instant;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private static final String ROOM_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int ROOM_CODE_LENGTH = 8;
    private static final int MAX_FULL_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 150;
    private static final int MAX_PHONE_LENGTH = 20;
    private static final int MAX_NOTES_LENGTH = 500;
    private static final int MAX_BOOKING_NIGHTS = 30;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9()+\\s-]{8,20}$");

    private final UserRepository userRepository;
    private final CountryRepository countryRepository;
    private final MailService mailService;
    
    public BookingService(BookingRepository bookingRepository,
                          RoomRepository roomRepository,
                          BookingDetailRepository bookingDetailRepository,
                          UserRepository userRepository,
                          CountryRepository countryRepository,
                          MailService mailService) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.userRepository = userRepository;
        this.countryRepository = countryRepository;
        this.mailService = mailService;
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
    public Page<BookingResponse> searchBookingsPaging(String keyword,
                                                      String status,
                                                      LocalDate checkIn,
                                                      LocalDate checkOut,
                                                      Pageable pageable) {
        String searchKeyword = keyword == null ? "" : keyword.trim();
        String searchStatus = status == null ? "" : status.trim();

        return bookingRepository.searchBookingListPaging(
                searchKeyword,
                searchStatus,
                checkIn,
                checkOut,
                pageable
        );
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
    public Long addWalkInBooking(BookingCreateRequest request) {
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

        validateRoomCapacity(selectedRooms, request);
        User guest = createGuestFromRequest(request);


        BookingStatus bookingStatus;
        if ("create-check-in".equals(request.getAction())) {
            bookingStatus = BookingStatus.CHECKED_IN;
        } else {
            bookingStatus = BookingStatus.CONFIRMED;
        }

        Booking booking = Booking.builder()
                .guestFirstName(request.getFirstName().trim())
                .guestLastName(request.getLastName().trim())
                .guestPhone(request.getPhoneNumber().trim())
                .guestEmail(request.getEmail().trim())
                .guest(guest)
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

            BookingDetail.BookingDetailBuilder detailBuilder = BookingDetail.builder()
                    .booking(savedBooking)
                    .room(room)
                    .roomType(room.getRoomType())
                    .checkInDate(checkInDate)
                    .checkOutDate(checkOutDate)
                    .pricePerNight(pricePerNight)
                    .numNights((int) nights)
                    .subtotal(subtotal);

            if (bookingStatus == BookingStatus.CHECKED_IN) {
                detailBuilder.roomCode(generateRoomCode());
                detailBuilder.roomCodeExpiresAt(generateRoomCodeExpiresAt(checkOutDate));

                room.setStatus(RoomStatus.OCCUPIED);
            }

            BookingDetail detail = detailBuilder.build();

            bookingDetails.add(detail);
            totalAmount = totalAmount.add(subtotal);
        }

        savedBooking.setTotalAmount(totalAmount);
        bookingRepository.save(savedBooking);

        bookingDetailRepository.saveAll(bookingDetails);

        if (bookingStatus == BookingStatus.CHECKED_IN) {
            roomRepository.saveAll(selectedRooms);
        }
        return savedBooking.getId();
    }

    private void validateCreateBookingRequest(BookingCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Booking request is required.");
        }

        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required.");
        }

        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required.");
        }

        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName().trim();

        if (firstName.length() > 50) {
            throw new IllegalArgumentException("First name must not exceed 50 characters.");
        }

        if (lastName.length() > 50) {
            throw new IllegalArgumentException("Last name must not exceed 50 characters.");
        }

        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Guest phone number is required.");
        }

        String phoneNumber = request.getPhoneNumber().trim();
        if (phoneNumber.length() > MAX_PHONE_LENGTH) {
            throw new IllegalArgumentException("Guest phone number must not exceed 20 characters.");
        }

        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new IllegalArgumentException("Invalid phone number format.");
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String email = request.getEmail().trim();

            if (email.length() > MAX_EMAIL_LENGTH) {
                throw new IllegalArgumentException("Email must not exceed 150 characters.");
            }

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("Invalid email format.");
            }
        }

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }

        if (request.getCountryId() == null) {
            throw new IllegalArgumentException("Country is required.");
        }

        if (request.getIdentityNumber() == null || request.getIdentityNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Identity number is required.");
        }

        if (request.getDateOfBirth() != null && request.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be in the future.");
        }

        validateBookingDates(request.getCheckInDate(), request.getCheckOutDate());

        if (request.getAdults() == null || request.getAdults() < 1) {
            throw new IllegalArgumentException("Adults must be at least 1.");
        }

        if (request.getChildren() == null || request.getChildren() < 0) {
            throw new IllegalArgumentException("Children must be at least 0.");
        }

        int totalGuests = request.getAdults() + request.getChildren();
        if (totalGuests < 1) {
            throw new IllegalArgumentException("Total guests must be at least 1.");
        }

        if (request.getRoomIds() == null || request.getRoomIds().isEmpty()) {
            throw new IllegalArgumentException("Please select at least one room.");
        }

        Set<Long> uniqueRoomIds = new HashSet<>(request.getRoomIds());
        if (uniqueRoomIds.size() != request.getRoomIds().size()) {
            throw new IllegalArgumentException("Duplicate room selection is not allowed.");
        }

        if (request.getNotes() != null && request.getNotes().length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("Notes must not exceed 500 characters.");
        }

        if (request.getAction() == null ||
                (!request.getAction().equals("create-only")
                        && !request.getAction().equals("create-check-in"))) {
            throw new IllegalArgumentException("Invalid booking action.");
        }

        if ("create-check-in".equals(request.getAction())
                && request.getCheckInDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot check in before the check-in date.");
        }

    }

    private void validateBookingDates(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException("Check-in and check-out dates are required.");
        }

        if (checkInDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Check-in date cannot be in the past.");
        }

        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }

        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);

        if (nights > MAX_BOOKING_NIGHTS) {
            throw new IllegalArgumentException("Booking duration must not exceed 30 nights.");
        }
    }

    private User createGuestFromRequest(BookingCreateRequest request) {
        String email = request.getEmail().trim();
        String phone = request.getPhoneNumber().trim();
        String identityNumber = request.getIdentityNumber().trim();

        User existingUser = userRepository
                .findByIdentityNumberAndIsDeletedFalse(identityNumber)
                .or(() -> userRepository.findByEmailAndIsDeletedFalse(email))
                .or(() -> userRepository.findByPhoneAndIsDeletedFalse(phone))
                .orElse(null);

        if (existingUser != null) {
            return existingUser;
        }

        Country country = countryRepository.findById(request.getCountryId())
                .orElseThrow(() -> new IllegalArgumentException("Country not found."));

        User guest = User.builder()
                .userType(UserType.GUEST)
                .approvalStatus(ApprovalStatus.APPROVED)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phone(phone)
                .email(email)
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .country(country)
                .identityType(resolveIdentityType(country))
                .identityNumber(identityNumber)
                .isActive(true)
                .isDeleted(false)
                .totalStays(0)
                .totalSpent(BigDecimal.ZERO)
                .build();

        return userRepository.save(guest);
    }

    private IdentityType resolveIdentityType(Country country) {
        if (country == null || country.getCountryName() == null) {
            return IdentityType.PASSPORT;
        }

        String countryName = country.getCountryName().trim().toLowerCase();

        if (countryName.equals("vietnam") || countryName.equals("việt nam")) {
            return IdentityType.CCCD;
        }

        return IdentityType.PASSPORT;
    }

    private void validateRoomCapacity(List<Room> selectedRooms, BookingCreateRequest request) {
        int totalGuests = request.getAdults() + request.getChildren();

        int totalCapacity = selectedRooms.stream()
                .map(Room::getRoomType)
                .mapToInt(roomType -> roomType.getCapacity() == null ? 0 : roomType.getCapacity())
                .sum();

        if (totalCapacity < totalGuests) {
            throw new IllegalArgumentException("Selected rooms do not have enough capacity for all guests.");
        }
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

    private String generateRoomCode(){
       String code;

       do {
           StringBuilder builder = new StringBuilder();

           for (int i = 0 ; i <ROOM_CODE_LENGTH; i++){
               int index = (int) (Math.random() * ROOM_CODE_CHARS.length());
               builder.append(ROOM_CODE_CHARS.charAt(index));
           }
           code = builder.toString();
       } while (bookingDetailRepository.existsByRoomCode(code));
       return  code;
    }

    private Instant generateRoomCodeExpiresAt(LocalDate checkOutDate){
        return checkOutDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    @Transactional(readOnly = true)
    public List<BookingDetailResponse> getCheckInCompleteInfo(Long bookingId) {
        List<BookingDetailResponse> roomCodes =
                bookingDetailRepository.findRoomCodesByBookingId(bookingId);

        if (roomCodes.isEmpty()) {
            throw new IllegalArgumentException("No room code found for booking id: " + bookingId);
        }

        return roomCodes;
    }

    @Transactional(readOnly = true)
    public CheckInProcedureResponse getCheckInProcedure(Long bookingId) {
        CheckInProcedureResponse response = bookingRepository.findCheckInProcedureByBookingId(bookingId);

        if (response == null) {
            throw new IllegalArgumentException("Booking not found.");
        }

        if (!"CONFIRMED".equals(response.getStatus())) {
            throw new IllegalArgumentException("Only confirmed bookings can be checked in.");
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAssignedRoomsForCheckIn(Long bookingId) {
        return bookingDetailRepository.findAssignedRoomsByBookingId(bookingId);
    }

    @Transactional
    public Long confirmCheckIn(Long bookingId) {
        if (bookingId == null) {
            throw new IllegalArgumentException("Booking id is required.");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new IllegalArgumentException("Booking has been deleted.");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only confirmed bookings can be checked in.");
        }

        if (booking.getCheckInDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot check in before the check-in date.");
        }

        List<BookingDetail> details = bookingDetailRepository.findDetailsWithRoomsByBookingId(bookingId);

        if (details.isEmpty()) {
            throw new IllegalArgumentException("No room assigned for this booking.");
        }

        for (BookingDetail detail : details) {
            Room room = detail.getRoom();

            if (room == null) {
                throw new IllegalArgumentException("Booking detail has no assigned room.");
            }

            if (Boolean.TRUE.equals(room.getIsDeleted())) {
                throw new IllegalArgumentException("Room " + room.getRoomNumber() + " has been deleted.");
            }

            if (room.getStatus() != RoomStatus.AVAILABLE) {
                throw new IllegalArgumentException("Room " + room.getRoomNumber() + " is not available.");
            }

            if (detail.getRoomCode() == null || detail.getRoomCode().isBlank()) {
                detail.setRoomCode(generateRoomCode());
            }

            detail.setRoomCodeExpiresAt(generateRoomCodeExpiresAt(booking.getCheckOutDate()));

            room.setStatus(RoomStatus.OCCUPIED);
        }

        booking.setStatus(BookingStatus.CHECKED_IN);

        bookingRepository.save(booking);
        bookingDetailRepository.saveAll(details);

        return booking.getId();
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getConfirmedBookingsForCheckIn(String keyword, Pageable pageable) {
        String searchKeyword = keyword == null ? "" : keyword.trim();
        return bookingRepository.findConfirmedBookingsForCheckIn(searchKeyword, pageable);
    }

    @Transactional
    public void sendRoomCodeEmail(Long bookingDetailId) {
        if (bookingDetailId == null) {
            throw new IllegalArgumentException("Booking detail id is required.");
        }

        BookingDetail detail = bookingDetailRepository.findById(bookingDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Booking detail not found."));

        Booking booking = detail.getBooking();

        if (booking == null) {
            throw new IllegalArgumentException("Booking not found.");
        }

        if (booking.getGuestEmail() == null || booking.getGuestEmail().isBlank()) {
            throw new IllegalArgumentException("Guest email is missing.");
        }

        if (detail.getRoom() == null) {
            throw new IllegalArgumentException("Room has not been assigned.");
        }

        if (detail.getRoomCode() == null || detail.getRoomCode().isBlank()) {
            detail.setRoomCode(generateRoomCode());
        }

        if (detail.getRoomCodeExpiresAt() == null) {
            detail.setRoomCodeExpiresAt(generateRoomCodeExpiresAt(detail.getCheckOutDate()));
        }

        bookingDetailRepository.save(detail);

        String guestName = (booking.getGuestFirstName() + " " + booking.getGuestLastName()).trim();

        mailService.sendRoomCodeEmail(
                booking.getGuestEmail(),
                guestName,
                booking.getBookingReference(),
                detail.getRoom().getRoomNumber(),
                detail.getRoomCode(),
                String.valueOf(detail.getCheckInDate()),
                String.valueOf(detail.getCheckOutDate())
        );
    }

    @Transactional
    public void sendRoomCodesEmail(Long bookingId) {
        if (bookingId == null) {
            throw new IllegalArgumentException("Booking id is required.");
        }

        List<BookingDetail> details = bookingDetailRepository.findDetailsWithRoomsByBookingId(bookingId);

        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("No room code found for this booking.");
        }

        Booking booking = details.get(0).getBooking();

        if (booking == null) {
            throw new IllegalArgumentException("Booking not found.");
        }

        if (booking.getGuestEmail() == null || booking.getGuestEmail().isBlank()) {
            throw new IllegalArgumentException("Guest email is missing.");
        }

        StringBuilder emailContent = new StringBuilder();
        emailContent.append("Your room access information:\n\n");

        for (BookingDetail detail : details) {
            if (detail.getRoom() == null) {
                throw new IllegalArgumentException("Room has not been assigned.");
            }

            if (detail.getRoomCode() == null || detail.getRoomCode().isBlank()) {
                detail.setRoomCode(generateRoomCode());
            }

            if (detail.getRoomCodeExpiresAt() == null) {
                detail.setRoomCodeExpiresAt(generateRoomCodeExpiresAt(detail.getCheckOutDate()));
            }

            emailContent
                    .append("Room Number: ")
                    .append(detail.getRoom().getRoomNumber())
                    .append("\n")
                    .append("Room Type: ")
                    .append(detail.getRoomType().getName())
                    .append("\n")
                    .append("Room Code: ")
                    .append(detail.getRoomCode())
                    .append("\n")
                    .append("Check-in Date: ")
                    .append(detail.getCheckInDate())
                    .append("\n")
                    .append("Check-out Date: ")
                    .append(detail.getCheckOutDate())
                    .append("\n")
                    .append("Expires At: ")
                    .append(detail.getRoomCodeExpiresAt())
                    .append("\n\n");
        }

        bookingDetailRepository.saveAll(details);

        String guestName = (booking.getGuestFirstName() + " " + booking.getGuestLastName()).trim();

        mailService.sendRoomCodesEmail(
                booking.getGuestEmail(),
                guestName,
                booking.getBookingReference(),
                emailContent.toString()
        );
    }

}

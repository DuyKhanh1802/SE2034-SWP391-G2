package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.BookingDetail;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingDetailResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {

    boolean existsByRoomCode(String roomCode);

    @Query("""
        SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingDetailResponse(
            bd.id,
            b.id,
            b.bookingReference,
            CONCAT(b.guestLastName, ' ', b.guestFirstName),
            r.roomNumber,
            v.variantName,
            bd.roomCode,
            bd.roomCodeExpiresAt,
            bd.checkInDate,
            bd.checkOutDate
        )
        FROM BookingDetail bd
        JOIN bd.booking b
        JOIN bd.room r
        JOIN bd.variant v
        JOIN v.roomType rt
        WHERE b.id = :bookingId
        ORDER BY r.roomNumber
        """)
    List<BookingDetailResponse> findRoomCodesByBookingId(@Param("bookingId") Long bookingId);

    @Query("""
        SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomResponse(
            r.id,
            r.roomNumber,
            v.variantName,
            v.pricePerNight
        )
        FROM BookingDetail bd
        JOIN bd.room r
        JOIN bd.variant v
        JOIN v.roomType rt
        WHERE bd.booking.id = :bookingId
        ORDER BY r.roomNumber
        """)
    List<RoomResponse> findAssignedRoomsByBookingId(@Param("bookingId") Long bookingId);

    @Query("""
        SELECT bd
        FROM BookingDetail bd
        JOIN FETCH bd.booking b
        JOIN FETCH bd.room r
        JOIN FETCH bd.variant v
        JOIN FETCH v.roomType rt
        WHERE b.id = :bookingId
        """)
    List<BookingDetail> findDetailsWithRoomsByBookingId(@Param("bookingId") Long bookingId);

    @Query("""
    SELECT bd
    FROM BookingDetail bd
    JOIN FETCH bd.booking b
    JOIN FETCH bd.room r
    JOIN FETCH bd.variant v
    JOIN FETCH v.roomType rt
    WHERE LOWER(b.guestEmail) = LOWER(:email)
      AND UPPER(bd.roomCode) = UPPER(:roomCode)
      AND bd.roomCodeExpiresAt > :now
      AND b.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus.CHECKED_IN
      AND b.isDeleted = false
""")
    Optional<BookingDetail> findValidGuestRoomAccess(@Param("email") String email,
                                                     @Param("roomCode") String roomCode,
                                                     @Param("now") Instant now);


    Optional<BookingDetail> findByRoomCode(String roomCode);
}



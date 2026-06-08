package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.BookingDetail;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingDetailResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {

    boolean existsByRoomCode(String roomCode);

    @Query("""
            SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingDetailResponse(
                b.id,
                b.bookingReference,
                CONCAT(b.guestFirstName, ' ', b.guestLastName),
                r.roomNumber,
                rt.name,
                bd.roomCode,
                bd.roomCodeExpiresAt,
                bd.checkInDate,
                bd.checkOutDate
            )
            FROM BookingDetail bd
            JOIN bd.booking b
            JOIN bd.room r
            JOIN bd.roomType rt
            WHERE b.id = :bookingId
            ORDER BY r.roomNumber
            """)
    List<BookingDetailResponse> findRoomCodesByBookingId(@Param("bookingId") Long bookingId);

    @Query("""
        SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomResponse(
            r.id,
            r.roomNumber,
            rt.name,
            rt.basePrice
        )
        FROM BookingDetail bd
        JOIN bd.room r
        JOIN bd.roomType rt
        WHERE bd.booking.id = :bookingId
        ORDER BY r.roomNumber
        """)
    List<RoomResponse> findAssignedRoomsByBookingId(@Param("bookingId") Long bookingId);

    @Query("""
        SELECT bd
        FROM BookingDetail bd
        JOIN FETCH bd.booking b
        JOIN FETCH bd.room r
        JOIN FETCH bd.roomType rt
        WHERE b.id = :bookingId
        """)
    List<BookingDetail> findDetailsWithRoomsByBookingId(@Param("bookingId") Long bookingId);
}
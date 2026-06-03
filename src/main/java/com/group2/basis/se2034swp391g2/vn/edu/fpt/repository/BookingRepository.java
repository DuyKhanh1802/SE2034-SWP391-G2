package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByBookingReference(String bookingReference);
    @Query("""
            SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingResponse(
                b.id,
                b.bookingReference,
                CONCAT(b.guestFirstName, ' ', b.guestLastName),
                CONCAT(rt.name, ' ', r.roomNumber),
                b.checkInDate,
                b.checkOutDate,
                CAST(b.status AS string),
                b.totalAmount
            )
            FROM Booking b
            JOIN BookingDetail bd ON bd.booking = b
            JOIN Room r ON bd.room = r
            JOIN RoomType rt ON r.roomType = rt
            WHERE b.isDeleted = false
            ORDER BY b.id DESC
            """)
    List<BookingResponse> findAllBookingList();


    @Query("""
        SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingResponse(
            b.id,
            b.bookingReference,
            CONCAT(b.guestFirstName, ' ', b.guestLastName),
            CONCAT(rt.name, ' ', r.roomNumber),
            b.checkInDate,
            b.checkOutDate,
            CAST(b.status AS string),
            b.totalAmount
        )
        FROM Booking b
        JOIN BookingDetail bd ON bd.booking = b
        JOIN Room r ON bd.room = r
        JOIN RoomType rt ON r.roomType = rt
        WHERE b.isDeleted = false
        AND (
            :keyword = ''
            OR LOWER(b.bookingReference) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.guestFirstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.guestLastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(CONCAT(b.guestFirstName, ' ', b.guestLastName)) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(rt.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(CONCAT(rt.name, ' ', r.roomNumber)) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        AND (
            :status = ''
            OR CAST(b.status AS string) = :status
        )
        AND (
            :checkIn IS NULL
            OR b.checkInDate >= :checkIn
        )
        AND (
            :checkOut IS NULL
            OR b.checkOutDate <= :checkOut
        )
        ORDER BY b.id DESC
        """)
    List<BookingResponse> searchBookingList(@Param("keyword") String keyword,
                                            @Param("status") String status,
                                            @Param("checkIn") LocalDate checkIn,
                                            @Param("checkOut") LocalDate checkOut);

}
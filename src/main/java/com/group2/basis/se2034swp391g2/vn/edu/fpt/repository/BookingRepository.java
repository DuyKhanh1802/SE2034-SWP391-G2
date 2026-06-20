package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.CheckInProcedureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByBookingReference(String bookingReference);

    boolean existsByPromotion_IdAndIsDeletedFalse(Long promotionId);


    @Query("""
            SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingResponse(
                b.id,
                b.bookingReference,
                CONCAT(b.guestLastName, ' ', b.guestFirstName),
                CONCAT(v.variantName, ' - Phòng ', r.roomNumber),
                b.checkInDate,
                b.checkOutDate,
                CAST(b.status AS string),
                b.totalAmount
            )
            FROM Booking b
            JOIN BookingDetail bd ON bd.booking = b
            JOIN Room r ON bd.room = r
            JOIN r.variant v
            JOIN v.roomType rt
            WHERE b.isDeleted = false
            ORDER BY b.id DESC
            """)
    List<BookingResponse> findAllBookingList();


    @Query("""
        SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingResponse(
            b.id,
            b.bookingReference,
            CONCAT(b.guestLastName, ' ', b.guestFirstName),
            CONCAT(v.variantName, ' - Phòng ', r.roomNumber),
            b.checkInDate,
            b.checkOutDate,
            CAST(b.status AS string),
            b.totalAmount
        )
        FROM Booking b
        JOIN BookingDetail bd ON bd.booking = b
        JOIN Room r ON bd.room = r
        JOIN r.variant v
        JOIN v.roomType rt
        WHERE b.isDeleted = false
        AND (
            :keyword = ''
            OR LOWER(b.bookingReference) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.guestFirstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.guestLastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(CONCAT(b.guestFirstName, ' ', b.guestLastName)) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(rt.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(v.variantName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(CONCAT(v.variantName, ' - Phòng ', r.roomNumber)) LIKE LOWER(CONCAT('%', :keyword, '%'))
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

    @Query(value = """
        SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingResponse(
            b.id,
            b.bookingReference,
            CONCAT(b.guestLastName, ' ', b.guestFirstName),
            CONCAT(v.variantName, ' - Phòng ', r.roomNumber),
            b.checkInDate,
            b.checkOutDate,
            CAST(b.status AS string),
            b.totalAmount
        )
        FROM Booking b
        JOIN BookingDetail bd ON bd.booking = b
        JOIN Room r ON bd.room = r
        JOIN r.variant v
        JOIN v.roomType rt
        WHERE b.isDeleted = false
        AND (
            :keyword = ''
            OR LOWER(b.bookingReference) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.guestFirstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.guestLastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(CONCAT(b.guestLastName, ' ', b.guestFirstName)) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(rt.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(v.variantName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(CONCAT(v.variantName, ' - Phòng ', r.roomNumber)) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        AND (:status = '' OR CAST(b.status AS string) = :status)
        AND (:checkIn IS NULL OR b.checkInDate >= :checkIn)
        AND (:checkOut IS NULL OR b.checkOutDate <= :checkOut)
        ORDER BY b.createdAt DESC, b.id DESC
        """,
        countQuery = """
        SELECT COUNT(bd.id)
        FROM Booking b
        JOIN BookingDetail bd ON bd.booking = b
        JOIN Room r ON bd.room = r
        JOIN r.variant v
        JOIN v.roomType rt
        WHERE b.isDeleted = false
        AND (
            :keyword = ''
            OR LOWER(b.bookingReference) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.guestFirstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.guestLastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(CONCAT(b.guestLastName, ' ', b.guestFirstName)) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(rt.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(v.variantName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(CONCAT(v.variantName, ' - Phòng ', r.roomNumber)) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        AND (:status = '' OR CAST(b.status AS string) = :status)
        AND (:checkIn IS NULL OR b.checkInDate >= :checkIn)
        AND (:checkOut IS NULL OR b.checkOutDate <= :checkOut)
        """)
    Page<BookingResponse> searchBookingListPaging(@Param("keyword") String keyword,
                                                  @Param("status") String status,
                                                  @Param("checkIn") LocalDate checkIn,
                                                  @Param("checkOut") LocalDate checkOut,
                                                  Pageable pageable);

    @Query("""
        SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.CheckInProcedureResponse(
            b.id,
            b.bookingReference,
            CONCAT(b.guestLastName, ' ', b.guestFirstName),
            b.checkInDate,
            b.checkOutDate,
            b.specialRequests,
            b.totalAmount,
            CAST(b.status AS string)
        )
        FROM Booking b
        WHERE b.id = :bookingId
        AND b.isDeleted = false
        """)
    CheckInProcedureResponse findCheckInProcedureByBookingId(@Param("bookingId") Long bookingId);

    @Query(value = """
        SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingResponse(
            b.id,
            b.bookingReference,
            CONCAT(b.guestLastName, ' ', b.guestFirstName),
            CONCAT(v.variantName, ' - Phòng ', r.roomNumber),
            b.checkInDate,
            b.checkOutDate,
            CAST(b.status AS string),
            b.totalAmount
        )
        FROM Booking b
        JOIN BookingDetail bd ON bd.booking = b
        JOIN Room r ON bd.room = r
        JOIN r.variant v
        JOIN v.roomType rt
        WHERE b.isDeleted = false
        AND b.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus.CONFIRMED
        AND (
            :keyword = ''
            OR LOWER(b.bookingReference) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.guestFirstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.guestLastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(CONCAT(b.guestLastName, ' ', b.guestFirstName)) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(rt.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(v.variantName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        """,
        countQuery = """
        SELECT COUNT(bd.id)
        FROM Booking b
        JOIN BookingDetail bd ON bd.booking = b
        JOIN Room r ON bd.room = r
        JOIN r.variant v
        JOIN v.roomType rt
        WHERE b.isDeleted = false
        AND b.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus.CONFIRMED
        AND (
            :keyword = ''
            OR LOWER(b.bookingReference) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.guestFirstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.guestLastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(CONCAT(b.guestLastName, ' ', b.guestFirstName)) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(rt.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(v.variantName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        """)
    Page<BookingResponse> findConfirmedBookingsForCheckIn(@Param("keyword") String keyword, Pageable pageable);

    long countByCreatedAtBetween(java.time.Instant from, java.time.Instant to);

    long countByStatusAndIsDeletedFalse(com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus status);

    long countByStatusAndIsDeletedFalseAndCheckInDateLessThanEqualAndCheckOutDateAfter(
            com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus status,
            LocalDate checkInDate,
            LocalDate checkOutDate
    );

}

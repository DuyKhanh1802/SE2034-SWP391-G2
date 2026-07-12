package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.BookingDetail;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingDetailResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ReceptionistDashboardView;
import org.springframework.data.domain.Pageable;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

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
    LEFT JOIN FETCH bd.room r
    JOIN FETCH bd.variant v
    JOIN FETCH v.roomType rt
    WHERE b.id = :bookingId
    ORDER BY bd.id ASC
    """)
    List<BookingDetail> findDetailsWithRoomsByBookingId(@Param("bookingId") Long bookingId);

    @Query("""
    SELECT b.id, r.roomNumber
    FROM BookingDetail bd
    JOIN bd.booking b
    LEFT JOIN bd.room r
    WHERE b.id IN :bookingIds
      AND r.roomNumber IS NOT NULL
    ORDER BY r.roomNumber
    """)
    List<Object[]> findRoomNumbersByBookingIds(@Param("bookingIds") Collection<Long> bookingIds);

    @Query(
            value = """
    SELECT bd
    FROM BookingDetail bd
    JOIN FETCH bd.booking b
    LEFT JOIN FETCH bd.room r
    JOIN FETCH bd.variant v
    JOIN FETCH v.roomType rt
    WHERE b.isDeleted = false
      AND (
          :keyword = ''
          OR LOWER(b.bookingReference) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR (r.roomNumber IS NOT NULL AND LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
          OR (
              :searchGuestName = true
              AND (
                  LOWER(b.guestFirstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(b.guestLastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(CONCAT(b.guestLastName, ' ', b.guestFirstName)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(CONCAT(b.guestFirstName, ' ', b.guestLastName)) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
          )
      )
      AND (:bookingStatus = '' OR CAST(b.status AS string) = :bookingStatus)
      AND (:checkIn IS NULL OR bd.checkInDate >= :checkIn)
      AND (:checkOut IS NULL OR bd.checkOutDate <= :checkOut)
      AND (
          :paymentStatus = ''
          OR (
              :paymentStatus = 'PAID'
              AND (
                  COALESCE(bd.totalAmount, 0)
                  + COALESCE((SELECT SUM(fi.totalAmount) FROM FolioItem fi WHERE fi.bookingDetail = bd AND fi.isVoided = false), 0)
              ) <= (
                  COALESCE((SELECT SUM(application.amount) FROM PaymentApplication application JOIN application.payment p WHERE application.bookingDetail = bd AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS AND p.paymentType <> com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND), 0)
                  - COALESCE((SELECT SUM(application.amount) FROM PaymentApplication application JOIN application.payment p WHERE application.bookingDetail = bd AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS AND p.paymentType = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND), 0)
              )
          )
          OR (
              :paymentStatus = 'UNPAID'
              AND (
                  COALESCE(bd.totalAmount, 0)
                  + COALESCE((SELECT SUM(fi.totalAmount) FROM FolioItem fi WHERE fi.bookingDetail = bd AND fi.isVoided = false), 0)
              ) > 0
              AND (
                  COALESCE((SELECT SUM(application.amount) FROM PaymentApplication application JOIN application.payment p WHERE application.bookingDetail = bd AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS AND p.paymentType <> com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND), 0)
                  - COALESCE((SELECT SUM(application.amount) FROM PaymentApplication application JOIN application.payment p WHERE application.bookingDetail = bd AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS AND p.paymentType = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND), 0)
              ) = 0
          )
          OR (
              :paymentStatus = 'PARTIAL'
              AND (
                  COALESCE((SELECT SUM(application.amount) FROM PaymentApplication application JOIN application.payment p WHERE application.bookingDetail = bd AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS AND p.paymentType <> com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND), 0)
                  - COALESCE((SELECT SUM(application.amount) FROM PaymentApplication application JOIN application.payment p WHERE application.bookingDetail = bd AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS AND p.paymentType = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND), 0)
              ) > 0
              AND (
                  COALESCE((SELECT SUM(application.amount) FROM PaymentApplication application JOIN application.payment p WHERE application.bookingDetail = bd AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS AND p.paymentType <> com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND), 0)
                  - COALESCE((SELECT SUM(application.amount) FROM PaymentApplication application JOIN application.payment p WHERE application.bookingDetail = bd AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS AND p.paymentType = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND), 0)
              ) < (
                  COALESCE(bd.totalAmount, 0)
                  + COALESCE((SELECT SUM(fi.totalAmount) FROM FolioItem fi WHERE fi.bookingDetail = bd AND fi.isVoided = false), 0)
              )
          )
      )
    ORDER BY bd.checkInDate DESC, b.id DESC, bd.id ASC
    """,
            countQuery = """
    SELECT COUNT(bd)
    FROM BookingDetail bd
    JOIN bd.booking b
    LEFT JOIN bd.room r
    WHERE b.isDeleted = false
      AND (
          :keyword = ''
          OR LOWER(b.bookingReference) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR (r.roomNumber IS NOT NULL AND LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
          OR (
              :searchGuestName = true
              AND (
                  LOWER(b.guestFirstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(b.guestLastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(CONCAT(b.guestLastName, ' ', b.guestFirstName)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(CONCAT(b.guestFirstName, ' ', b.guestLastName)) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
          )
      )
      AND (:bookingStatus = '' OR CAST(b.status AS string) = :bookingStatus)
      AND (:checkIn IS NULL OR bd.checkInDate >= :checkIn)
      AND (:checkOut IS NULL OR bd.checkOutDate <= :checkOut)
      AND (
          :paymentStatus = ''
          OR (
              :paymentStatus = 'PAID'
              AND (COALESCE(bd.totalAmount, 0) + COALESCE((SELECT SUM(fi.totalAmount) FROM FolioItem fi WHERE fi.bookingDetail = bd AND fi.isVoided = false), 0)) <=
                  (COALESCE((SELECT SUM(application.amount) FROM PaymentApplication application JOIN application.payment p WHERE application.bookingDetail = bd AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS AND p.paymentType <> com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND), 0)
                  - COALESCE((SELECT SUM(application.amount) FROM PaymentApplication application JOIN application.payment p WHERE application.bookingDetail = bd AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS AND p.paymentType = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND), 0))
          )
          OR (
              :paymentStatus = 'UNPAID'
              AND (COALESCE(bd.totalAmount, 0) + COALESCE((SELECT SUM(fi.totalAmount) FROM FolioItem fi WHERE fi.bookingDetail = bd AND fi.isVoided = false), 0)) > 0
              AND (COALESCE((SELECT SUM(application.amount) FROM PaymentApplication application JOIN application.payment p WHERE application.bookingDetail = bd AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS AND p.paymentType <> com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND), 0)
                  - COALESCE((SELECT SUM(application.amount) FROM PaymentApplication application JOIN application.payment p WHERE application.bookingDetail = bd AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS AND p.paymentType = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND), 0)) = 0
          )
          OR (
              :paymentStatus = 'PARTIAL'
              AND (COALESCE((SELECT SUM(application.amount) FROM PaymentApplication application JOIN application.payment p WHERE application.bookingDetail = bd AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS AND p.paymentType <> com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND), 0)
                  - COALESCE((SELECT SUM(application.amount) FROM PaymentApplication application JOIN application.payment p WHERE application.bookingDetail = bd AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS AND p.paymentType = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND), 0)) > 0
              AND (COALESCE((SELECT SUM(application.amount) FROM PaymentApplication application JOIN application.payment p WHERE application.bookingDetail = bd AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS AND p.paymentType <> com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND), 0)
                  - COALESCE((SELECT SUM(application.amount) FROM PaymentApplication application JOIN application.payment p WHERE application.bookingDetail = bd AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS AND p.paymentType = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND), 0)) <
                  (COALESCE(bd.totalAmount, 0) + COALESCE((SELECT SUM(fi.totalAmount) FROM FolioItem fi WHERE fi.bookingDetail = bd AND fi.isVoided = false), 0))
          )
      )
    """
    )
    Page<BookingDetail> searchFolioBookingDetails(
            @Param("keyword") String keyword,
            @Param("searchGuestName") boolean searchGuestName,
            @Param("bookingStatus") String bookingStatus,
            @Param("paymentStatus") String paymentStatus,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            Pageable pageable
    );

    @Query("""
    SELECT bd
    FROM BookingDetail bd
    JOIN FETCH bd.booking b
    LEFT JOIN FETCH bd.room r
    JOIN FETCH bd.variant v
    JOIN FETCH v.roomType rt
    WHERE b.isDeleted = false
      AND b.status IN :statuses
      AND bd.checkInDate < :toExclusiveDate
      AND bd.checkOutDate > :fromDate
    ORDER BY rt.name ASC, v.variantName ASC, bd.checkInDate ASC
    """)
    List<BookingDetail> findOccupancyReportDetails(@Param("fromDate") LocalDate fromDate,
                                                   @Param("toExclusiveDate") LocalDate toExclusiveDate,
                                                   @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
    SELECT MIN(bd.checkInDate)
    FROM BookingDetail bd
    JOIN bd.booking b
    WHERE b.isDeleted = false
      AND b.status IN :statuses
    """)
    Optional<LocalDate> findEarliestOccupancyCheckInDate(@Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
    SELECT MAX(bd.checkOutDate)
    FROM BookingDetail bd
    JOIN bd.booking b
    WHERE b.isDeleted = false
      AND b.status IN :statuses
    """)
    Optional<LocalDate> findLatestOccupancyCheckOutDate(@Param("statuses") Collection<BookingStatus> statuses);

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

    @Query("""
    select count(bd) > 0
    from BookingDetail bd
    join bd.booking b
    where bd.room.id = :roomId
      and b.isDeleted = false
      and b.status in (
          com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus.CONFIRMED,
          com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus.CHECKED_IN
      )
      and bd.checkOutDate > :today
""")
    boolean existsActiveOrFutureBookingByRoomId(@Param("roomId") Long roomId,
                                                @Param("today") LocalDate today);


    @Query("""
    SELECT CASE WHEN COUNT(bd) > 0 THEN true ELSE false END
    FROM BookingDetail bd
    JOIN bd.booking b
    WHERE bd.room.id = :roomId
      AND b.isDeleted = false
      AND b.status IN :blockingStatuses
      AND bd.checkOutDate >= CURRENT_DATE
    """)
    boolean existsActiveOrUpcomingBookingByRoomId(@Param("roomId") Long roomId,
                                                  @Param("blockingStatuses") Collection<BookingStatus> blockingStatuses);

    @Query("""
    SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ReceptionistDashboardView$CheckInRow(
        b.id,
        b.bookingReference,
        CONCAT(b.guestLastName, ' ', b.guestFirstName),
        b.guestPhone,
        CONCAT(
            v.variantName,
            CASE
                WHEN r.roomNumber IS NULL THEN ' - Chưa phân phòng'
                ELSE CONCAT(' - ', r.roomNumber)
            END
        ),
        b.checkInDate,
        b.checkOutDate,
        b.numAdults,
        b.numChildren,
        CAST(b.depositStatus AS string)
    )
    FROM BookingDetail bd
    JOIN bd.booking b
    JOIN bd.variant v
    LEFT JOIN bd.room r
    WHERE b.isDeleted = false
      AND b.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus.CONFIRMED
      AND b.checkInDate = :today
    ORDER BY b.checkInDate ASC, b.id DESC
""")
    List<ReceptionistDashboardView.CheckInRow> findTodayCheckInRows(
            @Param("today") LocalDate today,
            Pageable pageable
    );

    @Query("""
    SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ReceptionistDashboardView$CheckOutRow(
        b.id,
        r.roomNumber,
        CONCAT(b.guestLastName, ' ', b.guestFirstName),
        b.checkOutDate,
        b.totalAmount,
        COALESCE((
            SELECT SUM(p.amount)
            FROM Payment p
            WHERE p.booking = b
              AND CAST(p.status AS string) = 'SUCCESS'
        ), 0),
        b.totalAmount - COALESCE((
            SELECT SUM(p.amount)
            FROM Payment p
            WHERE p.booking = b
              AND CAST(p.status AS string) = 'SUCCESS'
        ), 0)
    )
    FROM BookingDetail bd
    JOIN bd.booking b
    JOIN bd.room r
    WHERE b.isDeleted = false
      AND b.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus.CHECKED_IN
      AND b.checkOutDate = :today
    ORDER BY r.roomNumber ASC
""")
    List<ReceptionistDashboardView.CheckOutRow> findTodayCheckOutRows(
            @Param("today") LocalDate today,
            Pageable pageable
    );
}



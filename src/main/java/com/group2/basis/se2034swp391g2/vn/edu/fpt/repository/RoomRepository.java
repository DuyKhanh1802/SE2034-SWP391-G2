package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ViewType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Room;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomStatusBoardResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.GuestRoomVariantProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ReceptionistDashboardView;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("""
        SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomResponse(
            r.id,
            r.roomNumber,
            v.id,
            v.variantName,
            v.pricePerNight,
            CAST(v.viewType AS string),
            v.capacity,
            v.maxAdults,
            v.maxChildren,
            rt.allowExtraBed,
            rt.maxExtraBeds,
            rt.extraBedPrice,
            rt.extraBedNote
        )
        FROM Room r
        JOIN r.variant v
        JOIN v.roomType rt
        WHERE r.isDeleted = false
          AND r.status = :roomStatus
          AND NOT EXISTS (
              SELECT bd.id
              FROM BookingDetail bd
              JOIN bd.booking b
              WHERE bd.room = r
                AND b.isDeleted = false
                AND b.status IN :blockingStatuses
                AND bd.checkInDate < :checkOutDate
                AND bd.checkOutDate > :checkInDate
          )
        ORDER BY r.roomNumber
        """)
    List<RoomResponse> findAvailableRooms(
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("roomStatus") RoomStatus roomStatus,
            @Param("blockingStatuses") List<BookingStatus> blockingStatuses
    );

    List<Room> findByIsDeletedFalse();

    Page<Room> findByIsDeletedFalse(Pageable pageable);

    Optional<Room> findByIdAndIsDeletedFalse(Long id);

    long countByIsDeletedFalse();

    long countByStatusAndIsDeletedFalse(RoomStatus status);

    boolean existsByRoomNumberAndIsDeletedFalse(String roomNumber);

    @Query("""
        SELECT r
        FROM Room r
        JOIN FETCH r.variant v
        JOIN FETCH v.roomType rt
        WHERE r.id IN :roomIds
          AND r.isDeleted = false
          AND r.status = :roomStatus
          AND NOT EXISTS (
              SELECT bd.id
              FROM BookingDetail bd
              JOIN bd.booking b
              WHERE bd.room = r
                AND b.isDeleted = false
                AND b.status IN :blockingStatuses
                AND bd.checkInDate < :checkOutDate
                AND bd.checkOutDate > :checkInDate
          )
        """)
    List<Room> findAvailableRoomsByIds(
            @Param("roomIds") List<Long> roomIds,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("roomStatus") RoomStatus roomStatus,
            @Param("blockingStatuses") List<BookingStatus> blockingStatuses
    );

    @Query("""
    SELECT DISTINCT r.roomNumber
    FROM Room r
    WHERE r.roomNumber IS NOT NULL
    ORDER BY r.roomNumber ASC
    """)
    List<String> findExistingRoomNumbers();

    @Query("""
        SELECT r
        FROM Room r
        JOIN FETCH r.variant v
        WHERE r.isDeleted = false
          AND r.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus.AVAILABLE
          AND v.id = :variantId
        ORDER BY r.roomNumber ASC
        """)
    List<Room> findAvailableRoomsByVariantId(@Param("variantId") Long variantId);

    @Query("""
        SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomStatusBoardResponse(
            r.id,
            r.roomNumber,
            r.floor,
            r.status,
            rt.name,
            v.variantName,
            r.note,
            (
                SELECT MAX(b.id)
                FROM BookingDetail bd
                JOIN bd.booking b
                WHERE bd.room = r
                  AND b.isDeleted = false
                  AND b.status IN (
                    com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus.CHECKED_IN,
                    com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus.PARTIALLY_CHECKED_OUT
                  )
                  AND (
                    bd.stayStatus IS NULL
                    OR bd.stayStatus = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingDetailStatus.CHECKED_IN
                  )
            ),
            (
                SELECT MAX(bd.id)
                FROM BookingDetail bd
                JOIN bd.booking b
                WHERE bd.room = r
                  AND b.isDeleted = false
                  AND b.status IN (
                    com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus.CHECKED_IN,
                    com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus.PARTIALLY_CHECKED_OUT
                  )
                  AND (
                    bd.stayStatus IS NULL
                    OR bd.stayStatus = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingDetailStatus.CHECKED_IN
                  )
            )
        )
        FROM Room r
        JOIN r.variant v
        JOIN v.roomType rt
        WHERE r.isDeleted = false
          AND (:floor IS NULL OR r.floor = :floor)
          AND (:roomTypeName IS NULL OR LOWER(rt.name) LIKE LOWER(CONCAT('%', :roomTypeName, '%')))
          AND (:status IS NULL OR r.status = :status)
          AND (:keyword = '' OR LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY r.floor ASC, r.roomNumber ASC
        """)
    List<RoomStatusBoardResponse> searchRoomStatusBoard(
            @Param("floor") Integer floor,
            @Param("roomTypeName") String roomTypeName,
            @Param("status") RoomStatus status,
            @Param("keyword") String keyword
    );

    @Query("""
        SELECT DISTINCT r.floor
        FROM Room r
        WHERE r.isDeleted = false
        ORDER BY r.floor ASC
        """)
    List<Integer> findDistinctFloors();

    @Query(
            value = """
                SELECT r
                FROM Room r
                JOIN FETCH r.variant v
                JOIN FETCH v.roomType rt
                WHERE r.isDeleted = false
                  AND (:keyword IS NULL OR r.roomNumber LIKE CONCAT('%', :keyword, '%'))
                  AND (:roomType IS NULL OR UPPER(rt.name) LIKE CONCAT('%', :roomType, '%'))
                                                          AND (:variantId IS NULL OR v.id = :variantId)
                                                          AND (:floor IS NULL OR r.floor = :floor)
                  AND (:viewType IS NULL OR v.viewType = :viewType)
                  AND (:status IS NULL OR r.status = :status)
                  AND (
                        :operatingStatus IS NULL
                        OR (
                            :operatingStatus = 'ACTIVE'
                            AND r.status <> com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus.MAINTENANCE
                        )
                        OR (
                            :operatingStatus = 'INACTIVE'
                            AND r.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus.MAINTENANCE
                        )
                  )
                ORDER BY r.roomNumber ASC
                """,
            countQuery = """
                SELECT COUNT(r)
                FROM Room r
                JOIN r.variant v
                JOIN v.roomType rt
                WHERE r.isDeleted = false
                  AND (:keyword IS NULL OR r.roomNumber LIKE CONCAT('%', :keyword, '%'))
                  AND (:roomType IS NULL OR UPPER(rt.name) LIKE CONCAT('%', :roomType, '%'))
                                                              AND (:variantId IS NULL OR v.id = :variantId)
                                                              AND (:floor IS NULL OR r.floor = :floor)
                  AND (:viewType IS NULL OR v.viewType = :viewType)
                  AND (:status IS NULL OR r.status = :status)
                  AND (
                        :operatingStatus IS NULL
                        OR (
                            :operatingStatus = 'ACTIVE'
                            AND r.status <> com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus.MAINTENANCE
                        )
                        OR (
                            :operatingStatus = 'INACTIVE'
                            AND r.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus.MAINTENANCE
                        )
                  )
                """
    )
    Page<Room> searchForAdmin(
            @Param("keyword") String keyword,
            @Param("roomType") String roomType,
            @Param("variantId") Long variantId,
            @Param("floor") Integer floor,
            @Param("viewType") ViewType viewType,
            @Param("status") RoomStatus status,
            @Param("operatingStatus") String operatingStatus,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT rt.name
        FROM Room r
        JOIN r.variant v
        JOIN v.roomType rt
        WHERE r.isDeleted = false
          AND rt.name IS NOT NULL
        ORDER BY rt.name ASC
        """)
    List<String> findDistinctRoomTypeNamesForAdmin();

    @Query("""
        SELECT r
        FROM Room r
        JOIN FETCH r.variant v
        JOIN FETCH v.roomType rt
        WHERE r.isDeleted = false
          AND r.id <> :currentRoomId
          AND r.status = :roomStatus
          AND NOT EXISTS (
              SELECT bd.id
              FROM BookingDetail bd
              JOIN bd.booking b
              WHERE bd.room = r
                AND b.id <> :bookingId
                AND b.isDeleted = false
                AND b.status IN :blockingStatuses
                AND bd.checkInDate < :checkOutDate
                AND bd.checkOutDate > :checkInDate
          )
        ORDER BY r.floor ASC, r.roomNumber ASC
    """)
    List<Room> findAvailableRoomsForRoomMove(
            @Param("bookingId") Long bookingId,
            @Param("currentRoomId") Long currentRoomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("roomStatus") RoomStatus roomStatus,
            @Param("blockingStatuses") List<BookingStatus> blockingStatuses
    );

    @Query("""
        SELECT COUNT(bd) > 0
        FROM BookingDetail bd
        JOIN bd.booking b
        WHERE bd.room.id = :roomId
          AND b.id <> :bookingId
          AND b.isDeleted = false
          AND b.status IN :blockingStatuses
          AND bd.checkInDate < :checkOutDate
          AND bd.checkOutDate > :checkInDate
    """)
    boolean existsRoomMoveBlockingBooking(
            @Param("roomId") Long roomId,
            @Param("bookingId") Long bookingId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("blockingStatuses") List<BookingStatus> blockingStatuses
    );

    @Query("""
    SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ReceptionistDashboardView$RoomRow(
        r.id,
        r.roomNumber,
        rt.name,
        v.variantName,
        r.floor,
        CAST(r.status AS string)
    )
    FROM Room r
    JOIN r.variant v
    JOIN v.roomType rt
    WHERE r.isDeleted = false
    ORDER BY r.floor ASC, r.roomNumber ASC
""")
    List<ReceptionistDashboardView.RoomRow> findDashboardRoomRows(Pageable pageable);
}

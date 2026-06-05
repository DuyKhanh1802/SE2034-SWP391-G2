package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Room;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("""
            SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomResponse(
                r.id,
                r.roomNumber,
                rt.name,
                rt.basePrice
            )
            FROM Room r
            JOIN r.roomType rt
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

    @Query("""
            SELECT r
            FROM Room r
            JOIN FETCH r.roomType rt
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
}
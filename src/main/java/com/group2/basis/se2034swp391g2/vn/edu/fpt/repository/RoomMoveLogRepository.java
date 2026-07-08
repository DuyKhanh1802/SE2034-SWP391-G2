package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomMoveLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomMoveLogRepository extends JpaRepository<RoomMoveLog, Long> {

    @Query("""
        SELECT rml
        FROM RoomMoveLog rml
        JOIN FETCH rml.oldRoom oldRoom
        JOIN FETCH rml.newRoom newRoom
        LEFT JOIN FETCH rml.movedBy movedBy
        LEFT JOIN FETCH rml.folioItem folioItem
        WHERE rml.booking.id = :bookingId
        ORDER BY rml.movedAt DESC
    """)
    List<RoomMoveLog> findByBookingIdWithRooms(@Param("bookingId") Long bookingId);
}
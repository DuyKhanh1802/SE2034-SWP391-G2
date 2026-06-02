package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;


import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByIsDeletedFalse();

    long countByIsDeletedFalse();

    long countByStatusAndIsDeletedFalse(RoomStatus status);
}
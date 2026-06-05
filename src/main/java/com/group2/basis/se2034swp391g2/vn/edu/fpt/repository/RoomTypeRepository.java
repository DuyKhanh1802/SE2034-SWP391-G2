package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;


import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    List<RoomType> findByIsDeletedFalse();
}
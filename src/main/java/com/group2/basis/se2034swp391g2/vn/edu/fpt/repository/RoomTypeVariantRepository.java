package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomTypeVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomTypeVariantRepository extends JpaRepository<RoomTypeVariant, Long> {

    List<RoomTypeVariant> findByIsDeletedFalseOrderByRoomType_NameAscVariantNameAsc();
}

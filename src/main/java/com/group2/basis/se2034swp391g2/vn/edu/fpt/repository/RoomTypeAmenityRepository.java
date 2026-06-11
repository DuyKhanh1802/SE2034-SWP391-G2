package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomTypeAmenity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomTypeAmenityRepository extends JpaRepository<RoomTypeAmenity, Long> {
    List<RoomTypeAmenity> findByRoomType_IdOrderBySortOrderAscIdAsc(Long roomTypeId);

    List<RoomTypeAmenity> findByAmenity_Id(Long amenityId);
}

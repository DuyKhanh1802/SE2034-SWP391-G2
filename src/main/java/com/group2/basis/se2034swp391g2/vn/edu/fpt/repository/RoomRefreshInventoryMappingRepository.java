package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomRefreshInventoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRefreshInventoryMappingRepository extends JpaRepository<RoomRefreshInventoryMapping, Long> {
    List<RoomRefreshInventoryMapping> findByRoomType_Id(Long roomTypeId);

    List<RoomRefreshInventoryMapping> findByItem_Id(Long itemId);
}

package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomRefreshInventoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRefreshInventoryMappingRepository extends JpaRepository<RoomRefreshInventoryMapping, Long> {
    List<RoomRefreshInventoryMapping> findByRoomType_Id(Long roomTypeId);

    List<RoomRefreshInventoryMapping> findByItem_Id(Long itemId);

    Optional<RoomRefreshInventoryMapping> findByRoomType_IdAndItem_Id(Long roomTypeId, Long itemId);

    Optional<RoomRefreshInventoryMapping> findByIdAndItem_Id(Long id, Long itemId);

    boolean existsByItem_Id(Long itemId);
}

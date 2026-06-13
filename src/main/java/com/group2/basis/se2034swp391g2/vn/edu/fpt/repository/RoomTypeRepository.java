package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ImageEntityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.HomeRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    List<RoomType> findByIsDeletedFalse();

    @Query(value = """
              SELECT
                rt.room_type_id AS id,
                rt.name AS name,
                rt.base_price AS basePrice,
                (rt.max_adults + rt.max_children) AS capacity,
                rt.description AS description,
                img.image_url AS imageUrl
            FROM room_types rt
            LEFT JOIN images img
                ON img.entity_type = 'ROOM_TYPE'
                AND img.entity_id = rt.room_type_id
                AND img.is_primary = 1
            WHERE rt.is_deleted = 0
            ORDER BY rt.room_type_id ASC """,nativeQuery = true)
    List<HomeRoomType> findAllRoomtypeForHome(@Param("entityType") ImageEntityType entityType);
}

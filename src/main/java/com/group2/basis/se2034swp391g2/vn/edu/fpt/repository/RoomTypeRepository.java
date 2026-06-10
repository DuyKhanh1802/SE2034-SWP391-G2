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

    @Query("SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.HomeRoomType(" +
            " rt.id, " +
            " rt.name, " +
            " rt.basePrice, " +
            " rt.capacity, " +
            " rt.description, " +
            " img.imageUrl" +
            ") " +
            " FROM RoomType rt " +
            " LEFT JOIN Image img " +
            " ON img.entityType = :entityType " +
            " AND img.entityId = rt.id " +
            " AND img.isPrimary = true " +
            " WHERE rt.isDeleted = false " +
            " ORDER BY rt.id ASC")
    List<HomeRoomType> findAllRoomtypeForHome(@Param("entityType") ImageEntityType entityType);
}
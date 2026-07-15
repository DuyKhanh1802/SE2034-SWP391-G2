package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;


import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ViewType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomTypeVariant;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.GuestRoomVariantProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.RoomVariantDetailProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


public interface RoomTypeVariantRepository extends JpaRepository<RoomTypeVariant, Long> {


    boolean existsByRoomType_IdAndViewTypeAndIsDeletedFalse(Long roomTypeId, ViewType viewType);


    Optional<RoomTypeVariant> findByRoomType_IdAndViewTypeAndIsDeletedFalse(Long roomTypeId, ViewType viewType);


    @Query("""
   SELECT v
   FROM RoomTypeVariant v
   JOIN FETCH v.roomType rt
   WHERE v.isDeleted = false
     AND rt.isDeleted = false
   ORDER BY rt.name ASC, v.variantName ASC
   """)
    List<RoomTypeVariant> findByIsDeletedFalseOrderByRoomType_NameAscVariantNameAsc();


    @Query(value = """
      SELECT
         rtv.variant_id AS variantId,
         rtv.variant_name AS variantName,
         rtv.view_type AS viewType,
         rtv.capacity AS capacity,
         rtv.price_per_night AS pricePerNight,
         beds.bedSummary AS bedSummary,


         CASE
             WHEN :checkInDate IS NULL OR :checkOutDate IS NULL THEN NULL
             ELSE available.availableRooms
         END AS availableRooms,


         rtv.room_size AS roomSize,
         rtv.description AS description,
         img.imageUrls AS imageUrls


      FROM room_type_variants rtv


      JOIN room_types rt
         ON rtv.room_type_id = rt.room_type_id


      LEFT JOIN (
         SELECT
             i.entity_id AS variantId,
             STRING_AGG(
                 i.image_url,
                 '|'
             ) WITHIN GROUP (
                 ORDER BY i.sort_order ASC
             ) AS imageUrls
         FROM images i
         WHERE i.entity_type = 'ROOM_TYPE_VARIANT'
         GROUP BY i.entity_id
      ) img
         ON img.variantId = rtv.variant_id


      LEFT JOIN (
         SELECT
             rtvb.variant_id AS variantId,
             STRING_AGG(
                 CONCAT(bt.name, ' x', rtvb.quantity),
                 ', '
             ) AS bedSummary
         FROM room_type_variant_beds rtvb
         JOIN bed_types bt
             ON rtvb.bed_type_id = bt.bed_type_id
         GROUP BY rtvb.variant_id
      ) beds
         ON beds.variantId = rtv.variant_id


      LEFT JOIN (
         SELECT
             room_count.variantId,
             room_count.totalRooms
                 - ISNULL(booked.bookedRooms, 0) AS availableRooms


         FROM (
             SELECT
                 r.variant_id AS variantId,
                 COUNT(r.room_id) AS totalRooms
             FROM rooms r
             WHERE r.is_deleted = 0
               AND r.status NOT IN (
                   'MAINTENANCE',
                   'OUT_OF_SERVICE'
               )
             GROUP BY r.variant_id
         ) room_count


         LEFT JOIN (
             SELECT
                 bd.variant_id AS variantId,
                 COUNT(bd.booking_detail_id) AS bookedRooms
             FROM booking_details bd


             JOIN bookings b
                 ON bd.booking_id = b.booking_id


             WHERE b.status NOT IN (
                 'CANCELLED',
                 'EXPIRED'
             )
               AND (
                   :checkInDate IS NULL
                   OR :checkOutDate IS NULL
                   OR (
                       bd.check_in_date < :checkOutDate
                       AND bd.check_out_date > :checkInDate
                   )
               )


             GROUP BY bd.variant_id
         ) booked
             ON booked.variantId = room_count.variantId


      ) available
         ON available.variantId = rtv.variant_id


      WHERE rtv.is_deleted = 0
        AND rt.is_deleted = 0


        AND (
             :roomTypeId IS NULL
             OR rt.room_type_id = :roomTypeId
        )


        AND (
             :viewType IS NULL
             OR :viewType = ''
             OR rtv.view_type = :viewType
        )


        AND (
             :checkInDate IS NULL
             OR :checkOutDate IS NULL
             OR available.availableRooms >= :roomCount
        )


        AND rtv.capacity >= CEILING(
             1.0 * (:adults + :children) / :roomCount
        )


        AND rtv.max_adults >= CEILING(
             1.0 * :adults / :roomCount
        )


        AND rtv.max_children >= CEILING(
             1.0 * :children / :roomCount
        )


      ORDER BY
         CASE
             WHEN :sort = 'priceAsc'
             THEN rtv.price_per_night
         END ASC,


         CASE
             WHEN :sort = 'priceDesc'
             THEN rtv.price_per_night
         END DESC,


         rt.base_price ASC,
         rtv.variant_id ASC
      """,
            nativeQuery = true)
    List<GuestRoomVariantProjection> findGuestRoomVariants(
            @Param("roomTypeId") Long roomTypeId,
            @Param("viewType") String viewType,
            @Param("sort") String sort,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("adults") Integer adults,
            @Param("children") Integer children,
            @Param("roomCount") Integer roomCount
    );




    @Query(value = """
   SELECT
       rtv.variant_id AS variantId,
       rtv.variant_name AS variantName,
       rt.name AS roomTypeName,
       rtv.view_type AS viewType,


       rtv.capacity AS capacity,
       rtv.room_size AS roomSize,
       rtv.description AS description,


       rtv.allow_extra_bed AS allowExtraBed,
       rtv.max_extra_beds AS maxExtraBeds,
       rtv.extra_bed_price AS extraBedPrice,
       rtv.extra_bed_note AS extraBedNote,


       floorInfo.minFloor AS minFloor,
       floorInfo.maxFloor AS maxFloor,


       img.primaryImageUrl AS primaryImageUrl,
       img.imageUrls AS imageUrls,
       img.totalImages AS totalImages,


       beds.bedSummary AS bedSummary,
       amenities.amenitySummary AS amenitySummary,
       services.serviceSummary AS serviceSummary,


       CASE
           WHEN :checkInDate IS NULL OR :checkOutDate IS NULL THEN NULL
           ELSE available.availableRooms
       END AS availableRooms


   FROM room_type_variants rtv


   JOIN room_types rt
       ON rtv.room_type_id = rt.room_type_id


   LEFT JOIN (
       SELECT
           t.entity_id AS variantId,
           MAX(CASE WHEN t.rn = 1 THEN t.image_url END) AS primaryImageUrl,
           STRING_AGG(t.image_url, '|') WITHIN GROUP (ORDER BY t.sort_order ASC) AS imageUrls,
           COUNT(*) AS totalImages
       FROM (
           SELECT
               i.entity_id,
               i.image_url,
               i.sort_order,
               ROW_NUMBER() OVER (
                   PARTITION BY i.entity_id
                   ORDER BY i.is_primary DESC, i.sort_order ASC
               ) AS rn
           FROM images i
           WHERE i.entity_type = 'ROOM_TYPE_VARIANT'
       ) t
       GROUP BY t.entity_id
   ) img
       ON img.variantId = rtv.variant_id


   LEFT JOIN (
       SELECT
           r.variant_id AS variantId,
           MIN(r.floor) AS minFloor,
           MAX(r.floor) AS maxFloor
       FROM rooms r
       WHERE r.is_deleted = 0
       GROUP BY r.variant_id
   ) floorInfo
       ON floorInfo.variantId = rtv.variant_id


   LEFT JOIN (
       SELECT
           rtvb.variant_id AS variantId,
           STRING_AGG(
               CONCAT(bt.name, ' x', rtvb.quantity),
               ', '
           ) AS bedSummary
       FROM room_type_variant_beds rtvb
       JOIN bed_types bt
           ON rtvb.bed_type_id = bt.bed_type_id
       GROUP BY rtvb.variant_id
   ) beds
       ON beds.variantId = rtv.variant_id


   LEFT JOIN (
       SELECT
           rta.room_type_id AS roomTypeId,
           STRING_AGG(
               a.name,
               '|'
           ) WITHIN GROUP (ORDER BY rta.sort_order ASC) AS amenitySummary
       FROM room_type_amenities rta
       JOIN amenities a
           ON rta.amenity_id = a.amenity_id
       GROUP BY rta.room_type_id
   ) amenities
       ON amenities.roomTypeId = rt.room_type_id


   LEFT JOIN (
       SELECT
           rtvs.variant_id AS variantId,
           STRING_AGG(
               CONCAT(s.name, ' x', rtvs.quantity),
               '|'
           ) AS serviceSummary
       FROM room_type_variant_services rtvs
       JOIN services s
           ON rtvs.service_id = s.service_id
       WHERE rtvs.is_deleted = 0
       GROUP BY rtvs.variant_id
   ) services
       ON services.variantId = rtv.variant_id


   LEFT JOIN (
       SELECT
           roomTotal.variantId,
           roomTotal.totalRooms - ISNULL(booked.bookedRooms, 0) AS availableRooms
       FROM (
           SELECT
               r.variant_id AS variantId,
               COUNT(r.room_id) AS totalRooms
           FROM rooms r
           WHERE r.is_deleted = 0
             AND r.status NOT IN ('MAINTENANCE', 'OUT_OF_SERVICE')
           GROUP BY r.variant_id
       ) roomTotal


       LEFT JOIN (
           SELECT
               bd.variant_id AS variantId,
               COUNT(bd.booking_detail_id) AS bookedRooms
           FROM booking_details bd
           JOIN bookings b
               ON bd.booking_id = b.booking_id
           WHERE b.status NOT IN ('CANCELLED', 'EXPIRED')
             AND (
                   :checkInDate IS NULL
                   OR :checkOutDate IS NULL
                   OR (
                       bd.check_in_date < :checkOutDate
                       AND bd.check_out_date > :checkInDate
                   )
             )
           GROUP BY bd.variant_id
       ) booked
           ON booked.variantId = roomTotal.variantId
   ) available
       ON available.variantId = rtv.variant_id


   WHERE rtv.is_deleted = 0
     AND rt.is_deleted = 0
     AND rtv.variant_id = :variantId
   """, nativeQuery = true)
    Optional<RoomVariantDetailProjection> findRoomVariantDetailById(
            @Param("variantId") Long variantId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );


    @Query(
            value = """
           SELECT v
           FROM RoomTypeVariant v
           JOIN v.roomType rt
           WHERE v.isDeleted = false
             AND (
                   :keyword IS NULL OR :keyword = ''
                   OR LOWER(v.variantName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(rt.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 )
             AND (:viewType IS NULL OR v.viewType = :viewType)
           ORDER BY rt.name ASC, v.variantName ASC
       """,
            countQuery = """
           SELECT COUNT(v)
           FROM RoomTypeVariant v
           JOIN v.roomType rt
           WHERE v.isDeleted = false
             AND (
                   :keyword IS NULL OR :keyword = ''
                   OR LOWER(v.variantName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(rt.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 )
             AND (:viewType IS NULL OR v.viewType = :viewType)
       """
    )
    Page<RoomTypeVariant> searchVariants(@Param("keyword") String keyword,
                                         @Param("viewType") ViewType viewType,
                                         Pageable pageable);


    @Query(value = """
   SELECT
       rtv.variant_id AS variantId,
       rtv.variant_name AS variantName,
       rt.name AS roomTypeName,
       rtv.view_type AS viewType,
       rtv.price_per_night AS pricePerNight,


       img.primaryImageUrl AS primaryImageUrl,
       services.serviceSummary AS serviceSummary,


       CASE
           WHEN :checkInDate IS NULL
             OR :checkOutDate IS NULL
           THEN NULL
           ELSE available.availableRooms
       END AS availableRooms


   FROM room_type_variants rtv


   JOIN room_types rt
       ON rtv.room_type_id = rt.room_type_id


   LEFT JOIN (
       SELECT
           imageData.entity_id AS variantId,
           MAX(
               CASE
                   WHEN imageData.rowNumber = 1
                   THEN imageData.image_url
               END
           ) AS primaryImageUrl


       FROM (
           SELECT
               i.entity_id,
               i.image_url,


               ROW_NUMBER() OVER (
                   PARTITION BY i.entity_id
                   ORDER BY
                       i.is_primary DESC,
                       i.sort_order ASC
               ) AS rowNumber


           FROM images i


           WHERE i.entity_type = 'ROOM_TYPE_VARIANT'
       ) imageData


       GROUP BY imageData.entity_id
   ) img
       ON img.variantId = rtv.variant_id


   LEFT JOIN (
       SELECT
           rtvs.variant_id AS variantId,


           STRING_AGG(
               CONCAT(s.name, ' x', rtvs.quantity),
               ', '
           ) AS serviceSummary


       FROM room_type_variant_services rtvs


       JOIN services s
           ON rtvs.service_id = s.service_id


       WHERE rtvs.is_deleted = 0


       GROUP BY rtvs.variant_id
   ) services
       ON services.variantId = rtv.variant_id


   LEFT JOIN (
       SELECT
           roomCount.variantId,


           roomCount.totalRooms
               - ISNULL(bookedCount.bookedRooms, 0)
               AS availableRooms


       FROM (
           SELECT
               r.variant_id AS variantId,
               COUNT(r.room_id) AS totalRooms


           FROM rooms r


           WHERE r.is_deleted = 0
             AND r.status NOT IN (
                 'MAINTENANCE',
                 'OUT_OF_SERVICE'
             )


           GROUP BY r.variant_id
       ) roomCount


       LEFT JOIN (
           SELECT
               bd.variant_id AS variantId,
               COUNT(bd.booking_detail_id) AS bookedRooms


           FROM booking_details bd


           JOIN bookings b
               ON bd.booking_id = b.booking_id


           WHERE b.status NOT IN (
               'CANCELLED',
               'EXPIRED'
           )
             AND (
                 :checkInDate IS NULL
                 OR :checkOutDate IS NULL
                 OR (
                     bd.check_in_date < :checkOutDate
                     AND bd.check_out_date > :checkInDate
                 )
             )


           GROUP BY bd.variant_id
       ) bookedCount
           ON bookedCount.variantId = roomCount.variantId


   ) available
       ON available.variantId = rtv.variant_id


   WHERE rtv.is_deleted = 0
     AND rt.is_deleted = 0
     AND rtv.variant_id IN (:variantIds)


     AND (
         :checkInDate IS NULL
         OR :checkOutDate IS NULL
         OR ISNULL(available.availableRooms, 0) > 0
     )


   ORDER BY rtv.variant_id ASC
   """, nativeQuery = true)
    List<GuestRoomVariantProjection> findSelectedRoomVariantsByIds(
            @Param("variantIds") List<Long> variantIds,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );
}


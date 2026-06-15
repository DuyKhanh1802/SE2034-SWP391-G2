package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomTypeVariant;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.GuestRoomVariantProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomTypeVariantRepository extends JpaRepository<RoomTypeVariant, Long> {

    List<RoomTypeVariant> findByIsDeletedFalseOrderByRoomType_NameAscVariantNameAsc();

    @Query(value = """
       SELECT
          rtv.variant_id AS variantId,
          rtv.variant_name AS variantName,
          rt.name AS roomTypeName,
          rtv.view_type AS viewType,

          rtv.capacity AS capacity,
          rtv.max_adults AS maxAdults,
          rtv.max_children AS maxChildren,
          rtv.room_size AS roomSize,

          rtv.description AS description,
          rtv.price_per_night AS pricePerNight,

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

       OUTER APPLY (
          SELECT
              MAX(CASE WHEN x.rn = 1 THEN x.image_url END) AS primaryImageUrl,
              STRING_AGG(x.image_url, '|') WITHIN GROUP (ORDER BY x.sort_order ASC) AS imageUrls,
              COUNT(*) AS totalImages
          FROM (
              SELECT
                  i.image_url,
                  i.sort_order,
                  ROW_NUMBER() OVER (
                      ORDER BY i.is_primary DESC, i.sort_order ASC
                  ) AS rn
              FROM images i
              WHERE i.entity_type = 'ROOM_TYPE_VARIANT'
                AND i.entity_id = rtv.variant_id
          ) x
       ) img

       OUTER APPLY (
          SELECT STRING_AGG(
             CONCAT(bt.name, ' x', rtvb.quantity),
             ', '
          ) AS bedSummary
          FROM room_type_variant_beds rtvb
          JOIN bed_types bt
             ON rtvb.bed_type_id = bt.bed_type_id
          WHERE rtvb.variant_id = rtv.variant_id
       ) beds

       OUTER APPLY (
          SELECT STRING_AGG(
             a.name,
             ', '
          ) WITHIN GROUP (ORDER BY rta.sort_order ASC) AS amenitySummary
          FROM room_type_amenities rta
          JOIN amenities a
             ON rta.amenity_id = a.amenity_id
          WHERE rta.room_type_id = rt.room_type_id
       ) amenities

       OUTER APPLY (
          SELECT STRING_AGG(
             CONCAT(s.name, ' x', rtvs.quantity),
             ', '
          ) AS serviceSummary
          FROM room_type_variant_services rtvs
          JOIN services s
             ON rtvs.service_id = s.service_id
          WHERE rtvs.variant_id = rtv.variant_id
            AND rtvs.is_deleted = 0
       ) services

       OUTER APPLY (
          SELECT
              COUNT(r.room_id)
              -
              (
                  SELECT COUNT(bd.booking_detail_id)
                  FROM booking_details bd
                  JOIN bookings b
                      ON bd.booking_id = b.booking_id
                  WHERE bd.variant_id = rtv.variant_id
                    AND b.status NOT IN ('CANCELLED', 'EXPIRED')
                    AND bd.check_in_date < :checkOutDate
                    AND bd.check_out_date > :checkInDate
              ) AS availableRooms
          FROM rooms r
          WHERE r.variant_id = rtv.variant_id
            AND r.is_deleted = 0
            AND r.status NOT IN ('MAINTENANCE', 'OUT_OF_SERVICE')
       ) available

       WHERE rtv.is_deleted = 0
         AND rt.is_deleted = 0

         AND (:roomTypeId IS NULL OR rt.room_type_id = :roomTypeId)

         AND (:viewType IS NULL OR :viewType = '' OR rtv.view_type = :viewType)

         AND (
              :checkInDate IS NULL
              OR :checkOutDate IS NULL
              OR available.availableRooms >= :roomCount
         )
         AND rtv.capacity >= CEILING(1.0 * (:adults + :children) / :roomCount)
         AND rtv.max_adults >= CEILING(1.0 * :adults / :roomCount)
         AND rtv.max_children >= CEILING(1.0 * :children / :roomCount)

       ORDER BY
          CASE WHEN :sort = 'priceAsc' THEN rtv.price_per_night END ASC,
          CASE WHEN :sort = 'priceDesc' THEN rtv.price_per_night END DESC,
          rt.base_price ASC,
          rtv.variant_id ASC
       """, nativeQuery = true)
    List<GuestRoomVariantProjection> findGuestRoomVariants(
            @Param("roomTypeId") Long roomTypeId,
            @Param("viewType") String viewType,
            @Param("sort") String sort,
            @Param("checkInDate") java.time.LocalDate checkInDate,
            @Param("checkOutDate") java.time.LocalDate checkOutDate,
            @Param("adults") Integer adults,
            @Param("children") Integer children,
            @Param("roomCount") Integer roomCount
    );
}

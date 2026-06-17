package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeVariantRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.GuestRoomVariantProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.RoomVariantDetailProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomTypeVariantService {

    private final RoomTypeVariantRepository roomTypeVariantRepository;
    private final RoomTypeRepository roomTypeRepository;


    public List<RoomType> getRoomTypes() {
        return roomTypeRepository.findByIsDeletedFalse();
    }

    public List<GuestRoomVariantProjection> getGuestRoomVariants(
            Long roomTypeId,
            String viewType,
            String sort,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer adults,
            Integer children,
            Integer roomCount
    ) {
        if (sort == null || sort.isBlank()) {
            sort = "recommended";
        }

        if (adults == null || adults < 1) {
            adults = 1;
        }

        if (children == null || children < 0) {
            children = 0;
        }

        if (roomCount == null || roomCount < 1) {
            roomCount = 1;
        }

        if (viewType != null && viewType.isBlank()) {
            viewType = null;
        }

        return roomTypeVariantRepository.findGuestRoomVariants(
                roomTypeId,
                viewType,
                sort,
                checkInDate,
                checkOutDate,
                adults,
                children,
                roomCount
        );
    }

    public RoomVariantDetailProjection getRoomVariantDetail(
            Long variantId,
            LocalDate checkInDate,
            LocalDate checkOutdate
    ){
       return roomTypeVariantRepository
               .findRoomVariantDetailById(variantId,checkInDate,checkOutdate)
               .orElseThrow(() -> new RuntimeException("Không tìm thấy hạng phòng nào"));
    }



}
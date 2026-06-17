package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ViewType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomTypeVariant;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeVariantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class RoomTypeVariantManagementService {

    private final RoomTypeVariantRepository roomTypeVariantRepository;

    public RoomTypeVariantManagementService(RoomTypeVariantRepository roomTypeVariantRepository) {
        this.roomTypeVariantRepository = roomTypeVariantRepository;
    }

    public List<RoomTypeVariant> getAllActiveVariants() {
        return roomTypeVariantRepository.findByIsDeletedFalseOrderByRoomType_NameAscVariantNameAsc();
    }

    public Page<RoomTypeVariant> searchVariants(String keyword, ViewType viewType, Pageable pageable) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        return roomTypeVariantRepository.searchVariants(normalizedKeyword, viewType, pageable);
    }
}
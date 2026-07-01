package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeVariantRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ServiceRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.BookingServiceProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.GuestRoomVariantProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service

public class BookingSelectionService {
    private final RoomTypeVariantRepository roomTypeVariantRepository;
    private final ServiceRepository serviceRepository;


    public List<Long> parseVariantIds(String variantIds){

        List<Long> results = new ArrayList<>();
        if(variantIds == null || variantIds.trim().isEmpty()){
            throw new IllegalArgumentException("Vui lòng chọn phòng trước khi chọn dịch vụ");
        }

        String[] idArray = variantIds.split(",");
        for (String idText : idArray){
            if(idText == null || idText.trim().isEmpty()){
                continue;
            }
            Long id = Long.parseLong(idText.trim());
            results.add(id);
        }
        return results;
    }


    public List<GuestRoomVariantProjection> getSelectRoomsForService(
            String variantIds,
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        List<Long> ids = parseVariantIds(variantIds);

        List<GuestRoomVariantProjection> rooms =
                roomTypeVariantRepository.findSelectedRoomVariantsByIds(
                        ids,
                        checkInDate,
                        checkOutDate
                );

        if (rooms == null || rooms.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy phòng đã chọn");
        }

        List<GuestRoomVariantProjection> selectedRooms = new ArrayList<>();

        for (Long id : ids) {
            for (GuestRoomVariantProjection room : rooms) {
                if (room.getVariantId().equals(id)) {
                    selectedRooms.add(room);
                    break;
                }
            }
        }

        if (selectedRooms.size() != ids.size()) {
            throw new IllegalArgumentException("Có phòng đã chọn không tồn tại");
        }

        return selectedRooms;
    }

    public Page<BookingServiceProjection> getBookingService(
            String category,
            String sort,
            String priceFilter,
            int page,
            int size
    ){
        String finalCategory = normalizeCategory(category);
        String finalSort = normalizeSort(sort);
        String finalPriceFilter = normalizePriceFilter(priceFilter);
        if(page < 0){
            page = 0;
        }

        if(size <= 0){
            size = 6;
        }

        Pageable pageable = PageRequest.of(page,size);

        return serviceRepository.findBookingServices(
                finalCategory,
                finalSort,
                finalPriceFilter,
                pageable
        );
    }

    public BigDecimal calculateRoomSubtotal(
            List<GuestRoomVariantProjection> selectedRooms,
            long nights
    ){
        BigDecimal total = BigDecimal.ZERO;

        if(selectedRooms == null || selectedRooms.isEmpty()){
            return total;
        }
        if(nights <= 0){
            return total;
        }
        for(GuestRoomVariantProjection room : selectedRooms){
            BigDecimal pricePerNight = room.getPricePerNight();
            if(pricePerNight == null){
                pricePerNight = BigDecimal.ZERO;
            }
            BigDecimal roomTotal = pricePerNight.multiply(BigDecimal.valueOf(nights));

            total = total.add(roomTotal);
        }
        return total;
    }

    public String normalizeCategory(String category){
        if(category == null || category.trim().isEmpty()){
            return "ALL";
        }
        String value = category.trim().toUpperCase();
        if(value.equals("DINING")){
            return "DINING";
        }

        if(value.equals("WELLNESS")){
            return "WELLNESS";
        }
        return "ALL";
    }

    public String normalizeSort(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return "recommended";
        }

        String value = sort.trim();

        if (value.equals("priceAsc")) {
            return "priceAsc";
        }

        if (value.equals("priceDesc")) {
            return "priceDesc";
        }

        return "recommended";
    }


    public String normalizePriceFilter(String priceFilter) {
        if (priceFilter == null || priceFilter.trim().isEmpty()) {
            return "ALL";
        }

        String value = priceFilter.trim().toUpperCase();

        if (value.equals("UNDER_200")) {
            return "UNDER_200";
        }

        if (value.equals("FROM_200_TO_500")) {
            return "FROM_200_TO_500";
        }

        if (value.equals("OVER_500")) {
            return "OVER_500";
        }

        return "ALL";
    }
}

package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Guest;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.BookingConfirmRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.PromotionApplyResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.BookingServiceProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.GuestRoomVariantProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.ServiceProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.BookingSelectionService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.PromotionService;
import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/page/booking")
public class BookingPageController {
    private final BookingSelectionService bookingSelectionService;
    private final PromotionService promotionService;

    @GetMapping("/services")
    public String bookingService(
            @RequestParam String variantIds,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkInDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkOutDate,

            @RequestParam(defaultValue = "1") Integer adults,
            @RequestParam(defaultValue = "0") Integer children,
            @RequestParam(defaultValue = "1") Integer roomCount,
            @RequestParam(required = false) String roomGuests,
            @RequestParam(required = false) String promoCode,

            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(defaultValue = "recommended") String sort,
            @RequestParam(defaultValue = "ALL") String priceFilter,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,

            Model model
    ){
        String selectedCategory = bookingSelectionService.normalizeCategory(category);
        String selectedSort = bookingSelectionService.normalizeSort(sort);
        String selectedPriceFilter = bookingSelectionService.normalizePriceFilter(priceFilter);

        List<GuestRoomVariantProjection> selectedRooms = bookingSelectionService.getSelectRoomsForService(
                variantIds,
                checkInDate,
                checkOutDate
        );
        Page<BookingServiceProjection> servicePage = bookingSelectionService.getBookingService(
                selectedCategory,
                selectedSort,
                selectedPriceFilter,
                page,
                size
        );

        long nights = 0;

        if(checkInDate != null && checkOutDate != null && checkOutDate.isAfter(checkInDate)){
            nights = ChronoUnit.DAYS.between(checkInDate,checkOutDate);
        }
        BigDecimal roomSubtotal = bookingSelectionService.calculateRoomSubtotal(selectedRooms,nights);

        BigDecimal serviceCharge = roomSubtotal.multiply(BigDecimal.valueOf(0.05));
        BigDecimal vat = roomSubtotal.add(serviceCharge).multiply(BigDecimal.valueOf(0.08));

        PromotionApplyResponse promotionResult = promotionService.checkPromotionCode(promoCode);

        BigDecimal promotionDiscountAmount = BigDecimal.ZERO;

        if(promotionResult.isValid()){
            promotionDiscountAmount = promotionResult.getDiscountAmount();

        }

        BigDecimal totalBeforeDiscount = roomSubtotal.add(serviceCharge).add(vat);

        if(promotionDiscountAmount.compareTo(totalBeforeDiscount) > 0){
            promotionDiscountAmount = totalBeforeDiscount;
        }
        BigDecimal grandTotal = totalBeforeDiscount.subtract(promotionDiscountAmount);

        model.addAttribute("variantIds", variantIds);
        model.addAttribute("selectedRooms", selectedRooms);

        model.addAttribute("services", servicePage.getContent());
        model.addAttribute("servicePage", servicePage);

        model.addAttribute("checkInDate", checkInDate);
        model.addAttribute("checkOutDate", checkOutDate);
        model.addAttribute("nights", nights);

        model.addAttribute("adults", adults);
        model.addAttribute("children", children);
        model.addAttribute("roomCount", roomCount);
        model.addAttribute("roomGuests", roomGuests);
        model.addAttribute("promoCode", promoCode);

        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("selectedSort", selectedSort);
        model.addAttribute("selectedPriceFilter", selectedPriceFilter);

        model.addAttribute("promotionResult", promotionResult);
        model.addAttribute("promotionDiscountAmount", promotionDiscountAmount);
        model.addAttribute("promotionMessage", promotionResult.getMessage());
        model.addAttribute("promotionValid", promotionResult.isValid());

        model.addAttribute("roomSubtotal", roomSubtotal);
        model.addAttribute("serviceCharge", serviceCharge);
        model.addAttribute("vat", vat);
        model.addAttribute("grandTotal", grandTotal);

        return "guest/BookingServices";

    }

    @PostMapping("/confirm")
    public String bookingConfirm(
            @ModelAttribute BookingConfirmRequest request,
            Model model
    ) {
        List<GuestRoomVariantProjection> selectedRooms =
                bookingSelectionService.getSelectRoomsForService(
                        request.getVariantIds(),
                        request.getCheckInDate(),
                        request.getCheckOutDate()
                );

        PromotionApplyResponse promotionResult =
                promotionService.checkPromotionCode(request.getPromoCode());

        model.addAttribute("request", request);
        model.addAttribute("selectedRooms", selectedRooms);
        model.addAttribute("promotionResult", promotionResult);

        return "page/BookingConfirm";
    }
}

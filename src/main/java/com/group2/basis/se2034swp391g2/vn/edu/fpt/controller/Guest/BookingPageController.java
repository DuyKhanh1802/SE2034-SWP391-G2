package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Guest;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.BookingConfirmRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingCompleteResult;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingConfirmView;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingSuccessView;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.PromotionApplyResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.BookingServiceProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.GuestRoomVariantProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.BookingSelectionService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.OnlineBookingService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.PromotionService;
import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private final OnlineBookingService onlineBookingService;

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
        BookingConfirmView confirmView = onlineBookingService.prepareConfirmView(request);

        model.addAttribute("request", request);
        model.addAttribute("confirmView", confirmView);

        return "guest/BookingConfirm";
    }

    @GetMapping("/confirm")
    public String bookingCofirmWithoutService(
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

            Model model
    ) {
        BookingConfirmRequest request = new BookingConfirmRequest();
        request.setVariantIds(variantIds);
        request.setCheckInDate(checkInDate);
        request.setCheckOutDate(checkOutDate);
        request.setAdults(adults);
        request.setChildren(children);
        request.setRoomCount(roomCount);
        request.setRoomGuests(roomGuests);
        request.setPromoCode(promoCode);

        return bookingConfirm(request,model);
    }

    @PostMapping("/complete")
    public String completeBooking(
            @ModelAttribute BookingConfirmRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            BookingCompleteResult result = onlineBookingService.completeOnlineBooking(request);

            redirectAttributes.addAttribute("bookingReference", result.getBookingReference());

            return "redirect:/page/booking/success";
        } catch (IllegalArgumentException e) {
            BookingConfirmView confirmView = onlineBookingService.prepareConfirmView(request);

            model.addAttribute("request", request);
            model.addAttribute("confirmView", confirmView);
            model.addAttribute("errorMessage", e.getMessage());

            return "guest/BookingConfirm";
        }
    }

    @GetMapping("/success")
    public String bookingSuccess(
            @RequestParam String bookingReference,
            Model model
    ) {
        BookingSuccessView successView = onlineBookingService.getBookingSuccessView(bookingReference);

        model.addAttribute("successView", successView);

        return "guest/BookingSuccess";
    }
}

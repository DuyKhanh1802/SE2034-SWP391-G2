package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Guest;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Page.GuestSessionAdvice;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.GuestMyBookingView;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.GuestRoomSession;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.GuestBookingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/guest")
public class MyBookingController {

    private final GuestBookingService guestBookingService;

    @GetMapping("/my-booking")
    public String myBooking(@RequestParam(required = false) String category,
                            HttpSession session,
                            Model model,
                            RedirectAttributes redirectAttributes) {

        GuestRoomSession guestRoomSession = getGuestRoomSession(session);

        if (guestRoomSession == null || guestRoomSession.getBookingDetailId() == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Phiên đăng nhập phòng đã hết hạn. Vui lòng đăng nhập lại."
            );
            return "redirect:/guest/login";
        }

        try {
            Long bookingDetailId = guestRoomSession.getBookingDetailId();

            String selectedCategory = normalizeCategory(category);

            GuestMyBookingView myBooking =
                    guestBookingService.getMyBooking(bookingDetailId);

            model.addAttribute("guestSession", guestRoomSession);
            model.addAttribute("myBooking", myBooking);
            model.addAttribute("services", guestBookingService.getAvailableService(selectedCategory));
            model.addAttribute("selectedCategory", selectedCategory);

            return "guest/my_booking";

        } catch (IllegalArgumentException e) {
            session.removeAttribute(GuestSessionAdvice.GUEST_ROOM_SESSION);

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/guest/login";
        }
    }

    @PostMapping("/services/add")
    public String addService(@RequestParam(required = false) Long serviceId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        GuestRoomSession guestRoomSession = getGuestRoomSession(session);

        if (guestRoomSession == null || guestRoomSession.getBookingDetailId() == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Phiên đăng nhập phòng đã hết hạn. Vui lòng đăng nhập lại."
            );
            return "redirect:/guest/login";
        }

        if (serviceId == null || serviceId <= 0) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Dịch vụ không hợp lệ."
            );
            return "redirect:/guest/my-booking";
        }

        try {
            guestBookingService.addService(
                    guestRoomSession.getBookingDetailId(),
                    serviceId
            );


            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã thêm dịch vụ vào booking của bạn."
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/guest/my-booking";
    }

    @PostMapping("/services/increase")
    public String increaseService(@RequestParam(required = false) Long folioItemId,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {

        GuestRoomSession guestRoomSession = getGuestRoomSession(session);

        if (guestRoomSession == null || guestRoomSession.getBookingDetailId() == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Phiên đăng nhập phòng đã hết hạn. Vui lòng đăng nhập lại."
            );
            return "redirect:/guest/login";
        }

        if (folioItemId == null || folioItemId <= 0) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Dịch vụ đã chọn không hợp lệ."
            );
            return "redirect:/guest/my-booking";
        }

        try {
            guestBookingService.increaseService(
                    guestRoomSession.getBookingDetailId(),
                    folioItemId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã tăng số lượng dịch vụ."
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/guest/my-booking";
    }

    @PostMapping("/services/decrease")
    public String decrease(@RequestParam(required = false) Long folioItemId,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {

        GuestRoomSession guestRoomSession = getGuestRoomSession(session);

        if (guestRoomSession == null || guestRoomSession.getBookingDetailId() == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Phiên đăng nhập phòng đã hết hạn. Vui lòng đăng nhập lại."
            );
            return "redirect:/guest/login";
        }

        if (folioItemId == null || folioItemId <= 0) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Dịch vụ đã chọn không hợp lệ."
            );
            return "redirect:/guest/my-booking";
        }

        try {
            guestBookingService.decreaseService(
                    guestRoomSession.getBookingDetailId(),
                    folioItemId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã giảm số lượng dịch vụ."
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/guest/my-booking";
    }

    @PostMapping("/services/remove")
    public String remove(@RequestParam(required = false) Long folioItemId,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {

        GuestRoomSession guestRoomSession = getGuestRoomSession(session);

        if (guestRoomSession == null || guestRoomSession.getBookingDetailId() == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Phiên đăng nhập phòng đã hết hạn. Vui lòng đăng nhập lại."
            );
            return "redirect:/guest/login";
        }

        if (folioItemId == null || folioItemId <= 0) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Dịch vụ đã chọn không hợp lệ."
            );
            return "redirect:/guest/my-booking";
        }

        try {
            guestBookingService.removeService(
                    guestRoomSession.getBookingDetailId(),
                    folioItemId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã xóa dịch vụ khỏi booking của bạn."
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/guest/my-booking";
    }

    private GuestRoomSession getGuestRoomSession(HttpSession session) {
        if (session == null) {
            return null;
        }

        Object value = session.getAttribute(GuestSessionAdvice.GUEST_ROOM_SESSION);

        if (!(value instanceof GuestRoomSession)) {
            return null;
        }

        return (GuestRoomSession) value;
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "ALL";
        }

        String value = category.trim().toUpperCase();

        if (value.equals("FOOD")) {
            return "FOOD";
        }

        if (value.equals("SPA")) {
            return "SPA";
        }

        return "ALL";
    }
}
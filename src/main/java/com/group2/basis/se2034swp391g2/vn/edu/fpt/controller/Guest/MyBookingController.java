package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Guest;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Page.GuestSessionAdvice;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.GuestMyBookingView;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.GuestRoomSession;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.GuestBookingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import lombok.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/guest")
public class MyBookingController {
    private final GuestBookingService guestBookingService;

    @GetMapping("/my-booking")
    public String myBooking(@RequestParam(required = false) String category,
                            HttpSession session,
                            Model model) {
        GuestRoomSession guestRoomSession = (GuestRoomSession) session.getAttribute(GuestSessionAdvice.GUEST_ROOM_SESSION);

        if (guestRoomSession == null) {
            return "redirect:/guest/login";
        }

        GuestMyBookingView myBooking = guestBookingService.getMyBooking(guestRoomSession.getBookingDetailId());

        String selectedCategory = category == null || category.isBlank() ? "ALL" : category.trim().toUpperCase();
        model.addAttribute("guestSession", guestRoomSession);
        model.addAttribute("myBooking", myBooking);
        model.addAttribute("service", guestBookingService.getAvailableService(selectedCategory));
        model.addAttribute("selectedCategory", selectedCategory);

        return "guest/my_booking";
    }

    @PostMapping("/services/increase")
    public String increaseService(@RequestParam Long folioItemId,
                                  HttpSession session
    ) {
        GuestRoomSession guestRoomSession = (GuestRoomSession) session.getAttribute(GuestSessionAdvice.GUEST_ROOM_SESSION);
        if (guestRoomSession == null) {
            return "redirect:/guest/login";
        }

        guestBookingService.increaseService(guestRoomSession.getBookingId(), folioItemId);
        return "redirect:/guest/my-booking";
    }

    @PostMapping("/services/add")
    public String addService(@RequestParam Long serviceId,
                             HttpSession session
    ) {
        GuestRoomSession guestRoomSession = (GuestRoomSession) session.getAttribute(GuestSessionAdvice.GUEST_ROOM_SESSION);
        if (guestRoomSession == null) {
            return "redirect:/guest/login";
        }

        guestBookingService.addService(guestRoomSession.getBookingId(), serviceId);
        return "redirect:/guest/my-booking";
    }

    @PostMapping("/services/decrease")
    public String decrease(@RequestParam Long folioItemId,
                           HttpSession session
    ) {
        GuestRoomSession guestRoomSession = (GuestRoomSession) session.getAttribute(GuestSessionAdvice.GUEST_ROOM_SESSION);
        if (guestRoomSession == null) {
            return "redirect:/guest/login";
        }

        guestBookingService.decreaseService(guestRoomSession.getBookingId(), folioItemId);
        return "redirect:/guest/my-booking";
    }

    @PostMapping("/services/remove")
    public String remove(@RequestParam Long folioItemId,
                         HttpSession session
    ) {
        GuestRoomSession guestRoomSession = (GuestRoomSession) session.getAttribute(GuestSessionAdvice.GUEST_ROOM_SESSION);
        if (guestRoomSession == null) {
            return "redirect:/guest/login";
        }

        guestBookingService.removeService(guestRoomSession.getBookingId(), folioItemId);
        return "redirect:/guest/my-booking";
    }

}

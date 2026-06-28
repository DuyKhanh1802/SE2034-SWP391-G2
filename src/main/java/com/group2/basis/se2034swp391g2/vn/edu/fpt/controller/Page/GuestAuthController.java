package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Page;


import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.BookingDetail;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Room;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.GuestRoomSession;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingDetailRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.time.Instant;


@Controller
@RequiredArgsConstructor
@RequestMapping("/guest")
public class GuestAuthController {


    private final BookingDetailRepository bookingDetailRepository;


    @GetMapping("/login")
    public String showLoginPage(HttpSession session) {
        session.removeAttribute(GuestSessionAdvice.GUEST_ROOM_SESSION);
        return "page/GuestRoomCodeLogin";
    }


    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String roomCode,
                        HttpSession session,
                        Model model) {


        session.removeAttribute(GuestSessionAdvice.GUEST_ROOM_SESSION);


        String cleanEmail = email == null ? "" : email.trim();
        String cleanRoomCode = roomCode == null ? "" : roomCode.trim().toUpperCase();


        if (cleanEmail.isBlank() || cleanRoomCode.isBlank()) {
            model.addAttribute("errorMessage", "Vui lòng nhập email và mã phòng.");
            model.addAttribute("email", cleanEmail);
            model.addAttribute("roomCode", cleanRoomCode);
            return "page/GuestRoomCodeLogin";
        }


        BookingDetail detail = bookingDetailRepository
                .findValidGuestRoomAccess(cleanEmail, cleanRoomCode, Instant.now())
                .orElse(null);


        if (detail == null) {
            model.addAttribute("errorMessage", "Email hoặc mã phòng không hợp lệ, đã hết hạn hoặc phòng chưa được nhận.");
            model.addAttribute("email", cleanEmail);
            model.addAttribute("roomCode", cleanRoomCode);
            return "page/GuestRoomCodeLogin";
        }


        Booking booking = detail.getBooking();
        if (booking == null || Boolean.TRUE.equals(booking.getIsDeleted())) {
            model.addAttribute("errorMessage", "Booking không tồn tại hoặc đã bị huỷ.");
            model.addAttribute("email", cleanEmail);
            model.addAttribute("roomCode", cleanRoomCode);
            return "page/GuestRoomCodeLogin";
        }


        User guest = booking.getGuest();
        Room room = detail.getRoom();


        String guestName = (booking.getGuestLastName() + " " + booking.getGuestFirstName()).trim();


        if (guestName.isBlank()) {
            guestName = cleanEmail;
        }


        GuestRoomSession guestSession = GuestRoomSession.builder()
                .bookingId(booking.getId())
                .bookingDetailId(detail.getId())
                .roomId(room != null ? room.getId() : null)
                .guestId(guest != null ? guest.getId() : null)
                .roomCode(detail.getRoomCode())
                .roomNumber(room != null ? room.getRoomNumber() : null)
                .guestName(guestName)
                .guestEmail(booking.getGuestEmail())
                .bookingReference(booking.getBookingReference())
                .avatarUrl(guest != null ? guest.getAvatarUrl() : null)
                .roomCodeExpiresAt(detail.getRoomCodeExpiresAt())
                .build();
        session.setAttribute(GuestSessionAdvice.GUEST_ROOM_SESSION, guestSession);


        return "redirect:/page/my-booking";
    }


    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute(GuestSessionAdvice.GUEST_ROOM_SESSION);
        return "redirect:/page/home";
    }
}


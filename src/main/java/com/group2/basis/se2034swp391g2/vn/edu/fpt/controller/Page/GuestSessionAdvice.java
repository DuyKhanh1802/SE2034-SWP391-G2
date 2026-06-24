package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Page;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.GuestRoomSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GuestSessionAdvice {

    public static final String GUEST_ROOM_SESSION = "GUEST_ROOM_SESSION";

    @ModelAttribute
    public void addGuestSessionToModel(HttpSession session, Model model) {
        GuestRoomSession guestSession =
                (GuestRoomSession) session.getAttribute(GUEST_ROOM_SESSION);

        model.addAttribute("guestSession", guestSession);
    }
}
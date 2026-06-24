package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Page;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.Gender;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.ChangePasswordRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.ProfileUpdateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.GuestRoomSession;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.CountryRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class GuestPageController {

    private final UserRepository userRepository;
    private final CountryRepository countryRepository;
    private final ProfileService profileService;

    @GetMapping("/guest/profile")
    public String guestProfile(HttpSession session) {
        GuestRoomSession guestSession =
                (GuestRoomSession) session.getAttribute(GuestSessionAdvice.GUEST_ROOM_SESSION);

        if (guestSession == null) {
            return "redirect:/guest/login";
        }

        return "redirect:/profile/edit";
    }
}
package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Guest;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.Gender;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.exception.ProfileValidationException;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.ChangePasswordRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.ProfileUpdateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.CountryRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final CountryRepository countryRepository;

    @GetMapping("/edit")
    public String showEditProfile(Model model,
                                  Authentication authentication,
                                  HttpSession session) {

        User currentUser = profileService.resolveCurrentUser(authentication, session);

        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", profileService.toProfileUpdateRequest(currentUser));
        }

        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new ChangePasswordRequest());
        }

        addCommonAttributes(model, currentUser);
        return "profile/EditProfile";
    }

    @PostMapping("/update")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileUpdateRequest profileForm,
                                BindingResult bindingResult,
                                Model model,
                                Authentication authentication,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        User currentUser = profileService.resolveCurrentUser(authentication, session);

        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        profileForm.setGuest(profileService.isGuest(currentUser));

        try {
            profileService.validateProfileBusinessRules(profileForm);

            if (bindingResult.hasErrors()) {
                model.addAttribute("passwordForm", new ChangePasswordRequest());
                addCommonAttributes(model, currentUser);
                return "profile/EditProfile";
            }

            profileService.updateProfile(currentUser.getId(), profileForm);

        } catch (ProfileValidationException ex) {
            ex.getFieldErrors().forEach((field, message) ->
                    bindingResult.rejectValue(field, field + ".invalid", message)
            );

            model.addAttribute("passwordForm", new ChangePasswordRequest());
            addCommonAttributes(model, currentUser);
            return "profile/EditProfile";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("passwordForm", new ChangePasswordRequest());
            addCommonAttributes(model, currentUser);
            return "profile/EditProfile";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật profile thành công");
        return "redirect:/profile/edit";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") ChangePasswordRequest passwordForm,
                                 BindingResult bindingResult,
                                 Model model,
                                 Authentication authentication,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        User currentUser = profileService.resolveCurrentUser(authentication, session);

        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        if (profileService.isGuest(currentUser)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Tài khoản quý khách không thể đổi mật khẩu"
            );
            return "redirect:/profile/edit";
        }

        try {
            profileService.validateChangePasswordBusinessRules(passwordForm, currentUser);

            if (bindingResult.hasErrors()) {
                model.addAttribute("profileForm", profileService.toProfileUpdateRequest(currentUser));
                addCommonAttributes(model, currentUser);
                return "profile/EditProfile";
            }

            profileService.changePassword(currentUser.getId(), passwordForm);

        } catch (ProfileValidationException ex) {
            ex.getFieldErrors().forEach((field, message) ->
                    bindingResult.rejectValue(field, field + ".invalid", message)
            );

            model.addAttribute("profileForm", profileService.toProfileUpdateRequest(currentUser));
            addCommonAttributes(model, currentUser);
            return "profile/EditProfile";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("profileForm", profileService.toProfileUpdateRequest(currentUser));
            addCommonAttributes(model, currentUser);
            return "profile/EditProfile";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Thay đổi mật khẩu thành công");
        return "redirect:/profile/edit";
    }

    private void addCommonAttributes(Model model, User currentUser) {
        boolean isGuest = profileService.isGuest(currentUser);

        model.addAttribute("isGuest", isGuest);
        model.addAttribute("isStaff", !isGuest);
        model.addAttribute("genders", Gender.values());
        model.addAttribute("countries", countryRepository.findAllByOrderByCountryNameAsc());
        model.addAttribute("currentUser", currentUser);
    }
}
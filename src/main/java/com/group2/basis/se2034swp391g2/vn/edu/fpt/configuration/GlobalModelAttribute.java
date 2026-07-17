package com.group2.basis.se2034swp391g2.vn.edu.fpt.configuration;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.utils.DisplayUtils;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.CustomerUserDetails;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ReceptionistNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttribute {

    private final ReceptionistNotificationService receptionistNotificationService;

    public GlobalModelAttribute(ReceptionistNotificationService receptionistNotificationService) {
        this.receptionistNotificationService = receptionistNotificationService;
    }

    @ModelAttribute
    public void addCurrentUri(
            HttpServletRequest request,
            Authentication authentication,
            Model model) {

        model.addAttribute(
                "currentUri",
                request.getRequestURI()
        );

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomerUserDetails principal) {
            model.addAttribute("currentUser", principal.getUser());
            model.addAttribute("currentUserDisplayName", DisplayUtils.formatDisplayName(principal.getUser()));

            boolean isReceptionist = principal.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_RECEPTIONIST".equals(authority.getAuthority()));
            if (isReceptionist && "GET".equalsIgnoreCase(request.getMethod())) {
                model.addAttribute(
                        "receptionistNotifications",
                        receptionistNotificationService.getHeaderNotifications()
                );
            }
        }
    }
}

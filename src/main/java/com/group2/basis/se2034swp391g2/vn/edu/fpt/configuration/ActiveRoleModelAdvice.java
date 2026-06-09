package com.group2.basis.se2034swp391g2.vn.edu.fpt.configuration;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.RoleSwitchController;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Set;
import java.util.stream.Collectors;

@ControllerAdvice(annotations = Controller.class)
public class ActiveRoleModelAdvice {

    @ModelAttribute
    public void ensureCurrentActiveRole(Authentication authentication, HttpSession session) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return;
        }

        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        Object currentActiveRole = session.getAttribute(RoleSwitchController.CURRENT_ACTIVE_ROLE);
        RoleName activeRole = currentActiveRole != null
                ? RoleName.valueOf(currentActiveRole.toString())
                : RoleSwitchController.resolveDefaultActiveRole(roles);

        session.setAttribute(RoleSwitchController.CURRENT_ACTIVE_ROLE, activeRole.name());
        session.setAttribute(RoleSwitchController.CURRENT_ACTIVE_ROLE_LABEL, RoleSwitchController.getRoleLabel(activeRole));
        session.setAttribute(RoleSwitchController.AVAILABLE_ACTIVE_ROLE_OPTIONS, RoleSwitchController.getAvailableActiveRoleOptions(roles));
    }
}

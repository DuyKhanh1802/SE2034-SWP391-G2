package com.group2.basis.se2034swp391g2.vn.edu.fpt.configuration;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.RoleSwitchController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ActiveRoleAccessFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String activeRole = session != null
                ? (String) session.getAttribute(RoleSwitchController.CURRENT_ACTIVE_ROLE)
                : null;

        if (activeRole == null || !isManagedStaffPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        RoleName requiredRole = resolveRequiredRole(request.getRequestURI());
        if (requiredRole == null || requiredRole.name().equals(activeRole)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.sendRedirect(RoleSwitchController.getDashboardPath(RoleName.valueOf(activeRole)));
    }

    private boolean isManagedStaffPath(String uri) {
        return uri.startsWith("/admin/list-user")
                || uri.startsWith("/admin/dashboard")
                || uri.startsWith("/admin/list_room")
                || uri.startsWith("/admin/rooms")
                || uri.startsWith("/admin/room-images")
                || uri.startsWith("/admin/services")
                || uri.startsWith("/admin/promotions")
                || uri.startsWith("/manager")
                || uri.startsWith("/receptionist");
    }

    private RoleName resolveRequiredRole(String uri) {
        if (uri.startsWith("/admin/list-user")) {
            return RoleName.SYSTEM_ADMIN;
        }
        if (uri.startsWith("/admin/")) {
            return RoleName.HOTEL_ADMIN;
        }
        if (uri.startsWith("/manager")) {
            return RoleName.MANAGER;
        }
        if (uri.startsWith("/receptionist")) {
            return RoleName.RECEPTIONIST;
        }
        return null;
    }
}

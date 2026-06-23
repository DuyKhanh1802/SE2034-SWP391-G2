package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.HotelAdmin;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.ServiceRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ServiceResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ServiceManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/hotel-admin/services")
public class HotelAdminServiceController {

    private final ServiceManagementService serviceManagementService;
    private final ProfileService profileService;

    public HotelAdminServiceController(ServiceManagementService serviceManagementService,
                                       ProfileService profileService) {
        this.serviceManagementService = serviceManagementService;
        this.profileService = profileService;
    }

    private void addLayoutData(Model model,
                               Authentication authentication,
                               HttpSession session,
                               HttpServletRequest request,
                               String pageTitle) {
        User currentUser = profileService.resolveCurrentUser(authentication, session);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("pageTitle", pageTitle);
    }

    @GetMapping
    public String listServices(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "5") int size,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Long categoryId,
                               @RequestParam(defaultValue = "ALL") String availability,
                               Model model,
                               Authentication authentication,
                               HttpSession session,
                               HttpServletRequest request) {

        addLayoutData(model, authentication, session, request, "Quản lý dịch vụ");

        Page<ServiceResponse> servicePage = serviceManagementService.searchServices(
                keyword,
                categoryId,
                availability,
                PageRequest.of(page, size)
        );

        model.addAttribute("services", servicePage.getContent());
        model.addAttribute("categories", serviceManagementService.getServiceCategories());

        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", servicePage.getTotalPages());
        model.addAttribute("totalItems", servicePage.getTotalElements());

        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedAvailability", availability);

        return "hotel_admin/ListService";
    }

    @GetMapping("/add")
    public String showAddServiceForm(Model model,
                                     Authentication authentication,
                                     HttpSession session,
                                     HttpServletRequest request) {

        addLayoutData(model, authentication, session, request, "Thêm dịch vụ");

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setIsAvailable(true);

        model.addAttribute("serviceRequest", serviceRequest);
        model.addAttribute("categories", serviceManagementService.getServiceCategories());

        return "hotel_admin/AddService";
    }

    @PostMapping("/add")
    public String createService(@ModelAttribute("serviceRequest") ServiceRequest serviceRequest,
                                Model model,
                                Authentication authentication,
                                HttpSession session,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {

        try {
            serviceManagementService.createService(serviceRequest);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Thêm dịch vụ mới thành công."
            );

            return "redirect:/hotel-admin/services";

        } catch (Exception e) {
            addLayoutData(model, authentication, session, request, "Thêm dịch vụ");

            model.addAttribute("serviceRequest", serviceRequest);
            model.addAttribute("categories", serviceManagementService.getServiceCategories());
            model.addAttribute("errorMessage", e.getMessage());

            return "hotel_admin/AddService";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditServiceForm(@PathVariable Long id,
                                      Model model,
                                      Authentication authentication,
                                      HttpSession session,
                                      HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {

        try {
            addLayoutData(model, authentication, session, request, "Chỉnh sửa dịch vụ");

            ServiceRequest serviceRequest = serviceManagementService.getServiceForEdit(id);

            model.addAttribute("serviceRequest", serviceRequest);
            model.addAttribute("categories", serviceManagementService.getServiceCategories());

            return "hotel_admin/EditService";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/hotel-admin/services";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateService(@PathVariable Long id,
                                @ModelAttribute("serviceRequest") ServiceRequest serviceRequest,
                                Model model,
                                Authentication authentication,
                                HttpSession session,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {

        try {
            serviceManagementService.updateService(id, serviceRequest);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Cập nhật dịch vụ thành công."
            );

            return "redirect:/hotel-admin/services";

        } catch (Exception e) {
            addLayoutData(model, authentication, session, request, "Chỉnh sửa dịch vụ");

            serviceRequest.setId(id);

            try {
                ServiceRequest oldData = serviceManagementService.getServiceForEdit(id);
                serviceRequest.setCurrentImageUrl(oldData.getCurrentImageUrl());
            } catch (Exception ignored) {
                serviceRequest.setCurrentImageUrl(null);
            }

            model.addAttribute("serviceRequest", serviceRequest);
            model.addAttribute("categories", serviceManagementService.getServiceCategories());
            model.addAttribute("errorMessage", e.getMessage());

            return "hotel_admin/EditService";
        }
    }
    @PostMapping("/edit/{id}/delete")
    public String deleteService(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            serviceManagementService.deleteService(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Xóa dịch vụ thành công."
            );

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/hotel-admin/services";
    }

    @PostMapping("/{id}/toggle-availability")
    public String toggleAvailability(@PathVariable Long id,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "5") int size,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) Long categoryId,
                                     @RequestParam(defaultValue = "ALL") String availability,
                                     RedirectAttributes redirectAttributes) {

        try {
            serviceManagementService.toggleAvailability(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Cập nhật trạng thái dịch vụ thành công."
            );

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("size", size);

        if (keyword != null && !keyword.isBlank()) {
            redirectAttributes.addAttribute("keyword", keyword);
        }

        if (categoryId != null) {
            redirectAttributes.addAttribute("categoryId", categoryId);
        }

        redirectAttributes.addAttribute("availability", availability);

        return "redirect:/hotel-admin/services";
    }
}
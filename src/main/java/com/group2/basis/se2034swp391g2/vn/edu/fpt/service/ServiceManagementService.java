package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ImageEntityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.ServiceCategory;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ServiceResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ServiceCategoryRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@org.springframework.stereotype.Service
public class ServiceManagementService {

    private final ServiceRepository serviceRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;

    public ServiceManagementService(ServiceRepository serviceRepository,
                                    ServiceCategoryRepository serviceCategoryRepository) {
        this.serviceRepository = serviceRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
    }

    public Page<ServiceResponse> searchServices(String keyword,
                                                Long categoryId,
                                                String availability,
                                                Pageable pageable) {

        Boolean availabilityValue = parseAvailability(availability);

        return serviceRepository.searchServicesForAdmin(
                normalizeKeyword(keyword),
                categoryId,
                availabilityValue,
                ImageEntityType.SERVICE,
                pageable
        );
    }

    public List<ServiceCategory> getServiceCategories() {
        return serviceCategoryRepository.findByIsDeletedFalseOrderByNameAsc();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        return keyword.trim();
    }

    private Boolean parseAvailability(String availability) {
        if (availability == null || availability.isBlank() || availability.equalsIgnoreCase("ALL")) {
            return null;
        }

        if (availability.equalsIgnoreCase("AVAILABLE")) {
            return true;
        }

        if (availability.equalsIgnoreCase("UNAVAILABLE")) {
            return false;
        }

        return null;
    }
    public void toggleAvailability(Long serviceId) {
        com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service =
                serviceRepository.findById(serviceId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ"));

        if (Boolean.TRUE.equals(service.getIsDeleted())) {
            throw new IllegalArgumentException("Dịch vụ đã bị xóa");
        }

        boolean currentStatus = Boolean.TRUE.equals(service.getIsAvailable());
        service.setIsAvailable(!currentStatus);

        serviceRepository.save(service);
    }
}
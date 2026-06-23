package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ImageEntityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Image;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.ServiceCategory;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.ServiceRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ServiceResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ImageRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ServiceCategoryRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@org.springframework.stereotype.Service
public class ServiceManagementService {

    private final ServiceRepository serviceRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ImageRepository imageRepository;
    private final CloudinaryService cloudinaryService;

    public ServiceManagementService(ServiceRepository serviceRepository,
                                    ServiceCategoryRepository serviceCategoryRepository,
                                    ImageRepository imageRepository,
                                    CloudinaryService cloudinaryService) {
        this.serviceRepository = serviceRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.imageRepository = imageRepository;
        this.cloudinaryService = cloudinaryService;
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

    @Transactional
    public void createService(ServiceRequest request) {
        validateServiceRequest(request);

        ServiceCategory category = getValidCategory(request.getCategoryId());

        com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service =
                new com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service();

        service.setName(request.getName().trim());
        service.setDescription(normalizeText(request.getDescription()));
        service.setPrice(request.getPrice());
        service.setCategory(category);
        service.setIsAvailable(Boolean.TRUE.equals(request.getIsAvailable()));
        service.setIsDeleted(false);

        com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service savedService =
                serviceRepository.save(service);

        saveServiceImageIfPresent(savedService.getId(), request);
    }

    @Transactional(readOnly = true)
    public ServiceRequest getServiceForEdit(Long serviceId) {
        com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service =
                getValidService(serviceId);

        ServiceRequest request = new ServiceRequest();
        request.setId(service.getId());
        request.setName(service.getName());
        request.setDescription(service.getDescription());
        request.setPrice(service.getPrice());
        request.setIsAvailable(service.getIsAvailable());

        if (service.getCategory() != null) {
            request.setCategoryId(service.getCategory().getId());
        }

        imageRepository
                .findFirstByEntityTypeAndEntityIdAndIsPrimaryTrueOrderBySortOrderAsc(
                        ImageEntityType.SERVICE,
                        service.getId()
                )
                .ifPresent(image -> request.setCurrentImageUrl(image.getImageUrl()));

        return request;
    }

    @Transactional
    public void updateService(Long serviceId, ServiceRequest request) {
        validateServiceRequest(request);

        com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service =
                getValidService(serviceId);

        ServiceCategory category = getValidCategory(request.getCategoryId());

        service.setName(request.getName().trim());
        service.setDescription(normalizeText(request.getDescription()));
        service.setPrice(request.getPrice());
        service.setCategory(category);
        service.setIsAvailable(Boolean.TRUE.equals(request.getIsAvailable()));

        serviceRepository.save(service);

        updateServiceImageIfPresent(service.getId(), request);
    }

    @Transactional
    public void toggleAvailability(Long serviceId) {
        com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service =
                getValidService(serviceId);

        boolean currentStatus = Boolean.TRUE.equals(service.getIsAvailable());
        service.setIsAvailable(!currentStatus);

        serviceRepository.save(service);
    }
    @Transactional
    public void deleteService(Long serviceId) {
        com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service =
                getValidService(serviceId);

        service.setIsDeleted(true);
        service.setIsAvailable(false);

        serviceRepository.save(service);
    }

    private com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service getValidService(Long serviceId) {
        com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service =
                serviceRepository.findById(serviceId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ"));

        if (Boolean.TRUE.equals(service.getIsDeleted())) {
            throw new IllegalArgumentException("Dịch vụ đã bị xóa");
        }

        return service;
    }

    private ServiceCategory getValidCategory(Long categoryId) {
        ServiceCategory category = serviceCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Loại dịch vụ không tồn tại"));

        if (Boolean.TRUE.equals(category.getIsDeleted())) {
            throw new IllegalArgumentException("Loại dịch vụ đã bị xóa");
        }

        return category;
    }

    private void saveServiceImageIfPresent(Long serviceId, ServiceRequest request) {
        if (request.getImageFile() == null || request.getImageFile().isEmpty()) {
            return;
        }

        String imageUrl = cloudinaryService.uploadServiceImage(request.getImageFile());

        Image image = new Image();
        image.setEntityType(ImageEntityType.SERVICE);
        image.setEntityId(serviceId);
        image.setImageUrl(imageUrl);
        image.setIsPrimary(true);
        image.setSortOrder(1);

        imageRepository.save(image);
    }

    private void updateServiceImageIfPresent(Long serviceId, ServiceRequest request) {
        if (request.getImageFile() == null || request.getImageFile().isEmpty()) {
            return;
        }

        String imageUrl = cloudinaryService.uploadServiceImage(request.getImageFile());

        Image image = imageRepository
                .findFirstByEntityTypeAndEntityIdAndIsPrimaryTrueOrderBySortOrderAsc(
                        ImageEntityType.SERVICE,
                        serviceId
                )
                .orElseGet(Image::new);

        image.setEntityType(ImageEntityType.SERVICE);
        image.setEntityId(serviceId);
        image.setImageUrl(imageUrl);
        image.setIsPrimary(true);
        image.setSortOrder(1);

        imageRepository.save(image);
    }

    private void validateServiceRequest(ServiceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu dịch vụ không hợp lệ");
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên dịch vụ không được để trống");
        }

        if (request.getName().trim().length() > 200) {
            throw new IllegalArgumentException("Tên dịch vụ không được vượt quá 200 ký tự");
        }

        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn loại dịch vụ");
        }

        if (request.getPrice() == null) {
            throw new IllegalArgumentException("Giá dịch vụ không được để trống");
        }

        if (request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá dịch vụ không được nhỏ hơn 0");
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        return keyword.trim();
    }

    private String normalizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        return text.trim();
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
}
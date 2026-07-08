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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@org.springframework.stereotype.Service
public class ServiceManagementService {

    private static final int MAX_SERVICE_NAME_LENGTH = 200;
    private static final int MAX_SERVICE_DESCRIPTION_LENGTH = 500;
    private static final BigDecimal MAX_SERVICE_PRICE = new BigDecimal("100000000");
    private static final long MAX_SERVICE_IMAGE_SIZE = 5L * 1024L * 1024L;

    private static final Set<String> ALLOWED_SERVICE_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private static final Set<String> ALLOWED_SERVICE_IMAGE_EXTENSIONS = Set.of(
            ".jpg",
            ".jpeg",
            ".png",
            ".webp"
    );

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
        validateServiceRequest(request, null);

        ServiceCategory category = getValidCategory(request.getCategoryId());

        com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service =
                new com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service();

        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setPrice(request.getPrice());
        service.setCategory(category);
        service.setIsAvailable(request.getIsAvailable());
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

    @Transactional(readOnly = true)
    public ServiceResponse getServiceDetail(Long serviceId) {
        com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service =
                getValidService(serviceId);

        Long categoryId = null;
        String categoryName = "Chưa có loại";

        if (service.getCategory() != null) {
            categoryId = service.getCategory().getId();
            categoryName = service.getCategory().getName();
        }

        String imageUrl = imageRepository
                .findFirstByEntityTypeAndEntityIdAndIsPrimaryTrueOrderBySortOrderAsc(
                        ImageEntityType.SERVICE,
                        service.getId()
                )
                .map(Image::getImageUrl)
                .orElse(null);

        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getPrice(),
                service.getIsAvailable(),
                categoryId,
                categoryName,
                imageUrl
        );
    }

    @Transactional
    public void updateService(Long serviceId, ServiceRequest request) {
        validateServiceRequest(request, serviceId);

        com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service =
                getValidService(serviceId);

        ServiceCategory category = getValidCategory(request.getCategoryId());

        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setPrice(request.getPrice());
        service.setCategory(category);
        service.setIsAvailable(request.getIsAvailable());

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
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ."));

        if (Boolean.TRUE.equals(service.getIsDeleted())) {
            throw new IllegalArgumentException("Dịch vụ đã bị xóa.");
        }

        return service;
    }

    private ServiceCategory getValidCategory(Long categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Vui lòng chọn loại dịch vụ.");
        }

        ServiceCategory category = serviceCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Loại dịch vụ không tồn tại."));

        if (Boolean.TRUE.equals(category.getIsDeleted())) {
            throw new IllegalArgumentException("Loại dịch vụ đã bị xóa.");
        }

        return category;
    }

    private void saveServiceImageIfPresent(Long serviceId, ServiceRequest request) {
        MultipartFile imageFile = request.getImageFile();

        if (imageFile == null || imageFile.isEmpty()) {
            return;
        }

        String imageUrl = uploadServiceImageSafely(imageFile);

        Image image = new Image();
        image.setEntityType(ImageEntityType.SERVICE);
        image.setEntityId(serviceId);
        image.setImageUrl(imageUrl);
        image.setIsPrimary(true);
        image.setSortOrder(1);

        imageRepository.save(image);
    }

    private void updateServiceImageIfPresent(Long serviceId, ServiceRequest request) {
        MultipartFile imageFile = request.getImageFile();

        if (imageFile == null || imageFile.isEmpty()) {
            return;
        }

        String imageUrl = uploadServiceImageSafely(imageFile);

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

    private String uploadServiceImageSafely(MultipartFile imageFile) {
        try {
            return cloudinaryService.uploadServiceImage(imageFile);
        } catch (Exception e) {
            throw new IllegalStateException("Tải ảnh lên Cloudinary thất bại.", e);
        }
    }

    private void validateServiceRequest(ServiceRequest request, Long editingServiceId) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu dịch vụ không hợp lệ.");
        }

        String normalizedName = normalizeRequiredText(request.getName());

        if (normalizedName == null) {
            throw new IllegalArgumentException("Vui lòng nhập tên dịch vụ.");
        }

        if (normalizedName.length() > MAX_SERVICE_NAME_LENGTH) {
            throw new IllegalArgumentException("Tên dịch vụ không được vượt quá 200 ký tự.");
        }

        boolean duplicatedName = editingServiceId == null
                ? serviceRepository.existsActiveByNormalizedName(normalizedName)
                : serviceRepository.existsActiveByNormalizedNameAndIdNot(normalizedName, editingServiceId);

        if (duplicatedName) {
            throw new IllegalArgumentException("Tên dịch vụ đã tồn tại.");
        }

        request.setName(normalizedName);

        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn loại dịch vụ.");
        }

        if (request.getPrice() == null) {
            throw new IllegalArgumentException("Vui lòng nhập giá dịch vụ.");
        }

        if (request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá dịch vụ phải lớn hơn 0.");
        }

        if (request.getPrice().compareTo(MAX_SERVICE_PRICE) > 0) {
            throw new IllegalArgumentException("Giá dịch vụ không được vượt quá 100,000,000 VND.");
        }

        if (!isWholeNumber(request.getPrice())) {
            throw new IllegalArgumentException("Giá dịch vụ phải là số nguyên VND.");
        }

        request.setPrice(request.getPrice().setScale(0));

        String normalizedDescription = normalizeOptionalText(request.getDescription());

        if (normalizedDescription != null && normalizedDescription.length() > MAX_SERVICE_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Mô tả dịch vụ không được vượt quá 500 ký tự.");
        }

        request.setDescription(normalizedDescription);

        if (request.getIsAvailable() == null) {
            request.setIsAvailable(true);
        }

        validateServiceImageIfPresent(request.getImageFile());
    }

    private void validateServiceImageIfPresent(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return;
        }

        if (imageFile.getSize() > MAX_SERVICE_IMAGE_SIZE) {
            throw new IllegalArgumentException("Kích thước ảnh không được vượt quá 5MB.");
        }

        String contentType = imageFile.getContentType();
        String normalizedContentType = contentType == null
                ? ""
                : contentType.toLowerCase(Locale.ROOT).trim();

        String originalFilename = imageFile.getOriginalFilename();
        String normalizedFilename = originalFilename == null
                ? ""
                : originalFilename.toLowerCase(Locale.ROOT).trim();

        boolean allowedContentType = ALLOWED_SERVICE_IMAGE_CONTENT_TYPES.contains(normalizedContentType);
        boolean allowedExtension = ALLOWED_SERVICE_IMAGE_EXTENSIONS.stream()
                .anyMatch(normalizedFilename::endsWith);

        if (!allowedContentType || !allowedExtension) {
            throw new IllegalArgumentException("File tải lên phải là ảnh JPG, JPEG, PNG hoặc WEBP.");
        }
    }

    private boolean isWholeNumber(BigDecimal value) {
        return value.stripTrailingZeros().scale() <= 0;
    }

    private String normalizeRequiredText(String text) {
        if (text == null) {
            return null;
        }

        String normalizedText = text.trim();

        if (normalizedText.isEmpty()) {
            return null;
        }

        return normalizedText;
    }

    private String normalizeOptionalText(String text) {
        if (text == null) {
            return null;
        }

        String normalizedText = text.trim();

        if (normalizedText.isEmpty()) {
            return null;
        }

        return normalizedText;
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
}
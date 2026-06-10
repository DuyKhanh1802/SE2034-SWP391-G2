package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Promotion;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.PromotionRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.PromotionListResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.PromotionResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.PromotionRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PromotionService {

    private static final long MAX_PROMOTION_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final DateTimeFormatter FORM_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final PromotionRepository promotionRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CloudinaryService cloudinaryService;

    public PromotionService(PromotionRepository promotionRepository,
                            UserRepository userRepository,
                            BookingRepository bookingRepository,
                            CloudinaryService cloudinaryService) {
        this.promotionRepository = promotionRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public PromotionListResponse getPromotionListResponse(int page,
                                                          int size,
                                                          String keyword,
                                                          String status) {
        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = 5;
        }

        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        String selectedStatus = status == null || status.isBlank() ? "all" : status;

        List<PromotionResponse> allPromotions = promotionRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Promotion::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();

        long totalPromotions = allPromotions.size();

        long activePromotions = allPromotions.stream()
                .filter(promotion -> "Đang hoạt động".equals(promotion.getStatus()))
                .count();

        long scheduledPromotions = allPromotions.stream()
                .filter(promotion -> "Sắp diễn ra".equals(promotion.getStatus()))
                .count();

        long expiredPromotions = allPromotions.stream()
                .filter(promotion -> "Đã hết hạn".equals(promotion.getStatus()))
                .count();

        long inactivePromotions = allPromotions.stream()
                .filter(promotion -> "Đã tắt".equals(promotion.getStatus()))
                .count();

        List<PromotionResponse> filteredPromotions = allPromotions.stream()
                .filter(promotion -> matchKeyword(promotion, normalizedKeyword))
                .filter(promotion -> matchStatus(promotion, selectedStatus))
                .toList();

        int totalFiltered = filteredPromotions.size();
        int totalPages = (int) Math.ceil((double) totalFiltered / size);

        if (totalPages > 0 && page >= totalPages) {
            page = totalPages - 1;
        }

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalFiltered);

        List<PromotionResponse> pagedPromotions =
                totalFiltered == 0 ? List.of() : filteredPromotions.subList(fromIndex, toIndex);

        return new PromotionListResponse(
                pagedPromotions,
                totalPromotions,
                activePromotions,
                scheduledPromotions,
                expiredPromotions,
                inactivePromotions,
                page,
                totalPages,
                size,
                page > 0,
                page < totalPages - 1
        );
    }

    public PromotionResponse getPromotionDetail(Long id) {
        return toResponse(getPromotionEntity(id));
    }

    public PromotionRequest getPromotionEditRequest(Long id) {
        Promotion promotion = getPromotionEntity(id);
        PromotionRequest request = new PromotionRequest();
        request.setName(promotion.getName());
        request.setDescription(promotion.getDescription());
        request.setDiscountAmount(normalizeEditableAmount(promotion.getDiscountAmount()));
        request.setUsageLimit(promotion.getUsageLimit());
        request.setShowOnHomepage(Boolean.TRUE.equals(promotion.getShowOnHomepage()));
        request.setFeatured(Boolean.TRUE.equals(promotion.getFeatured()));
        request.setImageUrl(promotion.getImageUrl());
        request.setImagePublicId(promotion.getImagePublicId());
        request.setValidFromInput(formatForDateTimeInput(promotion.getValidFrom()));
        request.setValidToInput(formatForDateTimeInput(promotion.getValidTo()));
        return request;
    }

    public Map<String, String> uploadPromotionImage(MultipartFile file) {
        validatePromotionImage(file);

        Map<?, ?> uploadResult = cloudinaryService.uploadPromotionImage(file);
        Object secureUrl = uploadResult.get("secure_url");
        Object publicId = uploadResult.get("public_id");

        if (secureUrl == null || publicId == null) {
            throw new IllegalArgumentException("Không lấy được thông tin ảnh sau khi upload.");
        }

        return Map.of(
                "imageUrl", secureUrl.toString(),
                "imagePublicId", publicId.toString()
        );
    }

    public void createPromotion(PromotionRequest request) {
        validatePromotionRequest(request, null, false);

        User currentUser = getCurrentUser();
        Promotion promotion = new Promotion();
        promotion.setCode(generatePromotionCode());
        promotion.setCreatedBy(currentUser);
        promotion.setCreatedAt(Instant.now());

        applyPromotionRequest(promotion, request);
        promotion.setUsageCount(0);

        promotionRepository.save(promotion);
    }

    public void updatePromotion(Long id, PromotionRequest request) {
        Promotion promotion = getPromotionEntity(id);
        validatePromotionRequest(request, promotion.getId(), true);
        applyPromotionRequest(promotion, request);
        promotionRepository.save(promotion);
    }

    public void deletePromotion(Long id) {
        Promotion promotion = getPromotionEntity(id);

        if (bookingRepository.existsByPromotion_IdAndIsDeletedFalse(id)) {
            throw new IllegalArgumentException("Không thể xóa khuyến mãi đã được áp dụng trong đơn đặt phòng.");
        }

        promotionRepository.delete(promotion);
    }

    private void applyPromotionRequest(Promotion promotion, PromotionRequest request) {
        promotion.setName(request.getName().trim());
        promotion.setDescription(normalizeDescription(request.getDescription()));
        promotion.setDiscountAmount(request.getDiscountAmount());
        promotion.setUsageLimit(request.getUsageLimit());
        promotion.setValidFrom(convertToInstant(request.getValidFromInput()));
        promotion.setValidTo(convertToInstant(request.getValidToInput()));
        promotion.setShowOnHomepage(resolveShowOnHomepage(request));
        promotion.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
        promotion.setImageUrl(normalizeImageUrl(request.getImageUrl()));
        promotion.setImagePublicId(normalizeImagePublicId(request.getImagePublicId()));

        if (promotion.getIsActive() == null) {
            promotion.setIsActive(true);
        }
    }

    private PromotionResponse toResponse(Promotion promotion) {
        return PromotionResponse.builder()
                .id(promotion.getId())
                .code(promotion.getCode())
                .name(promotion.getName())
                .description(promotion.getDescription())
                .discountAmount(promotion.getDiscountAmount())
                .usageLimit(promotion.getUsageLimit())
                .usageCount(promotion.getUsageCount())
                .validFrom(promotion.getValidFrom())
                .validTo(promotion.getValidTo())
                .isActive(promotion.getIsActive())
                .showOnHomepage(promotion.getShowOnHomepage())
                .featured(promotion.getFeatured())
                .imageUrl(promotion.getImageUrl())
                .imagePublicId(promotion.getImagePublicId())
                .createdAt(promotion.getCreatedAt())
                .status(getPromotionStatus(promotion))
                .displayStatus(getDisplayStatus(promotion))
                .build();
    }

    private Promotion getPromotionEntity(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Khuyến mãi không hợp lệ.");
        }

        return promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi."));
    }

    private String getPromotionStatus(Promotion promotion) {
        Instant now = Instant.now();

        if (Boolean.FALSE.equals(promotion.getIsActive())) {
            return "Đã tắt";
        }

        if (promotion.getValidFrom() != null && promotion.getValidFrom().isAfter(now)) {
            return "Sắp diễn ra";
        }

        if (promotion.getValidTo() != null && promotion.getValidTo().isBefore(now)) {
            return "Đã hết hạn";
        }

        return "Đang hoạt động";
    }

    private String getDisplayStatus(Promotion promotion) {
        if (Boolean.TRUE.equals(promotion.getFeatured())) {
            return "Banner nổi bật";
        }

        if (Boolean.TRUE.equals(promotion.getShowOnHomepage())) {
            return "Hiển thị trang chủ";
        }

        return "Ẩn";
    }

    private void validatePromotionRequest(PromotionRequest request, Long promotionId, boolean isEditMode) {
        validateName(request);
        validateDescription(request);
        validateDiscountAmount(request);
        validateUsageLimit(request);
        validateDateInput(request, isEditMode);
        validateUploadedImage(request);
        validateFeaturedBanner(request, promotionId);
    }

    private void validatePromotionImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh khuyến mãi.");
        }

        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("File tải lên phải là ảnh.");
        }

        if (file.getSize() > MAX_PROMOTION_IMAGE_SIZE) {
            throw new IllegalArgumentException("Kích thước ảnh không được vượt quá 5MB.");
        }
    }

    private void validateName(PromotionRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Tên chiến dịch không được để trống.");
        }

        if (request.getName().trim().length() > 200) {
            throw new IllegalArgumentException("Tên chiến dịch không được vượt quá 200 ký tự.");
        }
    }

    private void validateDescription(PromotionRequest request) {
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("Mô tả ngắn không được để trống.");
        }

        if (request.getDescription().trim().length() > 300) {
            throw new IllegalArgumentException("Mô tả không được vượt quá 300 ký tự.");
        }
    }

    private void validateDiscountAmount(PromotionRequest request) {
        if (request.getDiscountAmount() == null
                || request.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền giảm phải lớn hơn 0.");
        }
    }

    private void validateUsageLimit(PromotionRequest request) {
        if (request.getUsageLimit() == null || request.getUsageLimit() <= 0) {
            throw new IllegalArgumentException("Giới hạn lượt dùng phải lớn hơn 0.");
        }
    }

    private void validateDateInput(PromotionRequest request, boolean isEditMode) {
        if (request.getValidFromInput() == null || request.getValidFromInput().isBlank()) {
            throw new IllegalArgumentException("Ngày bắt đầu không được để trống.");
        }

        if (request.getValidToInput() == null || request.getValidToInput().isBlank()) {
            throw new IllegalArgumentException("Ngày kết thúc không được để trống.");
        }

        Instant validFrom = convertToInstant(request.getValidFromInput());
        Instant validTo = convertToInstant(request.getValidToInput());

        if (!isEditMode) {
            LocalDate today = LocalDate.now();
            LocalDate validFromDate = validFrom.atZone(ZoneId.systemDefault()).toLocalDate();

            if (validFromDate.isBefore(today)) {
                throw new IllegalArgumentException("Ngày bắt đầu không được nhỏ hơn ngày hiện tại.");
            }
        }

        if (!validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu.");
        }
    }

    private void validateUploadedImage(PromotionRequest request) {
        if (request.getImageUrl() == null || request.getImageUrl().isBlank()
                || request.getImagePublicId() == null || request.getImagePublicId().isBlank()) {
            throw new IllegalArgumentException("Vui lòng tải ảnh khuyến mãi thành công trước khi lưu.");
        }
    }

    private void validateFeaturedBanner(PromotionRequest request, Long promotionId) {
        if (!Boolean.TRUE.equals(request.getFeatured())) {
            return;
        }

        boolean hasActiveFeaturedPromotion = promotionId == null
                ? promotionRepository.existsByFeaturedTrueAndIsActiveTrueAndValidToAfter(Instant.now())
                : promotionRepository.existsByFeaturedTrueAndIsActiveTrueAndValidToAfterAndIdNot(Instant.now(), promotionId);

        if (hasActiveFeaturedPromotion) {
            throw new IllegalArgumentException(
                    "Hiện đã có một khuyến mãi được đặt làm banner nổi bật. Vui lòng tắt banner hiện tại trước khi chọn chiến dịch này."
            );
        }
    }

    private Boolean resolveShowOnHomepage(PromotionRequest request) {
        if (Boolean.TRUE.equals(request.getFeatured())) {
            return true;
        }

        return Boolean.TRUE.equals(request.getShowOnHomepage());
    }

    private String generatePromotionCode() {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String code;

        do {
            String randomPart = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 4)
                    .toUpperCase();

            code = "VH-" + yearMonth + "-" + randomPart;
        } while (promotionRepository.existsByCode(code));

        return code;
    }

    private Instant convertToInstant(String dateTimeInput) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeInput);
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            throw new IllegalArgumentException("Định dạng ngày giờ không hợp lệ.");
        }
    }

    private String formatForDateTimeInput(Instant instant) {
        if (instant == null) {
            return "";
        }

        return instant.atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(FORM_DATE_TIME_FORMATTER);
    }

    private String normalizeDescription(String description) {
        return description.trim();
    }

    private String normalizeImageUrl(String imageUrl) {
        return imageUrl.trim();
    }

    private String normalizeImagePublicId(String imagePublicId) {
        return imagePublicId.trim();
    }

    private BigDecimal normalizeEditableAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }

        BigDecimal normalized = amount.stripTrailingZeros();

        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }

        return normalized;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Người dùng chưa đăng nhập.");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng hiện tại."));
    }

    private boolean matchKeyword(PromotionResponse promotion, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        String code = promotion.getCode() == null ? "" : promotion.getCode().toLowerCase();
        String name = promotion.getName() == null ? "" : promotion.getName().toLowerCase();

        return code.contains(keyword) || name.contains(keyword);
    }

    private boolean matchStatus(PromotionResponse promotion, String status) {
        if ("all".equals(status)) {
            return true;
        }

        if ("active".equals(status)) {
            return "Đang hoạt động".equals(promotion.getStatus());
        }

        if ("scheduled".equals(status)) {
            return "Sắp diễn ra".equals(promotion.getStatus());
        }

        if ("expired".equals(status)) {
            return "Đã hết hạn".equals(promotion.getStatus());
        }

        if ("inactive".equals(status)) {
            return "Đã tắt".equals(promotion.getStatus());
        }

        return true;
    }
}

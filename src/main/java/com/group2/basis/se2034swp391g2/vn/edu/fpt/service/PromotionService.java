package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Promotion;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.PromotionRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.PromotionListResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.PromotionResponse;
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

    /*
     * Dung lượng tối đa ảnh khuyến mãi.
     */
    private static final long MAX_PROMOTION_IMAGE_SIZE = 5 * 1024 * 1024;

    private final PromotionRepository promotionRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public PromotionService(PromotionRepository promotionRepository,
                            UserRepository userRepository,
                            CloudinaryService cloudinaryService) {
        this.promotionRepository = promotionRepository;
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
    }

    /*
     * Lấy danh sách khuyến mãi có tìm kiếm, lọc trạng thái và phân trang.
     */
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

    /*
     * Upload ảnh khuyến mãi lên Cloudinary.
     */
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

    /*
     * Tạo mới chiến dịch khuyến mãi.
     */
    public void createPromotion(PromotionRequest request) {
        validatePromotionRequest(request);

        User currentUser = getCurrentUser();

        Promotion promotion = new Promotion();
        promotion.setCode(generatePromotionCode());
        promotion.setName(request.getName().trim());
        promotion.setDescription(normalizeDescription(request.getDescription()));
        promotion.setDiscountAmount(request.getDiscountAmount());
        promotion.setUsageLimit(request.getUsageLimit());
        promotion.setUsageCount(0);
        promotion.setValidFrom(convertToInstant(request.getValidFromInput()));
        promotion.setValidTo(convertToInstant(request.getValidToInput()));
        promotion.setIsActive(true);
        promotion.setShowOnHomepage(resolveShowOnHomepage(request));
        promotion.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
        promotion.setImageUrl(normalizeImageUrl(request.getImageUrl()));
        promotion.setImagePublicId(normalizeImagePublicId(request.getImagePublicId()));
        promotion.setCreatedBy(currentUser);
        promotion.setCreatedAt(Instant.now());

        promotionRepository.save(promotion);
    }

    /*
     * Chuyển Entity sang DTO để hiển thị.
     */
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

    /*
     * Tính trạng thái hoạt động của khuyến mãi.
     */
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

    /*
     * Tính trạng thái hiển thị của khuyến mãi.
     */
    private String getDisplayStatus(Promotion promotion) {
        if (Boolean.TRUE.equals(promotion.getFeatured())) {
            return "Banner nổi bật";
        }

        if (Boolean.TRUE.equals(promotion.getShowOnHomepage())) {
            return "Hiển thị trang chủ";
        }

        return "Ẩn";
    }

    /*
     * Validate dữ liệu tạo khuyến mãi.
     */
    private void validatePromotionRequest(PromotionRequest request) {
        validateName(request);
        validateDescription(request);
        validateDiscountAmount(request);
        validateUsageLimit(request);
        validateDateInput(request);
        validateUploadedImage(request);
        validateFeaturedBanner(request);
    }

    /*
     * Kiểm tra file ảnh khuyến mãi.
     */
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

    /*
     * Kiểm tra tên chiến dịch.
     */
    private void validateName(PromotionRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Tên chiến dịch không được để trống.");
        }

        if (request.getName().trim().length() > 200) {
            throw new IllegalArgumentException("Tên chiến dịch không được vượt quá 200 ký tự.");
        }
    }

    /*
     * Kiểm tra mô tả chiến dịch.
     */
    private void validateDescription(PromotionRequest request) {
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("Mô tả ngắn không được để trống.");
        }

        if (request.getDescription().trim().length() > 300) {
            throw new IllegalArgumentException("Mô tả không được vượt quá 300 ký tự.");
        }
    }

    /*
     * Kiểm tra số tiền giảm.
     */
    private void validateDiscountAmount(PromotionRequest request) {
        if (request.getDiscountAmount() == null
                || request.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền giảm phải lớn hơn 0.");
        }
    }

    /*
     * Kiểm tra giới hạn lượt dùng.
     */
    private void validateUsageLimit(PromotionRequest request) {
        if (request.getUsageLimit() == null || request.getUsageLimit() <= 0) {
            throw new IllegalArgumentException("Giới hạn lượt dùng phải lớn hơn 0.");
        }
    }

    /*
     * Kiểm tra thời gian áp dụng.
     */
    private void validateDateInput(PromotionRequest request) {
        if (request.getValidFromInput() == null || request.getValidFromInput().isBlank()) {
            throw new IllegalArgumentException("Ngày bắt đầu không được để trống.");
        }

        if (request.getValidToInput() == null || request.getValidToInput().isBlank()) {
            throw new IllegalArgumentException("Ngày kết thúc không được để trống.");
        }

        Instant validFrom = convertToInstant(request.getValidFromInput());
        Instant validTo = convertToInstant(request.getValidToInput());

        LocalDate today = LocalDate.now();
        LocalDate validFromDate = validFrom.atZone(ZoneId.systemDefault()).toLocalDate();

        if (validFromDate.isBefore(today)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được nhỏ hơn ngày hiện tại.");
        }

        if (!validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu.");
        }
    }

    /*
     * Kiểm tra ảnh đã upload thành công.
     */
    private void validateUploadedImage(PromotionRequest request) {
        if (request.getImageUrl() == null || request.getImageUrl().isBlank()
                || request.getImagePublicId() == null || request.getImagePublicId().isBlank()) {
            throw new IllegalArgumentException("Vui lòng tải ảnh khuyến mãi thành công trước khi lưu.");
        }
    }

    /*
     * Chỉ cho phép một banner nổi bật đang hoạt động.
     */
    private void validateFeaturedBanner(PromotionRequest request) {
        if (!Boolean.TRUE.equals(request.getFeatured())) {
            return;
        }

        boolean hasActiveFeaturedPromotion =
                promotionRepository.existsByFeaturedTrueAndIsActiveTrueAndValidToAfter(Instant.now());

        if (hasActiveFeaturedPromotion) {
            throw new IllegalArgumentException(
                    "Hiện đã có một khuyến mãi được đặt làm banner nổi bật. Vui lòng tắt banner hiện tại trước khi chọn chiến dịch này."
            );
        }
    }

    /*
     * Nếu là banner nổi bật thì tự động hiển thị ở trang chủ.
     */
    private Boolean resolveShowOnHomepage(PromotionRequest request) {
        if (Boolean.TRUE.equals(request.getFeatured())) {
            return true;
        }

        return Boolean.TRUE.equals(request.getShowOnHomepage());
    }

    /*
     * Sinh mã khuyến mãi tự động.
     */
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

    /*
     * Chuyển datetime-local sang Instant.
     */
    private Instant convertToInstant(String dateTimeInput) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeInput);
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            throw new IllegalArgumentException("Định dạng ngày giờ không hợp lệ.");
        }
    }

    /*
     * Chuẩn hóa mô tả.
     */
    private String normalizeDescription(String description) {
        return description.trim();
    }

    /*
     * Chuẩn hóa URL ảnh.
     */
    private String normalizeImageUrl(String imageUrl) {
        return imageUrl.trim();
    }

    /*
     * Chuẩn hóa public id ảnh.
     */
    private String normalizeImagePublicId(String imagePublicId) {
        return imagePublicId.trim();
    }

    /*
     * Lấy manager đang đăng nhập.
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Người dùng chưa đăng nhập.");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng hiện tại."));
    }

    /*
     * Kiểm tra khuyến mãi có khớp từ khóa tìm kiếm không.
     */
    private boolean matchKeyword(PromotionResponse promotion, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        String code = promotion.getCode() == null ? "" : promotion.getCode().toLowerCase();
        String name = promotion.getName() == null ? "" : promotion.getName().toLowerCase();

        return code.contains(keyword) || name.contains(keyword);
    }

    /*
     * Kiểm tra khuyến mãi có khớp tab trạng thái không.
     */
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
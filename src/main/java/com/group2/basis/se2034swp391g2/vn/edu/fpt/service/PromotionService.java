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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final UserRepository userRepository;

    public PromotionService(PromotionRepository promotionRepository,
                            UserRepository userRepository) {
        this.promotionRepository = promotionRepository;
        this.userRepository = userRepository;
    }

    /*
     * Lấy danh sách khuyến mãi cho màn hình quản lý.
     * Đồng thời tính tổng số khuyến mãi theo từng trạng thái.
     */
    public PromotionListResponse getPromotionListResponse() {
        List<PromotionResponse> promotions = promotionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();

        long totalPromotions = promotions.size();

        long activePromotions = promotions.stream()
                .filter(promotion -> "Đang hoạt động".equals(promotion.getStatus()))
                .count();

        long scheduledPromotions = promotions.stream()
                .filter(promotion -> "Sắp diễn ra".equals(promotion.getStatus()))
                .count();

        long expiredPromotions = promotions.stream()
                .filter(promotion -> "Đã hết hạn".equals(promotion.getStatus()))
                .count();

        long inactivePromotions = promotions.stream()
                .filter(promotion -> "Đã tắt".equals(promotion.getStatus()))
                .count();

        return new PromotionListResponse(
                promotions,
                totalPromotions,
                activePromotions,
                scheduledPromotions,
                expiredPromotions,
                inactivePromotions
        );
    }

    /*
     * Tạo mới một chiến dịch khuyến mãi.
     * Ảnh đã được upload riêng lên Cloudinary trước đó.
     * Service chỉ lưu imageUrl và imagePublicId vào database.
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
        promotion.setShowOnHomepage(Boolean.TRUE.equals(request.getShowOnHomepage()));
        promotion.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
        promotion.setImageUrl(normalizeImageUrl(request.getImageUrl()));
        promotion.setImagePublicId(normalizeImagePublicId(request.getImagePublicId()));
        promotion.setCreatedBy(currentUser);
        promotion.setCreatedAt(Instant.now());

        promotionRepository.save(promotion);
    }

    /*
     * Chuyển dữ liệu từ Entity sang DTO để hiển thị lên giao diện.
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
     * Tính trạng thái khuyến mãi dựa trên thời gian và trạng thái bật/tắt.
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
     * Tính trạng thái hiển thị của khuyến mãi trên trang chủ.
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
     * Gom toàn bộ validate nghiệp vụ ở service.
     */
    private void validatePromotionRequest(PromotionRequest request) {
        validateName(request);
        validateDescription(request);
        validateDiscountAmount(request);
        validateUsageLimit(request);
        validateDateInput(request);
    }

    /*
     * Tên chiến dịch là bắt buộc.
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
     * Mô tả là tùy chọn, nhưng không được quá dài.
     */
    private void validateDescription(PromotionRequest request) {
        if (request.getDescription() != null
                && request.getDescription().trim().length() > 300) {
            throw new IllegalArgumentException("Mô tả không được vượt quá 300 ký tự.");
        }
    }

    /*
     * Khuyến mãi chỉ giảm theo số tiền cố định.
     */
    private void validateDiscountAmount(PromotionRequest request) {
        if (request.getDiscountAmount() == null
                || request.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền giảm phải lớn hơn 0.");
        }
    }

    /*
     * Giới hạn lượt dùng phải lớn hơn 0.
     */
    private void validateUsageLimit(PromotionRequest request) {
        if (request.getUsageLimit() == null || request.getUsageLimit() <= 0) {
            throw new IllegalArgumentException("Giới hạn lượt dùng phải lớn hơn 0.");
        }
    }

    /*
     * Kiểm tra thời gian áp dụng khuyến mãi.
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
     * Sinh mã khuyến mãi tự động.
     * Ví dụ: VH-202606-A8K2
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
     * Chuyển dữ liệu datetime-local từ form HTML sang Instant để lưu database.
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
     * Chuẩn hóa mô tả trước khi lưu.
     */
    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }

    /*
     * Chuẩn hóa đường dẫn ảnh trước khi lưu.
     */
    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        return imageUrl.trim();
    }

    /*
     * Chuẩn hóa public id của ảnh trên Cloudinary.
     */
    private String normalizeImagePublicId(String imagePublicId) {
        if (imagePublicId == null || imagePublicId.isBlank()) {
            return null;
        }

        return imagePublicId.trim();
    }

    /*
     * Lấy manager đang đăng nhập từ Spring Security.
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
}
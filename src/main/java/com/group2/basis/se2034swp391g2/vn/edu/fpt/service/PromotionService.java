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

    private static final String STATUS_ACTIVE = "Đang hoạt động";
    private static final String STATUS_SCHEDULED = "Sắp diễn ra";
    private static final String STATUS_EXPIRED = "Đã hết hạn";
    private static final String STATUS_INACTIVE = "Đã tắt";

    private static final String DISPLAY_FEATURED = "Banner nổi bật";
    private static final String DISPLAY_HOMEPAGE = "Hiển thị trang chủ";
    private static final String DISPLAY_HIDDEN = "Ẩn";

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

    // Lấy dữ liệu cho màn danh sách: danh sách đã lọc, số liệu tổng quan và thông tin phân trang.
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
        long activePromotions = countPromotionsByStatus(allPromotions, STATUS_ACTIVE);
        long scheduledPromotions = countPromotionsByStatus(allPromotions, STATUS_SCHEDULED);
        long expiredPromotions = countPromotionsByStatus(allPromotions, STATUS_EXPIRED);
        long inactivePromotions = countPromotionsByStatus(allPromotions, STATUS_INACTIVE);

        List<PromotionResponse> filteredPromotions = allPromotions.stream()
                .filter(promotion -> matchKeyword(promotion, normalizedKeyword))
                .filter(promotion -> matchStatus(promotion, selectedStatus))
                .toList();

        int totalFiltered = filteredPromotions.size();
        int totalPages = (int) Math.ceil((double) totalFiltered / size);

        if (totalPages > 0 && page >= totalPages) {
            page = totalPages - 1;
        }

        List<PromotionResponse> pagedPromotions = getPagedPromotions(filteredPromotions, page, size);

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

    // Lấy dữ liệu chi tiết của một khuyến mãi để hiển thị ở màn view.
    public PromotionResponse getPromotionDetail(Long id) {
        return toResponse(getPromotionEntity(id));
    }

    // Chuyển entity hiện tại sang request object để form edit có thể đổ dữ liệu sẵn.
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

    // Upload ảnh lên Cloudinary và chỉ trả về các thông tin form cần giữ lại để lưu DB.
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

    // Tạo khuyến mãi mới: validate trước, sau đó set các thông tin hệ thống và lưu DB.
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

    // Cập nhật khuyến mãi cũ bằng dữ liệu mới từ form edit.
    public void updatePromotion(Long id, PromotionRequest request) {
        Promotion promotion = getPromotionEntity(id);

        validatePromotionRequest(request, promotion.getId(), true);
        applyPromotionRequest(promotion, request);

        promotionRepository.save(promotion);
    }

    // Chỉ xóa khi khuyến mãi chưa từng được áp dụng vào booking còn hiệu lực.
    public void deletePromotion(Long id) {
        Promotion promotion = getPromotionEntity(id);

        if (bookingRepository.existsByPromotion_IdAndIsDeletedFalse(id)) {
            throw new IllegalArgumentException("Không thể xóa khuyến mãi đã được áp dụng trong đơn đặt phòng.");
        }

        promotionRepository.delete(promotion);
    }

    // Gán dữ liệu từ request sang entity để dùng chung cho cả create và update.
    // Bật hoặc tắt khuyến mãi theo trạng thái hiện tại mà không đổi các cấu hình khác.
    public boolean togglePromotionStatus(Long id) {
        Promotion promotion = getPromotionEntity(id);
        boolean newActiveStatus = !Boolean.TRUE.equals(promotion.getIsActive());

        promotion.setIsActive(newActiveStatus);
        promotionRepository.save(promotion);

        return newActiveStatus;
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

    // Chuyển entity sang response để template chỉ nhận đúng dữ liệu cần hiển thị.
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

    // Lấy entity theo id và chặn luôn các trường hợp id không hợp lệ.
    private Promotion getPromotionEntity(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Khuyến mãi không hợp lệ.");
        }

        return promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi."));
    }

    // Xác định trạng thái thời gian của khuyến mãi để hiển thị ở list/detail.
    private String getPromotionStatus(Promotion promotion) {
        Instant now = Instant.now();

        if (Boolean.FALSE.equals(promotion.getIsActive())) {
            return STATUS_INACTIVE;
        }

        if (promotion.getValidFrom() != null && promotion.getValidFrom().isAfter(now)) {
            return STATUS_SCHEDULED;
        }

        if (promotion.getValidTo() != null && promotion.getValidTo().isBefore(now)) {
            return STATUS_EXPIRED;
        }

        return STATUS_ACTIVE;
    }

    // Xác định kiểu hiển thị trên trang chủ: banner, hiện ở homepage, hay ẩn.
    private String getDisplayStatus(Promotion promotion) {
        if (Boolean.TRUE.equals(promotion.getFeatured())) {
            return DISPLAY_FEATURED;
        }

        if (Boolean.TRUE.equals(promotion.getShowOnHomepage())) {
            return DISPLAY_HOMEPAGE;
        }

        return DISPLAY_HIDDEN;
    }

    // Gói toàn bộ validate business của form vào một đầu mối duy nhất.
    private void validatePromotionRequest(PromotionRequest request, Long promotionId, boolean isEditMode) {
        validateName(request);
        validateDescription(request);
        validateDiscountAmount(request);
        validateUsageLimit(request);
        validateDateInput(request, isEditMode);
        validateUploadedImage(request);
        validateFeaturedBanner(request, promotionId);
    }

    // Validate file ảnh trước khi gửi lên Cloudinary.
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

    // Validate tên chiến dịch.
    private void validateName(PromotionRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Tên chiến dịch không được để trống.");
        }

        if (request.getName().trim().length() > 200) {
            throw new IllegalArgumentException("Tên chiến dịch không được vượt quá 200 ký tự.");
        }
    }

    // Validate mô tả ngắn.
    private void validateDescription(PromotionRequest request) {
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("Mô tả ngắn không được để trống.");
        }

        if (request.getDescription().trim().length() > 300) {
            throw new IllegalArgumentException("Mô tả không được vượt quá 300 ký tự.");
        }
    }

    // Validate số tiền giảm.
    private void validateDiscountAmount(PromotionRequest request) {
        if (request.getDiscountAmount() == null
                || request.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền giảm phải lớn hơn 0.");
        }
    }

    // Validate giới hạn lượt dùng.
    private void validateUsageLimit(PromotionRequest request) {
        if (request.getUsageLimit() == null || request.getUsageLimit() <= 0) {
            throw new IllegalArgumentException("Giới hạn lượt dùng phải lớn hơn 0.");
        }
    }

    // Validate 2 mốc thời gian và quy tắc ngày kết thúc phải sau ngày bắt đầu.
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

    // Validate ảnh đã upload thành công trước khi lưu khuyến mãi.
    private void validateUploadedImage(PromotionRequest request) {
        if (request.getImageUrl() == null || request.getImageUrl().isBlank()
                || request.getImagePublicId() == null || request.getImagePublicId().isBlank()) {
            throw new IllegalArgumentException("Vui lòng tải ảnh khuyến mãi thành công trước khi lưu.");
        }
    }

    // Validate quy tắc chỉ có một banner nổi bật đang hoạt động tại một thời điểm.
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

    // Nếu đã là banner nổi bật thì luôn phải được hiển thị ở trang chủ.
    private Boolean resolveShowOnHomepage(PromotionRequest request) {
        if (Boolean.TRUE.equals(request.getFeatured())) {
            return true;
        }

        return Boolean.TRUE.equals(request.getShowOnHomepage());
    }

    // Sinh mã khuyến mãi tự động theo định dạng VH-yyyyMM-XXXX.
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

    // Chuyển chuỗi ngày giờ từ form sang Instant để lưu DB và so sánh logic thời gian.
    private Instant convertToInstant(String dateTimeInput) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeInput);
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            throw new IllegalArgumentException("Định dạng ngày giờ không hợp lệ.");
        }
    }

    // Format ngày giờ từ DB sang đúng định dạng mà form add/edit đang dùng.
    private String formatForDateTimeInput(Instant instant) {
        if (instant == null) {
            return "";
        }

        return instant.atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(FORM_DATE_TIME_FORMATTER);
    }

    // Chuẩn hóa mô tả trước khi lưu.
    private String normalizeDescription(String description) {
        return description.trim();
    }

    // Chuẩn hóa link ảnh trước khi lưu.
    private String normalizeImageUrl(String imageUrl) {
        return imageUrl.trim();
    }

    // Chuẩn hóa public id của ảnh trước khi lưu.
    private String normalizeImagePublicId(String imagePublicId) {
        return imagePublicId.trim();
    }

    // Đưa số tiền giảm về dạng dễ nhìn hơn khi đổ lên form edit.
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

    // Lấy user hiện tại để gắn vào thông tin người tạo khuyến mãi.
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Người dùng chưa đăng nhập.");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng hiện tại."));
    }

    // Lọc theo từ khóa: chỉ tìm trong mã khuyến mãi và tên chiến dịch.
    private boolean matchKeyword(PromotionResponse promotion, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        String code = promotion.getCode() == null ? "" : promotion.getCode().toLowerCase();
        String name = promotion.getName() == null ? "" : promotion.getName().toLowerCase();

        return code.contains(keyword) || name.contains(keyword);
    }

    // Lọc theo trạng thái mà người dùng đang chọn trên tab.
    private boolean matchStatus(PromotionResponse promotion, String status) {
        if ("all".equals(status)) {
            return true;
        }

        if ("active".equals(status)) {
            return STATUS_ACTIVE.equals(promotion.getStatus());
        }

        if ("scheduled".equals(status)) {
            return STATUS_SCHEDULED.equals(promotion.getStatus());
        }

        if ("expired".equals(status)) {
            return STATUS_EXPIRED.equals(promotion.getStatus());
        }

        if ("inactive".equals(status)) {
            return STATUS_INACTIVE.equals(promotion.getStatus());
        }

        return true;
    }

    // Đếm nhanh số khuyến mãi theo một trạng thái cụ thể để hiển thị phần tổng quan.
    private long countPromotionsByStatus(List<PromotionResponse> promotions, String status) {
        return promotions.stream()
                .filter(promotion -> status.equals(promotion.getStatus()))
                .count();
    }

    // Cắt danh sách theo trang hiện tại để trả về đúng dữ liệu cần hiển thị.
    private List<PromotionResponse> getPagedPromotions(List<PromotionResponse> promotions, int page, int size) {
        int totalFiltered = promotions.size();

        if (totalFiltered == 0) {
            return List.of();
        }

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalFiltered);

        return promotions.subList(fromIndex, toIndex);
    }
}

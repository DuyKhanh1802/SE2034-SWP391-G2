package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.DiscountType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Promotion;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.PromotionRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.PromotionListResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.PromotionResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.PromotionRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final UserRepository userRepository;

    public PromotionService(PromotionRepository promotionRepository,
                            UserRepository userRepository) {
        this.promotionRepository = promotionRepository;
        this.userRepository = userRepository;
    }

    public PromotionListResponse getPromotionListResponse() {
        List<PromotionResponse> promotions = promotionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();

        long totalPromotions = promotions.size();

        long activePromotions = promotions.stream()
                .filter(promotion -> "Active".equals(promotion.getStatus()))
                .count();

        long scheduledPromotions = promotions.stream()
                .filter(promotion -> "Scheduled".equals(promotion.getStatus()))
                .count();

        long expiredPromotions = promotions.stream()
                .filter(promotion -> "Expired".equals(promotion.getStatus()))
                .count();

        long inactivePromotions = promotions.stream()
                .filter(promotion -> "Inactive".equals(promotion.getStatus()))
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

    public PromotionResponse getPromotionById(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found."));

        return toResponse(promotion);
    }

    public void createPromotion(PromotionRequest request, Authentication authentication) {
        validatePromotionRequest(request);

        User currentUser = getCurrentUser(authentication);
        Promotion promotion = buildPromotion(request, currentUser);

        promotionRepository.save(promotion);
    }

    private Promotion buildPromotion(PromotionRequest request, User currentUser) {
        Promotion promotion = new Promotion();

        promotion.setCode(request.getCode().trim().toUpperCase());
        promotion.setName(request.getName().trim());
        promotion.setDiscountType(request.getDiscountType());
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setMaxDiscount(getValidMaxDiscount(request));
        promotion.setUsageLimit(request.getUsageLimit());
        promotion.setUsageCount(0);
        promotion.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        promotion.setValidFrom(convertToInstant(request.getValidFromInput()));
        promotion.setValidTo(convertToInstant(request.getValidToInput()));
        promotion.setCreatedBy(currentUser);
        promotion.setCreatedAt(Instant.now());

        return promotion;
    }

    private PromotionResponse toResponse(Promotion promotion) {
        return PromotionResponse.builder()
                .id(promotion.getId())
                .code(promotion.getCode())
                .name(promotion.getName())
                .discountType(promotion.getDiscountType())
                .discountValue(promotion.getDiscountValue())
                .maxDiscount(promotion.getMaxDiscount())
                .usageLimit(promotion.getUsageLimit())
                .usageCount(promotion.getUsageCount())
                .validFrom(promotion.getValidFrom())
                .validTo(promotion.getValidTo())
                .isActive(promotion.getIsActive())
                .createdAt(promotion.getCreatedAt())
                .status(getPromotionStatus(promotion))
                .build();
    }

    private String getPromotionStatus(Promotion promotion) {
        Instant now = Instant.now();

        if (Boolean.FALSE.equals(promotion.getIsActive())) {
            return "Inactive";
        }

        if (promotion.getValidFrom() != null && promotion.getValidFrom().isAfter(now)) {
            return "Scheduled";
        }

        if (promotion.getValidTo() != null && promotion.getValidTo().isBefore(now)) {
            return "Expired";
        }

        return "Active";
    }

    private void validatePromotionRequest(PromotionRequest request) {
        validateCode(request);
        validateName(request);
        validateDiscountType(request);
        validateDiscountValue(request);
        validateMaxDiscount(request);
        validateUsageLimit(request);
        validateDateInput(request);
    }

    private void validateCode(PromotionRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("Promotion code is required.");
        }

        String normalizedCode = request.getCode().trim().toUpperCase();

        if (promotionRepository.existsByCode(normalizedCode)) {
            throw new IllegalArgumentException("Promotion code already exists.");
        }
    }

    private void validateName(PromotionRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Promotion name is required.");
        }
    }

    private void validateDiscountType(PromotionRequest request) {
        if (request.getDiscountType() == null) {
            throw new IllegalArgumentException("Discount type is required.");
        }
    }

    private void validateDiscountValue(PromotionRequest request) {
        if (request.getDiscountValue() == null
                || request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount value must be greater than 0.");
        }

        if (request.getDiscountType() == DiscountType.PERCENTAGE
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage discount cannot be greater than 100.");
        }
    }

    private void validateMaxDiscount(PromotionRequest request) {
        if (request.getDiscountType() == DiscountType.PERCENTAGE
                && request.getMaxDiscount() != null
                && request.getMaxDiscount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Max discount must be greater than 0.");
        }
    }

    private void validateUsageLimit(PromotionRequest request) {
        if (request.getUsageLimit() == null || request.getUsageLimit() <= 0) {
            throw new IllegalArgumentException("Usage limit must be greater than 0.");
        }
    }

    private void validateDateInput(PromotionRequest request) {
        if (request.getValidFromInput() == null || request.getValidFromInput().isBlank()) {
            throw new IllegalArgumentException("Valid from is required.");
        }

        if (request.getValidToInput() == null || request.getValidToInput().isBlank()) {
            throw new IllegalArgumentException("Valid to is required.");
        }

        Instant validFrom = convertToInstant(request.getValidFromInput());
        Instant validTo = convertToInstant(request.getValidToInput());

        LocalDate today = LocalDate.now();
        LocalDate validFromDate = validFrom.atZone(ZoneId.systemDefault()).toLocalDate();

        if (validFromDate.isBefore(today)) {
            throw new IllegalArgumentException("Valid from must not be before today.");
        }

        if (!validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("Valid to must be after valid from.");
        }
    }

    private BigDecimal getValidMaxDiscount(PromotionRequest request) {
        if (request.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            return null;
        }

        return request.getMaxDiscount();
    }

    private Instant convertToInstant(String dateTimeInput) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeInput);
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date time format.");
        }
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("User is not authenticated.");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found."));
    }
}
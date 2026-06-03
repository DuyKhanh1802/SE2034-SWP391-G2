package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Promotion;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.PromotionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.util.List;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }

    public Promotion getPromotionById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found."));
    }

    public Promotion savePromotion(Promotion promotion) {
        validatePromotion(promotion);
        return promotionRepository.save(promotion);
    }

    private void validatePromotion(Promotion promotion) {
        if (promotion.getCode() == null || promotion.getCode().isBlank()) {
            throw new IllegalArgumentException("Promotion code is required.");
        }

        if (promotion.getName() == null || promotion.getName().isBlank()) {
            throw new IllegalArgumentException("Promotion name is required.");
        }

        String normalizedCode = promotion.getCode().trim().toUpperCase();
        String normalizedName = promotion.getName().trim();

        if (promotionRepository.existsByCode(normalizedCode)) {
            throw new IllegalArgumentException("Promotion code already exists.");
        }

        if (promotion.getDiscountType() == null) {
            throw new IllegalArgumentException("Discount type is required.");
        }

        if (promotion.getDiscountValue() == null
                || promotion.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount value must be greater than 0.");
        }

        String discountType = promotion.getDiscountType().name();

        if ("PERCENTAGE".equals(discountType)) {
            validatePercentagePromotion(promotion);
        }

        if ("FIXED_AMOUNT".equals(discountType)) {
            validateFixedAmountPromotion(promotion);
        }

        if (promotion.getUsageLimit() == null || promotion.getUsageLimit() <= 0) {
            throw new IllegalArgumentException("Usage limit must be greater than 0.");
        }

        if (promotion.getValidFrom() == null) {
            throw new IllegalArgumentException("Valid from is required.");
        }

        if (promotion.getValidTo() == null) {
            throw new IllegalArgumentException("Valid to is required.");
        }

        LocalDate today = LocalDate.now();
        LocalDate validFromDate = promotion.getValidFrom()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        if (validFromDate.isBefore(today)) {
            throw new IllegalArgumentException("Valid from must not be before today.");
        }

        if (!promotion.getValidTo().isAfter(promotion.getValidFrom())) {
            throw new IllegalArgumentException("Valid to must be after valid from.");
        }

        promotion.setCode(normalizedCode);
        promotion.setName(normalizedName);
    }

    private void validatePercentagePromotion(Promotion promotion) {
        if (promotion.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage discount cannot be greater than 100.");
        }

        if (promotion.getMaxDiscount() != null
                && promotion.getMaxDiscount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Max discount must be greater than 0.");
        }
    }

    private void validateFixedAmountPromotion(Promotion promotion) {
        /*
         * Max Discount chỉ áp dụng cho giảm giá theo phần trăm.
         * Nếu là giảm số tiền cố định, hệ thống bỏ qua giá trị này.
         */
        promotion.setMaxDiscount(null);
    }
}
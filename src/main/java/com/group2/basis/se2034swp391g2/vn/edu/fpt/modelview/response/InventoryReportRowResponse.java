package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InventoryReportRowResponse {

    private final Long itemId;
    private final String itemName;
    private final String categoryName;
    private final String unit;
    private final BigDecimal currentQuantity;
    private final BigDecimal minimumQuantity;
    private final BigDecimal unitCost;
    private final BigDecimal receivedQuantity;
    private final BigDecimal issuedQuantity;
    private final BigDecimal disposedQuantity;
    private final LocalDate latestReceiptDate;

    private LocalDate latestExpiryDate;

    public InventoryReportRowResponse(Long itemId,
                                      String itemName,
                                      String categoryName,
                                      String unit,
                                      BigDecimal currentQuantity,
                                      BigDecimal minimumQuantity,
                                      BigDecimal unitCost,
                                      BigDecimal receivedQuantity,
                                      BigDecimal issuedQuantity,
                                      BigDecimal disposedQuantity,
                                      LocalDate latestReceiptDate) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.categoryName = categoryName;
        this.unit = unit;
        this.currentQuantity = currentQuantity == null ? BigDecimal.ZERO : currentQuantity;
        this.minimumQuantity = minimumQuantity == null ? BigDecimal.ZERO : minimumQuantity;
        this.unitCost = unitCost == null ? BigDecimal.ZERO : unitCost;
        this.receivedQuantity = receivedQuantity == null ? BigDecimal.ZERO : receivedQuantity;
        this.issuedQuantity = issuedQuantity == null ? BigDecimal.ZERO : issuedQuantity;
        this.disposedQuantity = disposedQuantity == null ? BigDecimal.ZERO : disposedQuantity;
        this.latestReceiptDate = latestReceiptDate;
    }

    public Long getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getCurrentQuantity() {
        return currentQuantity;
    }

    public BigDecimal getMinimumQuantity() {
        return minimumQuantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public BigDecimal getIssuedQuantity() {
        return issuedQuantity;
    }

    public BigDecimal getDisposedQuantity() {
        return disposedQuantity;
    }

    public BigDecimal getTotalOutQuantity() {
        return issuedQuantity.add(disposedQuantity);
    }

    public BigDecimal getStockValue() {
        return currentQuantity.multiply(unitCost);
    }

    public LocalDate getLatestReceiptDate() {
        return latestReceiptDate;
    }

    public LocalDate getLatestExpiryDate() {
        return latestExpiryDate;
    }

    public void setLatestExpiryDate(LocalDate latestExpiryDate) {
        this.latestExpiryDate = latestExpiryDate;
    }

    public String getStockStatus() {
        if (currentQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return "OUT_OF_STOCK";
        }

        if (currentQuantity.compareTo(minimumQuantity) <= 0) {
            return "LOW";
        }

        return "NORMAL";
    }

    public String getStockStatusText() {
        return switch (getStockStatus()) {
            case "OUT_OF_STOCK" -> "Hết hàng";
            case "LOW" -> "Tồn thấp";
            default -> "Bình thường";
        };
    }

    public String getStockStatusClass() {
        return switch (getStockStatus()) {
            case "OUT_OF_STOCK" -> "status-out";
            case "LOW" -> "status-low";
            default -> "status-normal";
        };
    }

    public String getExpiryStatus() {
        if (latestExpiryDate == null) {
            return "NONE";
        }

        LocalDate today = LocalDate.now();

        if (latestExpiryDate.isBefore(today)) {
            return "EXPIRED";
        }

        if (!latestExpiryDate.isAfter(today.plusDays(30))) {
            return "EXPIRING_SOON";
        }

        return "VALID";
    }

    public String getExpiryStatusText() {
        return switch (getExpiryStatus()) {
            case "EXPIRED" -> "Đã hết hạn";
            case "EXPIRING_SOON" -> "Sắp hết hạn";
            case "VALID" -> "Còn hạn";
            default -> "Không có HSD";
        };
    }
}
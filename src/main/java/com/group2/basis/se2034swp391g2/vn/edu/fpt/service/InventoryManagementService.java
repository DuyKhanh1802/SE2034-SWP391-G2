package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionCategory;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.InventoryTransactionType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.*;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryManagementService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryReceiptRepository inventoryReceiptRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ServiceInventoryMappingRepository serviceInventoryMappingRepository;
    private final RoomRefreshInventoryMappingRepository roomRefreshInventoryMappingRepository;
    private final ServiceRepository serviceRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final CashTransactionService cashTransactionService;

    @Transactional(readOnly = true)
    public List<InventoryItem> getItems() {
        return inventoryItemRepository.findByIsDeletedFalseOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public InventoryItem getItem(Long id) {
        return inventoryItemRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hàng hóa."));
    }

    @Transactional
    public InventoryItem createItem(String name,
                                    String category,
                                    String unit,
                                    BigDecimal openingQuantity,
                                    BigDecimal minimumQuantity) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên hàng hóa là bắt buộc.");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("Đơn vị tính là bắt buộc.");
        }

        BigDecimal opening = normalizeNonNegative(openingQuantity);
        InventoryItem item = InventoryItem.builder()
                .code(generateItemCode())
                .name(name.trim())
                .category(normalizeText(category))
                .unit(unit.trim())
                .openingQuantity(opening)
                .currentQuantity(opening)
                .minimumQuantity(normalizeNonNegative(minimumQuantity))
                .unitCost(BigDecimal.ZERO)
                .isDeleted(false)
                .build();

        InventoryItem savedItem = inventoryItemRepository.save(item);
        if (opening.compareTo(BigDecimal.ZERO) > 0) {
            recordInventoryTransaction(savedItem, InventoryTransactionType.IN, opening,
                    "Tồn đầu kỳ", "OPENING", savedItem.getId(), null);
        }
        return savedItem;
    }

    @Transactional
    public InventoryReceipt createReceipt(Long itemId,
                                          BigDecimal quantity,
                                          BigDecimal unitCost,
                                          String supplier,
                                          String note,
                                          User createdBy) {
        InventoryItem item = getItem(itemId);
        validatePositive(quantity, "Số lượng nhập phải lớn hơn 0.");
        validatePositive(unitCost, "Đơn giá nhập phải lớn hơn 0.");

        BigDecimal totalCost = quantity.multiply(unitCost);
        InventoryReceipt receipt = InventoryReceipt.builder()
                .code(generateReceiptCode())
                .item(item)
                .quantity(quantity)
                .unitCost(unitCost)
                .totalCost(totalCost)
                .supplier(normalizeText(supplier))
                .note(normalizeText(note))
                .createdBy(createdBy)
                .build();

        InventoryReceipt savedReceipt = inventoryReceiptRepository.save(receipt);
        BigDecimal existingQuantity = item.getCurrentQuantity();
        item.setUnitCost(calculateWeightedUnitCost(item.getUnitCost(), existingQuantity, unitCost, quantity));
        item.setCurrentQuantity(item.getCurrentQuantity().add(quantity));
        inventoryItemRepository.save(item);

        recordInventoryTransaction(item, InventoryTransactionType.IN, quantity,
                "Nhập hàng " + savedReceipt.getCode(), "INVENTORY_RECEIPT", savedReceipt.getId(), createdBy);

        cashTransactionService.createInventoryPurchase(totalCost,
                "Chi nhập kho " + savedReceipt.getCode() + " - " + item.getName(),
                savedReceipt.getId(),
                createdBy);

        return savedReceipt;
    }

    @Transactional
    public ServiceInventoryMapping linkItemToService(Long serviceId,
                                                     Long itemId,
                                                     BigDecimal quantityPerUse) {
        com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service =
                serviceRepository.findById(serviceId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ."));
        InventoryItem item = getItem(itemId);
        validatePositive(quantityPerUse, "Số lượng tiêu hao phải lớn hơn 0.");

        ServiceInventoryMapping mapping = ServiceInventoryMapping.builder()
                .service(service)
                .item(item)
                .quantityPerUse(quantityPerUse)
                .build();

        return serviceInventoryMappingRepository.save(mapping);
    }

    @Transactional
    public RoomRefreshInventoryMapping linkItemToRoomRefresh(Long roomTypeId,
                                                             Long itemId,
                                                             BigDecimal quantityPerRefresh) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hạng phòng."));
        InventoryItem item = getItem(itemId);
        validatePositive(quantityPerRefresh, "Số lượng refresh phải lớn hơn 0.");

        RoomRefreshInventoryMapping mapping = RoomRefreshInventoryMapping.builder()
                .roomType(roomType)
                .item(item)
                .quantityPerRefresh(quantityPerRefresh)
                .build();

        return roomRefreshInventoryMappingRepository.save(mapping);
    }

    @Transactional
    public void consumeForService(com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service,
                                  BigDecimal serviceQuantity,
                                  Long sourceId,
                                  User createdBy) {
        if (service == null) {
            return;
        }
        BigDecimal multiplier = serviceQuantity == null ? BigDecimal.ONE : serviceQuantity;
        List<ServiceInventoryMapping> mappings = serviceInventoryMappingRepository.findByService_Id(service.getId());

        for (ServiceInventoryMapping mapping : mappings) {
            InventoryItem item = mapping.getItem();
            BigDecimal consumedQuantity = mapping.getQuantityPerUse().multiply(multiplier);
            item.setCurrentQuantity(item.getCurrentQuantity().subtract(consumedQuantity));
            inventoryItemRepository.save(item);
            recordInventoryTransaction(item, InventoryTransactionType.OUT, consumedQuantity,
                    "Tiêu hao từ dịch vụ " + service.getName(), "FOLIO_ITEM", sourceId, createdBy);
        }
    }

    @Transactional
    public void consumeForRoomRefresh(RoomType roomType,
                                      Long sourceId,
                                      User createdBy) {
        if (roomType == null) {
            return;
        }

        List<RoomRefreshInventoryMapping> mappings = roomRefreshInventoryMappingRepository.findByRoomType_Id(roomType.getId());
        for (RoomRefreshInventoryMapping mapping : mappings) {
            InventoryItem item = mapping.getItem();
            BigDecimal consumedQuantity = mapping.getQuantityPerRefresh();
            item.setCurrentQuantity(item.getCurrentQuantity().subtract(consumedQuantity));
            inventoryItemRepository.save(item);
            recordInventoryTransaction(item, InventoryTransactionType.OUT, consumedQuantity,
                    "Refresh phòng sau checkout - " + roomType.getName(), "ROOM_REFRESH", sourceId, createdBy);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateServiceCost(Long serviceId, BigDecimal serviceQuantity) {
        BigDecimal multiplier = serviceQuantity == null ? BigDecimal.ONE : serviceQuantity;
        return serviceInventoryMappingRepository.findByService_Id(serviceId).stream()
                .map(mapping -> mapping.getQuantityPerUse()
                        .multiply(mapping.getItem().getUnitCost())
                        .multiply(multiplier))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateRoomRefreshCost(Long roomTypeId) {
        return roomRefreshInventoryMappingRepository.findByRoomType_Id(roomTypeId).stream()
                .map(mapping -> mapping.getQuantityPerRefresh().multiply(mapping.getItem().getUnitCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public List<InventoryTransaction> getRecentTransactions() {
        return inventoryTransactionRepository.findTop30ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<InventoryReceipt> getRecentReceipts() {
        return inventoryReceiptRepository.findTop20ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<ServiceInventoryMapping> getMappingsForItem(Long itemId) {
        return serviceInventoryMappingRepository.findByItem_Id(itemId);
    }

    @Transactional(readOnly = true)
    public List<RoomRefreshInventoryMapping> getRefreshMappingsForItem(Long itemId) {
        return roomRefreshInventoryMappingRepository.findByItem_Id(itemId);
    }

    @Transactional(readOnly = true)
    public List<InventoryTransaction> getTransactionsForItem(Long itemId) {
        return inventoryTransactionRepository.findByItem_IdOrderByCreatedAtDesc(itemId);
    }

    @Transactional(readOnly = true)
    public List<com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service> getAvailableServices() {
        return serviceRepository.findByIsDeletedFalseAndIsAvailableTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<RoomType> getRoomTypes() {
        return roomTypeRepository.findByIsDeletedFalse();
    }

    @Transactional(readOnly = true)
    public BigDecimal getItemTotalIn(Long itemId) {
        return inventoryTransactionRepository.sumQuantityByItemAndType(itemId, InventoryTransactionType.IN);
    }

    @Transactional(readOnly = true)
    public BigDecimal getItemTotalOut(Long itemId) {
        return inventoryTransactionRepository.sumQuantityByItemAndType(itemId, InventoryTransactionType.OUT);
    }

    @Transactional(readOnly = true)
    public long countLowStockItems() {
        return inventoryItemRepository.countLowStockItems();
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> getLowStockItems() {
        return inventoryItemRepository.findLowStockItems();
    }

    private void recordInventoryTransaction(InventoryItem item,
                                            InventoryTransactionType type,
                                            BigDecimal quantity,
                                            String description,
                                            String sourceType,
                                            Long sourceId,
                                            User createdBy) {
        InventoryTransaction transaction = InventoryTransaction.builder()
                .item(item)
                .type(type)
                .quantity(quantity)
                .description(description)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .createdBy(createdBy)
                .build();

        inventoryTransactionRepository.save(transaction);
    }

    private BigDecimal normalizeNonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá trị không được âm.");
        }
        return value;
    }

    private void validatePositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal calculateWeightedUnitCost(BigDecimal currentCost,
                                                 BigDecimal currentQuantity,
                                                 BigDecimal receiptCost,
                                                 BigDecimal receiptQuantity) {
        BigDecimal existingCost = currentCost == null ? BigDecimal.ZERO : currentCost;
        BigDecimal existingQuantity = currentQuantity == null ? BigDecimal.ZERO : currentQuantity;
        BigDecimal totalQuantity = existingQuantity.add(receiptQuantity);

        if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return receiptCost;
        }

        BigDecimal currentValue = existingCost.multiply(existingQuantity);
        BigDecimal receiptValue = receiptCost.multiply(receiptQuantity);

        return currentValue.add(receiptValue)
                .divide(totalQuantity, 2, java.math.RoundingMode.HALF_UP);
    }

    private String generateItemCode() {
        String code;
        do {
            int randomNumber = (int) (Math.random() * 9000) + 1000;
            code = "IT-" + LocalDate.now(APP_ZONE).format(CODE_DATE_FORMATTER) + "-" + randomNumber;
        } while (inventoryItemRepository.existsByCode(code));
        return code;
    }

    private String generateReceiptCode() {
        String code;
        do {
            int randomNumber = (int) (Math.random() * 9000) + 1000;
            code = "IR-" + LocalDate.now(APP_ZONE).format(CODE_DATE_FORMATTER) + "-" + randomNumber;
        } while (inventoryReceiptRepository.existsByCode(code));
        return code;
    }
}

package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionCategory;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.InventoryTransactionType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentMethod;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.*;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryManagementService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
    private static final long MAX_IMPORT_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_IMPORT_ROWS = 1000;
    private static final int IMPORT_COLUMN_COUNT = 6;
    private static final List<String> IMPORT_HEADERS = List.of(
            "ten_hang",
            "loai",
            "don_vi",
            "ton_dau_ky",
            "nguong_canh_bao",
            "gia_von_truoc_vat");

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryCategoryRepository inventoryCategoryRepository;
    private final InventoryReceiptRepository inventoryReceiptRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ServiceInventoryMappingRepository serviceInventoryMappingRepository;
    private final RoomRefreshInventoryMappingRepository roomRefreshInventoryMappingRepository;
    private final ServiceRepository serviceRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final CashTransactionService cashTransactionService;
    private final FinancialChargeService financialChargeService;

    @Transactional(readOnly = true)
    public List<InventoryItem> getItems() {
        List<InventoryItem> items = inventoryItemRepository.findAllByIsDeletedFalse(Sort.by("id").ascending());
        attachLatestBatchExpiry(items);
        return items;
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> getItemsForSelection() {
        return inventoryItemRepository.findAllByIsDeletedFalse(Sort.by("id").ascending());
    }

    @Transactional(readOnly = true)
    public Page<InventoryItem> getItems(String keyword,
                                        Long categoryId,
                                        String stockStatus,
                                        int page,
                                        int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 5), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("id").ascending());
        String normalizedKeyword = normalizeText(keyword);
        Page<InventoryItem> result = inventoryItemRepository.searchItems(
                normalizedKeyword,
                categoryId,
                normalizeStockStatus(stockStatus),
                pageable);
        attachLatestBatchExpiry(result.getContent());
        return result;
    }

    @Transactional(readOnly = true)
    public List<InventoryCategory> getCategories() {
        return inventoryCategoryRepository.findByIsActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public BigDecimal getInventoryVatRate() {
        return financialChargeService.getInventoryVatRate();
    }

    @Transactional(readOnly = true)
    public String previewBatchCode(LocalDate receiptDate) {
        LocalDate effectiveReceiptDate = receiptDate == null ? LocalDate.now(APP_ZONE) : receiptDate;
        return generateBatchCode(effectiveReceiptDate);
    }

    @Transactional(readOnly = true)
    public InventoryItem getItem(Long id) {
        InventoryItem item = inventoryItemRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hàng hóa."));
        attachLatestBatchExpiry(item);
        return item;
    }

    @Transactional(readOnly = true)
    public boolean canDeleteItem(Long id) {
        InventoryItem item = getItem(id);
        return item.getCurrentQuantity().compareTo(BigDecimal.ZERO) == 0
                && !serviceInventoryMappingRepository.existsByItem_Id(id)
                && !roomRefreshInventoryMappingRepository.existsByItem_Id(id);
    }

    @Transactional
    public void softDeleteItem(Long id) {
        InventoryItem item = getItem(id);
        if (item.getCurrentQuantity().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Chỉ có thể xóa hàng hóa khi tồn kho bằng 0.");
        }
        if (serviceInventoryMappingRepository.existsByItem_Id(id)
                || roomRefreshInventoryMappingRepository.existsByItem_Id(id)) {
            throw new IllegalArgumentException("Hãy xóa toàn bộ quy tắc tiêu hao trước khi xóa hàng hóa.");
        }
        item.setIsDeleted(true);
        inventoryItemRepository.save(item);
    }

    @Transactional
    public InventoryImportResult importItemsFromExcel(MultipartFile file, User createdBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn file Excel cần import.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file Excel định dạng .xlsx.");
        }
        if (file.getSize() > MAX_IMPORT_FILE_SIZE) {
            throw new IllegalArgumentException("File Excel không được vượt quá 5 MB.");
        }

        Workbook workbook;
        try {
            workbook = new XSSFWorkbook(file.getInputStream());
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("File không phải workbook .xlsx hợp lệ hoặc đã bị hỏng.");
        }

        try (workbook) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("File Excel không có sheet dữ liệu.");
            }

            DataFormatter formatter = new DataFormatter(Locale.US);
            Sheet sheet = workbook.getSheetAt(0);
            validateImportHeader(sheet, formatter);

            if (sheet.getLastRowNum() > MAX_IMPORT_ROWS) {
                throw new IllegalArgumentException("File Excel chỉ được chứa tối đa 1.000 dòng dữ liệu.");
            }

            List<InventoryImportRow> validRows = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            Set<String> namesInFile = new HashSet<>();
            int skippedCount = 0;

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyRow(formatter, row)) {
                    continue;
                }

                int excelRowNumber = rowIndex + 1;
                List<String> rowErrors = new ArrayList<>();
                validateNoExtraColumns(formatter, row, excelRowNumber, rowErrors);

                String name = readTextCell(formatter, row, 0, "Tên hàng", excelRowNumber, rowErrors);
                String categoryName = readTextCell(formatter, row, 1, "Loại hàng", excelRowNumber, rowErrors);
                String unit = readTextCell(formatter, row, 2, "Đơn vị tính", excelRowNumber, rowErrors);

                validateRequiredAndLength(name, "Tên hàng", 150, excelRowNumber, rowErrors);
                validateRequiredAndLength(categoryName, "Loại hàng", 100, excelRowNumber, rowErrors);
                validateRequiredAndLength(unit, "Đơn vị tính", 30, excelRowNumber, rowErrors);

                InventoryCategory category = null;
                if (categoryName != null && !categoryName.isBlank()) {
                    category = inventoryCategoryRepository
                            .findByNameIgnoreCaseAndIsActiveTrue(categoryName)
                            .orElse(null);
                    if (category == null) {
                        rowErrors.add("Dòng " + excelRowNumber + ": loại hàng '" + categoryName
                                + "' không tồn tại hoặc đã ngừng sử dụng.");
                    }
                }

                BigDecimal openingQuantity = readDecimalCell(
                        row, 3, "Tồn đầu kỳ", excelRowNumber, 2, 10, rowErrors);
                BigDecimal minimumQuantity = readDecimalCell(
                        row, 4, "Ngưỡng cảnh báo", excelRowNumber, 2, 10, rowErrors);
                BigDecimal unitCost = readDecimalCell(
                        row, 5, "Giá vốn trước VAT", excelRowNumber, 0, 15, rowErrors);

                String normalizedName = name == null ? null : name.trim().toLowerCase(Locale.ROOT);
                if (normalizedName != null && !normalizedName.isBlank() && !namesInFile.add(normalizedName)) {
                    rowErrors.add("Dòng " + excelRowNumber + ": tên hàng bị trùng trong chính file Excel.");
                }

                if (!rowErrors.isEmpty()) {
                    errors.addAll(rowErrors);
                    continue;
                }

                if (inventoryItemRepository.existsByNameIgnoreCaseAndIsDeletedFalse(name.trim())) {
                    skippedCount++;
                    continue;
                }

                validRows.add(new InventoryImportRow(
                        name.trim(), category, unit.trim(),
                        openingQuantity, minimumQuantity, unitCost));
            }

            if (!errors.isEmpty()) {
                throw new IllegalArgumentException(buildImportErrorMessage(errors));
            }
            if (validRows.isEmpty() && skippedCount == 0) {
                throw new IllegalArgumentException("File Excel không có dòng hàng hóa nào.");
            }

            for (InventoryImportRow row : validRows) {
                createItem(row.name(), row.category(), row.unit(),
                        row.openingQuantity(), row.minimumQuantity(), row.unitCost(), createdBy);
            }
            return new InventoryImportResult(validRows.size(), skippedCount);
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đóng hoặc hoàn tất việc đọc file Excel.");
        }
    }

    private InventoryItem createItem(String name,
                                     InventoryCategory category,
                                     String unit,
                                     BigDecimal openingQuantity,
                                     BigDecimal minimumQuantity,
                                     BigDecimal unitCost,
                                     User createdBy) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên hàng hóa là bắt buộc.");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("Đơn vị tính là bắt buộc.");
        }
        if (category == null) {
            throw new IllegalArgumentException("Loại hàng là bắt buộc.");
        }
        if (inventoryItemRepository.existsByNameIgnoreCaseAndIsDeletedFalse(name.trim())) {
            throw new IllegalArgumentException("Hàng hóa đã tồn tại.");
        }

        BigDecimal opening = normalizeQuantity(openingQuantity, "Tồn đầu kỳ", false);
        BigDecimal normalizedMinimumQuantity = normalizeQuantity(minimumQuantity, "Mức tồn tối thiểu", false);
        BigDecimal normalizedUnitCost = normalizeNonNegative(unitCost);
        validateWholeNumber(normalizedUnitCost, "Giá vốn trước VAT");
        InventoryItem item = InventoryItem.builder()
                .name(name.trim())
                .category(category)
                .legacyCategory(category.getName())
                .unit(unit.trim())
                .openingQuantity(opening)
                .currentQuantity(opening)
                .minimumQuantity(normalizedMinimumQuantity)
                .unitCost(normalizedUnitCost)
                .isDeleted(false)
                .build();

        InventoryItem savedItem = inventoryItemRepository.save(item);
        if (opening.compareTo(BigDecimal.ZERO) > 0) {
            recordInventoryTransaction(savedItem, InventoryTransactionType.IN, opening,
                    "Tồn đầu kỳ", "OPENING", savedItem.getId(), createdBy);
        }
        return savedItem;
    }

    public record InventoryImportResult(int importedCount, int skippedCount) {
    }

    private record InventoryImportRow(String name,
                                      InventoryCategory category,
                                      String unit,
                                      BigDecimal openingQuantity,
                                      BigDecimal minimumQuantity,
                                      BigDecimal unitCost) {
    }

    @Transactional
    public InventoryReceipt createReceipt(Long itemId,
                                          BigDecimal quantity,
                                          BigDecimal unitCost,
                                          String supplier,
                                          String note,
                                          LocalDate receiptDate,
                                          String batchCode,
                                          LocalDate expiryDate,
                                          PaymentMethod paymentMethod,
                                          User createdBy) {
        InventoryItem item = getItem(itemId);
        validatePositive(quantity, "Số lượng nhập phải lớn hơn 0.");
        validateMaxScale(quantity, 2, "Số lượng nhập");
        validatePositive(unitCost, "Đơn giá nhập phải lớn hơn 0.");
        validateNaturalNumber(unitCost, "Đơn giá nhập");
        LocalDate effectiveReceiptDate = receiptDate == null ? LocalDate.now(APP_ZONE) : receiptDate;
        if (effectiveReceiptDate.isAfter(LocalDate.now(APP_ZONE))) {
            throw new IllegalArgumentException("Ngày nhập kho không được ở tương lai.");
        }
        if (expiryDate != null && !expiryDate.isAfter(effectiveReceiptDate)) {
            throw new IllegalArgumentException("Hạn sử dụng phải sau ngày nhập kho.");
        }
        String generatedBatchCode = expiryDate == null ? null : generateBatchCode(effectiveReceiptDate);

        BigDecimal subtotal = quantity.multiply(unitCost).setScale(0, java.math.RoundingMode.HALF_UP);
        BigDecimal vatRate = financialChargeService.getInventoryVatRate();
        BigDecimal vatAmount = financialChargeService.calculateRateAmount(subtotal, vatRate);
        BigDecimal totalCost = subtotal.add(vatAmount).setScale(0, java.math.RoundingMode.HALF_UP);
        InventoryReceipt receipt = InventoryReceipt.builder()
                .code(generateReceiptCode())
                .item(item)
                .quantity(quantity)
                .unitCost(unitCost)
                .subtotal(subtotal)
                .vatRate(vatRate)
                .vatAmount(vatAmount)
                .totalCost(totalCost)
                .supplier(normalizeText(supplier))
                .note(normalizeText(note))
                .receiptDate(effectiveReceiptDate)
                .batchCode(generatedBatchCode)
                .expiryDate(expiryDate)
                .createdBy(createdBy)
                .build();

        InventoryReceipt savedReceipt = inventoryReceiptRepository.save(receipt);
        BigDecimal existingQuantity = item.getCurrentQuantity();
        item.setUnitCost(calculateWeightedUnitCost(item.getUnitCost(), existingQuantity, unitCost, quantity));
        item.setCurrentQuantity(item.getCurrentQuantity().add(quantity));
        inventoryItemRepository.save(item);

        recordInventoryTransaction(item, InventoryTransactionType.IN, quantity,
                "Nhập kho từ phiếu nhập", "INVENTORY_RECEIPT", savedReceipt.getId(), createdBy);

        cashTransactionService.createInventoryPurchase(totalCost,
                "Chi nhập kho " + savedReceipt.getCode() + " - " + item.getName(),
                savedReceipt.getId(),
                paymentMethod,
                createdBy);

        return savedReceipt;
    }

    @Transactional
    public void disposeStock(Long itemId,
                             BigDecimal quantity,
                             String reason,
                             User createdBy) {
        InventoryItem item = getItem(itemId);
        validatePositive(quantity, "Số lượng hủy phải lớn hơn 0.");
        validateMaxScale(quantity, 2, "Số lượng hủy");
        ensureSufficientStock(item, quantity);

        String normalizedReason = normalizeText(reason);
        if (normalizedReason == null) {
            normalizedReason = "Hủy hàng hóa";
        }
        if (normalizedReason.length() > 250) {
            throw new IllegalArgumentException("Lý do hủy không được vượt quá 250 ký tự.");
        }

        item.setCurrentQuantity(item.getCurrentQuantity().subtract(quantity));
        inventoryItemRepository.save(item);

        recordInventoryTransaction(item, InventoryTransactionType.DISPOSAL, quantity,
                normalizedReason, "INVENTORY_DISPOSAL", item.getId(), createdBy);
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
        validateMaxScale(quantityPerUse, 2, "Số lượng tiêu hao");

        ServiceInventoryMapping mapping = serviceInventoryMappingRepository
                .findByService_IdAndItem_Id(serviceId, itemId)
                .orElseGet(() -> ServiceInventoryMapping.builder()
                        .service(service)
                        .item(item)
                        .build());
        mapping.setQuantityPerUse(quantityPerUse);

        return serviceInventoryMappingRepository.save(mapping);
    }

    @Transactional
    public RoomRefreshInventoryMapping linkItemToRoomRefresh(Long roomTypeId,
                                                             Long itemId,
                                                             BigDecimal quantityPerRefresh) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hạng phòng."));
        InventoryItem item = getItem(itemId);
        validatePositive(quantityPerRefresh, "Số lượng lấp đồ phải lớn hơn 0.");
        validateMaxScale(quantityPerRefresh, 2, "Số lượng lấp đồ");

        RoomRefreshInventoryMapping mapping = roomRefreshInventoryMappingRepository
                .findByRoomType_IdAndItem_Id(roomTypeId, itemId)
                .orElseGet(() -> RoomRefreshInventoryMapping.builder()
                        .roomType(roomType)
                        .item(item)
                        .build());
        mapping.setQuantityPerRefresh(quantityPerRefresh);

        return roomRefreshInventoryMappingRepository.save(mapping);
    }

    @Transactional
    public void deleteServiceMapping(Long itemId, Long mappingId) {
        getItem(itemId);
        ServiceInventoryMapping mapping = serviceInventoryMappingRepository.findByIdAndItem_Id(mappingId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quy tắc tiêu hao dịch vụ."));
        serviceInventoryMappingRepository.delete(mapping);
    }

    @Transactional
    public void deleteRoomRefreshMapping(Long itemId, Long mappingId) {
        getItem(itemId);
        RoomRefreshInventoryMapping mapping = roomRefreshInventoryMappingRepository.findByIdAndItem_Id(mappingId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quy tắc lấp đồ phòng."));
        roomRefreshInventoryMappingRepository.delete(mapping);
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
            ensureSufficientStock(item, consumedQuantity);
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
            ensureSufficientStock(item, consumedQuantity);
            item.setCurrentQuantity(item.getCurrentQuantity().subtract(consumedQuantity));
            inventoryItemRepository.save(item);
            recordInventoryTransaction(item, InventoryTransactionType.OUT, consumedQuantity,
                    "Lấp đồ phòng sau checkout - " + roomType.getName(), "ROOM_REFRESH", sourceId, createdBy);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateServiceCost(Long serviceId, BigDecimal serviceQuantity) {
        BigDecimal multiplier = serviceQuantity == null ? BigDecimal.ONE : serviceQuantity;
        return serviceInventoryMappingRepository.findByService_Id(serviceId).stream()
                .map(mapping -> mapping.getQuantityPerUse()
                        .multiply(mapping.getItem().getUnitCost())
                        .multiply(multiplier))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, java.math.RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateRoomRefreshCost(Long roomTypeId) {
        return roomRefreshInventoryMappingRepository.findByRoomType_Id(roomTypeId).stream()
                .map(mapping -> mapping.getQuantityPerRefresh().multiply(mapping.getItem().getUnitCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, java.math.RoundingMode.HALF_UP);
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
    public List<InventoryReceipt> getReceiptsForItem(Long itemId) {
        getItem(itemId);
        return inventoryReceiptRepository.findTop20ByItem_IdOrderByReceiptDateDescCreatedAtDesc(itemId);
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

    private void attachLatestBatchExpiry(InventoryItem item) {
        inventoryReceiptRepository
                .findFirstByItem_IdAndExpiryDateIsNotNullOrderByReceiptDateDescCreatedAtDesc(item.getId())
                .ifPresent(receipt -> item.setLatestBatchExpiryDate(receipt.getExpiryDate()));
    }

    private void attachLatestBatchExpiry(List<InventoryItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<Long> itemIds = items.stream()
                .map(InventoryItem::getId)
                .filter(id -> id != null)
                .toList();
        if (itemIds.isEmpty()) {
            return;
        }

        Map<Long, LocalDate> expiryDatesByItemId = inventoryReceiptRepository
                .findLatestExpiryDatesByItemIds(itemIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> toLocalDate(row[1]),
                        (first, ignored) -> first
                ));

        items.forEach(item -> item.setLatestBatchExpiryDate(expiryDatesByItemId.get(item.getId())));
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant().atZone(APP_ZONE).toLocalDate();
        }
        throw new IllegalArgumentException("Unsupported expiry date value: " + value);
    }

    @Transactional(readOnly = true)
    public Page<InventoryTransaction> getTransactions(Long itemId,
                                                      InventoryTransactionType type,
                                                      LocalDate dateFrom,
                                                      LocalDate dateTo,
                                                      int page,
                                                      int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 5), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());
        if (itemId != null) {
            getItem(itemId);
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc.");
        }
        Instant fromTime = dateFrom == null ? null : dateFrom.atStartOfDay(APP_ZONE).toInstant();
        Instant toTime = dateTo == null ? null : dateTo.plusDays(1).atStartOfDay(APP_ZONE).toInstant();
        return inventoryTransactionRepository.search(itemId, type, fromTime, toTime, pageable);
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
        return inventoryTransactionRepository.sumQuantityByItemAndType(itemId, InventoryTransactionType.OUT)
                .add(inventoryTransactionRepository.sumQuantityByItemAndType(itemId, InventoryTransactionType.DISPOSAL));
    }

    @Transactional(readOnly = true)
    public long countLowStockItems() {
        return inventoryItemRepository.countLowStockItems();
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> getLowStockItems() {
        return inventoryItemRepository.findLowStockItems();
    }

    @Transactional(readOnly = true)
    public List<String> getExpiringSoonItemCodes() {
        LocalDate today = LocalDate.now(APP_ZONE);
        return inventoryReceiptRepository
                .findItemIdsWithLatestExpiryDateBetween(today, today.plusDays(30))
                .stream()
                .map(id -> "IT-%04d".formatted(id))
                .toList();
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
                .remainingQuantity(item.getCurrentQuantity())
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

    private BigDecimal normalizeQuantity(BigDecimal value, String fieldName, boolean positive) {
        if (positive) {
            validatePositive(value, fieldName + " phải lớn hơn 0.");
        } else {
            value = normalizeNonNegative(value);
        }
        validateMaxScale(value, 2, fieldName);
        return value;
    }

    private String readCell(DataFormatter formatter, Row row, int cellIndex) {
        String value = formatter.formatCellValue(row.getCell(cellIndex));
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateImportHeader(Sheet sheet, DataFormatter formatter) {
        Row header = sheet.getRow(0);
        if (header == null) {
            throw new IllegalArgumentException("Thiếu dòng tiêu đề của file Excel.");
        }
        List<String> actualHeaders = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < IMPORT_COLUMN_COUNT; columnIndex++) {
            String value = readCell(formatter, header, columnIndex);
            actualHeaders.add(value == null ? "" : value.toLowerCase(Locale.ROOT));
        }
        if (!actualHeaders.equals(IMPORT_HEADERS)) {
            throw new IllegalArgumentException(
                    "Tiêu đề không đúng mẫu. Thứ tự bắt buộc: " + String.join(", ", IMPORT_HEADERS) + ".");
        }
        List<String> errors = new ArrayList<>();
        validateNoExtraColumns(formatter, header, 1, errors);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.getFirst());
        }
    }

    private boolean isEmptyRow(DataFormatter formatter, Row row) {
        if (row == null) {
            return true;
        }
        int lastCell = Math.max(row.getLastCellNum(), IMPORT_COLUMN_COUNT);
        for (int columnIndex = 0; columnIndex < lastCell; columnIndex++) {
            if (readCell(formatter, row, columnIndex) != null) {
                return false;
            }
        }
        return true;
    }

    private void validateNoExtraColumns(DataFormatter formatter,
                                        Row row,
                                        int excelRowNumber,
                                        List<String> errors) {
        if (row == null || row.getLastCellNum() <= IMPORT_COLUMN_COUNT) {
            return;
        }
        for (int columnIndex = IMPORT_COLUMN_COUNT; columnIndex < row.getLastCellNum(); columnIndex++) {
            if (readCell(formatter, row, columnIndex) != null) {
                errors.add("Dòng " + excelRowNumber + ": file chỉ được có đúng 6 cột.");
                return;
            }
        }
    }

    private String readTextCell(DataFormatter formatter,
                                Row row,
                                int cellIndex,
                                String fieldName,
                                int excelRowNumber,
                                List<String> errors) {
        Cell cell = row.getCell(cellIndex);
        if (cell != null && cell.getCellType() == CellType.FORMULA) {
            errors.add("Dòng " + excelRowNumber + ": " + fieldName + " không được dùng công thức.");
            return null;
        }
        if (cell != null && cell.getCellType() != CellType.BLANK
                && cell.getCellType() != CellType.STRING) {
            errors.add("Dòng " + excelRowNumber + ": " + fieldName + " phải là văn bản.");
            return null;
        }
        return readCell(formatter, row, cellIndex);
    }

    private void validateRequiredAndLength(String value,
                                           String fieldName,
                                           int maxLength,
                                           int excelRowNumber,
                                           List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add("Dòng " + excelRowNumber + ": " + fieldName + " là bắt buộc.");
        } else if (value.length() > maxLength) {
            errors.add("Dòng " + excelRowNumber + ": " + fieldName
                    + " không được vượt quá " + maxLength + " ký tự.");
        }
    }

    private BigDecimal readDecimalCell(Row row,
                                       int cellIndex,
                                       String fieldName,
                                       int excelRowNumber,
                                       int maxScale,
                                       int maxIntegerDigits,
                                       List<String> errors) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return BigDecimal.ZERO;
        }
        if (cell.getCellType() == CellType.FORMULA) {
            errors.add("Dòng " + excelRowNumber + ": " + fieldName + " không được dùng công thức.");
            return BigDecimal.ZERO;
        }

        String rawValue;
        if (cell.getCellType() == CellType.NUMERIC) {
            rawValue = NumberToTextConverter.toText(cell.getNumericCellValue());
        } else if (cell.getCellType() == CellType.STRING) {
            rawValue = cell.getStringCellValue().trim().replace(',', '.');
        } else {
            errors.add("Dòng " + excelRowNumber + ": " + fieldName + " phải là số.");
            return BigDecimal.ZERO;
        }

        if (!rawValue.matches(maxScale == 0 ? "\\d+" : "\\d+(?:\\.\\d{1," + maxScale + "})?")) {
            errors.add("Dòng " + excelRowNumber + ": " + fieldName
                    + (maxScale == 0
                    ? " phải là số nguyên không âm, không dùng dấu phân cách hàng nghìn."
                    : " phải là số không âm, tối đa " + maxScale
                    + " chữ số thập phân và không dùng dấu phân cách hàng nghìn."));
            return BigDecimal.ZERO;
        }

        BigDecimal value = new BigDecimal(rawValue);
        int integerDigits = Math.max(value.precision() - value.scale(), 1);
        if (integerDigits > maxIntegerDigits) {
            errors.add("Dòng " + excelRowNumber + ": " + fieldName + " vượt quá giới hạn lưu trữ.");
            return BigDecimal.ZERO;
        }
        return value;
    }

    private String buildImportErrorMessage(List<String> errors) {
        int displayCount = Math.min(errors.size(), 10);
        StringBuilder message = new StringBuilder("File Excel có ")
                .append(errors.size())
                .append(" lỗi: ");
        for (int index = 0; index < displayCount; index++) {
            if (index > 0) {
                message.append(" | ");
            }
            message.append(errors.get(index));
        }
        if (errors.size() > displayCount) {
            message.append(" | ... và ").append(errors.size() - displayCount).append(" lỗi khác.");
        }
        return message.toString();
    }

    private void validatePositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateMaxScale(BigDecimal value, int maxScale, String fieldName) {
        if (value == null) {
            return;
        }
        if (value.stripTrailingZeros().scale() > maxScale) {
            throw new IllegalArgumentException(fieldName + " chỉ được nhập tối đa "
                    + maxScale + " chữ số thập phân.");
        }
    }

    private void validateNaturalNumber(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException(fieldName + " phải là số tự nhiên.");
        }
        validateWholeNumber(value, fieldName);
    }

    private void validateWholeNumber(BigDecimal value, String fieldName) {
        if (value == null) {
            return;
        }
        if (value.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(fieldName + " phải là số nguyên VND, không nhập số thập phân.");
        }
    }

    private void ensureSufficientStock(InventoryItem item, BigDecimal consumedQuantity) {
        if (item.getCurrentQuantity().compareTo(consumedQuantity) < 0) {
            throw new IllegalArgumentException(
                    "Không đủ tồn kho cho " + item.getName()
                            + ". Hiện có " + item.getCurrentQuantity() + " " + item.getUnit() + ".");
        }
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeStockStatus(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (!"OUT_OF_STOCK".equals(normalized)
                && !"LOW".equals(normalized)
                && !"NORMAL".equals(normalized)) {
            return null;
        }
        return normalized;
    }

    private InventoryCategory getCategory(Long categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Loại hàng là bắt buộc.");
        }
        return inventoryCategoryRepository.findByIdAndIsActiveTrue(categoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Loại hàng không tồn tại hoặc đã ngừng sử dụng."));
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
                .divide(totalQuantity, 0, java.math.RoundingMode.HALF_UP);
    }

    private String generateReceiptCode() {
        String code;
        do {
            int randomNumber = (int) (Math.random() * 9000) + 1000;
            code = "IR-" + LocalDate.now(APP_ZONE).format(CODE_DATE_FORMATTER) + "-" + randomNumber;
        } while (inventoryReceiptRepository.existsByCode(code));
        return code;
    }

    private String generateBatchCode(LocalDate receiptDate) {
        LocalDate effectiveReceiptDate = receiptDate == null ? LocalDate.now(APP_ZONE) : receiptDate;
        String prefix = "LOT-" + effectiveReceiptDate.format(CODE_DATE_FORMATTER) + "-";
        int nextNumber = inventoryReceiptRepository
                .findTopByBatchCodeStartingWithOrderByBatchCodeDesc(prefix)
                .map(InventoryReceipt::getBatchCode)
                .map(code -> code.substring(prefix.length()))
                .filter(suffix -> suffix.matches("\\d{3}"))
                .map(Integer::parseInt)
                .orElse(0) + 1;

        if (nextNumber > 999) {
            throw new IllegalArgumentException("Đã vượt quá số lượng mã lô trong ngày nhập kho này.");
        }

        String batchCode;
        do {
            batchCode = prefix + "%03d".formatted(nextNumber++);
        } while (inventoryReceiptRepository.existsByBatchCode(batchCode));
        return batchCode;
    }
}

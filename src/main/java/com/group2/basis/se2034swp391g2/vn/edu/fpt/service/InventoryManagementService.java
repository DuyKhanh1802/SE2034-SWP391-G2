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
import org.apache.poi.ss.usermodel.DateUtil;
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
import java.text.Normalizer;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryManagementService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
    private static final List<DateTimeFormatter> IMPORT_DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    );
    private static final long MAX_IMPORT_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_IMPORT_ROWS = 1000;
    private static final int ITEM_IMPORT_COLUMN_COUNT = 4;
    private static final List<String> ITEM_IMPORT_HEADERS = List.of(
            "ten_hang",
            "loai",
            "don_vi",
            "nguong_canh_bao");
    private static final int RECEIPT_IMPORT_COLUMN_COUNT = 7;
    private static final List<String> RECEIPT_IMPORT_HEADERS = List.of(
            "ten_hang",
            "so_luong",
            "gia_von_truoc_vat",
            "han_su_dung",
            "phuong_thuc_thanh_toan",
            "nha_cung_cap",
            "ghi_chu");

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
        return inventoryCategoryRepository.findAllByOrderByNameAsc();
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
                .orElseThrow(() -> new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y hÃ ng hÃ³a."));
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
            throw new IllegalArgumentException("Chá»‰ cÃ³ thá»ƒ xÃ³a hÃ ng hÃ³a khi tá»“n kho báº±ng 0.");
        }
        if (serviceInventoryMappingRepository.existsByItem_Id(id)
                || roomRefreshInventoryMappingRepository.existsByItem_Id(id)) {
            throw new IllegalArgumentException("HÃ£y xÃ³a toÃ n bá»™ quy táº¯c tiÃªu hao trÆ°á»›c khi xÃ³a hÃ ng hÃ³a.");
        }
        item.setIsDeleted(true);
        inventoryItemRepository.save(item);
    }

    @Transactional
    public InventoryImportResult importItemsFromExcel(MultipartFile file, User createdBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lÃ²ng chá»n file Excel cáº§n import.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Chá»‰ há»— trá»£ file Excel Ä‘á»‹nh dáº¡ng .xlsx.");
        }
        if (file.getSize() > MAX_IMPORT_FILE_SIZE) {
            throw new IllegalArgumentException("File Excel khÃ´ng Ä‘Æ°á»£c vÆ°á»£t quÃ¡ 5 MB.");
        }

        Workbook workbook;
        try {
            workbook = new XSSFWorkbook(file.getInputStream());
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("File khÃ´ng pháº£i workbook .xlsx há»£p lá»‡ hoáº·c Ä‘Ã£ bá»‹ há»ng.");
        }

        try (workbook) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("File Excel khÃ´ng cÃ³ sheet dá»¯ liá»‡u.");
            }

            DataFormatter formatter = new DataFormatter(Locale.US);
            Sheet sheet = workbook.getSheetAt(0);
            validateImportHeader(sheet, formatter, ITEM_IMPORT_HEADERS, ITEM_IMPORT_COLUMN_COUNT);

            if (sheet.getLastRowNum() > MAX_IMPORT_ROWS) {
                throw new IllegalArgumentException("File Excel chá»‰ Ä‘Æ°á»£c chá»©a tá»‘i Ä‘a 1.000 dÃ²ng dá»¯ liá»‡u.");
            }

            List<InventoryImportRow> validRows = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            Set<String> namesInFile = new HashSet<>();
            int skippedCount = 0;

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyRow(formatter, row, ITEM_IMPORT_COLUMN_COUNT)) {
                    continue;
                }

                int excelRowNumber = rowIndex + 1;
                List<String> rowErrors = new ArrayList<>();
                validateNoExtraColumns(formatter, row, excelRowNumber, ITEM_IMPORT_COLUMN_COUNT, rowErrors);

                String name = readTextCell(formatter, row, 0, "TÃªn hÃ ng", excelRowNumber, rowErrors);
                String categoryName = readTextCell(formatter, row, 1, "Loáº¡i hÃ ng", excelRowNumber, rowErrors);
                String unit = readTextCell(formatter, row, 2, "ÄÆ¡n vá»‹ tÃ­nh", excelRowNumber, rowErrors);

                validateRequiredAndLength(name, "TÃªn hÃ ng", 150, excelRowNumber, rowErrors);
                validateRequiredAndLength(categoryName, "Loáº¡i hÃ ng", 100, excelRowNumber, rowErrors);
                validateRequiredAndLength(unit, "ÄÆ¡n vá»‹ tÃ­nh", 30, excelRowNumber, rowErrors);

                InventoryCategory category = null;
                if (categoryName != null && !categoryName.isBlank()) {
                    category = inventoryCategoryRepository
                            .findByNameIgnoreCase(categoryName)
                            .orElse(null);
                    if (category == null) {
                        rowErrors.add("DÃ²ng " + excelRowNumber + ": loáº¡i hÃ ng '" + categoryName
                                + "' khÃ´ng tá»“n táº¡i.");
                    }
                }

                BigDecimal minimumQuantity = readDecimalCell(
                        row, 3, "NgÆ°á»¡ng cáº£nh bÃ¡o", excelRowNumber, 2, 10, rowErrors);
                String normalizedName = name == null ? null : name.trim().toLowerCase(Locale.ROOT);
                if (normalizedName != null && !normalizedName.isBlank() && !namesInFile.add(normalizedName)) {
                    rowErrors.add("DÃ²ng " + excelRowNumber + ": tÃªn hÃ ng bá»‹ trÃ¹ng trong chÃ­nh file Excel.");
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
                        minimumQuantity, BigDecimal.ZERO));
            }

            if (!errors.isEmpty()) {
                throw new IllegalArgumentException(buildImportErrorMessage(errors));
            }
            if (validRows.isEmpty() && skippedCount == 0) {
                throw new IllegalArgumentException("File Excel khÃ´ng cÃ³ dÃ²ng hÃ ng hÃ³a nÃ o.");
            }

            for (InventoryImportRow row : validRows) {
                createItem(row.name(), row.category(), row.unit(),
                        row.minimumQuantity(), row.unitCost());
            }
            return new InventoryImportResult(validRows.size(), skippedCount);
        } catch (IOException e) {
            throw new IllegalArgumentException("KhÃ´ng thá»ƒ Ä‘Ã³ng hoáº·c hoÃ n táº¥t viá»‡c Ä‘á»c file Excel.");
        }
    }

    private InventoryItem createItem(String name,
                                     InventoryCategory category,
                                     String unit,
                                     BigDecimal minimumQuantity,
                                     BigDecimal unitCost) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("TÃªn hÃ ng hÃ³a lÃ  báº¯t buá»™c.");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("ÄÆ¡n vá»‹ tÃ­nh lÃ  báº¯t buá»™c.");
        }
        if (category == null) {
            throw new IllegalArgumentException("Loáº¡i hÃ ng lÃ  báº¯t buá»™c.");
        }
        if (inventoryItemRepository.existsByNameIgnoreCaseAndIsDeletedFalse(name.trim())) {
            throw new IllegalArgumentException("HÃ ng hÃ³a Ä‘Ã£ tá»“n táº¡i.");
        }

        BigDecimal normalizedMinimumQuantity = normalizeQuantity(minimumQuantity, "Má»©c tá»“n tá»‘i thiá»ƒu", false);
        BigDecimal normalizedUnitCost = normalizeNonNegative(unitCost);
        validateWholeNumber(normalizedUnitCost, "GiÃ¡ vá»‘n trÆ°á»›c VAT");
        InventoryItem item = InventoryItem.builder()
                .name(name.trim())
                .category(category)
                .unit(unit.trim())
                .currentQuantity(BigDecimal.ZERO)
                .minimumQuantity(normalizedMinimumQuantity)
                .unitCost(normalizedUnitCost)
                .isDeleted(false)
                .build();

        return inventoryItemRepository.save(item);
    }

    public record InventoryImportResult(int importedCount, int skippedCount) {
    }

    private record InventoryImportRow(String name,
                                      InventoryCategory category,
                                      String unit,
                                      BigDecimal minimumQuantity,
                                      BigDecimal unitCost) {
    }

    @Transactional
    public ReceiptImportResult importReceiptsFromExcel(MultipartFile file, User createdBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lÃ²ng chá»n file Excel cáº§n import.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Chá»‰ há»— trá»£ file Excel Ä‘á»‹nh dáº¡ng .xlsx.");
        }
        if (file.getSize() > MAX_IMPORT_FILE_SIZE) {
            throw new IllegalArgumentException("File Excel khÃ´ng Ä‘Æ°á»£c vÆ°á»£t quÃ¡ 5 MB.");
        }

        Workbook workbook;
        try {
            workbook = new XSSFWorkbook(file.getInputStream());
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("File khÃ´ng pháº£i workbook .xlsx há»£p lá»‡ hoáº·c Ä‘Ã£ bá»‹ há»ng.");
        }

        try (workbook) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("File Excel khÃ´ng cÃ³ sheet dá»¯ liá»‡u.");
            }

            DataFormatter formatter = new DataFormatter(Locale.US);
            Sheet sheet = workbook.getSheetAt(0);
            validateImportHeader(sheet, formatter, RECEIPT_IMPORT_HEADERS, RECEIPT_IMPORT_COLUMN_COUNT);

            if (sheet.getLastRowNum() > MAX_IMPORT_ROWS) {
                throw new IllegalArgumentException("File Excel chá»‰ Ä‘Æ°á»£c chá»©a tá»‘i Ä‘a 1.000 dÃ²ng dá»¯ liá»‡u.");
            }

            List<ReceiptImportRow> validRows = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyRow(formatter, row, RECEIPT_IMPORT_COLUMN_COUNT)) {
                    continue;
                }

                int excelRowNumber = rowIndex + 1;
                List<String> rowErrors = new ArrayList<>();
                validateNoExtraColumns(formatter, row, excelRowNumber, RECEIPT_IMPORT_COLUMN_COUNT, rowErrors);

                String itemName = readTextCell(formatter, row, 0, "TÃªn hÃ ng", excelRowNumber, rowErrors);
                validateRequiredAndLength(itemName, "TÃªn hÃ ng", 150, excelRowNumber, rowErrors);
                InventoryItem item = null;
                if (itemName != null && !itemName.isBlank()) {
                    item = inventoryItemRepository.findByNameIgnoreCaseAndIsDeletedFalse(itemName.trim()).orElse(null);
                    if (item == null) {
                        rowErrors.add("DÃ²ng " + excelRowNumber + ": hÃ ng hÃ³a '" + itemName + "' khÃ´ng tá»“n táº¡i.");
                    }
                }

                BigDecimal quantity = readDecimalCell(
                        row, 1, "Sá»‘ lÆ°á»£ng nháº­p", excelRowNumber, 2, 10, rowErrors);
                BigDecimal unitCost = readDecimalCell(
                        row, 2, "GiÃ¡ vá»‘n trÆ°á»›c VAT", excelRowNumber, 0, 15, rowErrors);
                LocalDate expiryDate = readDateCell(
                        formatter, row, 3, "Háº¡n sá»­ dá»¥ng", excelRowNumber, rowErrors);
                String paymentMethodText = readTextCell(
                        formatter, row, 4, "PhÆ°Æ¡ng thá»©c thanh toÃ¡n", excelRowNumber, rowErrors);
                String supplier = readTextCell(formatter, row, 5, "NhÃ  cung cáº¥p", excelRowNumber, rowErrors);
                String note = readTextCell(formatter, row, 6, "Ghi chÃº", excelRowNumber, rowErrors);

                validatePositiveQuantity(quantity, "Sá»‘ lÆ°á»£ng nháº­p", excelRowNumber, rowErrors);
                validatePositiveMoney(unitCost, "GiÃ¡ vá»‘n trÆ°á»›c VAT", excelRowNumber, rowErrors);
                if (expiryDate != null && !expiryDate.isAfter(LocalDate.now(APP_ZONE))) {
                    rowErrors.add("DÃ²ng " + excelRowNumber + ": háº¡n sá»­ dá»¥ng pháº£i sau ngÃ y nháº­p kho.");
                }
                validateOptionalLength(supplier, "NhÃ  cung cáº¥p", 150, excelRowNumber, rowErrors);
                validateOptionalLength(note, "Ghi chÃº", 300, excelRowNumber, rowErrors);

                PaymentMethod paymentMethod = parseImportPaymentMethod(paymentMethodText, excelRowNumber, rowErrors);

                if (!rowErrors.isEmpty()) {
                    errors.addAll(rowErrors);
                    continue;
                }

                validRows.add(new ReceiptImportRow(
                        item, quantity, unitCost, expiryDate, paymentMethod,
                        normalizeText(supplier), normalizeText(note)));
            }

            if (!errors.isEmpty()) {
                throw new IllegalArgumentException(buildImportErrorMessage(errors));
            }
            if (validRows.isEmpty()) {
                throw new IllegalArgumentException("File Excel khÃ´ng cÃ³ dÃ²ng lÃ´ hÃ ng nháº­p nÃ o.");
            }

            BigDecimal totalCost = BigDecimal.ZERO;
            for (ReceiptImportRow row : validRows) {
                InventoryReceipt receipt = createReceipt(
                        row.item().getId(),
                        row.quantity(),
                        row.unitCost(),
                        row.supplier(),
                        row.note(),
                        null,
                        null,
                        row.expiryDate(),
                        row.paymentMethod(),
                        createdBy);
                totalCost = totalCost.add(receipt.getTotalCost());
            }
            return new ReceiptImportResult(validRows.size(), totalCost);
        } catch (IOException e) {
            throw new IllegalArgumentException("KhÃ´ng thá»ƒ Ä‘Ã³ng hoáº·c hoÃ n táº¥t viá»‡c Ä‘á»c file Excel.");
        }
    }

    public record ReceiptImportResult(int importedCount, BigDecimal totalCost) {
    }

    private record ReceiptImportRow(InventoryItem item,
                                    BigDecimal quantity,
                                    BigDecimal unitCost,
                                    LocalDate expiryDate,
                                    PaymentMethod paymentMethod,
                                    String supplier,
                                    String note) {
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
        validatePositive(quantity, "Sá»‘ lÆ°á»£ng nháº­p pháº£i lá»›n hÆ¡n 0.");
        validateMaxScale(quantity, 2, "Sá»‘ lÆ°á»£ng nháº­p");
        validatePositive(unitCost, "ÄÆ¡n giÃ¡ trÆ°á»›c VAT / 1 Ä‘Æ¡n vá»‹ pháº£i lá»›n hÆ¡n 0.");
        validateNaturalNumber(unitCost, "ÄÆ¡n giÃ¡ trÆ°á»›c VAT / 1 Ä‘Æ¡n vá»‹");
        validateRequired(paymentMethod, "PhÆ°Æ¡ng thá»©c thanh toÃ¡n lÃ  báº¯t buá»™c.");
        validateOptionalLength(supplier, "NhÃ  cung cáº¥p", 150);
        validateOptionalLength(note, "Ghi chÃº", 300);
        LocalDate effectiveReceiptDate = receiptDate == null ? LocalDate.now(APP_ZONE) : receiptDate;
        if (effectiveReceiptDate.isAfter(LocalDate.now(APP_ZONE))) {
            throw new IllegalArgumentException("NgÃ y nháº­p kho khÃ´ng Ä‘Æ°á»£c á»Ÿ tÆ°Æ¡ng lai.");
        }
        if (expiryDate != null && !expiryDate.isAfter(effectiveReceiptDate)) {
            throw new IllegalArgumentException("Háº¡n sá»­ dá»¥ng pháº£i sau ngÃ y nháº­p kho.");
        }
        String generatedBatchCode = expiryDate == null ? null : generateBatchCode(effectiveReceiptDate);

        BigDecimal subtotal = quantity.multiply(unitCost).setScale(0, java.math.RoundingMode.HALF_UP);
        BigDecimal vatRate = financialChargeService.getInventoryVatRate();
        BigDecimal vatAmount = financialChargeService.calculateRateAmount(subtotal, vatRate);
        BigDecimal totalCost = subtotal.add(vatAmount).setScale(0, java.math.RoundingMode.HALF_UP);
        InventoryReceipt receipt = InventoryReceipt.builder()
                .code(generateTemporaryReceiptCode())
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

        InventoryReceipt savedReceipt = inventoryReceiptRepository.saveAndFlush(receipt);
        savedReceipt.setCode(formatReceiptCode(savedReceipt.getId()));
        savedReceipt = inventoryReceiptRepository.saveAndFlush(savedReceipt);
        BigDecimal existingQuantity = item.getCurrentQuantity();
        item.setUnitCost(calculateWeightedUnitCost(item.getUnitCost(), existingQuantity, unitCost, quantity));
        item.setCurrentQuantity(item.getCurrentQuantity().add(quantity));
        inventoryItemRepository.save(item);

        recordInventoryTransaction(item, InventoryTransactionType.IN, quantity,
                "INVENTORY_RECEIPT", savedReceipt.getId(), createdBy);

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
        validatePositive(quantity, "Sá»‘ lÆ°á»£ng há»§y pháº£i lá»›n hÆ¡n 0.");
        validateMaxScale(quantity, 2, "Sá»‘ lÆ°á»£ng há»§y");
        ensureSufficientStock(item, quantity);

        item.setCurrentQuantity(item.getCurrentQuantity().subtract(quantity));
        inventoryItemRepository.save(item);

        recordInventoryTransaction(item, InventoryTransactionType.DISPOSAL, quantity,
                "INVENTORY_DISPOSAL", item.getId(), createdBy, normalizeText(reason));
    }

    @Transactional
    public ServiceInventoryMapping linkItemToService(Long serviceId,
                                                     Long itemId,
                                                     BigDecimal quantityPerUse) {
        com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service =
                serviceRepository.findById(serviceId)
                        .orElseThrow(() -> new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y dá»‹ch vá»¥."));
        InventoryItem item = getItem(itemId);
        validatePositive(quantityPerUse, "Sá»‘ lÆ°á»£ng tiÃªu hao pháº£i lá»›n hÆ¡n 0.");
        validateMaxScale(quantityPerUse, 2, "Sá»‘ lÆ°á»£ng tiÃªu hao");

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
                .orElseThrow(() -> new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y háº¡ng phÃ²ng."));
        InventoryItem item = getItem(itemId);
        validatePositive(quantityPerRefresh, "Sá»‘ lÆ°á»£ng láº¥p Ä‘á»“ pháº£i lá»›n hÆ¡n 0.");
        validateMaxScale(quantityPerRefresh, 2, "Sá»‘ lÆ°á»£ng láº¥p Ä‘á»“");

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
                .orElseThrow(() -> new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y quy táº¯c tiÃªu hao dá»‹ch vá»¥."));
        serviceInventoryMappingRepository.delete(mapping);
    }

    @Transactional
    public void deleteRoomRefreshMapping(Long itemId, Long mappingId) {
        getItem(itemId);
        RoomRefreshInventoryMapping mapping = roomRefreshInventoryMappingRepository.findByIdAndItem_Id(mappingId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y quy táº¯c láº¥p Ä‘á»“ phÃ²ng."));
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
        validatePositive(multiplier, "Sá»‘ lÆ°á»£ng dá»‹ch vá»¥ pháº£i lá»›n hÆ¡n 0.");
        validateMaxScale(multiplier, 2, "Sá»‘ lÆ°á»£ng dá»‹ch vá»¥");
        if (sourceId == null) {
            throw new IllegalArgumentException("Thiáº¿u mÃ£ dá»‹ch vá»¥ trong hÃ³a Ä‘Æ¡n Ä‘á»ƒ ghi nháº­n tiÃªu hao kho.");
        }
        List<ServiceInventoryMapping> mappings = serviceInventoryMappingRepository.findByService_Id(service.getId());
        if (mappings.isEmpty()) {
            throw new IllegalStateException("Dá»‹ch vá»¥ chÆ°a Ä‘Æ°á»£c cáº¥u hÃ¬nh tiÃªu hao kho. Vui lÃ²ng liÃªn há»‡ thá»§ kho.");
        }

        for (ServiceInventoryMapping mapping : mappings) {
            if (mapping.getItem() == null) {
                throw new IllegalStateException("Quy táº¯c tiÃªu hao dá»‹ch vá»¥ Ä‘ang thiáº¿u hÃ ng hÃ³a.");
            }
            BigDecimal consumedQuantity = mapping.getQuantityPerUse().multiply(multiplier);
            consumeInventoryItem(mapping.getItem(), mapping.getQuantityPerUse(), consumedQuantity,
                    "Quy tắc tiêu hao dịch vụ", "FOLIO_ITEM", sourceId, createdBy);
        }
    }

    @Transactional
    public void consumeForRoomRefresh(RoomType roomType,
                                      Long sourceId,
                                      User createdBy) {
        if (roomType != null && roomType.getId() == null) {
            throw new IllegalArgumentException("Hạng phòng không hợp lệ để ghi nhận tiêu hao lấp đồ.");
        }
        if (roomType == null) {
            throw new IllegalArgumentException("ThiÃ¡ÂºÂ¿u hÃ¡ÂºÂ¡ng phÃƒÂ²ng Ã„â€˜Ã¡Â»Æ’ ghi nhÃ¡ÂºÂ­n tiÃƒÂªu hao lÃ¡ÂºÂ¥p Ã„â€˜Ã¡Â»â€œ.");
        }
        if (sourceId == null) {
            throw new IllegalArgumentException("ThiÃ¡ÂºÂ¿u mÃƒÂ£ phÃƒÂ²ng Ã„â€˜Ã¡Â»Æ’ ghi nhÃ¡ÂºÂ­n tiÃƒÂªu hao lÃ¡ÂºÂ¥p Ã„â€˜Ã¡Â»â€œ.");
        }

        List<RoomRefreshInventoryMapping> mappings = roomRefreshInventoryMappingRepository.findByRoomType_Id(roomType.getId());
        if (mappings.isEmpty()) {
            return;
        }

        for (RoomRefreshInventoryMapping mapping : mappings) {
            if (mapping.getItem() == null) {
                throw new IllegalStateException("Quy tÃ¡ÂºÂ¯c lÃ¡ÂºÂ¥p Ã„â€˜Ã¡Â»â€œ phÃƒÂ²ng Ã„â€˜ang thiÃ¡ÂºÂ¿u hÃƒÂ ng hÃƒÂ³a.");
            }
            BigDecimal consumedQuantity = mapping.getQuantityPerRefresh();
            consumeInventoryItem(mapping.getItem(), mapping.getQuantityPerRefresh(), consumedQuantity,
                    "Quy tắc lấp đồ phòng", "ROOM_REFRESH", sourceId, createdBy);
        }
    }

    private void consumeInventoryItem(InventoryItem item,
                                      BigDecimal configuredQuantity,
                                      BigDecimal consumedQuantity,
                                      String ruleName,
                                      String sourceType,
                                      Long sourceId,
                                      User createdBy) {
        validatePositive(configuredQuantity, "Số lượng tiêu hao phải lớn hơn 0.");
        validateMaxScale(configuredQuantity, 2, "Số lượng tiêu hao");
        validateConsumableInventoryItem(item, ruleName);
        ensureSufficientStock(item, consumedQuantity);

        item.setCurrentQuantity(item.getCurrentQuantity().subtract(consumedQuantity));
        inventoryItemRepository.save(item);
        recordInventoryTransaction(item, InventoryTransactionType.OUT, consumedQuantity,
                sourceType, sourceId, createdBy);
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
            throw new IllegalArgumentException("NgÃ y báº¯t Ä‘áº§u khÃ´ng Ä‘Æ°á»£c sau ngÃ y káº¿t thÃºc.");
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
                                            String sourceType,
                                            Long sourceId,
                                            User createdBy) {
        recordInventoryTransaction(item, type, quantity, sourceType, sourceId, createdBy, null);
    }

    private void recordInventoryTransaction(InventoryItem item,
                                            InventoryTransactionType type,
                                            BigDecimal quantity,
                                            String sourceType,
                                            Long sourceId,
                                            User createdBy,
                                            String reason) {
        InventoryTransaction transaction = InventoryTransaction.builder()
                .item(item)
                .type(type)
                .quantity(quantity)
                .remainingQuantity(item.getCurrentQuantity())
                .sourceType(sourceType)
                .sourceId(sourceId)
                .reason(reason)
                .createdBy(createdBy)
                .build();

        inventoryTransactionRepository.save(transaction);
    }

    private BigDecimal normalizeNonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("GiÃ¡ trá»‹ khÃ´ng Ä‘Æ°á»£c Ã¢m.");
        }
        return value;
    }

    private BigDecimal normalizeQuantity(BigDecimal value, String fieldName, boolean positive) {
        if (positive) {
            validatePositive(value, fieldName + " pháº£i lá»›n hÆ¡n 0.");
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

    private void validateImportHeader(Sheet sheet,
                                      DataFormatter formatter,
                                      List<String> expectedHeaders,
                                      int expectedColumnCount) {
        Row header = sheet.getRow(0);
        if (header == null) {
            throw new IllegalArgumentException("Thiáº¿u dÃ²ng tiÃªu Ä‘á» cá»§a file Excel.");
        }
        List<String> actualHeaders = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < expectedColumnCount; columnIndex++) {
            String value = readCell(formatter, header, columnIndex);
            actualHeaders.add(value == null ? "" : value.toLowerCase(Locale.ROOT));
        }
        if (!actualHeaders.equals(expectedHeaders)) {
            throw new IllegalArgumentException(
                    "TiÃªu Ä‘á» khÃ´ng Ä‘Ãºng máº«u. Thá»© tá»± báº¯t buá»™c: " + String.join(", ", expectedHeaders) + ".");
        }
        List<String> errors = new ArrayList<>();
        validateNoExtraColumns(formatter, header, 1, expectedColumnCount, errors);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.getFirst());
        }
    }

    private boolean isEmptyRow(DataFormatter formatter, Row row, int expectedColumnCount) {
        if (row == null) {
            return true;
        }
        int lastCell = Math.max(row.getLastCellNum(), expectedColumnCount);
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
                                        int expectedColumnCount,
                                        List<String> errors) {
        if (row == null || row.getLastCellNum() <= expectedColumnCount) {
            return;
        }
        for (int columnIndex = expectedColumnCount; columnIndex < row.getLastCellNum(); columnIndex++) {
            if (readCell(formatter, row, columnIndex) != null) {
                errors.add("DÃ²ng " + excelRowNumber + ": file chá»‰ Ä‘Æ°á»£c cÃ³ Ä‘Ãºng " + expectedColumnCount + " cá»™t.");
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
            errors.add("DÃ²ng " + excelRowNumber + ": " + fieldName + " khÃ´ng Ä‘Æ°á»£c dÃ¹ng cÃ´ng thá»©c.");
            return null;
        }
        if (cell != null && cell.getCellType() != CellType.BLANK
                && cell.getCellType() != CellType.STRING) {
            errors.add("DÃ²ng " + excelRowNumber + ": " + fieldName + " pháº£i lÃ  vÄƒn báº£n.");
            return null;
        }
        return readCell(formatter, row, cellIndex);
    }

    private LocalDate readDateCell(DataFormatter formatter,
                                   Row row,
                                   int cellIndex,
                                   String fieldName,
                                   int excelRowNumber,
                                   List<String> errors) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.FORMULA) {
            errors.add("DÃ²ng " + excelRowNumber + ": " + fieldName + " khÃ´ng Ä‘Æ°á»£c dÃ¹ng cÃ´ng thá»©c.");
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            if (!DateUtil.isCellDateFormatted(cell)) {
                errors.add("DÃ²ng " + excelRowNumber + ": " + fieldName
                        + " pháº£i lÃ  ngÃ y há»£p lá»‡ theo Ä‘á»‹nh dáº¡ng yyyy-MM-dd hoáº·c dd/MM/yyyy.");
                return null;
            }
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        if (cell.getCellType() != CellType.STRING) {
            errors.add("DÃ²ng " + excelRowNumber + ": " + fieldName
                    + " pháº£i lÃ  ngÃ y há»£p lá»‡ theo Ä‘á»‹nh dáº¡ng yyyy-MM-dd hoáº·c dd/MM/yyyy.");
            return null;
        }

        String rawValue = readCell(formatter, row, cellIndex);
        if (rawValue == null) {
            return null;
        }
        for (DateTimeFormatter dateFormatter : IMPORT_DATE_FORMATTERS) {
            try {
                return LocalDate.parse(rawValue, dateFormatter);
            } catch (DateTimeParseException ignored) {
                // Try next supported format.
            }
        }
        errors.add("DÃ²ng " + excelRowNumber + ": " + fieldName
                + " pháº£i lÃ  ngÃ y há»£p lá»‡ theo Ä‘á»‹nh dáº¡ng yyyy-MM-dd hoáº·c dd/MM/yyyy.");
        return null;
    }

    private void validateRequiredAndLength(String value,
                                           String fieldName,
                                           int maxLength,
                                           int excelRowNumber,
                                           List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add("DÃ²ng " + excelRowNumber + ": " + fieldName + " lÃ  báº¯t buá»™c.");
        } else if (value.length() > maxLength) {
            errors.add("DÃ²ng " + excelRowNumber + ": " + fieldName
                    + " khÃ´ng Ä‘Æ°á»£c vÆ°á»£t quÃ¡ " + maxLength + " kÃ½ tá»±.");
        }
    }

    private void validateOptionalLength(String value,
                                        String fieldName,
                                        int maxLength,
                                        int excelRowNumber,
                                        List<String> errors) {
        if (value != null && value.length() > maxLength) {
            errors.add("DÃ²ng " + excelRowNumber + ": " + fieldName
                    + " khÃ´ng Ä‘Æ°á»£c vÆ°á»£t quÃ¡ " + maxLength + " kÃ½ tá»±.");
        }
    }

    private void validateOptionalLength(String value, String fieldName, int maxLength) {
        if (value != null && value.trim().length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " khÃ´ng Ä‘Æ°á»£c vÆ°á»£t quÃ¡ " + maxLength + " kÃ½ tá»±.");
        }
    }

    private void validateRequired(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validatePositiveQuantity(BigDecimal value,
                                          String fieldName,
                                          int excelRowNumber,
                                          List<String> errors) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("DÃ²ng " + excelRowNumber + ": " + fieldName + " pháº£i lá»›n hÆ¡n 0.");
        }
    }

    private void validatePositiveMoney(BigDecimal value,
                                       String fieldName,
                                       int excelRowNumber,
                                       List<String> errors) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("DÃ²ng " + excelRowNumber + ": " + fieldName + " pháº£i lÃ  sá»‘ nguyÃªn VND lá»›n hÆ¡n 0.");
        }
    }

    private PaymentMethod parseImportPaymentMethod(String value, int excelRowNumber, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add("DÃ²ng " + excelRowNumber + ": PhÆ°Æ¡ng thá»©c thanh toÃ¡n lÃ  báº¯t buá»™c.");
            return null;
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .toUpperCase(Locale.ROOT)
                .replaceAll("[\\s_-]+", "");
        return switch (normalized) {
            case "CASH", "TIENMAT" -> PaymentMethod.CASH;
            case "TRANSFER", "CHUYENKHOAN" -> PaymentMethod.TRANSFER;
            default -> {
                errors.add("DÃ²ng " + excelRowNumber
                        + ": PhÆ°Æ¡ng thá»©c thanh toÃ¡n chá»‰ nháº­n CASH/Tiá»n máº·t hoáº·c TRANSFER/Chuyá»ƒn khoáº£n.");
                yield null;
            }
        };
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
            errors.add("DÃ²ng " + excelRowNumber + ": " + fieldName + " khÃ´ng Ä‘Æ°á»£c dÃ¹ng cÃ´ng thá»©c.");
            return BigDecimal.ZERO;
        }

        String rawValue;
        if (cell.getCellType() == CellType.NUMERIC) {
            rawValue = NumberToTextConverter.toText(cell.getNumericCellValue());
        } else if (cell.getCellType() == CellType.STRING) {
            rawValue = cell.getStringCellValue().trim().replace(',', '.');
        } else {
            errors.add("DÃ²ng " + excelRowNumber + ": " + fieldName + " pháº£i lÃ  sá»‘.");
            return BigDecimal.ZERO;
        }

        if (!rawValue.matches(maxScale == 0 ? "\\d+" : "\\d+(?:\\.\\d{1," + maxScale + "})?")) {
            errors.add("DÃ²ng " + excelRowNumber + ": " + fieldName
                    + (maxScale == 0
                    ? " pháº£i lÃ  sá»‘ nguyÃªn khÃ´ng Ã¢m, khÃ´ng dÃ¹ng dáº¥u phÃ¢n cÃ¡ch hÃ ng nghÃ¬n."
                    : " pháº£i lÃ  sá»‘ khÃ´ng Ã¢m, tá»‘i Ä‘a " + maxScale
                    + " chá»¯ sá»‘ tháº­p phÃ¢n vÃ  khÃ´ng dÃ¹ng dáº¥u phÃ¢n cÃ¡ch hÃ ng nghÃ¬n."));
            return BigDecimal.ZERO;
        }

        BigDecimal value = new BigDecimal(rawValue);
        int integerDigits = Math.max(value.precision() - value.scale(), 1);
        if (integerDigits > maxIntegerDigits) {
            errors.add("DÃ²ng " + excelRowNumber + ": " + fieldName + " vÆ°á»£t quÃ¡ giá»›i háº¡n lÆ°u trá»¯.");
            return BigDecimal.ZERO;
        }
        return value;
    }

    private String buildImportErrorMessage(List<String> errors) {
        int displayCount = Math.min(errors.size(), 10);
        StringBuilder message = new StringBuilder("File Excel cÃ³ ")
                .append(errors.size())
                .append(" lá»—i: ");
        for (int index = 0; index < displayCount; index++) {
            if (index > 0) {
                message.append(" | ");
            }
            message.append(errors.get(index));
        }
        if (errors.size() > displayCount) {
            message.append(" | ... vÃ  ").append(errors.size() - displayCount).append(" lá»—i khÃ¡c.");
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
            throw new IllegalArgumentException(fieldName + " chá»‰ Ä‘Æ°á»£c nháº­p tá»‘i Ä‘a "
                    + maxScale + " chá»¯ sá»‘ tháº­p phÃ¢n.");
        }
    }

    private void validateNaturalNumber(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException(fieldName + " pháº£i lÃ  sá»‘ tá»± nhiÃªn.");
        }
        validateWholeNumber(value, fieldName);
    }

    private void validateWholeNumber(BigDecimal value, String fieldName) {
        if (value == null) {
            return;
        }
        if (value.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(fieldName + " pháº£i lÃ  sá»‘ nguyÃªn VND, khÃ´ng nháº­p sá»‘ tháº­p phÃ¢n.");
        }
    }

    private void validateConsumableInventoryItem(InventoryItem item, String ruleName) {
        if (item == null) {
            throw new IllegalStateException(ruleName + " Ä‘ang thiáº¿u hÃ ng hÃ³a.");
        }
        if (Boolean.TRUE.equals(item.getIsDeleted())) {
            throw new IllegalStateException(ruleName + " Ä‘ang liÃªn káº¿t vá»›i hÃ ng hÃ³a Ä‘Ã£ bá»‹ xÃ³a: " + item.getName() + ".");
        }
        if (item.getCurrentQuantity() == null) {
            throw new IllegalStateException("Tá»“n kho hiá»‡n táº¡i cá»§a " + item.getName() + " khÃ´ng há»£p lá»‡.");
        }
    }

    private void ensureSufficientStock(InventoryItem item, BigDecimal consumedQuantity) {
        if (item.getCurrentQuantity().compareTo(consumedQuantity) < 0) {
            throw new IllegalArgumentException(
                    "KhÃ´ng Ä‘á»§ tá»“n kho cho " + item.getName()
                            + ". Hiá»‡n cÃ³ " + item.getCurrentQuantity() + " " + item.getUnit() + ".");
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

    private String generateTemporaryReceiptCode() {
        return "IR-TMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String formatReceiptCode(Long receiptId) {
        if (receiptId == null) {
            throw new IllegalStateException("KhÃ´ng thá»ƒ sinh mÃ£ phiáº¿u nháº­p khi chÆ°a cÃ³ ID phiáº¿u.");
        }
        return "IR-%04d".formatted(receiptId);
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
            throw new IllegalArgumentException("ÄÃ£ vÆ°á»£t quÃ¡ sá»‘ lÆ°á»£ng mÃ£ lÃ´ trong ngÃ y nháº­p kho nÃ y.");
        }

        String batchCode;
        do {
            batchCode = prefix + "%03d".formatted(nextNumber++);
        } while (inventoryReceiptRepository.existsByBatchCode(batchCode));
        return batchCode;
    }
}

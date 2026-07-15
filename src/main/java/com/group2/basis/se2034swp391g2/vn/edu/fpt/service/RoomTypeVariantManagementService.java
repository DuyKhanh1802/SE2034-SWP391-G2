package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;


import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ViewType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ImageEntityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Image;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomTypeVariant;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomTypeVariantService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ImageRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeVariantRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeVariantServiceRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ServiceRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.VariantServiceProjection;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class RoomTypeVariantManagementService {


    private static final long MAX_IMPORT_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_IMPORT_ROWS = 200;
    private static final int IMPORT_COLUMN_COUNT = 15;
    private static final int SERVICE_IMPORT_COLUMN_COUNT = 6;
    private static final List<String> IMPORT_HEADERS = List.of(
            "hạng phòng", "tên variant", "hướng nhìn", "giá mỗi đêm",
            "diện tích m2", "sức chứa", "người lớn tối đa", "trẻ em tối đa",
            "cho giường phụ", "số giường phụ tối đa", "giá giường phụ",
            "ghi chú giường phụ", "mô tả", "url ảnh chính", "url ảnh phụ"
    );
    private static final List<String> SERVICE_IMPORT_HEADERS = List.of(
            "hạng phòng", "hướng nhìn", "tên dịch vụ", "số lượng", "loại áp dụng", "ghi chú"
    );


    private final RoomTypeVariantRepository roomTypeVariantRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ImageRepository imageRepository;
    private final ServiceRepository serviceRepository;
    private final RoomTypeVariantServiceRepository roomTypeVariantServiceRepository;


    public RoomTypeVariantManagementService(RoomTypeVariantRepository roomTypeVariantRepository,
                                            RoomTypeRepository roomTypeRepository,
                                            ImageRepository imageRepository,
                                            ServiceRepository serviceRepository,
                                            RoomTypeVariantServiceRepository roomTypeVariantServiceRepository) {
        this.roomTypeVariantRepository = roomTypeVariantRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.imageRepository = imageRepository;
        this.serviceRepository = serviceRepository;
        this.roomTypeVariantServiceRepository = roomTypeVariantServiceRepository;
    }


    public List<RoomTypeVariant> getAllActiveVariants() {
        return roomTypeVariantRepository.findByIsDeletedFalseOrderByRoomType_NameAscVariantNameAsc();
    }


    public Page<RoomTypeVariant> searchVariants(String keyword, ViewType viewType, Pageable pageable) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        return roomTypeVariantRepository.searchVariants(normalizedKeyword, viewType, pageable);
    }


    public Map<Long, List<String>> getIncludedServiceLabels(List<Long> variantIds) {
        Map<Long, List<String>> labels = new HashMap<>();
        if (variantIds == null || variantIds.isEmpty()) {
            return labels;
        }


        for (VariantServiceProjection service :
                roomTypeVariantServiceRepository.findIncludedServicesByVariantIds(variantIds)) {
            labels.computeIfAbsent(service.getVariantId(), ignored -> new ArrayList<>())
                    .add(service.getServiceName() + " x" + service.getQuantity());
        }
        return labels;
    }


    @Transactional
    public VariantImportResult importFromExcel(MultipartFile file, User uploadedBy) {
        validateImportFile(file);


        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("File Excel không có sheet dữ liệu.");
            }


            DataFormatter formatter = new DataFormatter(Locale.US);
            Sheet sheet = workbook.getSheetAt(0);
            validateHeader(sheet, formatter);


            if (sheet.getLastRowNum() > MAX_IMPORT_ROWS) {
                throw new IllegalArgumentException("File Excel chỉ được chứa tối đa 200 dòng dữ liệu.");
            }


            List<VariantImportRow> validRows = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            Set<String> configurationsInFile = new HashSet<>();


            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyRow(formatter, row)) {
                    continue;
                }


                int excelRowNumber = rowIndex + 1;
                List<String> rowErrors = new ArrayList<>();
                validateNoExtraColumns(formatter, row, excelRowNumber, rowErrors);


                String roomTypeName = readText(formatter, row, 0, "Hạng phòng", excelRowNumber, rowErrors);
                String variantName = readText(formatter, row, 1, "Tên variant", excelRowNumber, rowErrors);
                String viewText = readText(formatter, row, 2, "Hướng nhìn", excelRowNumber, rowErrors);
                BigDecimal pricePerNight = readDecimal(row, 3, "Giá mỗi đêm", excelRowNumber, false, rowErrors);
                Integer roomSize = readInteger(row, 4, "Diện tích m2", excelRowNumber, true, rowErrors);
                Integer capacity = readInteger(row, 5, "Sức chứa", excelRowNumber, false, rowErrors);
                Integer maxAdults = readInteger(row, 6, "Người lớn tối đa", excelRowNumber, false, rowErrors);
                Integer maxChildren = readInteger(row, 7, "Trẻ em tối đa", excelRowNumber, false, rowErrors);
                String allowExtraBedText = readText(formatter, row, 8, "Cho giường phụ", excelRowNumber, rowErrors);
                Integer maxExtraBeds = readInteger(row, 9, "Số giường phụ tối đa", excelRowNumber, false, rowErrors);
                BigDecimal extraBedPrice = readDecimal(row, 10, "Giá giường phụ", excelRowNumber, true, rowErrors);
                String extraBedNote = readText(formatter, row, 11, "Ghi chú giường phụ", excelRowNumber, rowErrors);
                String description = readText(formatter, row, 12, "Mô tả", excelRowNumber, rowErrors);
                String primaryImageUrl = readText(formatter, row, 13, "URL ảnh chính", excelRowNumber, rowErrors);
                String additionalImageUrls = readText(formatter, row, 14, "URL ảnh phụ", excelRowNumber, rowErrors);


                validateRequiredLength(roomTypeName, "Hạng phòng", 100, excelRowNumber, rowErrors);
                validateRequiredLength(variantName, "Tên variant", 150, excelRowNumber, rowErrors);
                validateLength(extraBedNote, "Ghi chú giường phụ", 500, excelRowNumber, rowErrors);
                validateLength(description, "Mô tả", 1000, excelRowNumber, rowErrors);


                RoomType roomType = null;
                if (roomTypeName != null && !roomTypeName.isBlank()) {
                    roomType = roomTypeRepository.findByNameIgnoreCaseAndIsDeletedFalse(roomTypeName.trim()).orElse(null);
                    if (roomType == null) {
                        rowErrors.add("Dòng " + excelRowNumber + ": hạng phòng '" + roomTypeName + "' không tồn tại.");
                    }
                }


                ViewType viewType = parseViewType(viewText, excelRowNumber, rowErrors);
                Boolean allowExtraBed = parseBoolean(allowExtraBedText, excelRowNumber, rowErrors);
                validatePositive(pricePerNight, "Giá mỗi đêm", excelRowNumber, rowErrors);
                validatePositive(roomSize, "Diện tích m2", excelRowNumber, rowErrors);
                validatePositive(capacity, "Sức chứa", excelRowNumber, rowErrors);
                validatePositive(maxAdults, "Người lớn tối đa", excelRowNumber, rowErrors);
                validateNonNegative(maxChildren, "Trẻ em tối đa", excelRowNumber, rowErrors);
                validateNonNegative(maxExtraBeds, "Số giường phụ tối đa", excelRowNumber, rowErrors);


                if (capacity != null && maxAdults != null && maxChildren != null
                        && maxAdults + maxChildren < capacity) {
                    rowErrors.add("Dòng " + excelRowNumber + ": tổng người lớn và trẻ em tối đa phải từ sức chứa trở lên.");
                }
                if (Boolean.FALSE.equals(allowExtraBed) && maxExtraBeds != null && maxExtraBeds != 0) {
                    rowErrors.add("Dòng " + excelRowNumber + ": không cho giường phụ thì số giường phụ tối đa phải bằng 0.");
                }
                if (Boolean.FALSE.equals(allowExtraBed) && extraBedPrice != null && extraBedPrice.signum() != 0) {
                    rowErrors.add("Dòng " + excelRowNumber + ": không cho giường phụ thì giá giường phụ phải để trống hoặc bằng 0.");
                }
                if (Boolean.TRUE.equals(allowExtraBed) && (maxExtraBeds == null || maxExtraBeds <= 0)) {
                    rowErrors.add("Dòng " + excelRowNumber + ": cho giường phụ thì số giường phụ tối đa phải lớn hơn 0.");
                }


                List<String> imageUrls = parseImageUrls(primaryImageUrl, additionalImageUrls, excelRowNumber, rowErrors);
                String configurationKey = roomTypeName == null || viewType == null
                        ? null : roomTypeName.trim().toLowerCase(Locale.ROOT) + "|" + viewType.name();
                if (configurationKey != null && !configurationsInFile.add(configurationKey)) {
                    rowErrors.add("Dòng " + excelRowNumber + ": hạng phòng và hướng nhìn bị trùng trong file Excel.");
                }


                if (!rowErrors.isEmpty()) {
                    errors.addAll(rowErrors);
                    continue;
                }


                validRows.add(new VariantImportRow(configurationKey, roomType, variantName.trim(), viewType, pricePerNight,
                        roomSize, capacity, maxAdults, maxChildren, allowExtraBed, maxExtraBeds,
                        extraBedPrice, normalize(extraBedNote), normalize(description), imageUrls));
            }


            List<VariantServiceImportRow> serviceRows = parseServiceRows(
                    workbook, formatter, configurationsInFile, errors);


            if (!errors.isEmpty()) {
                throw new IllegalArgumentException(buildErrorMessage(errors));
            }
            if (validRows.isEmpty()) {
                throw new IllegalArgumentException("File Excel không có dòng room variant nào.");
            }


            int createdCount = 0;
            int updatedCount = 0;
            int imageCount = 0;
            Map<String, RoomTypeVariant> variantsByKey = new HashMap<>();
            for (VariantImportRow row : validRows) {
                RoomTypeVariant variant = roomTypeVariantRepository
                        .findByRoomType_IdAndViewTypeAndIsDeletedFalse(row.roomType().getId(), row.viewType())
                        .orElse(null);
                if (variant == null) {
                    variant = new RoomTypeVariant();
                    createdCount++;
                } else {
                    updatedCount++;
                }
                variant.setRoomType(row.roomType());
                variant.setVariantName(row.variantName());
                variant.setViewType(row.viewType());
                variant.setPricePerNight(row.pricePerNight());
                variant.setRoomSize(row.roomSize());
                variant.setCapacity(row.capacity());
                variant.setMaxAdults(row.maxAdults());
                variant.setMaxChildren(row.maxChildren());
                variant.setAllowExtraBed(row.allowExtraBed());
                variant.setMaxExtraBeds(row.maxExtraBeds());
                variant.setExtraBedPrice(row.extraBedPrice());
                variant.setExtraBedNote(row.extraBedNote());
                variant.setDescription(row.description());
                variant.setIsDeleted(false);
                RoomTypeVariant savedVariant = roomTypeVariantRepository.saveAndFlush(variant);


                List<Image> oldImages = imageRepository.findByEntityTypeAndEntityIdOrderBySortOrderAsc(
                        ImageEntityType.ROOM_TYPE_VARIANT, savedVariant.getId());
                if (!oldImages.isEmpty()) {
                    imageRepository.deleteAll(oldImages);
                    imageRepository.flush();
                }


                for (int imageIndex = 0; imageIndex < row.imageUrls().size(); imageIndex++) {
                    Image image = new Image();
                    image.setEntityType(ImageEntityType.ROOM_TYPE_VARIANT);
                    image.setEntityId(savedVariant.getId());
                    image.setImageUrl(row.imageUrls().get(imageIndex));
                    image.setIsPrimary(imageIndex == 0);
                    image.setSortOrder(imageIndex + 1);
                    image.setUploadedBy(uploadedBy);
                    image.setViewType(row.viewType());
                    imageRepository.save(image);
                    imageCount++;
                }
                variantsByKey.put(row.configurationKey(), savedVariant);
            }


            int serviceCount = synchronizeVariantServices(variantsByKey, serviceRows);


            return new VariantImportResult(createdCount, updatedCount, imageCount, serviceCount);
        } catch (IOException | RuntimeException e) {
            if (e instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalArgumentException("File không phải workbook .xlsx hợp lệ hoặc đã bị hỏng.");
        }
    }


    private List<VariantServiceImportRow> parseServiceRows(Workbook workbook,
                                                           DataFormatter formatter,
                                                           Set<String> variantKeys,
                                                           List<String> errors) {
        Sheet sheet = workbook.getSheet("Variant Services");
        if (sheet == null) {
            errors.add("Thiếu sheet 'Variant Services'. Hãy sử dụng file Excel mẫu mới nhất.");
            return List.of();
        }


        validateServiceHeader(sheet, formatter, errors);
        if (sheet.getLastRowNum() > 1000) {
            errors.add("Sheet Variant Services chỉ được chứa tối đa 1.000 dòng dữ liệu.");
            return List.of();
        }


        List<VariantServiceImportRow> rows = new ArrayList<>();
        Set<String> servicesInFile = new HashSet<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isEmptyRow(formatter, row, SERVICE_IMPORT_COLUMN_COUNT)) {
                continue;
            }


            int excelRowNumber = rowIndex + 1;
            List<String> rowErrors = new ArrayList<>();
            validateNoExtraColumns(formatter, row, excelRowNumber, SERVICE_IMPORT_COLUMN_COUNT,
                    "Variant Services", rowErrors);


            String roomTypeName = readText(formatter, row, 0, "Hạng phòng", excelRowNumber, rowErrors);
            String viewText = readText(formatter, row, 1, "Hướng nhìn", excelRowNumber, rowErrors);
            String serviceName = readText(formatter, row, 2, "Tên dịch vụ", excelRowNumber, rowErrors);
            Integer quantity = readInteger(row, 3, "Số lượng", excelRowNumber, false, rowErrors);
            String includedType = readText(formatter, row, 4, "Loại áp dụng", excelRowNumber, rowErrors);
            String note = readText(formatter, row, 5, "Ghi chú", excelRowNumber, rowErrors);


            validateRequiredLength(roomTypeName, "Hạng phòng", 100, excelRowNumber, rowErrors);
            validateRequiredLength(serviceName, "Tên dịch vụ", 200, excelRowNumber, rowErrors);
            validateLength(note, "Ghi chú", 300, excelRowNumber, rowErrors);
            validatePositive(quantity, "Số lượng", excelRowNumber, rowErrors);
            ViewType viewType = parseViewType(viewText, excelRowNumber, rowErrors);


            String normalizedIncludedType = includedType == null ? null : includedType.trim().toUpperCase(Locale.ROOT);
            if (!"INCLUDED".equals(normalizedIncludedType)) {
                rowErrors.add("Dòng " + excelRowNumber
                        + " sheet Variant Services: Loại áp dụng hiện chỉ nhận INCLUDED.");
            }


            Service service = null;
            if (serviceName != null && !serviceName.isBlank()) {
                service = serviceRepository
                        .findFirstByNameIgnoreCaseAndIsDeletedFalseAndIsAvailableTrueOrderByIdAsc(serviceName.trim())
                        .orElse(null);
                if (service == null) {
                    rowErrors.add("Dòng " + excelRowNumber + " sheet Variant Services: dịch vụ '"
                            + serviceName + "' không tồn tại hoặc đang ngừng hoạt động.");
                }
            }


            String variantKey = roomTypeName == null || viewType == null
                    ? null : configurationKey(roomTypeName, viewType);
            if (variantKey != null && !variantKeys.contains(variantKey)) {
                rowErrors.add("Dòng " + excelRowNumber
                        + " sheet Variant Services: không tìm thấy variant tương ứng trong sheet Room Variants.");
            }


            String serviceKey = variantKey == null || serviceName == null
                    ? null : variantKey + "|" + serviceName.trim().toLowerCase(Locale.ROOT);
            if (serviceKey != null && !servicesInFile.add(serviceKey)) {
                rowErrors.add("Dòng " + excelRowNumber
                        + " sheet Variant Services: dịch vụ bị trùng cho cùng một variant.");
            }


            if (!rowErrors.isEmpty()) {
                errors.addAll(rowErrors);
                continue;
            }
            rows.add(new VariantServiceImportRow(variantKey, service, quantity,
                    normalizedIncludedType, normalize(note)));
        }
        return rows;
    }


    private int synchronizeVariantServices(Map<String, RoomTypeVariant> variantsByKey,
                                           List<VariantServiceImportRow> importedRows) {
        Map<String, List<VariantServiceImportRow>> rowsByVariant = new HashMap<>();
        for (VariantServiceImportRow row : importedRows) {
            rowsByVariant.computeIfAbsent(row.variantKey(), ignored -> new ArrayList<>()).add(row);
        }


        int activeServiceCount = 0;
        for (Map.Entry<String, RoomTypeVariant> entry : variantsByKey.entrySet()) {
            RoomTypeVariant variant = entry.getValue();
            List<RoomTypeVariantService> existingRows =
                    roomTypeVariantServiceRepository.findByVariant_Id(variant.getId());
            Map<Long, RoomTypeVariantService> existingByServiceId = new HashMap<>();
            for (RoomTypeVariantService existing : existingRows) {
                existingByServiceId.put(existing.getService().getId(), existing);
                existing.setIsDeleted(true);
            }


            for (VariantServiceImportRow imported : rowsByVariant.getOrDefault(entry.getKey(), List.of())) {
                RoomTypeVariantService relation = existingByServiceId.get(imported.service().getId());
                if (relation == null) {
                    relation = new RoomTypeVariantService();
                    relation.setVariant(variant);
                    relation.setService(imported.service());
                }
                relation.setQuantity(imported.quantity());
                relation.setIncludedType(imported.includedType());
                relation.setNote(imported.note());
                relation.setIsDeleted(false);
                roomTypeVariantServiceRepository.save(relation);
                activeServiceCount++;
            }
            roomTypeVariantServiceRepository.saveAll(existingRows);
        }
        return activeServiceCount;
    }


    private void validateServiceHeader(Sheet sheet, DataFormatter formatter, List<String> errors) {
        Row header = sheet.getRow(0);
        if (header == null) {
            errors.add("Sheet Variant Services thiếu dòng tiêu đề.");
            return;
        }
        List<String> actualHeaders = new ArrayList<>();
        for (int index = 0; index < SERVICE_IMPORT_COLUMN_COUNT; index++) {
            String value = readCell(formatter, header, index);
            actualHeaders.add(value == null ? "" : value.toLowerCase(Locale.ROOT));
        }
        if (!actualHeaders.equals(SERVICE_IMPORT_HEADERS)) {
            errors.add("Tiêu đề sheet Variant Services không đúng mẫu.");
        }
        validateNoExtraColumns(formatter, header, 1, SERVICE_IMPORT_COLUMN_COUNT,
                "Variant Services", errors);
    }


    private String configurationKey(String roomTypeName, ViewType viewType) {
        return roomTypeName.trim().toLowerCase(Locale.ROOT) + "|" + viewType.name();
    }


    private void validateImportFile(MultipartFile file) {
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
    }


    private void validateHeader(Sheet sheet, DataFormatter formatter) {
        Row header = sheet.getRow(0);
        if (header == null) {
            throw new IllegalArgumentException("Thiếu dòng tiêu đề của file Excel.");
        }
        List<String> actualHeaders = new ArrayList<>();
        for (int index = 0; index < IMPORT_COLUMN_COUNT; index++) {
            String value = readCell(formatter, header, index);
            actualHeaders.add(value == null ? "" : value.toLowerCase(Locale.ROOT));
        }
        if (!actualHeaders.equals(IMPORT_HEADERS)) {
            throw new IllegalArgumentException("Tiêu đề không đúng mẫu. Hãy tải và sử dụng file mẫu Room Variant.");
        }
    }


    private boolean isEmptyRow(DataFormatter formatter, Row row) {
        return isEmptyRow(formatter, row, IMPORT_COLUMN_COUNT);
    }


    private boolean isEmptyRow(DataFormatter formatter, Row row, int expectedColumnCount) {
        if (row == null) return true;
        for (int index = 0; index < Math.max(row.getLastCellNum(), expectedColumnCount); index++) {
            if (readCell(formatter, row, index) != null) return false;
        }
        return true;
    }


    private void validateNoExtraColumns(DataFormatter formatter, Row row, int rowNumber, List<String> errors) {
        validateNoExtraColumns(formatter, row, rowNumber, IMPORT_COLUMN_COUNT, "Room Variants", errors);
    }


    private void validateNoExtraColumns(DataFormatter formatter, Row row, int rowNumber,
                                        int expectedColumnCount, String sheetName, List<String> errors) {
        if (row == null || row.getLastCellNum() <= expectedColumnCount) return;
        for (int index = expectedColumnCount; index < row.getLastCellNum(); index++) {
            if (readCell(formatter, row, index) != null) {
                errors.add("Dòng " + rowNumber + " sheet " + sheetName
                        + ": chỉ được có đúng " + expectedColumnCount + " cột.");
                return;
            }
        }
    }


    private String readCell(DataFormatter formatter, Row row, int index) {
        if (row == null) return null;
        String value = formatter.formatCellValue(row.getCell(index));
        return value == null || value.isBlank() ? null : value.trim();
    }


    private String readText(DataFormatter formatter, Row row, int index, String field,
                            int rowNumber, List<String> errors) {
        Cell cell = row.getCell(index);
        if (cell != null && cell.getCellType() == CellType.FORMULA) {
            errors.add("Dòng " + rowNumber + ": " + field + " không được dùng công thức.");
            return null;
        }
        if (cell != null && cell.getCellType() != CellType.BLANK && cell.getCellType() != CellType.STRING) {
            errors.add("Dòng " + rowNumber + ": " + field + " phải là văn bản.");
            return null;
        }
        return readCell(formatter, row, index);
    }


    private Integer readInteger(Row row, int index, String field, int rowNumber,
                                boolean optional, List<String> errors) {
        BigDecimal value = readNumber(row, index, field, rowNumber, optional, errors);
        if (value == null) return null;
        try {
            return value.intValueExact();
        } catch (ArithmeticException e) {
            errors.add("Dòng " + rowNumber + ": " + field + " phải là số nguyên hợp lệ.");
            return null;
        }
    }


    private BigDecimal readDecimal(Row row, int index, String field, int rowNumber,
                                   boolean optional, List<String> errors) {
        return readNumber(row, index, field, rowNumber, optional, errors);
    }


    private BigDecimal readNumber(Row row, int index, String field, int rowNumber,
                                  boolean optional, List<String> errors) {
        Cell cell = row.getCell(index);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            if (!optional) errors.add("Dòng " + rowNumber + ": " + field + " là bắt buộc.");
            return null;
        }
        if (cell.getCellType() == CellType.FORMULA) {
            errors.add("Dòng " + rowNumber + ": " + field + " không được dùng công thức.");
            return null;
        }
        String raw;
        if (cell.getCellType() == CellType.NUMERIC) {
            raw = NumberToTextConverter.toText(cell.getNumericCellValue());
        } else if (cell.getCellType() == CellType.STRING) {
            raw = cell.getStringCellValue().trim();
        } else {
            errors.add("Dòng " + rowNumber + ": " + field + " phải là số.");
            return null;
        }
        if (!raw.matches("\\d+")) {
            errors.add("Dòng " + rowNumber + ": " + field + " phải là số nguyên không âm, không có dấu phân cách.");
            return null;
        }
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            errors.add("Dòng " + rowNumber + ": " + field + " không hợp lệ.");
            return null;
        }
    }


    private ViewType parseViewType(String value, int rowNumber, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add("Dòng " + rowNumber + ": Hướng nhìn là bắt buộc.");
            return null;
        }
        try {
            return ViewType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            errors.add("Dòng " + rowNumber + ": Hướng nhìn chỉ nhận SEA, POOL, GARDEN hoặc CITY.");
            return null;
        }
    }


    private Boolean parseBoolean(String value, int rowNumber, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add("Dòng " + rowNumber + ": Cho giường phụ là bắt buộc.");
            return null;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "CÓ", "CO", "YES", "TRUE" -> true;
            case "KHÔNG", "KHONG", "NO", "FALSE" -> false;
            default -> {
                errors.add("Dòng " + rowNumber + ": Cho giường phụ chỉ nhận CÓ hoặc KHÔNG.");
                yield null;
            }
        };
    }


    private List<String> parseImageUrls(String primary, String additional, int rowNumber, List<String> errors) {
        List<String> urls = new ArrayList<>();
        if (primary == null || primary.isBlank()) {
            errors.add("Dòng " + rowNumber + ": URL ảnh chính là bắt buộc.");
        } else {
            urls.add(primary.trim());
        }
        if (additional != null && !additional.isBlank()) {
            for (String value : additional.split("\\|")) {
                if (!value.isBlank()) urls.add(value.trim());
            }
        }
        if (urls.size() > 10) {
            errors.add("Dòng " + rowNumber + ": mỗi variant chỉ được tối đa 10 ảnh.");
        }
        Set<String> uniqueUrls = new HashSet<>();
        for (String url : urls) {
            if (url.length() > 500 || !isHttpUrl(url)) {
                errors.add("Dòng " + rowNumber + ": URL ảnh không hợp lệ hoặc dài quá 500 ký tự: " + url);
            } else if (!uniqueUrls.add(url.toLowerCase(Locale.ROOT))) {
                errors.add("Dòng " + rowNumber + ": URL ảnh bị trùng.");
            }
        }
        return urls;
    }


    private boolean isHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }


    private void validateRequiredLength(String value, String field, int max, int row, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add("Dòng " + row + ": " + field + " là bắt buộc.");
        } else {
            validateLength(value, field, max, row, errors);
        }
    }


    private void validateLength(String value, String field, int max, int row, List<String> errors) {
        if (value != null && value.length() > max) {
            errors.add("Dòng " + row + ": " + field + " không được vượt quá " + max + " ký tự.");
        }
    }


    private void validatePositive(BigDecimal value, String field, int row, List<String> errors) {
        if (value != null && value.signum() <= 0) errors.add("Dòng " + row + ": " + field + " phải lớn hơn 0.");
    }


    private void validatePositive(Integer value, String field, int row, List<String> errors) {
        if (value != null && value <= 0) errors.add("Dòng " + row + ": " + field + " phải lớn hơn 0.");
    }


    private void validateNonNegative(Integer value, String field, int row, List<String> errors) {
        if (value != null && value < 0) errors.add("Dòng " + row + ": " + field + " không được âm.");
    }


    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }


    private String buildErrorMessage(List<String> errors) {
        int count = Math.min(errors.size(), 10);
        String message = String.join(" | ", errors.subList(0, count));
        if (errors.size() > count) message += " | ... và " + (errors.size() - count) + " lỗi khác.";
        return "File Excel có " + errors.size() + " lỗi: " + message;
    }


    public record VariantImportResult(int createdCount, int updatedCount, int imageCount, int serviceCount) {}


    private record VariantImportRow(String configurationKey, RoomType roomType,
                                    String variantName, ViewType viewType,
                                    BigDecimal pricePerNight, Integer roomSize, Integer capacity,
                                    Integer maxAdults, Integer maxChildren, Boolean allowExtraBed,
                                    Integer maxExtraBeds, BigDecimal extraBedPrice, String extraBedNote,
                                    String description, List<String> imageUrls) {}


    private record VariantServiceImportRow(String variantKey, Service service, Integer quantity,
                                           String includedType, String note) {}
}


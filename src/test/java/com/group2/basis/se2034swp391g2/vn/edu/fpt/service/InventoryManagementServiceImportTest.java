package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentMethod;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryCategory;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryItem;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryReceipt;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryTransaction;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.InventoryCategoryRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.InventoryItemRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.InventoryReceiptRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.InventoryTransactionRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomRefreshInventoryMappingRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ServiceInventoryMappingRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ServiceRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryManagementServiceImportTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;
    @Mock
    private InventoryCategoryRepository inventoryCategoryRepository;
    @Mock
    private InventoryReceiptRepository inventoryReceiptRepository;
    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock
    private ServiceInventoryMappingRepository serviceInventoryMappingRepository;
    @Mock
    private RoomRefreshInventoryMappingRepository roomRefreshInventoryMappingRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private RoomTypeRepository roomTypeRepository;
    @Mock
    private CashTransactionService cashTransactionService;
    @Mock
    private FinancialChargeService financialChargeService;

    @InjectMocks
    private InventoryManagementService service;

    @Test
    void rejectsWorkbookWithWrongHeaders() throws Exception {
        MockMultipartFile file = workbook(
                new String[]{"Tên hàng", "Loại", "Đơn vị", "Tồn", "Ngưỡng", "Giá"},
                new Object[][]{{"Khăn", "Đồ dùng tiêu hao", "cái", 10, 2, 5000}});

        assertThatThrownBy(() -> service.importItemsFromExcel(file, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tiêu đề không đúng mẫu");

        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    void reportsMultipleRowErrorsBeforeSavingAnything() throws Exception {
        when(inventoryCategoryRepository.findByNameIgnoreCaseAndIsActiveTrue("Không tồn tại"))
                .thenReturn(Optional.empty());

        MockMultipartFile file = workbook(validHeaders(), new Object[][]{
                {"", "Không tồn tại", "", "-1", "1,000", "12.5"}
        });

        assertThatThrownBy(() -> service.importItemsFromExcel(file, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File Excel có")
                .hasMessageContaining("Tên hàng là bắt buộc")
                .hasMessageContaining("không tồn tại")
                .hasMessageContaining("Tồn đầu kỳ")
                .hasMessageContaining("Giá vốn trước VAT");

        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    void importsValidRowsAndSkipsNamesAlreadyInDatabase() throws Exception {
        InventoryCategory category = InventoryCategory.builder()
                .id(4L)
                .name("Đồ dùng tiêu hao")
                .isActive(true)
                .build();
        when(inventoryCategoryRepository.findByNameIgnoreCaseAndIsActiveTrue("Đồ dùng tiêu hao"))
                .thenReturn(Optional.of(category));
        when(inventoryItemRepository.existsByNameIgnoreCaseAndIsDeletedFalse("Hàng mới"))
                .thenReturn(false);
        when(inventoryItemRepository.existsByNameIgnoreCaseAndIsDeletedFalse("Hàng đã có"))
                .thenReturn(true);
        when(inventoryItemRepository.save(any())).thenAnswer(invocation -> {
            InventoryItem item = invocation.getArgument(0);
            item.setId(27L);
            return item;
        });

        MockMultipartFile file = workbook(validHeaders(), new Object[][]{
                {"Hàng mới", "Đồ dùng tiêu hao", "kg", "1,5", "0,25", "15000"},
                {"Hàng đã có", "Đồ dùng tiêu hao", "cái", 2, 1, 5000}
        });

        User importer = User.builder()
                .id(7L)
                .firstName("Nam")
                .lastName("Hà Đức")
                .build();

        InventoryManagementService.InventoryImportResult result = service.importItemsFromExcel(file, importer);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);

        ArgumentCaptor<InventoryItem> itemCaptor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryItemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getCode()).isEqualTo("IT-0027");
        assertThat(itemCaptor.getValue().getOpeningQuantity()).isEqualByComparingTo(new BigDecimal("1.5"));
        assertThat(itemCaptor.getValue().getMinimumQuantity()).isEqualByComparingTo(new BigDecimal("0.25"));
        assertThat(itemCaptor.getValue().getUnitCost()).isEqualByComparingTo(new BigDecimal("15000"));

        ArgumentCaptor<InventoryTransaction> transactionCaptor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(inventoryTransactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getCreatedBy()).isSameAs(importer);
    }

    @Test
    void autoGeneratesBatchCodeWhenExpiryDateProvided() {
        InventoryItem item = InventoryItem.builder()
                .id(1L)
                .name("Trứng")
                .currentQuantity(BigDecimal.TEN)
                .unitCost(BigDecimal.ONE)
                .isDeleted(false)
                .build();
        LocalDate receiptDate = LocalDate.of(2026, 6, 25);
        when(inventoryItemRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(item));
        when(inventoryReceiptRepository.findTopByBatchCodeStartingWithOrderByBatchCodeDesc("LOT-260625-"))
                .thenReturn(Optional.empty());
        when(inventoryReceiptRepository.existsByBatchCode("LOT-260625-001")).thenReturn(false);
        when(inventoryReceiptRepository.existsByCode(any())).thenReturn(false);
        when(inventoryReceiptRepository.save(any(InventoryReceipt.class))).thenAnswer(invocation -> {
            InventoryReceipt receipt = invocation.getArgument(0);
            receipt.setId(9L);
            return receipt;
        });
        when(financialChargeService.getInventoryVatRate()).thenReturn(BigDecimal.ZERO);
        when(financialChargeService.calculateRateAmount(any(), any())).thenReturn(BigDecimal.ZERO);

        service.createReceipt(
                1L, BigDecimal.ONE, BigDecimal.TEN,
                null, null, receiptDate, null,
                receiptDate.plusDays(10), PaymentMethod.TRANSFER, null);

        ArgumentCaptor<InventoryReceipt> receiptCaptor = ArgumentCaptor.forClass(InventoryReceipt.class);
        verify(inventoryReceiptRepository).save(receiptCaptor.capture());
        assertThat(receiptCaptor.getValue().getBatchCode()).isEqualTo("LOT-260625-001");
    }

    @Test
    void rejectsExpiryDateNotAfterReceiptDate() {
        InventoryItem item = InventoryItem.builder()
                .id(1L)
                .name("Trứng")
                .currentQuantity(BigDecimal.TEN)
                .unitCost(BigDecimal.ONE)
                .isDeleted(false)
                .build();
        LocalDate receiptDate = LocalDate.now();
        when(inventoryItemRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.createReceipt(
                1L, BigDecimal.ONE, BigDecimal.TEN,
                null, null, receiptDate, "LOT-001", receiptDate, PaymentMethod.CASH, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Hạn sử dụng phải sau ngày nhập kho");
    }

    private String[] validHeaders() {
        return new String[]{
                "ten_hang",
                "loai",
                "don_vi",
                "ton_dau_ky",
                "nguong_canh_bao",
                "gia_von_truoc_vat"
        };
    }

    private MockMultipartFile workbook(String[] headers, Object[][] rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inventory Items");
            Row headerRow = sheet.createRow(0);
            for (int columnIndex = 0; columnIndex < headers.length; columnIndex++) {
                headerRow.createCell(columnIndex).setCellValue(headers[columnIndex]);
            }

            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                for (int columnIndex = 0; columnIndex < rows[rowIndex].length; columnIndex++) {
                    Object value = rows[rowIndex][columnIndex];
                    if (value instanceof Number number) {
                        row.createCell(columnIndex).setCellValue(number.doubleValue());
                    } else if (value != null) {
                        row.createCell(columnIndex).setCellValue(value.toString());
                    }
                }
            }

            workbook.write(output);
            return new MockMultipartFile(
                    "file",
                    "inventory.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray());
        }
    }
}

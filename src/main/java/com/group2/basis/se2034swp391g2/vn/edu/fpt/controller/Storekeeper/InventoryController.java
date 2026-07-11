package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Storekeeper;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryItem;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryTransaction;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.InventoryTransactionType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentMethod;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.InventoryManagementService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

@Controller
@RequiredArgsConstructor
public class InventoryController {

    private final ProfileService profileService;
    private final InventoryManagementService inventoryManagementService;

    @GetMapping("/storekeeper/inventory")
    public String inventory(Model model,
                            Authentication authentication,
                            HttpSession session,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Long categoryId,
                            @RequestParam(defaultValue = "ALL") String stockStatus,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size) {
        addHeaderAttributes(model, authentication, session, "Quản lý kho hàng");
        Page<InventoryItem> itemPage = inventoryManagementService.getItems(
                keyword, categoryId, stockStatus, page, size);
        model.addAttribute("items", itemPage.getContent());
        model.addAttribute("categories", inventoryManagementService.getCategories());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("stockStatus", stockStatus);
        model.addAttribute("currentPage", itemPage.getNumber());
        model.addAttribute("totalPages", itemPage.getTotalPages());
        model.addAttribute("totalElements", itemPage.getTotalElements());
        model.addAttribute("pageSize", itemPage.getSize());
        model.addAttribute("hasPrevious", itemPage.hasPrevious());
        model.addAttribute("hasNext", itemPage.hasNext());
        List<Integer> visiblePages = buildVisiblePages(itemPage.getNumber(), itemPage.getTotalPages());
        model.addAttribute("visiblePages", visiblePages);
        model.addAttribute("showLeadingEllipsis", !visiblePages.isEmpty() && visiblePages.getFirst() > 0);
        model.addAttribute("showTrailingEllipsis", !visiblePages.isEmpty() && visiblePages.getLast() < itemPage.getTotalPages() - 1);
        model.addAttribute("lowStockCount", inventoryManagementService.countLowStockItems());
        List<String> lowStockItemCodes = inventoryManagementService.getLowStockItems()
                .stream()
                .map(InventoryItem::getCode)
                .toList();
        model.addAttribute("lowStockItemCodes", lowStockItemCodes);
        model.addAttribute("lowStockItemCodesText", formatLimitedItemCodes(lowStockItemCodes));
        List<String> expiringSoonItemCodes = inventoryManagementService.getExpiringSoonItemCodes();
        model.addAttribute("expiringSoonItemCodes", expiringSoonItemCodes);
        model.addAttribute("expiringSoonItemCodesText", formatLimitedItemCodes(expiringSoonItemCodes));
        model.addAttribute("inventoryVatRate", inventoryManagementService.getInventoryVatRate());
        model.addAttribute("today", LocalDate.now());
        return "storekeeper/InventoryList";
    }

    private List<Integer> buildVisiblePages(int currentPage, int totalPages) {
        if (totalPages <= 0) {
            return List.of();
        }

        if (totalPages <= 5) {
            return IntStream.range(0, totalPages).boxed().toList();
        }

        int start;
        int end;

        if (currentPage <= 2) {
            start = 0;
            end = 3;
        } else if (currentPage >= totalPages - 3) {
            start = totalPages - 4;
            end = totalPages - 1;
        } else {
            start = currentPage - 1;
            end = currentPage + 2;
        }

        return IntStream.rangeClosed(start, end).boxed().toList();
    }

    @GetMapping("/storekeeper/inventory/{id}/edit")
    public String editInventoryItem(@PathVariable Long id,
                                    Model model,
                                    Authentication authentication,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        try {
            addHeaderAttributes(model, authentication, session, "Chỉnh sửa hàng hóa");
            model.addAttribute("item", inventoryManagementService.getItem(id));
            model.addAttribute("mappings", inventoryManagementService.getMappingsForItem(id));
            model.addAttribute("refreshMappings", inventoryManagementService.getRefreshMappingsForItem(id));
            model.addAttribute("services", inventoryManagementService.getAvailableServices());
            model.addAttribute("roomTypes", inventoryManagementService.getRoomTypes());
            model.addAttribute("canDelete", inventoryManagementService.canDeleteItem(id));
            return "storekeeper/InventoryEdit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/storekeeper/inventory";
        }
    }

    @PostMapping("/storekeeper/inventory/{id}/delete")
    public String deleteInventoryItem(@PathVariable Long id,
                                      RedirectAttributes redirectAttributes) {
        try {
            inventoryManagementService.softDeleteItem(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa hàng hóa.");
            return "redirect:/storekeeper/inventory";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/storekeeper/inventory/" + id + "/edit";
        }
    }

    @PostMapping("/storekeeper/inventory/{id}/dispose")
    public String disposeInventoryStock(@PathVariable Long id,
                                        @RequestParam BigDecimal quantity,
                                        @RequestParam(required = false) String reason,
                                        Authentication authentication,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        try {
            inventoryManagementService.disposeStock(id, quantity, reason, resolveCurrentUser(authentication, session));
            redirectAttributes.addFlashAttribute("successMessage", "Đã ghi nhận hủy hàng hóa.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/storekeeper/inventory/" + id + "/edit";
    }

    @PostMapping("/storekeeper/inventory/items/import")
    public String importInventoryItems(@RequestParam("file") MultipartFile file,
                                       Authentication authentication,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        try {
            InventoryManagementService.InventoryImportResult result =
                    inventoryManagementService.importItemsFromExcel(file, resolveCurrentUser(authentication, session));
            redirectAttributes.addFlashAttribute("successMessage",
                    formatImportSuccessMessage(result));
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/storekeeper/inventory";
    }

    @PostMapping("/storekeeper/inventory/receipts/import")
    public String importInventoryReceipts(@RequestParam("file") MultipartFile file,
                                          Authentication authentication,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {
        try {
            InventoryManagementService.ReceiptImportResult result =
                    inventoryManagementService.importReceiptsFromExcel(file, resolveCurrentUser(authentication, session));
            redirectAttributes.addFlashAttribute("successMessage",
                    formatReceiptImportSuccessMessage(result));
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/storekeeper/inventory";
    }

    @PostMapping("/storekeeper/inventory/receipts")
    public String createInventoryReceipt(@RequestParam Long itemId,
                                         @RequestParam BigDecimal quantity,
                                         @RequestParam BigDecimal unitCost,
                                         @RequestParam(required = false) String supplier,
                                         @RequestParam(required = false) String note,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receiptDate,
                                         @RequestParam(required = false) String batchCode,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
                                         @RequestParam PaymentMethod paymentMethod,
                                         Authentication authentication,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        try {
            inventoryManagementService.createReceipt(
                    itemId, quantity, unitCost, supplier, note, receiptDate, batchCode, expiryDate,
                    paymentMethod,
                    resolveCurrentUser(authentication, session));
            redirectAttributes.addFlashAttribute("successMessage", "Đã lập phiếu nhập hàng và ghi nhận giao dịch chi nhập kho.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/storekeeper/inventory";
    }

    private String formatImportSuccessMessage(InventoryManagementService.InventoryImportResult result) {
        return "Đã import " + result.importedCount()
                + " hàng hóa. Bỏ qua " + result.skippedCount()
                + " hàng hóa đã tồn tại.";
    }

    private String formatLimitedItemCodes(List<String> itemCodes) {
        if (itemCodes == null || itemCodes.isEmpty()) {
            return "";
        }
        int visibleCount = Math.min(itemCodes.size(), 5);
        String text = String.join(", ", itemCodes.subList(0, visibleCount));
        return itemCodes.size() > visibleCount ? text + ", ..." : text;
    }

    private String formatReceiptImportSuccessMessage(InventoryManagementService.ReceiptImportResult result) {
        NumberFormat currencyFormatter = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        return "Đã import " + result.importedCount()
                + " lô hàng nhập. Tổng tiền sau VAT: "
                + currencyFormatter.format(result.totalCost()) + " VND.";
    }

    @GetMapping("/storekeeper/inventory/items/options")
    @ResponseBody
    public List<InventoryItemOption> getInventoryItemOptions() {
        return inventoryManagementService.getItemsForSelection()
                .stream()
                .map(item -> new InventoryItemOption(item.getId(), item.getCode() + " - " + item.getName()))
                .toList();
    }

    @GetMapping("/storekeeper/inventory/receipts/next-batch-code")
    @ResponseBody
    public String previewReceiptBatchCode(@RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receiptDate) {
        return inventoryManagementService.previewBatchCode(receiptDate);
    }

    public record InventoryItemOption(Long id, String text) {
    }

    @PostMapping("/storekeeper/inventory/{itemId}/service-mappings/{mappingId}/delete")
    public String deleteServiceMapping(@PathVariable Long itemId,
                                       @PathVariable Long mappingId,
                                       RedirectAttributes redirectAttributes) {
        try {
            inventoryManagementService.deleteServiceMapping(itemId, mappingId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa quy tắc tiêu hao dịch vụ.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/storekeeper/inventory/" + itemId + "/edit";
    }

    @PostMapping("/storekeeper/inventory/{itemId}/room-refresh-mappings/{mappingId}/delete")
    public String deleteRoomRefreshMapping(@PathVariable Long itemId,
                                           @PathVariable Long mappingId,
                                           RedirectAttributes redirectAttributes) {
        try {
            inventoryManagementService.deleteRoomRefreshMapping(itemId, mappingId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa quy tắc lấp đồ phòng.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/storekeeper/inventory/" + itemId + "/edit";
    }

    @PostMapping("/storekeeper/inventory/service-mappings")
    public String linkInventoryToService(@RequestParam Long serviceId,
                                         @RequestParam Long itemId,
                                         @RequestParam BigDecimal quantityPerUse,
                                         RedirectAttributes redirectAttributes) {
        try {
            inventoryManagementService.linkItemToService(serviceId, itemId, quantityPerUse);
            redirectAttributes.addFlashAttribute("successMessage", "Da lien ket hang hoa voi dich vu.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/storekeeper/inventory/" + itemId + "/edit";
    }

    @PostMapping("/storekeeper/inventory/room-refresh-mappings")
    public String linkInventoryToRoomRefresh(@RequestParam Long roomTypeId,
                                             @RequestParam Long itemId,
                                             @RequestParam BigDecimal quantityPerRefresh,
                                             RedirectAttributes redirectAttributes) {
        try {
            inventoryManagementService.linkItemToRoomRefresh(roomTypeId, itemId, quantityPerRefresh);
            redirectAttributes.addFlashAttribute("successMessage", "Đã liên kết hàng hóa với lấp đồ phòng.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/storekeeper/inventory/" + itemId + "/edit";
    }

    @GetMapping("/storekeeper/inventory/{id}")
    public String inventoryDetail(@PathVariable Long id,
                                  Model model,
                                  Authentication authentication,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        try {
            addHeaderAttributes(model, authentication, session, "Chi tiet hang hoa");
            model.addAttribute("item", inventoryManagementService.getItem(id));
            model.addAttribute("totalIn", inventoryManagementService.getItemTotalIn(id));
            model.addAttribute("totalOut", inventoryManagementService.getItemTotalOut(id));
            model.addAttribute("mappings", inventoryManagementService.getMappingsForItem(id));
            model.addAttribute("refreshMappings", inventoryManagementService.getRefreshMappingsForItem(id));
            model.addAttribute("receipts", inventoryManagementService.getReceiptsForItem(id));
            model.addAttribute("services", inventoryManagementService.getAvailableServices());
            model.addAttribute("roomTypes", inventoryManagementService.getRoomTypes());
            return "storekeeper/InventoryDetail";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/storekeeper/inventory";
        }
    }

    @GetMapping("/storekeeper/inventory/history")
    public String inventoryHistory(@RequestParam(required = false) Long itemId,
                                   @RequestParam(required = false) String type,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "15") int size,
                                   Model model,
                                   Authentication authentication,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        try {
            addHeaderAttributes(model, authentication, session, "Lịch sử biến động kho");
            InventoryTransactionType selectedType = parseTransactionType(type);
            Page<InventoryTransaction> transactionPage =
                    inventoryManagementService.getTransactions(itemId, selectedType, dateFrom, dateTo, page, size);
            model.addAttribute("transactions", transactionPage.getContent());
            model.addAttribute("items", inventoryManagementService.getItems());
            model.addAttribute("selectedItemId", itemId);
            model.addAttribute("selectedType", selectedType);
            model.addAttribute("dateFrom", dateFrom);
            model.addAttribute("dateTo", dateTo);
            model.addAttribute("transactionTypes", InventoryTransactionType.values());
            model.addAttribute("currentPage", transactionPage.getNumber());
            model.addAttribute("totalPages", transactionPage.getTotalPages());
            model.addAttribute("totalElements", transactionPage.getTotalElements());
            model.addAttribute("pageSize", transactionPage.getSize());
            model.addAttribute("hasPrevious", transactionPage.hasPrevious());
            model.addAttribute("hasNext", transactionPage.hasNext());
            return "storekeeper/InventoryHistory";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/storekeeper/inventory/history";
        }
    }

    private void addHeaderAttributes(Model model,
                                     Authentication authentication,
                                     HttpSession session,
                                     String pageTitle) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("currentUser", resolveCurrentUser(authentication, session));
    }

    private User resolveCurrentUser(Authentication authentication, HttpSession session) {
        return profileService.resolveCurrentUser(authentication, session);
    }

    private InventoryTransactionType parseTransactionType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return InventoryTransactionType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

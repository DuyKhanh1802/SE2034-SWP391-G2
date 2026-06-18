package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Storekeeper;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryItem;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryTransaction;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.InventoryTransactionType;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

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
                            @RequestParam(required = false) String category,
                            @RequestParam(defaultValue = "ALL") String stockStatus,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size) {
        addHeaderAttributes(model, authentication, session, "Quản lý kho hàng");
        Page<InventoryItem> itemPage = inventoryManagementService.getItems(
                keyword, category, stockStatus, page, size);
        model.addAttribute("items", itemPage.getContent());
        model.addAttribute("allItems", inventoryManagementService.getItems());
        model.addAttribute("categories", inventoryManagementService.getCategories());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("stockStatus", stockStatus);
        model.addAttribute("currentPage", itemPage.getNumber());
        model.addAttribute("totalPages", itemPage.getTotalPages());
        model.addAttribute("totalElements", itemPage.getTotalElements());
        model.addAttribute("pageSize", itemPage.getSize());
        model.addAttribute("hasPrevious", itemPage.hasPrevious());
        model.addAttribute("hasNext", itemPage.hasNext());
        model.addAttribute("lowStockCount", inventoryManagementService.countLowStockItems());
        try {
            model.addAttribute("inventoryVatRate", inventoryManagementService.getInventoryVatRate());
            model.addAttribute("financialChargeConfigured", true);
        } catch (IllegalStateException e) {
            model.addAttribute("inventoryVatRate", BigDecimal.ZERO);
            model.addAttribute("financialChargeConfigured", false);
            model.addAttribute("configurationError", e.getMessage());
        }
        model.addAttribute("today", LocalDate.now());
        return "storekeeper/Inventory";
    }

    @PostMapping("/storekeeper/inventory/items")
    public String createInventoryItem(@RequestParam String name,
                                      @RequestParam(required = false) String category,
                                      @RequestParam String unit,
                                      @RequestParam(required = false) BigDecimal openingQuantity,
                                      @RequestParam(required = false) BigDecimal minimumQuantity,
                                      RedirectAttributes redirectAttributes) {
        try {
            inventoryManagementService.createItem(name, category, unit, openingQuantity, minimumQuantity);
            redirectAttributes.addFlashAttribute("successMessage", "Da them hang hoa.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/storekeeper/inventory";
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

    @PostMapping("/storekeeper/inventory/{id}/edit")
    public String updateInventoryItem(@PathVariable Long id,
                                      @RequestParam String name,
                                      @RequestParam(required = false) String category,
                                      @RequestParam String unit,
                                      @RequestParam BigDecimal minimumQuantity,
                                      RedirectAttributes redirectAttributes) {
        try {
            inventoryManagementService.updateItem(id, name, category, unit, minimumQuantity);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật hàng hóa.");
            return "redirect:/storekeeper/inventory/" + id;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/storekeeper/inventory/" + id + "/edit";
        }
    }

    @PostMapping("/storekeeper/inventory/items/import")
    public String importInventoryItems(@RequestParam("file") MultipartFile file,
                                       RedirectAttributes redirectAttributes) {
        try {
            InventoryManagementService.InventoryImportResult result =
                    inventoryManagementService.importItemsFromExcel(file);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Da import " + result.importedCount() + " hang hoa. Bo qua "
                            + result.skippedCount() + " hang hoa da ton tai.");
        } catch (IllegalArgumentException e) {
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
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receiptDate,
                                         Authentication authentication,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        try {
            inventoryManagementService.createReceipt(
                    itemId, quantity, unitCost, supplier, note, receiptDate,
                    resolveCurrentUser(authentication, session));
            redirectAttributes.addFlashAttribute("successMessage", "Da lap phieu nhap hang va ghi nhan chi quy.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/storekeeper/inventory";
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
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa quy tắc refresh phòng.");
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
            redirectAttributes.addFlashAttribute("successMessage", "Da lien ket hang hoa voi refresh phong.");
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
            model.addAttribute("transactions", inventoryManagementService.getTransactionsForItem(id));
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
                                   @RequestParam(required = false) InventoryTransactionType type,
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
            Page<InventoryTransaction> transactionPage =
                    inventoryManagementService.getTransactions(itemId, type, dateFrom, dateTo, page, size);
            model.addAttribute("transactions", transactionPage.getContent());
            model.addAttribute("items", inventoryManagementService.getItems());
            model.addAttribute("selectedItemId", itemId);
            model.addAttribute("selectedType", type);
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
}

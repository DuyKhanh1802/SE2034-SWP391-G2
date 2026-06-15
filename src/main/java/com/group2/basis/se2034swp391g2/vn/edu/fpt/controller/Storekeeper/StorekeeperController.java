package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Storekeeper;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.InventoryManagementService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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

@Controller
@RequiredArgsConstructor
public class StorekeeperController {

    private final ProfileService profileService;
    private final InventoryManagementService inventoryManagementService;

    @GetMapping("/storekeeper/inventory")
    public String inventory(Model model,
                            Authentication authentication,
                            HttpSession session) {
        addHeaderAttributes(model, authentication, session, "Quan ly kho hang");
        model.addAttribute("items", inventoryManagementService.getItems());
        model.addAttribute("services", inventoryManagementService.getAvailableServices());
        model.addAttribute("roomTypes", inventoryManagementService.getRoomTypes());
        model.addAttribute("recentTransactions", inventoryManagementService.getRecentTransactions());
        model.addAttribute("recentReceipts", inventoryManagementService.getRecentReceipts());
        return "storekeeper/inventory";
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
                                         Authentication authentication,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        try {
            inventoryManagementService.createReceipt(
                    itemId, quantity, unitCost, supplier, note, resolveCurrentUser(authentication, session));
            redirectAttributes.addFlashAttribute("successMessage", "Da lap phieu nhap hang va ghi nhan chi quy.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/storekeeper/inventory";
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
        return "redirect:/storekeeper/inventory/" + itemId;
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
        return "redirect:/storekeeper/inventory/" + itemId;
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
            return "storekeeper/inventory_detail";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/storekeeper/inventory";
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

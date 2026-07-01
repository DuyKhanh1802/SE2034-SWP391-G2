document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("receiptForm");
    const quantity = document.getElementById("receiptQuantity");
    const unitCost = document.getElementById("receiptUnitCost");
    const trackExpiry = document.getElementById("trackExpiry");
    const expiryFields = document.getElementById("expiryFields");
    const batchCode = document.getElementById("receiptBatchCode");
    const expiryDate = document.getElementById("receiptExpiryDate");
    const validationMessage = document.getElementById("receiptValidationMessage");
    const vatAmount = document.getElementById("receiptVatAmount");
    const totalCost = document.getElementById("receiptTotalCost");
    const itemSelect = document.getElementById("receiptItemSelect");
    const receiptModal = document.getElementById("receiptModal");
    if (!form || !quantity || !unitCost) {
        return;
    }

    const vatRate = Number(form.dataset.vatRate || 0);
    const today = form.dataset.today || new Date().toISOString().slice(0, 10);
    const formatter = new Intl.NumberFormat("vi-VN");

    function updateReceiptPreview() {
        const subtotal = Math.max(Number(quantity.value || 0), 0)
            * Math.max(Number(unitCost.value || 0), 0);
        const vat = Math.round(subtotal * vatRate / 100);
        vatAmount.value = formatter.format(vat) + " VND";
        totalCost.value = formatter.format(Math.round(subtotal + vat)) + " VND";
    }

    function showReceiptValidation(message) {
        if (validationMessage) {
            validationMessage.textContent = message || "";
            validationMessage.style.display = message ? "block" : "none";
        }
    }

    async function updateBatchCodePreview() {
        if (!trackExpiry || !trackExpiry.checked || !batchCode) {
            return;
        }
        try {
            const response = await fetch("/storekeeper/inventory/receipts/next-batch-code");
            batchCode.value = response.ok ? await response.text() : "";
        } catch (error) {
            batchCode.value = "";
        }
    }

    async function loadReceiptItems() {
        if (!itemSelect || itemSelect.dataset.loaded === "true") {
            return;
        }

        const placeholder = itemSelect.querySelector("option[value='']");
        if (placeholder) {
            placeholder.textContent = "Đang tải danh sách hàng hóa...";
        }
        itemSelect.disabled = true;

        try {
            const response = await fetch(itemSelect.dataset.optionsUrl || "/storekeeper/inventory/items/options");
            if (!response.ok) {
                throw new Error("Không thể tải lên danh sách hàng hoá");
            }

            const items = await response.json();
            itemSelect.querySelectorAll("option:not([value=''])").forEach(function (option) {
                option.remove();
            });
            items.forEach(function (item) {
                const option = document.createElement("option");
                option.value = item.id;
                option.textContent = item.text;
                itemSelect.appendChild(option);
            });

            itemSelect.dataset.loaded = "true";
            if (placeholder) {
                placeholder.textContent = items.length ? "Chọn hàng hóa" : "Chưa có hàng hóa";
            }
        } catch (error) {
            if (placeholder) {
                placeholder.textContent = "Không tải được danh sách hàng hóa";
            }
        } finally {
            itemSelect.disabled = false;
        }
    }

    function updateExpiryFields() {
        const enabled = trackExpiry && trackExpiry.checked;
        if (expiryFields) {
            expiryFields.style.display = enabled ? "grid" : "none";
        }
        if (expiryDate) {
            expiryDate.required = enabled;
            if (!enabled) {
                expiryDate.value = "";
            }
        }
        if (batchCode && !enabled) {
            batchCode.value = "";
        }
        updateBatchCodePreview();
        validateReceiptDates();
    }

    function validateReceiptDates() {
        let message = "";
        const expiryEnabled = trackExpiry && trackExpiry.checked;

        if (expiryEnabled && expiryDate && !expiryDate.value) {
            message = "Vui lòng chọn hạn sử dụng.";
        } else if (expiryEnabled && expiryDate && expiryDate.value <= today) {
            message = "Hạn sử dụng phải sau ngày nhập kho.";
        }

        [expiryDate, batchCode].forEach(function (input) {
            if (input) {
                input.setCustomValidity("");
            }
        });
        if (message && expiryDate) {
            expiryDate.setCustomValidity(message);
        }
        showReceiptValidation(message);
        return !message;
    }

    quantity.addEventListener("input", updateReceiptPreview);
    unitCost.addEventListener("input", updateReceiptPreview);
    [expiryDate, batchCode].forEach(function (input) {
        if (input) {
            input.addEventListener("input", validateReceiptDates);
        }
    });
    if (trackExpiry) {
        trackExpiry.addEventListener("change", updateExpiryFields);
    }
    if (receiptModal) {
        receiptModal.addEventListener("show.bs.modal", loadReceiptItems);
    }
    form.addEventListener("submit", function (event) {
        if (!validateReceiptDates()) {
            event.preventDefault();
            form.reportValidity();
        }
    });
    updateExpiryFields();
});

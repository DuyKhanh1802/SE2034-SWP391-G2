document.addEventListener("DOMContentLoaded", function () {

    const STORAGE_KEY = "vhotel_selected_services_by_room";

    const roomTabs = document.querySelectorAll(".room-tab");
    const serviceCards = document.querySelectorAll(".service-card");

    const currentRoomTitle = document.getElementById("currentRoomTitle");
    const currentRoomName = document.getElementById("currentRoomName");

    const serviceSelectionForm = document.getElementById("serviceSelectionForm");
    const selectedServiceHiddenInputs = document.getElementById("selectedServiceHiddenInputs");

    const skipServiceBtn = document.getElementById("skipServiceBtn");

    const roomSubtotalText = document.getElementById("roomSubtotalText");
    const serviceSubtotalText = document.getElementById("serviceSubtotalText");
    const serviceChargeText = document.getElementById("serviceChargeText");
    const vatText = document.getElementById("vatText");
    const grandTotalText = document.getElementById("grandTotalText");

    const baseRoomSubtotalInput = document.getElementById("baseRoomSubtotal");
    const promotionDiscountInput = document.getElementById("promotionDiscountAmount");

    const SERVICE_CHARGE_RATE = 0.05;
    const VAT_RATE = 0.08;

    let currentRoomIndex = 1;
    let selectedServicesByRoom = {};
    let isSkipService = false;

    function formatVnd(value) {
        return new Intl.NumberFormat("vi-VN").format(Math.round(value || 0)) + " VND";
    }

    function escapeHtml(value) {
        return String(value || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function getServiceData(serviceId) {
        const card = document.querySelector(`.service-card[data-service-id="${serviceId}"]`);

        if (!card) {
            return null;
        }

        return {
            serviceId: String(card.dataset.serviceId),
            serviceName: card.dataset.serviceName || "",
            price: Number(card.dataset.servicePrice || 0),
            categoryName: card.dataset.serviceCategory || "Service"
        };
    }

    function loadSelectedServices() {
        const raw = sessionStorage.getItem(STORAGE_KEY);

        if (!raw) {
            selectedServicesByRoom = {};
            return;
        }

        try {
            const data = JSON.parse(raw);

            if (data && typeof data === "object") {
                selectedServicesByRoom = data;
            } else {
                selectedServicesByRoom = {};
            }
        } catch (error) {
            selectedServicesByRoom = {};
            sessionStorage.removeItem(STORAGE_KEY);
        }
    }

    function saveSelectedServices() {
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(selectedServicesByRoom));
    }

    function getCurrentRoomServices() {
        const key = String(currentRoomIndex);

        if (!Array.isArray(selectedServicesByRoom[key])) {
            selectedServicesByRoom[key] = [];
        }

        return selectedServicesByRoom[key];
    }

    function findSelectedService(roomIndex, serviceId) {
        const list = selectedServicesByRoom[String(roomIndex)] || [];

        for (let i = 0; i < list.length; i++) {
            if (String(list[i].serviceId) === String(serviceId)) {
                return list[i];
            }
        }

        return null;
    }

    function setServiceQuantity(serviceId, quantity) {
        const serviceData = getServiceData(serviceId);

        if (!serviceData) {
            return;
        }

        const key = String(currentRoomIndex);
        const list = getCurrentRoomServices();

        let existingIndex = -1;

        for (let i = 0; i < list.length; i++) {
            if (String(list[i].serviceId) === String(serviceId)) {
                existingIndex = i;
                break;
            }
        }

        if (quantity <= 0) {
            if (existingIndex >= 0) {
                list.splice(existingIndex, 1);
            }
        } else {
            if (existingIndex >= 0) {
                list[existingIndex].quantity = quantity;
            } else {
                list.push({
                    serviceId: serviceData.serviceId,
                    serviceName: serviceData.serviceName,
                    price: serviceData.price,
                    quantity: quantity
                });
            }
        }

        selectedServicesByRoom[key] = list;

        saveSelectedServices();
        refreshVisibleQuantities();
        renderTripServices();
        updateTotal();
    }

    function refreshVisibleQuantities() {
        serviceCards.forEach(function (card) {
            const serviceId = card.dataset.serviceId;
            const selected = findSelectedService(currentRoomIndex, serviceId);
            const quantity = selected ? Number(selected.quantity || 0) : 0;

            const qtyElement = document.getElementById("serviceQty-" + serviceId);

            if (qtyElement) {
                qtyElement.textContent = quantity;
            }
        });
    }

    function renderTripServices() {
        document.querySelectorAll(".selected-service-list").forEach(function (box) {
            const roomIndex = box.id.replace("selectedServicesRoom-", "");
            const list = selectedServicesByRoom[String(roomIndex)] || [];

            if (list.length === 0) {
                box.innerHTML = "<em>Chưa chọn dịch vụ</em>";
                return;
            }

            let html = "";

            list.forEach(function (item) {
                const lineTotal = Number(item.price || 0) * Number(item.quantity || 0);

                html += `
                    <div class="selected-service-row">
                        <span>${escapeHtml(item.serviceName)} x${item.quantity}</span>
                        <strong>${formatVnd(lineTotal)}</strong>
                    </div>
                `;
            });

            box.innerHTML = html;
        });
    }

    function calculateServiceSubtotal() {
        let total = 0;

        Object.keys(selectedServicesByRoom).forEach(function (roomIndex) {
            const list = selectedServicesByRoom[roomIndex];

            if (!Array.isArray(list)) {
                return;
            }

            list.forEach(function (item) {
                total += Number(item.price || 0) * Number(item.quantity || 0);
            });
        });

        return total;
    }

    function updateTotal() {
        const roomSubtotal = baseRoomSubtotalInput
            ? Number(baseRoomSubtotalInput.value || 0)
            : 0;

        const discount = promotionDiscountInput
            ? Number(promotionDiscountInput.value || 0)
            : 0;

        const serviceSubtotal = calculateServiceSubtotal();

        const subtotalBeforeFees = roomSubtotal + serviceSubtotal;
        const serviceCharge = Math.round(subtotalBeforeFees * SERVICE_CHARGE_RATE);
        const vat = Math.round((subtotalBeforeFees + serviceCharge) * VAT_RATE);

        const totalBeforeDiscount = subtotalBeforeFees + serviceCharge + vat;
        const safeDiscount = Math.min(discount, totalBeforeDiscount);

        const grandTotal = totalBeforeDiscount - safeDiscount;

        if (roomSubtotalText) {
            roomSubtotalText.textContent = formatVnd(roomSubtotal);
        }

        if (serviceSubtotalText) {
            serviceSubtotalText.textContent = formatVnd(serviceSubtotal);
        }

        if (serviceChargeText) {
            serviceChargeText.textContent = formatVnd(serviceCharge);
        }

        if (vatText) {
            vatText.textContent = formatVnd(vat);
        }

        if (grandTotalText) {
            grandTotalText.textContent = formatVnd(grandTotal);
        }
    }

    function buildHiddenInputs() {
        if (!selectedServiceHiddenInputs) {
            return;
        }

        selectedServiceHiddenInputs.innerHTML = "";

        let index = 0;

        Object.keys(selectedServicesByRoom).forEach(function (roomIndex) {
            const list = selectedServicesByRoom[roomIndex];

            if (!Array.isArray(list)) {
                return;
            }

            list.forEach(function (item) {
                const quantity = Number(item.quantity || 0);

                if (quantity <= 0) {
                    return;
                }

                selectedServiceHiddenInputs.insertAdjacentHTML("beforeend", `
                    <input type="hidden" name="roomServices[${index}].roomIndex" value="${roomIndex}">
                    <input type="hidden" name="roomServices[${index}].serviceId" value="${item.serviceId}">
                    <input type="hidden" name="roomServices[${index}].quantity" value="${quantity}">
                `);

                index++;
            });
        });
    }

    roomTabs.forEach(function (tab) {
        tab.addEventListener("click", function () {
            roomTabs.forEach(function (item) {
                item.classList.remove("active");
            });

            tab.classList.add("active");

            currentRoomIndex = Number(tab.dataset.roomIndex || 1);

            if (currentRoomTitle) {
                currentRoomTitle.textContent = "Room " + currentRoomIndex;
            }

            if (currentRoomName) {
                currentRoomName.textContent = tab.dataset.roomName || "";
            }

            refreshVisibleQuantities();
        });
    });

    document.querySelectorAll(".plus-service").forEach(function (button) {
        button.addEventListener("click", function () {
            const serviceId = button.dataset.serviceId;
            const selected = findSelectedService(currentRoomIndex, serviceId);
            const currentQuantity = selected ? Number(selected.quantity || 0) : 0;

            setServiceQuantity(serviceId, currentQuantity + 1);
        });
    });

    document.querySelectorAll(".minus-service").forEach(function (button) {
        button.addEventListener("click", function () {
            const serviceId = button.dataset.serviceId;
            const selected = findSelectedService(currentRoomIndex, serviceId);
            const currentQuantity = selected ? Number(selected.quantity || 0) : 0;

            setServiceQuantity(serviceId, Math.max(0, currentQuantity - 1));
        });
    });

    document.querySelectorAll(".service-filter-bar a, .pagination-box a").forEach(function (link) {
        link.addEventListener("click", function () {
            saveSelectedServices();
        });
    });

    if (skipServiceBtn) {
        skipServiceBtn.addEventListener("click", function () {
            isSkipService = true;
        });
    }

    if (serviceSelectionForm) {
        serviceSelectionForm.addEventListener("submit", function () {
            if (isSkipService) {
                if (selectedServiceHiddenInputs) {
                    selectedServiceHiddenInputs.innerHTML = "";
                }

                sessionStorage.removeItem(STORAGE_KEY);
            } else {
                buildHiddenInputs();

                /*
                    Không xóa STORAGE_KEY ở đây.
                    Giữ lại để nếu khách quay lại từ trang xác nhận,
                    hệ thống vẫn khôi phục các dịch vụ đã chọn.
                */
                saveSelectedServices();
            }
        });
    }

    loadSelectedServices();
    refreshVisibleQuantities();
    renderTripServices();
    updateTotal();
});
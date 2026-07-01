document.addEventListener("DOMContentLoaded", function () {
    const checkInInput = document.querySelector("input[name='checkInDate']");
    const checkOutInput = document.querySelector("input[name='checkOutDate']");
    const roomCheckboxes = document.querySelectorAll(".room-checkbox");

    const nightLabel = document.getElementById("nightLabel");
    const roomCharge = document.getElementById("roomCharge");
    const taxAmount = document.getElementById("taxAmount");
    const totalPrice = document.getElementById("totalPrice");

    const countrySelect = document.getElementById("countryId");
    const phoneCodeInput = document.getElementById("phoneCode");
    const phoneNumberOnlyInput = document.getElementById("phoneNumberOnly");
    const phoneNumberHiddenInput = document.getElementById("phoneNumber");

    const depositPaidCheckbox = document.getElementById("depositPaid");
    const depositFields = document.getElementById("depositFields");
    const depositAmountInput = document.getElementById("depositAmount");
    const depositMethodInput = document.getElementById("depositMethod");
    const depositPreview = document.getElementById("depositPreview");
    const remainingAmount = document.getElementById("remainingAmount");

    const walkinForm = document.querySelector(".walkin-form");
    const extraBedSelects = document.querySelectorAll(".extra-bed-select");

    const selectedCapacity = document.getElementById("selectedCapacity");
    const adultsInput = document.querySelector("input[name='adults']");
    const childrenInput = document.querySelector("input[name='children']");
    const childAgesWrapper = document.getElementById("childAgesWrapper");
    const childAgesContainer = document.getElementById("childAgesContainer");
    const vatRateInput = document.getElementById("vatRate");
    const serviceChargeRateInput = document.getElementById("serviceChargeRate");
    const taxOnServiceChargeInput = document.getElementById("taxOnServiceCharge");

    const additionalServiceCharge = document.getElementById("additionalServiceCharge");
    const serviceChargeAmount = document.getElementById("serviceChargeAmount");
    const enableDiningService = document.getElementById("enableDiningService");
    const enableWellnessService = document.getElementById("enableWellnessService");

    const diningServicePanel = document.getElementById("diningServicePanel");
    const wellnessServicePanel = document.getElementById("wellnessServicePanel");

    const diningServiceRows = document.getElementById("diningServiceRows");
    const wellnessServiceRows = document.getElementById("wellnessServiceRows");

    const diningServiceTemplate = document.getElementById("diningServiceTemplate");
    const wellnessServiceTemplate = document.getElementById("wellnessServiceTemplate");

    const serviceHiddenInputs = document.getElementById("serviceHiddenInputs");

    document.querySelectorAll(".required-field").forEach(function (field) {
        field.addEventListener("input", function () {
            clearFieldError(field);
        });

        field.addEventListener("change", function () {
            clearFieldError(field);
        });
    });

    if (depositMethodInput) {
        depositMethodInput.addEventListener("change", function () {
            clearFieldError(depositMethodInput);
        });
    }
    function formatVnd(amount) {
        return new Intl.NumberFormat("vi-VN").format(amount) + " VND";
    }
    function getSelectedCapacity() {
        let adults = 0;
        let children = 0;

        roomCheckboxes.forEach(function (checkbox) {
            if (checkbox.checked) {
                const card = checkbox.nextElementSibling;
                adults += Number(card.dataset.adults || 0);
                children += Number(card.dataset.children || 0);
            }
        });

        return {
            adults: adults,
            children: children
        };
    }

    function getNights() {
        if (!checkInInput || !checkOutInput) {
            return 0;
        }

        const checkInValue = checkInInput.value;
        const checkOutValue = checkOutInput.value;

        if (!checkInValue || !checkOutValue) {
            return 0;
        }

        const checkInDate = new Date(checkInValue);
        const checkOutDate = new Date(checkOutValue);

        if (checkOutDate <= checkInDate) {
            return 0;
        }

        const oneDay = 1000 * 60 * 60 * 24;
        return Math.round((checkOutDate - checkInDate) / oneDay);
    }

    function getSelectedRoomTotalPerNight() {
        let selectedRoomTotalPerNight = 0;

        roomCheckboxes.forEach(function (checkbox) {
            if (checkbox.checked) {
                const card = checkbox.nextElementSibling;
                const price = Number(card.dataset.price || 0);
                selectedRoomTotalPerNight += price;
            }
        });

        return selectedRoomTotalPerNight;
    }

    function updateSummary() {
        const nights = getNights();
        const selectedRoomTotalPerNight = getSelectedRoomTotalPerNight();
        const capacity = getSelectedCapacity();
        const requiredAdults = Number(adultsInput ? adultsInput.value || 0 : 0);
        const requiredChildren = Number(childrenInput ? childrenInput.value || 0 : 0);

        if (selectedCapacity) {
            selectedCapacity.textContent =
                capacity.adults + "/" + requiredAdults + " người lớn, "
                + capacity.children + "/" + requiredChildren + " trẻ em";

            if (capacity.adults >= requiredAdults && capacity.children >= requiredChildren) {
                selectedCapacity.classList.remove("capacity-warning");
                selectedCapacity.classList.add("capacity-ok");
            } else {
                selectedCapacity.classList.remove("capacity-ok");
                selectedCapacity.classList.add("capacity-warning");
            }
        }

        const roomTotal = getRoomTotalWithWeekendSurcharge(selectedRoomTotalPerNight);

        function getRoomTotalWithWeekendSurcharge(pricePerNight) {
            if (!checkInInput || !checkOutInput || !checkInInput.value || !checkOutInput.value) {
                return 0;
            }

            let total = 0;
            let currentDate = new Date(checkInInput.value);
            const checkOutDate = new Date(checkOutInput.value);

            while (currentDate < checkOutDate) {
                const day = currentDate.getDay(); // 0 = Sunday, 6 = Saturday
                let nightlyPrice = pricePerNight;

                if (day === 0 || day === 6) {
                    nightlyPrice = nightlyPrice * 1.10;
                }

                total += nightlyPrice;
                currentDate.setDate(currentDate.getDate() + 1);
            }

            return Math.round(total);
        }


        let extraBedTotal = 0;

        extraBedSelects.forEach(function (select) {
            const count = Number(select.value || 0);
            const extraPrice = Number(select.dataset.extraPrice || 0);

            extraBedTotal += count * extraPrice * nights;
        });

        const serviceSubtotal = getAdditionalServiceTotal();

        const vatRate = getRate(vatRateInput);
        const serviceChargeRate = getRate(serviceChargeRateInput);

        const baseAmount = roomTotal + extraBedTotal + serviceSubtotal;

        const serviceCharge = Math.round(serviceSubtotal * serviceChargeRate);

        const vatBase = isTaxOnServiceCharge()
            ? baseAmount + serviceCharge
            : baseAmount;

        const tax = Math.round(vatBase * vatRate);

        const total = baseAmount + serviceCharge + tax;
        const suggestedDeposit = Math.round(total * 0.5);

        let deposit = 0;

        if (depositPaidCheckbox && depositPaidCheckbox.checked && depositAmountInput) {
            deposit = suggestedDeposit;
            depositAmountInput.value = suggestedDeposit;
        } else if (depositAmountInput) {
            depositAmountInput.value = "";
        }

        const remaining = total - deposit;

        if (nightLabel) {
            nightLabel.textContent = nights > 0
                ? "Tiền phòng (" + nights + " đêm)"
                : "Tiền phòng";
        }

        if (roomCharge) {
            roomCharge.textContent = formatVnd(roomTotal + extraBedTotal);
        }

        if (additionalServiceCharge) {
            additionalServiceCharge.textContent = formatVnd(serviceSubtotal);
        }

        if (serviceChargeAmount) {
            serviceChargeAmount.textContent = formatVnd(serviceCharge);
        }

        if (taxAmount) {
            taxAmount.textContent = formatVnd(tax);
        }

        if (totalPrice) {
            totalPrice.textContent = formatVnd(total);
        }

        if (depositPreview) {
            depositPreview.textContent = formatVnd(deposit);
        }

        if (remainingAmount) {
            remainingAmount.textContent = formatVnd(remaining);
        }
    }
    function getCurrentChildAgeValues() {
        const currentInputs = document.querySelectorAll(".child-age-input");

        if (currentInputs.length > 0) {
            return Array.from(currentInputs).map(function (input) {
                return input.value;
            });
        }

        const initialInputs = document.querySelectorAll(".initial-child-age");

        return Array.from(initialInputs).map(function (input) {
            return input.value;
        });
    }

    function renderChildAgeInputs() {
        if (!childrenInput || !childAgesWrapper || !childAgesContainer) {
            return;
        }

        const childCount = Number(childrenInput.value || 0);
        const oldValues = getCurrentChildAgeValues();

        childAgesContainer.innerHTML = "";

        if (childCount <= 0) {
            childAgesWrapper.style.display = "none";
            return;
        }

        childAgesWrapper.style.display = "flex";

        for (let i = 0; i < childCount; i++) {
            const item = document.createElement("div");
            item.className = "child-age-item";

            const label = document.createElement("span");
            label.textContent = "Trẻ em " + (i + 1);

            const input = document.createElement("input");
            input.type = "number";
            input.name = "childAges";
            input.min = "0";
            input.max = "12";
            input.placeholder = "Tuổi";
            input.className = "child-age-input";
            input.dataset.message = "Vui lòng nhập tuổi trẻ em từ 0 đến 12.";

            if (oldValues[i] !== undefined) {
                input.value = oldValues[i];
            }

            item.appendChild(label);
            item.appendChild(input);

            childAgesContainer.appendChild(item);
        }
    }
    function validateChildAges() {
        if (!childrenInput) {
            return true;
        }

        const childCount = Number(childrenInput.value || 0);

        if (childCount <= 0) {
            return true;
        }

        let isValid = true;
        const ageInputs = document.querySelectorAll(".child-age-input");

        if (ageInputs.length !== childCount) {
            showSummary("Vui lòng nhập đủ độ tuổi cho từng trẻ em.");
            return false;
        }

        ageInputs.forEach(function (input) {
            const age = Number(input.value);

            if (input.value === "" || Number.isNaN(age) || age < 0 || age > 12) {
                showSummary("Tuổi trẻ em phải nằm trong khoảng từ 0 đến 12.");
                isValid = false;
            }
        });

        return isValid;
    }
    if (childrenInput) {
        childrenInput.addEventListener("input", function () {
            renderChildAgeInputs();
            updateSummary();
        });

        childrenInput.addEventListener("change", function () {
            renderChildAgeInputs();
            updateSummary();
        });
    }

    function toggleDepositFields() {
        if (!depositPaidCheckbox || !depositFields) {
            return;
        }

        if (depositPaidCheckbox.checked) {
            depositFields.style.display = "grid";
        } else {
            depositFields.style.display = "none";

            if (depositAmountInput) {
                depositAmountInput.value = "";
            }

            if (depositMethodInput) {
                depositMethodInput.value = "";
            }
        }

        updateSummary();
    }

    function updatePhoneCode() {
        if (!countrySelect || !phoneCodeInput) {
            return;
        }

        const selectedOption = countrySelect.options[countrySelect.selectedIndex];
        const phoneCode = selectedOption ? selectedOption.getAttribute("data-phone-code") : "";

        phoneCodeInput.value = phoneCode || "";
        updateFullPhoneNumber();
    }

    function updateFullPhoneNumber() {
        if (!phoneCodeInput || !phoneNumberOnlyInput || !phoneNumberHiddenInput) {
            return;
        }

        const code = phoneCodeInput.value.trim();

        let number = phoneNumberOnlyInput.value
            .trim()
            .replace(/\s+/g, "")
            .replace(/-/g, "");

        if (number.startsWith("+")) {
            number = number.replace(code, "");
        }

        if (code.startsWith("+")) {
            number = number.replace(/^0+/, "");
        }

        phoneNumberHiddenInput.value = code + number;
    }

    roomCheckboxes.forEach(function (checkbox) {
        checkbox.addEventListener("change", updateSummary);
    });
    extraBedSelects.forEach(function (select) {
        select.addEventListener("change", updateSummary);
    });

    if (checkInInput) {
        checkInInput.addEventListener("change", updateSummary);
    }

    if (checkOutInput) {
        checkOutInput.addEventListener("change", updateSummary);
    }

    if (countrySelect) {
        countrySelect.addEventListener("change", updatePhoneCode);
    }

    if (phoneNumberOnlyInput) {
        phoneNumberOnlyInput.addEventListener("input", updateFullPhoneNumber);
    }

    if (depositPaidCheckbox) {
        depositPaidCheckbox.addEventListener("change", toggleDepositFields);
    }

    if (depositAmountInput) {
        depositAmountInput.addEventListener("input", updateSummary);
    }

    if (walkinForm) {
        walkinForm.addEventListener("submit", function (event) {
            updateFullPhoneNumber();
            clearAllErrors();

            const submitter = event.submitter;

            let isValid = true;

            // Nút ÁP DỤNG: chỉ cần validate ngày + số khách
            if (submitter && submitter.classList.contains("btn-apply-room")) {
                isValid = validateStayInfo();

                if (!isValid) {
                    event.preventDefault();
                    showSummary("Vui lòng nhập đầy đủ ngày lưu trú và số lượng khách trước khi tìm phòng.");
                }

                return;
            }

            // Nút tạo booking: validate toàn bộ
            if (!validateStayInfo()) {
                isValid = false;
            }
            if (!validateChildAges()) {
                isValid = false;
            }

            if (!validateGuestInfo()) {
                isValid = false;
            }

            if (!validateRoomSelection()) {
                isValid = false;
            }

            if (!validateDeposit()) {
                isValid = false;
            }

            if (!validateAdditionalServices()) {
                isValid = false;
            }

            if (isValid) {
                buildServiceHiddenInputs();
            }

            if (!isValid) {
                event.preventDefault();

                showSummary("Vui lòng kiểm tra lại các thông tin còn thiếu hoặc chưa đúng.");

                const firstError = walkinForm.querySelector(".form-group-line.has-error");

                if (firstError) {
                    firstError.scrollIntoView({
                        behavior: "smooth",
                        block: "center"
                    });
                }
            }
        });
    }

    const errorSummary = document.getElementById("formErrorSummary");

    function showSummary(message) {
        if (!errorSummary) {
            return;
        }

        const text = errorSummary.querySelector("span");
        if (text) {
            text.textContent = message || "Vui lòng kiểm tra lại các thông tin còn thiếu hoặc chưa đúng.";
        }

        errorSummary.classList.add("show");
    }

    function hideSummary() {
        if (errorSummary) {
            errorSummary.classList.remove("show");
        }
    }

    function showFieldError(field, message) {
        const wrapper = field.closest(".form-group-line");

        if (!wrapper) {
            return;
        }

        wrapper.classList.add("has-error");

        const error = wrapper.querySelector(".field-error");
        if (error) {
            error.textContent = message || "Vui lòng nhập thông tin này.";
        }
    }

    function clearFieldError(field) {
        const wrapper = field.closest(".form-group-line");

        if (!wrapper) {
            return;
        }

        wrapper.classList.remove("has-error");

        const error = wrapper.querySelector(".field-error");
        if (error) {
            error.textContent = "";
        }
    }

    function clearAllErrors() {
        document.querySelectorAll(".form-group-line.has-error").forEach(function (wrapper) {
            wrapper.classList.remove("has-error");

            const error = wrapper.querySelector(".field-error");
            if (error) {
                error.textContent = "";
            }
        });

        hideSummary();
    }

    function isBlank(value) {
        return value == null || value.trim() === "";
    }

    function validateField(field) {
        clearFieldError(field);
        if (field.id === "countryId") {
            let countryValue = field.value;

            if (field.tomselect) {
                countryValue = field.tomselect.getValue();
            }

            if (!countryValue) {
                showFieldError(field, field.dataset.message || "Vui lòng chọn quốc gia của khách.");
                return false;
            }

            return true;
        }

        if (isBlank(field.value)) {
            showFieldError(field, field.dataset.message || "Vui lòng nhập thông tin này.");
            return false;
        }

        if (field.type === "email" && !field.checkValidity()) {
            showFieldError(field, "Email không đúng định dạng. Ví dụ: khachhang@example.com");
            return false;
        }

        if (field.type === "number") {
            const value = Number(field.value);
            const min = field.getAttribute("min");

            if (Number.isNaN(value)) {
                showFieldError(field, field.dataset.message || "Giá trị nhập không hợp lệ.");
                return false;
            }

            if (min !== null && value < Number(min)) {
                showFieldError(field, field.dataset.message || "Giá trị nhập không hợp lệ.");
                return false;
            }
        }

        if (field.id === "phoneNumberOnly") {
            const phoneRegex = /^[0-9]{6,15}$/;
            if (!phoneRegex.test(field.value.trim())) {
                showFieldError(field, "Số điện thoại chỉ gồm 6 đến 15 chữ số.");
                return false;
            }
        }

        return true;
    }

    function validateStayInfo() {
        let isValid = true;

        const stayFields = [
            checkInInput,
            checkOutInput,
            adultsInput,
            childrenInput
        ];

        stayFields.forEach(function (field) {
            if (field && !validateField(field)) {
                isValid = false;
            }
        });

        if (checkInInput && checkOutInput && checkInInput.value && checkOutInput.value) {
            const checkIn = new Date(checkInInput.value);
            const checkOut = new Date(checkOutInput.value);

            if (checkOut <= checkIn) {
                showFieldError(checkOutInput, "Ngày trả phòng phải sau ngày nhận phòng.");
                isValid = false;
            }
        }

        return isValid;
    }

    function validateGuestInfo() {
        let isValid = true;

        const requiredFields = walkinForm.querySelectorAll(".required-field");

        requiredFields.forEach(function (field) {

            // Bỏ qua input search do TomSelect tự tạo
            if (field.closest(".ts-wrapper")) {
                return;
            }

            if (!validateField(field)) {
                isValid = false;
            }
        });

        // Validate riêng quốc gia
        if (countrySelect && !validateField(countrySelect)) {
            isValid = false;
        }

        return isValid;
    }

    function validateRoomSelection() {
        const selectedRooms = document.querySelectorAll(".room-checkbox:checked");

        if (selectedRooms.length === 0) {
            showSummary("Vui lòng chọn ít nhất một phòng trước khi tạo đặt phòng.");
            return false;
        }

        return true;
    }

    function validateDeposit() {
        if (!depositPaidCheckbox || !depositPaidCheckbox.checked) {
            return true;
        }

        if (!depositMethodInput || isBlank(depositMethodInput.value)) {
            showFieldError(depositMethodInput, "Vui lòng chọn phương thức thanh toán tiền đặt cọc.");
            return false;
        }

        return true;
    }

    function getRate(input) {
        if (!input || !input.value) {
            return 0;
        }

        return Number(input.value) / 100;
    }

    function isTaxOnServiceCharge() {
        if (!taxOnServiceChargeInput) {
            return false;
        }

        return taxOnServiceChargeInput.value === "true"
            || taxOnServiceChargeInput.value === "1";
    }

    function getAdditionalServiceTotal() {
        let total = 0;

        document.querySelectorAll(".service-row:not(.service-template)").forEach(function (row) {
            const select = row.querySelector(".service-select");
            const qtyInput = row.querySelector(".service-quantity");

            if (!select || !qtyInput || !select.value) {
                return;
            }

            const selectedOption = select.options[select.selectedIndex];
            const unitPrice = Number(selectedOption ? selectedOption.getAttribute("data-price") || 0 : 0);
            const quantity = Number(qtyInput.value || 0);

            if (quantity > 0) {
                total += unitPrice * quantity;
            }
        });

        return total;
    }

    function validateAdditionalServices() {
        let isValid = true;

        document.querySelectorAll(".service-row:not(.service-template)").forEach(function (row) {
            const roomSelect = row.querySelector(".service-room-select");
            const select = row.querySelector(".service-select");
            const qtyInput = row.querySelector(".service-quantity");

            if (!select || !qtyInput) {
                return;
            }

            const quantity = Number(qtyInput.value || 0);

            if (select.value && (!roomSelect || !roomSelect.value)) {
                showSummary("Vui lòng chọn phòng áp dụng cho từng dịch vụ.");
                isValid = false;
            }

            if (select.value && (quantity < 1 || Number.isNaN(quantity))) {
                showSummary("Số lượng dịch vụ phải ít nhất là 1.");
                isValid = false;
            }

            if (!select.value && quantity > 0) {
                showSummary("Vui lòng chọn dịch vụ hoặc xóa dòng dịch vụ trống.");
                isValid = false;
            }
        });

        return isValid;
    }

    function buildServiceHiddenInputs() {
        if (!serviceHiddenInputs) {
            return;
        }

        serviceHiddenInputs.innerHTML = "";

        document.querySelectorAll(".service-row:not(.service-template)").forEach(function (row) {
            const roomSelect = row.querySelector(".service-room-select");
            const serviceSelect = row.querySelector(".service-select");
            const qtyInput = row.querySelector(".service-quantity");

            if (!roomSelect || !serviceSelect || !qtyInput) {
                return;
            }

            if (!roomSelect.value || !serviceSelect.value) {
                return;
            }

            const quantity = Number(qtyInput.value || 0);

            if (quantity < 1) {
                return;
            }

            serviceHiddenInputs.appendChild(createHiddenInput("serviceRoomIds", roomSelect.value));
            serviceHiddenInputs.appendChild(createHiddenInput("serviceIds", serviceSelect.value));
            serviceHiddenInputs.appendChild(createHiddenInput("serviceQuantities", quantity));
        });
    }

    function createHiddenInput(name, value) {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = name;
        input.value = value;
        return input;
    }
    function createServiceRow(template) {
        if (!template) {
            return null;
        }

        const row = template.cloneNode(true);
        addRoomSelectToServiceRow(row);
        row.removeAttribute("id");
        row.classList.remove("service-template");
        row.style.display = "grid";

        const select = row.querySelector(".service-select");
        const qtyInput = row.querySelector(".service-quantity");
        const pricePreview = row.querySelector(".service-price-preview");
        const removeBtn = row.querySelector(".btn-remove-service-row");

        function updateRowPrice() {
            const selectedOption = select.options[select.selectedIndex];
            const unitPrice = Number(selectedOption ? selectedOption.getAttribute("data-price") || 0 : 0);
            const quantity = Number(qtyInput.value || 0);

            if (pricePreview) {
                pricePreview.textContent = formatVnd(unitPrice * quantity);
            }

            updateSummary();
        }

        if (select) {
            select.addEventListener("change", updateRowPrice);
        }

        if (qtyInput) {
            qtyInput.addEventListener("input", updateRowPrice);
            qtyInput.addEventListener("change", updateRowPrice);
        }

        if (removeBtn) {
            removeBtn.addEventListener("click", function () {
                row.remove();
                updateSummary();
            });
        }

        updateRowPrice();

        return row;
    }
    function addRoomSelectToServiceRow(row) {
        const firstServiceField = row.querySelector(".service-field");

        if (!firstServiceField) {
            return;
        }

        const roomField = document.createElement("div");
        roomField.className = "service-field";

        const label = document.createElement("label");
        label.textContent = "Phòng áp dụng";

        const select = document.createElement("select");
        select.className = "service-room-select";

        const defaultOption = document.createElement("option");
        defaultOption.value = "";
        defaultOption.textContent = "Chọn phòng";
        select.appendChild(defaultOption);

        document.querySelectorAll(".room-checkbox:checked").forEach(function (checkbox) {
            const roomCard = checkbox.nextElementSibling;
            const roomNumber = roomCard
                ? roomCard.querySelector(".room-number").textContent.trim()
                : checkbox.value;

            const option = document.createElement("option");
            option.value = checkbox.value;
            option.textContent = "Phòng " + roomNumber;

            select.appendChild(option);
        });

        roomField.appendChild(label);
        roomField.appendChild(select);

        firstServiceField.parentNode.insertBefore(roomField, firstServiceField);
    }

    function addServiceRow(type) {
        if (type === "dining") {
            const row = createServiceRow(diningServiceTemplate);
            if (row && diningServiceRows) {
                diningServiceRows.appendChild(row);
            }
        }

        if (type === "wellness") {
            const row = createServiceRow(wellnessServiceTemplate);
            if (row && wellnessServiceRows) {
                wellnessServiceRows.appendChild(row);
            }
        }

        updateSummary();
    }

    function toggleServicePanel(checkbox, panel, rowsContainer, type) {
        if (!checkbox || !panel || !rowsContainer) {
            return;
        }

        checkbox.addEventListener("change", function () {
            if (checkbox.checked) {
                panel.style.display = "block";

                if (rowsContainer.children.length === 0) {
                    addServiceRow(type);
                }
            } else {
                panel.style.display = "none";
                rowsContainer.innerHTML = "";
            }

            updateSummary();
        });
    }

    toggleServicePanel(enableDiningService, diningServicePanel, diningServiceRows, "dining");
    toggleServicePanel(enableWellnessService, wellnessServicePanel, wellnessServiceRows, "wellness");

    document.querySelectorAll(".btn-add-service-row").forEach(function (button) {
        button.addEventListener("click", function () {
            const target = button.getAttribute("data-target");
            addServiceRow(target);
        });
    });
    if (document.getElementById("countryId") && window.TomSelect) {
        if (countrySelect.tomselect) {
            countrySelect.tomselect.destroy();
        }

        new TomSelect("#countryId", {
            create: false,
            maxOptions: 300,
            searchField: ["text"],
            placeholder: "Tìm quốc gia...",
            onChange: function (value) {
                countrySelect.value = value;
                updatePhoneCode();
                clearFieldError(countrySelect);
            }
        });
    }
    updatePhoneCode();
    toggleDepositFields();
    updateSummary();
    renderChildAgeInputs();

    const moreButtons = document.querySelectorAll(".room-more-btn");

    moreButtons.forEach(function (button) {
        button.addEventListener("click", function (event) {
            event.preventDefault();
            event.stopPropagation();

            const moreBlock = button.closest(".room-more");
            const serviceBox = moreBlock.querySelector(".included-services");

            if (!serviceBox) {
                return;
            }

            serviceBox.classList.toggle("show");

            if (serviceBox.classList.contains("show")) {
                button.textContent = "Thu gọn";
            } else {
                button.textContent = "Xem thêm";
            }
        });
    });
});
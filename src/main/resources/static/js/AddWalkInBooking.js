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

        let extraBedTotal = 0;

        extraBedSelects.forEach(function (select) {
            const count = Number(select.value || 0);
            const extraPrice = Number(select.dataset.extraPrice || 0);

            extraBedTotal += count * extraPrice * nights;
        });

        const roomTotal = selectedRoomTotalPerNight * nights;
        const beforeTaxTotal = roomTotal + extraBedTotal;
        const tax = Math.round(beforeTaxTotal * 0.12);
        const total = beforeTaxTotal + tax;

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

            if (!validateGuestInfo()) {
                isValid = false;
            }

            if (!validateRoomSelection()) {
                isValid = false;
            }

            if (!validateDeposit()) {
                isValid = false;
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
            if (!validateField(field)) {
                isValid = false;
            }
        });

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
    updatePhoneCode();
    toggleDepositFields();
    updateSummary();

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
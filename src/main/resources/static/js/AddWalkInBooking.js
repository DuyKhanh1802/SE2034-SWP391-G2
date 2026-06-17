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
        walkinForm.addEventListener("submit", function () {
            updateFullPhoneNumber();
        });
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
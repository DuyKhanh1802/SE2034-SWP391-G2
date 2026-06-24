document.addEventListener("DOMContentLoaded", function () {
    if (typeof flatpickr === "undefined") {
        return;
    }

    function addYearDropdown(instance, startYear, endYear) {
        const calendar = instance.calendarContainer;
        const currentMonth = calendar.querySelector(".flatpickr-current-month");
        const yearWrapper = calendar.querySelector(".numInputWrapper");

        if (!currentMonth || !yearWrapper) {
            return;
        }

        // Tránh tạo trùng dropdown
        let oldSelect = calendar.querySelector(".flatpickr-year-select");
        if (oldSelect) {
            oldSelect.value = instance.currentYear;
            return;
        }

        // Ẩn ô năm mặc định của flatpickr
        yearWrapper.style.display = "none";

        const yearSelect = document.createElement("select");
        yearSelect.className = "flatpickr-year-select";

        for (let year = startYear; year <= endYear; year++) {
            const option = document.createElement("option");
            option.value = year;
            option.textContent = year;
            yearSelect.appendChild(option);
        }

        yearSelect.value = instance.currentYear;

        yearSelect.addEventListener("change", function () {
            instance.changeYear(Number(this.value));
        });

        currentMonth.appendChild(yearSelect);
    }

    const currentYear = new Date().getFullYear();

    // List Booking filter date picker
    flatpickr(".list-date-picker", {
        dateFormat: "d/m/Y",
        locale: "vn",
        allowInput: true,
        disableMobile: true,
        monthSelectorType: "dropdown",
        onReady: function (selectedDates, dateStr, instance) {
            addYearDropdown(instance, currentYear - 5, currentYear + 5);
        },
        onOpen: function (selectedDates, dateStr, instance) {
            addYearDropdown(instance, currentYear - 5, currentYear + 5);
        },
        onMonthChange: function (selectedDates, dateStr, instance) {
            addYearDropdown(instance, currentYear - 5, currentYear + 5);
        },
        onYearChange: function (selectedDates, dateStr, instance) {
            addYearDropdown(instance, currentYear - 5, currentYear + 5);
        }
    });
    // Ngày sinh: chọn từ 1900 đến năm hiện tại
    flatpickr(".birth-date", {
        dateFormat: "Y-m-d",
        altInput: true,
        altFormat: "d/m/Y",
        locale: "vn",
        maxDate: "today",
        allowInput: true,
        disableMobile: true,
        monthSelectorType: "dropdown",
        onReady: function (selectedDates, dateStr, instance) {
            addYearDropdown(instance, 1900, currentYear);
        },
        onOpen: function (selectedDates, dateStr, instance) {
            addYearDropdown(instance, 1900, currentYear);
        },
        onMonthChange: function (selectedDates, dateStr, instance) {
            addYearDropdown(instance, 1900, currentYear);
        },
        onYearChange: function (selectedDates, dateStr, instance) {
            addYearDropdown(instance, 1900, currentYear);
        }
    });

    const checkInInput = document.querySelector(".check-in-date");
    const checkOutInput = document.querySelector(".check-out-date");

    if (!checkInInput || !checkOutInput) {
        return;
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // Check-in/check-out: cho chọn từ năm hiện tại đến 5 năm sau
    const minStayYear = currentYear;
    const maxStayYear = currentYear + 5;

    const checkOutPicker = flatpickr(checkOutInput, {
        dateFormat: "Y-m-d",
        altInput: true,
        altFormat: "d/m/Y",
        locale: "vn",
        minDate: new Date(today.getTime() + 24 * 60 * 60 * 1000),
        allowInput: false,
        disableMobile: true,
        monthSelectorType: "dropdown",
        onReady: function (selectedDates, dateStr, instance) {
            addYearDropdown(instance, minStayYear, maxStayYear);
        },
        onOpen: function (selectedDates, dateStr, instance) {
            addYearDropdown(instance, minStayYear, maxStayYear);
        },
        onMonthChange: function (selectedDates, dateStr, instance) {
            addYearDropdown(instance, minStayYear, maxStayYear);
        },
        onYearChange: function (selectedDates, dateStr, instance) {
            addYearDropdown(instance, minStayYear, maxStayYear);
        }
    });

    flatpickr(checkInInput, {
        dateFormat: "Y-m-d",
        altInput: true,
        altFormat: "d/m/Y",
        locale: "vn",
        minDate: today,
        allowInput: false,
        disableMobile: true,
        monthSelectorType: "dropdown",
        onReady: function (selectedDates, dateStr, instance) {
            addYearDropdown(instance, minStayYear, maxStayYear);
        },
        onOpen: function (selectedDates, dateStr, instance) {
            addYearDropdown(instance, minStayYear, maxStayYear);
        },
        onMonthChange: function (selectedDates, dateStr, instance) {
            addYearDropdown(instance, minStayYear, maxStayYear);
        },
        onYearChange: function (selectedDates, dateStr, instance) {
            addYearDropdown(instance, minStayYear, maxStayYear);
        },
        onChange: function (selectedDates) {
            if (selectedDates.length === 0) {
                return;
            }

            const checkInDate = selectedDates[0];
            const minCheckOutDate = new Date(checkInDate);
            minCheckOutDate.setDate(minCheckOutDate.getDate() + 1);

            checkOutPicker.set("minDate", minCheckOutDate);

            const currentCheckOut = checkOutPicker.selectedDates[0];

            if (!currentCheckOut || currentCheckOut <= checkInDate) {
                checkOutPicker.setDate(minCheckOutDate, true);
            }
        }
    });
});
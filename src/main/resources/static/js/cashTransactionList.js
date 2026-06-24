document.addEventListener("DOMContentLoaded", function () {
    const dateInputs = document.querySelectorAll(".transaction-date-filter");

    if (!window.flatpickr || dateInputs.length === 0) {
        return;
    }

    function addYearDropdown(instance, startYear, endYear) {
        const calendar = instance.calendarContainer;
        const currentMonth = calendar.querySelector(".flatpickr-current-month");
        const yearWrapper = calendar.querySelector(".numInputWrapper");

        if (!currentMonth || !yearWrapper) {
            return;
        }

        let oldSelect = calendar.querySelector(".flatpickr-year-select");
        if (oldSelect) {
            oldSelect.value = instance.currentYear;
            return;
        }

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

    // Dùng lịch chọn ngày cho filter, nhưng vẫn hiển thị theo kiểu Việt Nam.
    dateInputs.forEach(function (input) {
        flatpickr(input, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            allowInput: false,
            disableMobile: true,
            monthSelectorType: "dropdown",
            locale: "vn",
            onReady: function (selectedDates, dateStr, instance) {
                addYearDropdown(instance, currentYear - 10, currentYear + 5);
            },
            onOpen: function (selectedDates, dateStr, instance) {
                addYearDropdown(instance, currentYear - 10, currentYear + 5);
            },
            onMonthChange: function (selectedDates, dateStr, instance) {
                addYearDropdown(instance, currentYear - 10, currentYear + 5);
            },
            onYearChange: function (selectedDates, dateStr, instance) {
                addYearDropdown(instance, currentYear - 10, currentYear + 5);
            }
        });
    });
});

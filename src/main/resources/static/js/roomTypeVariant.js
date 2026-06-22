document.addEventListener("DOMContentLoaded", function () {

    const roomSearchForm = document.getElementById("roomSearchForm");

    const checkInInput = document.getElementById("checkInDate");
    const checkOutInput = document.getElementById("checkOutDate");

    const dateSummary = document.getElementById("dateSummary");
    const nightSummary = document.getElementById("nightSummary") || document.getElementById("nightSummaryEmpty");
    const dateError = document.getElementById("dateError");

    const TRIP_STORAGE_KEY = "vhotel_selected_rooms_trip";

    const SERVICE_CHARGE_RATE = 0.05; // 5%
    const VAT_RATE = 0.08;            // 8%

    const tenThang = [
        "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
        "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
    ];

    const tenThu = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"];

    const homNay = new Date();
    homNay.setHours(0, 0, 0, 0);

    let thangDangXem = checkInInput && checkInInput.value
        ? new Date(checkInInput.value + "T00:00:00")
        : new Date(homNay.getFullYear(), homNay.getMonth(), 1);

    thangDangXem = new Date(thangDangXem.getFullYear(), thangDangXem.getMonth(), 1);

    let ngayNhanPhong = checkInInput && checkInInput.value ? checkInInput.value : "";
    let ngayTraPhong = checkOutInput && checkOutInput.value ? checkOutInput.value : "";

    let phongDangChon = 1;
    let danhSachPhongDaChon = [];
    let appliedPromoCode = "";

    const appliedPromoCodeInput = document.getElementById("appliedPromoCode");

    if (appliedPromoCodeInput && appliedPromoCodeInput.value) {
        appliedPromoCode = appliedPromoCodeInput.value.trim().toUpperCase();
    }

    function ganPromoCodeVaoForm() {
        const promoInput = document.getElementById("promoCode");
        const promoCodeHidden = document.getElementById("promoCodeHidden");

        const promo = promoInput ? promoInput.value.trim().toUpperCase() : "";

        if (promoInput) {
            promoInput.value = promo;
        }

        if (promoCodeHidden) {
            promoCodeHidden.value = promo;
        }

        appliedPromoCode = promo;
    }

    function dongTatCaBangChon() {
        document.querySelectorAll(".booking-panel").forEach(panel => {
            panel.classList.remove("active");
        });

        document.querySelectorAll(".search-item").forEach(item => {
            item.classList.remove("active");
        });
    }

    document.querySelectorAll(".search-item[data-panel-target]").forEach(item => {
        item.addEventListener("click", function (event) {
            event.stopPropagation();

            const panelId = item.getAttribute("data-panel-target");
            const panel = document.getElementById(panelId);

            if (!panel) return;

            const dangMo = panel.classList.contains("active");

            dongTatCaBangChon();

            if (!dangMo) {
                item.classList.add("active");
                panel.classList.add("active");

                if (panelId === "datePanel") {
                    hienThiHaiThang();
                }
            }
        });
    });

    document.querySelectorAll(".booking-panel").forEach(panel => {
        panel.addEventListener("click", function (event) {
            event.stopPropagation();
        });
    });

    document.addEventListener("click", dongTatCaBangChon);

    function dinhDangNgay(value) {
        if (!value) return "";

        const date = new Date(value + "T00:00:00");

        return String(date.getDate()).padStart(2, "0")
            + "/"
            + String(date.getMonth() + 1).padStart(2, "0");
    }

    function soDem() {
        if (!ngayNhanPhong || !ngayTraPhong) return 0;

        const inDate = new Date(ngayNhanPhong + "T00:00:00");
        const outDate = new Date(ngayTraPhong + "T00:00:00");

        return Math.round((outDate - inDate) / 86400000);
    }

    function capNhatTomTatNgay() {
        if (!dateSummary) return;

        if (ngayNhanPhong && ngayTraPhong) {
            dateSummary.textContent = dinhDangNgay(ngayNhanPhong) + " - " + dinhDangNgay(ngayTraPhong);

            if (nightSummary) {
                nightSummary.textContent = soDem() + " đêm";
            }
        } else {
            dateSummary.textContent = "Chọn ngày";

            if (nightSummary) {
                nightSummary.textContent = "0 đêm";
            }
        }

        capNhatYourTrip();
    }

    function taoNgayIso(nam, thang, ngay) {
        return nam
            + "-"
            + String(thang + 1).padStart(2, "0")
            + "-"
            + String(ngay).padStart(2, "0");
    }

    function trongKhoangChon(iso) {
        if (!ngayNhanPhong || !ngayTraPhong) return false;

        return iso > ngayNhanPhong && iso < ngayTraPhong;
    }

    function taoLich(thangDate, containerId) {
        const container = document.getElementById(containerId);

        if (!container) return;

        const nam = thangDate.getFullYear();
        const thang = thangDate.getMonth();

        const ngayDauThang = new Date(nam, thang, 1).getDay();
        const soNgay = new Date(nam, thang + 1, 0).getDate();

        let html = "";

        html += `<h3>${tenThang[thang]} ${nam}</h3>`;

        html += `<div class="week-row">`;
        tenThu.forEach(thu => {
            html += `<span>${thu}</span>`;
        });
        html += `</div>`;

        html += `<div class="day-grid">`;

        for (let i = 0; i < ngayDauThang; i++) {
            html += `<span class="day-empty"></span>`;
        }

        for (let ngay = 1; ngay <= soNgay; ngay++) {
            const iso = taoNgayIso(nam, thang, ngay);
            const dateObj = new Date(iso + "T00:00:00");

            const daQua = dateObj < homNay;
            const laNgayNhan = iso === ngayNhanPhong;
            const laNgayTra = iso === ngayTraPhong;
            const namTrongKhoang = trongKhoangChon(iso);

            let className = "calendar-day";

            if (daQua) className += " disabled";
            if (laNgayNhan || laNgayTra) className += " selected";
            if (namTrongKhoang) className += " in-range";

            html += `
                <button type="button"
                        class="${className}"
                        data-date="${iso}"
                        ${daQua ? "disabled" : ""}>
                    ${ngay}
                </button>
            `;
        }

        html += `</div>`;

        container.innerHTML = html;
    }

    function hienThiHaiThang() {
        const thangSau = new Date(
            thangDangXem.getFullYear(),
            thangDangXem.getMonth() + 1,
            1
        );

        taoLich(thangDangXem, "calendarMonthLeft");
        taoLich(thangSau, "calendarMonthRight");
    }

    const prevMonthBtn = document.getElementById("prevMonthBtn");

    if (prevMonthBtn) {
        prevMonthBtn.addEventListener("click", function () {
            thangDangXem = new Date(
                thangDangXem.getFullYear(),
                thangDangXem.getMonth() - 1,
                1
            );

            hienThiHaiThang();
        });
    }

    const nextMonthBtn = document.getElementById("nextMonthBtn");

    if (nextMonthBtn) {
        nextMonthBtn.addEventListener("click", function () {
            thangDangXem = new Date(
                thangDangXem.getFullYear(),
                thangDangXem.getMonth() + 1,
                1
            );

            hienThiHaiThang();
        });
    }

    const datePanel = document.getElementById("datePanel");

    if (datePanel) {
        datePanel.addEventListener("click", function (event) {
            const target = event.target;

            if (!target.classList.contains("calendar-day") || target.classList.contains("disabled")) {
                return;
            }

            const ngayDuocChon = target.getAttribute("data-date");

            if (!ngayNhanPhong || ngayTraPhong || ngayDuocChon <= ngayNhanPhong) {
                ngayNhanPhong = ngayDuocChon;
                ngayTraPhong = "";
            } else {
                ngayTraPhong = ngayDuocChon;
            }

            if (checkInInput) {
                checkInInput.value = ngayNhanPhong;
            }

            if (checkOutInput) {
                checkOutInput.value = ngayTraPhong;
            }

            if (dateError) {
                dateError.textContent = "";
            }

            capNhatTomTatNgay();
            hienThiHaiThang();
        });
    }

    const clearDateBtn = document.getElementById("clearDateBtn");

    if (clearDateBtn) {
        clearDateBtn.addEventListener("click", function () {
            ngayNhanPhong = "";
            ngayTraPhong = "";

            if (checkInInput) {
                checkInInput.value = "";
            }

            if (checkOutInput) {
                checkOutInput.value = "";
            }

            if (dateError) {
                dateError.textContent = "";
            }

            capNhatTomTatNgay();
            hienThiHaiThang();
        });
    }

    const applyDateBtn = document.getElementById("applyDateBtn");

    if (applyDateBtn) {
        applyDateBtn.addEventListener("click", function () {
            const dem = soDem();

            if (!ngayNhanPhong || !ngayTraPhong || dem <= 0) {
                if (dateError) {
                    dateError.textContent = "Vui lòng chọn ngày nhận phòng và ngày trả phòng hợp lệ.";
                }

                return;
            }

            if (checkInInput) {
                checkInInput.value = ngayNhanPhong;
            }

            if (checkOutInput) {
                checkOutInput.value = ngayTraPhong;
            }

            ganPromoCodeVaoForm();
            luuYourTripVaoSession();

            if (roomSearchForm) {
                roomSearchForm.submit();
            }
        });
    }

    function capNhatThanhChonPhong() {
        const roomCountInput = document.getElementById("roomCountInput");

        const currentRoomText = document.getElementById("currentRoomText");
        const currentGuestText = document.getElementById("currentGuestText");

        const roomBlocks = document.querySelectorAll(".guest-room-block");
        const totalRooms = roomBlocks.length || (roomCountInput ? parseInt(roomCountInput.value) : 1);

        if (phongDangChon > totalRooms) {
            phongDangChon = totalRooms;
        }

        if (phongDangChon < 1) {
            phongDangChon = 1;
        }

        if (currentRoomText) {
            currentRoomText.textContent = phongDangChon + " TRÊN " + totalRooms;
        }

        const currentBlock = roomBlocks[phongDangChon - 1];

        if (currentGuestText && currentBlock) {
            const adultCountElement = currentBlock.querySelector(".adult-count");
            const childCountElement = currentBlock.querySelector(".child-count");

            const adults = adultCountElement ? parseInt(adultCountElement.textContent) : 1;
            const children = childCountElement ? parseInt(childCountElement.textContent) : 0;

            let text = adults + " người lớn";

            if (children > 0) {
                text += ", " + children + " trẻ em";
            }

            currentGuestText.textContent = text;
        }
    }

    function capNhatTomTatKhach() {
        const blocks = document.querySelectorAll(".guest-room-block");

        let totalAdults = 0;
        let totalChildren = 0;
        let totalRooms = blocks.length;
        let roomGuestsData = [];

        blocks.forEach(block => {
            const adultCount = block.querySelector(".adult-count");
            const childCount = block.querySelector(".child-count");

            const adults = adultCount ? parseInt(adultCount.textContent) : 1;
            const children = childCount ? parseInt(childCount.textContent) : 0;

            totalAdults += adults;
            totalChildren += children;

            roomGuestsData.push(adults + "-" + children);
        });

        const adultsInput = document.getElementById("adultsInput");
        const childrenInput = document.getElementById("childrenInput");
        const roomCountInput = document.getElementById("roomCountInput");
        const roomGuestsInput = document.getElementById("roomGuestsInput");

        const guestSummary = document.getElementById("guestSummary");

        if (adultsInput) {
            adultsInput.value = totalAdults;
        }

        if (childrenInput) {
            childrenInput.value = totalChildren;
        }

        if (roomCountInput) {
            roomCountInput.value = totalRooms;
        }

        if (roomGuestsInput) {
            roomGuestsInput.value = roomGuestsData.join("|");
        }

        const adultText = totalAdults + " người lớn";
        const childText = totalChildren > 0 ? ", " + totalChildren + " trẻ em" : "";
        const roomText = ", " + totalRooms + " phòng";

        if (guestSummary) {
            guestSummary.textContent = adultText + childText + roomText;
        }

        if (danhSachPhongDaChon.length > totalRooms) {
            danhSachPhongDaChon = danhSachPhongDaChon.slice(0, totalRooms);
        }

        capNhatThanhChonPhong();
        capNhatYourTrip();
    }

    const addRoomBtn = document.getElementById("addRoomBtn");

    let roomCounter = document.querySelectorAll(".guest-room-block").length || 1;

    if (addRoomBtn) {
        addRoomBtn.addEventListener("click", function () {
            roomCounter++;

            const container = document.getElementById("roomGuestContainer");

            if (!container) return;

            const block = document.createElement("div");
            block.className = "guest-room-block";
            block.setAttribute("data-room-index", roomCounter);

            block.innerHTML = `
                <div class="room-block-header">
                    <h6>PHÒNG ${roomCounter}</h6>
                    <button type="button" class="remove-room-btn">Xóa phòng</button>
                </div>

                <div class="guest-row">
                    <strong>Người lớn <span>(từ 13 tuổi trở lên)</span></strong>

                    <div class="counter">
                        <button type="button" class="counter-btn minus-adult">−</button>
                        <span class="adult-count">1</span>
                        <button type="button" class="counter-btn plus-adult">+</button>
                    </div>
                </div>

                <div class="guest-row">
                    <strong>Trẻ em <span>(0–12 tuổi)</span></strong>

                    <div class="counter">
                        <button type="button" class="counter-btn minus-child">−</button>
                        <span class="child-count">0</span>
                        <button type="button" class="counter-btn plus-child">+</button>
                    </div>
                </div>

                <div class="child-age-box"></div>
            `;

            container.appendChild(block);
            capNhatTomTatKhach();
        });
    }

    const guestPanel = document.getElementById("guestPanel");

    if (guestPanel) {
        guestPanel.addEventListener("click", function (event) {
            const target = event.target;

            if (target.classList.contains("remove-room-btn")) {
                const roomBlock = target.closest(".guest-room-block");
                const roomBlocks = document.querySelectorAll(".guest-room-block");

                if (roomBlocks.length <= 1) {
                    return;
                }

                if (roomBlock) {
                    roomBlock.remove();
                }

                capNhatTomTatKhach();
            }

            if (target.classList.contains("plus-adult")) {
                const count = target.parentElement.querySelector(".adult-count");

                if (count) {
                    count.textContent = parseInt(count.textContent) + 1;
                }

                capNhatTomTatKhach();
            }

            if (target.classList.contains("minus-adult")) {
                const count = target.parentElement.querySelector(".adult-count");

                if (count) {
                    const value = parseInt(count.textContent);

                    if (value > 1) {
                        count.textContent = value - 1;
                    }
                }

                capNhatTomTatKhach();
            }

            if (target.classList.contains("plus-child")) {
                const block = target.closest(".guest-room-block");
                const count = block ? block.querySelector(".child-count") : null;

                if (count) {
                    count.textContent = parseInt(count.textContent) + 1;
                }

                if (block) {
                    hienThiTuoiTreEm(block);
                }

                capNhatTomTatKhach();
            }

            if (target.classList.contains("minus-child")) {
                const block = target.closest(".guest-room-block");
                const count = block ? block.querySelector(".child-count") : null;

                if (count) {
                    const value = parseInt(count.textContent);

                    if (value > 0) {
                        count.textContent = value - 1;
                    }
                }

                if (block) {
                    hienThiTuoiTreEm(block);
                }

                capNhatTomTatKhach();
            }
        });
    }

    function hienThiTuoiTreEm(block) {
        const childCountElement = block.querySelector(".child-count");
        const box = block.querySelector(".child-age-box");

        if (!childCountElement || !box) return;

        const childCount = parseInt(childCountElement.textContent);

        box.innerHTML = "";

        if (childCount === 0) return;

        const title = document.createElement("p");
        title.className = "child-age-title";
        title.textContent = "Tuổi của trẻ em";
        box.appendChild(title);

        for (let i = 1; i <= childCount; i++) {
            const select = document.createElement("select");
            select.className = "form-select form-select-sm child-age-select";

            for (let age = 0; age <= 12; age++) {
                const option = document.createElement("option");
                option.value = age;
                option.textContent = age + " tuổi";
                select.appendChild(option);
            }

            box.appendChild(select);
        }
    }

    function khoiPhucPhongTuHiddenInput() {
        const roomGuestsInput = document.getElementById("roomGuestsInput");
        const container = document.getElementById("roomGuestContainer");

        if (!roomGuestsInput || !container || !roomGuestsInput.value) return;

        const rooms = roomGuestsInput.value.split("|");

        container.innerHTML = "";
        roomCounter = rooms.length;

        rooms.forEach((room, index) => {
            const parts = room.split("-");
            const adults = parseInt(parts[0]) || 1;
            const children = parseInt(parts[1]) || 0;
            const roomNumber = index + 1;

            const block = document.createElement("div");
            block.className = "guest-room-block";
            block.setAttribute("data-room-index", roomNumber);

            block.innerHTML = `
                <div class="room-block-header">
                    <h6>PHÒNG ${roomNumber}</h6>
                    <button type="button" class="remove-room-btn">Xóa phòng</button>
                </div>

                <div class="guest-row">
                    <strong>Người lớn <span>(từ 13 tuổi trở lên)</span></strong>
                    <div class="counter">
                        <button type="button" class="counter-btn minus-adult">−</button>
                        <span class="adult-count">${adults}</span>
                        <button type="button" class="counter-btn plus-adult">+</button>
                    </div>
                </div>

                <div class="guest-row">
                    <strong>Trẻ em <span>(0–12 tuổi)</span></strong>
                    <div class="counter">
                        <button type="button" class="counter-btn minus-child">−</button>
                        <span class="child-count">${children}</span>
                        <button type="button" class="counter-btn plus-child">+</button>
                    </div>
                </div>

                <div class="child-age-box"></div>
            `;

            container.appendChild(block);
            hienThiTuoiTreEm(block);
        });
    }

    const applyGuestBtn = document.getElementById("applyGuestBtn");

    if (applyGuestBtn) {
        applyGuestBtn.addEventListener("click", function () {
            capNhatTomTatKhach();
            ganPromoCodeVaoForm();
            luuYourTripVaoSession();

            if (roomSearchForm) {
                roomSearchForm.submit();
            }
        });
    }

    const applyPromoBtn = document.getElementById("applyPromoBtn");
    const clearPromoBtn = document.getElementById("clearPromoBtn");

    if (clearPromoBtn) {
        clearPromoBtn.addEventListener("click", function () {
            const promoInput = document.getElementById("promoCode");
            const promoCodeHidden = document.getElementById("promoCodeHidden");

            if (promoInput) {
                promoInput.value = "";
            }

            if (promoCodeHidden) {
                promoCodeHidden.value = "";
            }

            appliedPromoCode = "";
            luuYourTripVaoSession();

            if (roomSearchForm) {
                roomSearchForm.submit();
            }
        });
    }

    if (applyPromoBtn) {
        applyPromoBtn.addEventListener("click", function () {
            ganPromoCodeVaoForm();
            luuYourTripVaoSession();

            if (roomSearchForm) {
                roomSearchForm.submit();
            }
        });
    }

    /* ============================= */
    /* YOUR TRIP */

    /* ============================= */

    function layTongSoPhong() {
        const roomCountInput = document.getElementById("roomCountInput");
        return roomCountInput ? parseInt(roomCountInput.value) || 1 : 1;
    }

    function laySoDemHienTai() {
        const checkIn = checkInInput ? checkInInput.value : "";
        const checkOut = checkOutInput ? checkOutInput.value : "";

        if (!checkIn || !checkOut) {
            return 0;
        }

        const inDate = new Date(checkIn + "T00:00:00");
        const outDate = new Date(checkOut + "T00:00:00");

        return Math.max(0, Math.round((outDate - inDate) / 86400000));
    }

    function layTextKhach() {
        const adultsInput = document.getElementById("adultsInput");
        const childrenInput = document.getElementById("childrenInput");

        const adults = adultsInput ? parseInt(adultsInput.value) || 1 : 1;
        const children = childrenInput ? parseInt(childrenInput.value) || 0 : 0;

        let text = adults + " người lớn";

        if (children > 0) {
            text += ", " + children + " trẻ em";
        }

        return text;
    }

    function formatVnd(value) {
        return new Intl.NumberFormat("vi-VN").format(value) + " VND";
    }

    function escapeHtml(value) {
        return String(value || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function luuYourTripVaoSession() {
        const checkIn = checkInInput ? checkInInput.value : "";
        const checkOut = checkOutInput ? checkOutInput.value : "";

        const adultsInput = document.getElementById("adultsInput");
        const childrenInput = document.getElementById("childrenInput");
        const roomCountInput = document.getElementById("roomCountInput");
        const roomGuestsInput = document.getElementById("roomGuestsInput");

        const data = {
            checkInDate: checkIn,
            checkOutDate: checkOut,
            adults: adultsInput ? adultsInput.value : "1",
            children: childrenInput ? childrenInput.value : "0",
            roomCount: roomCountInput ? roomCountInput.value : "1",
            roomGuests: roomGuestsInput ? roomGuestsInput.value : "",
            promoCode: appliedPromoCode,
            selectedRooms: danhSachPhongDaChon
        };

        sessionStorage.setItem(TRIP_STORAGE_KEY, JSON.stringify(data));
    }

    function khoiPhucYourTripTuSession() {
        const rawData = sessionStorage.getItem(TRIP_STORAGE_KEY);

        if (!rawData) {
            return;
        }

        try {
            const data = JSON.parse(rawData);

            const currentCheckIn = checkInInput ? checkInInput.value : "";
            const currentCheckOut = checkOutInput ? checkOutInput.value : "";

            if (data.checkInDate !== currentCheckIn || data.checkOutDate !== currentCheckOut) {
                sessionStorage.removeItem(TRIP_STORAGE_KEY);
                danhSachPhongDaChon = [];
                return;
            }

            if (Array.isArray(data.selectedRooms)) {
                danhSachPhongDaChon = data.selectedRooms;
            }

            if (!appliedPromoCode && data.promoCode) {
                appliedPromoCode = data.promoCode.trim().toUpperCase();
            }

            const totalRooms = layTongSoPhong();

            if (danhSachPhongDaChon.length > totalRooms) {
                danhSachPhongDaChon = danhSachPhongDaChon.slice(0, totalRooms);
            }

        } catch (error) {
            sessionStorage.removeItem(TRIP_STORAGE_KEY);
            danhSachPhongDaChon = [];
        }
    }

    function capNhatYourTrip() {
        const tripRoomList = document.getElementById("tripRoomList");

        const tripRoomSubtotalText = document.getElementById("tripRoomSubtotalText");
        const tripServiceChargeText = document.getElementById("tripServiceChargeText");
        const tripVatText = document.getElementById("tripVatText");
        const tripDiscountRow = document.getElementById("tripDiscountRow");
        const tripDiscountText = document.getElementById("tripDiscountText");
        const tripRoomTotalText = document.getElementById("tripRoomTotalText");

        const tripContinueBtn = document.getElementById("tripContinueBtn");

        if (!tripRoomList) {
            return;
        }

        const totalRooms = layTongSoPhong();
        const nights = laySoDemHienTai();
        const safeNights = nights > 0 ? nights : 1;
        const payableNights = nights > 0 ? nights : 0;
        const guestText = layTextKhach();

        let html = "";

        for (let i = 1; i <= totalRooms; i++) {
            const selectedRoom = danhSachPhongDaChon[i - 1];

            if (selectedRoom) {
                const roomTotal = selectedRoom.price * safeNights;

                html += `
                    <div class="trip-room-item">
                        <div class="trip-room-title">
                            <strong>Room ${i}</strong>

                            <div class="trip-room-actions">
                                <button type="button"
                                        class="trip-edit-room-btn"
                                        data-room-index="${i}">
                                    Edit
                                </button>

                                <button type="button"
                                        class="trip-remove-room-btn"
                                        data-room-index="${i}">
                                    Remove
                                </button>
                            </div>
                        </div>

                        <p class="trip-room-name">${escapeHtml(selectedRoom.variantName)}</p>
                        <small>${guestText}</small>

                        <div class="trip-room-price">
                            ${formatVnd(selectedRoom.price)} ${nights > 0 ? " / đêm" : ""}
                        </div>

                        <div class="trip-room-subtotal">
                            ${nights > 0 ? safeNights + " đêm: " + formatVnd(roomTotal) : "Chưa chọn ngày lưu trú"}
                        </div>
                    </div>
                `;
            } else {
                html += `
                    <div class="trip-room-item">
                        <div class="trip-room-title">
                            <strong>Room ${i}</strong>
                            <button type="button"
                                    class="trip-edit-room-btn"
                                    data-room-index="${i}">
                                Edit
                            </button>
                        </div>

                        <p class="trip-room-empty">Chưa chọn phòng</p>
                        <small>${guestText}</small>
                    </div>
                `;
            }
        }

        tripRoomList.innerHTML = html;

        const selectedCount = danhSachPhongDaChon.filter(Boolean).length;

        const roomSubtotal = danhSachPhongDaChon.reduce(function (sum, room) {
            if (!room) {
                return sum;
            }

            return sum + room.price * payableNights;
        }, 0);

        const serviceChargeAmount = Math.round(roomSubtotal * SERVICE_CHARGE_RATE);
        const vatAmount = Math.round((roomSubtotal + serviceChargeAmount) * VAT_RATE);

        const totalBeforeDiscount = roomSubtotal + serviceChargeAmount + vatAmount;

        const promotionDiscountInput = document.getElementById("promotionDiscountAmount");

        let discountAmount = promotionDiscountInput
            ? Number(promotionDiscountInput.value || 0)
            : 0;

        if (roomSubtotal <= 0) {
            discountAmount = 0;
        }

        if (discountAmount > totalBeforeDiscount) {
            discountAmount = totalBeforeDiscount;
        }

        const grandTotal = totalBeforeDiscount - discountAmount;

        if (tripRoomSubtotalText) {
            tripRoomSubtotalText.textContent = roomSubtotal > 0
                ? formatVnd(roomSubtotal)
                : "0 VND";
        }

        if (tripServiceChargeText) {
            tripServiceChargeText.textContent = serviceChargeAmount > 0
                ? formatVnd(serviceChargeAmount)
                : "0 VND";
        }

        if (tripVatText) {
            tripVatText.textContent = vatAmount > 0
                ? formatVnd(vatAmount)
                : "0 VND";
        }

        if (tripDiscountRow && tripDiscountText) {
            if (discountAmount > 0) {
                tripDiscountRow.style.display = "flex";
                tripDiscountText.textContent = "-" + formatVnd(discountAmount);
            } else {
                tripDiscountRow.style.display = "none";
                tripDiscountText.textContent = "-0 VND";
            }
        }

        if (tripRoomTotalText) {
            tripRoomTotalText.textContent = grandTotal > 0
                ? formatVnd(grandTotal)
                : "Chọn phòng để tính giá";
        }

        if (tripContinueBtn) {
            if (selectedCount === totalRooms) {
                tripContinueBtn.disabled = false;
                tripContinueBtn.classList.add("is-active");
            } else {
                tripContinueBtn.disabled = true;
                tripContinueBtn.classList.remove("is-active");
            }
        }

        document.querySelectorAll(".trip-edit-room-btn").forEach(function (button) {
            button.addEventListener("click", function () {
                phongDangChon = parseInt(this.dataset.roomIndex) || 1;
                capNhatThanhChonPhong();

                const filterBar = document.querySelector(".rose-filter-bar");

                if (filterBar) {
                    window.scrollTo({
                        top: filterBar.offsetTop,
                        behavior: "smooth"
                    });
                }
            });
        });

        document.querySelectorAll(".trip-remove-room-btn").forEach(function (button) {
            button.addEventListener("click", function () {
                const roomIndex = parseInt(this.dataset.roomIndex) || 1;

                danhSachPhongDaChon[roomIndex - 1] = null;

                phongDangChon = roomIndex;

                capNhatThanhChonPhong();
                capNhatYourTrip();
            });
        });

        luuYourTripVaoSession();
    }

    document.querySelectorAll(".reserve-btn").forEach(function (button) {
        button.addEventListener("click", function () {
            const totalRooms = layTongSoPhong();

            const variantId = this.dataset.variantId;
            const variantName = this.dataset.variantName;
            const price = Number(this.dataset.price || 0);

            danhSachPhongDaChon[phongDangChon - 1] = {
                variantId: variantId,
                variantName: variantName,
                price: price
            };

            capNhatYourTrip();

            if (totalRooms > 1 && phongDangChon < totalRooms) {
                phongDangChon++;
                capNhatThanhChonPhong();
            }

            const filterBar = document.querySelector(".rose-filter-bar");

            if (filterBar) {
                window.scrollTo({
                    top: filterBar.offsetTop,
                    behavior: "smooth"
                });
            }
        });
    });

    document.querySelectorAll(".rose-filter-bar a").forEach(function (link) {
        link.addEventListener("click", function () {
            luuYourTripVaoSession();

            const currentPromoCode = appliedPromoCode
                ? appliedPromoCode.trim().toUpperCase()
                : "";

            if (currentPromoCode) {
                const url = new URL(link.href, window.location.origin);
                url.searchParams.set("promoCode", currentPromoCode);
                link.href = url.pathname + url.search;
            }
        });
    });

    const continueToServiceForm = document.getElementById("continueToServiceForm");
    const selectedVariantIdsInput = document.getElementById("selectedVariantIdsInput");

    if (continueToServiceForm) {
        continueToServiceForm.addEventListener("submit", function (event) {
            const totalRooms = layTongSoPhong();

            const selectedRooms = danhSachPhongDaChon
                .slice(0, totalRooms)
                .filter(Boolean);

            if (selectedRooms.length < totalRooms) {
                event.preventDefault();
                return;
            }

            const variantIds = selectedRooms
                .map(function (room) {
                    return room.variantId;
                })
                .join(",");

            if (selectedVariantIdsInput) {
                selectedVariantIdsInput.value = variantIds;
            }

            const adultsInput = document.getElementById("adultsInput");
            const childrenInput = document.getElementById("childrenInput");
            const roomCountInput = document.getElementById("roomCountInput");
            const roomGuestsInput = document.getElementById("roomGuestsInput");
            const promoCodeHidden = document.getElementById("promoCodeHidden");

            const continueAdultsInput = document.getElementById("continueAdultsInput");
            const continueChildrenInput = document.getElementById("continueChildrenInput");
            const continueRoomCountInput = document.getElementById("continueRoomCountInput");
            const continueRoomGuestsInput = document.getElementById("continueRoomGuestsInput");
            const continuePromoCodeInput = document.getElementById("continuePromoCodeInput");

            if (continueAdultsInput && adultsInput) {
                continueAdultsInput.value = adultsInput.value;
            }

            if (continueChildrenInput && childrenInput) {
                continueChildrenInput.value = childrenInput.value;
            }

            if (continueRoomCountInput && roomCountInput) {
                continueRoomCountInput.value = roomCountInput.value;
            }

            if (continueRoomGuestsInput && roomGuestsInput) {
                continueRoomGuestsInput.value = roomGuestsInput.value;
            }

            if (continuePromoCodeInput && promoCodeHidden) {
                continuePromoCodeInput.value = promoCodeHidden.value;
            }

            sessionStorage.removeItem(TRIP_STORAGE_KEY);
            sessionStorage.removeItem("vhotel_selected_services_by_room");
        });
    }


    /* ============================= */
    /* ROOM DETAIL MODAL */
    /* ============================= */

    const roomDetailModal = document.getElementById("roomDetailModal");
    const roomDetailFrame = document.getElementById("roomDetailFrame");
    const roomDetailBackdrop = document.getElementById("roomDetailBackdrop");

    function moPopupChiTietPhong(url) {
        if (!roomDetailModal || !roomDetailFrame || !url || url === "javascript:void(0)") {
            return;
        }

        roomDetailFrame.src = url;
        roomDetailModal.classList.add("is-open");
        document.body.classList.add("modal-open");
    }

    function dongPopupChiTietPhong() {
        if (!roomDetailModal || !roomDetailFrame) {
            return;
        }

        roomDetailModal.classList.remove("is-open");
        document.body.classList.remove("modal-open");
        roomDetailFrame.src = "";
    }

    document.querySelectorAll(".room-detail-link").forEach(function (link) {
        link.addEventListener("click", function (event) {
            event.preventDefault();

            const url = link.getAttribute("data-url") || link.getAttribute("href");

            moPopupChiTietPhong(url);
        });
    });

    if (roomDetailBackdrop) {
        roomDetailBackdrop.addEventListener("click", dongPopupChiTietPhong);
    }

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            dongPopupChiTietPhong();
        }
    });

    window.addEventListener("message", function (event) {
        if (event.data && event.data.type === "CLOSE_ROOM_DETAIL_MODAL") {
            dongPopupChiTietPhong();
        }
    });

    khoiPhucPhongTuHiddenInput();

    document.querySelectorAll(".guest-room-block").forEach(block => {
        hienThiTuoiTreEm(block);
    });

    khoiPhucYourTripTuSession();

    hienThiHaiThang();
    capNhatTomTatNgay();
    capNhatTomTatKhach();
    capNhatYourTrip();

});
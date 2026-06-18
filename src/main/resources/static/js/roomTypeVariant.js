document.addEventListener("DOMContentLoaded", function () {

    const roomSearchForm = document.getElementById("roomSearchForm");

    const checkInInput = document.getElementById("checkInDate");
    const checkOutInput = document.getElementById("checkOutDate");

    const dateSummary = document.getElementById("dateSummary");
    const nightSummary = document.getElementById("nightSummary") || document.getElementById("nightSummaryEmpty");
    const dateError = document.getElementById("dateError");

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

            if (promoInput) {
                promoInput.value = "";
            }
        });
    }

    if (applyPromoBtn) {
        applyPromoBtn.addEventListener("click", function () {
            const promoInput = document.getElementById("promoCode");
            const promoSummary = document.getElementById("promoSummary");

            const promo = promoInput ? promoInput.value.trim() : "";

            if (promoSummary) {
                promoSummary.textContent = promo.length > 0 ? promo.toUpperCase() : "Chưa có mã";
            }

            dongTatCaBangChon();
        });
    }

    document.querySelectorAll(".reserve-btn").forEach(button => {
        button.addEventListener("click", function () {
            const variantId = this.dataset.variantId;

            const checkIn = checkInInput ? checkInInput.value : this.dataset.checkIn;
            const checkOut = checkOutInput ? checkOutInput.value : this.dataset.checkOut;

            const adultsInput = document.getElementById("adultsInput");
            const childrenInput = document.getElementById("childrenInput");
            const roomCountInput = document.getElementById("roomCountInput");

            const adults = adultsInput ? adultsInput.value : 1;
            const children = childrenInput ? childrenInput.value : 0;
            const totalRooms = roomCountInput ? parseInt(roomCountInput.value) : 1;

            danhSachPhongDaChon[phongDangChon - 1] = variantId;

            if (totalRooms > 1 && phongDangChon < totalRooms) {
                phongDangChon++;
                capNhatThanhChonPhong();

                const filterBar = document.querySelector(".rose-filter-bar");

                if (filterBar) {
                    window.scrollTo({
                        top: filterBar.offsetTop,
                        behavior: "smooth"
                    });
                }

                return;
            }

            let url = `/page/booking/create?variantIds=${danhSachPhongDaChon.join(",")}`;
            url += `&adults=${adults}&children=${children}&roomCount=${totalRooms}`;

            if (checkIn && checkOut && checkIn !== "null" && checkOut !== "null") {
                url += `&checkInDate=${checkIn}&checkOutDate=${checkOut}`;
            }

            window.location.href = url;
        });
    });

    const roomDetailModal = document.getElementById("roomDetailModal");
    const roomDetailFrame = document.getElementById("roomDetailFrame");
    const roomDetailBackdrop = document.getElementById("roomDetailBackdrop");

    function moPopupChiTietPhong(url) {
        if (!roomDetailModal || !roomDetailFrame || !url) {
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
        link.addEventListener("click", function () {
            const url = link.getAttribute("data-url");
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

    hienThiHaiThang();
    capNhatTomTatNgay();
    capNhatTomTatKhach();

});
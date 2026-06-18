document.addEventListener("DOMContentLoaded", function () {
    const approvalForm = document.getElementById("approvalForm");
    const approvalNote = document.getElementById("approvalNote");
    const approvalMessage = document.getElementById("approvalValidationMessage");

    if (approvalForm && approvalNote && approvalMessage) {
        approvalForm.addEventListener("submit", function (event) {
            const action = event.submitter ? event.submitter.value : "";

            approvalMessage.textContent = "";
            approvalNote.classList.remove("is-invalid");

            if (action === "reject" && approvalNote.value.trim() === "") {
                event.preventDefault();
                approvalNote.classList.add("is-invalid");
                approvalMessage.textContent = "Vui lòng nhập lý do từ chối tài khoản.";
                approvalNote.focus();
            }
        });
    }

    const checkboxes = Array.from(
        document.querySelectorAll('.multi-role-form input[type="checkbox"]')
    );
    const message = document.getElementById("roleValidationMessage");

    if (!message || checkboxes.length === 0) {
        return;
    }

    function roleLabel(roleCode) {
        const labels = {
            SYSTEM_ADMIN: "Quản trị hệ thống",
            HOTEL_ADMIN: "Quản trị khách sạn",
            MANAGER: "Quản lý",
            STOREKEEPER: "Thủ kho",
            RECEPTIONIST: "Lễ tân",
            GUEST: "Khách hàng"
        };
        return labels[roleCode] || roleCode;
    }

    function updateRoleMessage() {
        const selected = checkboxes
            .filter(function (checkbox) {
                return checkbox.checked;
            })
            .map(function (checkbox) {
                return checkbox.dataset.roleCode;
            });

        message.classList.remove("text-success", "text-danger", "text-warning");

        if (selected.length === 0) {
            message.textContent = "Vui lòng chọn ít nhất một vai trò.";
            message.classList.add("text-warning");
            return;
        }

        if (selected.includes("GUEST") && selected.length > 1) {
            message.textContent = "Không hợp lệ: Khách hàng không thể có thêm vai trò nhân viên.";
            message.classList.add("text-danger");
            return;
        }

        if (selected.includes("SYSTEM_ADMIN") && selected.length > 1) {
            message.textContent = "Không hợp lệ: Quản trị viên hệ thống không thể kiêm nhiệm vai trò khác.";
            message.classList.add("text-danger");
            return;
        }

        message.textContent = "Hợp lệ: " + selected.map(roleLabel).join(", ");
        message.classList.add("text-success");
    }

    checkboxes.forEach(function (checkbox) {
        checkbox.addEventListener("change", updateRoleMessage);
    });
    updateRoleMessage();
});

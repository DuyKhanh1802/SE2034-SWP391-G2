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

    const roleForm = document.querySelector(".single-role-form");
    const radios = Array.from(
        document.querySelectorAll('.single-role-form input[type="radio"][name="roleId"]')
    );
    const message = document.getElementById("roleValidationMessage");

    if (!roleForm || !message || radios.length === 0) {
        return;
    }

    function updateRoleMessage() {
        const selected = radios.find(function (radio) {
            return radio.checked;
        });

        message.classList.remove("text-success", "text-danger", "text-warning");

        if (!selected) {
            message.textContent = "Vui lòng chọn một vai trò.";
            message.classList.add("text-warning");
            return false;
        }

        const label = selected.closest(".role-check-item").querySelector("span").textContent.trim();
        message.textContent = "Vai trò đang chọn: " + label;
        message.classList.add("text-success");
        return true;
    }

    roleForm.addEventListener("submit", function (event) {
        if (!updateRoleMessage()) {
            event.preventDefault();
        }
    });

    radios.forEach(function (radio) {
        radio.addEventListener("change", updateRoleMessage);
    });
    updateRoleMessage();
});

document.addEventListener("DOMContentLoaded", function () {
    const voucherForm = document.getElementById("cashTransactionForm");

    if (!voucherForm) {
        return;
    }

    const amountInput = document.getElementById("amount");
    const paymentMethodInput = document.getElementById("paymentMethod");
    const descriptionInput = document.getElementById("description");
    const clientErrorMessage = document.getElementById("clientErrorMessage");

    function showFormError(input, message) {
        clientErrorMessage.textContent = message;
        clientErrorMessage.style.display = "block";
        input.focus();
        window.autoDismissAlert(clientErrorMessage);
    }

    function clearFormError() {
        clientErrorMessage.textContent = "";
        clientErrorMessage.style.display = "none";
    }

    voucherForm.addEventListener("submit", function (event) {
        clearFormError();

        if (!amountInput.value.trim()) {
            event.preventDefault();
            showFormError(amountInput, "Vui lòng nhập số tiền.");
            return;
        }

        const amount = Number(amountInput.value);
        if (!Number.isFinite(amount) || amount <= 0) {
            event.preventDefault();
            showFormError(amountInput, "Số tiền phải lớn hơn 0.");
            return;
        }

        if (!paymentMethodInput.value) {
            event.preventDefault();
            showFormError(paymentMethodInput, "Vui lòng chọn phương thức thanh toán.");
            return;
        }

        if (!descriptionInput.value.trim()) {
            event.preventDefault();
            showFormError(descriptionInput, "Vui lòng nhập nội dung phiếu.");
            return;
        }

        descriptionInput.value = descriptionInput.value.trim();
    });
});

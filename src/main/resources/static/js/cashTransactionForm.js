document.addEventListener("DOMContentLoaded", function () {
    const voucherForm = document.getElementById("cashTransactionForm");

    if (!voucherForm) {
        return;
    }

    const amountInput = document.getElementById("amount");
    const paymentMethodInput = document.getElementById("paymentMethod");
    const descriptionInput = document.getElementById("description");

    function setAmountMessage() {
        if (!amountInput.value) {
            amountInput.setCustomValidity("Vui lòng nhập số tiền.");
            return;
        }
        if (Number(amountInput.value) <= 0) {
            amountInput.setCustomValidity("Số tiền phải lớn hơn 0.");
            return;
        }
        amountInput.setCustomValidity("");
    }

    function setPaymentMethodMessage() {
        if (!paymentMethodInput.value) {
            paymentMethodInput.setCustomValidity("Vui lòng chọn phương thức thanh toán.");
            return;
        }
        paymentMethodInput.setCustomValidity("");
    }

    function setDescriptionMessage() {
        if (!descriptionInput.value.trim()) {
            descriptionInput.setCustomValidity("Vui lòng nhập nội dung phiếu.");
            return;
        }
        descriptionInput.setCustomValidity("");
    }

    amountInput.addEventListener("input", setAmountMessage);
    paymentMethodInput.addEventListener("change", setPaymentMethodMessage);
    descriptionInput.addEventListener("input", setDescriptionMessage);
    amountInput.addEventListener("invalid", setAmountMessage);
    paymentMethodInput.addEventListener("invalid", setPaymentMethodMessage);
    descriptionInput.addEventListener("invalid", setDescriptionMessage);

    // Gan san loi nhac tieng Viet de popup cua trinh duyet khong hien tieng Anh.
    setAmountMessage();
    setPaymentMethodMessage();
    setDescriptionMessage();

    voucherForm.addEventListener("submit", function (event) {
        setAmountMessage();
        setPaymentMethodMessage();
        setDescriptionMessage();

        if (!voucherForm.checkValidity()) {
            event.preventDefault();
            voucherForm.reportValidity();
        }
    });
});

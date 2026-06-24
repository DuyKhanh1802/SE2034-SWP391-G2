document.addEventListener("DOMContentLoaded", function () {
    const voucherForm = document.getElementById("cashTransactionForm");

    if (!voucherForm) {
        return;
    }

    const amountInput = document.getElementById("amount");
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

    function setDescriptionMessage() {
        if (!descriptionInput.value.trim()) {
            descriptionInput.setCustomValidity("Vui lòng nhập nội dung phiếu.");
            return;
        }
        descriptionInput.setCustomValidity("");
    }

    amountInput.addEventListener("input", setAmountMessage);
    descriptionInput.addEventListener("input", setDescriptionMessage);
    amountInput.addEventListener("invalid", setAmountMessage);
    descriptionInput.addEventListener("invalid", setDescriptionMessage);

    // Gắn sẵn lời nhắc tiếng Việt để popup của trình duyệt không hiện tiếng Anh.
    setAmountMessage();
    setDescriptionMessage();

    voucherForm.addEventListener("submit", function (event) {
        setAmountMessage();
        setDescriptionMessage();

        if (!voucherForm.checkValidity()) {
            event.preventDefault();
            voucherForm.reportValidity();
        }
    });
});

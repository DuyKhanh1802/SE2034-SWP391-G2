document.addEventListener("DOMContentLoaded", function () {
    const checkoutForm = document.querySelector(".checkout-form");
    const paymentMethod = document.getElementById("checkoutPaymentMethod");
    const vietQrModal = document.getElementById("vietQrModal");
    const confirmTransferPayment = document.getElementById("confirmTransferPayment");
    let transferConfirmed = false;

    if (!checkoutForm || !paymentMethod || !vietQrModal || !confirmTransferPayment) {
        return;
    }

    checkoutForm.addEventListener("submit", function (event) {
        if (paymentMethod.value !== "TRANSFER" || transferConfirmed) {
            return;
        }

        event.preventDefault();
        vietQrModal.classList.add("open");
        vietQrModal.setAttribute("aria-hidden", "false");
    });

    confirmTransferPayment.addEventListener("click", function () {
        transferConfirmed = true;
        checkoutForm.submit();
    });

    vietQrModal.querySelectorAll("[data-close-vietqr]").forEach(function (element) {
        element.addEventListener("click", function () {
            vietQrModal.classList.remove("open");
            vietQrModal.setAttribute("aria-hidden", "true");
        });
    });
});

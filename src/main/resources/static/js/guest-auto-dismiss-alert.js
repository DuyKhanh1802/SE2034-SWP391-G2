document.addEventListener("DOMContentLoaded", function () {
    const alerts = document.querySelectorAll(
        ".alert-error, .booking-error-box, .guest-alert, .alert.alert-danger, .alert.alert-warning"
    );

    alerts.forEach(function (alert) {
        setTimeout(function () {
            alert.style.transition = "opacity 0.35s ease, transform 0.35s ease";
            alert.style.opacity = "0";
            alert.style.transform = "translateY(-6px)";

            setTimeout(function () {
                alert.remove();
            }, 350);
        }, 3000);
    });
});
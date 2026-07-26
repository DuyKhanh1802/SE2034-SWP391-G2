document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll("[data-auto-dismiss-alert]").forEach(function (alert) {
        window.autoDismissAlert(alert);
    });
});

window.autoDismissAlert = function (alert) {
    if (!alert) {
        return;
    }

    const defaultDelay = 3000;
    const transitionDuration = 350;
    const configuredDelay = Number(alert.dataset.autoDismissDelay);
    const delay = alert.dataset.autoDismissDelay !== undefined
        && Number.isFinite(configuredDelay)
        && configuredDelay >= 0
        ? configuredDelay
        : defaultDelay;

    if (alert.autoDismissTimer) {
        clearTimeout(alert.autoDismissTimer);
    }

    alert.style.opacity = "1";
    alert.style.transform = "translateY(0)";
    alert.style.pointerEvents = "auto";

    alert.autoDismissTimer = setTimeout(function () {
        alert.style.transition = `opacity ${transitionDuration}ms ease, transform ${transitionDuration}ms ease`;
        alert.style.opacity = "0";
        alert.style.transform = "translateY(-6px)";
        alert.style.pointerEvents = "none";

        setTimeout(function () {
            if (alert.hasAttribute("data-auto-dismiss-reusable")) {
                alert.style.display = "none";
                alert.style.transition = "";
                alert.style.opacity = "1";
                alert.style.transform = "translateY(0)";
                alert.style.pointerEvents = "auto";
                return;
            }

            alert.remove();
        }, transitionDuration);
    }, delay);
};

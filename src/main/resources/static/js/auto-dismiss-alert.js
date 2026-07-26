document.addEventListener("DOMContentLoaded", function () {
    const defaultDelay = 3000;
    const transitionDuration = 350;

    document.querySelectorAll("[data-auto-dismiss-alert]").forEach(function (alert) {
        const delayValue = alert.dataset.autoDismissDelay;
        const configuredDelay = delayValue ? Number(delayValue) : Number.NaN;
        const delay = Number.isFinite(configuredDelay) && configuredDelay >= 0
            ? configuredDelay
            : defaultDelay;

        setTimeout(function () {
            alert.style.transition = `opacity ${transitionDuration}ms ease, transform ${transitionDuration}ms ease`;
            alert.style.opacity = "0";
            alert.style.transform = "translateY(-6px)";
            alert.style.pointerEvents = "none";

            setTimeout(function () {
                alert.remove();
            }, transitionDuration);
        }, delay);
    });
});

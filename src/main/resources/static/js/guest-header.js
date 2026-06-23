document.addEventListener("DOMContentLoaded", function () {
    const guestProfile = document.getElementById("guestProfileDropdown");
    const guestProfileBtn = document.getElementById("guestProfileBtn");

    if (!guestProfile || !guestProfileBtn) {
        return;
    }

    guestProfileBtn.addEventListener("click", function (event) {
        event.stopPropagation();
        guestProfile.classList.toggle("is-open");
    });

    document.addEventListener("click", function (event) {
        if (!guestProfile.contains(event.target)) {
            guestProfile.classList.remove("is-open");
        }
    });
});
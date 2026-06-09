document.addEventListener("DOMContentLoaded", function () {
    const header = document.getElementById("publicHeader");

    function handleHeaderScroll() {
        if (!header) return;

        if (window.scrollY > 80) {
            header.classList.add("is-compact");
        } else {
            header.classList.remove("is-compact");
        }
    }

    handleHeaderScroll();
    window.addEventListener("scroll", handleHeaderScroll);

    const sliderButtons = document.querySelectorAll(".slider-btn");

    sliderButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            const targetId = button.getAttribute("data-target");
            const slider = document.getElementById(targetId);

            if (!slider) return;

            const direction = button.classList.contains("slider-next") ? 1 : -1;
            const scrollAmount = slider.clientWidth * 0.82;

            slider.scrollBy({
                left: direction * scrollAmount,
                behavior: "smooth"
            });
        });
    });
});
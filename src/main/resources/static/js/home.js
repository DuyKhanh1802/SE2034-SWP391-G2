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

    const sliderButtons = document.querySelectorAll(".slider-btn, .room-hero-arrow");

    sliderButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            const targetId = button.getAttribute("data-target");
            const slider = document.getElementById(targetId);

            if (!slider) return;

            const isNext =
                button.classList.contains("slider-next") ||
                button.classList.contains("room-hero-next");

            const direction = isNext ? 1 : -1;
            const scrollAmount = slider.clientWidth;

            slider.scrollBy({
                left: direction * scrollAmount,
                behavior: "smooth"
            });
        });
    });

    // =========================
    // HERO VIDEO CONTROLS
    // =========================
    const heroVideo = document.querySelector(".hero-video");
    const soundToggle = document.getElementById("soundToggle");
    const videoToggle = document.getElementById("videoToggle");

    if (heroVideo && soundToggle) {
        soundToggle.addEventListener("click", function () {
            const icon = soundToggle.querySelector("i");

            heroVideo.muted = !heroVideo.muted;

            if (heroVideo.muted) {
                icon.className = "bi bi-volume-mute-fill";
            } else {
                icon.className = "bi bi-volume-up-fill";
                heroVideo.play();
            }
        });
    }

    if (heroVideo && videoToggle) {
        videoToggle.addEventListener("click", function () {
            const icon = videoToggle.querySelector("i");

            if (heroVideo.paused) {
                heroVideo.play();
                icon.className = "bi bi-pause-fill";
            } else {
                heroVideo.pause();
                icon.className = "bi bi-play-fill";
            }
        });
    }
});
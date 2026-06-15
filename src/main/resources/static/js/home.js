document.addEventListener("DOMContentLoaded", function () {
    /* ================= HEADER SCROLL ================= */
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


    /* ================= ROOM TYPE SLIDER ================= */
    const roomSliderButtons = document.querySelectorAll(".slider-btn, .room-hero-arrow");

    roomSliderButtons.forEach(function (button) {
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


    /* ================= HERO VIDEO / IMAGE SLIDER ================= */
    const slides = document.querySelectorAll(".hero-media-slide");
    const prevBtn = document.getElementById("heroPrevBtn");
    const nextBtn = document.getElementById("heroNextBtn");

    const heroVideo = document.getElementById("heroVideo");
    const playPauseBtn = document.getElementById("heroPlayPauseBtn");
    const muteBtn = document.getElementById("heroMuteBtn");

    let currentSlide = 0;

    function showSlide(index) {
        if (!slides.length) return;

        slides[currentSlide].classList.remove("active");

        currentSlide = (index + slides.length) % slides.length;

        slides[currentSlide].classList.add("active");

        handleVideoBySlide();
    }

    function handleVideoBySlide() {
        if (!heroVideo) return;

        const activeSlide = slides[currentSlide];
        const videoInActiveSlide = activeSlide.querySelector("video");

        if (videoInActiveSlide) {
            heroVideo.play().catch(function () {
                // Trình duyệt có thể chặn autoplay nếu có âm thanh
            });
        } else {
            heroVideo.pause();
        }
    }

    if (prevBtn) {
        prevBtn.addEventListener("click", function () {
            showSlide(currentSlide - 1);
        });
    }

    if (nextBtn) {
        nextBtn.addEventListener("click", function () {
            showSlide(currentSlide + 1);
        });
    }


    /* ================= PLAY / PAUSE VIDEO ================= */
    if (playPauseBtn && heroVideo) {
        playPauseBtn.addEventListener("click", function () {
            if (heroVideo.paused) {
                heroVideo.play();

                playPauseBtn.innerHTML = '<i class="bi bi-pause-fill"></i>';
            } else {
                heroVideo.pause();

                playPauseBtn.innerHTML = '<i class="bi bi-play-fill"></i>';
            }
        });
    }


    /* ================= MUTE / UNMUTE VIDEO ================= */
    if (muteBtn && heroVideo) {
        muteBtn.addEventListener("click", function () {
            heroVideo.muted = !heroVideo.muted;

            if (heroVideo.muted) {
                muteBtn.innerHTML = '<i class="bi bi-volume-mute-fill"></i>';
            } else {
                muteBtn.innerHTML = '<i class="bi bi-volume-up-fill"></i>';

                heroVideo.play().catch(function () {
                    console.log("Trình duyệt đang chặn phát video có âm thanh.");
                });
            }
        });
    }


    /* ================= DEFAULT VIDEO STATE ================= */
    if (heroVideo) {
        heroVideo.muted = true;

        if (muteBtn) {
            muteBtn.innerHTML = '<i class="bi bi-volume-mute-fill"></i>';
        }
    }
});
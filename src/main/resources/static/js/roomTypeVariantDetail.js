document.addEventListener("DOMContentLoaded", function () {
    const detailPage = document.getElementById("roomDetailPage");

    const imageUrlString = detailPage
        ? detailPage.getAttribute("data-image-urls")
        : "";

    const roomImages = imageUrlString
        ? imageUrlString.split("|")
            .map(function (url) {
                return url.trim();
            })
            .filter(function (url) {
                return url.length > 0;
            })
        : [];

    let currentIndex = 0;

    const mainImage = document.getElementById("mainRoomImage");
    const currentImageIndex = document.getElementById("currentImageIndex");
    const prevBtn = document.getElementById("prevImageBtn");
    const nextBtn = document.getElementById("nextImageBtn");
    const closeBtn = document.getElementById("closeDetailBtn");

    function updateImage() {
        if (!roomImages || roomImages.length === 0 || !mainImage) {
            return;
        }

        mainImage.src = roomImages[currentIndex];

        if (currentImageIndex) {
            currentImageIndex.textContent = currentIndex + 1;
        }
    }

    if (roomImages.length > 0) {
        updateImage();
    }

    if (prevBtn) {
        prevBtn.addEventListener("click", function () {
            if (roomImages.length === 0) {
                return;
            }

            currentIndex--;

            if (currentIndex < 0) {
                currentIndex = roomImages.length - 1;
            }

            updateImage();
        });
    }

    if (nextBtn) {
        nextBtn.addEventListener("click", function () {
            if (roomImages.length === 0) {
                return;
            }

            currentIndex++;

            if (currentIndex >= roomImages.length) {
                currentIndex = 0;
            }

            updateImage();
        });
    }

    if (closeBtn) {
        closeBtn.addEventListener("click", function () {
            if (window.parent && window.parent !== window) {
                window.parent.postMessage({
                    type: "CLOSE_ROOM_DETAIL_MODAL"
                }, "*");
            } else {
                window.history.back();
            }
        });
    }
});
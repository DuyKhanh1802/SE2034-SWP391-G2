document.addEventListener("DOMContentLoaded", function () {
    const roomImageModal = document.getElementById("roomImageModal");
    const roomModalImage = document.getElementById("roomModalImage");

    if (!roomImageModal || !roomModalImage) {
        return;
    }

    roomImageModal.addEventListener("show.bs.modal", function (event) {
        const clickedImage = event.relatedTarget;

        if (!clickedImage) {
            return;
        }

        const imageUrl = clickedImage.getAttribute("data-image-url");

        if (!imageUrl) {
            return;
        }

        roomModalImage.setAttribute("src", imageUrl);
    });

    roomImageModal.addEventListener("hidden.bs.modal", function () {
        roomModalImage.setAttribute("src", "");
    });
});
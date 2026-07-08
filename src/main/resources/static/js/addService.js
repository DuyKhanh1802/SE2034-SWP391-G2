document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("addServiceForm");

    if (!form) {
        return;
    }

    const nameInput = document.getElementById("name");
    const categoryInput = document.getElementById("categoryId");
    const priceInput = document.getElementById("price");
    const statusInput = document.getElementById("isAvailable");
    const imageInput = document.getElementById("imageFile");
    const descriptionInput = document.getElementById("description");

    const toast = document.getElementById("serviceToast");
    const toastMessage = document.getElementById("serviceToastMessage");
    const serverErrorMessage = document.getElementById("serverErrorMessage");

    const imagePreview = document.getElementById("serviceImagePreview");
    const imagePreviewImg = document.getElementById("serviceImagePreviewImg");

    const MAX_NAME_LENGTH = 200;
    const MAX_DESCRIPTION_LENGTH = 500;
    const MAX_PRICE = 100000000;
    const MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    const ALLOWED_IMAGE_TYPES = ["image/jpeg", "image/jpg", "image/png", "image/webp"];
    const ALLOWED_IMAGE_EXTENSIONS = [".jpg", ".jpeg", ".png", ".webp"];

    let toastTimer = null;

    function showToast(message) {
        if (!toast || !toastMessage) {
            alert(message);
            return;
        }

        toastMessage.textContent = message;
        toast.classList.add("show");

        if (toastTimer) {
            clearTimeout(toastTimer);
        }

        toastTimer = setTimeout(function () {
            toast.classList.remove("show");
        }, 3000);
    }

    function markError(input) {
        if (!input) {
            return;
        }

        input.classList.add("service-input-error");
        input.focus();
    }

    function clearError(input) {
        if (!input) {
            return;
        }

        input.classList.remove("service-input-error");
    }

    function clearAllErrors() {
        [
            nameInput,
            categoryInput,
            priceInput,
            statusInput,
            imageInput,
            descriptionInput
        ].forEach(clearError);
    }

    function showValidationError(input, message) {
        clearAllErrors();
        markError(input);
        showToast(message);
    }

    function isAllowedImageExtension(fileName) {
        const lowerFileName = fileName.toLowerCase();

        return ALLOWED_IMAGE_EXTENSIONS.some(function (extension) {
            return lowerFileName.endsWith(extension);
        });
    }

    function resetImagePreview() {
        if (!imagePreview || !imagePreviewImg) {
            return;
        }

        imagePreview.hidden = true;
        imagePreviewImg.removeAttribute("src");
    }

    function showImagePreview(file) {
        if (!imagePreview || !imagePreviewImg || !file) {
            resetImagePreview();
            return;
        }

        imagePreviewImg.src = URL.createObjectURL(file);
        imagePreview.hidden = false;
    }

    function validateImageFile(file) {
        if (!file) {
            return true;
        }

        const hasValidType = file.type && ALLOWED_IMAGE_TYPES.includes(file.type.toLowerCase());
        const hasValidExtension = file.name && isAllowedImageExtension(file.name);

        if (!hasValidType || !hasValidExtension) {
            showValidationError(imageInput, "File tải lên phải là ảnh JPG, JPEG, PNG hoặc WEBP.");
            resetImagePreview();
            return false;
        }

        if (file.size > MAX_IMAGE_SIZE) {
            showValidationError(imageInput, "Kích thước ảnh không được vượt quá 5MB.");
            resetImagePreview();
            return false;
        }

        return true;
    }

    form.addEventListener("submit", function (event) {
        clearAllErrors();

        const serviceName = nameInput.value.trim();
        const categoryId = categoryInput.value;
        const priceText = priceInput.value.trim();
        const priceValue = Number(priceText);
        const statusValue = statusInput.value;
        const description = descriptionInput.value.trim();
        const imageFile = imageInput.files && imageInput.files.length > 0
            ? imageInput.files[0]
            : null;

        nameInput.value = serviceName;
        descriptionInput.value = description;

        if (!serviceName) {
            event.preventDefault();
            showValidationError(nameInput, "Vui lòng nhập tên dịch vụ.");
            return;
        }

        if (serviceName.length > MAX_NAME_LENGTH) {
            event.preventDefault();
            showValidationError(nameInput, "Tên dịch vụ không được vượt quá 200 ký tự.");
            return;
        }

        if (!categoryId) {
            event.preventDefault();
            showValidationError(categoryInput, "Vui lòng chọn loại dịch vụ.");
            return;
        }

        if (!priceText) {
            event.preventDefault();
            showValidationError(priceInput, "Vui lòng nhập giá dịch vụ.");
            return;
        }

        if (!Number.isFinite(priceValue)) {
            event.preventDefault();
            showValidationError(priceInput, "Vui lòng nhập giá dịch vụ.");
            return;
        }

        if (priceValue <= 0) {
            event.preventDefault();
            showValidationError(priceInput, "Giá dịch vụ phải lớn hơn 0.");
            return;
        }

        if (priceValue > MAX_PRICE) {
            event.preventDefault();
            showValidationError(priceInput, "Giá dịch vụ không được vượt quá 100,000,000 VND.");
            return;
        }

        if (!Number.isInteger(priceValue)) {
            event.preventDefault();
            showValidationError(priceInput, "Giá dịch vụ phải là số nguyên VND.");
            return;
        }

        if (statusValue !== "true" && statusValue !== "false") {
            event.preventDefault();
            showValidationError(statusInput, "Trạng thái dịch vụ không hợp lệ.");
            return;
        }

        if (description.length > MAX_DESCRIPTION_LENGTH) {
            event.preventDefault();
            showValidationError(descriptionInput, "Mô tả dịch vụ không được vượt quá 500 ký tự.");
            return;
        }

        if (!validateImageFile(imageFile)) {
            event.preventDefault();
        }
    });

    [
        nameInput,
        categoryInput,
        priceInput,
        statusInput,
        descriptionInput
    ].forEach(function (input) {
        if (!input) {
            return;
        }

        input.addEventListener("input", function () {
            clearError(input);
        });

        input.addEventListener("change", function () {
            clearError(input);
        });
    });

    if (imageInput) {
        imageInput.addEventListener("change", function () {
            clearError(imageInput);

            const file = imageInput.files && imageInput.files.length > 0
                ? imageInput.files[0]
                : null;

            if (!file) {
                resetImagePreview();
                return;
            }

            if (validateImageFile(file)) {
                showImagePreview(file);
            }
        });
    }

    if (serverErrorMessage && serverErrorMessage.textContent.trim()) {
        showToast(serverErrorMessage.textContent.trim());
    }
});
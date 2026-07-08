document.addEventListener('DOMContentLoaded', function () {
    const roomNumberSelect = document.getElementById('roomNumber');
    const floorDisplay = document.getElementById('floorDisplay');
    const roomTypeDisplay = document.getElementById('roomTypeDisplay');
    const variantSelect = document.getElementById('variantId');
    const statusSelect = document.getElementById('status');
    const noteTextarea = document.getElementById('note');
    const saveRoomButton = document.getElementById('saveRoomButton');
    const addRoomForm = document.getElementById('addRoomForm');

    if (!roomNumberSelect || !floorDisplay || !roomTypeDisplay || !variantSelect) {
        return;
    }

    const allVariantOptions = Array.from(variantSelect.querySelectorAll('option'))
        .filter(function (option) {
            return option.value !== '';
        })
        .map(function (option) {
            return option.cloneNode(true);
        });

    const selectedVariantValue = variantSelect.value;

    function normalizeText(value) {
        return (value || '')
            .toString()
            .trim()
            .toLowerCase()
            .replace(/\s+/g, ' ');
    }

    function formatRoomTypeForDisplay(roomTypeName) {
        return (roomTypeName || '')
            .toString()
            .trim()
            .replace(/\s+Room$/i, '');
    }

    function setDetailFieldsEnabled(enabled) {
        variantSelect.disabled = !enabled;

        if (statusSelect) {
            statusSelect.disabled = !enabled;
        }

        if (noteTextarea) {
            noteTextarea.disabled = !enabled;
        }

        if (saveRoomButton) {
            saveRoomButton.disabled = !enabled;
        }
    }

    function resetRoomDetailFields() {
        floorDisplay.value = '';
        roomTypeDisplay.value = '';

        variantSelect.innerHTML = '<option value="">Chọn loại phòng chi tiết</option>';
        variantSelect.value = '';

        setDetailFieldsEnabled(false);
    }

    function renderVariantOptions(roomTypeName) {
        const normalizedRoomTypeName = normalizeText(roomTypeName);

        variantSelect.innerHTML = '<option value="">Chọn loại phòng chi tiết</option>';

        allVariantOptions.forEach(function (option) {
            const optionRoomTypeName = normalizeText(option.dataset.roomType);

            if (optionRoomTypeName === normalizedRoomTypeName) {
                variantSelect.appendChild(option.cloneNode(true));
            }
        });

        if (selectedVariantValue) {
            const selectedOption = Array.from(variantSelect.options)
                .find(function (option) {
                    return option.value === selectedVariantValue;
                });

            if (selectedOption) {
                variantSelect.value = selectedVariantValue;
            }
        }

        const hasAvailableVariant = variantSelect.options.length > 1;

        variantSelect.disabled = !hasAvailableVariant;

        if (saveRoomButton) {
            saveRoomButton.disabled = !hasAvailableVariant;
        }
    }

    function handleRoomNumberChange() {
        const selectedOption = roomNumberSelect.options[roomNumberSelect.selectedIndex];

        if (!selectedOption || !selectedOption.value) {
            resetRoomDetailFields();
            return;
        }

        const floor = selectedOption.dataset.floor || '';
        const roomTypeName = selectedOption.dataset.roomType || '';

        floorDisplay.value = floor;
        roomTypeDisplay.value = formatRoomTypeForDisplay(roomTypeName);

        setDetailFieldsEnabled(true);
        renderVariantOptions(roomTypeName);
    }

    if (addRoomForm) {
        addRoomForm.addEventListener('submit', function (event) {
            const selectedStatus = statusSelect ? statusSelect.value : '';
            const note = noteTextarea ? noteTextarea.value.trim() : '';

            if (selectedStatus === 'MAINTENANCE' && note.length === 0) {
                event.preventDefault();
                alert('Vui lòng nhập lý do bảo trì.');
                noteTextarea.focus();
                return;
            }

            if (note.length > 500) {
                event.preventDefault();
                alert('Số ký tự đã vượt quá giới hạn cho phép.');
                noteTextarea.focus();
            }
        });
    }

    resetRoomDetailFields();

    if (roomNumberSelect.value) {
        handleRoomNumberChange();
    }

    roomNumberSelect.addEventListener('change', handleRoomNumberChange);
});
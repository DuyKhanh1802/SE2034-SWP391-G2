document.addEventListener('DOMContentLoaded', function () {
    const roomNumberSelect = document.getElementById('roomNumber');
    const floorDisplay = document.getElementById('floorDisplay');
    const roomTypeDisplay = document.getElementById('roomTypeDisplay');
    const viewTypeDisplay = document.getElementById('viewTypeDisplay');
    const variantSelect = document.getElementById('variantId');
    const statusSelect = document.getElementById('status');
    const noteTextarea = document.getElementById('note');
    const saveRoomButton = document.getElementById('saveRoomButton');
    const addRoomForm = document.getElementById('addRoomForm');
    let toastTimer;


    if (!roomNumberSelect || !floorDisplay || !roomTypeDisplay || !viewTypeDisplay || !variantSelect) {
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


    function formatViewTypeForDisplay(viewType) {
        const labels = {
            GARDEN: 'Garden View',
            CITY: 'City View',
            SEA: 'Sea View',
            POOL: 'Pool View'
        };


        return labels[viewType] || viewType || '';
    }


    function showErrorToast(message) {
        let toast = document.getElementById('addRoomClientToast');


        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'addRoomClientToast';
            toast.className = 'client-toast';
            toast.innerHTML = '<i class="fa-solid fa-circle-exclamation"></i><span></span>';
            document.body.appendChild(toast);
        }


        toast.querySelector('span').textContent = message;
        toast.classList.add('show');
        clearTimeout(toastTimer);
        toastTimer = setTimeout(function () {
            toast.classList.remove('show');
        }, 3000);
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
        viewTypeDisplay.value = '';


        variantSelect.innerHTML = '<option value="">Chọn loại phòng chi tiết</option>';
        variantSelect.value = '';


        setDetailFieldsEnabled(false);
    }


    function renderVariantOptions(roomTypeName, viewType) {
        const normalizedRoomTypeName = normalizeText(roomTypeName);
        const normalizedViewType = normalizeText(viewType);


        variantSelect.innerHTML = '<option value="">Chọn loại phòng chi tiết</option>';


        allVariantOptions.forEach(function (option) {
            const optionRoomTypeName = normalizeText(option.dataset.roomType);
            const optionViewType = normalizeText(option.dataset.viewType);


            if (optionRoomTypeName === normalizedRoomTypeName
                && optionViewType === normalizedViewType) {
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


        if (!variantSelect.value && variantSelect.options.length === 2) {
            variantSelect.selectedIndex = 1;
        }


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
        const viewType = selectedOption.dataset.viewType || '';


        floorDisplay.value = floor;
        roomTypeDisplay.value = formatRoomTypeForDisplay(roomTypeName);
        viewTypeDisplay.value = formatViewTypeForDisplay(viewType);


        setDetailFieldsEnabled(true);
        renderVariantOptions(roomTypeName, viewType);
    }


    if (addRoomForm) {
        addRoomForm.addEventListener('submit', function (event) {
            const selectedStatus = statusSelect ? statusSelect.value : '';
            const note = noteTextarea ? noteTextarea.value.trim() : '';


            if (!variantSelect.value) {
                event.preventDefault();
                showErrorToast('Không được để loại phòng trống.');
                variantSelect.focus();
                return;
            }


            if (!selectedStatus) {
                event.preventDefault();
                showErrorToast('Không được để trạng thái trống.');
                statusSelect.focus();
                return;
            }


            if (selectedStatus === 'MAINTENANCE' && note.length === 0) {
                event.preventDefault();
                showErrorToast('Vui lòng nhập lý do bảo trì.');
                noteTextarea.focus();
                return;
            }


            if (note.length > 500) {
                event.preventDefault();
                showErrorToast('Số ký tự đã vượt quá giới hạn cho phép.');
                noteTextarea.focus();
            }
        });
    }


    document.querySelectorAll('.error-toast, .success-toast').forEach(function (toast) {
        setTimeout(function () {
            toast.remove();
        }, 3000);
    });


    resetRoomDetailFields();


    if (roomNumberSelect.value) {
        handleRoomNumberChange();
    }


    roomNumberSelect.addEventListener('change', handleRoomNumberChange);
});

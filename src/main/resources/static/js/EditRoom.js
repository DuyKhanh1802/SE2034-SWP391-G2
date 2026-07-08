document.addEventListener('DOMContentLoaded', function () {
    const editRoomForm = document.getElementById('editRoomForm');
    const roomStatus = document.getElementById('roomStatus');
    const roomNote = document.getElementById('roomNote');

    const deleteRoomModal = document.getElementById('deleteRoomModal');
    const openDeleteRoomModal = document.getElementById('openDeleteRoomModal');
    const closeDeleteRoomModal = document.getElementById('closeDeleteRoomModal');

    const clientToast = document.getElementById('clientToast');
    const clientToastMessage = document.getElementById('clientToastMessage');

    function showToast(message) {
        if (!clientToast || !clientToastMessage) {
            alert(message);
            return;
        }

        clientToastMessage.textContent = message;
        clientToast.classList.add('show');

        setTimeout(function () {
            clientToast.classList.remove('show');
        }, 3000);
    }

    function validateEditRoomForm(event) {
        const statusValue = roomStatus ? roomStatus.value.trim() : '';
        const noteValue = roomNote ? roomNote.value.trim() : '';

        if (!statusValue) {
            event.preventDefault();
            showToast('Vui lòng chọn trạng thái phòng.');
            return;
        }

        if (statusValue !== 'AVAILABLE' && statusValue !== 'MAINTENANCE') {
            event.preventDefault();
            showToast('Trạng thái chỉ được phép cập nhật là AVAILABLE hoặc MAINTENANCE.');
            return;
        }

        if (roomNote && roomNote.value.length > 500) {
            event.preventDefault();
            showToast('Số ký tự đã vượt quá giới hạn cho phép.');
            return;
        }

        if (statusValue === 'MAINTENANCE' && noteValue.length === 0) {
            event.preventDefault();
            showToast('Vui lòng nhập lý do bảo trì.');
        }
    }

    function openModal() {
        if (deleteRoomModal) {
            deleteRoomModal.classList.add('show');
        }
    }

    function closeModal() {
        if (deleteRoomModal) {
            deleteRoomModal.classList.remove('show');
        }
    }

    if (editRoomForm) {
        editRoomForm.addEventListener('submit', validateEditRoomForm);
    }

    if (openDeleteRoomModal) {
        openDeleteRoomModal.addEventListener('click', openModal);
    }

    if (closeDeleteRoomModal) {
        closeDeleteRoomModal.addEventListener('click', closeModal);
    }

    if (deleteRoomModal) {
        deleteRoomModal.addEventListener('click', function (event) {
            if (event.target === deleteRoomModal) {
                closeModal();
            }
        });
    }

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            closeModal();
        }
    });

    const serverToasts = document.querySelectorAll('.room-toast');
    serverToasts.forEach(function (toast) {
        setTimeout(function () {
            toast.style.display = 'none';
        }, 3000);
    });
});
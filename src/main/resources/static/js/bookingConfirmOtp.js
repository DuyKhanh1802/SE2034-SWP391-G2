document.addEventListener("DOMContentLoaded", function () {
    const guestEmailInput = document.getElementById("guestEmailInput");
    const sendOtpBtn = document.getElementById("sendBookingOtpBtn");
    const confirmBookingBtn = document.getElementById("confirmBookingBtn");

    const emailVerifyMessage = document.getElementById("emailVerifyMessage");

    const otpModal = document.getElementById("bookingOtpModal");
    const closeOtpModalBtn = document.getElementById("closeBookingOtpModal");
    const otpInput = document.getElementById("bookingOtpInput");
    const otpMessage = document.getElementById("bookingOtpMessage");
    const verifyOtpBtn = document.getElementById("verifyBookingOtpBtn");
    const resendOtpBtn = document.getElementById("resendBookingOtpBtn");

    const otpCountdownBox = document.getElementById("otpCountdownBox");
    const otpCountdownText = document.getElementById("otpCountdownText");

    const AUTO_RESEND_SECONDS = 60;

    let autoResendTimer = null;
    let autoResendRemainingSeconds = AUTO_RESEND_SECONDS;
    let autoResendUsed = false;
    let verifiedEmail = "";

    function getCsrfHeaders() {
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');

        const headers = {
            "Content-Type": "application/x-www-form-urlencoded"
        };

        if (tokenMeta && headerMeta && tokenMeta.content && headerMeta.content) {
            headers[headerMeta.content] = tokenMeta.content;
        }

        return headers;
    }

    function isValidGmail(email) {
        return /^[A-Za-z0-9._%+-]+@gmail\.com$/i.test(email);
    }

    function setEmailMessage(message, isSuccess) {
        if (!emailVerifyMessage) return;

        emailVerifyMessage.textContent = message;
        emailVerifyMessage.classList.toggle("success", !!isSuccess);
    }

    function setOtpMessage(message, isSuccess) {
        if (!otpMessage) return;

        otpMessage.textContent = message;
        otpMessage.classList.toggle("success", !!isSuccess);
    }

    function lockConfirmButton() {
        if (confirmBookingBtn) {
            confirmBookingBtn.disabled = true;
        }
    }

    function unlockConfirmButton() {
        if (confirmBookingBtn) {
            confirmBookingBtn.disabled = false;
        }
    }

    function setSendButtonsDisabled(disabled) {
        if (sendOtpBtn) {
            sendOtpBtn.disabled = disabled;
        }

        if (resendOtpBtn) {
            resendOtpBtn.disabled = disabled;
        }
    }

    function resetSendButtonText() {
        if (sendOtpBtn) {
            sendOtpBtn.textContent = "Gửi OTP";
        }

        if (resendOtpBtn) {
            resendOtpBtn.textContent = "Gửi lại mã OTP";
        }
    }

    function openOtpModal() {
        if (!otpModal) return;

        otpModal.classList.add("is-open");

        if (otpInput) {
            otpInput.value = "";

            setTimeout(function () {
                otpInput.focus();
            }, 100);
        }

        if (otpMessage) {
            otpMessage.textContent = "";
            otpMessage.classList.remove("success");
        }
    }

    function closeOtpModal() {
        if (!otpModal) return;

        otpModal.classList.remove("is-open");

        // Nếu khách đóng popup thì không tự gửi lại OTP nữa
        stopAutoResendCountdown();
        resetSendButtonText();
        setSendButtonsDisabled(false);
    }

    function updateOtpCountdownText() {
        if (!otpCountdownBox || !otpCountdownText) {
            return;
        }

        const label = otpCountdownBox.querySelector("span");

        if (!label) {
            return;
        }

        if (autoResendRemainingSeconds > 0) {
            otpCountdownBox.classList.remove("is-done");
            label.textContent = "Mã OTP sẽ tự động được gửi lại sau";
            otpCountdownText.textContent = autoResendRemainingSeconds + "s";

            if (resendOtpBtn) {
                resendOtpBtn.disabled = true;
                resendOtpBtn.textContent = "Gửi lại sau " + autoResendRemainingSeconds + "s";
            }
        } else {
            otpCountdownBox.classList.add("is-done");
            label.textContent = "Mã OTP mới đã được gửi lại";
            otpCountdownText.textContent = "Đã gửi";

            if (resendOtpBtn) {
                resendOtpBtn.disabled = false;
                resendOtpBtn.textContent = "Gửi lại mã OTP";
            }
        }
    }

    function startAutoResendCountdown() {
        if (autoResendUsed) {
            return;
        }

        autoResendRemainingSeconds = AUTO_RESEND_SECONDS;
        updateOtpCountdownText();

        if (autoResendTimer) {
            clearInterval(autoResendTimer);
        }

        autoResendTimer = setInterval(function () {
            autoResendRemainingSeconds--;
            updateOtpCountdownText();

            if (autoResendRemainingSeconds <= 0) {
                clearInterval(autoResendTimer);
                autoResendTimer = null;

                autoResendUsed = true;

                // Tự động gửi lại OTP đúng 1 lần sau 30 giây
                sendOtp(true);
            }
        }, 1000);
    }

    function stopAutoResendCountdown() {
        if (autoResendTimer) {
            clearInterval(autoResendTimer);
            autoResendTimer = null;
        }
    }

    async function sendOtp(isAutoResend) {
        const email = guestEmailInput
            ? guestEmailInput.value.trim().toLowerCase()
            : "";

        lockConfirmButton();
        verifiedEmail = "";

        if (!email) {
            setEmailMessage("Vui lòng nhập email trước khi gửi OTP.", false);
            return;
        }

        if (!isValidGmail(email)) {
            setEmailMessage("Email phải đúng định dạng Gmail, ví dụ: example@gmail.com.", false);
            return;
        }

        if (sendOtpBtn) {
            sendOtpBtn.disabled = true;
            sendOtpBtn.textContent = "Đang gửi...";
        }

        if (resendOtpBtn) {
            resendOtpBtn.disabled = true;
        }

        try {
            const response = await fetch("/page/booking/send-email-otp", {
                method: "POST",
                headers: getCsrfHeaders(),
                credentials: "same-origin",
                body: new URLSearchParams({
                    email: email
                })
            });

            const data = await response.json();

            if (!response.ok || !data.success) {
                const message = data.message || "Không thể gửi mã OTP.";

                if (isAutoResend) {
                    setOtpMessage(message, false);
                } else {
                    setEmailMessage(message, false);
                }

                if (data.remainingSeconds) {
                    autoResendRemainingSeconds = Number(data.remainingSeconds);
                    updateOtpCountdownText();
                }

                return;
            }

            if (isAutoResend) {
                setEmailMessage("Mã OTP mới đã được tự động gửi lại về email.", true);
                setOtpMessage("Mã OTP mới đã được gửi lại. Vui lòng kiểm tra email.", true);

                autoResendRemainingSeconds = 0;
                updateOtpCountdownText();
                resetSendButtonText();

                if (sendOtpBtn) {
                    sendOtpBtn.disabled = false;
                }

                if (resendOtpBtn) {
                    resendOtpBtn.disabled = false;
                }
            } else {
                setEmailMessage(data.message || "Mã OTP đã được gửi về email.", true);

                openOtpModal();
                startAutoResendCountdown();

                if (sendOtpBtn) {
                    sendOtpBtn.disabled = false;
                    sendOtpBtn.textContent = "Gửi OTP";
                }
            }

        } catch (error) {
            const message = "Không thể gửi OTP. Vui lòng kiểm tra cấu hình email hoặc thử lại.";

            if (isAutoResend) {
                setOtpMessage(message, false);
            } else {
                setEmailMessage(message, false);
            }

            resetSendButtonText();
            setSendButtonsDisabled(false);
        }
    }

    async function verifyOtp() {
        const email = guestEmailInput
            ? guestEmailInput.value.trim().toLowerCase()
            : "";

        const otp = otpInput
            ? otpInput.value.trim()
            : "";

        if (!email) {
            setOtpMessage("Vui lòng nhập email.", false);
            return;
        }

        if (!otp || otp.length !== 6) {
            setOtpMessage("Vui lòng nhập mã OTP gồm 6 chữ số.", false);
            return;
        }

        if (verifyOtpBtn) {
            verifyOtpBtn.disabled = true;
            verifyOtpBtn.textContent = "Đang xác thực...";
        }

        try {
            const response = await fetch("/page/booking/verify-email-otp", {
                method: "POST",
                headers: getCsrfHeaders(),
                credentials: "same-origin",
                body: new URLSearchParams({
                    email: email,
                    otp: otp
                })
            });

            const data = await response.json();

            if (!response.ok || !data.success) {
                setOtpMessage(data.message || "Mã OTP không chính xác.", false);
                lockConfirmButton();
                return;
            }

            verifiedEmail = email;

            setOtpMessage(data.message || "Xác thực email thành công.", true);
            setEmailMessage("Email đã được xác thực thành công.", true);

            unlockConfirmButton();
            stopAutoResendCountdown();

            resetSendButtonText();
            setSendButtonsDisabled(false);

            setTimeout(function () {
                if (otpModal) {
                    otpModal.classList.remove("is-open");
                }
            }, 700);

        } catch (error) {
            setOtpMessage("Không thể xác thực OTP. Vui lòng thử lại.", false);
            lockConfirmButton();
        } finally {
            if (verifyOtpBtn) {
                verifyOtpBtn.disabled = false;
                verifyOtpBtn.textContent = "Xác thực OTP";
            }
        }
    }

    if (sendOtpBtn) {
        sendOtpBtn.addEventListener("click", function () {
            autoResendUsed = false;
            stopAutoResendCountdown();
            sendOtp(false);
        });
    }

    if (resendOtpBtn) {
        resendOtpBtn.addEventListener("click", function () {
            autoResendUsed = true;
            stopAutoResendCountdown();
            sendOtp(true);
        });
    }

    if (verifyOtpBtn) {
        verifyOtpBtn.addEventListener("click", verifyOtp);
    }

    if (closeOtpModalBtn) {
        closeOtpModalBtn.addEventListener("click", closeOtpModal);
    }

    if (otpModal) {
        otpModal.addEventListener("click", function (event) {
            if (event.target.classList.contains("otp-modal-backdrop")) {
                closeOtpModal();
            }
        });
    }

    if (otpInput) {
        otpInput.addEventListener("input", function () {
            otpInput.value = otpInput.value.replace(/\D/g, "").slice(0, 6);
        });

        otpInput.addEventListener("keydown", function (event) {
            if (event.key === "Enter") {
                event.preventDefault();
                verifyOtp();
            }
        });
    }

    if (guestEmailInput) {
        guestEmailInput.addEventListener("input", function () {
            const currentEmail = guestEmailInput.value.trim().toLowerCase();

            if (currentEmail !== verifiedEmail) {
                verifiedEmail = "";
                lockConfirmButton();
                setEmailMessage("Email đã thay đổi. Vui lòng xác thực lại OTP.", false);

                stopAutoResendCountdown();
                autoResendUsed = false;
                resetSendButtonText();
                setSendButtonsDisabled(false);
            }
        });
    }

    lockConfirmButton();
});
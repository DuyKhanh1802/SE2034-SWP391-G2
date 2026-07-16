package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Guest;


import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Payment;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.BookingConfirmRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingCompleteResult;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingConfirmView;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.PaymentRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.OnlineBookingService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;


@Controller
@RequiredArgsConstructor
@RequestMapping("/page/booking")
public class VnPayBookingController {


    private final VnPayService vnPayService;
    private final OnlineBookingService onlineBookingService;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    @PostMapping("/payment/vnpay")
    public String createVnPayPayment(@ModelAttribute BookingConfirmRequest request,
                                     Model model,
                                     HttpSession session,
                                     HttpServletRequest httpRequest) {
        try {
            BookingCompleteResult result =
                    onlineBookingService.createPendingOnlineBookingForVnPay(request);


            Booking booking = bookingRepository
                    .findByBookingReference(result.getBookingReference())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking vừa tạo."));


            BigDecimal amount = booking.getGrandTotal();


            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                amount = booking.getTotalAmount();
            }


            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Tổng tiền thanh toán không hợp lệ.");
            }
            Payment pendingPayment = paymentRepository
                    .findFirstByBookingIdAndPaymentTypeAndStatus(
                            booking.getId(),
                            PaymentType.DEPOSIT,
                            PaymentStatus.PENDING
                    )
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch thanh toán đang chờ."));

            String txnRef = pendingPayment.getTransactionRef();


            String paymentUrl = vnPayService.createPaymentUrl(
                    txnRef,
                    amount,
                    httpRequest
            );


            return "redirect:" + paymentUrl;


        } catch (IllegalArgumentException e) {
            BookingConfirmView confirmView =
                    onlineBookingService.prepareConfirmView(request);


            model.addAttribute("request", request);
            model.addAttribute("confirmView", confirmView);
            model.addAttribute("errorMessage", e.getMessage());


            return "guest/BookingConfirm";
        }
    }


    @GetMapping("/vnpay-return")
    public String vnpayReturn(@RequestParam Map<String, String> params,
                              Model model,
                              RedirectAttributes redirectAttributes) {


        boolean validSignature = vnPayService.verifyReturnData(params);


        if (!validSignature) {
            model.addAttribute("success", false);
            model.addAttribute("message", "Chữ ký VNPay trả về không hợp lệ.");
            return "guest/payment-result";
        }


        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String transactionNo = params.get("vnp_TransactionNo");


        boolean paidSuccess = "00".equals(responseCode)
                && "00".equals(transactionStatus);

        Payment payment = paymentRepository.findByTransactionRef(txnRef).orElse(null);
        if (payment == null) {
            model.addAttribute("success", false);
            model.addAttribute("message", "Không tìm thấy giao dịch thanh toán trong hệ thống.");
            model.addAttribute("txnRef", txnRef);
            return "guest/payment-result";
        }

        if (!isReturnedAmountValid(payment, params.get("vnp_Amount"))) {
            model.addAttribute("success", false);
            model.addAttribute("message", "Số tiền VNPay trả về không khớp với giao dịch trong hệ thống.");
            model.addAttribute("txnRef", txnRef);
            return "guest/payment-result";
        }

        if (payment.getPaymentType() != PaymentType.DEPOSIT) {
            model.addAttribute("success", false);
            model.addAttribute("message", "Giao dịch này không thuộc luồng thanh toán đặt phòng qua VNPay.");
            model.addAttribute("txnRef", txnRef);
            return "guest/payment-result";
        }

        if (paidSuccess) {
            Booking booking =
                    onlineBookingService.confirmVnPayPaymentSuccess(
                            txnRef,
                            transactionNo
                    );


            redirectAttributes.addAttribute(
                    "bookingReference",
                    booking.getBookingReference()
            );


            return "redirect:/page/booking/success";
        }


        onlineBookingService.markVnPayPaymentFailed(txnRef);


        model.addAttribute("success", false);
        model.addAttribute("message", "Thanh toán không thành công. Mã lỗi: " + responseCode);
        model.addAttribute("txnRef", txnRef);


        return "guest/payment-result";
    }



    private boolean isReturnedAmountValid(Payment payment, String returnedAmount) {
        if (payment == null || payment.getAmount() == null || returnedAmount == null || returnedAmount.isBlank()) {
            return false;
        }

        try {
            BigDecimal expected = payment.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP);
            BigDecimal actual = new BigDecimal(returnedAmount).setScale(0, RoundingMode.HALF_UP);
            return expected.compareTo(actual) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }


}


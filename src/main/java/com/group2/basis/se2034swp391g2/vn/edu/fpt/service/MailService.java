package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.BookingDetail;
import java.util.List;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendResetPasswordOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("ViHotel - Password Reset OTP");
            message.setText(
                    "Hello,\n\n" +
                            "We received a request to reset your ViHotel account password.\n\n" +
                            "Your OTP code is: " + otp + "\n\n" +
                            "This OTP will expire in 5 minutes.\n\n" +
                            "If you did not request this action, please ignore this email.\n\n" +
                            "ViHotel Team"
            );

            mailSender.send(message);

        } catch (MailException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send reset password OTP email: " + e.getMessage(), e);
        }
    }

    public void sendRoomCodeEmail(String toEmail,
                                  String guestName,
                                  String bookingReference,
                                  String roomNumber,
                                  String roomCode,
                                  String checkInDate,
                                  String checkOutDate) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("ViHotel Hanoi - Your Room Code");

            message.setText(
                    "Hello " + guestName + ",\n\n" +
                            "Welcome to ViHotel Hanoi.\n\n" +
                            "Your booking reference is: " + bookingReference + "\n" +
                            "Room Number: " + roomNumber + "\n" +
                            "Your Room Code: " + roomCode + "\n\n" +
                            "Check-in Date: " + checkInDate + "\n" +
                            "Check-out Date: " + checkOutDate + "\n\n" +
                            "Please keep this code private. You can use this room code to access the guest portal during your stay.\n\n" +
                            "ViHotel Hanoi Team"
            );

            mailSender.send(message);

        } catch (MailException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send room code email: " + e.getMessage(), e);
        }
    }

    public void sendRoomCodesEmail(String toEmail,
                                   String guestName,
                                   String bookingReference,
                                   String emailContent) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("ViHotel Hanoi - Your Room Access Code");
            message.setText(
                    "Hello " + guestName + ",\n\n" +
                            "Welcome to ViHotel Hanoi.\n\n" +
                            "Your booking reference is: " + bookingReference + "\n\n" +
                            emailContent + "\n\n" +
                            "Please keep your room code private. You can use it to access the guest portal during your stay.\n\n" +
                            "ViHotel Hanoi Team"
            );

            mailSender.send(message);

        } catch (MailException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send room code email: " + e.getMessage(), e);
        }
    }


    public void sendBookingPendingPaymentEmail(
            String toEmail,
            String guestName,
            String bookingReference,
            String checkInDate,
            String checkOutDate,
            Integer totalRooms,
            String totalAmount,
            String bankName,
            String accountNumber,
            String accountName,
            String transferContent
    ){
        try{
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("V'Hotel Hanoi - Đã tiếp nhận yêu cầu đặt phòng");
            message.setText(
                    "Xin chào " + guestName + ",\n\n" +
                            "Cảm ơn quý khách đã lựa chọn V'Hotel Hanoi.\n\n" +
                            "Chúng tôi đã tiếp nhận yêu cầu đặt phòng trực tuyến của quý khách.\n" +
                            "Hiện tại, đơn đặt phòng của quý khách đang chờ xác nhận thanh toán.\n\n" +

                            "Thông tin đặt phòng:\n" +
                            "Mã đặt phòng: " + bookingReference + "\n" +
                            "Ngày nhận phòng: " + checkInDate + "\n" +
                            "Ngày trả phòng: " + checkOutDate + "\n" +
                            "Tổng số phòng: " + totalRooms + "\n" +
                            "Số tiền cần chuyển khoản: " + totalAmount + "\n\n" +

                            "Thông tin chuyển khoản:\n" +
                            "Ngân hàng: " + bankName + "\n" +
                            "Số tài khoản: " + accountNumber + "\n" +
                            "Tên chủ tài khoản: " + accountName + "\n" +
                            "Nội dung chuyển khoản: " + transferContent + "\n\n" +

                            "Sau khi V'Hotel xác nhận giao dịch chuyển khoản thành công, " +
                            "trạng thái đặt phòng của quý khách sẽ được cập nhật thành ĐÃ XÁC NHẬN.\n\n" +

                            "Trân trọng,\n" +
                            "Đội ngũ V'Hotel Hanoi"
            );
            mailSender.send(message);
        } catch (MailException e){
            e.printStackTrace();
            throw new RuntimeException("Gửi email chờ xác nhận thanh toán thất bại: " + e.getMessage(), e);
        }
    }

    public void sendBookingConfirmedEmail(Booking booking, List<BookingDetail> details) {
        String to = booking.getGuestEmail();
        String subject = "[V'Hotel Hanoi] Xác nhận đặt phòng thành công";

        String guestName = booking.getGuestLastName() + " " + booking.getGuestFirstName();

        String roomInfo = details.stream()
                .map(detail -> {
                    String variantName = detail.getVariant() != null
                            ? detail.getVariant().getVariantName()
                            : "Hạng phòng";

                    String roomNumber = detail.getRoom() != null
                            ? detail.getRoom().getRoomNumber()
                            : "Chưa phân phòng";

                    return "- " + variantName + " - Phòng " + roomNumber;
                })
                .collect(java.util.stream.Collectors.joining("\n"));

        String body = """
            Xin chào %s,

            Đặt phòng của quý khách đã được V'Hotel Hanoi xác nhận.

            Mã đặt phòng: %s
            Ngày nhận phòng: %s
            Ngày trả phòng: %s

            Phòng đã phân:
            %s

            Số tiền đã thanh toán: %,d VND

            Thời gian nhận phòng dự kiến: từ 14:00.
            Mã phòng sẽ được gửi cho quý khách sau khi hoàn tất thủ tục check-in.

            Trân trọng,
            V'Hotel Hanoi
            """.formatted(
                guestName,
                booking.getBookingReference(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                roomInfo,
                booking.getDepositAmount() != null ? booking.getDepositAmount().longValue() : 0L
        );

        sendSimpleEmail(to, subject, body);
    }

    private void sendSimpleEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

        } catch (MailException e) {
            e.printStackTrace();
            throw new RuntimeException("Gửi email thất bại: " + e.getMessage(), e);
        }
    }
}
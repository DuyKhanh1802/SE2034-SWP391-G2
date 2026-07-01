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
            message.setSubject("V'Hotel Hanoi - Mã OTP đặt lại mật khẩu");

            message.setText(
                    "Xin chào,\n\n" +
                            "Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản V'Hotel của quý khách.\n\n" +
                            "Mã OTP của quý khách là: " + otp + "\n\n" +
                            "Mã OTP có hiệu lực trong vòng 5 phút.\n\n" +
                            "Nếu quý khách không thực hiện yêu cầu này, vui lòng bỏ qua email.\n\n" +
                            "Trân trọng,\n" +
                            "Đội ngũ V'Hotel Hanoi"
            );

            mailSender.send(message);

        } catch (MailException e) {
            e.printStackTrace();
            throw new RuntimeException("Gửi email OTP thất bại: " + e.getMessage(), e);
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
            message.setSubject("V'Hotel Hanoi - Mã truy cập phòng");

            message.setText(
                    "Xin chào " + guestName + ",\n\n" +
                            "Chào mừng quý khách đến với V'Hotel Hanoi.\n\n" +

                            "Mã đặt phòng: " + bookingReference + "\n" +
                            "Số phòng: " + roomNumber + "\n" +
                            "Mã phòng: " + roomCode + "\n\n" +

                            "Ngày nhận phòng: " + checkInDate + "\n" +
                            "Ngày trả phòng: " + checkOutDate + "\n\n" +

                            "Vui lòng giữ bí mật mã phòng. Quý khách có thể sử dụng mã này để truy cập cổng thông tin khách trong suốt thời gian lưu trú.\n\n" +

                            "Trân trọng,\n" +
                            "Đội ngũ V'Hotel Hanoi"
            );

            mailSender.send(message);

        } catch (MailException e) {
            e.printStackTrace();
            throw new RuntimeException("Gửi email mã phòng thất bại: " + e.getMessage(), e);
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
            message.setSubject("V'Hotel Hanoi - Thông tin truy cập phòng");

            message.setText(
                    "Xin chào " + guestName + ",\n\n" +
                            "Chào mừng quý khách đến với V'Hotel Hanoi.\n\n" +

                            "Mã đặt phòng của quý khách là: " + bookingReference + "\n\n" +

                            "Thông tin truy cập phòng của quý khách:\n\n" +

                            emailContent +

                            "\n" +
                            "Vui lòng giữ bí mật mã phòng của quý khách. " +
                            "Quý khách có thể sử dụng mã này để truy cập cổng thông tin khách trong suốt thời gian lưu trú.\n\n" +

                            "Nếu cần hỗ trợ, vui lòng liên hệ lễ tân của khách sạn bất cứ lúc nào.\n\n" +

                            "Trân trọng,\n" +
                            "Đội ngũ V'Hotel Hanoi"
            );

            mailSender.send(message);

        } catch (MailException e) {
            e.printStackTrace();
            throw new RuntimeException("Gửi email mã phòng thất bại: " + e.getMessage(), e);
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

                    Đặt phòng của quý khách đã được V'Hotel Hanoi xác nhận thành công.

                    Thông tin đặt phòng:

                    Mã đặt phòng: %s
                    Ngày nhận phòng: %s
                    Ngày trả phòng: %s

                    Phòng đã được phân:

                    %s

                    Số tiền đã thanh toán: %,d VND

                    Quý khách có thể làm thủ tục nhận phòng từ 14:00.
                    
                    Quý khách vui lòng hoàn tất thủ tục nhận phòng trước 18:00 cùng ngày.
                
                    Nếu quý khách chưa đến khách sạn hoặc không liên hệ với lễ tân trước 18:00, đặt phòng có thể được xử lý theo trạng thái khách không đến theo chính sách của khách sạn.

                    Sau khi hoàn tất thủ tục check-in, hệ thống sẽ tự động gửi mã phòng qua email để quý khách sử dụng trong suốt thời gian lưu trú.

                    Xin cảm ơn quý khách đã lựa chọn V'Hotel Hanoi.

                    Trân trọng,

                    Đội ngũ V'Hotel Hanoi
                """.formatted(
                guestName,
                booking.getBookingReference(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                roomInfo,
                booking.getDepositAmount() != null
                        ? booking.getDepositAmount().longValue()
                        : 0L
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

    public void sendCheckInDeadlineReminderEmail(String toEmail,
                                                 String guestName,
                                                 String bookingReference,
                                                 String checkInDate) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("V'Hotel Hanoi - Nhắc nhở thời hạn nhận phòng");

            message.setText(
                    "Xin chào " + guestName + ",\n\n" +
                            "V'Hotel Hanoi xin nhắc quý khách về đặt phòng sắp quá hạn nhận phòng.\n\n" +

                            "Mã đặt phòng: " + bookingReference + "\n" +
                            "Ngày nhận phòng: " + checkInDate + "\n\n" +

                            "Theo chính sách của khách sạn, quý khách vui lòng hoàn tất thủ tục nhận phòng trước 18:00 cùng ngày.\n\n" +

                            "Nếu quý khách chưa đến khách sạn hoặc không liên hệ với lễ tân trước 18:00, " +
                            "đặt phòng có thể được xử lý theo trạng thái khách không đến.\n\n" +

                            "Nếu quý khách cần hỗ trợ hoặc đến muộn, vui lòng liên hệ lễ tân để được hỗ trợ.\n\n" +

                            "Trân trọng,\n" +
                            "Đội ngũ V'Hotel Hanoi"
            );

            mailSender.send(message);

        } catch (MailException e) {
            e.printStackTrace();
            throw new RuntimeException("Gửi email nhắc nhận phòng thất bại: " + e.getMessage(), e);
        }
    }
}
package com.group2.basis.se2034swp391g2.vn.edu.fpt.service.scheduler;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.BookingService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CheckInReminderScheduler {

    private final BookingService bookingService;
    private final MailService mailService;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Scheduled(cron = "0 0 17 * * *", zone = "Asia/Ho_Chi_Minh")
    //@Scheduled(cron = "0 * * * * *", zone = "Asia/Ho_Chi_Minh")  sửa thành cái này để test gửi mỗi phút
    public void sendCheckInDeadlineReminder() {
        List<Booking> bookings = bookingService.getTodayConfirmedBookingsForReminder();

        for (Booking booking : bookings) {
            if (booking.getGuestEmail() == null || booking.getGuestEmail().isBlank()) {
                continue;
            }

            String guestName = (booking.getGuestLastName() + " " + booking.getGuestFirstName()).trim();

            mailService.sendCheckInDeadlineReminderEmail(
                    booking.getGuestEmail(),
                    guestName,
                    booking.getBookingReference(),
                    booking.getCheckInDate().format(DATE_FORMAT)
            );
        }
    }
}
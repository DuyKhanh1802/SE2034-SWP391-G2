package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.BookingDetail;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Room;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomTypeVariant;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.GuestMyBookingView;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingDetailRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingRepository;
import org.springframework.stereotype.Service;
import lombok.*;

@Service
@RequiredArgsConstructor
public class GuestBookingService {

    private final BookingDetailRepository bookingDetailRepository;

    public GuestMyBookingView getMyBooking(Long bookingDetailId){
        BookingDetail detail = bookingDetailRepository.findById(bookingDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin phòng"));

        Booking booking = detail.getBooking();

        if(booking == null || Boolean.TRUE.equals(booking.getIsDeleted())){
            throw new IllegalArgumentException("Không tìm thấy booking");
        }
        GuestMyBookingView view = new GuestMyBookingView();

        view.setBookingId(booking.getId());
        view.setBookingDetailId(detail.getId());

        view.setBookingReference(booking.getBookingReference());
        view.setBookingStatus(booking.getStatus() == null ? null :  booking.getStatus().name());
        view.setDepositStatus(booking.getDepositStatus() == null ? null : booking.getDepositStatus().name());

        String guestName = ((booking.getGuestFirstName() == null ? "" : booking.getGuestFirstName())
                             + " "
                             +(booking.getGuestLastName() == null ? "" : booking.getGuestLastName().trim())
                             );
        view.setGuestName(guestName);
        view.setEmail(booking.getGuestEmail());
        view.setPhone(booking.getGuestPhone());

        view.setRoomCode(detail.getRoomCode());

        Room room = detail.getRoom();

        if(room != null){
            view.setRoomNumber(room.getRoomNumber());
        }

        RoomTypeVariant variant = detail.getVariant();
        if(variant != null){
            view.setVariantName(variant.getVariantName());

            if(variant.getRoomType() != null){
                view.setRoomTypeName(variant.getRoomType().getName());
            }
        }

        view.setCheckInDate(detail.getCheckInDate());
        view.setCheckOutDate(detail.getCheckOutDate());
        view.setNumNights(detail.getNumNights());

        view.setNumAdults(detail.getNumAdults());
        view.setNumChildren(detail.getNumChildren());

        view.setPricePerNight(detail.getPricePerNight());
        view.setRoomSubtotal(detail.getSubtotal());
        view.setServiceChargeAmount(detail.getServiceChargeAmount());
        view.setVatAmount(detail.getVatAmount());
        view.setTotalAmount(detail.getTotalAmount());

        view.setServiceSummary(detail.getServiceSummary());
        view.setSpecialRequests(booking.getSpecialRequests());

        return view;
    }
}

package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.BookingDetailGuest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BookingDetailGuestRepository extends JpaRepository<BookingDetailGuest, Long> {
    @Query("""
            select guest
            from BookingDetailGuest guest
            join fetch guest.bookingDetail detail
            where detail.id in :bookingDetailIds
            order by detail.id asc, guest.isPrimary desc, guest.id asc
            """)
    List<BookingDetailGuest> findByBookingDetailIdIn(@Param("bookingDetailIds") Collection<Long> bookingDetailIds);

    List<BookingDetailGuest> findByBookingDetail_Booking_Id(Long bookingId);

    void deleteByBookingDetail_Booking_Id(Long bookingId);
}

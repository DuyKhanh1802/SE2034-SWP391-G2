package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.utils;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.BookingDetail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class BookingDiscountAllocator {

    private BookingDiscountAllocator() {
    }

    public static BigDecimal discountForDetail(Booking booking,
                                               BookingDetail targetDetail,
                                               List<BookingDetail> bookingDetails) {
        if (booking == null || targetDetail == null || bookingDetails == null || bookingDetails.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal remainingDiscount = money(booking.getDiscountAmount()).max(BigDecimal.ZERO);
        if (remainingDiscount.signum() == 0) {
            return BigDecimal.ZERO;
        }

        List<BookingDetail> orderedDetails = bookingDetails.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        BookingDetail::getId,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        for (BookingDetail detail : orderedDetails) {
            BigDecimal allocatedDiscount = remainingDiscount.min(
                    money(detail.getTotalAmount()).max(BigDecimal.ZERO)
            );
            if (sameDetail(detail, targetDetail)) {
                return allocatedDiscount;
            }
            remainingDiscount = remainingDiscount.subtract(allocatedDiscount);
            if (remainingDiscount.signum() <= 0) {
                return BigDecimal.ZERO;
            }
        }

        return BigDecimal.ZERO;
    }

    public static BigDecimal discountedTotal(Booking booking,
                                             BookingDetail detail,
                                             List<BookingDetail> bookingDetails) {
        return money(detail == null ? null : detail.getTotalAmount())
                .subtract(discountForDetail(booking, detail, bookingDetails))
                .max(BigDecimal.ZERO);
    }

    private static boolean sameDetail(BookingDetail left, BookingDetail right) {
        if (left == right) {
            return true;
        }
        return left.getId() != null && left.getId().equals(right.getId());
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(0, RoundingMode.HALF_UP);
    }
}

package com.roomreserved.service;

import com.roomreserved.model.Booking;
import com.roomreserved.payment.PayMongoCheckoutService;
import com.roomreserved.repo.BookingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BookingHoldExpiryJob {
    private final BookingRepository bookings;
    private final PayMongoCheckoutService payments;
    private final BookingService bookingService;

    public BookingHoldExpiryJob(BookingRepository bookings, PayMongoCheckoutService payments, BookingService bookingService) {
        this.bookings = bookings;
        this.payments = payments;
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${BOOKING_HOLD_SWEEP_MS:60000}")
    public void expireAbandonedHolds() {
        for (Booking booking : bookings.findExpiredPendingHolds(LocalDateTime.now())) {
            boolean sessionClosed = booking.getPaymongoCheckoutSessionId() == null
                    || payments.expireCheckoutSession(booking.getPaymongoCheckoutSessionId());
            if (sessionClosed) bookingService.expireHoldIfStillPending(booking.getId());
            // If PayMongo reports paid/processing or cannot be reached, keep the row for a later
            // retry. Expired holds no longer count against availability; webhook handling reconciles payment.
        }
    }

}

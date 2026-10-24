package com.roomreserved.payment;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PaymentWebController {
    private final PayMongoCheckoutService payments;
    public PaymentWebController(PayMongoCheckoutService payments) { this.payments = payments; }

    @PostMapping("/payments/checkout/{bookingId}")
    public String checkout(@PathVariable Long bookingId, Authentication auth, RedirectAttributes flash) {
        try {
            String url = payments.createOrReuseCheckout(bookingId, auth.getName());
            return "redirect:" + url;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            flash.addFlashAttribute("paymentError", ex.getMessage());
            return "redirect:/bookings";
        }
    }

    @GetMapping("/payments/return")
    public String returned(RedirectAttributes flash) {
        // A browser redirect is not proof of payment. The verified webhook updates the booking.
        flash.addFlashAttribute("paymentPendingVerification", true);
        return "redirect:/bookings";
    }
}

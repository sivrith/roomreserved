package com.roomreserved.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PayMongoWebhookController {
    private final PayMongoCheckoutService payments;
    public PayMongoWebhookController(PayMongoCheckoutService payments) { this.payments = payments; }

    @PostMapping(value = "/webhooks/paymongo", consumes = {"application/json", "application/*+json"})
    public ResponseEntity<Map<String, String>> receive(@RequestBody String rawBody,
            @RequestHeader(value = "Paymongo-Signature", required = false) String signature) {
        PayMongoCheckoutService.WebhookResult result = payments.processWebhook(rawBody, signature);
        return switch (result) {
            case PROCESSED, IGNORED_EVENT -> ResponseEntity.ok(Map.of("status", "received"));
            case PAID_AFTER_HOLD_EXPIRED -> ResponseEntity.ok(Map.of("status", "received", "note", "manual reconciliation required"));
            case NOT_CONFIGURED -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "webhook not configured"));
            case INVALID_SIGNATURE -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "invalid signature"));
            case MISSING_REFERENCE, NO_MATCH -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("status", "unmatched payment event"));
            case INVALID_PAYLOAD -> ResponseEntity.badRequest().body(Map.of("status", "invalid payload"));
        };
    }
}

package com.roomreserved.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomreserved.model.Booking;
import com.roomreserved.repo.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class PayMongoCheckoutService {
    private static final Logger log = LoggerFactory.getLogger(PayMongoCheckoutService.class);
    private final BookingRepository bookings;
    private final ObjectMapper mapper;
    private final String secretKey;
    private final String webhookSecret;
    private final boolean liveMode;
    private final String appBaseUrl;
    private final List<String> paymentMethods;
    private final RestClient restClient;

    public PayMongoCheckoutService(BookingRepository bookings, ObjectMapper mapper,
            @Value("${roomreserved.paymongo.secret-key:}") String secretKey,
            @Value("${roomreserved.paymongo.webhook-secret:}") String webhookSecret,
            @Value("${roomreserved.paymongo.live-mode:false}") boolean liveMode,
            @Value("${roomreserved.paymongo.app-base-url:http://localhost:8080}") String appBaseUrl,
            @Value("${roomreserved.paymongo.payment-method-types:qrph}") String methods,
            @Value("${roomreserved.paymongo.api-base-url:https://api.paymongo.com}") String apiBaseUrl) {
        this.bookings = bookings;
        this.mapper = mapper;
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        this.liveMode = liveMode;
        this.appBaseUrl = trimTrailingSlash(appBaseUrl);
        this.paymentMethods = Arrays.stream(methods.split(",")).map(String::trim).filter(s -> !s.isBlank()).distinct().toList();
        this.restClient = RestClient.builder().baseUrl(apiBaseUrl)
                .defaultHeaders(headers -> {
                    if (!this.secretKey.isBlank()) headers.setBasicAuth(this.secretKey, "");
                    headers.set(HttpHeaders.ACCEPT, "application/json");
                }).build();
    }

    /** Creates or reuses a hosted checkout session for a still-active booking hold. */
    @Transactional
    public String createOrReuseCheckout(Long bookingId, String signedInEmail) {
        requireConfiguredSecret();
        Booking booking = bookings.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found."));
        if (booking.getUser() == null || !booking.getUser().getEmail().equalsIgnoreCase(signedInEmail)) {
            throw new IllegalArgumentException("You cannot pay for this reservation.");
        }
        if ("CONFIRMED".equals(booking.getStatus()) && "PAID".equals(booking.getPaymentStatus())) {
            throw new IllegalArgumentException("This reservation has already been paid.");
        }
        if (!"PENDING".equals(booking.getStatus()) || booking.getHoldExpiresAt() == null || !booking.getHoldExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("This reservation hold has expired. Please select your dates and book again.");
        }
        if (booking.getPaymongoCheckoutSessionId() != null && booking.getPaymongoCheckoutUrl() != null
                && booking.getPaymongoCheckoutUrl().startsWith("https://checkout.paymongo.com/")) {
            return booking.getPaymongoCheckoutUrl();
        }
        if (paymentMethods.isEmpty()) throw new IllegalStateException("No PayMongo payment methods are configured.");

        int amountCentavos = booking.getTotalPrice().multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).intValueExact();
        if (amountCentavos < 100) throw new IllegalArgumentException("The payable amount must be at least PHP 1.00.");

        Map<String, Object> lineItem = new LinkedHashMap<>();
        lineItem.put("name", "Room reservation " + booking.getBookingReference());
        lineItem.put("amount", amountCentavos);
        lineItem.put("currency", "PHP");
        lineItem.put("quantity", 1);
        lineItem.put("description", booking.getRoom().getName() + " · " + booking.getCheckIn() + " to " + booking.getCheckOut());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("bookingReference", booking.getBookingReference());
        metadata.put("bookingId", String.valueOf(booking.getId()));

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("line_items", List.of(lineItem));
        attributes.put("payment_method_types", paymentMethods);
        attributes.put("success_url", appBaseUrl + "/payments/return?reference=" + booking.getBookingReference());
        attributes.put("cancel_url", appBaseUrl + "/bookings?paymentCancelled=true");
        attributes.put("reference_number", booking.getBookingReference());
        attributes.put("description", "RoomReserved booking " + booking.getBookingReference());
        attributes.put("send_email_receipt", true);
        attributes.put("show_line_items", true);
        attributes.put("metadata", metadata);
        if (booking.getUser() != null) {
            Map<String, Object> billing = new LinkedHashMap<>();
            if (booking.getUser().getFullName() != null) billing.put("name", booking.getUser().getFullName());
            if (booking.getUser().getEmail() != null) billing.put("email", booking.getUser().getEmail());
            if (!billing.isEmpty()) attributes.put("billing", billing);
        }
        Map<String, Object> request = Map.of("data", Map.of("attributes", attributes));

        try {
            JsonNode response = restClient.post().uri("/v2/checkout_sessions")
                    .header("Content-Type", "application/json")
                    .body(request).retrieve().body(JsonNode.class);
            JsonNode data = response == null ? null : response.path("data");
            String sessionId = data == null ? "" : data.path("id").asText("");
            String checkoutUrl = data == null ? "" : data.path("attributes").path("checkout_url").asText("");
            if (sessionId.isBlank() || !checkoutUrl.startsWith("https://checkout.paymongo.com/")) {
                throw new IllegalStateException("PayMongo did not return a valid hosted checkout session.");
            }
            booking.setPaymentProvider("PAYMONGO");
            booking.setPaymongoCheckoutSessionId(sessionId);
            booking.setPaymongoCheckoutUrl(checkoutUrl);
            booking.setPaymentStatus("AWAITING_PAYMENT");
            booking.setPaymentUpdatedAt(LocalDateTime.now());
            bookings.save(booking);
            return checkoutUrl;
        } catch (RestClientResponseException ex) {
            log.warn("PayMongo checkout creation failed with HTTP {} for booking {}", ex.getStatusCode().value(), booking.getBookingReference());
            throw new IllegalStateException("PayMongo could not start checkout. Check that your API key and selected payment methods are active in the PayMongo dashboard.");
        } catch (org.springframework.web.client.RestClientException ex) {
            log.warn("Could not reach PayMongo while creating checkout for booking {}", booking.getBookingReference());
            throw new IllegalStateException("Could not reach PayMongo. Please try again shortly.");
        }
    }

    /**
     * Attempts to close an abandoned hosted checkout before its inventory hold is released.
     * A false result means PayMongo says it may already be paid/processing; the sweep retries and
     * the webhook remains the source of truth.
     */
    public boolean expireCheckoutSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank() || secretKey.isBlank()) return false;
        try {
            restClient.post().uri("/v2/checkout_sessions/{id}/expire", sessionId)
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientResponseException ex) {
            // PayMongo returns 400 for paid, expired, or in-flight sessions. Retrieve the session
            // to distinguish an already-expired checkout from one that may still be processing.
            try {
                JsonNode current = restClient.get().uri("/v2/checkout_sessions/{id}", sessionId)
                        .retrieve().body(JsonNode.class);
                String status = current == null ? "" : current.path("data").path("attributes").path("status").asText("");
                if ("expired".equalsIgnoreCase(status)) return true;
                JsonNode existingPayments = current == null ? null : current.path("data").path("attributes").path("payments");
                if (existingPayments != null && existingPayments.isArray()) {
                    for (JsonNode payment : existingPayments) {
                        String paymentStatus = payment.path("attributes").path("status").asText("");
                        if ("paid".equalsIgnoreCase(paymentStatus) || "processing".equalsIgnoreCase(paymentStatus)
                                || "pending".equalsIgnoreCase(paymentStatus)) return false;
                    }
                }
            } catch (org.springframework.web.client.RestClientException retrieveError) {
                log.info("Could not retrieve PayMongo checkout session {} after expire returned HTTP {}", sessionId, ex.getStatusCode().value());
            }
            log.info("PayMongo did not confirm checkout session {} as expired (HTTP {}); will retry/reconcile", sessionId, ex.getStatusCode().value());
            return false;
        } catch (org.springframework.web.client.RestClientException ex) {
            log.warn("Could not reach PayMongo to expire checkout session {}", sessionId);
            return false;
        }
    }

    /** Validates PayMongo's webhook signature and treats the webhook, not the return URL, as payment truth. */
    @Transactional
    public WebhookResult processWebhook(String rawBody, String signatureHeader) {
        if (webhookSecret.isBlank()) return WebhookResult.NOT_CONFIGURED;
        if (!validSignature(rawBody, signatureHeader)) return WebhookResult.INVALID_SIGNATURE;
        try {
            JsonNode root = mapper.readTree(rawBody);
            JsonNode eventData = root.path("data");
            String type = eventData.path("type").asText("");
            JsonNode resource = eventData.path("data");
            // PayMongo webhook payloads can wrap the event in data.attributes or data.type.
            if (!type.startsWith("checkout_session.")) {
                JsonNode eventAttributes = eventData.path("attributes");
                type = eventAttributes.path("type").asText(type);
                resource = eventAttributes.path("data");
            }
            if (!type.equals("checkout_session.payment.paid") && !type.equals("checkout_session.payment.failed")) {
                return WebhookResult.IGNORED_EVENT;
            }
            String sessionId = resource.path("id").asText("");
            JsonNode attrs = resource.path("attributes");
            String reference = attrs.path("reference_number").asText("");
            if (reference.isBlank()) reference = attrs.path("metadata").path("bookingReference").asText("");
            if (reference.isBlank() || sessionId.isBlank()) return WebhookResult.MISSING_REFERENCE;

            Booking booking = bookings.findByBookingReference(reference).orElse(null);
            if (booking == null || !sessionId.equals(booking.getPaymongoCheckoutSessionId())) return WebhookResult.NO_MATCH;
            if (type.equals("checkout_session.payment.failed")) {
                if (!"PAID".equals(booking.getPaymentStatus()) && !"PAID_REVIEW_REQUIRED".equals(booking.getPaymentStatus())) {
                    booking.setPaymentStatus("FAILED");
                    booking.setPaymentUpdatedAt(LocalDateTime.now());
                    bookings.save(booking);
                }
                return WebhookResult.PROCESSED;
            }

            // Validate the paid payment amount and currency against our server-calculated booking total.
            // Never trust a browser redirect or an amount supplied by the client.
            long expectedCentavos = booking.getTotalPrice().multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP).longValueExact();
            boolean matchingPaidPayment = false;
            JsonNode paymentsNode = attrs.path("payments");
            if (paymentsNode.isArray()) {
                for (JsonNode payment : paymentsNode) {
                    JsonNode paymentAttrs = payment.path("attributes");
                    if ("paid".equalsIgnoreCase(paymentAttrs.path("status").asText("") )
                            && "PHP".equalsIgnoreCase(paymentAttrs.path("currency").asText(""))
                            && paymentAttrs.path("amount").asLong(-1) == expectedCentavos) {
                        matchingPaidPayment = true;
                        break;
                    }
                }
            }
            if (!matchingPaidPayment) {
                log.error("PayMongo paid event did not contain a matching PHP payment amount for booking {}", reference);
                return WebhookResult.INVALID_PAYLOAD;
            }

            // Payment has arrived, but an expired hold must never silently become a confirmed booking.
            if (!"PENDING".equals(booking.getStatus()) || booking.getHoldExpiresAt() == null
                    || !booking.getHoldExpiresAt().isAfter(LocalDateTime.now())) {
                booking.setPaymentStatus("PAID_REVIEW_REQUIRED");
                booking.setPaymentUpdatedAt(LocalDateTime.now());
                bookings.save(booking);
                log.error("PayMongo reports paid checkout for expired/non-pending booking {}; manual reconciliation/refund required", reference);
                return WebhookResult.PAID_AFTER_HOLD_EXPIRED;
            }
            if (!"PAID".equals(booking.getPaymentStatus())) {
                booking.setPaymentStatus("PAID");
                booking.setStatus("CONFIRMED");
                booking.setHoldExpiresAt(null);
                booking.setPaymongoPaymentIntentId(attrs.path("payment_intent").path("id").asText(null));
                booking.setPaymentUpdatedAt(LocalDateTime.now());
                bookings.save(booking);
            }
            return WebhookResult.PROCESSED;
        } catch (Exception ex) {
            log.warn("Unable to process PayMongo webhook payload", ex);
            return WebhookResult.INVALID_PAYLOAD;
        }
    }

    private boolean validSignature(String rawBody, String header) {
        if (rawBody == null || header == null || header.isBlank()) return false;
        Map<String, String> parts = new HashMap<>();
        for (String part : header.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2) parts.put(pair[0].trim(), pair[1].trim());
        }
        String timestamp = parts.get("t");
        String supplied = parts.get(liveMode ? "li" : "te");
        if (timestamp == null || supplied == null || supplied.isBlank()) return false;
        try {
            long epoch = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - epoch) > 600) return false;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((timestamp + "." + rawBody).getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(digest);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), supplied.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
        } catch (Exception ex) {
            return false;
        }
    }

    private void requireConfiguredSecret() {
        if (secretKey.isBlank() || secretKey.equals("sk_test_replace_me") || secretKey.equals("sk_live_replace_me")) {
            throw new IllegalStateException("PayMongo is not configured. Set PAYMONGO_SECRET_KEY on the server first.");
        }
        if (liveMode && !secretKey.startsWith("sk_live_")) throw new IllegalStateException("Live mode requires a live PayMongo secret key.");
        if (!liveMode && !secretKey.startsWith("sk_test_")) throw new IllegalStateException("Test mode requires a test PayMongo secret key.");
        if (webhookSecret.isBlank()) throw new IllegalStateException("Set PAYMONGO_WEBHOOK_SECRET before enabling checkout so payment confirmations can be verified.");
        if (liveMode && !appBaseUrl.startsWith("https://")) throw new IllegalStateException("Live PayMongo checkout requires APP_BASE_URL to use public HTTPS.");
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "http://localhost:8080";
        return value.replaceAll("/+$", "");
    }

    public enum WebhookResult { PROCESSED, IGNORED_EVENT, INVALID_SIGNATURE, NOT_CONFIGURED, MISSING_REFERENCE, NO_MATCH, PAID_AFTER_HOLD_EXPIRED, INVALID_PAYLOAD }
}

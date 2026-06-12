package com.taskflow.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.taskflow.config.RazorpayConfig;
import com.taskflow.dto.request.CreateOrderRequest;
import com.taskflow.dto.request.VerifyPaymentRequest;
import com.taskflow.dto.response.OrderResponse;
import com.taskflow.dto.response.PaymentResponse;
import com.taskflow.entity.*;
import com.taskflow.exception.PaymentException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.PaymentRepository;
import com.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================
 * PAYMENT SERVICE — Razorpay integration for subscriptions.
 * ============================================================
 *
 * THE COMPLETE PAYMENT FLOW:
 *
 *   STEP 1: createOrder()
 *     User clicks "Upgrade to Pro"
 *     Frontend → POST /api/payments/create-order { plan: "PRO" }
 *     Backend calls Razorpay API → gets orderId (order_abc123)
 *     Backend saves Payment(status=CREATED) to DB
 *     Returns orderId + keyId to frontend
 *
 *   STEP 2: User completes payment (in Razorpay modal on frontend)
 *     Razorpay processes card/UPI/netbanking
 *     On success, frontend gets: razorpay_order_id, razorpay_payment_id, razorpay_signature
 *
 *   STEP 3: verifyPayment()
 *     Frontend → POST /api/payments/verify { orderId, paymentId, signature }
 *     Backend computes EXPECTED signature using HMAC-SHA256
 *     Compares computed vs received signature
 *     If match → payment is genuine → upgrade user's subscription
 *     If mismatch → payment tampered → mark FAILED, throw exception
 *
 *   STEP 4: (Optional) handleWebhook()
 *     Razorpay also sends a POST to your webhook URL after payment
 *     This is a backup confirmation channel (in case frontend crashes after payment)
 *     Webhooks are signed with a separate webhook_secret
 *
 * WHAT IS HMAC-SHA256?
 *   HMAC = Hash-based Message Authentication Code
 *   SHA256 = the hash algorithm used (produces 256-bit / 32-byte hash)
 *
 *   Think of it like a tamper-proof seal:
 *   - Razorpay knows your secret key
 *   - For each payment, Razorpay computes: HMAC(orderId + "|" + paymentId, your_secret)
 *   - Sends the result as razorpay_signature
 *   - Your backend computes the SAME thing
 *   - If they match → response genuinely came from Razorpay (only Razorpay knows your secret)
 *   - If they don't match → response was tampered with by a hacker
 *
 *   Used by: Stripe (stripe-signature header), Shopify webhooks, GitHub webhooks,
 *   Slack event subscriptions — all use HMAC to prove requests are authentic.
 *
 * WHY amountInPaise (Long) NOT amountInRupees (Double)?
 *   Floating point math is WRONG for money:
 *     0.1 + 0.2 = 0.30000000000000004 in Java (IEEE 754 floating point)
 *   Storing as Long (integer in smallest currency unit) is exact.
 *   Industry standard: Stripe uses cents, Razorpay uses paise.
 *   In code: Rs.499 = 49900L, Rs.999 = 99900L, Rs.2999 = 299900L
 *
 * INTERVIEW Q: How does payment verification work?
 * A: Client sends orderId + paymentId + signature. Backend recomputes
 *    HMAC(orderId + "|" + paymentId, key_secret) and compares with received signature.
 *    Match = legitimate Razorpay response. Mismatch = tampered/fraudulent.
 * ============================================================
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    // Prices in paise (Rs.1 = 100 paise)
    // Map.of() creates an immutable map — cannot be accidentally modified at runtime
    private static final Map<SubscriptionPlan, Long> PLAN_PRICES = Map.of(
            SubscriptionPlan.BASIC,      49900L,  // Rs.499
            SubscriptionPlan.PRO,        99900L,  // Rs.999
            SubscriptionPlan.ENTERPRISE, 299900L  // Rs.2999
    );

    private static final Map<SubscriptionPlan, String> PLAN_DISPLAY_NAMES = Map.of(
            SubscriptionPlan.FREE,       "Free Plan",
            SubscriptionPlan.BASIC,      "Basic Plan",
            SubscriptionPlan.PRO,        "Pro Plan",
            SubscriptionPlan.ENTERPRISE, "Enterprise Plan"
    );

    /**
     * STEP 1: Create a Razorpay order.
     *
     * This does NOT charge the user yet. It just reserves an order ID with Razorpay.
     * The actual payment happens in the frontend's Razorpay modal.
     *
     * Razorpay requires amount in paise, currency, and a receipt ID (your internal reference).
     */
    public OrderResponse createOrder(CreateOrderRequest request, User currentUser) {
        if (request.getPlan() == SubscriptionPlan.FREE) {
            throw new PaymentException("Cannot create a payment order for the FREE plan");
        }

        Long amountInPaise = PLAN_PRICES.get(request.getPlan());

        try {
            // Build the Razorpay order request using their JSONObject API
            JSONObject orderOptions = new JSONObject();
            orderOptions.put("amount", amountInPaise);       // in paise
            orderOptions.put("currency", "INR");
            // Receipt = YOUR internal reference ID (shown on Razorpay dashboard)
            orderOptions.put("receipt", "rcpt_" + currentUser.getId() + "_" + System.currentTimeMillis());
            // Notes are metadata visible in Razorpay dashboard (not required)
            JSONObject notes = new JSONObject();
            notes.put("userId", currentUser.getId());
            notes.put("plan", request.getPlan().name());
            orderOptions.put("notes", notes);

            // Calls Razorpay API → returns order object with an "id" field
            Order razorpayOrder = razorpayClient.orders.create(orderOptions);
            String razorpayOrderId = razorpayOrder.get("id");

            log.info("Razorpay order created: {} for user {} plan {}",
                    razorpayOrderId, currentUser.getEmail(), request.getPlan());

            // Save the pending payment to OUR database
            Payment payment = Payment.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .amountInPaise(amountInPaise)
                    .currency("INR")
                    .status(PaymentStatus.CREATED)
                    .plan(request.getPlan())
                    .user(currentUser)
                    .build();
            paymentRepository.save(payment);

            // Return everything the frontend needs to open the Razorpay modal
            return OrderResponse.builder()
                    .orderId(razorpayOrderId)
                    .amountInPaise(amountInPaise)
                    .amountInRupees(amountInPaise / 100.0)
                    .currency("INR")
                    .keyId(razorpayConfig.getKeyId()) // frontend needs this (NOT the secret)
                    .plan(request.getPlan())
                    .planDisplayName(PLAN_DISPLAY_NAMES.get(request.getPlan()))
                    .description("TaskFlow " + PLAN_DISPLAY_NAMES.get(request.getPlan()) + " - 1 month subscription")
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for user {}: {}", currentUser.getEmail(), e.getMessage());
            throw new PaymentException("Failed to create payment order. Please try again.", e);
        }
    }

    /**
     * STEP 3: Verify payment signature and activate subscription.
     *
     * The frontend sends the three values it received from Razorpay after payment.
     * We verify the HMAC signature to ensure the payment is genuine.
     *
     * SIGNATURE ALGORITHM:
     *   data      = razorpay_order_id + "|" + razorpay_payment_id
     *   expected  = HMAC_SHA256(data, key_secret)
     *   if expected == received_signature → GENUINE → activate subscription
     *   else → TAMPERED → mark FAILED → throw exception
     */
    public PaymentResponse verifyPayment(VerifyPaymentRequest request, User currentUser) {
        // Find the payment in our DB
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", request.getRazorpayOrderId()));

        // Verify this payment belongs to the current user
        if (!payment.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("This payment does not belong to your account");
        }

        // Prevent processing the same payment twice (idempotency check)
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.warn("Duplicate payment verification attempt for order {}", request.getRazorpayOrderId());
            return PaymentResponse.fromEntity(payment);
        }

        // Check for replay attacks — has this razorpay_payment_id been used before?
        if (paymentRepository.existsByRazorpayPaymentId(request.getRazorpayPaymentId())) {
            throw new PaymentException("Payment ID already used. Possible replay attack detected.");
        }

        // THE CRITICAL STEP: verify the HMAC signature
        boolean signatureValid = verifyHmacSignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!signatureValid) {
            // Mark as failed so we have a record of the tampered attempt
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.warn("Invalid payment signature for order {} — possible fraud attempt", request.getRazorpayOrderId());
            throw new PaymentException("Payment verification failed: invalid signature. This incident has been logged.");
        }

        // Signature is valid — update payment record
        payment.setStatus(PaymentStatus.PAID);
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        paymentRepository.save(payment);

        // Upgrade the user's subscription plan
        currentUser.setSubscriptionPlan(payment.getPlan());
        currentUser.setSubscriptionExpiresAt(LocalDateTime.now().plusMonths(1));
        userRepository.save(currentUser);

        log.info("Payment verified. User {} upgraded to {} plan until {}",
                currentUser.getEmail(), payment.getPlan(), currentUser.getSubscriptionExpiresAt());

        return PaymentResponse.fromEntity(payment);
    }

    /**
     * WEBHOOK HANDLER: Called by Razorpay server directly when payment events happen.
     *
     * WHY WEBHOOKS?
     *   Consider this scenario: user pays, then their internet cuts out before
     *   the frontend can call verifyPayment(). Without webhooks, the payment is
     *   stuck in CREATED status forever and the subscription never activates.
     *
     *   Webhooks are Razorpay's way of saying "Hey, payment was successful" directly
     *   to your server — no frontend required. A backup confirmation channel.
     *
     *   IMPORTANT: This endpoint is PUBLIC (no JWT) because Razorpay calls it,
     *   not your users. We verify authenticity with the webhook_secret signature instead.
     *
     * WEBHOOK SIGNATURE VERIFICATION:
     *   Different from payment signature!
     *   expected = HMAC_SHA256(raw_request_body, webhook_secret)
     *   Compare with X-Razorpay-Signature header value.
     *
     * @param payload   Raw request body as String (must NOT be parsed before signature check)
     * @param signature X-Razorpay-Signature header value
     */
    public void handleWebhook(String payload, String signature) {
        // Verify webhook signature first
        boolean signatureValid = verifyWebhookSignature(payload, signature);
        if (!signatureValid) {
            log.warn("Invalid Razorpay webhook signature received");
            throw new PaymentException("Invalid webhook signature");
        }

        // Parse the payload
        JSONObject webhookData = new JSONObject(payload);
        String event = webhookData.getString("event");
        log.info("Razorpay webhook received: {}", event);

        // Handle payment captured event
        if ("payment.captured".equals(event)) {
            JSONObject paymentEntity = webhookData
                    .getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String orderId = paymentEntity.getString("order_id");
            String paymentId = paymentEntity.getString("id");

            // Update payment status if not already paid (prevents double-processing)
            paymentRepository.findByRazorpayOrderId(orderId).ifPresent(payment -> {
                if (payment.getStatus() != PaymentStatus.PAID) {
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setRazorpayPaymentId(paymentId);
                    paymentRepository.save(payment);

                    // Activate subscription for the user
                    User user = payment.getUser();
                    user.setSubscriptionPlan(payment.getPlan());
                    user.setSubscriptionExpiresAt(LocalDateTime.now().plusMonths(1));
                    userRepository.save(user);

                    log.info("Webhook: subscription activated for {} via order {}", user.getEmail(), orderId);
                }
            });
        }

        // payment.failed event
        if ("payment.failed".equals(event)) {
            JSONObject paymentEntity = webhookData
                    .getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");
            String orderId = paymentEntity.getString("order_id");
            paymentRepository.findByRazorpayOrderId(orderId).ifPresent(payment -> {
                if (payment.getStatus() == PaymentStatus.CREATED) {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                    log.info("Webhook: payment failed for order {}", orderId);
                }
            });
        }
    }

    /**
     * GET /api/payments/history — user's own payment records.
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(User currentUser) {
        return paymentRepository.findByUserOrderByCreatedAtDesc(currentUser)
                .stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ============================================================
    // PRIVATE HELPERS — HMAC SIGNATURE VERIFICATION
    // ============================================================

    /**
     * Verifies Razorpay payment signature.
     *
     * Algorithm:
     *   data     = razorpay_order_id + "|" + razorpay_payment_id
     *   expected = HMAC_SHA256(data, key_secret)
     *   return expected.equals(receivedSignature)
     *
     * Mac = Message Authentication Code
     * HmacSHA256 = HMAC using SHA-256 hash
     * SecretKeySpec = wraps the raw key bytes into a JCE Key object
     */
    private boolean verifyHmacSignature(String orderId, String paymentId, String receivedSignature) {
        try {
            String data = orderId + "|" + paymentId;
            String expected = computeHmac(data, razorpayConfig.getKeySecret());
            return expected.equals(receivedSignature);
        } catch (Exception e) {
            log.error("HMAC verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifies Razorpay webhook signature.
     *
     * Uses webhook_secret (different from key_secret).
     * Algorithm is the same HMAC-SHA256, but the key is the webhook secret.
     */
    private boolean verifyWebhookSignature(String payload, String receivedSignature) {
        try {
            String expected = computeHmac(payload, razorpayConfig.getWebhookSecret());
            return expected.equals(receivedSignature);
        } catch (Exception e) {
            log.error("Webhook HMAC verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Core HMAC-SHA256 computation.
     *
     * Returns the hex-encoded HMAC of the given data using the given key.
     *
     * Steps:
     *   1. Get a Mac instance for "HmacSHA256"
     *   2. Initialize it with the secret key
     *   3. Feed in the data bytes
     *   4. Get the 32-byte hash result
     *   5. Convert each byte to 2-character hex (e.g., byte 255 -> "ff")
     *
     * throws NoSuchAlgorithmException if "HmacSHA256" isn't in the JDK (never happens in Java 8+)
     * throws InvalidKeyException if the key is malformed
     */
    private String computeHmac(String data, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Convert byte array to lowercase hex string
        // String.format("%02x", b) = always 2 hex chars (e.g., byte 7 → "07", not "7")
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}

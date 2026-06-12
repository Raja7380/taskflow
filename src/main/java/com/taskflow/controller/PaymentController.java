package com.taskflow.controller;

import com.taskflow.dto.request.CreateOrderRequest;
import com.taskflow.dto.request.VerifyPaymentRequest;
import com.taskflow.dto.response.OrderResponse;
import com.taskflow.dto.response.PaymentResponse;
import com.taskflow.entity.User;
import com.taskflow.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PAYMENT CONTROLLER — Endpoints for Razorpay payment integration.
 *
 * FLOW SUMMARY:
 *   1. POST /api/payments/create-order  → get orderId from Razorpay
 *   2. [Frontend opens Razorpay modal]  → user pays
 *   3. POST /api/payments/verify        → verify HMAC, activate subscription
 *   4. GET  /api/payments/history       → see all payments
 *
 * All endpoints require authentication (@AuthenticationPrincipal).
 * The webhook endpoint is in WebhookController (public, no JWT).
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Razorpay payment integration for subscriptions")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments/create-order
     *
     * Creates a Razorpay order. Frontend uses the returned orderId
     * to open the Razorpay checkout modal.
     *
     * Request body: { "plan": "PRO" }
     *
     * Response: { "orderId": "order_abc123", "amountInPaise": 99900,
     *             "keyId": "rzp_test_...", "currency": "INR", ... }
     */
    @PostMapping("/create-order")
    @Operation(summary = "Create a Razorpay payment order for a subscription plan")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(paymentService.createOrder(request, currentUser));
    }

    /**
     * POST /api/payments/verify
     *
     * Verifies HMAC signature and activates the user's subscription.
     * Called by frontend AFTER user completes payment in Razorpay modal.
     *
     * Request body:
     * {
     *   "razorpayOrderId": "order_abc123",
     *   "razorpayPaymentId": "pay_xyz456",
     *   "razorpaySignature": "abc123def456..."  (HMAC-SHA256 hex string)
     * }
     */
    @PostMapping("/verify")
    @Operation(summary = "Verify Razorpay payment signature and activate subscription")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(paymentService.verifyPayment(request, currentUser));
    }

    /**
     * GET /api/payments/history
     *
     * Returns all payments made by the current user, newest first.
     * Includes both successful and failed payments.
     */
    @GetMapping("/history")
    @Operation(summary = "Get current user's payment history")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(paymentService.getMyPayments(currentUser));
    }
}

package com.taskflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * REQUEST DTO — Body for POST /api/payments/verify
 *
 * After the user completes payment in the Razorpay checkout modal,
 * Razorpay gives the frontend THREE values:
 *
 *   razorpay_order_id  — the order we created
 *   razorpay_payment_id — Razorpay's unique ID for this payment (pay_xxxx)
 *   razorpay_signature  — HMAC-SHA256 proof that this is a genuine Razorpay response
 *
 * The frontend sends all three here. Backend verifies the signature cryptographically.
 *
 * WHY VERIFY ON THE BACKEND?
 *   The frontend JavaScript could be tampered with by the user (browser DevTools).
 *   Never trust "I paid" from the frontend without backend verification.
 *   The HMAC signature can only be verified with your secret key (which only backend has).
 */
@Data
public class VerifyPaymentRequest {

    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;
}

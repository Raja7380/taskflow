package com.taskflow.dto.response;

import com.taskflow.entity.SubscriptionPlan;
import lombok.Builder;
import lombok.Data;

/**
 * RESPONSE DTO — Returned by POST /api/payments/create-order
 *
 * The frontend needs these values to initialize the Razorpay checkout modal:
 *
 * JavaScript example (what frontend does with this response):
 *
 *   const options = {
 *     key: response.keyId,           // Your Razorpay key ID
 *     amount: response.amountInPaise, // In paise
 *     currency: response.currency,
 *     order_id: response.orderId,    // MUST match what Razorpay generated
 *     handler: function(payment) {
 *       // Called when user completes payment
 *       verifyPayment(payment.razorpay_order_id,
 *                     payment.razorpay_payment_id,
 *                     payment.razorpay_signature)
 *     }
 *   }
 *   const rzp = new Razorpay(options)
 *   rzp.open()  // Opens the payment modal
 *
 * keyId is included here so the frontend doesn't need to hardcode it.
 * The key SECRET is never sent to the frontend — only the key ID.
 */
@Data
@Builder
public class OrderResponse {

    private String orderId;           // e.g. "order_abc123xyz"
    private Long amountInPaise;       // e.g. 99900 (Rs.999)
    private Double amountInRupees;    // e.g. 999.0 (for display only)
    private String currency;          // "INR"
    private String keyId;             // Your Razorpay Key ID (NOT the secret)
    private SubscriptionPlan plan;
    private String planDisplayName;   // "Pro Plan"
    private String description;       // "TaskFlow Pro - 1 month subscription"
}

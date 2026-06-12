package com.taskflow.dto.response;

import com.taskflow.entity.Payment;
import com.taskflow.entity.PaymentStatus;
import com.taskflow.entity.SubscriptionPlan;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RESPONSE DTO — Represents a single payment record.
 * Used by GET /api/payments/history and POST /api/payments/verify response.
 *
 * NOTE: razorpaySignature is intentionally excluded — it's an internal
 * verification artifact, not useful to the frontend.
 */
@Data
@Builder
public class PaymentResponse {

    private Long id;
    private String razorpayOrderId;
    private String razorpayPaymentId;   // null until payment is completed
    private Long amountInPaise;
    private Double amountInRupees;      // amountInPaise / 100.0 — for display
    private String currency;
    private PaymentStatus status;
    private SubscriptionPlan plan;
    private String planDisplayName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentResponse fromEntity(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .razorpayOrderId(p.getRazorpayOrderId())
                .razorpayPaymentId(p.getRazorpayPaymentId())
                .amountInPaise(p.getAmountInPaise())
                .amountInRupees(p.getAmountInPaise() / 100.0)
                .currency(p.getCurrency())
                .status(p.getStatus())
                .plan(p.getPlan())
                .planDisplayName(toPlanDisplayName(p.getPlan()))
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private static String toPlanDisplayName(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE -> "Free Plan";
            case BASIC -> "Basic Plan - Rs.499/month";
            case PRO -> "Pro Plan - Rs.999/month";
            case ENTERPRISE -> "Enterprise Plan - Rs.2999/month";
        };
    }
}

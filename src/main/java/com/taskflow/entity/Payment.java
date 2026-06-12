package com.taskflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * PAYMENT ENTITY — Records every payment transaction in the database.
 *
 * WHY STORE PAYMENT RECORDS IN YOUR OWN DB?
 *   Razorpay stores payments on their server, but you should ALSO store them in
 *   your own database for these reasons:
 *
 *   1. AUDIT TRAIL — who paid, when, for what plan, what amount
 *   2. USER HISTORY — "GET /api/payments/history" needs your own data
 *   3. INDEPENDENCE — if Razorpay API is down, you still know who has active subscriptions
 *   4. COMPLIANCE — financial records must be stored for 7+ years in most countries
 *   5. DISPUTE RESOLUTION — if user claims "I paid but plan didn't activate", you have proof
 *
 * WHY amountInPaise AND NOT amountInRupees?
 *   Razorpay (and most payment gateways worldwide) use the SMALLEST currency unit:
 *   - India: paise (Rs.1 = 100 paise) → Rs.499 stored as 49900
 *   - USA: cents ($1 = 100 cents) → $9.99 stored as 999
 *   - Japan: yen (no subdivision) → stored as-is
 *
 *   Why? Floating point math is INACCURATE for money:
 *     0.1 + 0.2 = 0.30000000000000004 (not 0.3) in Java float
 *   Storing as long integer eliminates this rounding error entirely.
 *   NEVER store money as double or float in a real system.
 *
 * WHY STORE razorpaySignature?
 *   The HMAC signature proves the payment was verified. Storing it creates a
 *   cryptographic record: "This payment was legitimately verified, here's proof."
 *
 * DENORMALIZED FIELDS:
 *   amountInPaise and currency are duplicated from Razorpay's records.
 *   This denormalization is intentional — if Razorpay's records change,
 *   your DB still has the original amount at the time of the transaction.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_user", columnList = "user_id"),
        @Index(name = "idx_payment_order", columnList = "razorpay_order_id", unique = true),
        @Index(name = "idx_payment_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Razorpay order ID — created by Razorpay when we call orders.create()
    // Format: "order_xxxxxxxxxxxxxxxx"
    @Column(name = "razorpay_order_id", unique = true, nullable = false)
    private String razorpayOrderId;

    // Razorpay payment ID — set AFTER user completes payment
    // Format: "pay_xxxxxxxxxxxxxxxx"
    // null while status = CREATED or FAILED
    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    // HMAC-SHA256 signature — proof that payment was verified
    // null until verifyPayment() succeeds
    @Column(name = "razorpay_signature", length = 512)
    private String razorpaySignature;

    // Amount in paise — Rs.499 = 49900 paise
    // NEVER store money as float/double — integer arithmetic is exact
    @Column(nullable = false)
    private Long amountInPaise;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    // Which subscription plan this payment is for
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan plan;

    // The user who made this payment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

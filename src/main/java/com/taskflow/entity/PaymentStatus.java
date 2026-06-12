package com.taskflow.entity;

/**
 * PAYMENT STATUS — Lifecycle states of a payment.
 *
 * WHY A STATUS ENUM INSTEAD OF A BOOLEAN?
 *   A payment isn't just "paid or not paid". It goes through stages:
 *
 *   CREATED --> user initiates checkout
 *   PAID    --> payment captured by Razorpay, signature verified
 *   FAILED  --> payment failed or signature verification failed
 *   REFUNDED --> money sent back to user
 *
 *   A boolean (isPaid) can't represent FAILED or REFUNDED.
 *   An enum captures every meaningful state cleanly.
 *
 *   This is the same State Machine pattern from Session 3 (TaskStatus).
 *   Payment systems use it too -- the status of a Swiggy order, UPI transaction,
 *   or Amazon delivery all follow the same pattern.
 *
 * INTERVIEW Q: What payment states do you track?
 * A: At minimum: CREATED (order placed with gateway), PAID (captured),
 *    FAILED (declined/error), REFUNDED (money returned).
 *    Complex systems add: PENDING_VERIFICATION, PARTIALLY_REFUNDED, DISPUTED, CHARGEBACK.
 */
public enum PaymentStatus {
    CREATED,   // Razorpay order created, user hasn't paid yet
    PAID,      // Payment captured and signature verified — subscription active
    FAILED,    // Payment failed or HMAC signature mismatch
    REFUNDED   // Full refund issued to user
}

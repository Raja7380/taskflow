package com.taskflow.exception;

/**
 * PAYMENT EXCEPTION — Thrown when a payment operation fails.
 *
 * Examples of when this is thrown:
 *   - Razorpay API call to create an order fails
 *   - HMAC signature verification fails (payment tampered or wrong keys)
 *   - Trying to pay for the FREE plan
 *   - Payment already verified (duplicate request)
 *   - Webhook signature invalid
 *
 * Maps to HTTP 400 Bad Request via GlobalExceptionHandler.
 */
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}

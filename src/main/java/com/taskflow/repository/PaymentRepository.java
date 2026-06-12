package com.taskflow.repository;

import com.taskflow.entity.Payment;
import com.taskflow.entity.PaymentStatus;
import com.taskflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PAYMENT REPOSITORY — Data access for Payment records.
 *
 * Derived query methods Spring Data generates automatically:
 *
 * findByRazorpayOrderId     → SELECT * FROM payments WHERE razorpay_order_id = ?
 * findByUser                 → SELECT * FROM payments WHERE user_id = ?
 * findByUserAndStatus        → SELECT * FROM payments WHERE user_id = ? AND status = ?
 * existsByRazorpayPaymentId  → SELECT COUNT(*) > 0 WHERE razorpay_payment_id = ?
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Look up payment by the Razorpay order ID (received in verify-payment request)
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    // User's full payment history, newest first
    List<Payment> findByUserOrderByCreatedAtDesc(User user);

    // User's successful payments only
    List<Payment> findByUserAndStatus(User user, PaymentStatus status);

    // Check if a Razorpay payment ID has already been processed (prevent replay attacks)
    boolean existsByRazorpayPaymentId(String razorpayPaymentId);
}

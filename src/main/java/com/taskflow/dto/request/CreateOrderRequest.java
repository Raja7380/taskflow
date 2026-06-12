package com.taskflow.dto.request;

import com.taskflow.entity.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * REQUEST DTO — Body for POST /api/payments/create-order
 *
 * Frontend sends:
 * {
 *   "plan": "PRO"
 * }
 *
 * Backend creates a Razorpay order and returns the orderId + amount
 * that the frontend uses to open the Razorpay checkout modal.
 */
@Data
public class CreateOrderRequest {

    @NotNull(message = "Plan is required")
    private SubscriptionPlan plan;
}

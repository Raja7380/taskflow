package com.taskflow.controller;

import com.taskflow.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * WEBHOOK CONTROLLER — Receives event notifications from Razorpay.
 *
 * WHY IS THIS ENDPOINT PUBLIC (no JWT)?
 *   Webhooks are called by RAZORPAY'S SERVERS, not your users.
 *   Razorpay doesn't have a JWT token — it's an external service.
 *   Authentication is done via HMAC signature verification in PaymentService.
 *
 * WHAT IS A WEBHOOK?
 *   Normal API: YOUR app calls RAZORPAY's API ("did the payment succeed?")
 *   Webhook:    RAZORPAY calls YOUR API ("payment just succeeded!")
 *
 *   It's "reverse API" — the server pushes events to you instead of you polling.
 *
 *   Real industry examples:
 *   - Stripe webhooks: notify when a subscription renews
 *   - GitHub webhooks: notify when code is pushed (triggers CI/CD)
 *   - Slack webhooks: post a message to a channel when something happens
 *   - Twilio: notify when an SMS is delivered
 *   - Shopify: notify when a product is sold
 *
 * WEBHOOK SETUP ON RAZORPAY DASHBOARD:
 *   1. Go to Razorpay Dashboard → Settings → Webhooks
 *   2. Add webhook URL: https://yourapp.com/api/webhooks/razorpay
 *   3. Select events: payment.captured, payment.failed
 *   4. Copy the webhook secret → add to application.yml as razorpay.webhook-secret
 *
 * FOR LOCAL TESTING WITH NGROK:
 *   Your webhook URL must be publicly accessible. In development:
 *   1. Install ngrok: https://ngrok.com
 *   2. Run: ngrok http 8080
 *   3. Copy the public URL (e.g., https://abc123.ngrok.io)
 *   4. Use https://abc123.ngrok.io/api/webhooks/razorpay as webhook URL in Razorpay
 *
 * IMPORTANT — @RequestBody String payload (not @RequestBody JSONObject):
 *   We read the raw body as a String BEFORE parsing it.
 *   The HMAC signature is computed over the EXACT bytes Razorpay sent.
 *   If we let Spring parse it to JSON first, byte order/whitespace may change,
 *   making our computed signature differ from Razorpay's.
 *   Always verify webhook signatures on the raw body string.
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "External service event handlers (no JWT required)")
public class WebhookController {

    private final PaymentService paymentService;

    /**
     * POST /api/webhooks/razorpay
     *
     * Razorpay calls this endpoint when a payment event occurs.
     * This is a PUBLIC endpoint — no JWT required.
     * Authentication is done via X-Razorpay-Signature header verification.
     *
     * @param payload   Raw JSON body from Razorpay (String, not parsed)
     * @param signature X-Razorpay-Signature header — HMAC-SHA256 of the body
     */
    @PostMapping("/razorpay")
    @Operation(summary = "Razorpay webhook handler (called by Razorpay, not users)")
    public ResponseEntity<Void> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        log.info("Received Razorpay webhook");
        paymentService.handleWebhook(payload, signature);
        // Always return 200 quickly — Razorpay retries if it gets non-2xx or timeout
        return ResponseEntity.ok().build();
    }
}

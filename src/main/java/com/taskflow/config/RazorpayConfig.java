package com.taskflow.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAZORPAY CONFIGURATION — Sets up the Razorpay API client.
 *
 * WHY A @Configuration CLASS?
 *   RazorpayClient is a third-party object — not a Spring-managed class.
 *   We use @Configuration + @Bean to teach Spring how to create it and
 *   make it injectable everywhere via @Autowired / @RequiredArgsConstructor.
 *
 *   Same pattern used for: Redis clients, S3 clients, email senders, etc.
 *   Any third-party object you want Spring to manage goes here.
 *
 * HOW TO GET YOUR RAZORPAY KEYS:
 *   1. Sign up at https://razorpay.com
 *   2. Go to Settings -> API Keys
 *   3. Generate Test Keys (start with rzp_test_...)
 *   4. Add to application.yml or set as environment variables:
 *
 *      export RAZORPAY_KEY_ID=rzp_test_yourKeyId
 *      export RAZORPAY_KEY_SECRET=yourKeySecret
 *      export RAZORPAY_WEBHOOK_SECRET=yourWebhookSecret
 *
 * TEST vs LIVE MODE:
 *   Test keys (rzp_test_...): use Razorpay's test card numbers, no real money charged
 *   Live keys (rzp_live_...): real money transactions — only use in production
 *
 * @Value("${razorpay.key-id}"):
 *   Reads the value from application.yml:
 *     razorpay:
 *       key-id: rzp_test_abc123
 *   Or from environment variable RAZORPAY_KEY_ID (if using ${RAZORPAY_KEY_ID:default}).
 *
 * INTERVIEW Q: How do you avoid hardcoding secrets in code?
 * A: Use environment variables. In application.yml, use ${ENV_VAR_NAME}.
 *    In production (Docker/Kubernetes), inject secrets as env vars — never commit them to git.
 */
@Configuration
@Getter
public class RazorpayConfig {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    /**
     * Creates and registers a RazorpayClient Spring bean.
     *
     * RazorpayClient wraps all Razorpay API calls:
     *   razorpayClient.orders.create(...)   → create an order
     *   razorpayClient.payments.fetch(id)   → fetch payment details
     *   razorpayClient.refunds.create(...)  → issue a refund
     *
     * throws RazorpayException if keyId/keySecret are invalid.
     * Spring will fail on startup if this bean can't be created — fail fast is good.
     */
    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        return new RazorpayClient(keyId, keySecret);
    }
}

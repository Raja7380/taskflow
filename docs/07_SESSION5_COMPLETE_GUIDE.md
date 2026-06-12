# Session 5: Razorpay Payment Integration — Complete Learning Guide

---

## Table of Contents

1. [Why Every SaaS App Needs Payments](#1-why-every-saas-app-needs-payments)
2. [Payment Gateways — What They Are and Why You Need One](#2-payment-gateways--what-they-are-and-why-you-need-one)
3. [The Complete Razorpay Payment Flow](#3-the-complete-razorpay-payment-flow)
4. [HMAC-SHA256 — The Security Behind Payment Verification](#4-hmac-sha256--the-security-behind-payment-verification)
5. [Why Store Money as Long (Paise), Not Double (Rupees)?](#5-why-store-money-as-long-paise-not-double-rupees)
6. [Webhooks — The Backup Confirmation Channel](#6-webhooks--the-backup-confirmation-channel)
7. [Subscription Plans and Pricing Tiers](#7-subscription-plans-and-pricing-tiers)
8. [New Files Added in Session 5](#8-new-files-added-in-session-5)
9. [Database Changes](#9-database-changes)
10. [How to Set Up Razorpay Test Keys](#10-how-to-set-up-razorpay-test-keys)
11. [Testing With Swagger — Step by Step](#11-testing-with-swagger--step-by-step)
12. [Interview Q&A](#12-interview-qa)

---

## 1. Why Every SaaS App Needs Payments

**SaaS** = Software as a Service. You pay to use the software, not own it.

Every professional app you use charges money:
- Jira: $7.75/user/month
- Notion: $8/month  
- Slack: $7.25/user/month
- GitHub: $4/month (Pro)
- Zoom: $14.99/month

They all follow the same model: **Free tier** to acquire users → **Paid tiers** for serious users.

Why a free tier?
- Users don't want to pay without trying
- Word of mouth: free users tell their friends
- Once users depend on your app, upgrading feels natural

Why paid tiers?
- Server costs money (AWS, database, CDN)
- Engineering team needs salaries
- Customer support
- Revenue = company survives

**TaskFlow follows this model:**
- FREE: 3 projects (try before you buy)
- BASIC: Rs.499/month (growing teams)
- PRO: Rs.999/month (power users + AI features)
- ENTERPRISE: Rs.2999/month (large organizations)

---

## 2. Payment Gateways — What They Are and Why You Need One

A **payment gateway** is a service that handles the complex, regulated process of moving money between bank accounts.

**Without a payment gateway, you would need to:**
1. Get a PCI DSS certification (credit card industry security standard — costs $50,000+/year)
2. Build integrations with Visa, Mastercard, RuPay, UPI, netbanking
3. Handle fraud detection
4. Comply with RBI regulations
5. Build a reconciliation system
6. Handle refunds, chargebacks, disputes

**With Razorpay:**
- You call their API → they handle all of the above
- They charge ~2% per transaction
- Takes 1-2 days to integrate vs 2+ years to build yourself

Popular payment gateways by country:
| Country | Gateway |
|---------|---------|
| India | Razorpay, PayU, CCAvenue |
| USA | Stripe, PayPal, Square |
| Global | Stripe (available in 46+ countries) |
| Europe | Adyen, Mollie |

Razorpay is India's most popular payment gateway — used by Swiggy, BYJU'S, Nykaa, OYO.

---

## 3. The Complete Razorpay Payment Flow

This is the most important thing to understand in Session 5. There are 3 parties:
- **YOUR BACKEND** (Spring Boot)
- **YOUR FRONTEND** (React — built in Session 8)
- **RAZORPAY SERVERS** (external)

```
STEP 1: User clicks "Upgrade to Pro"
   Frontend --> POST /api/payments/create-order { "plan": "PRO" }
   Backend  --> Calls Razorpay API: orders.create({ amount: 99900, currency: "INR" })
   Razorpay --> Returns order_id: "order_abc123xyz"
   Backend  --> Saves Payment(orderId="order_abc123", status=CREATED) to DB
   Backend  --> Returns { orderId: "order_abc123", keyId: "rzp_test_...", amount: 99900 } to frontend

STEP 2: User pays (frontend only, your backend not involved)
   Frontend --> Opens Razorpay checkout modal (a JavaScript popup)
   User     --> Enters card/UPI/netbanking details
   Razorpay --> Processes payment on their servers
   Razorpay --> Gives frontend: razorpay_order_id + razorpay_payment_id + razorpay_signature

STEP 3: Verify payment (backend verification)
   Frontend --> POST /api/payments/verify {
                  razorpayOrderId: "order_abc123",
                  razorpayPaymentId: "pay_def456",
                  razorpaySignature: "abc123..."
               }
   Backend  --> Computes HMAC("order_abc123|pay_def456", key_secret)
   Backend  --> Compares computed hash with received signature
   If match --> Mark payment PAID, upgrade user to PRO plan
   If no match --> Mark payment FAILED, return 400 error

STEP 4: Webhook (backup, async)
   Razorpay --> POST /api/webhooks/razorpay (calls YOUR server directly)
              { "event": "payment.captured", "payload": { ... } }
   Backend  --> Verifies X-Razorpay-Signature header
   Backend  --> If payment not already marked PAID --> mark PAID, upgrade user
```

**Why Step 3 AND Step 4 (both verify AND webhook)?**

In a perfect world, Step 3 alone is enough. But networks are unreliable:
- User pays → browser crashes before frontend calls Step 3
- User pays → server returns 500 → frontend doesn't retry
- User pays → internet disconnects

In these cases, Step 3 never happens. The webhook (Step 4) is Razorpay's backup:
"I'll tell your server directly, regardless of what the frontend does."

---

## 4. HMAC-SHA256 — The Security Behind Payment Verification

This is one of the most important cryptography concepts in payment systems.

### The Problem: How do you know the payment is real?

After a user "pays" in the Razorpay modal, the frontend has:
- `razorpay_order_id: "order_abc123"`
- `razorpay_payment_id: "pay_def456"`
- `razorpay_signature: "some_hex_string"`

But JavaScript runs in the user's browser. A hacker could:
1. Open browser DevTools
2. Set `razorpay_payment_id = "pay_fake123"`
3. Send a fake verification request

How does your backend know if `pay_def456` is REAL or FAKE?

### The Solution: HMAC Signatures

**HMAC** = Hash-based Message Authentication Code

It's like a tamper-proof wax seal on an envelope. Only someone with the secret key can create it.

**How Razorpay creates the signature:**
```
data     = razorpay_order_id + "|" + razorpay_payment_id
signature = HMAC_SHA256(data, YOUR_KEY_SECRET)
```

Only Razorpay knows your `key_secret`. So only Razorpay can create a valid signature.

**How your backend verifies it:**
```java
String data = orderId + "|" + paymentId;
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(new SecretKeySpec(keySecret.getBytes(), "HmacSHA256"));
byte[] hash = mac.doFinal(data.getBytes());
// Convert hash bytes to hex string
String expected = "abc123def456...";   // What YOU computed
String received = "abc123def456...";   // What Razorpay sent

if (expected.equals(received)) → GENUINE Razorpay response
else                           → TAMPERED or FAKE → reject!
```

**What SHA-256 produces:**
- Input: any text
- Output: always exactly 32 bytes = 64 hex characters
- Example: `"order_abc|pay_def"` → `"3a9b2c1d4e5f6789..."`

**Key properties:**
1. Same input → always same output
2. Tiny change in input → completely different output (avalanche effect)
3. Cannot reverse it: given the hash, you cannot find the original input
4. Cannot fake it: without the secret key, you cannot produce a valid HMAC

**Real industry usage:**
- Stripe uses `Stripe-Signature` header (same HMAC concept)
- GitHub uses `X-Hub-Signature-256` for webhook verification
- Slack uses `X-Slack-Signature` for event verification
- AWS uses HMAC-SHA256 in Signature Version 4 for all API requests

**Your implementation:**
```java
private String computeHmac(String data, String secret)
        throws NoSuchAlgorithmException, InvalidKeyException {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec keySpec = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    mac.init(keySpec);
    byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

    // Convert each byte to 2 hex chars: byte 255 → "ff", byte 7 → "07"
    StringBuilder hex = new StringBuilder();
    for (byte b : hashBytes) {
        hex.append(String.format("%02x", b));
    }
    return hex.toString();
}
```

---

## 5. Why Store Money as Long (Paise), Not Double (Rupees)?

This is a classic interview question and a real bug that has cost companies millions.

**The problem with floating point:**
```java
double a = 0.1;
double b = 0.2;
System.out.println(a + b);  // Prints: 0.30000000000000004 (NOT 0.3!)
```

This is not a Java bug — it's how all computers represent decimals in binary (IEEE 754 standard). Binary can't represent 0.1 exactly, just like decimal can't represent 1/3 exactly (0.333333...).

**For money, this is catastrophic:**
```java
// User balance: Rs.999.99
double balance = 999.99;
// Charge Rs.0.01
balance = balance - 0.01;
System.out.println(balance);  // 999.98? NO: 999.9799999999999...
```

Over millions of transactions, these rounding errors accumulate. Real incidents:
- Vancouver Stock Exchange 1982: rounding errors in index calculation led to 15% error over 22 months
- Knight Capital 2012: algorithmic trading bug (not floating point, but numeric handling) → lost $440 million in 45 minutes

**The solution: always store money as the smallest currency unit (integer)**
```java
// Rs.499.00 = 49900 paise (integer, exact)
// Rs.999.99 = 99999 paise (integer, exact)
// Rs.0.01   = 1 paise    (integer, exact)

long balance = 99999L;  // Rs.999.99 in paise
balance = balance - 1L; // Subtract 1 paise
// balance = 99998L      EXACT. No rounding error possible.
```

**Rule: Never store money as float or double. Always use Long (or BigDecimal for display).**

Razorpay (and Stripe) enforce this by accepting amounts in paise/cents. If you send `amount: 999.99`, they throw an error. Must send `amount: 99999`.

---

## 6. Webhooks — The Backup Confirmation Channel

Webhooks are "reverse API calls" — instead of your code calling someone else's API, they call yours.

### Normal API (Pull model):
```
Your app → asks → External service
"Did the payment succeed?"
```

### Webhook (Push model):
```
External service → tells → Your app
"Payment just succeeded! Here's what happened."
```

**Real examples:**

| Service | Webhook event | What you do |
|---------|--------------|-------------|
| Razorpay | `payment.captured` | Activate subscription |
| GitHub | `push` | Trigger CI/CD build |
| Stripe | `invoice.paid` | Extend subscription |
| Shopify | `order/created` | Reserve stock, notify warehouse |
| Twilio | `message.delivered` | Update delivery status in your DB |
| Zoom | `meeting.ended` | Save recording, send meeting notes |

### Your Webhook Endpoint

```java
@PostMapping("/razorpay")
public ResponseEntity<Void> handleRazorpayWebhook(
        @RequestBody String payload,           // Raw body — NOT parsed JSON!
        @RequestHeader("X-Razorpay-Signature") String signature) {

    paymentService.handleWebhook(payload, signature);
    return ResponseEntity.ok().build();  // Always return 200 fast
}
```

**Why `@RequestBody String payload` (not `@RequestBody SomeDTO`)?**

The HMAC signature is computed over the EXACT bytes Razorpay sent. If Spring parses the JSON first (reordering keys, changing whitespace), the bytes change, and your computed signature won't match Razorpay's.

**Rule:** Always verify webhook signatures on the raw string body before parsing.

**Why return 200 immediately?**

Razorpay considers your webhook "failed" if it doesn't get a 2xx response within 5 seconds. If you return non-2xx, Razorpay retries up to 5 times.
For long processing (sending emails, updating multiple records), return 200 first, then process asynchronously.

**Webhook Security:**
- The endpoint must be publicly accessible (Razorpay calls it)
- No JWT required (Razorpay doesn't have your JWT)
- Authentication is done via HMAC signature with a SEPARATE `webhook_secret`
- This is why the webhook URL is in the `permitAll()` list in SecurityConfig

---

## 7. Subscription Plans and Pricing Tiers

```
SubscriptionPlan enum:
  FREE        → default for all new users
  BASIC       → Rs.499/month
  PRO         → Rs.999/month
  ENTERPRISE  → Rs.2999/month
```

**Feature gating (future implementation):**

Once a user pays, `user.getSubscriptionPlan()` returns their plan. You can gate features:

```java
// Example: AI features are PRO-only
public AiSummaryResponse generateAiSummary(Long projectId, User currentUser) {
    if (currentUser.getSubscriptionPlan() == SubscriptionPlan.FREE) {
        throw new AccessDeniedException("AI features require Pro plan. Upgrade at /api/payments/create-order");
    }
    // ... proceed with AI summary
}
```

**Subscription expiry:**

`user.subscriptionExpiresAt` is set to `now + 1 month` on successful payment.
A scheduled job (added in Session 9) checks daily if subscriptions have expired and downgrades users to FREE.

---

## 8. New Files Added in Session 5

### Enums
- `entity/SubscriptionPlan.java` — FREE, BASIC, PRO, ENTERPRISE
- `entity/PaymentStatus.java` — CREATED, PAID, FAILED, REFUNDED

### Entity
- `entity/Payment.java` — DB record of every payment: orderId, paymentId, signature, amount, status, plan, user

### Config
- `config/RazorpayConfig.java` — creates `RazorpayClient` bean from key-id/key-secret

### Exception
- `exception/PaymentException.java` — thrown on payment failures → 400 Bad Request

### DTOs
- `dto/request/CreateOrderRequest.java` — `{ "plan": "PRO" }`
- `dto/request/VerifyPaymentRequest.java` — `{ razorpayOrderId, razorpayPaymentId, razorpaySignature }`
- `dto/response/OrderResponse.java` — orderId, amount, keyId returned after order creation
- `dto/response/PaymentResponse.java` — payment record with fromEntity() mapper

### Repository
- `repository/PaymentRepository.java` — findByRazorpayOrderId, findByUser, existsByRazorpayPaymentId

### Service
- `service/PaymentService.java` — createOrder(), verifyPayment(), handleWebhook(), getMyPayments()

### Controllers
- `controller/PaymentController.java` — POST /create-order, POST /verify, GET /history
- `controller/WebhookController.java` — POST /api/webhooks/razorpay (public endpoint)

### Modified Files
- `entity/User.java` — added `subscriptionPlan` (default FREE) + `subscriptionExpiresAt`
- `config/SecurityConfig.java` — added `/api/webhooks/**` to permitAll()
- `exception/GlobalExceptionHandler.java` — added PaymentException → 400 handler
- `application.yml` — added `razorpay.key-id`, `key-secret`, `webhook-secret`

---

## 9. Database Changes

Hibernate auto-creates these tables from your entities:

```sql
-- New: payments table
CREATE TABLE payments (
    id                  BIGSERIAL PRIMARY KEY,
    razorpay_order_id   VARCHAR(255) UNIQUE NOT NULL,  -- "order_abc123"
    razorpay_payment_id VARCHAR(255),                   -- "pay_def456" (null until paid)
    razorpay_signature  VARCHAR(512),                   -- HMAC hex string (null until paid)
    amount_in_paise     BIGINT NOT NULL,               -- 99900 = Rs.999
    currency            VARCHAR(3) NOT NULL DEFAULT 'INR',
    status              VARCHAR(20) NOT NULL,           -- CREATED/PAID/FAILED/REFUNDED
    plan                VARCHAR(20) NOT NULL,           -- BASIC/PRO/ENTERPRISE
    user_id             BIGINT NOT NULL REFERENCES users(id),
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP
);

-- Indexes for performance (already in @Table(indexes = {...}))
CREATE UNIQUE INDEX idx_payment_order ON payments(razorpay_order_id);
CREATE INDEX idx_payment_user ON payments(user_id);
CREATE INDEX idx_payment_status ON payments(status);

-- Modified: users table (new columns added)
ALTER TABLE users ADD COLUMN subscription_plan VARCHAR(20) NOT NULL DEFAULT 'FREE';
ALTER TABLE users ADD COLUMN subscription_expires_at TIMESTAMP;
```

---

## 10. How to Set Up Razorpay Test Keys

1. Go to https://razorpay.com and click "Sign Up" (free, no credit card)

2. On the dashboard, go to **Settings → API Keys**

3. Click **"Generate Test Key"** — you get:
   - Key ID: `rzp_test_xxxxxxxxxx`
   - Key Secret: `yyyyyyyyyyyyyyy`

4. Open `application.yml` and replace:
   ```yaml
   razorpay:
     key-id: rzp_test_xxxxxxxxxx      # your actual test key ID
     key-secret: yyyyyyyyyyyyyyy      # your actual test key secret
   ```

5. For webhook secret (optional for testing without real webhook):
   - Go to **Settings → Webhooks → Add Webhook**
   - URL: `https://your-ngrok-url/api/webhooks/razorpay`
   - Select events: `payment.captured`, `payment.failed`
   - Copy the webhook secret and add to `application.yml`

**Test Card for Razorpay test mode:**
```
Card Number: 4111 1111 1111 1111
Expiry:      Any future date (e.g., 12/26)
CVV:         Any 3 digits (e.g., 123)
OTP:         1234
```

---

## 11. Testing With Swagger — Step by Step

Open `http://localhost:8080/swagger-ui.html`

### Step 1: Register and login

1. `POST /api/auth/register` — create an account
2. `POST /api/auth/login` — get your JWT token
3. Click "Authorize" button → paste your token as `Bearer <token>`

### Step 2: Create a payment order

**POST /api/payments/create-order**
```json
{
  "plan": "PRO"
}
```

Expected response:
```json
{
  "orderId": "order_xxxxxxxxxxxxxxxx",
  "amountInPaise": 99900,
  "amountInRupees": 999.0,
  "currency": "INR",
  "keyId": "rzp_test_yourkeyid",
  "plan": "PRO",
  "planDisplayName": "Pro Plan",
  "description": "TaskFlow Pro Plan - 1 month subscription"
}
```

### Step 3: Simulate payment completion (test mode)

In a real flow, the frontend would open the Razorpay modal. For Swagger testing, you need to simulate the payment. Use Razorpay's test dashboard to see the created order.

### Step 4: Verify payment

**POST /api/payments/verify**
```json
{
  "razorpayOrderId": "order_xxx",
  "razorpayPaymentId": "pay_yyy",
  "razorpaySignature": "computed_hmac_hex"
}
```

If you use the correct values from a real Razorpay test payment, the response will show:
```json
{
  "status": "PAID",
  "plan": "PRO",
  "planDisplayName": "Pro Plan - Rs.999/month"
}
```

### Step 5: Check payment history

**GET /api/payments/history**

Shows all your payments with their status.

---

## 12. Interview Q&A

**Q: How does Razorpay payment integration work?**

A: Three steps. (1) Backend creates a Razorpay order via their API — gets an orderId. (2) Frontend receives orderId and opens the Razorpay checkout modal for the user to pay. (3) After payment, frontend sends orderId + paymentId + signature to our backend. Backend verifies the HMAC-SHA256 signature using our key secret. If valid, mark payment as PAID and upgrade the user's subscription plan.

---

**Q: What is HMAC and why is it used for payment verification?**

A: HMAC (Hash-based Message Authentication Code) is a way to verify that a message came from a trusted sender and wasn't tampered with. Razorpay computes HMAC_SHA256(orderId + "|" + paymentId, key_secret) and sends it as the signature. Our backend recomputes the same thing. If they match, the payment is genuine — only Razorpay knows our key_secret, so only Razorpay could have produced that signature. If they don't match, the request was tampered with or faked.

---

**Q: Why do you store amount in paise (Long) instead of rupees (Double)?**

A: Floating point numbers (double/float) can't represent decimal values exactly in binary — 0.1 + 0.2 = 0.30000000000000004 in Java. For money, this causes rounding errors that accumulate over millions of transactions. Storing as Long in the smallest currency unit (paise for INR, cents for USD) uses integer arithmetic, which is exact. Rs.499 = 49900L. Razorpay and Stripe both enforce this in their APIs.

---

**Q: What is a webhook and how do you secure it?**

A: A webhook is a "reverse API call" — instead of your app polling an external service, the external service calls your app when something happens. For payment webhooks, Razorpay calls `POST /api/webhooks/razorpay` with payment event data. Security: (1) the endpoint is public (no JWT since Razorpay doesn't have one), (2) we verify the `X-Razorpay-Signature` header — HMAC of the raw request body with our webhook_secret. If signature is invalid, reject the request. This prevents fake webhook calls from attackers.

---

**Q: What happens if a user pays but the frontend crashes before calling verify?**

A: The webhook handles this. Razorpay sends a `payment.captured` event directly to our server at `/api/webhooks/razorpay`, regardless of what the frontend does. Our webhook handler checks if the payment is already marked PAID (to avoid double-processing), and if not, marks it PAID and activates the subscription. This is why you always need both: verify (frontend-triggered) AND webhook (Razorpay-triggered as backup).

---

**Q: How do you prevent duplicate payment processing (idempotency)?**

A: Two checks. (1) Before verification, check `payment.getStatus() == PAID` — if already paid, return the existing record without re-processing. (2) Check `existsByRazorpayPaymentId(paymentId)` — if this specific Razorpay payment ID has already been processed, reject as a replay attack. This ensures the same payment can never activate a subscription twice, even if the verify endpoint is called multiple times.

---

**Q: How do subscription plans gate features?**

A: The `User` entity has a `subscriptionPlan` field (enum: FREE/BASIC/PRO/ENTERPRISE) and `subscriptionExpiresAt`. In service methods that need premium features, check `currentUser.getSubscriptionPlan()`. If the user is FREE, throw `AccessDeniedException` with a message explaining how to upgrade. In production, this logic can be extracted into an AOP annotation like `@RequiresPlan(PRO)` — the same AOP pattern from Session 4.

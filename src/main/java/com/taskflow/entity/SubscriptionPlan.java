package com.taskflow.entity;

/**
 * SUBSCRIPTION PLAN — Defines what each tier of user gets.
 *
 * WHY SUBSCRIPTION PLANS EXIST:
 *   SaaS (Software as a Service) products like Jira, Notion, Slack all use
 *   tiered pricing. Users pay MORE for MORE features. This is how software
 *   companies generate recurring revenue (MRR — Monthly Recurring Revenue).
 *
 *   The pattern: give a free tier to acquire users, then upsell.
 *   - Slack: free (10k messages), Pro ($7.25/user/month)
 *   - Notion: free (personal), Pro ($8/month), Business ($15/month)
 *   - Jira: free (10 users), Standard ($7.75/user/month)
 *
 * STORED AS STRING IN DB (@Enumerated(EnumType.STRING)):
 *   If you store as ordinal (0, 1, 2...) and add a new enum between existing ones,
 *   all existing data shifts. String storage is resilient to reordering.
 *
 * INTERVIEW Q: How do you handle feature gating in a subscription system?
 * A: Check the user's subscriptionPlan before allowing the operation.
 *    Example: if (user.getPlan() == FREE && projectCount >= 3) throw exception
 *    More advanced: use annotations + AOP (like @RequiresPlan(PRO)).
 */
public enum SubscriptionPlan {

    FREE,        // Default — 3 projects, 10 tasks/project (no payment needed)
    BASIC,       // Rs.499/month — 10 projects, unlimited tasks, basic analytics
    PRO,         // Rs.999/month — unlimited projects + tasks, AI features, priority support
    ENTERPRISE   // Rs.2999/month — team features, SSO, dedicated support
}

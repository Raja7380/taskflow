package com.taskflow.learn.monitoring;

/**
 * =====================================================================
 * MONITORING & OBSERVABILITY — Prometheus, Grafana, ELK Stack
 * =====================================================================
 *
 * THE THREE PILLARS OF OBSERVABILITY:
 *   1. METRICS — Numbers (CPU usage, request count, response time)
 *   2. LOGS — Text records of what happened (errors, events)
 *   3. TRACES — Request journey across services (distributed tracing)
 *
 * =====================================================================
 * 1. PROMETHEUS + GRAFANA (Metrics)
 * =====================================================================
 *
 * PROMETHEUS:
 *   WHAT: Open-source monitoring system that PULLS metrics from your app
 *   HOW: Your app exposes /actuator/prometheus endpoint
 *        Prometheus scrapes this endpoint every 15 seconds
 *        Stores metrics as time-series data
 *
 *   Spring Boot setup:
 *     1. Add dependency: micrometer-registry-prometheus
 *     2. Enable actuator: management.endpoints.web.exposure.include=prometheus
 *     3. App exposes: http://localhost:8080/actuator/prometheus
 *
 *   Metrics exposed:
 *     http_server_requests_seconds_count — total request count
 *     http_server_requests_seconds_sum — total response time
 *     jvm_memory_used_bytes — JVM memory usage
 *     hikaricp_connections_active — active DB connections
 *     custom metrics — you can create your own!
 *
 * GRAFANA:
 *   WHAT: Visualization tool — creates dashboards from Prometheus data
 *   HOW: Connect to Prometheus, write queries, build graphs
 *
 *   Example dashboard:
 *     - Request rate graph (requests/second)
 *     - Response time percentiles (p50, p95, p99)
 *     - Error rate (% of 5xx responses)
 *     - JVM heap memory usage
 *     - Database connection pool stats
 *
 * =====================================================================
 * 2. ELK STACK (Logging)
 * =====================================================================
 *
 * ELK = Elasticsearch + Logstash + Kibana
 *
 * ELASTICSEARCH:
 *   WHAT: Search engine that stores and indexes log data
 *   Think of it as Google for your logs
 *
 * LOGSTASH:
 *   WHAT: Collects, processes, and forwards logs
 *   App logs -> Logstash (parse, enrich) -> Elasticsearch
 *
 * KIBANA:
 *   WHAT: Web UI to search and visualize logs in Elasticsearch
 *   Search: "Show me all ERROR logs from auth-service in the last hour"
 *
 * SIMPLER ALTERNATIVE: Loki + Grafana
 *   Loki = lightweight log aggregation (like Prometheus but for logs)
 *   Uses the same Grafana for visualization
 *   Much simpler to set up than full ELK stack
 *
 * =====================================================================
 * INTERVIEW QUESTIONS:
 * =====================================================================
 *
 * Q: How do you monitor a Spring Boot application?
 * A: Spring Boot Actuator exposes health, metrics, and info endpoints.
 *    Prometheus scrapes /actuator/prometheus for metrics.
 *    Grafana visualizes the metrics in dashboards.
 *    For logs: centralized logging with ELK or Loki.
 *
 * Q: What metrics would you monitor?
 * A: Request rate, error rate, response time (p95, p99),
 *    JVM memory, GC pauses, DB connection pool, thread count.
 *    Business metrics: registrations/hour, tasks created/day.
 *
 * Q: What is the difference between monitoring and observability?
 * A: Monitoring = watching known metrics for known problems
 *    Observability = ability to understand internal state from external output
 *    Monitoring tells you WHAT is broken.
 *    Observability helps you understand WHY.
 */
public class MonitoringGuide {
    // This is a learning reference file — no executable code.
}

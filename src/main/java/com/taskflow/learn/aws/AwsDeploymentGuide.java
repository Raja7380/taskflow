package com.taskflow.learn.aws;

/**
 * =====================================================================
 * AWS (Amazon Web Services) — Cloud Deployment Guide
 * =====================================================================
 *
 * WHAT IS AWS?
 *   The world's largest cloud computing platform.
 *   Instead of buying physical servers, you RENT computing resources
 *   from Amazon and pay only for what you use.
 *
 * WHY AWS?
 *   1. No upfront cost — pay-as-you-go
 *   2. Scale instantly — need more servers? Click a button
 *   3. Global — deploy in 30+ regions worldwide
 *   4. Reliable — 99.99% uptime SLA
 *   5. Most used — ~33% market share, most job postings mention AWS
 *
 * =====================================================================
 * KEY SERVICES FOR TASKFLOW:
 * =====================================================================
 *
 * 1. EC2 (Elastic Compute Cloud) — Virtual Servers
 *    WHAT: Rent virtual machines (servers) in the cloud
 *    USE: Run your Spring Boot application
 *
 *    How to deploy TaskFlow on EC2:
 *      1. Launch an EC2 instance (t2.micro = free tier)
 *      2. SSH into the instance
 *      3. Install Java 17 + Docker
 *      4. Clone your repo
 *      5. Run docker-compose up -d
 *
 *    Instance types:
 *      t2.micro  — free tier, 1 vCPU, 1GB RAM (development)
 *      t3.medium — 2 vCPU, 4GB RAM (small production)
 *      m5.large  — 2 vCPU, 8GB RAM (medium production)
 *
 * 2. RDS (Relational Database Service) — Managed PostgreSQL
 *    WHAT: AWS runs PostgreSQL for you (backups, updates, scaling)
 *    USE: Production database for TaskFlow
 *
 *    WHY not run PostgreSQL on EC2?
 *      - RDS handles backups automatically
 *      - RDS handles failover (if DB crashes, standby takes over)
 *      - RDS handles security patches
 *      - You focus on your app, not database maintenance
 *
 * 3. S3 (Simple Storage Service) — File Storage
 *    WHAT: Store files (images, documents, backups) in the cloud
 *    USE: Store task attachments, user profile pictures
 *
 *    Key concepts:
 *      Bucket = top-level folder (globally unique name)
 *      Object = file stored in a bucket
 *      Key = file path within the bucket
 *
 *    Example:
 *      s3://taskflow-uploads/users/123/avatar.jpg
 *      Bucket: taskflow-uploads
 *      Key: users/123/avatar.jpg
 *
 * 4. ECS (Elastic Container Service) — Run Docker containers
 *    WHAT: Run Docker containers without managing servers
 *    USE: Deploy TaskFlow's Docker containers
 *
 *    ECS vs EC2:
 *      EC2: You manage the server + containers
 *      ECS: You just provide Docker images, AWS manages the rest
 *
 *    With Fargate (serverless ECS):
 *      No servers to manage AT ALL. Just say "run this container"
 *      and AWS figures out where to put it.
 *
 * 5. ELASTICACHE — Managed Redis
 *    WHAT: AWS-managed Redis (like RDS but for Redis)
 *    USE: Caching layer for TaskFlow
 *
 * 6. ROUTE 53 — DNS
 *    WHAT: Maps domain names to your servers
 *    USE: taskflow.yourdomain.com -> your EC2/ECS
 *
 * 7. ALB (Application Load Balancer)
 *    WHAT: Distributes traffic across multiple servers
 *    USE: If TaskFlow runs on 3 EC2 instances, ALB splits traffic
 *
 * =====================================================================
 * DEPLOYMENT OPTIONS (Simplest to Most Complex):
 * =====================================================================
 *
 * Option 1: EC2 + Docker (simplest)
 *   - Launch EC2 instance
 *   - Install Docker
 *   - docker-compose up
 *   - Cost: ~$0 (free tier) to $10/month
 *
 * Option 2: ECS + Fargate (recommended)
 *   - Push Docker image to ECR (Elastic Container Registry)
 *   - Create ECS task definition
 *   - Run on Fargate (serverless)
 *   - Cost: ~$15-30/month
 *
 * Option 3: ECS + RDS + ElastiCache (production)
 *   - App on ECS Fargate
 *   - DB on RDS PostgreSQL
 *   - Cache on ElastiCache Redis
 *   - ALB for load balancing
 *   - Cost: ~$50-100/month
 *
 * =====================================================================
 * INTERVIEW QUESTIONS:
 * =====================================================================
 *
 * Q: What AWS services have you used?
 * A: EC2 for compute, RDS for PostgreSQL, S3 for file storage,
 *    ECS/Fargate for container orchestration, ElastiCache for Redis.
 *
 * Q: EC2 vs ECS vs Lambda?
 * A: EC2 = virtual machine (full control, you manage everything)
 *    ECS = container orchestration (you provide Docker images)
 *    Lambda = serverless functions (runs code on events, no server)
 *    Choose based on needed control level and pricing model.
 *
 * Q: How would you deploy a Spring Boot app on AWS?
 * A: Containerize with Docker, push to ECR, deploy on ECS Fargate.
 *    Use RDS for database, ElastiCache for Redis, ALB for load balancing.
 *    CI/CD with GitHub Actions -> auto-deploy on push.
 *
 * Q: What is the difference between S3 and EBS?
 * A: S3 = object storage (files, accessible via URL, unlimited)
 *    EBS = block storage (virtual hard drive, attached to EC2)
 *    S3 for files/uploads, EBS for EC2 server disk.
 */
public class AwsDeploymentGuide {
    // This is a learning reference file — no executable code.
}

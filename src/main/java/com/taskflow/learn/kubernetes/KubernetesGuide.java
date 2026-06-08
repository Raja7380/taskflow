package com.taskflow.learn.kubernetes;

/**
 * =====================================================================
 * KUBERNETES (K8s) — Container Orchestration Guide
 * =====================================================================
 *
 * WHAT IS KUBERNETES?
 *   An open-source platform that MANAGES Docker containers at scale.
 *
 *   Docker = packages your app into a container
 *   Kubernetes = manages 100s of containers across multiple servers
 *
 *   Think of it as:
 *   Docker = shipping container (packages your app)
 *   Kubernetes = the shipping port (manages all containers, decides
 *                which ship/server they go on, replaces broken ones)
 *
 * WHY KUBERNETES?
 *   Without K8s, you manually:
 *     - Start containers on each server
 *     - Restart crashed containers
 *     - Scale up when traffic increases
 *     - Load balance between instances
 *     - Roll out updates without downtime
 *   K8s does ALL of this automatically!
 *
 * =====================================================================
 * KEY CONCEPTS:
 * =====================================================================
 *
 * 1. POD — The smallest deployable unit
 *    A Pod = 1 or more containers that share network + storage
 *    Usually: 1 Pod = 1 container = 1 instance of your app
 *
 *    apiVersion: v1
 *    kind: Pod
 *    metadata:
 *      name: taskflow-pod
 *    spec:
 *      containers:
 *        - name: taskflow
 *          image: taskflow:1.0
 *          ports:
 *            - containerPort: 8080
 *
 * 2. DEPLOYMENT — Manages multiple identical Pods
 *    "I want 3 copies of my app running at all times"
 *    If one Pod crashes, Deployment creates a new one automatically!
 *
 *    apiVersion: apps/v1
 *    kind: Deployment
 *    metadata:
 *      name: taskflow-deployment
 *    spec:
 *      replicas: 3    # Run 3 identical Pods
 *      selector:
 *        matchLabels:
 *          app: taskflow
 *      template:
 *        metadata:
 *          labels:
 *            app: taskflow
 *        spec:
 *          containers:
 *            - name: taskflow
 *              image: taskflow:1.0
 *              ports:
 *                - containerPort: 8080
 *              resources:
 *                requests:
 *                  memory: "256Mi"
 *                  cpu: "250m"
 *                limits:
 *                  memory: "512Mi"
 *                  cpu: "500m"
 *
 * 3. SERVICE — Exposes Pods to the network
 *    Pods have random IPs that change. Service gives a STABLE address.
 *
 *    Types:
 *      ClusterIP (default) — accessible only within K8s cluster
 *      NodePort — accessible from outside via node's IP + port
 *      LoadBalancer — creates cloud load balancer (AWS ALB, GCP LB)
 *
 *    apiVersion: v1
 *    kind: Service
 *    metadata:
 *      name: taskflow-service
 *    spec:
 *      type: LoadBalancer
 *      selector:
 *        app: taskflow
 *      ports:
 *        - port: 80
 *          targetPort: 8080
 *
 * 4. CONFIGMAP & SECRET — External configuration
 *    ConfigMap = non-sensitive config (app name, feature flags)
 *    Secret = sensitive config (passwords, API keys) — Base64 encoded
 *
 * 5. INGRESS — HTTP routing to services
 *    Routes external HTTP traffic to internal services:
 *      /api/* -> taskflow-service
 *      /auth/* -> auth-service
 *
 * 6. HORIZONTAL POD AUTOSCALER (HPA)
 *    "If CPU > 70%, add more Pods. If CPU < 30%, remove Pods."
 *    Automatically scales based on metrics!
 *
 * =====================================================================
 * COMMON COMMANDS:
 * =====================================================================
 *
 * kubectl get pods              — list all pods
 * kubectl get deployments       — list all deployments
 * kubectl get services          — list all services
 * kubectl apply -f deploy.yaml  — apply a configuration file
 * kubectl logs pod-name         — view pod logs
 * kubectl exec -it pod-name -- bash  — shell into a pod
 * kubectl scale deployment/taskflow --replicas=5  — scale to 5 pods
 * kubectl rollout status deployment/taskflow  — check deployment status
 * kubectl rollout undo deployment/taskflow    — rollback to previous version
 *
 * =====================================================================
 * HOW TO DEPLOY TASKFLOW ON KUBERNETES:
 * =====================================================================
 *
 * 1. Build Docker image: docker build -t taskflow:1.0 .
 * 2. Push to registry: docker push yourname/taskflow:1.0
 * 3. Create K8s manifests (deployment.yaml, service.yaml)
 * 4. Apply: kubectl apply -f k8s/
 * 5. Check: kubectl get pods -w (watch pods come up)
 *
 * =====================================================================
 * INTERVIEW QUESTIONS:
 * =====================================================================
 *
 * Q: What is Kubernetes?
 * A: An open-source container orchestration platform that automates
 *    deployment, scaling, and management of containerized applications.
 *
 * Q: Pod vs Container?
 * A: A Pod is a K8s wrapper around 1+ containers. Containers in a Pod
 *    share network (localhost) and storage. Pod is the scheduling unit.
 *
 * Q: Deployment vs Pod?
 * A: Pod = single instance. Deployment = manages multiple Pod replicas,
 *    handles rolling updates, and auto-replaces failed Pods.
 *
 * Q: How does K8s handle rolling updates?
 * A: Creates new Pods with new version, waits for them to be ready,
 *    then terminates old Pods. Zero downtime deployment!
 *
 * Q: What happens when a Pod crashes?
 * A: The Deployment controller detects the failed Pod and creates
 *    a new one automatically. This is called "self-healing".
 */
public class KubernetesGuide {
    // This is a learning reference file — no executable code.
}

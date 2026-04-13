#!/bin/bash

# Deploy Exam Platform to EKS
echo "Deploying Exam Platform to EKS..."

# Apply namespace first
kubectl apply -f namespace.yaml

# Apply secrets and config
kubectl apply -f secrets.yaml
kubectl apply -f configmap.yaml

# Apply deployments and services
echo "Deploying backend and frontend..."
kubectl apply -f backend-deployment.yaml
kubectl apply -f frontend-deployment.yaml

# Apply ingress
kubectl apply -f ingress.yaml

# Apply HPA
kubectl apply -f hpa.yaml

echo "Deployment complete!"

# Check status
echo "Checking deployment status..."
kubectl get pods -n exam-platform
kubectl get services -n exam-platform

echo "To get the Ingress ALB URL:"
echo "kubectl get ingress exam-platform-ingress -n exam-platform"
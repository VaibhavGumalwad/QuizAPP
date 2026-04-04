# Kubernetes Deployment for Exam Platform

## Prerequisites

1. **EKS Cluster** running and kubectl configured
2. **Docker images** pushed to ECR:
   - exam-platform-backend:latest
   - exam-platform-frontend:latest
3. **MySQL database** accessible from EKS cluster
4. **AWS Load Balancer Controller** installed (for ingress)

## Quick Deployment

1. **Update image URLs** in deployment files:
   ```bash
   # Replace <your-account-id> with your AWS account ID
   sed -i 's/<your-account-id>/123456789012/g' backend-deployment.yaml
   sed -i 's/<your-account-id>/123456789012/g' frontend-deployment.yaml
   ```

2. **Update MySQL host** in backend-deployment.yaml:
   ```bash
   # Replace your-mysql-host with actual MySQL endpoint
   sed -i 's/your-mysql-host/your-rds-endpoint.amazonaws.com/g' backend-deployment.yaml
   ```

3. **Deploy all resources**:
   ```bash
   chmod +x deploy.sh
   ./deploy.sh
   ```

## Manual Deployment

```bash
# 1. Create namespace
kubectl apply -f namespace.yaml

# 2. Create secrets and config
kubectl apply -f secrets.yaml
kubectl apply -f configmap.yaml

# 3. Deploy backend
kubectl apply -f backend-deployment.yaml

# 4. Deploy frontend
kubectl apply -f frontend-deployment.yaml

# 5. Setup ingress (optional)
kubectl apply -f ingress.yaml

# 6. Setup auto-scaling (optional)
kubectl apply -f hpa.yaml
```

## Verify Deployment

```bash
# Check pods
kubectl get pods -n exam-platform

# Check services
kubectl get services -n exam-platform

# Get frontend URL
kubectl get service frontend-service -n exam-platform

# Check logs
kubectl logs -f deployment/backend-deployment -n exam-platform
kubectl logs -f deployment/frontend-deployment -n exam-platform
```

## Configuration

### Backend Environment Variables
- `SPRING_DATASOURCE_URL`: MySQL connection string
- `SPRING_DATASOURCE_USERNAME`: Database username (from secret)
- `SPRING_DATASOURCE_PASSWORD`: Database password (from secret)
- `JWT_SECRET`: JWT signing secret (from secret)
- `GEMINI_API_KEY`: Google Gemini API key (from secret)

### Frontend Environment Variables
- `REACT_APP_API_URL`: Backend API URL (http://backend-service:8081/api)

## Scaling

The deployment includes Horizontal Pod Autoscaler (HPA):
- **Backend**: 2-10 replicas based on CPU (70%) and memory (80%)
- **Frontend**: 2-5 replicas based on CPU (70%)

## Access

- **Frontend**: Via LoadBalancer service or Ingress
- **Backend**: Internal service (backend-service:8081)
- **API**: Via frontend LoadBalancer + /api path or Ingress

## Troubleshooting

1. **Pods not starting**:
   ```bash
   kubectl describe pod <pod-name> -n exam-platform
   ```

2. **Image pull errors**:
   - Verify ECR repository exists
   - Check IAM permissions for EKS nodes
   - Ensure images are tagged correctly

3. **Database connection issues**:
   - Verify MySQL host is accessible from EKS
   - Check security groups allow port 3306
   - Verify credentials in secrets

4. **Service not accessible**:
   ```bash
   kubectl get endpoints -n exam-platform
   kubectl describe service <service-name> -n exam-platform
   ```
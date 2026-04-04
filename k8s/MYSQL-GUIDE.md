# MySQL Deployment Options for Exam Platform

## Option 1: MySQL on EKS (Development/Testing)

### Advantages:
- Quick setup
- No additional AWS costs
- Full control over configuration
- Good for development/testing

### Disadvantages:
- Single point of failure
- Manual backup management
- Limited scalability

### Deployment:
```bash
# Deploy MySQL with persistent storage
kubectl apply -f mysql-init.yaml
kubectl apply -f mysql-deployment.yaml

# Wait for MySQL to be ready
kubectl wait --for=condition=ready pod -l app=mysql -n exam-platform --timeout=300s

# Deploy the rest of the application
./deploy.sh
```

### Access MySQL:
```bash
# Port forward to access MySQL directly
kubectl port-forward service/mysql-service 3306:3306 -n exam-platform

# Connect using MySQL client
mysql -h localhost -P 3306 -u root -p
```

## Option 2: Amazon RDS MySQL (Production)

### Advantages:
- Managed service (automated backups, updates)
- High availability with Multi-AZ
- Automatic scaling
- Better security and monitoring

### Disadvantages:
- Additional cost (~$15-50/month)
- Less control over configuration
- Network latency (minimal)

### Setup RDS:
1. **Add RDS to Terraform**:
   ```bash
   # The mysql-rds.tf file is already created
   cd terraform
   terraform plan
   terraform apply
   ```

2. **Update backend deployment**:
   ```yaml
   # In backend-deployment.yaml, change:
   - name: SPRING_DATASOURCE_URL
     value: "jdbc:mysql://your-rds-endpoint:3306/exam_platform"
   ```

3. **Get RDS endpoint**:
   ```bash
   terraform output mysql_endpoint
   ```

## Option 3: External MySQL (Existing Database)

### If you have existing MySQL:
1. **Update backend-deployment.yaml**:
   ```yaml
   - name: SPRING_DATASOURCE_URL
     value: "jdbc:mysql://your-mysql-host:3306/exam_platform"
   ```

2. **Update secrets.yaml** with correct credentials

3. **Ensure network connectivity** from EKS to your MySQL

## Recommended Approach

### For Development:
```bash
# Use MySQL on EKS
kubectl apply -f mysql-init.yaml
kubectl apply -f mysql-deployment.yaml
./deploy.sh
```

### For Production:
```bash
# Use RDS MySQL
cd terraform
terraform apply  # This creates RDS instance

# Update backend-deployment.yaml with RDS endpoint
# Then deploy
kubectl apply -f namespace.yaml
kubectl apply -f secrets.yaml
kubectl apply -f backend-deployment.yaml
kubectl apply -f frontend-deployment.yaml
```

## Database Migration

### From EKS MySQL to RDS:
```bash
# 1. Backup data from EKS MySQL
kubectl exec -it deployment/mysql-deployment -n exam-platform -- mysqldump -u root -p exam_platform > backup.sql

# 2. Restore to RDS
mysql -h your-rds-endpoint -u root -p exam_platform < backup.sql

# 3. Update backend deployment to use RDS
# 4. Redeploy backend
kubectl rollout restart deployment/backend-deployment -n exam-platform
```

## Monitoring MySQL

### Check MySQL status:
```bash
# Check pods
kubectl get pods -l app=mysql -n exam-platform

# Check logs
kubectl logs -f deployment/mysql-deployment -n exam-platform

# Check persistent volume
kubectl get pvc -n exam-platform

# Connect to MySQL
kubectl exec -it deployment/mysql-deployment -n exam-platform -- mysql -u root -p
```

### MySQL Performance:
```sql
-- Check database size
SELECT 
    table_schema AS 'Database',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
FROM information_schema.tables 
WHERE table_schema = 'exam_platform'
GROUP BY table_schema;

-- Check active connections
SHOW PROCESSLIST;

-- Check table status
SHOW TABLE STATUS FROM exam_platform;
```

## Backup Strategy

### For EKS MySQL:
```bash
# Create backup job
kubectl create job mysql-backup --from=cronjob/mysql-backup -n exam-platform

# Manual backup
kubectl exec deployment/mysql-deployment -n exam-platform -- mysqldump -u root -p exam_platform > backup-$(date +%Y%m%d).sql
```

### For RDS MySQL:
- Automated backups are enabled (7 days retention)
- Point-in-time recovery available
- Manual snapshots can be created

## Security Considerations

1. **Use secrets** for database credentials
2. **Enable SSL/TLS** for database connections
3. **Restrict network access** using security groups
4. **Regular security updates** (automatic with RDS)
5. **Monitor access logs** and unusual activity

## Cost Comparison

### EKS MySQL:
- Storage: ~$2/month (20GB EBS)
- Compute: Included in node costs
- **Total: ~$2/month**

### RDS MySQL (db.t3.micro):
- Instance: ~$15/month
- Storage: ~$2/month (20GB)
- **Total: ~$17/month**

Choose based on your requirements for availability, management overhead, and budget.
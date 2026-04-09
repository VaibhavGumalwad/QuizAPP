# Jenkins CI/CD — Exam Platform (ECR + EKS)

## Architecture

```
GitHub/SCM  →  Jenkins  →  ECR (image storage)  →  EKS (deployment)
```

## Pipeline Stages

```
Checkout
  ↓
Unit Tests  (Backend Maven + Frontend npm)  ← parallel
  ↓
SonarQube Analysis + Quality Gate
  ↓
ECR Login
  ↓
Docker Build & Push to ECR  (Backend + Frontend)  ← parallel
  ↓
Trivy Security Scan
  ↓
Deploy to EKS  (kubectl apply all k8s manifests)
  ↓
Smoke Test  (pod status + ALB health check)
  ↓
Auto-rollback on failure
```

---

## 1. Jenkins Credentials

Go to **Manage Jenkins → Credentials → System → Global credentials (unrestricted)**

| Credential ID    | Type                 | Value                          |
|------------------|----------------------|--------------------------------|
| `aws-credentials`| AWS Credentials      | IAM Access Key + Secret Key    |
| `aws-account-id` | Secret Text          | Your 12-digit AWS Account ID   |

---

## 2. Jenkins Plugins Required

- Pipeline
- Git
- Credentials Binding
- SonarQube Scanner
- Amazon ECR (or Docker Pipeline)

---

## 3. Tools on Jenkins Agent

```bash
# Docker
sudo apt-get install -y docker.io
sudo usermod -aG docker jenkins

# AWS CLI v2
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o awscliv2.zip
unzip awscliv2.zip && sudo ./aws/install

# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -sL https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Trivy
sudo apt-get install -y wget apt-transport-https gnupg lsb-release
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
echo "deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main" \
  | sudo tee /etc/apt/sources.list.d/trivy.list
sudo apt-get update && sudo apt-get install -y trivy
```

---

## 4. IAM Policy for Jenkins

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload",
        "ecr:PutImage"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "eks:DescribeCluster",
        "eks:ListClusters"
      ],
      "Resource": "*"
    }
  ]
}
```

---

## 5. ECR Repositories (one-time setup)

```bash
aws ecr create-repository --repository-name exam-platform-backend  --region us-east-1
aws ecr create-repository --repository-name exam-platform-frontend --region us-east-1
```

---

## 6. EKS Cluster Access for Jenkins

Allow the Jenkins IAM user/role to call kubectl against your cluster:

```bash
# Update kubeconfig locally first
aws eks update-kubeconfig --region us-east-1 --name exam-platform-eks-cluster

# Add Jenkins IAM user to aws-auth ConfigMap
kubectl edit configmap aws-auth -n kube-system
```

Add under `mapUsers` (for IAM user) or `mapRoles` (for IAM role):

```yaml
mapUsers: |
  - userarn: arn:aws:iam::<ACCOUNT_ID>:user/jenkins
    username: jenkins
    groups:
      - system:masters
```

---

## 7. Kubernetes Secrets (one-time setup)

```bash
kubectl create namespace exam-platform

kubectl create secret generic mysql-secret \
  --namespace exam-platform \
  --from-literal=username=admin \
  --from-literal=password=<db-password>

kubectl create secret generic app-secret \
  --namespace exam-platform \
  --from-literal=jwt-secret=<jwt-secret> \
  --from-literal=gemini-api-key=<gemini-api-key>
```

---

## 8. Create the Jenkins Pipeline Job

1. **New Item → Pipeline** → name it `exam-platform-cicd`
2. Under **Pipeline**:
   - Definition: `Pipeline script from SCM`
   - SCM: `Git`
   - Repository URL: your repo URL
   - Script Path: `Jenkinsfile`
3. Save → **Build with Parameters**

---

## 9. Pipeline Parameters

| Parameter    | Default | Description                                      |
|--------------|---------|--------------------------------------------------|
| `IMAGE_TAG`  | (blank) | Custom tag; auto-generates `<hash>-<build#>`     |
| `SKIP_TESTS` | false   | Skip Maven + npm tests (useful for hotfixes)     |
| `SKIP_SONAR` | false   | Skip SonarQube scan                              |

---

## 10. SonarQube Setup

**Manage Jenkins → Configure System → SonarQube servers**
- Name: `SonarQube`
- Server URL: `http://<sonar-host>:9000`
- Authentication token: add as Jenkins Secret Text credential, reference it here

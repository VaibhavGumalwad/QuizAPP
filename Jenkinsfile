pipeline {
    agent any

    parameters {
        string(name: 'IMAGE_TAG',    defaultValue: '', description: 'Docker image tag — leave blank to auto-generate from commit hash')
        booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Skip unit tests')
        booleanParam(name: 'SKIP_SONAR', defaultValue: false, description: 'Skip SonarQube quality gate')
    }

    environment {
        // ── AWS / ECR ──────────────────────────────────────────────────────────
        AWS_REGION      = 'us-east-1'
        AWS_ACCOUNT_ID  = credentials('aws-account-id')          // Secret Text credential
        ECR_REGISTRY    = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
        BACKEND_REPO    = 'exam-platform-backend'
        FRONTEND_REPO   = 'exam-platform-frontend'

        // ── EKS ───────────────────────────────────────────────────────────────
        EKS_CLUSTER     = 'exam-platform-eks-cluster'
        K8S_NAMESPACE   = 'exam-platform'

        // ── Misc ──────────────────────────────────────────────────────────────
        DOCKER_BUILDKIT = '1'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 45, unit: 'MINUTES')
        disableConcurrentBuilds()
        timestamps()
    }

    stages {

        // ── 1. Checkout ────────────────────────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.BUILD_TAG = params.IMAGE_TAG?.trim() ?: "${env.GIT_SHORT}-${env.BUILD_NUMBER}"
                    echo "Image tag: ${env.BUILD_TAG}"
                }
            }
        }

        // ── 2. Unit Tests (parallel) ───────────────────────────────────────────
        stage('Unit Tests') {
            when { expression { !params.SKIP_TESTS } }
            parallel {
                stage('Backend') {
                    steps {
                        dir('backend') {
                            sh 'mvn test -B'
                        }
                    }
                    post {
                        always {
                            junit 'backend/target/surefire-reports/*.xml'
                        }
                    }
                }
                stage('Frontend') {
                    steps {
                        dir('frontend') {
                            sh 'npm ci && npm test -- --watchAll=false --passWithNoTests'
                        }
                    }
                }
            }
        }

        // ── 3. SonarQube ──────────────────────────────────────────────────────
        stage('SonarQube Analysis') {
            when { expression { !params.SKIP_SONAR } }
            steps {
                withSonarQubeEnv('SonarQube') {
                    dir('backend') {
                        sh '''
                            mvn sonar:sonar \
                              -Dsonar.projectKey=exam-platform \
                              -Dsonar.projectName="Exam Platform" \
                              -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }

        stage('Quality Gate') {
            when { expression { !params.SKIP_SONAR } }
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // ── 4. ECR Login ──────────────────────────────────────────────────────
        stage('ECR Login') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                                  credentialsId: 'aws-credentials']]) {
                    sh '''
                        aws ecr get-login-password --region $AWS_REGION \
                          | docker login --username AWS --password-stdin $ECR_REGISTRY
                    '''
                }
            }
        }

        // ── 5. Build & Push to ECR (parallel) ─────────────────────────────────
        stage('Build & Push to ECR') {
            parallel {
                stage('Backend Image') {
                    steps {
                        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                                          credentialsId: 'aws-credentials']]) {
                            sh """
                                docker build \
                                  -t ${ECR_REGISTRY}/${BACKEND_REPO}:${BUILD_TAG} \
                                  -t ${ECR_REGISTRY}/${BACKEND_REPO}:latest \
                                  ./backend

                                docker push ${ECR_REGISTRY}/${BACKEND_REPO}:${BUILD_TAG}
                                docker push ${ECR_REGISTRY}/${BACKEND_REPO}:latest
                            """
                        }
                    }
                }
                stage('Frontend Image') {
                    steps {
                        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                                          credentialsId: 'aws-credentials']]) {
                            sh """
                                docker build \
                                  -t ${ECR_REGISTRY}/${FRONTEND_REPO}:${BUILD_TAG} \
                                  -t ${ECR_REGISTRY}/${FRONTEND_REPO}:latest \
                                  ./frontend

                                docker push ${ECR_REGISTRY}/${FRONTEND_REPO}:${BUILD_TAG}
                                docker push ${ECR_REGISTRY}/${FRONTEND_REPO}:latest
                            """
                        }
                    }
                }
            }
        }

        // ── 6. Trivy Image Security Scan ──────────────────────────────────────
        stage('Security Scan (Trivy)') {
            steps {
                sh """
                    trivy image --exit-code 0 --severity HIGH,CRITICAL \
                      ${ECR_REGISTRY}/${BACKEND_REPO}:${BUILD_TAG}

                    trivy image --exit-code 0 --severity HIGH,CRITICAL \
                      ${ECR_REGISTRY}/${FRONTEND_REPO}:${BUILD_TAG}
                """
            }
        }

        // ── 7. Deploy to EKS ──────────────────────────────────────────────────
        stage('Deploy to EKS') {
            steps {
                withCredentials([
                    [$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-credentials'],
                    string(credentialsId: 'rds-username',   variable: 'RDS_USERNAME'),
                    string(credentialsId: 'rds-password',   variable: 'RDS_PASSWORD'),
                    string(credentialsId: 'jwt-secret',     variable: 'JWT_SECRET_VAL'),
                    string(credentialsId: 'gemini-api-key', variable: 'GEMINI_KEY')
                ]) {
                    sh """
                        # Configure kubectl for the EKS cluster
                        aws eks update-kubeconfig \
                          --region ${AWS_REGION} \
                          --name ${EKS_CLUSTER}

                        # Fetch RDS endpoint from Terraform output
                        RDS_ENDPOINT=$(cd terraform && terraform output -raw mysql_endpoint | cut -d: -f1)

                        # Inject real account ID, build tag, and RDS endpoint into manifests
                        sed -i 's|<your-account-id>|${AWS_ACCOUNT_ID}|g' k8s/backend-deployment.yaml
                        sed -i 's|<your-account-id>|${AWS_ACCOUNT_ID}|g' k8s/frontend-deployment.yaml
                        sed -i 's|:latest|:${BUILD_TAG}|g'               k8s/backend-deployment.yaml
                        sed -i 's|:latest|:${BUILD_TAG}|g'               k8s/frontend-deployment.yaml
                        sed -i "s|<rds-endpoint>|${RDS_ENDPOINT}|g"      k8s/backend-deployment.yaml
                        sed -i "s|<rds-endpoint>|${RDS_ENDPOINT}|g"      k8s/configmap.yaml

                        # Fetch ALB hostname for frontend API URL
                        ALB_HOST=$(kubectl get ingress exam-platform-ingress \
                          -n ${K8S_NAMESPACE} \
                          -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "")
                        if [ -n "$ALB_HOST" ]; then
                          sed -i "s|http://backend-service:8081/api|http://${ALB_HOST}/api|g" k8s/frontend-deployment.yaml
                        fi

                        # Apply manifests in dependency order
                        kubectl apply -f k8s/namespace.yaml
                        kubectl apply -f k8s/configmap.yaml

                        # Create secrets from Jenkins credentials (never stored in git)
                        kubectl create secret generic mysql-secret \
                          --namespace ${K8S_NAMESPACE} \
                          --from-literal=username=${RDS_USERNAME} \
                          --from-literal=password=${RDS_PASSWORD} \
                          --save-config --dry-run=client -o yaml | kubectl apply -f -

                        kubectl create secret generic app-secret \
                          --namespace ${K8S_NAMESPACE} \
                          --from-literal=jwt-secret=${JWT_SECRET_VAL} \
                          --from-literal=gemini-api-key=${GEMINI_KEY} \
                          --save-config --dry-run=client -o yaml | kubectl apply -f -

                        kubectl apply -f k8s/backend-deployment.yaml
                        kubectl apply -f k8s/frontend-deployment.yaml
                        kubectl apply -f k8s/ingress.yaml
                        kubectl apply -f k8s/hpa.yaml

                        # Wait for rollouts to complete
                        kubectl rollout status deployment/backend-deployment  -n ${K8S_NAMESPACE} --timeout=300s
                        kubectl rollout status deployment/frontend-deployment -n ${K8S_NAMESPACE} --timeout=300s
                    """
                }
            }
        }

        // ── 8. Smoke Test ─────────────────────────────────────────────────────
        stage('Smoke Test') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                                  credentialsId: 'aws-credentials']]) {
                    sh """
                        echo "=== Pod Status ==="
                        kubectl get pods -n ${K8S_NAMESPACE}

                        echo "=== Services ==="
                        kubectl get svc -n ${K8S_NAMESPACE}

                        echo "=== Ingress ==="
                        kubectl get ingress -n ${K8S_NAMESPACE}

                        # Health check via ALB ingress (if available)
                        INGRESS_HOST=\$(kubectl get ingress exam-platform-ingress \
                          -n ${K8S_NAMESPACE} \
                          -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || true)

                        if [ -n "\$INGRESS_HOST" ]; then
                            echo "Checking health at http://\$INGRESS_HOST/api/actuator/health"
                            curl -sf --retry 5 --retry-delay 10 \
                              http://\$INGRESS_HOST/api/actuator/health || echo "Health endpoint not ready yet"
                        else
                            echo "Ingress hostname not yet assigned — skipping HTTP check"
                        fi
                    """
                }
            }
        }
    }

    // ── Post Actions ──────────────────────────────────────────────────────────
    post {
        always {
            sh """
                docker rmi ${ECR_REGISTRY}/${BACKEND_REPO}:${BUILD_TAG}  || true
                docker rmi ${ECR_REGISTRY}/${FRONTEND_REPO}:${BUILD_TAG} || true
            """
            cleanWs()
        }
        success {
            echo "✅ Deployed ${env.BUILD_TAG} to EKS cluster: ${EKS_CLUSTER}"
        }
        failure {
            echo "❌ Pipeline failed — Build: ${env.BUILD_TAG}"
            // Roll back on failure
            sh """
                aws eks update-kubeconfig --region ${AWS_REGION} --name ${EKS_CLUSTER} || true
                kubectl rollout undo deployment/backend-deployment  -n ${K8S_NAMESPACE} || true
                kubectl rollout undo deployment/frontend-deployment -n ${K8S_NAMESPACE} || true
            """ 
        }
    }
}

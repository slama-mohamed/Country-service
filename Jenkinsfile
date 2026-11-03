
pipeline {
    agent any

    options {
        timeout(time: 60, unit: 'MINUTES')
    }

    tools {
        jdk 'JDK21'
        maven 'M2_HOME'
    }

    environment {
        DOCKERHUB_USER = 'mohamedslamaa'
        IMAGE_NAME = 'country-service'

        SONAR_PROJECT_KEY = 'country-service'
        NEXUS_URL = 'http://localhost:8081'

        APP_PORT = '8081'
        HOST_PORT = '8084'

        K8S_DEPLOYMENT = 'my-country-service'
        K8S_SERVICE = 'my-country-service'
    }

    stages {

        // 1. Checkout
        stage('Checkout code') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/slama-mohamed/Country-service.git'
            }
        }

        // 2. Compile, test and package
        stage('Compile, test and package') {
            steps {
                sh '''
                    set -e
                    mvn -B clean verify
                    test -f target/country-service-0.0.1-SNAPSHOT.jar
                '''
            }
        }

        // 3. SonarQube
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('MySonarQubeServer') {
                    sh '''
                        mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.2.0.4988:sonar \
                        -Dsonar.projectKey="${SONAR_PROJECT_KEY}" \
                        -Dsonar.projectName=country-service \
                        -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                    '''
                }
            }
        }

        // 4. Deploy JAR to Nexus
        stage('Deploy JAR to Nexus') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-credentials',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASS'
                    )
                ]) {
                    sh '''
                        set +x
                        set -e

                        trap 'rm -f nexus-settings.xml' EXIT

                        cat > nexus-settings.xml <<EOF
<settings>
    <servers>
        <server>
            <id>nexus-releases</id>
            <username>${NEXUS_USER}</username>
            <password>${NEXUS_PASS}</password>
        </server>
        <server>
            <id>nexus-snapshots</id>
            <username>${NEXUS_USER}</username>
            <password>${NEXUS_PASS}</password>
        </server>
    </servers>
</settings>
EOF

                        mvn -B -s nexus-settings.xml deploy -DskipTests
                    '''
                }
            }
        }

        // 5. Upload reports to Nexus
        stage('Upload reports to Nexus') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-credentials',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASS'
                    )
                ]) {
                    sh '''
                        set +x
                        set -e

                        REPORT_URL="${NEXUS_URL}/repository/reports/country-service/${BUILD_NUMBER}"

                        if [ -f target/site/jacoco/jacoco.xml ]; then
                            curl --fail --silent --show-error \
                                --retry 2 --retry-delay 3 \
                                -u "${NEXUS_USER}:${NEXUS_PASS}" \
                                --upload-file target/site/jacoco/jacoco.xml \
                                "${REPORT_URL}/jacoco.xml"
                        fi

                        if [ -d target/surefire-reports ]; then
                            for file in target/surefire-reports/*.xml; do
                                [ -f "$file" ] || continue

                                curl --fail --silent --show-error \
                                    --retry 2 --retry-delay 3 \
                                    -u "${NEXUS_USER}:${NEXUS_PASS}" \
                                    --upload-file "$file" \
                                    "${REPORT_URL}/$(basename "$file")"
                            done
                        fi
                    '''
                }
            }
        }

        // 6. Build Docker image
        stage('Build Docker image') {
            steps {
                sh '''
                    set -e

                    docker build \
                        -t ${DOCKERHUB_USER}/${IMAGE_NAME}:${BUILD_NUMBER} \
                        -t ${DOCKERHUB_USER}/${IMAGE_NAME}:latest .

                    docker images ${DOCKERHUB_USER}/${IMAGE_NAME}
                '''
            }
        }

        // 7. Push Docker image
        stage('Push to Docker Hub') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-pwd',
                        usernameVariable: 'DOCKERHUB_CRED_USER',
                        passwordVariable: 'DOCKERHUB_PASS'
                    )
                ]) {
                    sh '''
                        set +x
                        set -e

                        echo "$DOCKERHUB_PASS" | docker login \
                            --username "$DOCKERHUB_CRED_USER" \
                            --password-stdin

                        docker push "${DOCKERHUB_USER}/${IMAGE_NAME}:${BUILD_NUMBER}"
                        docker push "${DOCKERHUB_USER}/${IMAGE_NAME}:latest"
                    '''
                }
            }
        }

        // 8. Deploy with Docker
        stage('Deploy micro-service with Docker') {
            steps {
                sh '''
                    set -e

                    docker rm -f country-service-app || true

                    docker run -d \
                        --name country-service-app \
                        --restart unless-stopped \
                        -p ${HOST_PORT}:${APP_PORT} \
                        ${DOCKERHUB_USER}/${IMAGE_NAME}:${BUILD_NUMBER}

                    docker ps --filter name=country-service-app
                '''
            }
        }

        // 9. Verify Docker deployment
        stage('Verify Docker deployment') {
            steps {
                sh '''
                    set -e

                    echo "Waiting for Spring Boot health endpoint..."

                    for i in $(seq 1 30); do
                        CODE=$(curl -s -o /dev/null -w "%{http_code}" \
                            "http://localhost:${HOST_PORT}/actuator/health" || true)

                        if [ "$CODE" = "200" ]; then
                            echo "Application is healthy."
                            exit 0
                        fi

                        sleep 2
                    done

                    echo "Application did not become healthy."
                    docker logs country-service-app || true
                    exit 1
                '''
            }
        }

        // 10. Deploy to Kubernetes
        stage('Deploy to Kubernetes') {
            steps {
                withKubeConfig([credentialsId: 'kubeconfig-file']) {
                    sh '''
                        set -e

                        echo "Deploying application to Kubernetes..."

                        sed "s|image: .*|image: ${DOCKERHUB_USER}/${IMAGE_NAME}:${BUILD_NUMBER}|" \
                            deployment.yaml | kubectl apply -f -

                        kubectl apply -f service.yaml

                        kubectl rollout status \
                            deployment/${K8S_DEPLOYMENT} \
                            --timeout=120s

                        echo "Kubernetes deployment completed."

                        kubectl get pods
                        kubectl get deployments
                        kubectl get services
                    '''
                }
            }
        }
    }

    // Post actions
    post {
        success {
            echo 'Pipeline CI/CD completed successfully!'
        }

        failure {
            echo 'Pipeline failed. Check Console Output.'
        }

        always {
            sh 'docker logout || true'
            echo 'Pipeline execution finished.'
        }
    }
}

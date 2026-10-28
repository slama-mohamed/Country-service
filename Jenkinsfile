
pipeline {
    agent any

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
    }

    stages {

        stage('Checkout code') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/slama-mohamed/Country-service.git'
            }
        }

        stage('Compile, test and package') {
            steps {
                sh 'mvn clean verify'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('MySonarQubeServer') {
                    sh '''
                        mvn org.sonarsource.scanner.maven:sonar-maven-plugin:5.2.0.4988:sonar \
                          -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                          -Dsonar.projectName=country-service \
                          -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                    '''
                }
            }
        }

        stage('Deploy jar to Nexus') {
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

                        mvn -s nexus-settings.xml deploy -DskipTests
                    '''
                }
            }
        }

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

                        if [ -f target/site/jacoco/jacoco.xml ]; then
                            curl --fail --silent --show-error \
                              -u "$NEXUS_USER:$NEXUS_PASS" \
                              --upload-file target/site/jacoco/jacoco.xml \
                              "$NEXUS_URL/repository/reports/country-service/${BUILD_NUMBER}/jacoco.xml"
                        fi

                        if [ -d target/surefire-reports ]; then
                            for file in target/surefire-reports/*.xml; do
                                [ -f "$file" ] || continue

                                curl --fail --silent --show-error \
                                  -u "$NEXUS_USER:$NEXUS_PASS" \
                                  --upload-file "$file" \
                                  "$NEXUS_URL/repository/reports/country-service/${BUILD_NUMBER}/$(basename "$file")"
                            done
                        fi
                    '''
                }
            }
        }

        stage('Build Docker image') {
            steps {
                sh '''
                    set -e

                    docker build \
                      -t ${DOCKERHUB_USER}/${IMAGE_NAME}:${BUILD_NUMBER} .

                    docker tag \
                      ${DOCKERHUB_USER}/${IMAGE_NAME}:${BUILD_NUMBER} \
                      ${DOCKERHUB_USER}/${IMAGE_NAME}:latest
                '''
            }
        }

        stage('Push to DockerHub') {
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
                          -u "$DOCKERHUB_CRED_USER" \
                          --password-stdin

                        retry_push() {
                            IMAGE="$1"
                            ATTEMPT=1

                            while [ "$ATTEMPT" -le 3 ]; do
                                echo "Push de $IMAGE - tentative $ATTEMPT/3"

                                if docker push "$IMAGE"; then
                                    echo "Push réussi : $IMAGE"
                                    return 0
                                fi

                                if [ "$ATTEMPT" -eq 3 ]; then
                                    echo "Échec du push après 3 tentatives."
                                    return 1
                                fi

                                ATTEMPT=$((ATTEMPT + 1))
                                echo "Nouvelle tentative dans 10 secondes..."
                                sleep 10
                            done
                        }

                        retry_push "${DOCKERHUB_USER}/${IMAGE_NAME}:${BUILD_NUMBER}"
                        retry_push "${DOCKERHUB_USER}/${IMAGE_NAME}:latest"

                        docker logout
                    '''
                }
            }
        }

        stage('Deploy micro-service') {
            steps {
                sh '''
                    set -e

                    docker rm -f country-service-app || true

                    docker run -d \
                      --name country-service-app \
                      --restart unless-stopped \
                      -p ${HOST_PORT}:${APP_PORT} \
                      ${DOCKERHUB_USER}/${IMAGE_NAME}:${BUILD_NUMBER}
                '''
            }
        }

        stage('Verify deployment') {
            steps {
                sh '''
                    echo "Attente du démarrage de l'application..."

                    for i in $(seq 1 30); do
                        CODE=$(curl -s -o /dev/null \
                          -w "%{http_code}" \
                          http://localhost:${HOST_PORT}/ || true)

                        if [ "$CODE" = "200" ]; then
                            echo "Le service répond avec HTTP 200 sur le port ${HOST_PORT}"
                            exit 0
                        fi

                        sleep 2
                    done

                    echo "Le service ne répond pas après 60 secondes."
                    docker logs country-service-app || true
                    exit 1
                '''
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withKubeConfig([credentialsId: 'kubeconfig-file']) {
                    sh '''
                        set -e

                        sed "s|image: .*|image: ${DOCKERHUB_USER}/${IMAGE_NAME}:${BUILD_NUMBER}|" \
                          deployment.yaml | kubectl apply -f -

                        kubectl apply -f service.yaml

                        kubectl rollout status deployment/my-country-service \
                          --timeout=120s

                        kubectl get pods
                        kubectl get svc my-country-service
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
        }

        failure {
            echo 'Pipeline failed. Check the stage logs above.'
        }

        always {
            sh 'docker logout || true'
            echo 'CI/CD pipeline execution finished.'
        }
    }
}

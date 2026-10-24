
pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'M2_HOME'
    }

    environment {
        DOCKERHUB_USER = 'mohamedslamaa'
        SONAR_PROJECT_KEY = 'country-service'
        NEXUS_URL = 'http://localhost:8081'
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
                    docker build -t ${DOCKERHUB_USER}/country-service:${BUILD_NUMBER} .
                    docker tag ${DOCKERHUB_USER}/country-service:${BUILD_NUMBER} \
                        ${DOCKERHUB_USER}/country-service:latest
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
                        echo "$DOCKERHUB_PASS" | docker login \
                            -u "$DOCKERHUB_CRED_USER" --password-stdin

                        docker push ${DOCKERHUB_USER}/country-service:${BUILD_NUMBER}
                        docker push ${DOCKERHUB_USER}/country-service:latest

                        docker logout
                    '''
                }
            }
        }

        stage('Deploy micro-service') {
            steps {
                sh '''
                    docker rm -f country-service-app || true

                    docker run -d \
                        --name country-service-app \
                        -p 8082:8081 \
                        ${DOCKERHUB_USER}/country-service:${BUILD_NUMBER}
                '''
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
            echo 'CI/CD pipeline execution finished.'
        }
    }
}


pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'M2_HOME'
    }

    environment {
        DOCKERHUB_USER = 'mohamedslamaa'
        IMAGE_NAME     = 'country-service'
        FULL_IMAGE     = "${DOCKERHUB_USER}/${IMAGE_NAME}"
    }

    stages {
        stage('Checkout code') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/slama-mohamed/Country-service.git'
            }
        }

        stage('Compile, test code, package') {
            steps {
                sh 'mvn clean install'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                        testResults: '**/target/surefire-reports/*.xml'

                    archiveArtifacts artifacts: 'target/site/jacoco/**',
                        allowEmptyArchive: true
                }
            }
        }

        
		stage('SonarQube Analysis') {
		    steps {
		        withSonarQubeEnv('MySonarQubeServer') {
		            sh '''
		                mvn org.sonarsource.scanner.maven:sonar-maven-plugin:5.2.0.4988:sonar \
		                    -Dsonar.projectKey=country-service \
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
                    sh 'mvn deploy -DskipTests'
                }
            }
        }

        stage('Upload reports to Nexus') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-creds',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASS'
                    )
                ]) {
                    sh '''
                        set -eu

                        test -d target/site/jacoco
                        test -d target/surefire-reports

                        cd target

                        tar -czf jacoco-report.tar.gz -C site jacoco
                        tar -czf surefire-reports.tar.gz surefire-reports

                        BASE="http://localhost:8081/repository/reports/country-service/${BUILD_NUMBER}"

                        curl --fail --show-error \
                            -u "$NEXUS_USER:$NEXUS_PASS" \
                            --upload-file jacoco-report.tar.gz \
                            "$BASE/jacoco-report.tar.gz"

                        curl --fail --show-error \
                            -u "$NEXUS_USER:$NEXUS_PASS" \
                            --upload-file surefire-reports.tar.gz \
                            "$BASE/surefire-reports.tar.gz"
                    '''
                }
            }
        }

        // Étapes Docker
        stage('Build Docker image') {
            steps {
                sh '''
                    docker build \
                        -t ${FULL_IMAGE}:${BUILD_NUMBER} \
                        -t ${FULL_IMAGE}:latest .
                '''
            }
        }

        stage('Push to DockerHub') {
            steps {
                withCredentials([
                    string(
                        credentialsId: 'dockerhub-pwd',
                        variable: 'DOCKERHUB_PWD'
                    )
                ]) {
                    sh '''
                        set -eu

                        echo "$DOCKERHUB_PWD" | docker login \
                            -u "$DOCKERHUB_USER" \
                            --password-stdin

                        docker push "${FULL_IMAGE}:${BUILD_NUMBER}"
                        docker push "${FULL_IMAGE}:latest"

                        docker logout
                    '''
                }
            }
        }

        stage('Deploy micro-service') {
            steps {
                sh '''
                    set -eu

                    docker rm -f country-app || true

                    docker run -d \
                        --name country-app \
                        -p 8082:8081 \
                        "${FULL_IMAGE}:${BUILD_NUMBER}"

                    sleep 15

                    docker ps --filter name=country-app
                '''
            }
        }
    }

    post {
        success {
            echo "Pipeline terminé avec succès."
            echo "Application : http://localhost:8082"
            echo "Image Docker : ${FULL_IMAGE}:${BUILD_NUMBER}"
        }

        failure {
            echo 'Pipeline en échec : consulte la Console Output.'
        }
    }
}

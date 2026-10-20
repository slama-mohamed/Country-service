pipeline {
    agent any
    tools {
        maven 'M2_HOME'
        jdk 'JDK21'
    }
    stages {
        stage('Checkout code') {
            steps {
                checkout scm
            }
        }
        stage('Compile code') {
            steps {
                sh 'mvn compile'
            }
        }
        stage('Test code') {
            steps {
                sh 'mvn test jacoco:report'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Package code') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }
        stage('SonarQube Analysis') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                    withSonarQubeEnv('MySonarQubeServer') {
                        sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.10.0.2594:sonar -Dsonar.projectKey=country-service -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml'
                    }
                }
            }
        }
        stage('Deploy to Nexus') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'nexus-credentials',
                                 usernameVariable: 'NEXUS_USER',
                                 passwordVariable: 'NEXUS_PASS')]) {
                    sh 'mvn -s settings.xml deploy -DskipTests'
                }
            }
        }
        stage('Upload reports to Nexus') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'nexus-credentials',
                                 usernameVariable: 'NEXUS_USER',
                                 passwordVariable: 'NEXUS_PASS')]) {
                    sh '''
                        tar -czf jacoco-report.tar.gz -C target/site jacoco
                        tar -czf surefire-reports.tar.gz -C target surefire-reports
                        curl -f -u "$NEXUS_USER:$NEXUS_PASS" --upload-file jacoco-report.tar.gz \
                             "http://localhost:8081/repository/reports/country-service/$BUILD_NUMBER/jacoco-report.tar.gz"
                        curl -f -u "$NEXUS_USER:$NEXUS_PASS" --upload-file surefire-reports.tar.gz \
                             "http://localhost:8081/repository/reports/country-service/$BUILD_NUMBER/surefire-reports.tar.gz"
                    '''
                }
            }
        }
    }
}

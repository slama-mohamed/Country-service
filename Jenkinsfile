pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'M2_HOME'
    }

    environment {
        NEXUS_URL = 'http://localhost:8081'
    }

    stages {
        stage('Compile, test, package') {
            steps {
                sh 'mvn clean verify'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Deploy artifact to Nexus') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'nexus-credentials',
                                                  usernameVariable: 'NEXUS_USER',
                                                  passwordVariable: 'NEXUS_PASS')]) {
                    sh 'mvn deploy -DskipTests -s settings.xml'
                }
            }
        }

        stage('Publish reports to Nexus') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'nexus-credentials',
                                                  usernameVariable: 'NEXUS_USER',
                                                  passwordVariable: 'NEXUS_PASS')]) {
                    sh '''
                        BASE="$NEXUS_URL/repository/reports/country-service/$BUILD_NUMBER"
                        tar -czf target/jacoco-report.tar.gz -C target/site jacoco
                        tar -czf target/surefire-reports.tar.gz -C target surefire-reports

                        curl -sSf -u "$NEXUS_USER:$NEXUS_PASS" \
                             --upload-file target/jacoco-report.tar.gz "$BASE/jacoco-report.tar.gz"
                        curl -sSf -u "$NEXUS_USER:$NEXUS_PASS" \
                             --upload-file target/surefire-reports.tar.gz "$BASE/surefire-reports.tar.gz"
                    '''
                }
            }
        }
    }
}

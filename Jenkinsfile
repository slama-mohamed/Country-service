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
        SONAR_PROJECT_KEY = 'country-service'
        NEXUS_URL = 'http://localhost:8081'
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

        // 6. Build, push et déploiement avec Ansible (Docker Hub + Kubernetes)
        stage('Deploy using Ansible playbook') {
            steps {
                withCredentials([
                    string(credentialsId: 'vault-pass', variable: 'VP'),
                    file(credentialsId: 'kubeconfig-file', variable: 'KUBE_FILE')
                ]) {
                    sh '''
                        set +x
                        set -e

                        trap 'rm -f .vp config' EXIT

                        cp "$KUBE_FILE" config
                        echo "$VP" > .vp

                        ansible-playbook -i hosts playbookCICD.yml \
                            --vault-password-file .vp
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'Ansible playbook executed successfully!'
        }

        failure {
            echo 'Ansible playbook execution failed! Check Console Output.'
        }

        always {
            sh 'docker logout || true'
            echo 'Pipeline execution finished.'
        }
    }
}
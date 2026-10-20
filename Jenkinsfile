pipeline {
    agent any

    tools {
        jdk 'JDK21'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Code recupere depuis GitHub'
            }
        }

        stage('Build Maven') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t my-country-service:latest .'
            }
        }
    }
}

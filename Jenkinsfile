pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'auth-service'
        PATH = "/usr/local/bin:/usr/bin:/bin:${env.PATH}"
    }

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: "git rev-parse --short HEAD",
                        returnStdout: true
                    ).trim()
                    env.BUILD_TAG = "local-${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                }
                echo "Building: ${env.BUILD_TAG}"
            }
        }

        stage('Build') {
            steps {
                echo 'Compiling application...'
                sh 'mvn clean compile -B'
            }
        }

        stage('Unit Tests') {
            steps {
                echo 'Running unit tests...'
                sh 'mvn test -B'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java'
                    )
                }
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging application...'
                sh 'mvn package -DskipTests -B'
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh 'docker version'
                    sh "docker build -t ${DOCKER_IMAGE}:${BUILD_TAG} ."
                    sh "docker tag ${DOCKER_IMAGE}:${BUILD_TAG} ${DOCKER_IMAGE}:latest"

                    echo "Docker image built successfully"
                }
            }
        }

       stage('Test Docker Image') {
           steps {
               script {
                   sh """
                   docker run --rm \
                     --entrypoint java \
                     ${DOCKER_IMAGE}:${BUILD_TAG} \
                     -version
                   """
               }
           }
       }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
            echo "Image: ${DOCKER_IMAGE}:${BUILD_TAG}"
        }
        failure {
            echo 'Pipeline failed! Check logs above.'
        }
        always {
            echo 'Cleaning workspace...'

        }
    }
}

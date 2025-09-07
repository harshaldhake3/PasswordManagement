pipeline {
    agent any

    tools {
        maven 'maven3'
    }

    environment {
        SCANNER_HOME = tool 'sonar-scanner'
        SLACK_CHANNEL = 'jenkins_builds'
        REGISTRY = "harshaladmin"
        IMAGE = "passman"
        TAG = "latest"
        COMMIT_TAG = "${env.GIT_COMMIT[0..6]}"

        // Nexus
        NEXUS_URL = "http://nexus.example.com:8081/repository/maven-releases/"

        // JFrog
        JFROG_URL = "https://yourcompany.jfrog.io/artifactory"
        JFROG_REPO = "docker-local"
    }

    stages {
        stage('Git Checkout') {
            steps {
                git branch: 'main', credentialsId: 'github', url: 'https://github.com/harshaldhake3/passman_project.git'
            }
        }

        stage('Compile') {
            steps {
                sh 'mvn compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Integration Test') {
            steps {
                sh 'mvn verify -DskipUnitTests'
            }
        }

        stage('Trivy FileSys Scan') {
            steps {
                sh 'trivy fs --format table -o fs-report.html .'
            }
        }

        stage('SonarQube Scanner Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh '''$SCANNER_HOME/bin/sonar-scanner \
                        -Dsonar.projectKey=Passman \
                        -Dsonar.projectName=Passman \
                        -Dsonar.java.binaries=target'''
                }
            }
        }

        stage('SonarQube Quality Gate') {
            steps {
                waitForQualityGate abortPipeline: false, credentialsId: 'sonarqube'
            }
        }

        stage('Build Application') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }

        stage('Publish to Nexus Repo') {
            steps {
                withMaven(globalMavenSettingsConfig: 'settings', maven: 'maven3') {
                    sh 'mvn deploy'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'dockerhub', toolName: 'docker') {
                        sh "docker build -t ${REGISTRY}/${IMAGE}:${TAG} -t ${REGISTRY}/${IMAGE}:${COMMIT_TAG} ."
                    }
                }
            }
        }

        stage('Docker Image Trivy Scan') {
            steps {
                sh "trivy image --format table -o image-report.html ${REGISTRY}/${IMAGE}:${TAG}"
            }
        }

        stage('Push Docker Image to DockerHub') {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'dockerhub', toolName: 'docker') {
                        sh "docker push ${REGISTRY}/${IMAGE}:${TAG}"
                        sh "docker push ${REGISTRY}/${IMAGE}:${COMMIT_TAG}"
                    }
                }
            }
        }

        stage('Push Docker Image to JFrog Artifactory') {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'jfrog-cred', toolName: 'docker') {
                        sh """
                        docker tag ${REGISTRY}/${IMAGE}:${TAG} ${JFROG_URL}/${JFROG_REPO}/${IMAGE}:${TAG}
                        docker tag ${REGISTRY}/${IMAGE}:${COMMIT_TAG} ${JFROG_URL}/${JFROG_REPO}/${IMAGE}:${COMMIT_TAG}
                        docker push ${JFROG_URL}/${JFROG_REPO}/${IMAGE}:${TAG}
                        docker push ${JFROG_URL}/${JFROG_REPO}/${IMAGE}:${COMMIT_TAG}
                        """
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withKubeConfig(caCertificate: '', clusterName: 'kubernetes', contextName: '', credentialsId: 'kubernetes-cp', namespace: 'webapps', restrictKubeConfigAccess: false, serverUrl: 'https://192.168.1.11:6443') {
                    sh 'kubectl apply -f k8s/production.yaml'
                }
            }
        }

        stage('Verify Deploy to Kubernetes') {
            steps {
                withKubeConfig(caCertificate: '', clusterName: 'kubernetes', contextName: '', credentialsId: 'kubernetes-cp', namespace: 'webapps', restrictKubeConfigAccess: false, serverUrl: 'https://192.168.1.11:6443') {
                    sh 'kubectl get pods -n webapps'
                    sh 'kubectl get svc -n webapps'
                }
            }
        }
    }

    post {
        always {
            script {
                def duration = currentBuild.durationString.replace(" and counting", "")
                slackSend(
                    channel: "${SLACK_CHANNEL}",
                    color: currentBuild.result == 'SUCCESS' ? 'good' : 'danger',
                    message: """
*Build Information:*
- *Project*: ${env.JOB_NAME}
- *Build Number*: #${env.BUILD_NUMBER}
- *Triggered By*: ${currentBuild.getBuildCauses()[0]?.userName ?: 'Unknown'}
- *Status*: ${currentBuild.result}
- *Duration*: ${duration}
- *Details*: <${env.BUILD_URL}|Open Build>
"""
                )
            }
        }

        success {
            slackSend(
                channel: "${SLACK_CHANNEL}",
                color: 'good',
                message: "🎉 *Build succeeded!* Project: ${env.JOB_NAME}, Build: #${env.BUILD_NUMBER} - <${env.BUILD_URL}|View Details>"
            )
        }

        failure {
            slackSend(
                channel: "${SLACK_CHANNEL}",
                color: 'danger',
                message: "❌ *Build failed!* Project: ${env.JOB_NAME}, Build: #${env.BUILD_NUMBER} - <${env.BUILD_URL}|View Details>"
            )
        }
    }
}


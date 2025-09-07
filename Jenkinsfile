pipeline {
    agent any

    tools {
        jdk 'Java'
        maven 'maven3'
    }
    
    environment {
        SCANNER_HOME = tool 'sonar-scanner'
    }

    stages {
        stage('Git Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/harshaldhake3/PasswordManagement.git'
            }
        }

        stage('MVN Compile') {
            steps {
                sh 'mvn compile'
            }
        }    

        stage('MVN Test') {
            steps {
                sh 'mvn test -DskipTests=True'
            }
        }         

        stage('Trivy Filesystem Scan') {
            steps {
                sh 'trivy fs --format table -o fs-report.html .'
            }
        }   

        stage('Sonarqube Analysis') {
            steps {
                withSonarQubeEnv('sonar-server') {
                    sh '''
                        $SCANNER_HOME/bin/sonar-scanner \
                          -Dsonar.projectName=PasswordManager \
                          -Dsonar.projectKey=PasswordManager \
                          -Dsonar.sources=src/main/java \
                          -Dsonar.java.binaries=target/classes
                    '''
                }
            }
        }

        stage('MVN Build') {
            steps {
                sh 'mvn package'
            }
        } 

        stage('Publish Artifact Nexus') {
            steps {
                withMaven(
                    globalMavenSettingsConfig: 'PasswordManager',
                    jdk: 'Java',
                    maven: 'maven3',
                    mavenSettingsConfig: '',
                    traceability: true
                ) {
                    sh 'mvn deploy'
                }
            }
        }    
        stage('Docker Build & Tag') {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'docker-login', toolName: 'docker') {
                        sh 'docker build -t harshaladmin/passwordmanager:latest .'
                    }
                }
            }
        }
        stage('Trivy Image Scan') {
            steps {
                script {
            // Optional: download DB once on agent for faster scans
//                    sh 'trivy image --download-db-only || true'

            // Scan the Docker image with timeout and severity filtering
                    sh '''
                        trivy image \
                            --skip-update \
                            --severity HIGH,CRITICAL \
                            --timeout 20m \
                            --format table \
                            -o trivy-image-scan-report.html \
                            harshaladmin/passwordmanager:latest
            '''
        }
    }
}
        stage('Docker Push') {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'docker-login', toolName: 'docker') {
                        sh 'docker push harshaladmin/passwordmanager:latest'
                    }
                }
            }
        }        
    }
}

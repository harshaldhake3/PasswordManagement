pipeline {
    agent any

    tools {
        jdk 'jdk17'
        maven 'maven3'
    }

    stages {
        stage ('Git Checkout') {
            steps {
                git branch: 'main' , url: 'https://github.com/harshaldhake3/PasswordManagement.git'
            }
        }
    }
}
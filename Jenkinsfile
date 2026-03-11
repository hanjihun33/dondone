pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        DEPLOY_DIR = '/srv/dondone/app'
        TARGET_BRANCH = 'develop'
        HEALTHCHECK_URL = 'https://dondone.duckdns.org/health'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend Test') {
            steps {
                dir('apps/dondone-backend') {
                    sh '''
                        sed -i 's/\r$//' ./gradlew
                        chmod +x ./gradlew
                        ./gradlew test --console=plain --no-daemon
                    '''
                }
            }
        }

        stage('Sync Server Repo') {
            steps {
                sh '''
                    git config --global --get-all safe.directory | grep -Fx "$DEPLOY_DIR" || git config --global --add safe.directory "$DEPLOY_DIR"
                    cd "$DEPLOY_DIR"
                    git fetch origin
                    git checkout "$TARGET_BRANCH"
                    git pull origin "$TARGET_BRANCH"
                '''
            }
        }

        stage('Compose Validate') {
            steps {
                sh '''
                    cd "$DEPLOY_DIR"
                    docker compose config > /dev/null
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    cd "$DEPLOY_DIR"
                    docker compose build api-server
                    docker compose up -d postgres redis api-server nginx
                '''
            }
        }

        stage('Health Check') {
            steps {
                sh '''
                    curl -fsS "$HEALTHCHECK_URL"
                '''
            }
        }
    }

    post {
        success {
            echo 'Deploy succeeded'
        }
        failure {
            echo 'Deploy failed'
        }
    }
}

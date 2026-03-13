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
                withCredentials([usernamePassword(credentialsId: 'gitlab-credentials', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
                    sh '''
                        git config --global --get-all safe.directory | grep -Fx "$DEPLOY_DIR" || git config --global --add safe.directory "$DEPLOY_DIR"

                        cat > /tmp/git-askpass.sh <<'EOF'
#!/bin/sh
case "$1" in
  *Username*) echo "$GIT_USERNAME" ;;
  *Password*) echo "$GIT_PASSWORD" ;;
esac
EOF
                        chmod 700 /tmp/git-askpass.sh
                        trap 'rm -f /tmp/git-askpass.sh' EXIT

                        cd "$DEPLOY_DIR"
                        export GIT_ASKPASS=/tmp/git-askpass.sh
                        export GIT_TERMINAL_PROMPT=0

                        git fetch origin
                        git checkout "$TARGET_BRANCH"
                        git pull origin "$TARGET_BRANCH"
                    '''
                }
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
                    for i in $(seq 1 12); do
                        if curl -fsS --max-time 5 "$HEALTHCHECK_URL"; then
                            echo "Health check passed"
                            exit 0
                        fi
                        echo "Attempt $i/12 failed, retrying in 5s..."
                        sleep 5
                    done
                    echo "Health check failed after 60s"
                    exit 1
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

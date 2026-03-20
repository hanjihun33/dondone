def runLogged(String scriptText) {
    sh """#!/bin/bash
set -euo pipefail
{
${scriptText}
} 2>&1 | tee -a "${env.WORKSPACE}/${env.LOG_FILE}"
"""
}

def sendMMNotify(boolean success, Map info) {
    try {
        def action = info.action ?: "Build"
        def emoji = success ? ":jenkins7:" : ":angry_jenkins:"
        def statusMsg = success ? "성공 ✅" : "실패 ❌"
        def color = success ? "#36a64f" : "#dc3545"

        def mainText = "### ${emoji} DonDone ${action} ${statusMsg}\n"
        def links = []
        if (info.mention) links << "${info.mention}"
        if (info.buildUrl) links << "[빌드 결과 확인](${info.buildUrl})"
        mainText += links.join(" ｜ ")

        def fields = []

        if (info.branch) {
            fields << [short: false, title: "Branch", value: "`${info.branch}`"]
        }

        if (info.commit) {
            fields << [short: false, title: "Commit", value: info.commit]
        }

        def buildValue = "`${env.BUILD_NUMBER}`"
        if (info.duration) buildValue += " · ${info.duration}"
        fields << [short: !success, title: "Build", value: buildValue]

        if (!success && info.failedStage) {
            fields << [short: true, title: "Failed Stage", value: "`${info.failedStage}`"]
        }

        def attachments = []
        attachments << [
            color : color,
            fields: fields
        ]

        if (!success && info.details) {
            attachments << [
                color: color,
                text : "**Error Log:**\n```text\n${info.details}\n```"
            ]
        }

        def payload = [
            username   : "Jenkins",
            icon_emoji : emoji,
            text       : mainText,
            attachments: attachments
        ]

        writeFile file: 'payload.json', text: groovy.json.JsonOutput.toJson(payload)

        withCredentials([string(credentialsId: 'mattermost-webhook', variable: 'MM_WEBHOOK')]) {
            sh '''#!/bin/bash
set +x
curl -sS -H 'Content-Type: application/json' \
  --data-binary @payload.json \
  "$MM_WEBHOOK" || true
'''
        }
    } catch (err) {
        echo "Mattermost notify failed: ${err}"
    }
}

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
        LOG_FILE = 'jenkins-console.log'
    }

    stages {
        stage('Init') {
            steps {
                script {
                    env.FAILED_STAGE = 'Init'
                    env.COMMIT_MSG = ''
                }
                writeFile file: env.LOG_FILE, text: ''
            }
        }

        stage('Checkout') {
            steps {
                script {
                    env.FAILED_STAGE = 'Checkout'
                }
                checkout scm
                script {
                    env.COMMIT_MSG = sh(script: "git log -1 --pretty=%s", returnStdout: true).trim()
                }
            }
        }

        stage('Backend Test') {
            steps {
                script {
                    env.FAILED_STAGE = 'Backend Test'
                }
                dir('apps/dondone-backend') {
                    runLogged('''
                        sed -i 's/\r$//' ./gradlew
                        chmod +x ./gradlew
                        ./gradlew test --console=plain --no-daemon
                    ''')
                }
            }
        }

        stage('Sync Server Repo') {
            steps {
                script {
                    env.FAILED_STAGE = 'Sync Server Repo'
                }
                withCredentials([usernamePassword(credentialsId: 'gitlab-credentials', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
                    runLogged('''
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
                    ''')
                }
            }
        }

        stage('Compose Validate') {
            steps {
                script {
                    env.FAILED_STAGE = 'Compose Validate'
                }
                runLogged('''
                    cd "$DEPLOY_DIR"
                    docker compose config > /dev/null
                ''')
            }
        }

        stage('Database Migrate') {
            steps {
                script {
                    env.FAILED_STAGE = 'Database Migrate'
                }
                runLogged('''
                    cd "$DEPLOY_DIR"
                    docker compose up -d postgres

                    set -a
                    . ./.env
                    set +a

                    for i in $(seq 1 12); do
                        POSTGRES_CONTAINER_ID="$(docker compose ps -q postgres || true)"
                        POSTGRES_HEALTH="unknown"

                        if [ -n "$POSTGRES_CONTAINER_ID" ]; then
                            POSTGRES_HEALTH="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' "$POSTGRES_CONTAINER_ID" || echo unknown)"
                        fi

                        echo "Postgres health attempt $i/12: $POSTGRES_HEALTH"

                        if [ "$POSTGRES_HEALTH" = "healthy" ]; then
                            break
                        fi

                        sleep 5
                    done

                    if [ "$POSTGRES_HEALTH" != "healthy" ]; then
                        docker compose ps || true
                        docker compose logs --tail=200 postgres || true
                        echo "postgres did not become healthy before migration"
                        exit 1
                    fi

                    for sql_file in deploy/sql/*.sql; do
                        [ -f "$sql_file" ] || continue
                        echo "Applying migration: $sql_file"
                        docker compose exec -T postgres \
                          psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
                          < "$sql_file"
                    done
                ''')
            }
        }

        stage('Deploy') {
            steps {
                script {
                    env.FAILED_STAGE = 'Deploy'
                }
                runLogged('''
                    cd "$DEPLOY_DIR"
                    docker compose build api-server
                    docker compose up -d postgres redis api-server
                ''')
            }
        }

        stage('Health Check') {
            steps {
                script {
                    env.FAILED_STAGE = 'Health Check'
                }
                runLogged('''
                    cd "$DEPLOY_DIR"

                    for i in $(seq 1 24); do
                        API_CONTAINER_ID="$(docker compose ps -q api-server || true)"
                        API_HEALTH="unknown"
                        API_STATUS="unknown"

                        if [ -n "$API_CONTAINER_ID" ]; then
                            API_STATUS="$(docker inspect --format '{{.State.Status}}' "$API_CONTAINER_ID" || echo unknown)"
                            API_HEALTH="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' "$API_CONTAINER_ID" || echo unknown)"
                        fi

                        echo "Attempt $i/24: api-server status=$API_STATUS health=$API_HEALTH"

                        if [ "$API_STATUS" = "exited" ] || [ "$API_STATUS" = "dead" ]; then
                            docker compose ps || true
                            docker compose logs --tail=200 api-server || true
                            echo "api-server is not running"
                            exit 1
                        fi

                        if [ "$API_HEALTH" = "unhealthy" ]; then
                            docker compose ps || true
                            docker compose logs --tail=200 api-server || true
                            echo "api-server became unhealthy"
                            exit 1
                        fi

                        if [ "$API_HEALTH" = "healthy" ]; then
                            echo "api-server is healthy"
                            break
                        fi

                        echo "Attempt $i/24 failed, retrying in 5s..."
                        sleep 5
                    done

                    API_CONTAINER_ID="$(docker compose ps -q api-server || true)"
                    API_HEALTH="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' "$API_CONTAINER_ID" || echo unknown)"

                    if [ "$API_HEALTH" != "healthy" ]; then
                        docker compose ps || true
                        docker compose logs --tail=200 api-server || true
                        echo "api-server did not become healthy after 120s"
                        exit 1
                    fi

                    docker compose up -d nginx

                    for i in $(seq 1 12); do
                        if curl -fsS --max-time 5 "$HEALTHCHECK_URL"; then
                            echo "External health check passed"
                            exit 0
                        fi
                        echo "External attempt $i/12 failed, retrying in 5s..."
                        sleep 5
                    done

                    docker compose ps || true
                    docker compose logs --tail=200 api-server nginx || true
                    echo "External health check failed after 60s"
                    exit 1
                ''')
            }
        }
    }

    post {
        success {
            script {
                def rawBranch = env.GIT_BRANCH ?: env.TARGET_BRANCH ?: "unknown"
                def branch = rawBranch.replaceFirst(/^origin\//, "")
                def duration = currentBuild.durationString.replace(' and counting', '')
                sendMMNotify(true, [
                    branch  : branch,
                    commit  : env.COMMIT_MSG ?: "",
                    duration: duration,
                    action  : "Deploy",
                    buildUrl: env.BUILD_URL
                ])
            }
            echo 'Deploy succeeded'
        }

        failure {
            script {
                def rawBranch = env.GIT_BRANCH ?: env.TARGET_BRANCH ?: "unknown"
                def branch = rawBranch.replaceFirst(/^origin\//, "")
                def duration = currentBuild.durationString.replace(' and counting', '')

                def details = ''
                try {
                    details = readFile(file: env.LOG_FILE).trim()
                    if (details.length() > 4000) {
                        details = details.substring(details.length() - 4000)
                    }
                } catch (err) {
                    details = "Error log collection failed: ${err}"
                }

                sendMMNotify(false, [
                    mention    : "@here",
                    branch     : branch,
                    commit     : env.COMMIT_MSG ?: "",
                    duration   : duration,
                    action     : "Deploy",
                    failedStage: env.FAILED_STAGE ?: "unknown",
                    details    : details,
                    buildUrl   : env.BUILD_URL
                ])
            }
            echo 'Deploy failed'
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Project Phoenix – Jenkins Declarative Pipeline
// Selenium + Maven + TestNG + Extent Reports
// ──────────────────────────────────────────────────────────────────────────────
//
// Prerequisites:
//   • Jenkins with Maven plugin (configured as 'Maven-3.9')
//   • JDK 11 configured as 'JDK-11'
//   • Google Chrome installed on the agent
//   • Credentials: PHOENIX_USERNAME, PHOENIX_PASSWORD stored in Jenkins
// ──────────────────────────────────────────────────────────────────────────────

pipeline {
    agent { label 'linux-chrome' }

    tools {
        maven 'Maven-3.9'
        jdk   'JDK-11'
    }

    environment {
        BASE_URL       = credentials('PHOENIX_BASE_URL')
        VALID_USERNAME = credentials('PHOENIX_VALID_USERNAME')
        VALID_PASSWORD = credentials('PHOENIX_VALID_PASSWORD')
        MAVEN_OPTS     = '-Xmx1024m'
    }

    options {
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '30'))
        disableConcurrentBuilds()
    }

    triggers {
        cron('H 2 * * 1-5')
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                sh 'java -version && mvn -version'
            }
        }

        // ── API Tests (GATE) ───────────────────────────────────────────────
        stage('API Tests') {
            steps {
                echo '🔌 Running API tests...'
                sh '''
                    mvn test -P api \
                        -Dbaseurl=${BASE_URL} \
                        -DapiBaseUrl=${BASE_URL} \
                        -DvalidUsername=${VALID_USERNAME} \
                        -DvalidPassword=${VALID_PASSWORD} \
                        -Dheadless=true \
                        --no-transfer-progress
                '''
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'extent-reports/**/*.html', allowEmptyArchive: true
                }
                failure {
                    error('API tests failed – blocking UI test execution.')
                }
            }
        }

        // ── UI Tests (Chrome headless, gated) ─────────────────────────────
        stage('UI Tests') {
            steps {
                echo '🖥️ Running UI tests on Chrome (headless)...'
                sh '''
                    mvn test -P ui \
                        -Dbrowser=chrome \
                        -Dheadless=true \
                        -Dbaseurl=${BASE_URL} \
                        -DvalidUsername=${VALID_USERNAME} \
                        -DvalidPassword=${VALID_PASSWORD} \
                        --no-transfer-progress
                '''
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'extent-reports/**', allowEmptyArchive: true
                }
                failure {
                    archiveArtifacts artifacts: 'extent-reports/screenshots/**', allowEmptyArchive: true
                }
            }
        }

        // ── Publish Report ─────────────────────────────────────────────────
        stage('Extent Report') {
            steps {
                echo '📊 Publishing Extent Report...'
                publishHTML(target: [
                    allowMissing         : true,
                    alwaysLinkToLastBuild: true,
                    keepAll              : true,
                    reportDir            : 'extent-reports',
                    reportFiles          : '*.html',
                    reportName           : 'Project Phoenix – Extent Report'
                ])
            }
        }
    }

    post {
        always {
            cleanWs(cleanWhenNotBuilt: false, deleteDirs: true,
                    cleanWhenSuccess: false, cleanWhenUnstable: false)
        }
        failure {
            echo '❌ Pipeline failed on branch: ' + env.BRANCH_NAME
            // slackSend channel: '#qa-alerts', color: 'danger',
            //   message: "❌ Project Phoenix failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
        }
        success {
            echo 'All tests passed!'
        }
    }
}

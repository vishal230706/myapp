pipeline {
    agent any

    tools {
        // Names defined under: Manage Jenkins -> Tools
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }

    environment {
        APP_NAME      = 'ecommerce-app'
        JAR_NAME      = 'ecommerce-app-1.0.0.jar'
        
        // Remote VM connection info
        DEPLOY_HOST   = '192.168.1.50'
        DEPLOY_USER   = 'deployer'
        DEPLOY_DIR    = '/opt/ecommerce'
        
        // SSH credentials ID from Jenkins Credentials Store
        SSH_KEY_ID    = 'deploy-ssh-credentials'
    }

    stages {
        stage('Checkout') {
            steps {
                cleanWs()
                checkout scm
            }
        }

        stage('Compile & Test') {
            steps {
                sh 'mvn clean test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package JAR') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }

        stage('Deploy over SSH') {
            steps {
                sshagent([env.SSH_KEY_ID]) {
                    sh """
                        # Create app directory if absent
                        ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} "mkdir -p ${DEPLOY_DIR}"

                        # Copy the compiled JAR artifact
                        scp -o StrictHostKeyChecking=no target/${JAR_NAME} ${DEPLOY_USER}@${DEPLOY_HOST}:${DEPLOY_DIR}/${APP_NAME}.jar

                        # Restart app systemd service on the server
                        ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} "sudo systemctl restart ${APP_NAME}.service"
                    """
                }
            }
        }

        stage('Smoke Test') {
            steps {
                sh """
                    echo "Checking health endpoint..."
                    for i in {1..12}; do
                        RESPONSE=\$(curl -s -o /dev/null -w "%{http_code}" http://${DEPLOY_HOST}:8080/actuator/health || true)
                        if [ "\$RESPONSE" -eq 200 ]; then
                            echo "Application online and healthy."
                            exit 0
                        fi
                        echo "Waiting for app to start (attempt \$i/12)..."
                        sleep 5
                    done
                    echo "Health check failed."
                    exit 1
                """
            }
        }
    }

    post {
        success {
            echo "CI/CD Pipeline finished successfully. Application is live on http://${DEPLOY_HOST}:8080/api/products"
        }
        failure {
            echo "Pipeline run failed. Check logs for details."
        }
    }
}

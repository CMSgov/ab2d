pipeline {
    environment {
        OKTA_CLIENT_ID = credentials('OKTA_CLIENT_ID')
        OKTA_CLIENT_PASSWORD = credentials('OKTA_CLIENT_PASSWORD')
        SECONDARY_USER_OKTA_CLIENT_ID = credentials('SECONDARY_USER_OKTA_CLIENT_ID')
        SECONDARY_USER_OKTA_CLIENT_PASSWORD = credentials('SECONDARY_USER_OKTA_CLIENT_PASSWORD')

        // Get code climate id
        CC_TEST_REPORTER_ID = credentials('CC_TEST_REPORTER_ID')

        // Tell e2e test that it should override the docker compose it normally uses locally
        // with docker-compose.jenkins.yml
        E2E_ENVIRONMENT = 'CI'
        AB2D_HOME="${WORKSPACE}/opt/ab2d"

        // R4 V2 endpoints enabled
        AB2D_V2_ENABLED = true

        // Number of beneficiaries to search in each PatientClaimsProcessorImpl thread - Patient batch size
        SEARCH_BENE_BATCH_SIZE=10

        ARTIFACTORY_URL = credentials('ARTIFACTORY_URL')

        AWS_DEFAULT_REGION = "us-east-1"
        ECR_REPO_ENV_AWS_ACCOUNT_NUMBER = "777200079629"
    }

    agent {
        label 'build'
    }

    tools {
        maven 'maven-3.6.3'
        jdk 'openjdk17'
    }

    stages {

        stage('Create ab2d workspace directory and copy in keystore') {
            steps {
                sh '''
                    mkdir -p "$WORKSPACE/opt/ab2d"
                '''
            }
        }

        // Make sure codeclimate is present in the environment
        stage('Download Code Coverage') {
            when { expression { return false } }
            steps {
                sh '''
                    mkdir -p codeclimate

                    if [ ! -f ./codeclimate/cc-test-reporter ]; then
                        curl -L https://codeclimate.com/downloads/test-reporter/test-reporter-latest-linux-amd64 \
                            > ./codeclimate/cc-test-reporter && chmod +x ./codeclimate/cc-test-reporter
                    fi
                   '''
            }
        }

        stage('Clean maven') {

            steps {
                sh '''
                    mvn --version
                    echo $WORKSPACE
                    mvn -U clean
                '''
            }
        }

        stage('Package without tests - Create SBOM') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'artifactoryuserpass', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
                    sh 'mvn package -s settings.xml -DskipTests -Dusername=${ARTIFACTORY_USER} -Dpassword=${ARTIFACTORY_PASSWORD} -Drepository_url=${ARTIFACTORY_URL}'
                }
            }
        }

        stage('Run unit and integration tests') {
            when { expression { return false } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'artifactoryuserpass', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
                    sh '''
                        export AB2D_EFS_MOUNT="${AB2D_HOME}"
                        mvn -s settings.xml -Dusername=${ARTIFACTORY_USER} -Dpassword=${ARTIFACTORY_PASSWORD} -Drepository_url=${ARTIFACTORY_URL} test -pl common,job,coverage,api,worker
                    '''
                }
            }
        }

        stage('Run e2e-bfd-test') {
            when { expression { return false } }
            steps {

                withCredentials([file(credentialsId: 'SANDBOX_BFD_KEYSTORE', variable: 'SANDBOX_BFD_KEYSTORE'),
                                 string(credentialsId: 'SANDBOX_BFD_KEYSTORE_PASSWORD', variable: 'AB2D_BFD_KEYSTORE_PASSWORD'),
                                 usernamePassword(credentialsId: 'artifactoryuserpass', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {

                    sh '''
                        export AB2D_BFD_KEYSTORE_LOCATION="$WORKSPACE/opt/ab2d/ab2d_bfd_keystore"

                        cp $SANDBOX_BFD_KEYSTORE $AB2D_BFD_KEYSTORE_LOCATION

                        test -f $AB2D_BFD_KEYSTORE_LOCATION && echo "created keystore file"

                        chmod 666 $AB2D_BFD_KEYSTORE_LOCATION

                        ls -la $AB2D_BFD_KEYSTORE_LOCATION

                        export AB2D_V2_ENABLED=true

                        mvn test -s settings.xml -pl e2e-bfd-test -am -Dtest=EndToEndBfdTests -DfailIfNoTests=false -Dusername=${ARTIFACTORY_USER} -Dpassword=${ARTIFACTORY_PASSWORD} -Drepository_url=${ARTIFACTORY_URL}
                    '''
                }
            }
        }
        stage('SonarQube Analysis') {
            when { expression { return false } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'artifactoryuserpass', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
                    git branch: 'master', credentialsId: 'GITHUB_AB2D_JENKINS_PAT', url: env.GIT_URL
                    git branch: env.BRANCH_NAME, credentialsId: 'GITHUB_AB2D_JENKINS_PAT', url: env.GIT_URL
                    // Automatically saves the an id for the SonarQube build
                    withSonarQubeEnv('CMSSonar') {
                        sh '''
                            mvn -s settings.xml sonar:sonar -Dsonar.projectKey=ab2d-project -DskipTests -Dusername=${ARTIFACTORY_USER} -Dpassword=${ARTIFACTORY_PASSWORD} -Drepository_url=${ARTIFACTORY_URL}
                        '''
                    }
                }
            }
        }

        // New Way in declarative pipeline
        stage("Quality Gate") {
            when { expression { return false } }
            options {
                timeout(time: 10, unit: 'MINUTES')
            }
            steps {
                // Parameter indicates whether to set pipeline to UNSTABLE if Quality Gate fails
                // true = set pipeline to UNSTABLE, false = don't
                waitForQualityGate abortPipeline: true
            }
        }


        stage('Run e2e-test') {

            steps {

                withCredentials([file(credentialsId: 'SANDBOX_BFD_KEYSTORE', variable: 'SANDBOX_BFD_KEYSTORE'),
                                 string(credentialsId: 'SANDBOX_BFD_KEYSTORE_PASSWORD', variable: 'AB2D_BFD_KEYSTORE_PASSWORD'),
                                 usernamePassword(credentialsId: 'artifactoryuserpass', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {

                    sh '''
                        export AB2D_BFD_KEYSTORE_LOCATION="/opt/ab2d/ab2d_bfd_keystore"

                        export KEYSTORE_LOCATION="$WORKSPACE/opt/ab2d/ab2d_bfd_keystore"

                        export JENKINS_UID=$(id -u)
                        export JENKINS_GID=$(id -g)

                        cp $SANDBOX_BFD_KEYSTORE $KEYSTORE_LOCATION

                        test -f $KEYSTORE_LOCATION && echo "created keystore file"

                        chmod 666 $KEYSTORE_LOCATION

                        ls -la $KEYSTORE_LOCATION

                        # Log into ECR for Docker Compose in e2e tests
                        aws --region "${AWS_DEFAULT_REGION}" ecr get-login-password |
                          docker login --username AWS --password-stdin "${ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"

                        mvn test -s settings.xml -pl e2e-test -am -Dtest=TestRunner -DfailIfNoTests=false -Dusername=${ARTIFACTORY_USER} -Dpassword=${ARTIFACTORY_PASSWORD} -Drepository_url=${ARTIFACTORY_URL}
                    '''
                }
            }
        }

        stage('Run codeclimate tests') {
            when { expression { return false } }
            steps {
                sh '''
                    export JACOCO_SOURCE_PATH=./api/src/main/java
                    ./codeclimate/cc-test-reporter format-coverage ./api/target/site/jacoco/jacoco.xml --input-type jacoco -o codeclimate.api.json

                    export JACOCO_SOURCE_PATH=./common/src/main/java
                   ./codeclimate/cc-test-reporter format-coverage ./common/target/site/jacoco/jacoco.xml --input-type jacoco -o codeclimate.common.json

                    export JACOCO_SOURCE_PATH=./job/src/main/java
                   ./codeclimate/cc-test-reporter format-coverage ./job/target/site/jacoco/jacoco.xml --input-type jacoco -o codeclimate.job.json

                   export JACOCO_SOURCE_PATH=./coverage/src/main/java
                   ./codeclimate/cc-test-reporter format-coverage ./coverage/target/site/jacoco/jacoco.xml --input-type jacoco -o codeclimate.coverage.json

                    export JACOCO_SOURCE_PATH=./worker/src/main/java
                    ./codeclimate/cc-test-reporter format-coverage ./worker/target/site/jacoco/jacoco.xml --input-type jacoco -o codeclimate.worker.json
                '''
            }
        }

        stage('Cleanup - first pass of docker deletions part 1') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                    sh '''
                      docker volume ls -qf dangling=true | xargs -I name docker volume rm name
                      docker ps -aq | xargs -I name docker rm --force name
                    '''
                }
            }
        }

        stage('Cleanup - first pass of docker deletions part 2') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                    sh '''
                      docker images | grep _api | awk '{print $3}' | xargs -I name docker rmi --force name
                      docker images | grep _worker | awk '{print $3}' | xargs -I name docker rmi --force name
                    '''
                }
            }
        }

        stage('Cleanup - second pass of docker deletions') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                    sh '''
                      docker volume ls -qf dangling=true | xargs -I name docker volume rm name
                      docker images | grep _api | awk '{print $3}' | xargs -I name docker rmi --force name
                      docker images | grep _worker | awk '{print $3}' | xargs -I name docker rmi --force name
                    '''
                }
            }
        }

        stage('Cleanup - delete all but the defaut docker networks') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                    sh '''
                      # Delete all but the defaut docker networks
                      docker network ls | awk '{print $1, $2}' | grep -v " bridge" | grep -v " host" | grep -v " none" \
		        | grep -v "NETWORK ID" | awk '{print $1}' | xargs -I name docker network rm name
                    '''
                }
            }
        }

    }

    post {
        always {
            script {
                sh '''
                  rm -rf "$WORKSPACE/opt/ab2d" 2> /dev/null
                  rm -rf "$WORKSPACE/.m2/repository/gov/cms/ab2d" 2> /dev/null
                '''
            }
        }
    }

}

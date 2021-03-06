pipeline {
    environment {
        OKTA_CLIENT_ID = credentials('OKTA_CLIENT_ID')
        OKTA_CLIENT_PASSWORD = credentials('OKTA_CLIENT_PASSWORD')
        SECONDARY_USER_OKTA_CLIENT_ID = credentials('SECONDARY_USER_OKTA_CLIENT_ID')
        SECONDARY_USER_OKTA_CLIENT_PASSWORD = credentials('SECONDARY_USER_OKTA_CLIENT_PASSWORD')

        AB2D_HICN_HASH_PEPPER = credentials('HICN_HASH_PEPPER')
        AB2D_HICN_HASH_ITER = 1000

        AB2D_CLAIMS_SKIP_BILLABLE_PERIOD_CHECK = true

        // Get code climate id
        CC_TEST_REPORTER_ID = credentials('CC_TEST_REPORTER_ID')

        // HPMS key id and secret
        HPMS_AUTH_KEY_ID = credentials('HPMS_AUTH_KEY_ID')
        HPMS_AUTH_KEY_SECRET = credentials('HPMS_AUTH_KEY_SECRET')

        // Tell e2e test that it should override the docker compose it normally uses locally
        // with docker-compose.jenkins.yml
        E2E_ENVIRONMENT = 'CI'
        AB2D_HOME="${WORKSPACE}/opt/ab2d"

        // R4 V2 endpoints enabled
        AB2D_V2_ENABLED = true
    }

    agent {
        label 'build'
    }

    tools {
        maven 'maven-3.6.3'
        jdk 'adoptjdk13'
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

                    mvn clean
                '''
            }
        }

        stage('Package without tests') {

            steps {
                sh 'mvn package -DskipTests'
            }
        }

        stage('Run unit and integration tests') {

            steps {
                sh '''
                    export AB2D_EFS_MOUNT="${AB2D_HOME}"
                    mvn test -pl eventlogger,fhir,common,api,worker,bfd,filter,audit,hpms
                '''
            }
        }

        stage('Run e2e-patient-test') {

            steps {

                withCredentials([file(credentialsId: 'SANDBOX_BFD_KEYSTORE', variable: 'SANDBOX_BFD_KEYSTORE'),
                            string(credentialsId: 'SANDBOX_BFD_KEYSTORE_PASSWORD', variable: 'AB2D_BFD_KEYSTORE_PASSWORD')]) {

                    sh '''
                        export AB2D_BFD_KEYSTORE_LOCATION="$WORKSPACE/opt/ab2d/ab2d_bfd_keystore"

                        cp $SANDBOX_BFD_KEYSTORE $AB2D_BFD_KEYSTORE_LOCATION

                        test -f $AB2D_BFD_KEYSTORE_LOCATION && echo "created keystore file"

                        chmod 666 $AB2D_BFD_KEYSTORE_LOCATION

                        ls -la $AB2D_BFD_KEYSTORE_LOCATION

                        export AB2D_V2_ENABLED=false

                        mvn test -pl e2e-patient-test -am -Dtest=PatientEndpointTests -DfailIfNoTests=false
                    '''
                }
            }
        }

        stage('Run e2e-test') {

            steps {

                withCredentials([file(credentialsId: 'SANDBOX_BFD_KEYSTORE', variable: 'SANDBOX_BFD_KEYSTORE'),
                            string(credentialsId: 'SANDBOX_BFD_KEYSTORE_PASSWORD', variable: 'AB2D_BFD_KEYSTORE_PASSWORD')]) {

                    sh '''
                        export AB2D_BFD_KEYSTORE_LOCATION="/opt/ab2d/ab2d_bfd_keystore"

                        export KEYSTORE_LOCATION="$WORKSPACE/opt/ab2d/ab2d_bfd_keystore"

                        export JENKINS_UID=$(id -u)
                        export JENKINS_GID=$(id -g)

                        cp $SANDBOX_BFD_KEYSTORE $KEYSTORE_LOCATION

                        test -f $KEYSTORE_LOCATION && echo "created keystore file"

                        chmod 666 $KEYSTORE_LOCATION

                        ls -la $KEYSTORE_LOCATION

                        mvn test -pl e2e-test -am -Dtest=TestRunner -DfailIfNoTests=false
                    '''
                }
            }
        }

        stage('Run codeclimate tests') {

            steps {
                sh '''
                    export JACOCO_SOURCE_PATH=./api/src/main/java
                    ./codeclimate/cc-test-reporter format-coverage ./api/target/site/jacoco/jacoco.xml --input-type jacoco -o codeclimate.api.json

                    export JACOCO_SOURCE_PATH=./fhir/src/main/java
                    ./codeclimate/cc-test-reporter format-coverage ./fhir/target/site/jacoco/jacoco.xml --input-type jacoco -o codeclimate.fhir.json

                    export JACOCO_SOURCE_PATH=./audit/src/main/java
                    ./codeclimate/cc-test-reporter format-coverage ./audit/target/site/jacoco/jacoco.xml --input-type jacoco -o codeclimate.audit.json

                    export JACOCO_SOURCE_PATH=./common/src/main/java
                   ./codeclimate/cc-test-reporter format-coverage ./common/target/site/jacoco/jacoco.xml --input-type jacoco -o codeclimate.common.json

                    export JACOCO_SOURCE_PATH=./filter/src/main/java
                    ./codeclimate/cc-test-reporter format-coverage ./filter/target/site/jacoco/jacoco.xml --input-type jacoco -o codeclimate.filter.json

                    export JACOCO_SOURCE_PATH=./hpms/src/main/java
                    ./codeclimate/cc-test-reporter format-coverage ./hpms/target/site/jacoco/jacoco.xml --input-type jacoco -o codeclimate.hpms.json

                    export JACOCO_SOURCE_PATH=./bfd/src/main/java
                    ./codeclimate/cc-test-reporter format-coverage ./bfd/target/site/jacoco/jacoco.xml --input-type jacoco -o codeclimate.bfd.json

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
                  rm -rf target **/target 2> /dev/null
                '''
            }
        }
    }

}

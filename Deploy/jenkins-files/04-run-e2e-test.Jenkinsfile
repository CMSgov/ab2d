pipeline {
  parameters {
    string(
      defaultValue: "PROD",
      description: '',
      name: 'E2E_TARGET_ENV'
    )
    string(
      defaultValue: "",
      description: '',
      name: 'OKTA_CONTRACT_NUMBER'
    )
    string(
      defaultValue: "",
      description: '',
      name: 'OKTA_CLIENT_ID'
    )
    string(
      defaultValue: "",
      description: '',
      name: 'OKTA_CLIENT_PASSWORD'
    )
    string(
      defaultValue: "",
      description: '',
      name: 'SECONDARY_USER_OKTA_CLIENT_ID'
    )
    string(
      defaultValue: "",
      description: '',
      name: 'SECONDARY_USER_OKTA_CLIENT_PASSWORD'
    )
  }
  agent {
    node {
      label 'agent01'
      customWorkspace 'workspace/production/03-run-chef-inspec-compliance-checks-for-production'
    }
  }
  stages {
    stage('Clear the working directory') {
      steps {
        script {
          dir(env.WORKSPACE) {
            sh 'rm -rf *'
          }
        }
      }
    }
    stage('Clone ab2d repo') {
      steps {
        script {
          sh 'mkdir -p ab2d; cd ab2d'
          dir ('ab2d') {
            git branch: 'feature/ab2d-586-automate-splunk-integration',
            url: 'https://github.com/CMSgov/ab2d.git'
          }
        }
      }
    }
    stage('build ab2d') {
      steps {
        script {
          dir ('ab2d') {
            sh 'mvn clean package -DskipTests'
          }
        }
      }
    }
    stage('run e2e tests') {
      steps {
        script {
	  dir ('ab2d/e2e-test/target') {
	    sh 'java -cp e2e-test-0.0.1-SNAPSHOT-fat-tests.jar gov.cms.ab2d.e2etest.TestLauncher "${E2E_TARGET_ENV}" "${OKTA_CONTRACT_NUMBER}"'
	  }
	}
      }
    }
  }
}
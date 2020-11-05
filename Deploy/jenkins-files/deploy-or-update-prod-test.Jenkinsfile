pipeline {
  agent {
    label 'deployment'
  }
  stages {
    stage('Deploy IAM components for Prod Test') {
      steps {
        script {
	  dir ('Deploy/bash') {
	    sh '''
	      ./run-test-iam-automation.sh
	    sh '''
	  }
	}
      }
    }
  }
  post {
    always {
      script {
        dir('Deploy/bash') {
	  // Cleanup
	  sh '''
            ./run-test-automation-cleanup.sh
	  sh '''
        }
      }
    }
  } 
}

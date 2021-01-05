pipeline {
  environment {
    TARGET_AWS_ACCOUNT_NUMBER_PARAM = credentials("${TARGET_AWS_ACCOUNT_NUMBER_PARAM}")
    CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER_PARAM = credentials("${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER_PARAM}")
    DATABASE_SECRET_DATETIME_PARAM = credentials("${DATABASE_SECRET_DATETIME_PARAM}")
  }
  agent {
    label 'deployment'
  }
  stages {
    stage('Deploy or update application') {
      steps {
        script {
	      dir ('Deploy/bash') {
	        sh '''
	          ./deploy-application.sh
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
            # cleanup
	      sh '''
        }
      }
    }
  }
}

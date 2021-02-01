pipeline {
  environment {
    AWS_ACCOUNT_NUMBER_PARAM = credentials("${AWS_ACCOUNT_NUMBER_PARAM}")
    CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER_PARAM = credentials("${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER_PARAM}")
    DATABASE_SECRET_DATETIME_PARAM = credentials("${DATABASE_SECRET_DATETIME_PARAM}")
  }
  agent {
    label 'deployment'
  }
  stages {
    stage('Deploy S3 terraform backend') {
      steps {
        script {
	      dir ('Deploy/bash') {
	        sh '''
	          ./deploy-s3-terraform-backend.sh
	        sh '''
	      }
	    }
      }
    }
    stage('Deploy or update core module') {
      steps {
        script {
	      dir ('Deploy/bash') {
	        sh '''
	          ./deploy-core-module.sh
	        sh '''
	      }
	    }
      }
    }
    stage('Deploy or update data module') {
      steps {
        script {
	      dir ('Deploy/bash') {
	        sh '''
	          ./deploy-data-module.sh
	        sh '''
	      }
	    }
      }
    }
    stage('Deploy or update worker module') {
      steps {
        script {
	      dir ('Deploy/bash') {
	        sh '''
	          ./deploy-worker-module.sh
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

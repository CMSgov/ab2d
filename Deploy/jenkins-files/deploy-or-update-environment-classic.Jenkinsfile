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
    stage('Initialize or verify base environment') {
      when {
        expression { params.UPDATE_BASE_ENVIRONMENT == 'true' }
      }
      steps {
        script {
	      dir ('Deploy/bash') {
	        sh '''
	          ./initialize-environment.sh
	        sh '''
	      }
	    }
      }
    }
    stage('Deploy or update gold disk') {
      when {
        expression { params.UPDATE_GOLD_DISK == 'true' }
      }
      steps {
        script {
	      dir ('Deploy/bash') {
	        sh '''
	          ./update-gold-disk.sh
	        sh '''
	      }
	    }
      }
    }
    stage('Deploy or update core infrastructure') {
      when {
        expression { params.UPDATE_CORE_INFRASTRUCTURE == 'true' }
      }
      steps {
        script {
	      dir ('Deploy/bash') {
	        sh '''
	          ./deploy-infrastructure.sh
	        sh '''
	      }
	    }
      }
    }
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

pipeline {
  environment {
    DATABASE_SECRET_DATETIME_PARAM = credentials("${DATABASE_SECRET_DATETIME_PARAM}")
    EXCLUDE_PROD_CMS_ENV_AS_TARGET_OF_DATABASE_MIGRATION_PARAM =
      credentials("${EXCLUDE_PROD_CMS_ENV_AS_TARGET_OF_DATABASE_MIGRATION_PARAM}")
    EXCLUDE_SBX_CMS_ENV_AS_TARGET_OF_DATABASE_MIGRATION_PARAM =
      credentials("${EXCLUDE_SBX_CMS_ENV_AS_TARGET_OF_DATABASE_MIGRATION_PARAM}")
    SOURCE_AWS_ACCOUNT_NUMBER_PARAM = credentials("${SOURCE_AWS_ACCOUNT_NUMBER_PARAM}")
    TARGET_AWS_ACCOUNT_NUMBER_PARAM = credentials("${TARGET_AWS_ACCOUNT_NUMBER_PARAM}")
  }
  agent {
    label 'deployment'
  }
  stages {
    stage('Get required data as CSV files from Production') {
      steps {
        script {
	      dir ('Deploy/bash') {
	        sh '''
	          ./backup-database-as-csv.sh
	        sh '''
	      }
	    }
      }
    }
    stage('Reconcile databases') {
      steps {
        script {
	      dir ('Deploy/bash') {
	        sh '''
	          ./reconcile-databases.sh
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

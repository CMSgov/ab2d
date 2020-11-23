pipeline {
  environment {
    DATABASE_SECRET_DATETIME_PARAM = credentials("${DATABASE_SECRET_DATETIME_PARAM}")
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
    stage('Import CSV files into a temporary schema in Production Validation') {
      steps {
        script {
	      dir ('Deploy/bash') {
	        sh '''
	          ./restore-csv-data-to-temporary-schema.sh
	        sh '''
	      }
	    }
      }
    }
    stage('Reconcile Production Validation data with Production data') {
      steps {
        script {
	      dir ('Deploy/bash') {
	        sh '''
	          ./reconcile-prod-validation-with-prod.sh
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

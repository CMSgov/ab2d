pipeline {
  environment {
    DATABASE_SECRET_DATETIME_PARAM = credentials("${DATABASE_SECRET_DATETIME_PARAM}")
    TARGET_AWS_ACCOUNT_NUMBER_PARAM = credentials("${TARGET_AWS_ACCOUNT_NUMBER_PARAM}")
  }
  parameters {
    string(name: 'API_URL_PREFIX_PARAM', defaultValue: '', description: '')
    string(name: 'CLOUD_TAMER_PARAM', defaultValue: 'false', description: '')
    string(name: 'CONTRACT_NUMBER_PARAM', defaultValue: '', description: '')
    credentials(name: 'DATABASE_SECRET_DATETIME_PARAM', description: '', defaultValue: '', required: true )
    credentials(name: 'TARGET_AWS_ACCOUNT_NUMBER_PARAM', description: '', defaultValue: '', required: true )
    string(name: 'TARGET_CMS_ENV_PARAM', defaultValue: '', description: '')
  }
  agent {
    label 'deployment'
  }
  stages {
    stage('Start job via record insert') {
      steps {
        script {
	      dir ('Deploy/bash') {
	        sh '''
	          ./start-job-via-record-insert.sh
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

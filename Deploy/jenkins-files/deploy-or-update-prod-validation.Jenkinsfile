pipeline {
  agent {
    label 'deployment'
  }
  stages {
    stage('Deploy or update core module') {
      steps {
        script {
	      dir ('Deploy/bash') {
	        sh '''
	          ../deploy-core-module.sh
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
	          ../deploy-data-module.sh
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
	          ../deploy-worker-module.sh
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

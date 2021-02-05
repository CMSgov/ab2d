pipeline {
  agent {
    label 'deployment'
  }
  stages {
    stage('Deploy or update static website') {
      steps {
        script {
	      dir ('Deploy/bash') {
	        sh '''
	          ./deploy-or-update-website-akamai.sh
	        sh '''
	      }
	    }
      }
    }
  }
  post {
    always {
      script {
        dir('website') {
          // Cleanup
          sh '''
            rm -rf _site
          sh '''
        }
      }
    }
  }
}

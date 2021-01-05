pipeline {
  environment {
    AUTH_ENV = credentials("${AUTH}")
  }
  agent {
    label 'deployment'
  }
  stages {
    stage('Delete process files (if exist)') {
      steps {
        script {
	  dir ('examples/bash') {
	    sh '''
	      rm -f jobId.txt
	      rm -f response.json
	    sh '''
	  }
	}
      }
    }
    stage('Run and monitor export job') {
      steps {
        script {
          dir ('examples/bash') {
	    sh '''
              source ./bootstrap.sh -prod --auth $AUTH_ENV --directory .
	      ./start-job.sh
	      ./monitor-job.sh
	    sh '''
          }
        }
      }
    }
  }
  post {
    always {
      script {
        dir('examples/bash') {
	  // Delete process files (if exist)
	  sh '''
            rm -f jobId.txt
	    rm -f response.json
	  sh '''
        }
      }
    }
  } 
}

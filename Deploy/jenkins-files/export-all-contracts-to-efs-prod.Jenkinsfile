pipeline {
  environment {
    AUTH_ENV = credentials("${AUTH}")
  }
  agent {
    label 'deployment'
  }
  stages {
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
}

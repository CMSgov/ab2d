pipeline {
  agent {
    label 'deployment'
  }
  stages {
    stage('Run and monitor export job') {
      steps {
        script {
          dir ('examples/bash') {
	    sh '''
              source ./bootstrap.sh -prod --auth $AUTH --directory .
	      ./start-job.sh
	      ./monitor-job.sh
	    sh '''
          }
        }
      }
    }
  }
}

pipeline {
  environment {
    AUTH_ENV = credentials("${AUTH}")
  }
  agent {
    label 'deployment'
  }
  stages {
    stage('Bootstrap export job') {
      steps {
        script {
          dir ('examples/bash') {
	    sh '''
              source ./bootstrap.sh -prod --auth $AUTH_ENV --directory .
	    sh '''
          }
        }
      }
    }
    stage('Start export job') {
      steps {
        script {
          dir ('examples/bash') {
	    sh '''
              ./start-job.sh
	    sh '''
          }
        }
      }
    }
    stage('Monitor job') {
      steps {
        script {
          dir ('examples/bash') {
	    sh '''
              ./monitor-job.sh
	    sh '''
          }
        }
      }
    }
  }
}

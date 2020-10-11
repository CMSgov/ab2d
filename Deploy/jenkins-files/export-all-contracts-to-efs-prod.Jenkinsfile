pipeline {
  agent {
    label 'deployment'
  }
  stages {
    stage('Bootstrap the process') {
      steps {
        script {
          dir ('examples/bash') {
            sh 'source ./bootstrap.sh -prod --auth $AUTH --directory .'
          }
        }
      }
    }
    stage('Start export job') {
      steps {
        script {
          dir ('examples/bash') {
            sh './start-job.sh'
          }
        }
      }
    }
    stage('Monitor job') {
      steps {
        script {
          dir ('examples/bash') {
            sh './monitor-job.sh'
          }
        }
      }
    }    
  }
}

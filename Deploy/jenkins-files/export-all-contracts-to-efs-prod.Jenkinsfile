pipeline {
  agent {
    label 'deployment'
  }
  stages {
    stage('Create working directory') {
      steps {
        script {
          dir('.') {
	    sh 'rm -rf ./export-all-contracts-to-efs-prod/$WORKSPACE_DIR'
	    sh 'mkdir -p ./export-all-contracts-to-efs-prod/$WORKSPACE_DIR'
          }
        }
      } 
    }
    stage('Bootstrap the process') {
      steps {
        script {
          dir ('examples/bash') {
            sh 'source ./bootstrap.sh -prod --auth $AUTH --directory ./export-all-contracts-to-efs-prod/$WORKSPACE_DIR'
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
  post {
    always {
      script {
        dir('.') {
          sh 'rm -rf ./export-all-contracts-to-efs-prod/$WORKSPACE_DIR'
        }
      }
    }
  }
}

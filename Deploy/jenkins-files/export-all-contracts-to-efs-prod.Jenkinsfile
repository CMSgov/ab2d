pipeline {
  agent {
    label 'deployment'
    node {
      customWorkspace "workspace/export-all-contracts-to-efs-prod/${params.WORKSPACE_DIR}"
    }
  }
  stages {
    stage('Clear the working directory') {
      steps {
        script {
          dir(env.WORKSPACE) {
            sh 'rm -rf *'
          }
        }
      } 
    }
    stage('Bootstrap the process') {
      steps {
        script {
          dir ('examples/bash') {
            sh 'source ./bootstrap.sh -prod --auth $params.AUTH --directory .'
          }
        }
      }
    }
    stage('Start export job') {
      steps {
        script {
          dir ('examples/bash') {
            ./start-job.sh
          }
        }
      }
    }
    stage('Monitor job') {
      steps {
        script {
          dir ('examples/bash') {
            ./monitor-job.sh
          }
        }
      }
    }
    post {
      always {
        script {
          dir(env.WORKSPACE) {
            sh 'rm -rf *'
          }
        }
      }
    }
}

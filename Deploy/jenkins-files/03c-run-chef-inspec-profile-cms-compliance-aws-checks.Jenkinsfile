pipeline {
  parameters {
    string(
      defaultValue: "ab2d-east-prod",
      description: 'Corresponds to the environment associated with an AWS account..',
      name: 'CMS_ENV_PARAM'
    )
    string(
      defaultValue: "false",
      description: 'Corresponds to whether the CloudTamer API should be used.',
      name: 'CLOUD_TAMER_PARAM'
    )
  }
  agent {
    node {
      label 'agent01'
      customWorkspace 'workspace/production/03-run-chef-inspec-compliance-checks-for-production'
    }
  }
  triggers {
    cron('@midnight')
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
    stage('Clone ab2d repo') {
      steps {
        script {
          sh 'mkdir -p ab2d; cd ab2d'
          dir ('ab2d') {
            git branch: 'feature/ab2d-586-automate-splunk-integration',
            url: 'https://github.com/CMSgov/ab2d.git'
          }
        }
      }
    }
    stage('Clone cms-ars-3.1-moderate-aws-foundations-cis-overlay repo') {
      steps {
        script {
          sh 'mkdir -p profiles/cms-ars-3.1-moderate-aws-foundations-cis-overlay; cd profiles'
          dir ('profiles/cms-ars-3.1-moderate-aws-foundations-cis-overlay') {
            git branch: 'master',
            credentialsId: 'GITHUB_CMS_GOV_HV7K_PAT',
            url: 'https://github.cms.gov/ISPG/cms-ars-3.1-moderate-aws-foundations-cis-overlay.git'
          }
        }
      }
    }
    stage('Clone cis-aws-foundations-baseline repo') {
      steps {
        script {
          sh 'mkdir -p profiles/cis-aws-foundations-baseline; cd profiles'
          dir ('profiles/cis-aws-foundations-baseline') {
            git branch: 'master',
            url: 'https://github.com/mitre/cis-aws-foundations-baseline.git'
          }
        }
      }
    }
    stage('Perform Inspec Analysis on AWS') {
      steps {
        script {
          dir ('ab2d/Deploy/bash') {
            sh './run-inspec-for-aws-using-cms-inspec-profile.sh'
          }
        }
      }
    }
  }
}

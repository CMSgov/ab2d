pipeline {
    agent {
        node {
            label 'agent01'
            customWorkspace 'workspace/inspec'
        }
    }
    triggers {
      cron('@midnight')
    }
    stages {
        stage('Clone cms-ars-3.1-moderate-red-hat-enterprise-linux-7-stig-overlay repo') {
            steps {
                script {
                    sh 'mkdir -p profiles/cms-ars-3.1-moderate-red-hat-enterprise-linux-7-stig-overlay; cd profiles'
                    dir ('profiles/cms-ars-3.1-moderate-red-hat-enterprise-linux-7-stig-overlay') {
                        git branch: 'master',
                        credentialsId: 'GITHUB_CMS_GOV_HV7K_PAT',
                        url: 'https://github.cms.gov/ISPG/cms-ars-3.1-moderate-red-hat-enterprise-linux-7-stig-overlay.git'
                    }
                }
            }
        }
        stage('Clone inspec-profile-disa_stig-el7 repo') {
            steps {
                script {
                    sh 'mkdir -p profiles/inspec-profile-disa_stig-el7; cd profiles'
                    dir ('profiles/inspec-profile-disa_stig-el7') {
                        git branch: 'master',
                        credentialsId: 'GITHUB_CMS_GOV_HV7K_PAT',
                        url: 'https://github.cms.gov/ISPG/inspec-profile-disa_stig-el7.git'
                    }                    
                }
            }
        }
        stage('Install requiried Ruby gems') {
            steps {
                script {
                    dir ('profiles/cms-ars-3.1-moderate-red-hat-enterprise-linux-7-stig-overlay') {
                        sh 'bundle install'
                    }
                    dir ('profiles/inspec-profile-disa_stig-el7') {
                        sh 'bundle install'
                    }
                }
            }
        }
    }
}

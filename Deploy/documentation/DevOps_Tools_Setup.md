# DevOps Tools Setup

## Table of Contents

1. [Prepare development machine](#prepare-development-machine)
   * [Configue HomeBrew](#configue-homebrew)
   * [Install python3](#install-python3)
   * [Configure pip3](#configure-pip3)
   * [Configure development CloudTamer credentials](#configure-development-cloudtamer-credentials)
   * [Verify that you can get logs from api and worker nodes](#verify-that-you-can-get-logs-from-api-and-worker-nodes)
   * [Verify that you can connect to api and worker nodes](#verify-that-you-can-connect-to-api-and-worker-nodes)
   * [Verify that you can set AWS environment variables](#verify-that-you-can-set-aws-environment-variables)
1. [Bookmark Jenkins master](#bookmark-jenkins-master)
   
## Prepare development machine

### Configue HomeBrew

1. Verify that HomeBrew is installed by checking the HomeBrew version

   1. Enter the following

      ```ShellSession
      $ brew --version
      ```

   1. If the version is not displayed, stop here and contact Lonnie about next steps

1. Ensure that you have ownership of homebrew

   ```ShellSession
   $ sudo chown -R $(whoami) \
     /usr/local/var/homebrew \
     /usr/local/homebrew
   ```

1. Ensure that you have ownership of dependent directories

   ```ShellSession
   $ sudo chown -R $(whoami) \
     /usr/local/etc/bash_completion.d \
     /usr/local/lib/pkgconfig \
     /usr/local/share/aclocal \
     /usr/local/share/doc \
     /usr/local/share/man \
     /usr/local/share/man/man1 \
     /usr/local/share/man/man8 \
     /usr/local/share/zsh \
     /usr/local/share/zsh/site-functions
   ```

### Install python3

1. Determine if python3 is already installed

   ```ShellSession
   $ python3 --version
   ```
   
1. If python3 is not installed, do the following:

   ```ShellSession
   $ brew install python3
   ```

### Configure pip3

1. Upgrade pip3

   *Ignore the incompatible warnings.*
   
   ```ShellSession
   $ pip3 install --upgrade pip
   ```

1. Install lxml

   *Note that "lxml" is a library for parsing XML and HTML.*
   
   ```ShellSession
   $ pip3 install lxml
   ```

1. Install requests

   ```ShellSession
   $ pip3 install requests
   ```

1. Install boto3

   ```ShellSession
   $ pip3 install boto3
   ```

### Configure development CloudTamer credentials

1. Backup the file that you use for setting up your shell's environment

   *Example for bash:*

   ```ShellSession
   $ cp ~/.bash_profile ~/.bash_profile_backup
   ```

1. Set CloudTamer user name

   *Format:*

   ```ShellSession
   TEMP_CLOUDTAMER_USER_NAME={your eua id}
   TEMP_CLOUDTAMER_PASSWORD={your cloudtamer password}
   ```

1. Add development CloudTamer credentials to your interactive shell script

   *Example for bash:*

   ```ShellSession
   $ printf '\n# Set development CloudTamer credentials' >> ~/.bash_profile
   $ printf "\nexport CLOUDTAMER_USER_NAME=\"$TEMP_CLOUDTAMER_USER_NAME\"" >> ~/.bash_profile
   $ printf "\nexport CLOUDTAMER_PASSWORD=\"$TEMP_CLOUDTAMER_PASSWORD\"" >> ~/.bash_profile
   ```

1. Load updated profile in the current terminal

   ```ShellSession
   $ source ~/.bash_profile
   ```

1. Verify development CloudTamer credentials have been set 

   ```ShellSession
   $ echo "${CLOUDTAMER_USER_NAME}"
   $ echo "${CLOUDTAMER_PASSWORD}"
   ```

### Verify that you can get logs from api and worker nodes

1. Ensure that your are connected to CMS Cisco VPN before proceeding

1. Change to the "repo" directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Run the get logs script

   ```ShellSession
   $ ./Deploy/bash/get-logs.sh
   ```

1. Enter the number of the desired AWS account where the desired logs reside

   *Example for Sbx:*

   ```
   2
   ```

1. Wait for the message logs to be downloaded from each node in the environment

   *Note that this may take a while.*

1. Note the "ENVIRONMENT SUMMARY"

   *Example for Sbx:*

   ```
   **********************************************
   ENVIRONMENT SUMMARY
   **********************************************
   
   CMS_ENV=ab2d-sbx-sandbox
   SSH_PRIVATE_KEY=ab2d-sbx-sandbox.pem
   API_COUNT=2
   WORKER_COUNT=2
   LOGS_DOWNLOADED_COUNT=4
   ```

1. Note the "LOGS SUMMARY"

   *Example for Sbx:*

   ```
   **********************************************
   LOGS DOWNLOADED
   **********************************************
   
   ~/Downloads/messages-api-node-10.242.26.68.txt
   ~/Downloads/messages-api-node-10.242.26.71.txt
   ~/Downloads/messages-worker-node-10.242.26.89.txt
   ~/Downloads/messages-worker-node-91.txt
   ```
   
### Verify that you can connect to api and worker nodes

1. Ensure that your are connected to CMS Cisco VPN before proceeding

1. Change to the "repo" directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Run the get logs script

   ```ShellSession
   $ ./Deploy/bash/connect-to-node.sh
   ```
   
1. Enter the number of the desired AWS account where desired node resides

   *Example for Sbx:*

   ```
   2
   ```

1. Enter the number of the node type of the deisred node

   *Example for an API node:*

   ```
   1
   ```

   *Example for an Worker node:*

   ```
   2
   ```

1. Enter the number of the deisred node

   *Example for first listed node:*

   ```
   1
   ```

1. Verify that you are automantically connected to the desired node

1. Note that if you don't do anything on the node for a given period of time, you will be automatically logged out of that node

1. If you are still connected to the node and are finished working with it, enter the following to exit the node

   ```ShellSession
   $ exit
   ```

### Verify that you can set AWS environment variables

1. Ensure that your are connected to CMS Cisco VPN before proceeding

1. Change to the "repo" directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Set AWS environment variables using the CloudTamer API

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Enter the number of the desired AWS account where the desired logs reside

   *Example for Mgmt:*

   ```
   1
   ```

   *Example for Dev:*

   ```
   2
   ```

   *Example for Sbx:*

   ```
   3
   ```

   *Example for Impl:*

   ```
   4
   ```

1. Note that temporary AWS credentials from CloudTamer will expire after an hour

1. Verify that the environment is set correctly by getting the key name used by the instances

   1. Enter the following

      ```ShellSession
      $ aws --region us-east-1 ec2 describe-instances \
        --query "Reservations[*].Instances[*].KeyName" \
	--output text \
        | head -1
      ```

   1. Verify that the key name output matches the selected environment

      *Example for Mgmt:*
      
      ```
      ab2d-mgmt-east-dev
      ```

      *Example for Dev:*
      
      ```
      ab2d-dev
      ```

      *Example for Sbx:*
      
      ```
      ab2d-sbx-sandbox
      ```

      *Example for Impl:*
      
      ```
      ab2d-east-impl
      ```

## Bookmark Jenkins master

1. Ensure that you are connected to the Cisco VPN

1. Open Chrome

1. Log on to the public GitHub

1. Note that you will be accessing Jenkins via GitHub authentication

1. Get the URL for Jenkins master from 1Password

   - **Vault:** ab2d

   - **Item:** Jenkins Master Login

1. Note the **website** value (this is the URL that you use to access Jenkins master)

1. Copy and paste the URL into the address bar

1. Verify that the Jenkins GUI appears

1. Bookmark "Jenkins" to make it easier to access the next time

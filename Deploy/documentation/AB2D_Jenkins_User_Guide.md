# AB2D Jenkins User Guide

## Table of Contents

1. [Manage Jenkins](#manage-jenkins)
   * [Obtain and bookmark the Jenkins URL for AB2D](#obtain-and-bookmark-the-jenkins-url-for-ab2d)
   * [Upgrade Jenkins](#upgrade-jenkins)
     * [Determine if there is a newer version of Jenkins available](#determine-if-there-is-a-newer-version-of-jenkins-available)
     * [Upgrade Jenkins software](#upgrade-jenkins-software)
     * [Upgrade Jenkins plugins](#upgrade-jenkins-plugins)
     * [Verify that Jenkins agent is online](#verify-that-jenkins-agent-is-online)
   * [Add a new GitHub user in Jenkins](#add-a-new-github-user-in-jenkins)
   * [Log on to Jenkins using GitHub OAuth authentication](#log-on-to-jenkins-using-github-oauth-authentication)
1. Export Jenkins projects

## Manage Jenkins

### Obtain and bookmark the Jenkins URL for AB2D

1. Open 1Password

1. Log on to 1Password

1. Search for the following entry in 1Password

   - **1Password Entry:** Jenkins Master Login

   - **Vault:** ab2d

1. Copy the **website** value for the Jenkins URL

1. Create a browser bookmark for the Jenkins URL

### Upgrade Jenkins

#### Determine if there is a newer version of Jenkins available

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI

1. Select **Manage Jenkins** from the leftmost panel

1. If there is a newer version of Jenkins available, jump to the following section:

   [Upgrade Jenkins software](#upgrade-jenkins-software)

1. If there are only plugin updates listed, jump to the following section:

   [Upgrade Jenkins plugins](#upgrade-jenkins-plugins)
   
#### Upgrade Jenkins software

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal
   
1. Connect to Jenkins master

   1. Change to the "Deploy" directory

      ```ShellSession
      $ cd ~/code/ab2d/Deploy
      ```

   1. Get credentials for the Management AWS account

      ```ShellSession
      $ source ./bash/set-env.sh
      ```

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Stop Jenkins

   ```ShellSession
   $ sudo systemctl stop jenkins
   ```

1. Clear the jenkins log

   1. Switch to the jenkins user
   
      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Clear the Jenkins log
   
      ```ShellSession
      $ cat /dev/null > /var/log/jenkins/jenkins.log
      ```

   1. Exit the jenkins user

      ```ShellSession
      $ exit
      ```

1. Install Jenkins Redhat public key

   ```ShellSession
   $ sudo rpm --import https://pkg.jenkins.io/redhat/jenkins.io.key
   ```

1. Upgrade Jenkins

   ```ShellSession
   $ sudo yum update jenkins -y
   ```

1. Start Jenkins

   ```ShellSession
   $ sudo systemctl start jenkins
   ```

1. Verify the status of Jenkins

   ```ShellSessiion
   $ systemctl status jenkins
   ```

1. Wait for about one minute

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master private ip}:8080

   1. Log on to the Jenkins GUI

1. Select **Manage Jenkins** from the leftmost panel

1. Verify that Jenkins has been updated to the new version

1. If there are plugin updates listed, jump to the following section:

   [Upgrade Jenkins plugins](#upgrade-jenkins-plugins)

1. If there are no plugin updates listed, jump to the following section:

   [Verify that Jenkins agent is online](#verify-that-jenkins-agent-is-online)

### Upgrade Jenkins plugins

1. Open Chrome

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI

1. Select **Manage Jenkins** from the leftmost panel

1. Select **Manage Plugins**

1. Select the **Updates** tab

1. If there are updates listed, do the following:

   1. Select **Check now**

   1. Wait for the check to complete

   1. Note any warnings for plugins to determine if you will need to make any changes to existing projects to conform with the update

   1. Check all plugins listed under the "Updates" tab
   
   1. Select **Download now and install after restart**

   1. Check **Restart Jenkins when installation is complete and no jobs are running**

   1. Wait for Jenkins to restart to enter the "Running" phase

   1. Refresh the page

   1. Log on to Jenkins (if not still logged in)

#### Verify that Jenkins agent is online

1. Open Chrome

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI

1. Scroll to the bottom of the page

1. If "agent01" is offline, do the following:

   1. Select "agent01"

   1. Select **See log for more details**

   1. If there is an "ERROR: Server rejected the 1 private key(s) for jenkins (credentialId:jenkins/method:publickey)" error, do the following:

      > *** TO DO ***: document the process that involves the following

      - create a new ssh keypair that overwrites existing keypair

      - do the ssh-copy-id processes on and between the jenkins master and jenkins agent

   1. If there is a different error, resolve it

### Add a new GitHub user in Jenkins

1. Note that these instructions can only be done by someone that has already been added to Jenkins

1. Open Chrome

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI

1. Select **Manage Jenkins** from the leftmost panel

1. Scroll down to the "Security" section

1. Select **Configure Global Security** under the "Security" section

1. Scroll down to the **Authorization** section

1. Add a GitHub user

   1. Select **Add user or group**

   1. Type the GitHub user in the **User or group name** text box

      *Example:*

      ```
      fsmith
      ```

   1. Select **OK** on the "User or group name" dialog

   1. Select the ![Select All](images/jenkins-select-all.png) icon

   1. Select **Apply**

1. Repeat the "Add a GitHub user" step for any additional users

1. Select **Save**

### Log on to Jenkins using GitHub OAuth authentication

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI

1. Note that you will be prompted to log on to GitHub, if you are not already logged in

1. Log on to GitHub (if not already logged in)

1. If this is your first time logging into Jenkins, do the following:

   1. Note an "Authorize jenkins-github-authentication" page appears with the following information

      *Format:*
      
      - **jenkins-github-authentication by lhanekam:** wants to access your {your github user} account

      - **Organizations and teams:** Read-only access

      - **Repositories:** Public and private

      - **Personal user data:** Email addresses (read-only)

      - **Organization access:** CMSgov

   1. Select **Authorize lhanekam**

1. Verify that the Jenkins page loads

## Export Jenkins projects

1. Create desired directory structure

   *Example:*
   
   ```ShellSession
   $ mkdir -p ~/Downloads/jenkins/development
     && mkdir -p ~/Downloads/jenkins/sandbox
   ```

1. Export the development folder

   *Format:*

   ```ShellSession
   $ java -jar /opt/jenkins-cli/jenkins-cli.jar \
     -s "http://{jenkins master public ip}:8080" \
     -auth {github user}:{github personal access token} \
     get-job development \
     > ~/Downloads/jenkins/development.xml
   ```

1. Export the "deploy-to-development" project

   *Format:*

   ```ShellSession
   $ java -jar /opt/jenkins-cli/jenkins-cli.jar \
     -s "http://{jenkins master public ip}:8080" \
     -auth {github user}:{github personal access token} \
     get-job development/deploy-to-development \
     > ~/Downloads/jenkins/development/deploy-to-development.xml
   ```

1. Export the sandbox folder

   *Format:*

   ```ShellSession
   $ java -jar /opt/jenkins-cli/jenkins-cli.jar \
     -s "http://{jenkins master public ip}:8080" \
     -auth {github user}:{github personal access token} \
     get-job sandbox \
     > ~/Downloads/jenkins/sandbox.xml
   ```

1. Export the "deploy-to-sandbox" project

   *Format:*

   ```ShellSession
   $ java -jar /opt/jenkins-cli/jenkins-cli.jar \
     -s "http://{jenkins master public ip}:8080" \
     -auth {github user}:{github personal access token} \
     get-job sandbox/deploy-to-sandbox \
     > ~/Downloads/jenkins/sandbox/deploy-to-sandbox.xml
   ```

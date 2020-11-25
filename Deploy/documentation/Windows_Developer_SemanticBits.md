# Windows Developer SemanticBits

## Table of Contents

1. [Implement a Docker on Ubuntu setup](#implement-a-docker-on-ubuntu-setup)
1. [Install other development tools on ubuntu](#install-other-development-tools-on-ubuntu)
1. [Configure AB2D](#configure-ab2d)

## Implement a Docker on Ubuntu setup

1. Disconnect from VPN

1. If Docker Desktop is installed, do the following:

   1. Stop Docker Desktop if it is running in your system bar (whale icon with blocks on it)

   1. Uninstall docker desktop

1. If Ubuntu is installed, uninstall Ubuntu so that you can do a fresh Ubuntu install

1. Install the latest Ubuntu from Microsoft Store (currently Ubuntu 20.04)

1. Open Ubuntu and set your desired username and password

1. Update the apt package index

   ```ShellSession
   $ sudo apt-get update
   ```

1. Install packages to allow apt to use a repository over HTTPS

   ```ShellSession
   $ sudo apt-get install \
     apt-transport-https \
     ca-certificates \
     curl \
     gnupg-agent \
     software-properties-common
   ```

1. Add Docker’s official GPG key:

   ```ShellSession
   $ curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
   ```

1. Verify that you now have the key with the fingerprint 9DC8 5822 9FC7 DD38 854A  E2D8 8D81 803C 0EBF CD88, by searching for the last 8 characters of the fingerprint

   ```ShellSession
   $ sudo apt-key fingerprint 0EBFCD88
   ```

1. Set up the stable repository for docker

   ```ShellSession
   $ sudo add-apt-repository \
     "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
     $(lsb_release -cs) \
     stable"
   ```

1. Update the apt package index again

   ```ShellSession
   $ sudo apt-get update
   ```

1. Remove existing mdadm

   ```ShellSession
   $ sudo apt remove mdadm
   ```
   
1. Install docker engeine

   ```ShellSession
   $ sudo apt-get install docker-ce docker-ce-cli containerd.io
   ```

1. Verify docker is available

   ```ShellSession
   $ docker --version
   ```

1. Install docker-compose

   ```ShellSession
   $ sudo curl -L "https://github.com/docker/compose/releases/download/1.27.4/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
   ```

1. Set docker-compose permissions

   ```ShellSession
   $ sudo chmod +x /usr/local/bin/docker-compose
   ```

1. Verify that docker-compose is installed by checking its version

   ```ShellSession
   $ docker-compose --version
   ```

1. Start the Docker dameon and leave it running

   ```ShellSession
   $ sudo dockerd
   ```

1. Verify that the following is displayed in the last line of the output

   ```
   API listen on /var/run/docker.sock
   ```

1. Open PowerShell as an administrator

1. Verify that git is accessible from powershell by checking the version

   ```ShellSession
   $ git --version
   ```

1. If git is not available from PowerShell, install it from here

   > https://git-scm.com/downloads

1. List the distributions in the already open PowerShell

   ```ShellSession
   $ wsl -l -v
   ```

1. Note the output

   ```
   NAME                           STATE           VERSION
   * Ubuntu                        Stopped         2
   ```
 
1. If ubuntu is not listed as version 2, do the following

   1. Convert Ubuntu to version 2 in the already open PowerShell (this takes a while)

      ```ShellSession
      $ wsl --set-version Ubuntu 2
      ```

   1. Wait fo the conversion to complete

   1. List the distributions again in the already open PowerShell

      ```ShellSession
      $ wsl -l -v
      ```

   1. Note the changed output

      ```
      NAME                            STATE           VERSION
      * Ubuntu                        Stopped         2
      ```

1. Set v2 as the default version for future installations in the already open PowerShell

   ```ShellSession
   $ wsl --set-default-version 2
   ```

1. Close PowerShell

1. Open a second ubuntu window as an administrator
   
1. Run hello world docker example in the second ubuntu window

   ```ShellSession
   $ sudo docker run hello-world
   ```

1. Here is the getting started link for docker

   > https://docs.docker.com/get-started/

1. After you are done working with docker for the day, do the following:

   1. Return to the ubuntu window that is running docker

   1. Click on the ubuntu window so that it has focus

   1. Press **Ctrl+c** on the keyboard to kill the docker daemon

1. If Ctrl+c fails to stop the docker daemon, do the following:

   1. Close the ubuntu window

   1. Restart Windows

   1. Open Ubuntu as an administrator

   1. Delete the "docker.pid" file

      ```ShellSession
      $ sudo rm -f /var/run/docker.pid
      ```

1. After you have everything setup, you can start your day as follows

   1. Log on to Windows and give it time to do all its initial behind the scenes setup

   1. Open Ubuntu as an administrator

   1. Start the Docker dameon and leave it running

      ```ShellSession
      $ sudo dockerd
      ```

   1. Verify that the following is displayed in the last line of the output

      ```
      API listen on /var/run/docker.sock
      ```

   1. Minimize this ubuntu windows so that it is out of your way

   1. Don't forget to shut it down correctly at the end of your day with Ctrl+c as documented above

1. If you need to change to a different version of Docker, do the following:

   1. If Docker is running in another Ubuntu window, do the following:

      1. Return to the Ubuntu window that is running docker

      1. Click on the Ubuntu window so that it has focus

      1. Press **Ctrl+c** on the keyboard to kill the docker daemon

      1. Close this Ubuntu window

   1. Open an ubuntu window as administrator
   
   1. Set the desired docker version

      *Example:*

      ```ShellSession
      $ DOCKER_VERSION=19.03.12
      ```

   1. Get the ubuntu docker version

      ```ShellSession
      $ UBUNTU_DOCKER_VERSION=$(apt-cache madison docker-ce | grep "${DOCKER_VERSION}" | awk -F"|" '{print $2}' | tr -d '[:space:]')
      ```

   1. Verify that there is an Ubuntu docker version for that version of docker

      ```ShellSession
      $ echo "${UBUNTU_DOCKER_VERSION}"
      ```

   1. Verity that the Ubuntu docker version was displyed

      *Example:*

      ```
      5:19.03.12~3-0~ubuntu-focal
      ```

   1. Install the desired docker version

      ```ShelSession
      $ sudo apt-get install docker-ce="${UBUNTU_DOCKER_VERSION}" docker-ce-cli="${UBUNTU_DOCKER_VERSION}" containerd.io
      ```

   1. Verify the docker version

      ```ShellSession
      $ docker --version
      ```

   1. Note the output

      *Example:*

      ```
      Docker version 19.03.12, build 48a66213fe
      ```
   
   1. Run the docker daemon

      1. Open an new Ubuntu window as administrator

      1. Start the Docker dameon and leave it running

         ```ShellSession
         $ sudo dockerd
         ```

      1. Verify that the following is displayed in the last line of the output

         ```
         API listen on /var/run/docker.sock
         ```

      1. Minimize this ubuntu windows so that it is out of your way

      1. Don't forget to shut it down correctly at the end of your day with Ctrl+c as documented above

## Install other development tools on ubuntu

1. Open Ubuntu as administrator

1. Install Java Development Kit (JDK) 13

   ```ShellSession
   $ sudo apt-get install openjdk-13-jdk
   ```

1. Backup ".bashrc"

   ```ShellSession
   $ cp ~/.bashrc ~/.bashrc_backup
   ```

1. Set JAVA_HOME environment variable

   ```ShellSession
   $ printf '\n# Set JAVA_HOME environment variable' >> ~/.bashrc \
     && printf '\nexport JAVA_HOME="/usr/lib/jvm/java-13-openjdk-amd64"' >> ~/.bashrc
   ```

1. Apply the ".bashrc" change to the current terminal session

   ```ShellSession
   $ source ~/.bashrc
   ```

1. Install make

   ```ShellSession
   $ sudo apt install make
   ```

1. Install maven

   ```ShellSession
   $ sudo apt install maven
   ```

## Configure AB2D

1. Create a "share" directory on your Windows machine

   1. Open the command prompt (not ubuntu)

   1. Create a "share" directory

      ```ShellSession
      mkdir "C:\share"
      ```

   1. Close the command prompt

1. Copy "AB2D Dev : BFD Prod Sbx : Keystore" from 1Password to the "C:\share" directory

1. Open ubuntu as an administrator

1. Create "/opt/ab2d" directory

   ```ShellSession
   $ sudo mkdir -p /opt/ab2d
   ```

1. Set permissions on the "/opt/ab2d" directory

   ```ShellSession
   $ sudo chown -R $(id -un):$(id -gn) /opt/ab2d
   ```

1. Copy ab2d_dev_keystore to the /opt/ab2d directory

   ```ShellSession
   $ cp /mnt/c/share/ab2d_dev_keystore /opt/ab2d
   ```
   
1. Verify that the keystore has been copied to the "/opt/ab2d" directory

   ```
   $ ls /opt/ab2d
   ```

1. Set permissions on the "ab2d_prod_keystore" file

   ```ShellSession
   $ sudo chmod 600 /opt/ab2d/ab2d_dev_keystore
   ```

1. Open your ".bashrc" file

   ```ShellSession
   $ vim ~/.bashrc
   ```

1. Copy and paste the following at the end of your ".bashrc" file

   *Format:*

   ```
   # AB2D Development Settings
   export HPMS_AUTH_KEY_ID="{'AB2D Dev : HPMS IMPL/staging : API Key ID' from 1Password}"
   export HPMS_AUTH_KEY_SECRET="{'AB2D Dev : HPMS IMPL/staging : API Key Secret' from 1Password}"
   export AB2D_HPMS_URL="{HPMS URL}"
   export NEW_RELIC_LICENSE_KEY="{AB2D New Relic}"
   export NEW_RELIC_APP_NAME="AB2D for {your first name}.{your last name}"
   export AB2D_BFD_KEYSTORE_PASSWORD="{'AB2D Dev : BFD Prod Sbx : Keystore Password' from 1Password}"
   export AB2D_BFD_KEYSTORE_LOCATION="/opt/ab2d/ab2d_dev_keystore"
   export AB2D_HICN_HASH_PEPPER="{'AB2D Dev : BFD Prod Sbx : HICN Hash Pepper' from 1Password}"
   export AB2D_HICN_HASH_ITER="{'AB2D Dev : BFD Prod Sbx : HICN Hash Iter' from 1Password}"
   export AB2D_CLAIMS_SKIP_BILLABLE_PERIOD_CHECK=true
   export AB2D_KEYSTORE_LOCATION="{'AB2D Dev : API : Keystore' from 1Password}"
   export AB2D_KEYSTORE_PASSWORD="{'AB2D Dev : API : Keystore Password' from 1Password}"
   export AB2D_KEY_ALIAS="{'AB2D Dev : API : Key Alias' from 1Password}"
   ```

1. Save and close the file

1. Apply the ".bashrc" changes to the current terminal session

   ```ShellSession
   $ source ~/.bashrc
   ```

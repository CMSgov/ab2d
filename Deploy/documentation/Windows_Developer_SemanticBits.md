# Windows Developer SemanticBits

## Table of Contents

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
   $ sudo curl -L “https://github.com/docker/compose/releases/download/1.27.4/docker-compose-$(uname -s)-$(uname -m)” -o /usr/local/bin/docker-compose
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

   - Convert Ubuntu to version 2 in the already open PowerShell (this takes a while)

     ```ShellSession
     $ wsl --set-version Ubuntu 2
     ```

   - Wait fo the conversion to complete

   - List the distributions again in the already open PowerShell

     ```ShellSession
     $ wsl -l -v
     ```

   - Note the changed output

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

   - Return to the ubuntu window that is running docker

   - Click on the ubuntu window so that it has focus

   - Press **Ctrl+c** on the keyboard to kill the docker daemon

1. If Ctrl+c fails to stop the docker daemon, do the following:

   - Close the ubuntu window

   - Restart Windows

   - Open Ubuntu as an administrator

   - Delete the "docker.pid" file

     ```ShellSession
     $ sudo rm -f /var/run/docker.pid
     ```

1. After you have everything setup, you can start your day as follows

   - Log on to Windows and give it time to do all its initial behind the scenes setup

   - Open Ubuntu as an administrator

   - Start the Docker dameon and leave it running

     ```ShellSession
     $ sudo dockerd
     ```

   - Verify that the following is displayed in the last line of the output

     ```
     API listen on /var/run/docker.sock
     ```

   - Minimize this ubuntu windows so that it is out of your way

   - Don't forget to shut it down correctly at the end of your day as documented above

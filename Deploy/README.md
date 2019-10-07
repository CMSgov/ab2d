# ab2d

## Table of Contents

1. [Setup develoment machine](#setup-develoment-machine)
   * [Assumptions](#assumptions)
   * [Change ownership of HomeBrew related directories](#change-ownership-of-homebrew-related-directories)
   * [Install python3](#install-python3)
   * [Configure python3](#configure-python3)
   * [Configure pip3](#configure-pip3)
   * [Install Packer](#install-packer)
   * [Install Terraform](#install-terraform)
   * [Install the AWS CLI using pip3](#install-the-aws-cli-using-pip3)
   * [Create an AWS IAM user](#create-an-aws-iam-user)
   * [Configure AWS CLI](#configure-aws-cli)
2. [Deploy solution](#deploy-solution)

## Setup develoment machine

### Assumptions

*Note that these instructions assume the following:*

- the development machine is a Mac

- you already have a default SSH keypair

- HomeBrew is installed

### Change ownership of HomeBrew related directories

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

2. Install lxml

   *Note that "lxml" is a library for parsing XML and HTML.*
   
   ```ShellSession
   $ pip3 install lxml
   ```

3. Install requests

   ```ShellSession
   $ pip3 install requests
   ```

4. Install "detect-secrets"

   ```ShellSession
   $ pip3 install detect-secrets
   ```

### Install Packer

1. Install Packer using HomeBrew

   ```ShellSession
   $ brew install packer
   ```

2. Verify the packer installation by checking the version of packer

   ```ShellSession
   $ packer --version
   ```

### Install Terraform

1. Install Terraform 0.12

   ```ShellSession
   $ brew install terraform@0.12
   ```
   
2. Check the terraform version

   ```ShellSession
   $ terraform --version
   ```

### Install the AWS CLI using pip3

1. Install the AWS CLI using pip3

   ```ShellSession
   $ pip3 install awscli --upgrade --user --no-warn-script-location
   ```

1. Check AWS CLI version

   ```ShellSession
   $ aws --version
   ```

1. Backup existing profile shell script
   
   *Example for bash:*

   ```ShellSession
   $ cp ~/.bash_profile ~/.bash_profile_backup
   ```

1. Add AWS CLI path to your interactive shell script

   *Example for bash:*
   
   ```ShellSession
   $ printf '\n# Add AWS CLI to Path' >> ~/.bash_profile
   $ printf '\nexport PATH="$PATH:$HOME/Library/Python/3.7/bin"' >> ~/.bash_profile
   ```

1. Load updated profile in the current terminal

   ```ShellSession
   $ source ~/.bash_profile
   ```

1. Check the AWS version

   ```ShellSession
   $ aws --version
   ```
   
### Create an AWS IAM user

1. Request AWS administrator to create a user that has both console and programmatic access

1. Note that the administrator will provide you with a "credentials.csv" file that will include the following information
   
   - User name

   - Password

   - Access key ID

   - Secret access key

   - Console login link

### Configure AWS CLI

1. Configure AWS CLI

   *Example for "semanticbitsdemo" AWS account:*
   
   ```ShellSession
   $ aws configure --profile=sbdemo
   ```

1. Enter {your aws access key} at the **AWS Access Key ID** prompt

1. Enter {your aws secret access key} at the AWS Secret Access Key prompt

1. Enter the following at the **Default region name** prompt

   ```
   us-east-1
   ```

1. Enter the following at the **Default output format** prompt

   ```
   json
   ```

1. Examine the contents of your AWS credentials file

   ```ShellSession
   $ cat ~/.aws/credentials
   ```
   
## Deploy solution

1. Change to the "ab2d" repo directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```
   
1. Set AWS_PROFILE

   ```ShellSession
   $ source ./Deploy/scripts/set-aws-profile.sh
   ```

1. Deploy the solution to the AWS account associated with the AWS profile

   ```ShellSession
   $ ./Deploy/deploy.sh
   ```
   
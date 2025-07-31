This project encapsulates all AB2D lambdas managed by the dev team. AB2D ops manages some lambdas.
The goals of this project are: 
- Streamline creating, testing, and deploying lambdas to aws
- Share code between out lambdas while limiting the need to import external libraries. 
  - Lambdas should be a small and fast. External libraries tend to cover large usecases
    - An example would be Spring. Spring is amazing but it's also HUGE
  - Libraries can absolutely still be used but should be scrutinized. 
- Make it as easy as possible to get started building and running lambdas locally
  - A single, easy to use script to build all the lambdas, start localstack, and setup infrastructure


## Install and Use Pre-commit

Anyone committing to this repo must use the pre-commit hook to lower the likelihood that secrets will be exposed.

### Step 1: Install pre-commit

You can install pre-commit using the MacOS package manager Homebrew:

```sh
brew install pre-commit
```

Other installation options can be found in the [pre-commit documentation](https://pre-commit.com/#install).

### Step 2: Install the hooks

Run the following command to install the gitleaks hook:

```sh
pre-commit install
```

This will download and install the pre-commit hooks specified in `.pre-commit-config.yaml`.


## Terraform

This projects uses terraform for local testing. This was done purely as a convenience since the ops team also uses terraform.
The major caveat is all resources use the same IAM. That massively simplifies the terraform in this code.
In this context, permissions are an operational concern and not a development concern. 

## Build

AWS lambdas need to be zipped. The follow command will build all projects and zip all build files into a zip file. It's very similar to a fat jar.

```
gradle buildZip
```

## Run

In the root directory run
./start.sh

This script will

- builds all lambdas with the buildZip task
- init terraform using the terraform docker image
- Sets up a localstack docker image
- Applies the terraform that facilitates running lambdas

Running start.sh multiple times is the correct way to deploy changes. 
You can pass clean to start.sh to run gradle clean 
- ./start.sh clean

## Stop

In the root directory run
./stop.sh

This script will

- destroy localstack infrastructure (if the localstack container is still running)
- stop the localstack docker image
- delete terraform lock files (if this is not done start.sh can fail on future runs)

## Deploy

Use this jenkins job to deploy
https://jenkins.ab2d.cms.gov/job/AB2D-Ops/job/Terraform/job/Deploy%20Lambda/


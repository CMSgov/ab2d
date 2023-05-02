# AB2D

[![Maintainability](https://api.codeclimate.com/v1/badges/322dab715b4324c33fee/maintainability)](https://codeclimate.com/github/CMSgov/ab2d/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/322dab715b4324c33fee/test_coverage)](https://codeclimate.com/github/CMSgov/ab2d/test_coverage)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/CMSgov/ab2d.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/CMSgov/ab2d/alerts/)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/CMSgov/ab2d.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/CMSgov/ab2d/context:java)
[![Language grade: Python](https://img.shields.io/lgtm/grade/python/g/CMSgov/ab2d.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/CMSgov/ab2d/context:python)
[![Automated Release Notes by gren](https://img.shields.io/badge/%F0%9F%A4%96-release%20notes-00B2EE.svg)](https://github-tools.github.io/github-release-notes/)

## Table of Contents

1. [Create volume directory](#create-volume-directory)
1. [Running Locally with Intelij](#running-locally-with-intelij)

## Create volume directory

1. Create a volume directory

   ```ShellSession
   $ mkdir -p /opt/ab2d
   ```

1. Note that this directory acts as a shared volume for the API and worker containers

## Running Locally with Intelij
1. Remove comments in the docker-compose.yml to set db ports
   ```ShellSession
      ports:
      - "5432:5432"
   ```
2. Run postgress and localstack locally using Docker

   ```ShellSession
   $ docker-compose up db localstack
   ```

Worker Setup

3. Select Run/Debug Configuration > Edit Configurations > add configuration (+) > Spring Boot
4. In Main Class select gov.cms.ab2d.worker.SpringBootApp
5. Go to 1Password and search for 'AB2D Local Env Variables'. Use the configs in the note for the Environment Variables field
6. Run the configuration


API Setup
6. Select Run/Debug Configuration > Edit Configurations > add configuration (+) > Spring Boot
7. In Main Class select gov.cms.ab2d.api.SpringBootApp
8. Go to 1Password and search for 'AB2D Local Env Variables'. Use the configs in the note for the Environment Variables field
9. Run the configuration


[AB2D Deploy](Deploy/README.md)

## Installing and Using Pre-commit with Gitleaks

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

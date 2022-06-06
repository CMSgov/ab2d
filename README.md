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
2. Run postgress locally using Docker

   ```ShellSession
   $ docker-compose up db
   ```

Worker Setup

3. Select Run/Debug Configuration > Edit Configurations > add configuration (+) > Spring Boot
4. In Main Class select gov.cms.ab2d.worker.SpringBootApp
5. Go to 1Password and search for 'AB2D Local Env Variables'. Use the configs in the note for the Environment Variables field
6. Uncomment the localstack service in docker-compose.tml
   1. docker compose up localstack
7. Set these VM options
   1.  -DLOCALSTACK_URL=127.0.0.1:4566 to the VM options of worker
       -Dcloud.aws.region.static=us-east-1
       -Dcom.amazonaws.sdk.disableCertChecking
8. Run the configuration


API Setup
6. Select Run/Debug Configuration > Edit Configurations > add configuration (+) > Spring Boot
7. In Main Class select gov.cms.ab2d.api.SpringBootApp
8. Go to 1Password and search for 'AB2D Local Env Variables'. Use the configs in the note for the Environment Variables field
9. Run the configuration


[AB2D Deploy](Deploy/README.md)

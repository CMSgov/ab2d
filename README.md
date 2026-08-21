# AB2D

[![Maintainability](https://qlty.sh/gh/CMSgov/projects/ab2d/maintainability.svg)](https://qlty.sh/gh/CMSgov/projects/ab2d)
[![Test Coverage](https://qlty.sh/gh/CMSgov/projects/ab2d/coverage.svg)](https://qlty.sh/gh/CMSgov/projects/ab2d)
[![CodeQL](https://github.com/CMSgov/ab2d/actions/workflows/codeql.yml/badge.svg)](https://github.com/CMSgov/ab2d/actions/workflows/codeql.yml)
[![Automated Release Notes by gren](https://img.shields.io/badge/%F0%9F%A4%96-release%20notes-00B2EE.svg)](https://github-tools.github.io/github-release-notes/)

## Table of Contents

1. [Create volume directory](#create-volume-directory)
1. [Running Locally](#running-locally)
1. [Installing and Using Pre-Commit](#installing-and-using-pre-commit)

## Create volume directory

1. Create a volume directory

   ```ShellSession
   $ mkdir -p /opt/ab2d
   ```

1. Note that this directory acts as a shared volume for the API and worker containers

## Running Locally

### Prerequisites 

- Docker (with `docker compose`)
- Java 25

### Setup
Build the worker/api jar:

```sh
mvn -DskipTests package -Dcheckstyle.skip
```

Build the contracts jar:

  ```sh
  (cd contracts && ./gradlew -x test build)
  docker build -t ab2d-contracts-local:latest contracts
  ```

### Quick start

```sh
local-dev/up.sh              # handles setting up docker compose and running sql scripts
local-dev/submit-job.sh      # submit job, poll, download NDJSON
local-dev/down.sh            # tear down everything
```

Use the `down` script to wipe the database if needed:

```sh
local-dev/down.sh --volumes
```

#### Troubleshooting Docker Desktop on macOS

Possible errors include:

```text
Error response from daemon: error while mounting volume '/var/lib/docker/volumes/ab2d_tmp-volume/_data': 
failed to mount local volume: mount /opt/ab2d:/var/lib/docker/volumes/ab2d_tmp-volume/_data, flags: 0x1000: no such file or directory
```

```text
Error response from daemon: mounts denied: 
The path /opt/ab2d is not shared from the host and is not known to Docker.
```

Solution:
1. Add a file share for `/opt/ab2d` under Docker → Preferences → Resources → File Sharing. 
2. Go to macOS System Settings → Privacy & Security
   - Select Full Disk Access
   - Grant permission to Docker Desktop
   - Restart Docker Desktop

### Attaching a Debugger in IntelliJ

To attach a debugger to the worker or API, set up the peripheral modules with the following commands:

```sh
# database, localstack, and bfd mock
MOCK_PUBLIC_URL=http://localhost:9999 \
  docker compose -f docker-compose.yml -f docker-compose.local.yml \
  up -d db localstack mock-bfd mock-bfd-init

# contracts service
docker compose -f docker-compose.yml -f docker-compose.local.yml \
  up -d --no-deps contracts
```

Then, start the worker/api by running the local configuration for each app. Two configurations are included, named `api (local)` and `worker (local)`. 

Once the environment is running, seed the local database to run test jobs:

```sh
psql -h localhost -U ab2d -d ab2d -f local-dev/sql/seed-contract.sql
psql -h localhost -U ab2d -d ab2d -f local-dev/sql/seed-coverage.sql
```

When prompted for a password, use the default: `ab2d`.

Once the database is seeded, submit a job using the job submission script, `local-dev/submit-job.sh`, or by sending a request to the API directly at https://localhost:8443.

## Installing and Using Pre-commit

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

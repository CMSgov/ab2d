# Ab2d-Contracts

This service gets data from hpms and shares contract information with other services.

### Installing and Using Pre-commit

Anyone committing to this repo must use the pre-commit hook to lower the likelihood that secrets will be exposed.

#### Step 1: Install pre-commit

You can install pre-commit using the MacOS package manager Homebrew:

```sh
brew install pre-commit
```

Other installation options can be found in the [pre-commit documentation](https://pre-commit.com/#install).

#### Step 2: Install the hooks

Run the following command to install the gitleaks hook:

```sh
pre-commit install
```

This will download and install the pre-commit hooks specified in `.pre-commit-config.yaml`.

### Running Locally with Intelij

1. Run Postgress/SQS using Docker and localstack

   ```ShellSession
   $ docker-compose up db localstack
   ```

Contract Service Setup
2. Select Run/Debug Configuration > Edit Configurations > add configuration (+) > Spring Boot
3. In Main Class select gov.cms.ab2d.contracts.SpringBootApp
4. Go to 1Password and search for 'Event Service Local Env Variables' or 'AB2D Local Env Variables'. Use the configs in the note for the Environment Variables field
5. Run the configuration

### Rest Examples
1. Go to path to access swagger Locally: http://localhost:8070/swagger-ui/index.html






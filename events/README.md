# Ab2d-Events

This service extracts event data from a SQS queue and process it.

### Running Locally with Intelij

1. Run postgress and localstack locally using Docker

   ```ShellSession
   $ docker-compose up db localstack
   ```

Event Service Setup
2. Select Run/Debug Configuration > Edit Configurations > add configuration (+) > Spring Boot
3. In Main Class select gov.cms.ab2d.eventlogger.SpringBootApp
4. Go to 1Password and search for 'Event Service Local Env Variables'. Use the configs in the note for the Environment Variables field
5. Run the configuration

### Add new messages to be processed
1. Follow instructions in the [Events Client](https://github.com/CMSgov/AB2D-Libs/tree/main/ab2d-events-client) to add new messages (events)
2. Go to file gov/cms/ab2d/eventlogger/api/EventsListener.java and add your new message. 
```aidl
            case "NewMessage" -> {
                //Send message to logManager. Most likly the logging type you needs exist already.
                //if not you'll have to add new logger to event service and methods to LogManager
                //Look at past logging (sql, kinesis and slack)
                logManager.methodcall(((MyMessage) sqsMessage).getVariablesFromObject());
            }
```
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





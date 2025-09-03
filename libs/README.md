# AB2D LIBS

This repository stores all dependencies for AB2D.
Confirm you have your gradle configured, so you can connect to the CMS repository locally. 

## Locally Build
```
gradle -b build.gradle
```

## Publishing
To publish any changes, You can force an update by using the jenkins_force_publish file.
New jars won't be published unless you change the library version to one that does not exist. 
Example of version for bfd in /ab2d-bfd/build.gradle
```
version = '1.0'
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

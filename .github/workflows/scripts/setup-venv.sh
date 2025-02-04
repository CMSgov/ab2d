#!/usr/bin/env bash

# Any error will cause the script to exit immediately
set -e

# Create python3 virtual environment for deployments
# and verify that

python3 --version

# Create virtualenv
python3 -m venv ./venv
. ./venv/bin/activate

# Check that version is active and log for
python --version

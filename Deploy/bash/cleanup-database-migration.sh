#!/bin/bash

set -e #Exit on first error
set -x #Be verbose

# Delete database backup

rm -rf "${HOME}/database_backup"

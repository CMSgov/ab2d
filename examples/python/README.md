# Python Client

A simple client for starting a job in sandbox or production, monitor that job,
and download the results. To prevent issues these scripts persist the job
id and list of files generated.

This script will not overwrite already existing export files.

```
Usage: 
  python (-prod | -sandbox) --auth <base64 username:password> [--contract <contract number>] [--directory <dir>]
        [(--only_start|--only_monitor|--only_download)]

Arguments:
  -sandbox -- if running against ab2d sandbox environment
  -prod -- if running against ab2d production environment
  --auth -- base64 encoded "clientid:password"
  --contract -- if searching specific contract then give contract number ex. Z0001
  --directory -- if you want files and job info saved to specific directory
  --only_start -- if you only want to start a job
  --only_monitor -- if you only want to monitor an already started a job
  --only_download -- if you only want to download an already finished job
```

### Files

1. /directory/jobId.txt -- id of the job created
2. /directory/response.json -- list of files created 
3. /directory/*.ndjson -- downloaded results of exports 

### Limitations

1. Assumes all scripts use the same directory
2. Assumes all scripts use the same base64 encoded AUTH token

### Example

If you want to:
1. Start a job running against production
2. Pull a specific contract named 'ABCDE'
3. And save all results for this job to the directory /opt/foo

Then run the following command: 
`python job-cli.py -prod --auth $AUTH --contract ABCDE --directory /opt/foo`

## Install or verify python 3, pip, and required pip modules

1. If you are using Linux, jump to the following section:

   [Install or verify python 3, pip, and required pip modules on Linux](#install-or-verify-python-3-pip-and-required-pip-modules-on-linux)

1. If you are using Mac, jump to the following section:

   [Install or verify python 3, pip, and required pip modules on Mac](#install-or-verify-python-3-pip-and-required-pip-modules-on-mac)

1. If you are using Windows, jump to the following section:

   [Install or verify python 3, pip, and required pip modules on Windows](#install-or-verify-python-3-pip-and-required-pip-modules-on-windows)

### Install or verify python 3, pip, and required pip modules on Linux

> *** TO DO ***

### Install or verify python 3, pip, and required pip modules on Mac

> *** TO DO ***

### Install or verify python 3, pip, and required pip modules on Windows

#### Determine if python 3 is already installed on Windows

1. Check to see if python 3 is installed

   ```ShellSession
   where python
   ```

1. Note the output

   *Example:*

   ```
   {your user directory\AppData\Local\Microsoft\WindowsApps\python3.exe
   ```

1. Note the following

   - if you only see one line of output displaying "python3.exe" under the "WindowsApps" directory, this means python3 is NOT installed

   - these instructions will install from the official python site instead of the Windows Store (which if often locked by administrators)

1. If you only had one line of "...\WindowsApps\python3.exe" output, jump to the following section:

   [Install python 3 on Windows](#install-python-3-on-windows)

1. If more that one line was displayed in the output and includes a "...\Python\Python3{version}\python.exe" line, jump to the following section:

   [Verify python and pip work from the command prompt on Windows](#verify-python-and-pip-work-from-the-command-prompt-on-windows)

#### Install python 3 on Windows

1. Open a web browser

1. Enter the following in the address bar

   > https://www.python.org/downloads

1. Select the following under "Download the latest version of Windows"

   *Format:*

   ```
   Download Python 3.{version}
   ```

   *Example:*

   ```
   Download Python 3.9.0
   ```

1. Wait for the download to complete

1. Open the downloaded file

   *Format:*

   ```
   python-3.{version}
   ```

   *Example:*

   ```
   python-3.{version}-amd64.exe
   ```

1. Check the following

   *Format:*

   ```
   Add Python 3.{version} to PATH
   ```

   *Example:*

   ```
   Add Python 3.9 to PATH
   ```

1. Select **Install Now**

1. Wait for the installation to complete

1. Select **Close**

#### Verify python and pip work from the command prompt on Windows

1. Select the the Windows icon (likely in the bottom left of your window)

1. Type the following in the search text box

   ```
   command
   ```

1. Select **Command Prompt** from the results

1. Verify that python is installed properly, by checking its version

   1. Check the python3 version

      ```ShellSession
      python --version
      ```

   1. Verify that the version of python that you installed is displayed

      ```
      Python 3.9.0
      ```

1. Verify that pip is installed properly, by checking its version

   1. Check the python3 version

      ```ShellSession
      pip --version
      ```

   1. Verify that the version of python that you installed is displayed

      *Format:*

      ```
      pip {version} from {installation directory}\pip (python 3.{version}
      ```

#### Install required pip modules on Windows

1. Install the "requests" pip module

   ```ShellSession
   pip install requests
   ```

1. Verify that the requests module installed correctly

   1. Show the request pip module

      ```ShellSession
      pip show requests
      ```

   1. Verify that a description of the "requests" module is displayed

## Get your data files

### Choose the method you want to use to get your data files

*Note that for this example we are running an export job on sandbox.*

1. If you want to run the process stages individually in order to understand the process, jump to the following section:

   [Running Stages Individually](#running-stages-individually)

1. If you just want all process stages to run automatically, jump to the following section:

   [Running All Stages](#running-all-stages)

### Running Stages Individually

1. Set the AUTH value

   *On Mac or Linux:*

   ```bash
   AUTH=<auth value>
   ```

   *On Windows from command prompt:*

   ```ShellSession
   SET AUTH=<auth value>
   ```

1. Set a variable with today's date

   *Example on Mac or Linux:*

   ```bash
   TODAYS_DATE=2020-10-30
   ```

   *On Windows from command prompt:*

   ```ShellSession
   SET TODAYS_DATE=2020-10-30
   ```

1. Create a directory for downloading export files

   *Make sure to change the directory using today's date (YYYY-MM-DD):*

   *Example on Mac or Linux:*

   ```bash
   mkdir -p ~/ab2d/$TODAYS_DATE && TARGET_DIR=~/ab2d/$TODAYS_DATE
   ```

   *Example on Windows from command prompt:*

   ```ShellSession
   mkdir "%HOMEPATH%/documents/%TODAYS_DATE%" && SET TARGET_DIR="%HOMEPATH%/documents/%TODAYS_DATE%"
   ```

1. Start the export job

   *On Mac or Linux:*

   ```bash
   python job-cli.py -sandbox --auth $AUTH --directory $TARGET_DIR --only_start
   ```

   *On Windows from command prompt:*

   ```ShellSession
   python job-cli.py -sandbox --auth %AUTH% --directory %TARGET_DIR% --only_start
   ```

1. Verify that a job id was created

   *On Mac or Linux:*

   ```bash
   cat $TARGET_DIR/job_id.txt
   ```

   *On Windows from command prompt:*

   ```ShellSession
   type %TARGET_DIR%\job_id.txt
   ```

1. Monitor the job

   *On Mac or Linux:*

   ```bash
   python job-cli.py -sandbox --auth $AUTH --directory $TARGET_DIR --only_monitor
   ```

   *On Windows from command prompt:*

   ```ShellSession
   python job-cli.py -sandbox --auth %AUTH% --directory %TARGET_DIR% --only_monitor
   ```

1. Verify that a job completed by verifying the list of files in the response

   *On Mac or Linux:*

   ```bash
   cat $TARGET_DIR/response.json
   ```

   *On Windows from command prompt:*

   ```ShellSession
   type %TARGET_DIR%\response.json
   ```

1. Download the files

   *Note that this process will only download the files once. Running again will not overwrite the files but will also not download anything.*

   *On Mac or Linux:*

   ```bash
   python job-cli.py -sandbox --auth $AUTH --directory $TARGET_DIR --only_download
   ```

   *On Windows from command prompt:*

   ```ShellSession
   python job-cli.py -sandbox --auth %AUTH% --directory %TARGET_DIR% --only_download
   ```

1. List the files that you have downloaded

   *On Mac or Linux:*

   ```bash
   ls $TARGET_DIR/*.ndjson
   ```

   *On Windows from command prompt:*

   ```ShellSession
   dir %TARGET_DIR%\*.ndjson
   ```

1. Stop here, you have completed the file download

### Running All Stages

1. Set the AUTH value

   *On Mac or Linux:*

   ```bash
   AUTH=<auth value>
   ```

   *On Windows from command prompt:*

   ```ShellSession
   SET AUTH=<auth value>
   ```

1. Set a variable with today's date

   *Example on Mac or Linux:*

   ```bash
   TODAYS_DATE=2020-10-30
   ```

   *On Windows from command prompt:*

   ```ShellSession
   SET TODAYS_DATE=2020-10-30
   ```

1. Create a directory for downloading export files

   *Make sure to change the directory using today's date (YYYY-MM-DD):*

   *Example on Mac or Linux:*

   ```bash
   mkdir -p ~/ab2d/$TODAYS_DATE && TARGET_DIR=~/ab2d/$TODAYS_DATE
   ```

   *Example on Windows from command prompt:*

   ```ShellSession
   mkdir "%HOMEPATH%/documents/%TODAYS_DATE%" && SET TARGET_DIR="%HOMEPATH%/documents/%TODAYS_DATE%"
   ```

1. Start the export job, monitor the job, and download the files

   *On Mac or Linux:*

   ```bash
   python job-cli.py -sandbox --auth $AUTH --directory $TARGET_DIR
   ```

   *On Windows from command prompt:*

   ```ShellSession
   python job-cli.py -sandbox --auth %AUTH% --directory %TARGET_DIR%
   ```

1. List the files that you have downloaded

   *On Mac or Linux:*

   ```bash
   ls $TARGET_DIR/*.ndjson
   ```

   *On Windows from command prompt:*

   ```ShellSession
   dir %TARGET_DIR%\*.ndjson
   ```

1. Stop here, you have completed the file download

## Bundling Python and Script

### Build Python in Source

1. Download the latest version of Python as a tar file 
1. Unpack the tar file `tar -xvf python-tar`
1. Change directory to the folder created `cd python-tar`
1. Configure the installer `./configurer --prefix /path/to/ab2d/examples/python/python --with-openssl=$(brew --prefix openssl)`
1. Run `make`
1. Run `make install`
1. Check that the following directories exist in `/path/to/ab2d/examples/python/python`:
`bin, lib, share, include`

### Setup Virtual Environment

Before zipping up Python we need to create a virtual environment. This part assumes access to IntelliJ or PyCharm.

In IntelliJ these are the steps

1. Go to File -> Project Structure
1. Look for Platform Settings -> SDKs and click on it
1. Add a new Python SDK, set the python interpreter as `ab2d/examples/python/python/bin/python3`,
and set the `venv` home as `examples/python/venv`
1. Open a terminal in IntelliJ and in that terminal change directories to `ab2d/examples/python/`
1. Then install the `requests` library by running `venv/bin/pip3 install requests`

### Zip

1. Change directory `cd examples`
1. `zip -r client.zip python`

### Unzip and Run

**When developing move `/path/to/ab2d/examples/python/python` and `/path/to/ab2d/examples/python/venv` to different
directories to check that no symbolic links**

1. Unzip `unzip client.zip`
1. Change directory to the python directory
1. Export the path to the python folder `PYTHONPATH=./python`
1. Run a job by using `/venv/bin/python3 job-cli.py ...`
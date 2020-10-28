# Bash Client

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


## Running

For this example the job is run against sandbox.

### Dependencies

If you are not using a bundled version of this script then you first need to install the required dependencies
using the following steps.

1. Python 3.x: any version of Python3
    1. Check that Python has been added to your path by opening a Terminal, typing `python3`, and hitting enter.
1. Install Pip3: any version of pip3
    1. Check that pip3 has been added to your path by opening a Terminal, typing `pip3`, and hitting enter
    1. If `pip3` does not work check whether it has been aliased to `pip`
    by opening a Terminal, typing `pip --version`, and hitting enter
1. Install the Python requests library by running either of the following commands:
    1. `pip3 install requests`
    1. `pip install requests`


### Running Stages Individually

1. Set the OKTA_CLIENT_ID and OKTA_CLIENT_PASSWORD
   ```bash
   OKTA_CLIENT_ID=<client id>
   OKTA_CLIENT_PASSWORD=<client password>
   ```
1. Create the AUTH token `AUTH=$(echo -n "${OKTA_CLIENT_ID}:${OKTA_CLIENT_PASSWORD}" | base64)`
1. Run `python job-cli.py -sandbox --auth $AUTH --directory <directory> --only_start` to start a job.
If successful a file containing the job id will be saved in `<directory>/jobId.txt`
1. Run `python job-cli.py -sandbox --auth $AUTH --directory <directory> --only_monitor` which will monitor
the state of the running job. When the job finished the full HTTP response will be saved to `<directory>/response.json`
1. Run `python job-cli.py -sandbox --auth $AUTH --directory <directory> --only_download` to get the files.
This will only download the files once. Running again will not overwrite the files but will also not download anything.

### Running All Stages
1. Set the OKTA_CLIENT_ID and OKTA_CLIENT_PASSWORD
   ```bash
   OKTA_CLIENT_ID=<client id>
   OKTA_CLIENT_PASSWORD=<client password>
   ```
1. Create the AUTH token `AUTH=$(echo -n "${OKTA_CLIENT_ID}:${OKTA_CLIENT_PASSWORD}" | base64)`
1. Run `python job-cli.py -sandbox --auth $AUTH --directory <directory>`
to start, monitor, and download results from a job.

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
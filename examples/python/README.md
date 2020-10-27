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

### Setup

Before zipping up Python we need to create a virtual environment. This part assumes access to IntelliJ or PyCharm.

In IntelliJ these are the steps

1. Go to File -> Project Structure
2. Look for Platform Settings -> SDKs and click on it
3. Add a new Python SDK and set the `venv` home as `examples/python/venv`

### Zip

1. Change directory `cd examples`
1. `zip -r client.zip python`

### Unzip and Run

1. Unzip `unzip client.zip`
1. Change directory to the python directory
1. Run a job by using `/venv/bin/python3.8 job-cli.py ...`
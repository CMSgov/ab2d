# AB2D PowerShell Directions

1. Note the following

   - these directions assume that you are on a Windows machine with PowerShell

   - sandbox is publically available

   - production is only accessible if you machine has been whitelisted to use it

1. Open PowerShell

1. Create a download directory where you will be saving the AB2D data

   *Example:*

   ```ShellSession
   New-Item -ItemType directory -Path C:\ab2d\2020-10-01
   ```

1. Change to the download directory

   *Example:*
   
   ```ShellSession
   Set-Location -Path C:\ab2d\2020-10-01
   ```

1. Note that under each of the following steps, you do the following:

   - copy all lines to the clipboard

   - paste all lines into PowerShell

   - presss Enter on the keyboard

1. Set target environment variables for target environment

   *Sandbox (working example):*

   ```ShellSession
   $BASE64_ENCODED_ID_PASSWORD='MG9hMnQwbHNyZFp3NXVXUngyOTc6SEhkdVdHNkxvZ0l2RElRdVdncDNabG85T1lNVmFsVHRINU9CY3VIdw='
   $AUTHENTICATION_URL='https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token'
   $AB2D_API_URL='https://sandbox.ab2d.cms.gov/api'
   ```

   *Production (replace {variable} with your settings):*

   ```ShellSession
   $BASE64_ENCODED_ID_PASSWORD='{Base64-encoded id:password}'
   $AUTHENTICATION_URL='https://idm.cms.gov/oauth2/aus2ytanytjdaF9cr297/v1/token'
   $AB2D_API_URL='https://api.ab2d.cms.gov/api'
   ```
   
1. Create an export job and monitor the status

   ```ShellSession
   $JOB_RESULTS = &.\create-and-monitor-export-job.ps1 | select -Last 1
   ```

1. Download file(s)

   ```ShellSession
   .\download-results.ps1
   ```

1. Open your downloaded file(s) in an editor to view the data

   *Sandbox example of the downloaded file:*

   ```
   C:\ab2d\2020-10-01\Z0000_0001.ndjson
   ```

# AB2D PowerShell Directions

1. Note the following

   - these directions assume that you are on a Windows machine with PowerShell

   - sandbox is publically available

   - production is only accessible if you machine has been whitelisted to use it

1. Open PowerShell as an administrator

   1. Select the Windows icon (likely in the bottom left of the screen)

   1. Type the following in the search text box

      ```
      powershell
      ```

   1. Right click on **Windows PowerShell**

   1. Select **Run as administrator**

   1. If the "User Account Conntrol" window appears, select **Yes**

1. Allow PowerShell to run scripts that are not digitally signed

   ```ShellSession
   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
   ```
   
1. Create a download directory where you will be saving the AB2D data

   *Be sure to change the date to today's date.*
   
   *Example:*

   ```ShellSession
   mkdir $home\documents\2020-10-22
   ```

1. Change to the download directory

   *Be sure to change the date to today's date.*
   
   *Example:*

   ```ShellSession
   cd $home\documents\2020-10-22
   ```

1. Note that under each of the following steps, you do the following:

   - copy all lines to the clipboard

   - paste all lines into PowerShell

   - press Enter on the keyboard

1. Set target environment variables for target environment

   *Sandbox (working example):*

   ```ShellSession
   $BASE64_ENCODED_ID_PASSWORD='MG9hMnQwbHNyZFp3NXVXUngyOTc6SEhkdVdHNkxvZ0l2RElRdVdncDNabG85T1lNVmFsVHRINU9CY3VIdw=='
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
   $home\documents\2020-10-22\Z0000_0001.ndjson
   ```

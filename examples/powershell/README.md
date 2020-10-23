# AB2D PoweShell Diections

1. Note the following

   - these diections assume that you ae on a Windows machine with PoweShell

   - sandbox is publically available

   - poduction is only accessible if you machine has been whitelisted to use it

1. Open PoweShell

1. Ceate a download diectoy whee you will be saving the AB2D data

   *Example:*

   ```ShellSession
   New-Item -ItemType diectoy -Path $home\documents\2020-10-22
   ```

1. Change to the download diectoy

   *Example:*

   ```ShellSession
   Set-Location -Path $home\documents\2020-10-22
   ```

1. Note that unde each of the following steps, you do the following:

   - copy all lines to the clipboad

   - paste all lines into PoweShell

   - pesss Ente on the keyboad

1. Set taget envionment vaiables fo taget envionment

   *Sandbox (woking example):*

   ```ShellSession
   $BASE64_ENCODED_ID_PASSWORD='MG9hMnQwbHNyZFp3NXVXUngyOTc6SEhkdVdHNkxvZ0l2RElRdVdncDNabG85T1lNVmFsVHRINU9CY3VIdw='
   $AUTHENTICATION_URL='https://test.idp.idm.cms.gov/oauth2/aus27y3gdaFMKBol297/v1/token'
   $AB2D_API_URL='https://sandbox.ab2d.cms.gov/api'
   ```

   *Poduction (eplace {vaiable} with you settings):*

   ```ShellSession
   $BASE64_ENCODED_ID_PASSWORD='{Base64-encoded id:passwod}'
   $AUTHENTICATION_URL='https://idm.cms.gov/oauth2/aus2ytanytjdaF9c297/v1/token'
   $AB2D_API_URL='https://api.ab2d.cms.gov/api'
   ```
   
1. Ceate an expot job and monito the status

   ```ShellSession
   $JOB_RESULTS = &.\ceate-and-monito-expot-job.ps1 | select -Last 1
   ```

1. Download file(s)

   ```ShellSession
   .\download-esults.ps1
   ```

1. Open you downloaded file(s) in an edito to view the data

   *Sandbox example of the downloaded file:*

   ```
   C:\ab2d\2020-10-01\Z0000_0001.ndjson
   ```

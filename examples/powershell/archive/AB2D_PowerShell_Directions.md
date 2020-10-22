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

1. Set API variables

   ```ShellSession
   $AUTH = "$BASE64_ENCODED_ID_PASSWORD"
   $OKTA_URI_WITH_PARAMS = "$AUTHENTICATION_URL`?grant_type=client_credentials&scope=clientCreds"
   $EXPORT_URL = "$AB2D_API_URL/v1/fhir/Patient/`$export`?_outputFormat=application%2Ffhir%2Bndjson&_type=ExplanationOfBenefit"
   $AUTH
   $OKTA_URI_WITH_PARAMS
   $EXPORT_URL
   ```

1. Get bearer token

   ```ShellSession
   $headers = New-Object "System.Collections.Generic.Dictionary[[String],[String]]"
   $headers.Add("Authorization", "Basic $AUTH")
   $headers.Add("Accept", "application/json")
   $headers.Add("Content-Type", "application/x-www-form-urlencoded")
   $response = Invoke-RestMethod "$OKTA_URI_WITH_PARAMS" -Method "POST" -Headers $headers -Body $body
   $BEARER_TOKEN = $response.access_token
   $BEARER_TOKEN
   ```

1. Create an export job

   ```ShellSession
   $headers = New-Object "System.Collections.Generic.Dictionary[[String],[String]]"
   $headers.Add("Accept", "application/json")
   $headers.Add("Prefer", "respond-async")
   $headers.Add("Authorization", "Bearer $BEARER_TOKEN")
   $response = Invoke-WebRequest "$EXPORT_URL" -Method 'GET' -Headers $headers -Body $body
   $STATUS_URL = $response.Headers['Content-Location']
   $STATUS_URL
   ```

1. Check job status until you get a status of 200

   ```ShellSession
   $headers = New-Object "System.Collections.Generic.Dictionary[[String],[String]]"
   $headers.Add("Accept", "application/json")
   $headers.Add("Authorization", "Bearer $BEARER_TOKEN")
   $response = Invoke-WebRequest "$STATUS_URL" -Method 'GET' -Headers $headers -Body $body
   $response.StatusCode
   ```
   
1. Get number of files, first file index, and last file index

   ```ShellSession
   $NUMBER_OF_FILES = ($response | ConvertFrom-Json).output.Count
   $FIRST_FILE_INDEX = 0
   $LAST_FILE_INDEX = ($response | ConvertFrom-Json).output.Count - 1
   Write-Host "There are $NUMBER_OF_FILES file(s) with index(es) ranging from $FIRST_FILE_INDEX to $LAST_FILE_INDEX."
   ```
   
1. Download file(s) incrementing the file index after each file is downloaded until the last file index is reached

   ```ShellSession
   $FILE_INDEX = 0
   $FILE_URL = ($response | ConvertFrom-Json).output[$FILE_INDEX].url
   $FILE_URL
   $FILE = $FILE_URL.split("/")[9]
   $FILE
   $headers = New-Object "System.Collections.Generic.Dictionary[[String],[String]]"
   $headers.Add("Authorization", "Bearer $BEARER_TOKEN")
   Invoke-WebRequest "$FILE_URL" -Method 'GET' -Headers $headers -Body $body -Outfile $FILE
   ```

1. Open your downloaded file(s) in an editor to view the data

   *Sandbox example of the downloaded file:*

   ```
   C:\ab2d\2020-10-01\Z0000_0001.ndjson
   ```

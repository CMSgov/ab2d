######################
# download-results.ps1
######################

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

function Get-Bearer-Token {
  $headers = New-Object "System.Collections.Generic.Dictionary[[String],[String]]"
  $headers.Add("Authorization", "Basic $AUTH")
  $headers.Add("Accept", "application/json")
  $headers.Add("Content-Type", "application/x-www-form-urlencoded")
  $response = Invoke-RestMethod "$OKTA_URI_WITH_PARAMS" -Method "POST" -Headers $headers -Body $body
  Write-Host '---------------------------------------------------------------------------------------------------------------------'
  Write-Host 'The latest bearer token used to authenticate with the AB2D API (expires in 1 hour)'
  Write-Host '---------------------------------------------------------------------------------------------------------------------'
  Write-Host $response.access_token
  Write-Host ''
  return $response.access_token
}

# Set API variables

$AUTH = "$BASE64_ENCODED_ID_PASSWORD"
$OKTA_URI_WITH_PARAMS = "$AUTHENTICATION_URL`?grant_type=client_credentials&scope=clientCreds"
$EXPORT_URL = "$AB2D_API_URL/v1/fhir/Patient/`$export`?_outputFormat=application%2Ffhir%2Bndjson&_type=ExplanationOfBenefit"

# Get number of files, first file index, and last file index

$NUMBER_OF_FILES = ($JOB_RESULTS | ConvertFrom-Json).output.Count
$FIRST_FILE_INDEX = 0
$LAST_FILE_INDEX = ($JOB_RESULTS | ConvertFrom-Json).output.Count - 1
Write-Host "There are $NUMBER_OF_FILES file(s) with index(es) ranging from $FIRST_FILE_INDEX to $LAST_FILE_INDEX."
Write-Host ''

# Download file(s) incrementing the file index after each file is downloaded until the last file index is reached

$FILE_INDEX = 0
while ($FILE_INDEX -ne ($LAST_FILE_INDEX + 1)) {
  # Refresh bearer token
  $BEARER_TOKEN = Get-Bearer-Token
  $FILE_URL = ($JOB_RESULTS | ConvertFrom-Json).output[$FILE_INDEX].url
  $FILE = $FILE_URL.split("/")[9]
  Write-Host '---------------------------------------------------------------------------------------------------------------------'
  Write-Host "File URL: $($FILE_URL)"
  Write-Host "Downloading $($FILE)..."
  Write-Host '---------------------------------------------------------------------------------------------------------------------'
  Write-Host ''
  $headers = New-Object "System.Collections.Generic.Dictionary[[String],[String]]"
  $headers.Add("Authorization", "Bearer $BEARER_TOKEN")
  Invoke-WebRequest "$FILE_URL" -Method 'GET' -Headers $headers -Body $body -TimeoutSec 1800 -Outfile $FILE
  $FILE_INDEX++
}

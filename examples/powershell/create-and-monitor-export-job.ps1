###################################
# create-and-monitor-export-job.ps1
###################################

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
Write-Host ''
Write-Host '---------------------------------------------------------------------------------------------------------------------'
Write-Host 'The AUTH used for getting bearer token'
Write-Host '---------------------------------------------------------------------------------------------------------------------'
Write-Host $AUTH
Write-Host ''
Write-Host '---------------------------------------------------------------------------------------------------------------------'
Write-Host 'The OKTA URI used for getting bearer token'
Write-Host '---------------------------------------------------------------------------------------------------------------------'
Write-Host $OKTA_URI_WITH_PARAMS
Write-Host ''
Write-Host '---------------------------------------------------------------------------------------------------------------------'
Write-Host 'The AB2D API endpoint for starting an export job'
Write-Host '---------------------------------------------------------------------------------------------------------------------'
Write-Host $EXPORT_URL
Write-Host ''

# Get initial bearer token

$BEARER_TOKEN = Get-Bearer-Token

# Create an export job

$headers = New-Object "System.Collections.Generic.Dictionary[[String],[String]]"
$headers.Add("Accept", "application/json")
$headers.Add("Prefer", "respond-async")
$headers.Add("Authorization", "Bearer $BEARER_TOKEN")
$response = Invoke-WebRequest "$EXPORT_URL" -Method 'GET' -Headers $headers -Body $body
$STATUS_URL = $response.Headers['Content-Location']
Write-Host '---------------------------------------------------------------------------------------------------------------------'
Write-Host 'The AB2D API status URL that is used to check the status of the job'
Write-Host '---------------------------------------------------------------------------------------------------------------------'
Write-Host $STATUS_URL
Write-Host ''

# Check job status until you get a status of 200

$JOB_COMPLETE = 0
$COUNTER = 0
$SLEEP_TIME_IN_SECONDS = 60
$TOTAL_PROCESSING_TIME = 0
$REFRESH_TOKEN_FACTOR_IN_SECONDS = 1800
while ($response.StatusCode -ne "200") {
  $headers = New-Object "System.Collections.Generic.Dictionary[[String],[String]]"
  $headers.Add("Accept", "application/json")
  $headers.Add("Authorization", "Bearer $BEARER_TOKEN")
  $response = Invoke-WebRequest "$STATUS_URL" -Method 'GET' -Headers $headers -Body $body
  if ($response.StatusCode -ne "200") {
    Write-Host '---------------------------------------------------------------------------------------------------------------------'
    Write-Host "Current status code: $($response.StatusCode)"
    if ($TOTAL_PROCESSING_TIME -ne 0) {
      Write-Host "Job process time (in seconds): $($TOTAL_PROCESSING_TIME)"
    } else {
      Write-Host "Starting job monitoring..."
    }
    Write-Host '---------------------------------------------------------------------------------------------------------------------'
    Write-Host ''
  } else {
    Write-Host '---------------------------------------------------------------------------------------------------------------------'
    Write-Host 'Export job complete'
    Write-Host '---------------------------------------------------------------------------------------------------------------------'
    Write-Host ''
    $JOB_COMPLETE = 1
  }
  if ($JOB_COMPLETE -eq 0) {
    Start-Sleep -Seconds $SLEEP_TIME_IN_SECONDS
    $COUNTER++
    $TOTAL_PROCESSING_TIME += $SLEEP_TIME_IN_SECONDS
    if (($TOTAL_PROCESSING_TIME % $REFRESH_TOKEN_FACTOR_IN_SECONDS) -eq 0) {
      # Refresh bearer token
      $BEARER_TOKEN = Get-Bearer-Token
    }
  }
}

# Return response

return $response

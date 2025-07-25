# AB2D FHIR Testing

AB2D uses the [Inferno Bulk Data Test Kit](https://github.com/inferno-framework/bulk-data-test-kit) to make sure AB2D conforms to the FHIR Implementation Guide.

## Config
The tests that we want to check are specified in [config.json](./config.json).

Tests that are required to pass to maintain the current level of conformance are the "required_tests". Required tests include:
- TLS Tests (`bulk_data_server_tls_version`...)
- Patient Export tests (`bulk_data_patient_export`...)

Tests that are not required because they are not applicable:
- Group Export tests (`bulk_data_group_export`...)
- System-level exports (`bulk_data_system_export`...) are not supported by AB2D
- FHIR Validation (...`_export_validation_stu2`...) is outside the scope of AB2D

## Run the Tests
To run the tests, execute the bash script `run-fhir-test.sh`

The bash script relies on the folowing parameters, and will return an error if any are not set:
  - `BULK_URL` - the AB2D Endpoint you are trying to hit. ex: `'https://sandbox.ab2d.cms.gov/api/v2/fhir/'` for AB2D sandbox
  - `TOKEN_URL` - the full AB2D Auth endpoint used to get an access token
  - `CLIENT_ID` - Client ID used for authenticating
  - `CLIENT_SECRET` - Client Secret used for authenticating


## Results
The script returns each test that is configured in the config.json, and whether it passed or failed.

```sh
- PASS - bulk_data_v200-bulk_data_export_tests_v200-bulk_data_server_tests_stu2-bulk_data_server_tls_version_stu2
- PASS - bulk_data_v200-bulk_data_export_tests_v200-bulk_data_patient_export_v200-bulk_data_patient_export_patient_stu2-bulk_data_patient_export_operation_support
...
```

and then returns a summary of # of tests passed and failed:

```sh
SUMMARY:
 - Tests Passed: 14
 - Tests Failed: 0
 ```
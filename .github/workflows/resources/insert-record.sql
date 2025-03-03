/*

Generates SQL to insert a record into the job table.
For testing, export the variables below:

export JOB_ID=$(uuidgen)
export DATABASE_SCHEMA_NAME='public'
export CONTRACT_NUMBER='Z0001'
export ORGANIZATION='Z0001'
export FHIR_VERSION='R4'
export SINCE='2020-02-13T00:00:00.000-05:00'
export UNTIL="'2024-12-31T00:00:00.000-05:00'"
export API_URL_PREFIX='https://impl.ab2d.cms.gov'

*/
INSERT INTO ${DATABASE_SCHEMA_NAME}.job (
    id,
    job_uuid,
    organization,
    created_at,
    expires_at,
    resource_types,
    status,
    status_message,
    request_url,
    progress,
    contract_number,
    since,
    until,
    fhir_version,
    started_by
)
VALUES (
    (select nextval('hibernate_sequence')),
    '${JOB_ID}',
    '${ORGANIZATION}',
    (select now()),
    (select now() + INTERVAL '1 day'),
    'ExplanationOfBenefit',
    'SUBMITTED',
    '0%',
    '${API_URL_PREFIX}/api/v1/fhir/Patient/$export?_outputFormat=application%252Ffhir%252Bndjson&_type=ExplanationOfBenefit',
    0,
    '${CONTRACT_NUMBER}',
    '${SINCE}',
    ${UNTIL},
    '${FHIR_VERSION}',
    'JENKINS'
);
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
    '${API_URL_PREFIX}/api/v1/fhir/Patient/\$export?_outputFormat=application%252Ffhir%252Bndjson&_type=ExplanationOfBenefit',
    0,
    '${CONTRACT_NUMBER}',
    '${SINCE}',
    ${UNTIL},
    '${FHIR_VERSION}',
    'JENKINS'
);
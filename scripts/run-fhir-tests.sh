# Set up inferno server
docker build -t inferno:1 https://github.com/inferno-framework/bulk-data-test-kit.git
docker compose -f ../ab2d/fhir-test/docker-compose.inferno.yml run inferno bundle exec inferno migrate
docker compose -f ../ab2d/fhir-test/docker-compose.inferno.yml up -d
sleep 10
docker stop fhir-test-hl7_validator_service-1

# Run the tests
docker build --no-cache -t fhir-test -f ../ab2d/fhir-test/Dockerfile.fhir-test .
docker run --network=bridge --rm \
-e BULK_URL="$BULK_URL" \
-e TOKEN_URL="$TOKEN_URL" \
-e CLIENT_ID="$CLIENT_ID" \
-e CLIENT_SECRET="$CLIENT_SECRET" \
fhir-test
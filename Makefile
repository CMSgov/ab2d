docker-build:
	docker-compose up build

fhir_testing:
	# Set up inferno server
	docker build -t inferno:1 https://github.com/inferno-framework/bulk-data-test-kit.git
	docker compose -f ../ab2d/fhir-test/docker-compose.inferno.yml run inferno bundle exec inferno migrate
	docker compose -f ../ab2d/fhir-test/docker-compose.inferno.yml up -d
	sleep 10
	docker stop fhir-test-hl7_validator_service-1

	# Get config
	$(eval CLIENT_ID := $(shell aws ssm get-parameter --name '/ab2d/mgmt/okta/sensitive/test-pdp-100-id' --with-decryption --query 'Parameter.Value' --output text))
	$(eval CLIENT_SECRET := $(shell aws ssm get-parameter --name '/ab2d/mgmt/okta/sensitive/test-pdp-100-secret' --with-decryption --query 'Parameter.Value' --output text))
	$(eval BULK_URL = 'http://host.docker.internal:8443/api/v2/fhir/')
	$(eval TOKEN_URL = 'https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token')

	# Run the tests
	docker build --no-cache -t fhir_testing -f ../ab2d/fhir-test/Dockerfile.fhir-test .
	@docker run --network=bridge --rm \
	-e BULK_URL='${BULK_URL}' \
	-e TOKEN_URL='${TOKEN_URL}' \
	-e CLIENT_ID='${CLIENT_ID}' \
	-e CLIENT_SECRET='${CLIENT_SECRET}' \
	fhir_testing

.PHONY: docker-build fhir_testing
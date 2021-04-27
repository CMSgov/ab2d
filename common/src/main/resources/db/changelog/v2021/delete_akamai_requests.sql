--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:delete_akamai_records failOnError:true

DELETE FROM event_api_response eap
USING event_api_request ear
WHERE ear.request_id = eap.request_id AND ear.url LIKE '%akamai%';

DELETE FROM event_api_request ear
WHERE ear.url LIKE '%akamai%';
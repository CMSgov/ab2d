docker-build:
    docker-compose rm -f
    docker-compose pull
	docker-compose up build --no-cache
.PHONY: docker-build
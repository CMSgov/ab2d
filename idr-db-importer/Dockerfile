FROM --platform=linux/arm64 amazoncorretto:25-alpine3.23

WORKDIR /usr/src/ab2d-idr-db-importer

RUN apk -U upgrade --no-cache

COPY build/libs/ab2d-idr-db-importer-*-SNAPSHOT.jar /usr/src/ab2d-idr-db-importer/ab2d-idr-db-importer.jar

CMD java \
    --add-opens=java.base/java.nio=ALL-UNNAMED \
    -XX:+UseContainerSupport \
    -jar ab2d-idr-db-importer.jar
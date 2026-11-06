FROM --platform=linux/arm64 amazoncorretto:25-al2023-jdk

WORKDIR /usr/src/ab2d-idr-db-importer

RUN yum update -y --security

COPY build/libs/ab2d-idr-db-importer-*-SNAPSHOT.jar /usr/src/ab2d-idr-db-importer/ab2d-idr-db-importer.jar

CMD java \
    --add-opens=java.base/java.nio=ALL-UNNAMED \
    -XX:+UseContainerSupport \
    -jar ab2d-idr-db-importer.jar
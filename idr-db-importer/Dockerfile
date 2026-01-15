FROM amazoncorretto:21-al2023-jdk
WORKDIR /usr/src/ab2d-idr-db-importer
ADD target/idr-db-importer-*-SNAPSHOT.jar /usr/src/ab2d-idr-db-importer/ab2d-idr-db-importer.jar

CMD java \
    -XX:+UseContainerSupport \
    -jar ab2d-idr-db-importer.jar

FROM --platform=linux/arm64 amazoncorretto:25-alpine3.23

WORKDIR /usr/src/ab2d-idr-db-importer

RUN apk -U upgrade --no-cache

# Distrust eMudhra emSign roots (eMudhra intermediate CA key compromise, 2026-10-20).
RUN keytool -delete -cacerts -storepass changeit -alias emsigneccrootca-c3 ; \
    keytool -delete -cacerts -storepass changeit -alias emsigneccrootca-g3 ; \
    keytool -delete -cacerts -storepass changeit -alias "emsigneccrootcag3 [jdk]" ; \
    keytool -delete -cacerts -storepass changeit -alias emsignrootca-c1 ; \
    keytool -delete -cacerts -storepass changeit -alias emsignrootca-g1 ; \
    keytool -delete -cacerts -storepass changeit -alias "emsignrootcag1 [jdk]" ; \
    keytool -delete -cacerts -storepass changeit -alias "emsignrootcag2 [jdk]" ; \
    ! keytool -list -cacerts -storepass changeit | grep -qi emsign

COPY build/libs/ab2d-idr-db-importer-*-SNAPSHOT.jar /usr/src/ab2d-idr-db-importer/ab2d-idr-db-importer.jar

CMD java \
    --add-opens=java.base/java.nio=ALL-UNNAMED \
    -XX:+UseContainerSupport \
    -jar ab2d-idr-db-importer.jar
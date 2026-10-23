FROM amazoncorretto:25-al2023-jdk
WORKDIR /usr/src/ab2d-events
ADD build/libs/*.jar /usr/src/ab2d-events/ab2d-event.jar

RUN yum update -y --security

# Distrust eMudhra emSign roots (eMudhra intermediate CA key compromise, 2026-10-20).
RUN keytool -exportcert -rfc -cacerts -storepass changeit -alias emsigneccrootca-c3 -file /etc/pki/ca-trust/source/blocklist/emsigneccrootca-c3.pem ; \
    keytool -exportcert -rfc -cacerts -storepass changeit -alias emsigneccrootca-g3 -file /etc/pki/ca-trust/source/blocklist/emsigneccrootca-g3.pem ; \
    keytool -exportcert -rfc -cacerts -storepass changeit -alias emsignrootca-c1    -file /etc/pki/ca-trust/source/blocklist/emsignrootca-c1.pem ; \
    keytool -exportcert -rfc -cacerts -storepass changeit -alias emsignrootca-g1    -file /etc/pki/ca-trust/source/blocklist/emsignrootca-g1.pem ; \
    update-ca-trust && \
    ! keytool -list -cacerts -storepass changeit | grep -qi emsign

CMD java \
    -XX:+UseContainerSupport \
    -jar ab2d-event.jar

EXPOSE 8050

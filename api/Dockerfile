FROM amazoncorretto:25-al2023-jdk
WORKDIR /usr/src/ab2d-api
COPY target/api-*-SNAPSHOT.jar /usr/src/ab2d-api/api.jar
COPY target/datadog/dd-java-agent.jar /usr/src/ab2d-api/datadog/dd-java-agent.jar

# When running in production with one container per EC2 instance these values are acceptable
ENV JVM_INITIAL_RAM=40.0
ENV JVM_MIN_RAM=20.0
ENV JVM_MAX_RAM=80.0

#Update Packages
RUN yum update -y --security

# Distrust eMudhra emSign roots (eMudhra intermediate CA key compromise, 2026-10-20).
RUN keytool -exportcert -rfc -cacerts -storepass changeit -alias emsigneccrootca-c3 -file /etc/pki/ca-trust/source/blocklist/emsigneccrootca-c3.pem ; \
    keytool -exportcert -rfc -cacerts -storepass changeit -alias emsigneccrootca-g3 -file /etc/pki/ca-trust/source/blocklist/emsigneccrootca-g3.pem ; \
    keytool -exportcert -rfc -cacerts -storepass changeit -alias emsignrootca-c1    -file /etc/pki/ca-trust/source/blocklist/emsignrootca-c1.pem ; \
    keytool -exportcert -rfc -cacerts -storepass changeit -alias emsignrootca-g1    -file /etc/pki/ca-trust/source/blocklist/emsignrootca-g1.pem ; \
    update-ca-trust && \
    ! keytool -list -cacerts -storepass changeit | grep -qi emsign

#JAVA Commands
CMD ["sh", "-c", "exec java \
    -XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=${JVM_INITIAL_RAM} \
    -XX:MinRAMPercentage=${JVM_MIN_RAM} \
    -XX:MaxRAMPercentage=${JVM_MAX_RAM} \
    -javaagent:/usr/src/ab2d-api/datadog/dd-java-agent.jar \
    -jar api.jar"]
EXPOSE 8443

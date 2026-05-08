#AWS Corretto and AL2
FROM amazoncorretto:25-al2023-jdk
WORKDIR /usr/src/ab2d-worker
COPY target/worker-*-SNAPSHOT-exe.jar /usr/src/ab2d-worker/worker.jar
COPY target/newrelic/newrelic.jar /usr/src/ab2d-worker/newrelic/newrelic.jar

# When running in production with one container per EC2 instance these values are acceptable
ENV JVM_INITIAL_RAM=40.0
ENV JVM_MIN_RAM=20.0
ENV JVM_MAX_RAM=80.0
ENV NEW_RELIC_EXPERIMENTAL_RUNTIME=true

#Update Packages
RUN yum update -y --security

#JAVA commands
CMD java \
    -XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=${JVM_INITIAL_RAM} \
    -XX:MinRAMPercentage=${JVM_MIN_RAM} \
    -XX:MaxRAMPercentage=${JVM_MAX_RAM} \
    -javaagent:/usr/src/ab2d-worker/newrelic/newrelic.jar \
    -jar worker.jar

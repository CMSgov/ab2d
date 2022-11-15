FROM amazoncorretto:17-al2-jdk
WORKDIR /usr/src/ab2d-api
ADD target/api-*-SNAPSHOT.jar /usr/src/ab2d-api/api.jar
ADD target/newrelic/newrelic.jar /usr/src/ab2d-api/newrelic/newrelic.jar

# When running in production with one container per EC2 instance these values are acceptable
ENV JVM_INITIAL_RAM=40.0
ENV JVM_MIN_RAM=20.0
ENV JVM_MAX_RAM=80.0

#Update Packages
RUN yum update -y --security

#JAVA Commands
CMD java \
    -XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=${JVM_INITIAL_RAM} \
    -XX:MinRAMPercentage=${JVM_MIN_RAM} \
    -XX:MaxRAMPercentage=${JVM_MAX_RAM} \
    -javaagent:/usr/src/ab2d-api/newrelic/newrelic.jar \
    -jar api.jar
EXPOSE 8443

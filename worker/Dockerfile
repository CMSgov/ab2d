FROM openjdk:13
WORKDIR /tmp/src/ab2d-worker
ADD target /tmp/src/ab2d-worker
CMD java \
    -XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=40.0 \
    -XX:MinRAMPercentage=20.0 \
    -XX:MaxRAMPercentage=80.0 \
    -javaagent:/tmp/src/ab2d-worker/newrelic/newrelic.jar \
    -jar worker-*-SNAPSHOT.jar

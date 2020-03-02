FROM openjdk:13
WORKDIR /usr/src/ab2d-api
ADD target /usr/src/ab2d-api
CMD java \
    -XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=40.0 \
    -XX:MinRAMPercentage=20.0 \
    -XX:MaxRAMPercentage=80.0 \
    -javaagent:/usr/src/ab2d-api/newrelic/newrelic.jar \
    -jar api-*-SNAPSHOT.jar
EXPOSE 8080
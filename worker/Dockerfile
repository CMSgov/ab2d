FROM openjdk:13
WORKDIR /usr/src/ab2d-worker
ADD target /usr/src/ab2d-worker
CMD java -javaagent:/usr/src/ab2d-api/newrelic/newrelic.jar -jar worker-*-SNAPSHOT.jar
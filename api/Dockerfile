FROM openjdk:13
WORKDIR /usr/src/ab2d-api
ADD target /usr/src/ab2d-api
CMD java -javaagent:/usr/src/ab2d-api/newrelic/newrelic.jar -jar api-*-SNAPSHOT.jar
EXPOSE 8080
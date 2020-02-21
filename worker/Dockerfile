#FROM openjdk:13
#WORKDIR /usr/src/ab2d-worker
#ADD target /usr/src/ab2d-worker
#CMD java -javaagent:/usr/src/ab2d-worker/newrelic/newrelic.jar -jar worker-*-SNAPSHOT.jar

FROM openjdk:13
VOLUME /tmp
ARG JAR_FILE=target/worker-*-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
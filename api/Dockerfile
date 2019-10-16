FROM openjdk:12
WORKDIR /usr/src/ab2d-api
ADD target /usr/src/ab2d-api
CMD java -jar -Xverify:none api-*-SNAPSHOT.jar
EXPOSE 8080
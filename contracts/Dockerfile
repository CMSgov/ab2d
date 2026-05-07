FROM amazoncorretto:21.0.10-alpine3.23
WORKDIR /usr/src/ab2d-contracts
ADD build/libs/contracts-*.jar /usr/src/ab2d-contracts/ab2d-contracts.jar
CMD java -jar /usr/src/ab2d-contracts/ab2d-contracts.jar

CMD java \
    -XX:+UseContainerSupport \
    -jar ab2d-contracts.jar

EXPOSE 8070

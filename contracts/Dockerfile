FROM amazoncorretto:21-al2023-jdk
WORKDIR /usr/src/ab2d-contracts
ADD ./build/libs/ab2d-contracts-*.jar /usr/src/ab2d-contracts/ab2d-contracts.jar
CMD java -jar /usr/src/ab2d-contracts/ab2d-contracts.jar

CMD java \
    -XX:+UseContainerSupport \
    -jar ab2d-contracts.jar

EXPOSE 8070

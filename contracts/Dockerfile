FROM amazoncorretto:25-al2023-jdk
WORKDIR /usr/src/ab2d-contracts
ADD build/libs/contracts-*.jar /usr/src/ab2d-contracts/ab2d-contracts.jar

RUN yum update -y --security

CMD java \
    -XX:+UseContainerSupport \
    -jar ab2d-contracts.jar

EXPOSE 8070

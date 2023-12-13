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

#Install PG_Cron 15
RUN git clone https://github.com/citusdata/pg_cron.git
RUN cd pg_cron

# Ensure pg_config is in your path, e.g.
RUN export PATH=/usr/pgsql-15/bin:$PATH
RUN make && sudo PATH=$PATH make install
RUN cd ..

#JAVA Commands
CMD java \
    -XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=${JVM_INITIAL_RAM} \
    -XX:MinRAMPercentage=${JVM_MIN_RAM} \
    -XX:MaxRAMPercentage=${JVM_MAX_RAM} \
    -javaagent:/usr/src/ab2d-api/newrelic/newrelic.jar \
    -jar api.jar
EXPOSE 8443

#AWS Corretto and AL2
FROM amazoncorretto:17-al2-jdk
WORKDIR /usr/src/ab2d-worker
ADD target/worker-*-SNAPSHOT-exe.jar /usr/src/ab2d-worker/worker.jar
ADD target/newrelic/newrelic.jar /usr/src/ab2d-worker/newrelic/newrelic.jar

# When running in production with one container per EC2 instance these values are acceptable
ENV JVM_INITIAL_RAM=40.0
ENV JVM_MIN_RAM=20.0
ENV JVM_MAX_RAM=80.0

#Update Packages
RUN yum update -y --security
RUN yum install -y git

#Install PG_Cron 15
RUN git clone https://github.com/citusdata/pg_cron.git
RUN cd pg_cron

# Ensure pg_config is in your path, e.g.
RUN export PATH=/usr/pgsql-15/bin:$PATH
RUN make && sudo PATH=$PATH make install
RUN cd ..

#JAVA commands
CMD java \
    -XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=${JVM_INITIAL_RAM} \
    -XX:MinRAMPercentage=${JVM_MIN_RAM} \
    -XX:MaxRAMPercentage=${JVM_MAX_RAM} \
    -javaagent:/usr/src/ab2d-worker/newrelic/newrelic.jar \
    -jar worker.jar

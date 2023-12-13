FROM postgres:15.2
RUN apt-get update && apt-get install -y curl
RUN apt-get -y install postgresql-15-cron
FROM postgres:15.2
RUN apt-get update
RUN apt-get install -y curl postgresql-15-cron
FROM postgres:15-bullseye
RUN apt-get update
RUN apt-get install -y curl postgresql-15-cron
RUN echo "pg_cron_15 Installed..."
# COPY postgresql.conf /var/lib/postgresql/data/postgresql.conf
RUN echo "shared_preload_libraries = 'pg_cron'" >> /var/lib/postgresql/data/postgresql.conf
# RUN cat /var/lib/postgresql/data/postgresql.conf
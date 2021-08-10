--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset sb-wnyffenegger:coverage-fake-partitions failOnError:true context:test

create table if not exists tst partition of coverage_partitioned
    for values in ('TST-12', 'TST-34', 'TST-56', 'TST-78', 'TST-90') partition by list(year);

create table if not exists tst_2020 partition of tst
    for values in (2020);

create table if not exists tst_2021 partition of tst
    for values in (2021);

create table if not exists tst_2022 partition of tst
    for values in (2022);

create table if not exists tst_2023 partition of tst
    for values in (2023);

-- drop table if exists coverage;
--
-- alter table if exists coverage_partitioned rename to coverage;

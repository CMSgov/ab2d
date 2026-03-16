--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset sb-wnyffenegger:coverage-fake-partitions failOnError:true context:test

-- May be removed or altered in future once cutover to partitioned coverage table is complete
-- this is used for testing code changes locally

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

create table if not exists tst_2024 partition of tst
    for values in (2024);

create table if not exists tst_2025 partition of tst
    for values in (2025);

create table if not exists tst_2026 partition of tst
    for values in (2026);

create table if not exists tst_2027 partition of tst
    for values in (2027);

create table if not exists tst_2028 partition of tst
    for values in (2028);

create table if not exists tst_2029 partition of tst
    for values in (2029);

create table if not exists tst_2030 partition of tst
    for values in (2030);

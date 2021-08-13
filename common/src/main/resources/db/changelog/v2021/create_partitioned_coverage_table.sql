--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset sb-wnyffenegger:create_partitioned_coverage_table failOnError:true

create table coverage_partitioned
(
    bene_coverage_period_id integer not null
        constraint fk_coverage_to_bene_coverage_period
            references bene_coverage_period,
    bene_coverage_search_event_id bigint not null
        constraint fk_coverage_to_bene_coverage_search_event
            references event_bene_coverage_search_status_change,
    contract varchar(15) not null,
    year integer not null,
    month integer not null,
    beneficiary_id bigint not null,
    current_mbi varchar(32),
    historic_mbis varchar(256),
    primary key (beneficiary_id, contract, year, month, bene_coverage_search_event_id)
)
    partition by list(contract);

create index on coverage_partitioned (bene_coverage_period_id, beneficiary_id, contract, year);
create index on coverage_partitioned (bene_coverage_search_event_id, beneficiary_id, contract, year);



create table sandbox partition of coverage_partitioned
    for values in ('Z0000', 'Z0001', 'Z0002', 'Z0005', 'Z0010') partition by list(year);

create table sandbox_2020 partition of sandbox
    for values in (2020);

create table sandbox_2021 partition of sandbox
    for values in (2021);

create table sandbox_2022 partition of sandbox
    for values in (2022);

create table sandbox_2023 partition of sandbox
    for values in (2023);



create table coverage_anthem_united partition of coverage_partitioned
    for values in ('S8182', 'S5596', 'S3375', 'S5805', 'S8841')
    partition by list(year);

create table coverage_anthem_united_2020 partition of coverage_anthem_united
    for values in (2020);

create table coverage_anthem_united_2021 partition of coverage_anthem_united
    for values in (2021);

create table coverage_anthem_united_2022 partition of coverage_anthem_united
    for values in (2022);

create table coverage_anthem_united_2023 partition of coverage_anthem_united
    for values in (2023);



create table coverage_bcbs partition of coverage_partitioned
    for values in ('S5960', 'S2893', 'S5743', 'S5540', 'S6506', 'S5726', 'S5584', 'S1030',
        'S5953', 'S5450', 'S2468', 'S8067', 'S5715', 'S5593', 'S1140', 'S5993', 'S6875')
    partition by list(year);

create table coverage_bcbs_2020 partition of coverage_bcbs
    for values in (2020);

create table coverage_bcbs_2021 partition of coverage_bcbs
    for values in (2021);

create table coverage_bcbs_2022 partition of coverage_bcbs
    for values in (2022);

create table coverage_bcbs_2023 partition of coverage_bcbs
    for values in (2023);



create table coverage_centene partition of coverage_partitioned
    for values in ('S4802', 'S5810', 'S5768')
    partition by list(year);

create table coverage_centene_2020 partition of coverage_centene
    for values in (2020);

create table coverage_centene_2021 partition of coverage_centene
    for values in (2021);

create table coverage_centene_2022 partition of coverage_centene
    for values in (2022);

create table coverage_centene_2023 partition of coverage_centene
    for values in (2023);



create table coverage_cigna1 partition of coverage_partitioned
    for values in ('S5617', 'S5983')
    partition by list(year);

create table coverage_cigna1_2020 partition of coverage_cigna1
    for values in (2020);

create table coverage_cigna1_2021 partition of coverage_cigna1
    for values in (2021);

create table coverage_cigna1_2022 partition of coverage_cigna1
    for values in (2022);

create table coverage_cigna1_2023 partition of coverage_cigna1
    for values in (2023);



create table coverage_cigna2 partition of coverage_partitioned
    for values in ('S5660')
    partition by list(year);

create table coverage_cigna2_2020 partition of coverage_cigna2
    for values in (2020);

create table coverage_cigna2_2021 partition of coverage_cigna2
    for values in (2021);

create table coverage_cigna2_2022 partition of coverage_cigna2
    for values in (2022);

create table coverage_cigna2_2023 partition of coverage_cigna2
    for values in (2023);



create table coverage_cvs partition of coverage_partitioned
    for values in ('S5601')
    partition by list(year);

create table coverage_cvs_2020 partition of coverage_cvs
    for values in (2020);

create table coverage_cvs_2021 partition of coverage_cvs
    for values in (2021);

create table coverage_cvs_2022 partition of coverage_cvs
    for values in (2022);

create table coverage_cvs_2023 partition of coverage_cvs
    for values in (2023);



create table coverage_humana partition of coverage_partitioned
    for values in ('S5884', 'S5552', 'S2874')
    partition by list(year);

create table coverage_humana_2020 partition of coverage_humana
    for values in (2020);

create table coverage_humana_2021 partition of coverage_humana
    for values in (2021);

create table coverage_humana_2022 partition of coverage_humana
    for values in (2022);

create table coverage_humana_2023 partition of coverage_humana
    for values in (2023);



create table coverage_united1 partition of coverage_partitioned
    for values in ('S5921')
    partition by list(year);

create table coverage_united1_2020 partition of coverage_united1
    for values in (2020);

create table coverage_united1_2021 partition of coverage_united1
    for values in (2021);

create table coverage_united1_2022 partition of coverage_united1
    for values in (2022);

create table coverage_united1_2023 partition of coverage_united1
    for values in (2023);



create table coverage_united2 partition of coverage_partitioned
    for values in ('S5820')
    partition by list(year);

create table coverage_united_2020 partition of coverage_united2
    for values in (2020);

create table coverage_united_2021 partition of coverage_united2
    for values in (2021);

create table coverage_united_2022 partition of coverage_united2
    for values in (2022);

create table coverage_united_2023 partition of coverage_united2
    for values in (2023);



create table coverage_mutual_dean_clear_cambia_rite partition of coverage_partitioned
    for values in ('S6946', 'S5609', 'S5916', 'S7126', 'S9701', 'S7694')
    partition by list(year);

create table coverage_mutual_dean_clear_cambia_rite_2020 partition of coverage_mutual_dean_clear_cambia_rite
    for values in (2020);

create table coverage_mutual_dean_clear_cambia_rite_2021 partition of coverage_mutual_dean_clear_cambia_rite
    for values in (2021);

create table coverage_mutual_dean_clear_cambia_rite_2022 partition of coverage_mutual_dean_clear_cambia_rite
    for values in (2022);

create table coverage_mutual_dean_clear_cambia_rite_2023 partition of coverage_mutual_dean_clear_cambia_rite
    for values in (2023);



create table coverage_misc partition of coverage_partitioned
    for values in ('E3014', 'S5877', 'S5966', 'S9325', 'S5904', 'S3994', 'S0655', 'S1822', 'E0654',
        'S4501', 'S3521', 'S3875', 'S3285', 'E4744', 'S5975', 'S0586', 'S5588', 'S8677', 'S2465',
        'S5857', 'S1894', 'S2668', 'S4219', 'S3389', 'S5795', 'S5753')
    partition by list(year);

create table coverage_misc_2020 partition of coverage_misc
    for values in (2020);

create table coverage_misc_2021 partition of coverage_misc
    for values in (2021);

create table coverage_misc_2022 partition of coverage_misc
    for values in (2022);

create table coverage_misc_2023 partition of coverage_misc
    for values in (2023);

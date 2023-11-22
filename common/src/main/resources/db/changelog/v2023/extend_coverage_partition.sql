create table sandbox_2024 partition of sandbox
    for values in (2024);
create table sandbox_2025 partition of sandbox
    for values in (2025);

create table coverage_anthem_united_2024 partition of coverage_anthem_united
    for values in (2024);
create table coverage_anthem_united_2025 partition of coverage_anthem_united
    for values in (2025);

create table coverage_bcbs_2024 partition of coverage_bcbs
    for values in (2024);
create table coverage_bcbs_2025 partition of coverage_bcbs
    for values in (2025);

create table coverage_centene_2024 partition of coverage_centene
    for values in (2024);
create table coverage_centene_2025 partition of coverage_centene
    for values in (2025);

create table coverage_cigna1_2024 partition of coverage_cigna1
    for values in (2024);
create table coverage_cigna1_2025 partition of coverage_cigna1
    for values in (2025);

create table coverage_cigna2_2024 partition of coverage_cigna2
    for values in (2024);
create table coverage_cigna2_2025 partition of coverage_cigna2
    for values in (2025);

create table coverage_cvs_2024 partition of coverage_cvs
    for values in (2024);
create table coverage_cvs_2025 partition of coverage_cvs
    for values in (2025);

create table coverage_humana_2024 partition of coverage_humana
    for values in (2024);
create table coverage_humana_2025 partition of coverage_humana
    for values in (2025);

create table coverage_united1_2024 partition of coverage_united1
    for values in (2024);
create table coverage_united1_2025 partition of coverage_united1
    for values in (2025);

create table coverage_united_2024 partition of coverage_united2
    for values in (2024);
create table coverage_united_2025 partition of coverage_united2
    for values in (2025);

create table coverage_mutual_dean_clear_cambia_rite_2024 partition of coverage_mutual_dean_clear_cambia_rite
    for values in (2024);
create table coverage_mutual_dean_clear_cambia_rite_2025 partition of coverage_mutual_dean_clear_cambia_rite
    for values in (2025);

create table coverage_misc_2024 partition of coverage_misc
    for values in (2024);
create table coverage_misc_2025 partition of coverage_misc
    for values in (2025);
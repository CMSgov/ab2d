--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset adaykin:modify_hpms_ids failOnError:true

UPDATE sponsor SET hpms_id = hpms_id * 10000;
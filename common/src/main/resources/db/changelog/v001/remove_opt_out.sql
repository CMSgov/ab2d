--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:remove_opt_out failOnError:true

drop table opt_out_file;
drop table opt_out;

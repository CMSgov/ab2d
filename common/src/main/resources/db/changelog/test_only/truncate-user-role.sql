--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset AB2D-2921 failOnError:true context:test
truncate user_role;
truncate user_account cascade;
truncate role cascade;


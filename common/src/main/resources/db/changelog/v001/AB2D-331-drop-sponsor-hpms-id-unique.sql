--changeset adaykin:AB2D-331-DropContstraint failOnError:true dbms:postgresql
ALTER TABLE sponsor DROP CONSTRAINT "uc_sponsor_hpms_id";
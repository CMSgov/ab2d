--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:ab2d_1167_add_column_to_opt_out_table failOnError:true
ALTER TABLE opt_out
 ADD COLUMN filename        VARCHAR(255)    NOT NULL;

--rollback  ALTER TABLE opt_out DROP COLUMN filename;


--changeset spathiyil:ab2d_1167_add_indexes_on_opt_out_table failOnError:true
CREATE INDEX "ix_opt_out_ccw_id"        ON opt_out (ccw_id);
CREATE INDEX "ix_opt_out_ccw_id_hicn"   ON opt_out (ccw_id, hicn);

--rollback DROP INDEX "ix_opt_out_ccw_id";
--rollback DROP INDEX "ix_opt_out_ccw_id_hicn";

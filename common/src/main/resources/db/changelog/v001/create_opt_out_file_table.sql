CREATE TABLE opt_out_file (
    id BIGINT NOT NULL,
    filename VARCHAR(256) NOT NULL
);

ALTER TABLE opt_out_file ADD CONSTRAINT "pk_opt_out_file" PRIMARY KEY (id);
ALTER TABLE opt_out_file ADD CONSTRAINT "uc_opt_out_file_filename" UNIQUE (filename);

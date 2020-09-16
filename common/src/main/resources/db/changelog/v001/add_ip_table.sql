CREATE TABLE sponsor_ip (
    sponsor_id BIGINT NOT NULL,
    ip_address INET,
    PRIMARY KEY (sponsor_id, ip_address)
);

ALTER TABLE sponsor_ip ADD CONSTRAINT "fk_sponsor_ip_to_sponsor"  FOREIGN KEY (sponsor_id) REFERENCES sponsor (id);

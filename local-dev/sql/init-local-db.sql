DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'cmsadmin') THEN
        CREATE ROLE cmsadmin;
    END IF;
END $$;

CREATE SCHEMA IF NOT EXISTS contract AUTHORIZATION ab2d;

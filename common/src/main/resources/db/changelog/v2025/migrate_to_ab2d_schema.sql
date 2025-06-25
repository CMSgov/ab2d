
-- Create the new schema
CREATE SCHEMA IF NOT EXISTS ab2d;



-- Move tables
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT tablename FROM pg_tables WHERE schemaname = 'public'
    LOOP
        EXECUTE 'ALTER TABLE public.' || quote_ident(r.tablename) || ' SET SCHEMA ab2d;';
    END LOOP;
END $$;

-- Move views
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT table_name FROM information_schema.views WHERE table_schema = 'public'
    LOOP
        EXECUTE 'ALTER VIEW public.' || quote_ident(r.table_name) || ' SET SCHEMA ab2d;';
    END LOOP;
END $$;

--  Move sequences
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = 'public'
    LOOP
        EXECUTE 'ALTER SEQUENCE public.' || quote_ident(r.sequence_name) || ' SET SCHEMA ab2d;';
    END LOOP;
END $$;

--  Move procedures
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT p.oid, n.nspname, p.proname,
               pg_get_function_identity_arguments(p.oid) AS args
        FROM pg_proc p
        JOIN pg_namespace n ON p.pronamespace = n.oid
        WHERE n.nspname = 'public'
    LOOP
		EXECUTE 'ALTER PROCEDURE public.' || quote_ident(r.proname) || '(' || r.args || ') SET SCHEMA ab2d;';
    END LOOP;
END $$;

-- Set search_path so ab2d is used before public
ALTER ROLE postgres SET search_path TO ab2d, public;

-- Revoke CREATE privileges
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

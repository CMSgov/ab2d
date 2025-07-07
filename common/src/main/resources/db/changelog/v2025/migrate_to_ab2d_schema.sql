CREATE SCHEMA IF NOT EXISTS ab2d;
ALTER ROLE cmsadmin SET search_path TO ab2d,public;
SET search_path to ab2d,public;

-- Move tables except databasechangelog and databasechangeloglock under 'public'
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT tablename FROM pg_tables WHERE schemaname = 'public'
    LOOP
        IF r.tablename not like '%databasechangelog%' THEN
    	    EXECUTE 'ALTER TABLE public.' || quote_ident(r.tablename) || ' SET SCHEMA ab2d;';
    	END IF;
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

--  Move procedures (where prokind='p')
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT p.oid, p.prokind, n.nspname, p.proname,
               pg_get_function_identity_arguments(p.oid) AS args
        FROM pg_proc p
        JOIN pg_namespace n ON p.pronamespace = n.oid
        WHERE n.nspname = 'public' and p.prokind = 'p'
    LOOP
		EXECUTE 'ALTER PROCEDURE public.' || quote_ident(r.proname) || '(' || r.args || ') SET SCHEMA ab2d;';
    END LOOP;
END $$;

--  Move functions (where prokind='f')
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT p.oid, p.prokind, n.nspname, p.proname,
               pg_get_function_identity_arguments(p.oid) AS args
        FROM pg_proc p
        JOIN pg_namespace n ON p.pronamespace = n.oid
        WHERE n.nspname = 'public' and p.prokind = 'f'
    LOOP
		EXECUTE 'ALTER FUNCTION public.' || quote_ident(r.proname) || '(' || r.args || ') SET SCHEMA ab2d;';
    END LOOP;
END $$;


-- Revoke CREATE privileges
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

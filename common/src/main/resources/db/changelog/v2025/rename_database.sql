-- (1) Kill any other backends first, if needed:
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = current_database()
  AND pid   <> pg_backend_pid();

-- (2) Conditionally rename to “main” only for the envs:
DO
$$
BEGIN
  IF current_database() IN ('dev','impl','sbx','prod') THEN
    EXECUTE format(
      'ALTER DATABASE %I RENAME TO main',
      current_database()
    );
END IF;
END
$$;
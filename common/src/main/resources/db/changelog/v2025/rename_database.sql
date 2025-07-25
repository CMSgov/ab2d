SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = current_database()
  AND pid   <> pg_backend_pid();


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
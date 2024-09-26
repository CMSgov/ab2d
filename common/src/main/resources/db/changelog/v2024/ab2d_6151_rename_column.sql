DO $$
BEGIN
    IF EXISTS (SELECT 1
     FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'current_mbi' AND
column_name = 'opt_out_flag')
    THEN
ALTER TABLE public.current_mbi RENAME opt_out_flag TO share_data;
END IF;
END
$$;
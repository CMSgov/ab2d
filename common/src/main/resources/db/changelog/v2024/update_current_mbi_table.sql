ALTER TABLE public.current_mbi ADD COLUMN IF NOT EXISTS effective_date TIMESTAMP;
ALTER TABLE public.current_mbi ADD COLUMN IF NOT EXISTS opt_out_flag BOOLEAN DEFAULT 'FALSE';

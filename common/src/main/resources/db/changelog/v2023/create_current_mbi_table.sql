CREATE TABLE IF NOT EXISTS public.current_mbi (mbi VARCHAR(32) NOT NULL);
CREATE UNIQUE INDEX unique_mbi ON public.current_mbi(mbi);

INSERT INTO public.current_mbi
SELECT DISTINCT current_mbi from coverage_anthem_united
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_bcbs
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_centene
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_cigna1
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_cigna2
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_cvs
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_centene
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_humana
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_misc
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_mutual_dean_clear_cambia_rite
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_united1
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_united2
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_default
UNION DISTINCT
SELECT DISTINCT current_mbi from sandbox
ON CONFLICT DO NOTHING
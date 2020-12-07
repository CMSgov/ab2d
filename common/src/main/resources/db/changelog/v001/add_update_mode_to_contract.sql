ALTER TABLE contract ADD COLUMN "update_mode" VARCHAR(32) NOT NULL DEFAULT('AUTOMATIC');  -- Is there a standard size for an enum?  32 is arbitrary

-- Fix the test contracts
update contract
set update_mode = 'TEST'
where contract_name like 'Z%';

------------------------------
-- Create ab2d_analyst user
------------------------------
DO
$$
    begin
        if not exists(SELECT * FROM pg_user WHERE usename = 'ab2d_analyst') THEN
            Create Role ab2d_analyst noinherit login password 'ab2d';
        end if;

        if not exists(SELECT * FROM pg_user WHERE usename = 'cmsadmin') THEN
            --- login for cms admin
            Create Role cmsadmin noinherit login password 'ab2d';
        end if;
    end


$$
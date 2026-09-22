DO $$
BEGIN
    IF to_regclass('public.users') IS NOT NULL THEN

ALTER TABLE users
DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users
    ADD CONSTRAINT users_role_check
        CHECK (role IN ('USER', 'ADMIN', 'OWNER'));

END IF;
END $$;

ALTER TABLE organizations ADD COLUMN api_key VARCHAR(64) UNIQUE;

UPDATE organizations SET api_key = replace(gen_random_uuid()::text || gen_random_uuid()::text, '-', '') WHERE api_key IS NULL;

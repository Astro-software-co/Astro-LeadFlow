-- Remove as colunas do fluxo legado Webmotors por webhook (decisão D1: polling é o único caminho).
-- As credenciais da Webmotors vivem em webmotors_accounts (V19). marketplace_type='WEBMOTORS'
-- continua válido no CHECK (cota, marketplace_configs e origem do lead usam o enum).
ALTER TABLE marketplace_credentials
    DROP COLUMN IF EXISTS wm_client_id,
    DROP COLUMN IF EXISTS wm_client_secret,
    DROP COLUMN IF EXISTS wm_access_token,
    DROP COLUMN IF EXISTS wm_token_expires_at,
    DROP COLUMN IF EXISTS wm_integrator_email,
    DROP COLUMN IF EXISTS wm_cnpj;

## Why
O CI/CD esta pronto em standby (workflows, Dockerfile do frontend, compose de producao, README da AWS), mas nada rodou: faltam a conta AWS, a validacao dos workflows e o teste do perfil `prod`.

## What Changes
- Validar `docker build` do frontend e do backend.
- Rodar os workflows em um PR e corrigir o que falhar (inclusive o job e2e, hoje manual).
- Subir o backend com o perfil `prod` localmente para validar o fail-fast das variaveis.
- Com a conta AWS: Budget com alertas, OIDC, EC2, SSM Parameter Store, DNS e TLS; staging em modo mock.

## Impact
`.github/`, `infra/prod/`, `infra/aws/`, `backend/src/main/resources/application-prod.yml`. Orcamento de teste: US$ 100.

## Perguntas em aberto
- Dominio e DNS.
- Banco: RDS `db.t4g.micro` ou Postgres no proprio Compose com backup para o S3?
- Staging e producao em contas ou instancias separadas?

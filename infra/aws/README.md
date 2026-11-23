# Deploy na AWS (standby)

Tudo aqui esta pronto mas **nao foi executado**: depende da conta AWS. Custos abaixo sao **estimativas**; confirme na calculadora da AWS.

## Antes de tudo
1. Billing > Budgets: criar budget mensal de US$ 100 com alertas em US$ 50 e US$ 80.
2. Evitar NAT Gateway (~US$ 32/mes) e ALB (~US$ 16+/mes) no periodo de teste.

## Passo a passo
1. **OIDC GitHub**: IAM > Identity providers > `token.actions.githubusercontent.com` (audience `sts.amazonaws.com`). Criar role com trust restrito a `repo:Astro-software-co/Astro-LeadFlow:environment:staging` (e `production`) e permissao `ssm:SendCommand` / `ssm:GetCommandInvocation` na instancia.
2. **EC2**: `t4g.small`/`t4g.medium` (ARM, Amazon Linux 2023, ~US$ 12-25/mes), com instance profile `AmazonSSMManagedInstanceCore` (+ leitura do SSM Parameter Store e `ses:SendEmail`). Instalar Docker + plugin compose. Security group: 80/443 abertos, sem SSH (acesso via SSM).
3. **Arquivos na EC2**: copiar `infra/prod/*` para `/opt/leadflow` (`docker-compose.prod.yml`, `Caddyfile`) e criar `.env` a partir de `.env.example`; segredos no SSM Parameter Store (SecureString).
4. **Banco**: `docker compose --profile local-db` (mais barato; fazer backup para S3) ou RDS `db.t4g.micro` (~US$ 12-15/mes) via `SPRING_DATASOURCE_URL`.
5. **Imagens**: `build-images.yml` publica no GHCR (multi-arch). Pacote privado: `docker login ghcr.io` na EC2 com PAT `read:packages`, ou tornar os pacotes publicos.
6. **GitHub**: criar environments `staging`/`production` com variaveis `AWS_ROLE_ARN`, `AWS_REGION`, `AWS_INSTANCE_ID`. Rodar Actions > Deploy manualmente.
7. **DNS**: apontar o dominio (A record) para o Elastic IP da EC2; o Caddy emite o TLS sozinho.
8. **E-mail (SES)**: verificar dominio e sair do sandbox. **Ainda nao ha implementacao de producao**: `EmailService` so tem `MailHogEmailService`.

## Riscos conhecidos (verificados no codigo)
- `application.yaml` usa `SPRING_PROFILES_ACTIVE:dev` como default: em producao **sempre** definir `prod`.
- `DevBootstrapConfig` tem `@Profile("dev")`, entao nao roda em `prod`. Sob `dev` ele semeia usuarios com senha padrao `senha123`.
- **Nao existe `application-prod.yml`**: datasource e Redis so estao em `application-dev.yml`; em prod vem de `SPRING_DATASOURCE_*` / `SPRING_DATA_REDIS_*`. O perfil `dev` tambem exclui o `SqsAutoConfiguration`; em `prod` isso nao acontece e o boot com `APP_AWS_ENABLED=false` nao foi validado.
- Terraform nao foi criado (nao instalado no ambiente).

# Deploy na AWS — staging (passo a passo)

> Escopo: **um ambiente (staging)** na região **us-east-1**, imagens no **Amazon ECR**, deploy automático a partir da branch **`master`**, sem domínio próprio por enquanto (endereço temporário `*.sslip.io`).
> Automação: `infra/aws/` (cria a infraestrutura) · `infra/prod/` (scripts da EC2) · `.github/workflows/` (CI/CD).
> **Estado da validação:** ver a seção 9. Em resumo: a lógica foi testada localmente com stubs e o boot do perfil `prod` foi comprovado, mas **nenhuma chamada real à AWS foi executada** — espere pequenos ajustes na primeira execução e rode passo a passo.

## 1. O que será criado

```
GitHub (master) ──push──► Actions: build ──► ECR (imagens sha-xxxxxxx)
                                  └─► deploy ──► S3 (pacote infra/prod) ──► SSM RunCommand ──► EC2
EC2 (t3.medium, sem SSH) : Caddy (HTTPS) → nginx (SPA + /api) → backend (Spring, perfil prod)
                           redis · postgres (volumes Docker, backup diário → S3)
Segredos: SSM Parameter Store (/leadflow/staging/*) ──► .env gerado a cada deploy (chmod 600)
```

| Recurso | Detalhe |
|---|---|
| Orçamento | US$ 100/mês com alertas em 50%, 80% (real) e 100% (previsto) |
| ECR | `astro-leadflow-backend`, `astro-leadflow-frontend` (varredura no push; mantém as 20 últimas) |
| S3 | `leadflow-staging-<conta>`: `artifacts/` (30 dias) e `backups/` (30 dias, configurável) |
| IAM | provedor OIDC do GitHub; roles `…-gha-ecr` (push), `…-gha-deploy` (SSM), `…-instance` (EC2). **Sem chaves de acesso de longa duração** |
| EC2 | Amazon Linux 2023 x86, `t3.medium` (4 GB), disco gp3 30 GB criptografado, IMDSv2, **sem porta 22** (acesso por SSM Session Manager) |
| Rede | security group só com 80/443; Elastic IP |
| Parâmetros | ~39 chaves `SecureString`; segredos gerados aleatoriamente uma única vez |

**Custo estimado (confirme na calculadora da AWS):** EC2 t3.medium ≈ US$ 30/mês, disco ≈ US$ 2,5, IPv4 público ≈ US$ 3,6, ECR/S3 poucos dólares → **≈ US$ 40/mês** (cabe no orçamento de teste por mais de dois meses). Evite NAT Gateway e Load Balancer neste período.

## 2. Decisões tomadas (e por quê)

| Tema | Decisão | Observação |
|---|---|---|
| Região | **us-east-1** | Mais barata e com todos os serviços (SES, Lex, Bedrock). **Atenção LGPD:** os dados dos leads ficam nos EUA = transferência internacional; declarar na política de privacidade (Fase 3 do plano) e validar com o jurídico. Alternativa: `sa-east-1` (mude `AWS_REGION` em `config.env`; verifique a disponibilidade do Lex/Bedrock lá). |
| Registry | **ECR** | A EC2 puxa pela role da instância; nenhum token pessoal guardado. |
| Ambientes | **só staging** | Produção entra depois como outro environment com aprovação manual. |
| Gatilho | **push na `master`** | Fluxo: PR `develop → master` com checks obrigatórios. A `master` ainda **não existe** (seção 5). |
| Domínio | **ainda não há** | Usa `<ip>.sslip.io` (HTTPS pelo Caddy). Troca depois com `switch-domain.sh`. Meta e Abacate Pay precisarão do domínio definitivo. |
| Arquitetura | **amd64 / t3** | Evita build arm64 por QEMU (lento). Para Graviton (t4g, ~20% mais barato) seria preciso build arm64 nativo. |
| Banco | **Postgres no Compose** | Ponto único de falha; backup diário no S3 (RPO ≈ 24 h). RDS fica para produção. |

## 3. Pré-requisitos (na sua máquina)

- **AWS CLI v2**, **jq**, **git**; **GitHub CLI (`gh`)** autenticado (opcional, mas poupa trabalho).
- Uma identidade AWS com permissão de administrador **apenas para o bootstrap**.

### 3.1 Segurança da conta (faça ANTES de qualquer coisa)
1. **Usuário raiz:** ativar **MFA** e **não usar** no dia a dia (nem criar chaves de acesso para ele).
2. Criar um **usuário administrador** (IAM Identity Center é o ideal; IAM user com MFA serve) e usar **credenciais temporárias**: `aws configure sso` → `aws sso login --profile leadflow-admin`. Evite chaves de longa duração.
3. Em *Billing → Account → IAM user and role access to Billing information*: **ativar**, senão o passo do orçamento falha para usuários não raiz.
4. Conferir se a conta está sob o plano de uso esperado (free tier/créditos) e que o e-mail de cobrança é lido por alguém.

## 4. Criar a infraestrutura

```bash
git fetch origin && git checkout claude/marketplace-integrations-ynztxc   # contém infra/aws, infra/prod e os workflows
cp infra/aws/config.env.example infra/aws/config.env
$EDITOR infra/aws/config.env     # GH_REPO, BUDGET_EMAIL, AWS_PROFILE (se usar SSO)

aws sts get-caller-identity --profile leadflow-admin      # confirme que é a conta certa
./infra/aws/bootstrap.sh --dry-run                        # NADA é alterado; revise o que seria criado
./infra/aws/bootstrap.sh                                  # pede confirmação ("sim")
```

- Idempotente: pode rodar de novo; só cria o que falta. Para um passo isolado: `./infra/aws/bootstrap.sh --only 5`.
- Ordem recomendada na primeira vez (para ver cada resultado): `--only 1`, `--only 2,3`, `--only 4`, `--only 5`, `--only 6,7`.
- O passo 5 leva ~3–6 min (espera o agente SSM e a instalação do Docker). No final imprime o **IP** e o **domínio temporário** `<ip>.sslip.io`.
- Estado salvo em `infra/aws/.state/staging.env` (ignorado pelo git).

### 4.1 O que conferir ao terminar
- Console AWS: *Budgets* (1 orçamento), *ECR* (2 repositórios), *S3* (1 bucket), *IAM → Roles* (3 roles `leadflow-staging-*`), *EC2* (1 instância `running`, **sem key pair**), *Systems Manager → Fleet Manager* (instância **Online**), *Parameter Store* (`/leadflow/staging/*`).
- Acesso à instância sem SSH: `aws ssm start-session --target <instance-id>`.

## 5. GitHub: branch `master`, proteção e primeiro deploy

1. **Criar a `master`** a partir da `develop` (ou reaproveitar a `main`, hoje desatualizada, para não ter duas branches parecidas):
   ```bash
   git fetch origin && git push origin origin/develop:refs/heads/master
   ```
2. **Proteger a `master`** (Settings → Branches): exigir PR, exigir os checks `CI` (backend, frontend) e `Security`, bloquear force-push.
3. **Environment `staging`** (o passo 7 já o cria e preenche as variáveis; senão, manualmente): em *Deployment branches* restringir a **`master`**.
   Variáveis do **repositório**: `AWS_REGION`, `AWS_ECR_ROLE_ARN`. Variáveis do **environment `staging`**: `AWS_DEPLOY_ROLE_ARN`, `AWS_INSTANCE_ID`, `ARTIFACT_BUCKET`, `STAGING_URL`.
4. **Os workflows precisam estar na `master`** para disparar: faça o PR da branch com `infra/` e `.github/` para a `develop` e depois `develop → master`.
5. **Primeiro deploy:** o merge na `master` dispara `Deploy` (build → deploy → smoke). Acompanhe em *Actions*. Para reimplantar/rollback manual: *Actions → Deploy → Run workflow* com `image_tag` (ex.: `sha-abc1234`).

> Os workflows usam o OIDC: a confiança das roles está presa ao repositório `GH_REPO` e às branches `master`/`develop` (push no ECR) e ao environment `staging` (deploy). Se o nome do repositório mudar, rode o passo 4 de novo.

## 6. Verificação pós-deploy

```bash
curl -I https://<dominio>/                      # 200 (SPA)
curl -s https://<dominio>/api/v1/plans | head   # JSON dos planos (endpoint público)
aws ssm start-session --target <instance-id>    # dentro da instância:
  cd /opt/leadflow && docker compose -f docker-compose.prod.yml ps
  docker compose -f docker-compose.prod.yml logs --tail 100 backend
```
- O `/actuator/health` **não** é exposto pelo Caddy/nginx (só `/api/`); a saúde é verificada dentro da instância (healthcheck do Compose) e pelo smoke test.
- Primeiro acesso: o sistema não tem cadastro público; crie o primeiro usuário pelo fluxo de onboarding/admin (ver `docs/GETTING-STARTED.md`). `DevBootstrapConfig` (usuários de desenvolvimento) **não** roda em `prod`.
- Certificado: o Caddy emite o TLS sozinho (Let's Encrypt, com ZeroSSL de reserva). O `sslip.io` é um domínio compartilhado e pode sofrer limite de emissão; se falhar, tente de novo depois ou use um domínio próprio.

## 7. Operação

| Tarefa | Como |
|---|---|
| **Rollback** | Automático se a nova versão não ficar saudável em 5 min (volta para `.last_good_tag`). Manual: *Actions → Deploy → image_tag = tag antiga*. **Migrations Flyway não são revertidas:** mantenha-as compatíveis com a versão anterior (adicionar antes de remover). |
| **Trocar um segredo** | `./infra/aws/set-param.sh ABACATEPAY_API_KEY` (pede o valor sem eco) e rode o Deploy para regenerar o `.env`. |
| **Ligar integrações reais** | Mesma coisa via `set-param.sh` (`META_API_MODE=real`, tokens, etc.). Veja `infra/prod/.env.example` para a lista completa. |
| **Backup** | Timer diário 03:00 (Brasília) → `s3://<bucket>/backups/postgres/`. **Teste a restauração** (`gunzip -c arquivo.sql.gz \| docker compose exec -T postgres psql -U leadflow -d leadflow`) antes de confiar nele. Os backups contêm dados pessoais: a retenção (30 dias) deve constar da política de privacidade. |
| **Domínio** | Registre o domínio, crie um **registro A** para o IP elástico (`state: PUBLIC_IP`), rode `./infra/aws/switch-domain.sh app.seudominio.com.br`, atualize `STAGING_URL` no environment e faça deploy. |
| **Desligar para economizar** | `CONFIRM=sim ./infra/aws/teardown.sh` (remove EC2/IP/SG; **o banco vive no disco da EC2** — faça backup antes). |

## 8. Achados do teste de boot local do perfil `prod` (2026-11-23)

Subi o jar com `SPRING_PROFILES_ACTIVE=prod`, Postgres 16 e Redis reais e as variáveis do `.env.example`:

| # | Achado | Tratamento |
|---|---|---|
| 1 | **Perfil `prod` sem nenhum appender de log** no `logback-spring.xml` (o root logger só era configurado em `!prod`): erros de boot do Spring ficavam **mudos** e o `docker logs` quase não mostraria nada. | **Corrigido** neste commit (bloco `prod` com console + appender de erros). |
| 2 | O filtro de webhook do Abacate Pay **exige `ABACATEPAY_WEBHOOK_SECRET`** em produção (falha de boot se vazio). | O modelo de parâmetros gera um valor placeholder. Troque pelo real quando houver conta. |
| 3 | Com `APP_AWS_ENABLED=false`, o `SqsAutoConfiguration` **não é excluído** e o boot falha **sem região AWS**. | `AWS_REGION` está no modelo de parâmetros. Pendente no código: excluir a autoconfiguração quando `app.aws.enabled=false` (change `aws-deploy-activation`). |
| 4 | `/actuator/health` geral fica **DOWN** sem SMTP (`MailHealthIndicator`); o `readiness` (usado pelo Compose) fica UP. | `MANAGEMENT_HEALTH_MAIL_ENABLED=false` no modelo até haver SES. |
| 5 | Flyway avisa que o PostgreSQL 16 é mais novo que o suportado (15). Todas as migrations aplicaram sem erro. | Só aviso. Se incomodar: fixar `postgres:15-alpine` no compose ou atualizar o Flyway. |

Resultado: `readiness UP` em ~12 s, `liveness UP`, `/api/v1/plans` 200, `/actuator/info` e `/actuator/metrics` retornam 403 sem autenticação.

## 9. Estado da validação (seja cético com o que não foi provado)

| Item | Estado |
|---|---|
| Boot do backend em `prod` (jar + Postgres + Redis reais) | **Provado** localmente |
| Migrations V1…V19 em Postgres 16 | **Provado** |
| `deploy.sh` (sucesso, rollback para a tag anterior, primeiro deploy sem rollback, validação de tag) e `render-env.sh` (`.env` 600, escape de `$`) | **Provado com stubs** de `docker`/`aws` |
| `bootstrap.sh --dry-run` (0 chamadas que alteram a conta) e `06-params.sh` (39 parâmetros, idempotente, segredos e `SAME` corretos) | **Provado com stub** de `aws` |
| Sintaxe YAML dos workflows, JSON das políticas IAM, `bash -n` de todos os scripts | **Provado** |
| **Chamadas reais à AWS** (permissões IAM, OIDC, EC2/SSM, ECR, S3, Budgets) | **NÃO executado** — sem credenciais aqui |
| `docker build` das imagens e `docker compose up` na EC2 | **NÃO executado** (sem daemon Docker aqui) |
| Emissão de TLS pelo Caddy com `sslip.io`; SSM RunCommand ponta a ponta; push no ECR pelo Actions | **NÃO executado** |
| Smoke test do pipeline completo | **NÃO executado** |

## 10. Próximos passos depois do primeiro deploy

1. Domínio definitivo (`switch-domain.sh`) — também destrava o formulário da Webmotors e os webhooks da Meta/Abacate Pay.
2. SES (change `email-ses-prod`): verificar domínio, sair do sandbox, implementar `SesEmailService`; depois remover `MANAGEMENT_HEALTH_MAIL_ENABLED=false`.
3. Alarmes de CloudWatch (status check da instância, CPU, disco) e teste de restauração do backup.
4. Produção: segundo environment com **revisores obrigatórios**, banco em RDS e a mesma imagem promovida do staging.
5. Excluir o `SqsAutoConfiguration` quando `app.aws.enabled=false` (achado 3) e fixar a versão do Postgres/Flyway (achado 5).
6. Revisar a política de privacidade: região (EUA), retenção de backups (30 dias) e logs.

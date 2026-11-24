# Infraestrutura AWS (staging)

Automação que cria tudo o que o deploy precisa. **Passo a passo por script:** [`docs/DEPLOY-AWS-STAGING.md`](../../docs/DEPLOY-AWS-STAGING.md). **Pelo console (sem scripts):** [`docs/DEPLOY-AWS-CONSOLE.md`](../../docs/DEPLOY-AWS-CONSOLE.md).
Para o caminho manual, `make-env-file.sh` gera (sem acessar a AWS) o bloco de configuração a colar no SSM como `/leadflow/staging/ENV_FILE`.

```bash
./infra/aws/preflight.sh                                # só leitura: mostra quem você é e descobre travas da conta/organização
cp infra/aws/config.env.example infra/aws/config.env   # ajuste GH_REPO, BUDGET_EMAIL etc.
./infra/aws/bootstrap.sh --dry-run                      # mostra o que seria criado, sem alterar nada
./infra/aws/bootstrap.sh                                # cria (idempotente; pode rodar de novo)
```

| Passo | Script | Cria |
|---|---|---|
| 1 | `steps/01-budget.sh` | Orçamento mensal com alertas (50% / 80% / 100% previsto) |
| 2 | `steps/02-ecr.sh` | Repositórios ECR `astro-leadflow-backend` e `-frontend` |
| 3 | `steps/03-bucket.sh` | Bucket S3 (pacote de deploy e backups do Postgres, com expiração) |
| 4 | `steps/04-iam.sh` | OIDC do GitHub + roles (push no ECR, deploy via SSM) + role da EC2 |
| 5 | `steps/05-ec2.sh` | Security group (80/443), EC2 Amazon Linux 2023 sem SSH (acesso por SSM), Elastic IP |
| 6 | `steps/06-params.sh` | Parâmetros do SSM (segredos gerados uma única vez) a partir de `infra/prod/staging.params.template` |
| 7 | `steps/07-github.sh` | Environment `staging` e variáveis dos workflows (requer `gh`) |

Utilitários: `set-param.sh CHAVE` (altera um parâmetro sem expor o valor), `switch-domain.sh <dominio>`, `teardown.sh` (remove EC2/IP/SG).
Os modelos de política IAM ficam em `policies/` (placeholders `__ACCOUNT_ID__`, `__GH_REPO__` etc.).
Os scripts que rodam dentro da EC2 estão em `infra/prod/` (`bootstrap-instance.sh`, `render-env.sh`, `deploy.sh`, `backup-postgres.sh`).

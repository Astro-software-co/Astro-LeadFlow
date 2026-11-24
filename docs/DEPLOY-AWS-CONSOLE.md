# Deploy na AWS pelo console (passo a passo, sem scripts)

> Equivalente manual de `infra/aws/` (que cria a mesma coisa por script). Use este roteiro quando preferir **clicar pela interface** ou quando não puder rodar scripts.
> Região: **us-east-1 (N. Virgínia)** em todas as telas. Os nomes de menu abaixo seguem o console em português; podem variar um pouco, então use a busca do console (`Alt+S`) pelo nome do serviço.
> Ambiente: **staging**. Deploy automático a partir da branch **`master`** pelo GitHub Actions.

**Tempo estimado:** 1h30 a 2h no total. **Custo:** ≈ US$ 40/mês (estimativa; confirme na calculadora da AWS).

---

## Parte 0 — O que adiantar ENQUANTO a conta está em validação (não precisa de AWS)

Tudo abaixo você consegue fazer hoje e deixa o dia do console muito mais curto.

### 0.1 No GitHub (pelo navegador)
1. **Leve os arquivos de infraestrutura para a `develop`:** abra um *Pull request* da branch `claude/marketplace-integrations-ynztxc` para `develop`, espere o CI ficar verde e faça o merge.
2. **Crie a branch `master`:** *Code → o seletor de branch → digite `master` → "Create branch: master from develop"*.
3. **Proteja a `master`:** *Settings → Branches → Add branch ruleset* (ou *Add rule*) para `master`: exigir *pull request*, exigir os checks **CI** e **Security**, bloquear *force push*.
4. **Crie o environment `staging`:** *Settings → Environments → New environment → `staging`* → em *Deployment branches and tags* escolha **Selected branches** e adicione `master`. As variáveis dele ficam para a Parte 6 (precisam dos dados da AWS).

### 0.2 Preparar valores (anote num bloco de notas)
| O que | Valor |
|---|---|
| E-mail dos alertas de custo | o seu |
| Nome do repositório | `Astro-software-co/Astro-LeadFlow` (maiúsculas exatas) |
| Região | `us-east-1` |
| Domínio | ainda não há → usaremos `<ip-com-hifens>.sslip.io` |

### 0.3 (Opcional) Registrar o domínio
Se já quiser um domínio próprio (ex.: no Registro.br), registre agora; o apontamento DNS só poderá ser feito depois que existir o IP elástico (Parte 5). Ele também destrava o formulário da Webmotors.

### 0.4 Quando o console voltar: segurança antes de tudo
1. **MFA** no usuário raiz (*conta → Credenciais de segurança → Autenticação multifator*). Não use o usuário raiz no dia a dia.
2. *Billing and Cost Management → Account → "IAM user and role access to Billing information"* → **Ativar**.

---

## Parte 1 — Orçamento (faça primeiro)

*Billing and Cost Management → Budgets → Create budget*
1. Template: **Customize (advanced)** → tipo **Cost budget**.
2. Nome: `leadflow-staging-monthly`. Período: **Monthly**, **Recurring budget**. Valor: **US$ 100**.
3. Alertas (*Add an alert threshold*), todos por e-mail para o seu endereço:
   - 50% do valor **Actual**
   - 80% do valor **Actual**
   - 100% do valor **Forecasted**
4. *Create budget*. ✔️ Chega um e-mail de confirmação.

## Parte 2 — Repositórios de imagens (ECR)

*Elastic Container Registry → Private registry → Repositories → Create repository* (faça duas vezes):
- Nomes: `astro-leadflow-backend` e `astro-leadflow-frontend`.
- *Tag immutability*: **Disabled**. *Scan on push*: **Enabled**. *Encryption*: **AES-256**.

Em **cada** repositório: *Lifecycle policies → Create rule*: prioridade **1**, *Image status* **Any**, *Match criteria* **Image count more than 20**, ação **Expire**.

✔️ Anote o *URI do registry*: `<ID_DA_CONTA>.dkr.ecr.us-east-1.amazonaws.com`.

## Parte 3 — Bucket S3 (pacote de deploy e backups)

*S3 → Create bucket*
- Nome: `leadflow-staging-<ID_DA_CONTA>` (precisa ser único no mundo; o ID da conta está no menu superior direito).
- Região: **us-east-1**. *Block all public access*: **marcado** (padrão). *Default encryption*: **SSE-S3**.
- Depois de criar: *Management → Lifecycle rules → Create lifecycle rule* (duas regras):
  - `expira-backups`: *Limit the scope* → prefixo `backups/` → *Expire current versions of objects* após **30 dias**.
  - `expira-artifacts`: prefixo `artifacts/` → expirar após **30 dias**.
- Adicione as *tags* `Project=leadflow`, `Environment=staging`.

> Os backups contêm dados pessoais; o prazo de 30 dias deve constar da política de privacidade.

## Parte 4 — Permissões (IAM)

### 4.1 Provedor do GitHub (OIDC)
*IAM → Identity providers → Add provider*: tipo **OpenID Connect**; *Provider URL* `https://token.actions.githubusercontent.com`; *Audience* `sts.amazonaws.com` → *Add provider*.

### 4.2 Role que publica imagens — `leadflow-staging-gha-ecr`
*IAM → Roles → Create role → Custom trust policy*, cole:
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Federated": "arn:aws:iam::<ID_DA_CONTA>:oidc-provider/token.actions.githubusercontent.com" },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": { "token.actions.githubusercontent.com:aud": "sts.amazonaws.com" },
      "StringLike": { "token.actions.githubusercontent.com:sub": [
        "repo:Astro-software-co/Astro-LeadFlow:ref:refs/heads/master",
        "repo:Astro-software-co/Astro-LeadFlow:ref:refs/heads/develop"
      ] }
    }
  }]
}
```
*Next* (sem anexar políticas gerenciadas) → nome `leadflow-staging-gha-ecr` → *Create*. Abra a role → *Add permissions → Create inline policy → JSON*, cole e nomeie `inline`:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    { "Sid": "Auth", "Effect": "Allow", "Action": "ecr:GetAuthorizationToken", "Resource": "*" },
    { "Sid": "Push", "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability", "ecr:InitiateLayerUpload", "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload", "ecr:PutImage", "ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer",
        "ecr:DescribeImages", "ecr:DescribeRepositories"
      ],
      "Resource": [
        "arn:aws:ecr:us-east-1:<ID_DA_CONTA>:repository/astro-leadflow-backend",
        "arn:aws:ecr:us-east-1:<ID_DA_CONTA>:repository/astro-leadflow-frontend"
      ] }
  ]
}
```

### 4.3 Role que faz o deploy — `leadflow-staging-gha-deploy`
*Create role → Custom trust policy*:
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Federated": "arn:aws:iam::<ID_DA_CONTA>:oidc-provider/token.actions.githubusercontent.com" },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": {
        "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
        "token.actions.githubusercontent.com:sub": "repo:Astro-software-co/Astro-LeadFlow:environment:staging"
      }
    }
  }]
}
```
Nome `leadflow-staging-gha-deploy`. Depois, política inline `inline`:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    { "Sid": "RunOnTaggedInstance", "Effect": "Allow", "Action": "ssm:SendCommand",
      "Resource": "arn:aws:ec2:us-east-1:<ID_DA_CONTA>:instance/*",
      "Condition": { "StringEquals": {
        "ssm:resourceTag/Project": "leadflow", "ssm:resourceTag/Environment": "staging" } } },
    { "Sid": "RunDocument", "Effect": "Allow", "Action": "ssm:SendCommand",
      "Resource": "arn:aws:ssm:us-east-1::document/AWS-RunShellScript" },
    { "Sid": "ReadResult", "Effect": "Allow",
      "Action": [ "ssm:GetCommandInvocation", "ssm:ListCommandInvocations" ], "Resource": "*" },
    { "Sid": "UploadBundle", "Effect": "Allow",
      "Action": [ "s3:PutObject", "s3:DeleteObject" ],
      "Resource": "arn:aws:s3:::leadflow-staging-<ID_DA_CONTA>/artifacts/*" },
    { "Sid": "ListBundle", "Effect": "Allow", "Action": "s3:ListBucket",
      "Resource": "arn:aws:s3:::leadflow-staging-<ID_DA_CONTA>" }
  ]
}
```

### 4.4 Role da instância — `leadflow-staging-instance`
*Create role → AWS service → EC2* (caso de uso **EC2**). Anexe a política gerenciada **`AmazonSSMManagedInstanceCore`**. Nome `leadflow-staging-instance`. Depois, política inline `inline`:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    { "Sid": "EcrAuth", "Effect": "Allow", "Action": "ecr:GetAuthorizationToken", "Resource": "*" },
    { "Sid": "EcrPull", "Effect": "Allow",
      "Action": [ "ecr:BatchCheckLayerAvailability", "ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer" ],
      "Resource": [
        "arn:aws:ecr:us-east-1:<ID_DA_CONTA>:repository/astro-leadflow-backend",
        "arn:aws:ecr:us-east-1:<ID_DA_CONTA>:repository/astro-leadflow-frontend"
      ] },
    { "Sid": "ReadParams", "Effect": "Allow",
      "Action": [ "ssm:GetParametersByPath", "ssm:GetParameters", "ssm:GetParameter" ],
      "Resource": "arn:aws:ssm:us-east-1:<ID_DA_CONTA>:parameter/leadflow/staging*" },
    { "Sid": "DecryptParams", "Effect": "Allow", "Action": "kms:Decrypt", "Resource": "*",
      "Condition": { "StringEquals": { "kms:ViaService": "ssm.us-east-1.amazonaws.com" } } },
    { "Sid": "ReadBundle", "Effect": "Allow", "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::leadflow-staging-<ID_DA_CONTA>/artifacts/*" },
    { "Sid": "Backups", "Effect": "Allow", "Action": [ "s3:PutObject", "s3:GetObject" ],
      "Resource": "arn:aws:s3:::leadflow-staging-<ID_DA_CONTA>/backups/*" },
    { "Sid": "ListBucket", "Effect": "Allow", "Action": "s3:ListBucket",
      "Resource": "arn:aws:s3:::leadflow-staging-<ID_DA_CONTA>" }
  ]
}
```
(O console cria automaticamente um *instance profile* com o mesmo nome.)

✔️ Anote os **ARNs** das duas roles do GitHub (`arn:aws:iam::<ID_DA_CONTA>:role/leadflow-staging-gha-ecr` e `…-gha-deploy`).

> Se o console recusar a criação do provedor ou das roles com `explicit deny in a service control policy`, a organização bloqueou isso: avise o administrador da organização e me envie a mensagem.

## Parte 5 — Servidor (EC2)

*EC2 → Launch instance*
| Campo | Valor |
|---|---|
| Nome | `leadflow-staging` |
| Tags | `Project=leadflow`, `Environment=staging` — em *Resource types* marque **Instances** e **Volumes** (o deploy usa essas tags) |
| AMI | **Amazon Linux 2023** (x86_64) |
| Tipo | **t3.medium** (4 GB) |
| Key pair | **Proceed without a key pair** (acesso será pelo Session Manager, sem SSH) |
| Rede | VPC padrão, *Auto-assign public IP*: **Enable** |
| Security group | **Create**: nome `leadflow-staging`; regras de entrada **HTTP (80)** e **HTTPS (443)** de `0.0.0.0/0`; **remova** a regra de SSH |
| Armazenamento | **30 GiB**, **gp3**, *Encrypted* |
| *Advanced details → IAM instance profile* | `leadflow-staging-instance` |
| *Metadata version* | **V2 only (token required)** |
| *Metadata response hop limit* | **2** (senão os containers não alcançam as credenciais da instância) |
| *User data* | cole o **conteúdo inteiro** de [`infra/prod/bootstrap-instance.sh`](../infra/prod/bootstrap-instance.sh) (abra no GitHub → *Raw* → copiar) |

*Launch instance*. Anote o **Instance ID** (`i-…`).

### 5.1 IP fixo (Elastic IP)
*EC2 → Elastic IPs → Allocate Elastic IP address* (tags `Project=leadflow`, `Environment=staging`) → *Actions → Associate Elastic IP address* → escolha a instância. Anote o **IP** (ex.: `3.91.10.20`). O domínio temporário é o IP com hifens: `3-91-10-20.sslip.io`.

### 5.2 Conferir que o servidor está pronto (3 a 6 minutos depois)
- *Systems Manager → Fleet Manager*: a instância deve aparecer **Online**.
- *EC2 → selecione a instância → Connect → Session Manager → Connect* e rode:
  ```bash
  test -f /opt/leadflow/.bootstrapped && docker --version && docker compose version && echo PRONTO
  ```

## Parte 6 — Configuração e segredos (Parameter Store)

### 6.1 Gerar o bloco de configuração (no seu computador, sem AWS)
No **WSL** (ou qualquer bash), dentro do repositório:
```bash
./infra/aws/make-env-file.sh --account <ID_DA_CONTA> --ip <IP_ELASTICO> > ~/leadflow-staging.generated
```
Ele gera ~39 linhas `CHAVE=valor` com os segredos aleatórios. **Não commite** esse arquivo.

### 6.2 Colar no SSM
*Systems Manager → Parameter Store → Create parameter*
- Nome: `/leadflow/staging/ENV_FILE`. *Tier*: **Standard** (o bloco tem ~1,5 KB; o limite é 4 KB). *Type*: **SecureString**, chave KMS padrão (`alias/aws/ssm`).
- *Value*: cole o conteúdo do arquivo gerado.
- Depois **apague o arquivo local**: `shred -u ~/leadflow-staging.generated`.

Para trocar um segredo depois (ex.: chave real do Abacate Pay): crie outro parâmetro `/leadflow/staging/ABACATEPAY_API_KEY` (SecureString). Parâmetros individuais **têm precedência** sobre o `ENV_FILE`. Em seguida faça um novo deploy.

## Parte 7 — Variáveis no GitHub

*Settings → Secrets and variables → Actions → Variables*
- **Do repositório** (*Repository variables*):
  | Nome | Valor |
  |---|---|
  | `AWS_REGION` | `us-east-1` |
  | `AWS_ECR_ROLE_ARN` | ARN da role `leadflow-staging-gha-ecr` |
- **Do environment `staging`** (*Settings → Environments → staging → Environment variables*):
  | Nome | Valor |
  |---|---|
  | `AWS_DEPLOY_ROLE_ARN` | ARN da role `leadflow-staging-gha-deploy` |
  | `AWS_INSTANCE_ID` | `i-…` da instância |
  | `ARTIFACT_BUCKET` | `leadflow-staging-<ID_DA_CONTA>` |
  | `STAGING_URL` | `https://3-91-10-20.sslip.io` (com o seu IP) |

## Parte 8 — Primeiro deploy

1. Faça o *Pull request* `develop → master` e o merge (se ainda não fez).
2. *Actions → Deploy*: acompanhe `build` (imagens no ECR) → `deploy` (SSM) → *Smoke test*. A primeira execução leva ~15 a 25 min (build do backend).
3. Verifique no navegador `https://<ip-com-hifens>.sslip.io/` (a tela do sistema) e `https://<ip-com-hifens>.sslip.io/api/v1/plans` (JSON dos planos).

## Parte 9 — Conferência final

| Onde | O que deve existir |
|---|---|
| Budgets | `leadflow-staging-monthly` com 3 alertas |
| ECR | 2 repositórios; após o deploy, imagens `sha-xxxxxxx` |
| S3 | `leadflow-staging-<ID>` com 2 regras de ciclo de vida |
| IAM | provedor OIDC + 3 roles `leadflow-staging-*` |
| EC2 | 1 instância `running`, sem key pair, com IP elástico e tags |
| Systems Manager | instância **Online**; parâmetro `/leadflow/staging/ENV_FILE` |
| GitHub | `master` protegida; environment `staging`; 6 variáveis |

## Operação rápida

| Tarefa | Como |
|---|---|
| Ver logs | EC2 → Connect → Session Manager: `cd /opt/leadflow && docker compose -f docker-compose.prod.yml logs --tail 100 backend` |
| Rollback | *Actions → Deploy → Run workflow* com `image_tag` de uma tag anterior (ex.: `sha-abc1234`). Falha de saúde na nova versão já volta sozinha para a anterior. |
| Trocar segredo | Novo parâmetro individual no SSM (Parte 6.2) e novo deploy |
| Trocar domínio | Registro **A** no DNS para o IP elástico; edite no `ENV_FILE` `DOMAIN`, `APP_BASE_URL`, `PUBLIC_API_URL`, `APP_CORS_ALLOWED_ORIGINS`, `ML_REDIRECT_URI`; atualize `STAGING_URL`; deploy |
| Desligar para economizar | Encerrar a instância e liberar o IP elástico (**o banco vive no disco da EC2**: faça backup antes) |

## Se algo falhar

| Sintoma | O que fazer |
|---|---|
| `explicit deny in a service control policy` | A organização bloqueou essa ação; anote qual e peça ao administrador da organização |
| Instância não aparece **Online** no Fleet Manager | Confira se o *instance profile* `leadflow-staging-instance` foi escolhido no lançamento e aguarde 5 min |
| Workflow falha em *Configure AWS credentials* | Confira as maiúsculas de `Astro-software-co/Astro-LeadFlow` no *trust* da role e o ARN na variável |
| Deploy falha com status `Failed` | O log do job mostra os últimos logs do backend; me envie |
| Certificado inválido no navegador | O `sslip.io` pode ter limite de emissão; espere uns minutos e recarregue |

> **Estado de validação:** este roteiro foi escrito a partir dos scripts testados localmente com stubs; **nenhuma tela do console foi exercitada por mim**. Se algum rótulo for diferente do que você vê, siga o nome do campo e me avise.

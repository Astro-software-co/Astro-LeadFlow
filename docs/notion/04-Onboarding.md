# Onboarding

## Pré-requisitos

| Ferramenta | Versão mínima |
|---|---|
| Java (JDK) | 21 |
| Node.js | 20+ |
| npm | 10+ |
| Docker + Docker Compose | Qualquer versão recente |
| PostgreSQL | 15+ (ou via Docker) |
| Redis | 7+ (ou via Docker) |

---

## Setup no Primeiro Dia

### 1. Clonar e instalar dependências

```bash
git clone git@github.com:Astro-software-co/Astro-LeadFlow.git
cd Astro-LeadFlow
cd frontend && npm install
```

### 2. Subir a infraestrutura local

```bash
cd infra
docker compose up -d
```

### 3. Iniciar backend e frontend

```bash
# Terminal 1
cd backend && ./gradlew bootRun

# Terminal 2
cd frontend && npm run dev
```

### 4. Verificar tudo funcionando

```bash
# Frontend
cd frontend
npm run lint
npm run test:coverage
npm run build

# Backend
cd backend
./gradlew test
```

### 5. Abrir o app

- Landing page: `http://localhost:3000`
- App autenticado: `http://localhost:3000/app/login`
- Swagger: `http://localhost:8080/swagger-ui.html`

---

## Leituras Obrigatórias

Antes de tocar no código:

1. **[Architecture](./02-Architecture.md)** — entenda a separação landing/app, a cadeia de segurança do backend e a camada de serviços do frontend.
2. **[API Reference](./05-API-Reference.md)** — os contratos do backend são a fonte de verdade.
3. **[Roadmap e Próximos Passos](./06-Roadmap.md)** — entenda o que está planejado antes de propor mudanças.
4. **`CLAUDE.md`** na raiz do repositório — regras técnicas e de produto que o time já decidiu.

---

## Regras de Alto Impacto

### Multi-tenancy

- O `organizationId` vem do JWT — nunca assuma um ID fixo.
- Toda query key do React Query deve começar com `[orgId, ...]`.
- O `logout` deve chamar `queryClient.clear()` antes de limpar o contexto de auth.

### Module Boundaries (frontend)

- Páginas de rota (`/app/*`) importam domínio somente via `@/modules/*`.
- Nunca importe diretamente de `services/` ou `hooks/api/` em páginas de rota.
- O teste `src/test/module-boundaries.test.ts` quebra o CI se a regra for violada.

### Namespace do Backend

- Pacote canônico: `com.astro.leadflow.shared.modules.*`.
- Nunca criar código em `com.astro.leadflow.modules.*`.
- O `ArchitectureTest` valida isso automaticamente.

### Segurança de Token

- `accessToken` fica apenas em memória no AuthContext.
- `refreshToken` fica no cookie `httpOnly` — o backend faz o set.
- Nunca escrever token em `localStorage` ou `sessionStorage`.

### Responsividade

- Toda tela é testada em 375px / 768px / 1024px / 1440px.
- Mobile-first: CSS base = mobile, adicionar breakpoints `sm:` / `md:` / `lg:` progressivamente.

---

## Tarefas de Rampa Sugeridas

1. Adicionar um teste de regressão a um domínio que você vai tocar.
2. Validar um runbook de ponta a ponta no ambiente local.
3. Revisar o roadmap ativo em `.planning/STATE.md` e `.planning/ROADMAP.md`.
4. Abrir o Swagger e explorar os contratos de um módulo do interesse.

---

## Variáveis de Ambiente

### Frontend

```env
# .env.development
VITE_API_URL=/api/v1
VITE_USE_MOCKS=true          # true em dev; false em e2e e produção
VITE_APP_NAME=Astro LeadFlow
```

### Backend (application-dev.yml)

```
spring.datasource.url=jdbc:postgresql://localhost:5432/leadflow
spring.datasource.username=leadflow
spring.datasource.password=leadflow123
spring.data.redis.host=localhost
spring.data.redis.port=6379
app.meta.webhook-secret=     # obrigatório em produção; opcional em dev
```

---

## Páginas Relacionadas

- [Overview](./01-Overview.md)
- [Architecture](./02-Architecture.md)
- [Runbooks](./03-Runbooks.md)

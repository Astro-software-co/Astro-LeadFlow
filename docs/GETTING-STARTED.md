# Getting Started

## Requisitos

- Java 21
- Node.js 20+
- npm
- Docker e Docker Compose

## Subir infraestrutura local

Na raiz do backend:

```bash
cd backend
docker compose up -d
```

Isso sobe:

- PostgreSQL
- Redis
- MailHog
- LocalStack

## Subir backend

```bash
cd backend
./gradlew bootRun
```

URLs uteis:

- API: `http://localhost:8080/api/v1`
- Swagger: `http://localhost:8080/swagger-ui.html`

## Subir frontend

```bash
cd frontend
npm install
npm run dev
```

URL local:

- App: `http://localhost:3000`

## Credencial dev garantida

Com o profile `dev`, o backend garante um usuario administrativo idempotente:

- email: `admin@astro.dev`
- senha: `senha123`

Esse bootstrap tambem garante uma organizacao demo e uma assinatura basica se estiverem ausentes.

## Primeiro teste recomendado

1. Abrir `http://localhost:3000/app/login`
2. Entrar com `admin@astro.dev / senha123`
3. Confirmar acesso ao dashboard
4. Recarregar a pagina
5. Confirmar que a sessao continua valida

## Se o login falhar

Checklist rapido:

1. confirmar backend ativo em `:8080`
2. confirmar Postgres e Redis ativos
3. confirmar `frontend/.env.development` com `VITE_API_URL=/api/v1`
4. confirmar `frontend/.env.development` com `VITE_USE_MOCKS=false`
5. verificar no log do backend se o bootstrap dev foi executado

## Se a sessao nao persistir apos refresh

Checklist:

1. inspecionar no navegador se o cookie `astro_refresh_token` foi setado
2. confirmar que as chamadas usam `withCredentials`
3. confirmar que o frontend esta usando `/api/v1` via proxy do Vite
4. confirmar que nao houve mistura entre `localhost` e `127.0.0.1`
5. validar `POST /api/v1/auth/refresh` no Network

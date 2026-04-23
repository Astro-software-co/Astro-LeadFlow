# Testing

## Frontend

Unitarios:

```bash
cd frontend
npm run test
```

Lint:

```bash
cd frontend
npm run lint
```

E2E:

```bash
cd frontend
npm run test:e2e
```

## Backend

```bash
cd backend
./gradlew test
```

## Suite focada de auth

Frontend:

```bash
cd frontend
npm run test -- auth.service AuthContext api users.service useTeam
```

Backend:

```bash
cd backend
./gradlew test --tests 'com.astro.leadflow.shared.modules.iam.api.AuthControllerTest' --tests 'com.astro.leadflow.shared.modules.iam.service.AuthServiceTest'
```

## Cenarios manuais obrigatorios

### Login real

1. fazer login com `admin@astro.dev / senha123`
2. confirmar `GET /users/me` com sucesso

### Persistencia apos refresh

1. entrar no dashboard
2. recarregar a pagina
3. confirmar permanencia autenticada

### Expiracao por inatividade

1. autenticar
2. ficar sem interacoes por mais de 15 minutos
3. disparar uma chamada autenticada
4. confirmar retorno para login

### Keepalive em uso ativo

1. autenticar
2. manter interacoes ocasionais na tela por mais de 15 minutos
3. confirmar que a sessao continua valida

### Logout

1. autenticar
2. executar logout
3. confirmar ida para `/app/login`
4. confirmar que `POST /auth/refresh` falha para o cookie antigo

### Filtro assignedTo

1. abrir `/app/leads`
2. confirmar que o seletor mostra usuarios assignable da organizacao
3. mudar filtros e paginacao
4. confirmar que o diretorio de responsaveis nao muda junto com o dataset

## Observacoes de infraestrutura para testes

- frontend E2E espera backend acessivel
- Playwright funciona melhor com frontend em `127.0.0.1:3000` e backend em `127.0.0.1:8080`
- Redis precisa estar ativo para validar fluxo real de sessao

# Deployment

## Dependencias obrigatorias

- PostgreSQL
- Redis
- backend Spring Boot
- frontend estatico ou servidor web para a SPA

Opcional conforme uso:

- Mail provider real
- AWS S3/SQS/SNS
- integracoes Meta, Webmotors e Mercado Livre

## Requisitos para producao

- HTTPS na borda
- cookie de refresh com `Secure=true`
- CORS restrito aos dominos reais
- Redis dedicado para:
  - rate limit
  - blocklist de refresh tokens
  - sessao deslizante autenticada
- Postgres com backup e monitoramento

## Variaveis que precisam ser revisitadas antes de publicar

- `JWT_SECRET`
- `AUTH_REFRESH_COOKIE_SECURE`
- `AUTH_REFRESH_COOKIE_SAME_SITE`
- `AUTH_SESSION_IDLE_TIMEOUT_MS`
- `APP_BASE_URL`
- `RATE_LIMIT_*`
- `META_*`
- `AWS_*`
- `WEBMOTORS_*`
- `ML_*`

## Estrategia de sessao em producao

Para manter o comportamento esperado:

- usar access token curto
- manter refresh token em cookie `httpOnly`
- manter Redis altamente disponivel
- expor backend atras de HTTPS
- nao compartilhar segredo JWT entre ambientes sem necessidade

## Deploy local de referencia

Infra basica:

```bash
cd backend
docker compose up -d
```

Backend:

```bash
cd backend
./gradlew bootRun
```

Frontend:

```bash
cd frontend
npm run build
npm run preview
```

## Checklist de validacao apos deploy

1. login funcional
2. cookie de refresh presente e `httpOnly`
3. reload preserva sessao
4. inatividade encerra sessao apos 15 minutos
5. logout invalida a sessao atual
6. `GET /users/assignable` responde para usuarios autenticados esperados
7. monitoramento de Redis e Postgres ativo

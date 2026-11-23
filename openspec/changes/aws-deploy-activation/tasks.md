## 1. Validacao local
- [ ] 1.1 `docker build` do frontend e do backend
- [ ] 1.2 Subir o backend com `SPRING_PROFILES_ACTIVE=prod` e variaveis de teste
- [ ] 1.3 Rodar `ci.yml` e `security.yml` em um PR

## 2. Conta AWS (Ruy)
- [ ] 2.1 Criar conta e Budget com alertas de US$ 50 e US$ 80
- [ ] 2.2 Role OIDC para o GitHub, EC2 com SSM, segredos no Parameter Store

## 3. Staging
- [ ] 3.1 Deploy manual via `deploy.yml` em modo mock
- [ ] 3.2 Smoke test (login, lead, checkout com erro claro sem chave)

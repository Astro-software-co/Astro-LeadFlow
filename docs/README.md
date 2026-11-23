# Astro LeadFlow Docs

Esta pasta guarda documentos operacionais e arquivos de ponte do repositório.

A documentação consolidada e navegável do projeto fica no Obsidian:

- Windows: `C:\Users\ruy.de.castro\Desktop\ruy\meuObsidian\Projetos\Leadflow`
- WSL/Linux: `/mnt/c/Users/ruy.de.castro/Desktop/ruy/meuObsidian/Projetos/Leadflow`
- Entrada: `00-index.md`
- Ponte no repositório: [OBSIDIAN_VAULT.md](./OBSIDIAN_VAULT.md)

Regra para agentes: consultar o vault somente sob demanda, quando o usuário pedir ou quando a tarefa depender de contexto documentado.

Arquivos principais:

- [OBSIDIAN_VAULT.md](./OBSIDIAN_VAULT.md)
- [ARCHITECTURE.md](./ARCHITECTURE.md)
- [GETTING-STARTED.md](./GETTING-STARTED.md)
- [DEVELOPMENT.md](./DEVELOPMENT.md)
- [TESTING.md](./TESTING.md)
- [CONFIGURATION.md](./CONFIGURATION.md)
- [API.md](./API.md)
- [DEPLOYMENT.md](./DEPLOYMENT.md)

Arquivos historicos e material de apoio:

- [HEALTH_REPORT.md](./HEALTH_REPORT.md)
- [varredura-geral-2026-04-17.md](./varredura-geral-2026-04-17.md)
- [notion/INDEX.md](./notion/INDEX.md)

Leitura recomendada para onboarding rapido:

1. [GETTING-STARTED.md](./GETTING-STARTED.md)
2. [ARCHITECTURE.md](./ARCHITECTURE.md)
3. [CONFIGURATION.md](./CONFIGURATION.md)
4. [TESTING.md](./TESTING.md)

Estado atual destacado:

- frontend de desenvolvimento usa integracao real por padrao
- sessao autenticada usa `accessToken` curto em memoria
- `refreshToken` fica em cookie `httpOnly`
- sessao server-side e deslizante com timeout de inatividade de 15 minutos via Redis
- ambiente `dev` garante o usuario `admin@astro.dev / senha123`
- filtro de leads por responsavel usa `GET /api/v1/users/assignable`

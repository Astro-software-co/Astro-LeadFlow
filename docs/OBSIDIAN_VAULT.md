# Obsidian Vault — Astro LeadFlow

## Onde está o vault

A documentação consolidada do projeto fica em:

```
C:\Users\ruy.de.castro\Desktop\ruy\meuObsidian\Projetos\Leadflow
```

Via WSL/Linux:
```
/mnt/c/Users/ruy.de.castro/Desktop/ruy/meuObsidian/Projetos/Leadflow
```

## Como abrir no Obsidian

1. Abrir o Obsidian
2. "Open folder as vault" → selecionar `C:\Users\ruy.de.castro\Desktop\ruy\meuObsidian\`
3. Navegar para `Projetos/Leadflow/00-index.md`

## Estrutura do vault

```
Projetos/Leadflow/
├── 00-index.md                    # Ponto de entrada — começar aqui
├── 00-visao-geral/                # Visão geral, objetivos, glossário
├── 01-negocio/                    # Contexto de negócio, proposta de valor
├── 02-regras-de-negocio/          # Leads, usuários, planos, onboarding
├── 03-arquitetura/                # Arquitetura geral, decisões, integrações
├── 04-infraestrutura/             # Docker, ambientes, deploy, observabilidade
├── 05-backend/                    # Kotlin/Spring: módulos, API, auth, DB
├── 06-frontend/                   # React: rotas, telas, componentes, UX
├── 07-integracoes/                # Marketplaces, WhatsApp, pagamentos
├── 08-bloqueios/                  # Bloqueios e riscos atuais
├── 09-pendencias/                 # Pendências técnicas e de negócio
├── 10-decisoes-tecnicas/          # ADRs — Architecture Decision Records
├── 11-status-e-roadmap/           # Status atual, roadmap, histórico
├── 12-prompts-e-agentes/          # Como usar com Claude Code e Codex
└── 99-revisao/                    # Conflitos, deprecated, revisão pendente
```

## Como pedir para Claude/Codex lerem a documentação

Exemplos de pedidos explícitos:

```
"Leia o Obsidian antes de continuar"
"Consulte a documentação do Leadflow"
"Use o contexto do vault"
"Veja a nota de [tema específico]"
"Atualize o status no Obsidian"
```

## Regra importante: leitura NÃO automática

Os agentes **não devem ler o vault automaticamente** em toda conversa. Isso evita:
- Consumo desnecessário de tokens
- Lentidão na resposta
- Carregamento de contexto irrelevante

A leitura só acontece **quando o usuário solicitar explicitamente** ou quando uma tarefa exigir contexto específico documentado aqui.

## Versionamento e rollback

O vault Obsidian fica fora do repositório Git do projeto. Para versionar o vault, use Git diretamente na pasta do vault ou mantenha commits/snapshots antes de mudanças grandes.

Este repositório mantém a ponte de uso em `docs/OBSIDIAN_VAULT.md`, `CLAUDE.md`, `AGENTS.md` e `.claude/skills/astro-leadflow-project-docs/SKILL.md`.

Para rollback de decisões e histórico operacional, os arquivos `.planning/` preservam os artefatos GSD de cada fase.

## Skill de consulta

Uma skill local do Claude Code está configurada no repositório em:
```
.claude/skills/astro-leadflow-project-docs/SKILL.md
```

Use quando precisar de contexto amplo do projeto.

Para Codex, use as instruções em:

```
AGENTS.md
```

O comportamento esperado é o mesmo: consulta sob demanda, leitura mínima necessária e registro de conflitos/deprecated no vault.

## Documentos principais do vault

| Nota                                  | Quando usar                              |
|---------------------------------------|------------------------------------------|
| `00-index.md`                         | Ponto de entrada e mapa geral            |
| `11-status-e-roadmap/status-atual.md` | Estado atual do projeto                  |
| `02-regras-de-negocio/leads-e-funil.md` | Ciclo de vida de leads               |
| `10-decisoes-tecnicas/adr-index.md`   | Decisões arquiteturais tomadas           |
| `09-pendencias/proximos-passos.md`    | O que fazer agora                        |
| `05-backend/api-e-swagger.md`         | Endpoints da API                         |

# Astro LeadFlow — Codex Instructions

## Project Context

Astro LeadFlow is a multi-tenant CRM SaaS for automotive dealerships. The repository contains:

- `backend/`: Kotlin/Spring Boot API.
- `frontend/`: React/Vite CRM and landing frontend.
- `infra/`: monitoring and infrastructure assets.
- `.planning/`: GSD planning artifacts.
- `docs/`: repository bridge documents and operational references.

## Obsidian Documentation

The consolidated project knowledge base lives outside the repository:

```text
C:\Users\ruy.de.castro\Desktop\ruy\meuObsidian\Projetos\Leadflow
```

WSL/Linux path:

```text
/mnt/c/Users/ruy.de.castro/Desktop/ruy/meuObsidian/Projetos/Leadflow
```

Start from:

```text
00-index.md
```

## On-Demand Rule

Do not read the Obsidian vault automatically on every task.

Read it only when:

- The user explicitly asks to consult Obsidian, the vault, or project documentation.
- The task depends on current product rules, roadmap, decisions, or known conflicts.
- A change may invalidate documented business rules, architecture decisions, or agent instructions.
- The user asks to update project documentation.

For narrow tasks, read only the relevant note from the vault instead of loading the whole tree.

## Vault Update Rule

When updating project documentation:

1. Read `00-index.md` first if the target note is unclear.
2. Update the smallest relevant note.
3. Preserve Obsidian links in `[[note]]` format.
4. Add related-note links when a new connection is useful for graph navigation.
5. Update the note's "Última atualização" metadata.
6. Register conflicting information in `99-revisao/conflitos-encontrados.md`.
7. Register obsolete information in `99-revisao/itens-deprecated.md`.

## Repository Markdown Policy

- Preserve `README.md`, `CLAUDE.md`, `AGENTS.md`, `docs/OBSIDIAN_VAULT.md`, `.planning/`, `.claude/`, and operational docs that are still used by tooling or external exports.
- Prefer the Obsidian vault for consolidated project knowledge.
- Do not add broad duplicate documentation under `docs/` unless it is a repository bridge, generated artifact, or operational file.

## Current Technical Guardrails

- PT-BR default, BRL default.
- React 18 + Vite + Tailwind 3 + shadcn/ui.
- Use Lucide icons for frontend UI.
- Backend contracts are the source of truth.
- Do not store auth tokens in `localStorage`.
- Keep access tokens in memory and restore sessions through backend httpOnly refresh cookie.
- React Query cache keys must be organization scoped.
- Clear the full query cache on logout.
- Sonner is the canonical toast system.

## Useful Commands

```bash
cd frontend && npm run lint
cd frontend && npm run test:coverage
cd frontend && npm run test:e2e
cd backend && ./gradlew test
```

## Related Documentation

- Repository bridge: `docs/OBSIDIAN_VAULT.md`
- Vault index: `Projetos/Leadflow/00-index.md`
- Agent usage notes in vault:
  - `12-prompts-e-agentes/uso-com-codex.md`
  - `12-prompts-e-agentes/padrao-de-atualizacao-do-vault.md`
  - `12-prompts-e-agentes/skill-astro-leadflow.md`

## OpenSpec

Specs of the implemented behavior live in `openspec/specs/`, open work in `openspec/changes/`, and the current status, blockers and open business rules in `openspec/STATUS.md`. Read `openspec/STATUS.md` before starting a task.

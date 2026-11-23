## Why
O `AwsLexEngine` e um stub que devolve "Lex AWS ainda nao esta ativo". O bot Lex nao existe na AWS e nao ha IaC. Sem ele o produto nao qualifica de verdade, so responde por palavra-chave (mock).

## What Changes
- `AwsLexEngine` chamando `RecognizeText` (Lex V2, pt_BR), mapeando slots para o contrato canonico, sem nunca lancar excecao.
- Bot Lex definido em IaC (Terraform ou CDK), com alias e versao.
- Medicao de uso do Lex (hoje o `AiTokenTracker` ignora tokens zero).
- Spike para decidir o slot de texto livre do veiculo em pt_BR.
- Opcional: `AwsBedrockEngine` como fallback do plano Enterprise.

## Impact
Modulo `chatbot` (engines aws, config), IaC novo em `infra/`. Plano completo em `docs/CHATBOT-LEX-PLANO.md`. Depende da conta AWS.

## Perguntas em aberto
- Qual tipo de slot usar para o veiculo em texto livre?
- O Lex entende "50 mil" como numero em pt_BR?
- Custo por requisicao de texto (confirmar no preco atual da AWS).
- Regra de negocio: horario comercial por organizacao e mensagem de handoff fora do horario ("retornamos as 9h").

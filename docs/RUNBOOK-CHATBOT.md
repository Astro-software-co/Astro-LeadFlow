# Runbook: Chatbot

## Modo Dev

O modo padrao e `CHATBOT_ENGINE_MODE=mock`. Nesse modo, Lex e Bedrock sao substituidos por engines deterministicas por keyword matching, sem custo externo.

## Ativacao AWS [AWS-PENDING]

1. Configure `CHATBOT_ENGINE_MODE=aws`.
2. Configure `AWS_REGION`, `AWS_LEX_BOT_ID`, `AWS_LEX_BOT_ALIAS_ID`, `AWS_LEX_LOCALE_ID=pt_BR` e `AWS_BEDROCK_MODEL_ID`.
3. Garanta credenciais AWS no ambiente.
4. Valide quotas de IA por plano antes de liberar Enterprise.

As classes AWS permanecem compilaveis, mas inativas enquanto `CHATBOT_ENGINE_MODE=mock`.

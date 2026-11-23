## 1. Spike
- [ ] 1.1 Criar bot de teste e decidir o slot de veiculo e o tratamento de numeros em pt_BR

## 2. Motor
- [ ] 2.1 Bean `LexRuntimeV2Client` configuravel (regiao, credenciais por instance profile)
- [ ] 2.2 `AwsLexEngine.handle` com `RecognizeText`, `sessionId` = id da sessao do bot, atributos com orgId e leadId
- [ ] 2.3 Mapear slots e intents para o contrato canonico; timeout e fallback para template
- [ ] 2.4 Testes com `LexRuntimeV2Client` mockado

## 3. Infra e medicao
- [ ] 3.1 IaC do bot (intents, slot types, prompts pt_BR, alias)
- [ ] 3.2 Contar requisicoes do Lex no `AiTokenTracker`/metricas

## 4. Regras de horario
- [ ] 4.1 Horario comercial configuravel por organizacao
- [ ] 4.2 Mensagem de handoff fora do horario

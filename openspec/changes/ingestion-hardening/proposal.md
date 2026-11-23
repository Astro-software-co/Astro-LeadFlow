## Why
Hoje leads de marketplace se perdem em tres pontos: com `APP_AWS_ENABLED=false` o evento e gravado e nunca processado; o `X-Webhook-Token` provavelmente nao e enviado pelo Mercado Livre (e com token vazio o endpoint fica aberto); e lead sem telefone vai para a DLQ. Isso quebra o valor central do produto (todo lead visivel em segundos).

## What Changes
- Processar a ingestao inline quando a fila estiver desligada.
- Autenticar a notificacao do Mercado Livre por assinatura HMAC ou por consulta ao recurso, e nunca aceitar webhook sem autenticacao em producao.
- Serializar o refresh de token do Mercado Livre por credencial (token rotativo, uso unico).
- Aceitar lead sem telefone como lead pendente.
- Buscar marca, modelo e ano do anuncio no Mercado Livre.
- Webmotors: implementar polling se a Webmotors confirmar que so ha consulta.

## Impact
Modulo `ingestion` (controller, producer, consumer, fetchers, auth services). Detalhes e fontes em `docs/INTEGRACOES-MARKETPLACES-META.md`.

## Perguntas em aberto
- A Webmotors entrega por push ou so por consulta? Qual a lista de `idTipoLead`?
- O Mercado Livre assina as notificacoes? Qual header e composicao?
- Contatos `question` do ML trazem telefone?
- Regra de negocio: lead sem telefone vira lead pendente, e quem o trata?
- Regra de negocio: a chave de duplicidade continua sendo so o telefone, ou telefone mais veiculo?

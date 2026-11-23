## 1. Entrada sem AWS
- [ ] 1.1 Processar o evento de marketplace inline quando `app.aws.enabled=false`
- [ ] 1.2 Teste: webhook sem SQS cria o lead

## 2. Autenticidade do webhook
- [ ] 2.1 Confirmar com o Mercado Livre o formato da assinatura (ou validar consultando `/vis/leads/{id}`)
- [ ] 2.2 Remover o `return true` quando o token estiver vazio em producao
- [ ] 2.3 Testes de 401 e de assinatura valida

## 3. Token do Mercado Livre
- [ ] 3.1 Serializar o refresh por credencial (lock ou transacao)
- [ ] 3.2 Tratar `invalid_grant` com sinal para reautorizacao guiada

## 4. Lead sem telefone e dados do anuncio
- [ ] 4.1 Decidir a regra do lead pendente (pergunta em aberto)
- [ ] 4.2 Buscar marca, modelo e ano pelo `item_id`

## 5. Webmotors
- [ ] 5.1 Obter resposta da Webmotors (push ou consulta)
- [ ] 5.2 Se consulta, criar job de polling analogo ao do Mercado Livre

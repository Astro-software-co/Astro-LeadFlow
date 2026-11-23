## Why
A integracao com a Meta nunca foi validada contra a API real. O codigo usa Graph API v18.0, envia template apenas pelo nome (sem `components`) e o filtro de assinatura nao foi testado com POST assinado real. Sem a Meta o lead nao recebe a primeira mensagem.

## What Changes
- Versao da Graph API configuravel e atualizada.
- `sendTemplate` com `components` quando o template tiver variaveis.
- Teste do filtro de assinatura com POST assinado real e confirmacao da chave de replay.
- Validacao ponta a ponta em `meta.api.mode=real` com numero de teste.

## Impact
Modulo `communication` (meta client, filtro de assinatura, sender). Depende de acoes fora do codigo: verificacao de negocio, numero, token permanente e aprovacao do template `welcome_lead`.

## Perguntas em aberto
- A Meta aceita `welcome_lead` como UTILITY ou reclassifica como MARKETING?
- Qual o preco por mensagem de marketing em BRL (rate card oficial)?
- Regra de negocio: quem absorve o custo das mensagens (limite do plano x repasse)?
- A v18.0 ainda responde?

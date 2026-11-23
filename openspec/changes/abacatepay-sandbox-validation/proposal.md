## Why
O checkout, o webhook e o `payment_failed` estao implementados e testados com mocks, mas nunca rodaram contra o Abacate Pay real. Ha tambem verificacoes manuais pendentes da Fase 14.

## What Changes
- Cadastrar a conta e a chave sandbox; rodar checkout e webhook de ponta a ponta.
- Cadastrar os 9 produtos (ou deixar o codigo cria-los) conforme `docs/ABACATEPAY-PRODUTOS.md`, gravando o id em `plans.abacate_product_id`.
- Conferir no navegador que a barra de abas de Configuracoes nao tem scroll vertical em notebook.

## Impact
Modulo `saas`, banco (`plans`), documentacao. Depende de a Ruy criar a conta e a chave sandbox.

## Perguntas em aberto
- O Abacate Pay aceita imagem no produto? Quais os valores aceitos para o ciclo?
- Regra de negocio: o que acontece com a organizacao em `PAST_DUE` (acesso mantido, limitado ou bloqueado)? E quando o trial de 14 dias acaba sem plano pago?
- O plano `free` (padrao em `DEFAULT_PLAN`) nao esta nos seeds: ele deve existir?
- As descricoes dos planos prometem chatbot e IA; vender antes de ativar o Lex?

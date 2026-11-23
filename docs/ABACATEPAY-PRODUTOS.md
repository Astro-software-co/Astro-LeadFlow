# Produtos de assinatura para cadastrar no Abacate Pay

São 9 produtos: 3 planos x 3 ciclos. Os preços, limites e ciclos vêm do banco (`V1` e `V5__saas_plan_offers.sql`); a descrição e as imagens abaixo são conteúdo novo.

## Antes de cadastrar à mão: o código já cria os produtos

`AbacatePaySubscriptionService.ensureRemoteProduct` cria o produto no Abacate Pay no **primeiro checkout** de cada plano e grava o id em `plans.abacate_product_id`. Duas opções:

1. **Deixar o código criar** (mais simples). O nome será `<Plano> <Ciclo>` (ex.: "Básico Mensal") e a descrição será a que está em `plans.description`, hoje genérica ("Plano Basico para equipes em crescimento."). Para usar a descrição nova, atualizar `plans.description` antes do primeiro checkout.
2. **Cadastrar à mão** (necessário se quiser imagem e nome com a marca). Depois, gravar o id de cada produto em `plans.abacate_product_id`, senão o código criará um duplicado no primeiro checkout:

```sql
UPDATE plans SET abacate_product_id = 'prod_...' , abacate_product_external_id = 'leadflow-plan:' || id
WHERE code = 'BASICO_MONTHLY';  -- repetir para os 9 códigos da tabela abaixo
```

Preço no Abacate Pay deve ser exatamente o do banco, em centavos (o checkout cobra o produto, não o banco).

## Tabela de produtos

| Código no banco | Nome do produto | Ciclo (Abacate) | Preço | Em centavos |
|---|---|---|---|---|
| `BASICO_MONTHLY` | Astro LeadFlow Básico Mensal | MONTHLY | R$ 149,00 | 14900 |
| `BASICO_SEMIANNUALLY` | Astro LeadFlow Básico Semestral | SEMIANNUALLY | R$ 804,60 | 80460 |
| `BASICO_ANNUALLY` | Astro LeadFlow Básico Anual | ANNUALLY | R$ 1.430,40 | 143040 |
| `PROFISSIONAL_MONTHLY` | Astro LeadFlow Profissional Mensal | MONTHLY | R$ 299,00 | 29900 |
| `PROFISSIONAL_SEMIANNUALLY` | Astro LeadFlow Profissional Semestral | SEMIANNUALLY | R$ 1.614,60 | 161460 |
| `PROFISSIONAL_ANNUALLY` | Astro LeadFlow Profissional Anual | ANNUALLY | R$ 2.870,40 | 287040 |
| `ENTERPRISE_MONTHLY` | Astro LeadFlow Enterprise Mensal | MONTHLY | R$ 599,00 | 59900 |
| `ENTERPRISE_SEMIANNUALLY` | Astro LeadFlow Enterprise Semestral | SEMIANNUALLY | R$ 3.234,60 | 323460 |
| `ENTERPRISE_ANNUALLY` | Astro LeadFlow Enterprise Anual | ANNUALLY | R$ 5.750,40 | 575040 |

Os valores semestral e anual já incluem desconto sobre o mensal: **10%** no semestral (149 x 6 x 0,90) e **20%** no anual (149 x 12 x 0,80). Vale dizer isso na descrição do ciclo.

## Descrições (até 255 caracteres, texto simples)

Cada produto usa a descrição do plano mais a frase do ciclo.

**Básico** (1 marketplace, 1.000 mensagens/mês, sem chatbot de IA, só respostas por template)

- Mensal: `Para começar a centralizar seus leads: 1 marketplace conectado, até 1.000 mensagens por mês, Kanban de vendas, dashboard e estoque de veículos. Cobrança mensal.`
- Semestral: `Para começar a centralizar seus leads: 1 marketplace conectado, até 1.000 mensagens por mês, Kanban de vendas, dashboard e estoque. Cobrança a cada 6 meses, 10% de desconto.`
- Anual: `Para começar a centralizar seus leads: 1 marketplace conectado, até 1.000 mensagens por mês, Kanban de vendas, dashboard e estoque. Cobrança anual, 20% de desconto.`

**Profissional** (3 marketplaces, 3.000 mensagens/mês, chatbot de qualificação Lex)

- Mensal: `Para lojas com mais volume: até 3 marketplaces, 3.000 mensagens por mês e chatbot que qualifica cada lead no WhatsApp antes de chegar ao vendedor. Inclui Kanban, dashboard e estoque. Cobrança mensal.`
- Semestral: `Para lojas com mais volume: até 3 marketplaces, 3.000 mensagens por mês e chatbot que qualifica cada lead no WhatsApp. Inclui Kanban, dashboard e estoque. Cobrança a cada 6 meses, 10% de desconto.`
- Anual: `Para lojas com mais volume: até 3 marketplaces, 3.000 mensagens por mês e chatbot que qualifica cada lead no WhatsApp. Inclui Kanban, dashboard e estoque. Cobrança anual, 20% de desconto.`

**Enterprise** (10 marketplaces, 10.000 mensagens/mês, chatbot Lex com IA generativa de apoio, 500 mil tokens/mês)

- Mensal: `Para grupos e redes de lojas: até 10 marketplaces, 10.000 mensagens por mês e chatbot com IA generativa de apoio para entender qualquer pergunta do cliente. Inclui Kanban, dashboard e estoque. Cobrança mensal.`
- Semestral: `Para grupos e redes de lojas: até 10 marketplaces, 10.000 mensagens por mês e chatbot com IA generativa de apoio. Inclui Kanban, dashboard e estoque. Cobrança a cada 6 meses, 10% de desconto.`
- Anual: `Para grupos e redes de lojas: até 10 marketplaces, 10.000 mensagens por mês e chatbot com IA generativa de apoio. Inclui Kanban, dashboard e estoque. Cobrança anual, 20% de desconto.`

Atenção às promessas: o chatbot com Lex e a IA generativa **ainda não estão ativos** (o bot está em modo mock, ver `docs/CHATBOT-LEX-PLANO.md`) e a integração real com Webmotors e Mercado Livre depende das aprovações. Se for vender antes disso, troque "chatbot que qualifica" por "em breve" nas descrições do Profissional e do Enterprise.

## Imagens: três ilustrações, reaproveitadas nos três ciclos

Uma imagem por plano (o ciclo vai no nome do produto, não na imagem). Formato quadrado 1:1, 1024x1024 ou maior. Dois motivos para não pedir texto dentro da imagem: geradores de imagem erram texto, e o nome já aparece ao lado no checkout.

Direção visual comum (cole no início de cada prompt):

> Ilustração vetorial limpa em estilo Soft UI, fundo azul-ardósia muito escuro (#0F172A) com um leve brilho azul (#2563EB) ao centro, formas geométricas arredondadas, sombras suaves, traços finos em azul claro e branco, composição centralizada e simétrica com respiro nas bordas, aparência profissional de produto SaaS. Sem texto, sem letras, sem logotipos, sem pessoas, sem fotos de estoque, sem gradiente roxo ou rosa, sem efeito vidro exagerado.

**Prompt 1 — Básico (um canal, entrada organizada)**

> [Direção visual comum]. Uma silhueta estilizada e minimalista de um carro sedan visto de lado, ao centro, ligada por uma única linha luminosa azul a um pequeno ícone de anúncio de marketplace à esquerda e a um balão de conversa do WhatsApp à direita. Uma pequena estrela de quatro pontas acima do carro, discreta. Transmite simplicidade e um primeiro passo: um canal, um fluxo.

**Prompt 2 — Profissional (vários canais convergindo, automação)**

> [Direção visual comum]. A mesma silhueta de carro sedan ao centro, agora com três linhas luminosas azuis que partem de três ícones de anúncio de marketplace e convergem num funil estilizado, seguido de um balão de conversa do WhatsApp com um pequeno ícone de robô assistente. Uma estrela de quatro pontas brilhante no topo. Transmite volume, automação e qualificação do lead.

**Prompt 3 — Enterprise (rede de lojas, escala)**

> [Direção visual comum]. Uma constelação de dez pontos de luz conectados por linhas finas, formando uma rede ao redor de uma silhueta central de carro sedan, com pequenos ícones de concessionária e de gráfico de barras nos nós. Uma grande estrela de quatro pontas luminosa no centro superior, com halo azul. Transmite escala, rede de lojas e inteligência.

Prompt negativo (para geradores que aceitam): `texto, letras, números, marca d'água, logotipo de terceiros, pessoas, rostos, foto realista, roxo, rosa, neon excessivo, vidro fosco exagerado, ruído, baixa resolução`

Dicas de consistência: gerar os três na mesma sessão com a mesma semente (seed) quando a ferramenta permitir, e escolher a variação em que a silhueta do carro seja igual nas três. Se a ferramenta distorcer o carro, gere o fundo e a rede separados e componha o carro depois.

## Não verifiquei

- Se o Abacate Pay aceita imagem no cadastro do produto, qual o formato e o tamanho máximo, e se o campo de ciclo usa exatamente `MONTHLY`, `SEMIANNUALLY` e `ANNUALLY`. Pelo código, esses nomes são os que o backend envia; confirme no painel antes de cadastrar à mão.
- O plano `free` (padrão em `DEFAULT_PLAN`) não está nos seeds do banco e não é vendável (`isSelfServiceAvailable` exige preço maior que zero).

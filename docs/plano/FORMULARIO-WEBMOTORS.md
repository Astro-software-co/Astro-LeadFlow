# Rascunho — Solicitação de liberação da API de Consulta de Leads (Webmotors)

> Os campos `[PREENCHER]` dependem de dados que só o Ruy tem. As respostas dissertativas estão redigidas com base no que **já existe** no produto; os itens marcados **(depende da Fase N)** só podem ser declarados depois que a fase correspondente de `docs/plano/PLANO-IMPLEMENTACAO.md` estiver pronta.
> **Não enviar** antes de: Fase 2 (LGPD) e Fase 3 (páginas legais + insumos) concluídas.

## A. Campos do formulário (chamado ao Suporte do Portal do Desenvolvedor)

| Campo | Resposta | Origem / status |
|---|---|---|
| Nome do solicitante | `[PREENCHER]` | Ruy |
| Contato do solicitante (e-mail e telefone) | `[PREENCHER]` | Ruy |
| Contato do canal de suporte (e-mail e telefone) | `[PREENCHER]` ex.: `suporte@<dominio>` + telefone | Depende do **domínio** e de a Fase 9 (SES) ativar o e-mail |
| Nome do CRM | **Astro LeadFlow** | Definido |
| Razão social | `[PREENCHER]` (Astro Software Co.? confirmar a grafia jurídica) | Ruy |
| CNPJ | `[PREENCHER]` | Ruy |
| Site | `[PREENCHER]` URL pública da landing | Depende do deploy/domínio |
| Nome da APP do Portal do Desenvolvedor | `[PREENCHER]` — **registrar o app antes** no Portal (Sensedia) | Ruy |
| Logo do CRM (PNG) | `frontend/public/brand/astro-leadflow-logo.png` | Fase 3 / T3.3 (arquivo do Ruy) |
| CNPJ de clientes em comum com a Webmotors | `[PREENCHER]` ao menos o da concessionária piloto | Ruy / piloto |
| Regiões de atuação do CRM | `[PREENCHER]` | Ruy |

## B. Respostas dissertativas (rascunho)

### B1. Qual o objetivo da integração?
O Astro LeadFlow é um CRM SaaS para concessionárias e revendas de veículos. O objetivo é **consultar, por meio da API de Consulta de Leads, os leads gerados pelos anúncios da loja na Webmotors** e disponibilizá-los no funil comercial da própria loja (Kanban), com atribuição a vendedores e histórico de atendimento, para que cada lead seja visível e acionável em segundos. A integração é feita por **polling autenticado** com as credenciais do usuário integrador que **a própria loja** cria no Cockpit e autoriza, e permite **aceitar ou recusar** a proposta (com o motivo padronizado da Webmotors) diretamente pela tela do lojista.

### B2. De que forma os dados dos leads Webmotors serão processados e disponibilizados aos lojistas?
- **Coleta:** o sistema consulta periodicamente a API usando as credenciais da loja; credenciais ficam armazenadas **cifradas** (AES-256-GCM) e nunca aparecem em logs ou telas. **(o módulo Webmotors já cifra as suas; o escopo completo de segredos depende da Fase 6.)**
- **Isolamento:** cada loja (organização) só acessa os próprios leads; todo acesso é autenticado e filtrado pela organização do usuário.
- **Processamento:** a proposta é registrada, o telefone é normalizado e validado, e, após o **aceite**, o contato é obtido e o lead entra no funil da loja. Se o mesmo cliente demonstrar interesse em outros veículos da loja, os interesses são **agrupados no mesmo atendimento** em vez de gerar cartões separados. **(depende da Fase 4.)**
- **Disponibilização:** o lojista (gerente) vê a fila de propostas, aceita ou recusa, e acompanha o lead no Kanban. Vendedores veem apenas os leads atribuídos a eles, conforme perfil de acesso.
- **Contato inicial e consentimento:** o primeiro contato é uma mensagem de WhatsApp que **identifica a loja, o veículo e o portal de origem** e traz a opção de **não receber mais mensagens** (`SAIR`). **Assim que o cliente responde, o sistema solicita o aceite dos Termos e Condições e da Política de Privacidade**, e o atendimento automatizado só prossegue após o aceite; quem não aceita ou se descadastra **não recebe novas mensagens do sistema**. Mensagens só são enviadas em atendimento novo ou reaberto, nunca a cada novo interesse. **(depende da Fase 4 / bloco 4.C, da validação jurídica da base legal e da homologação da Meta.)**
- **Finalidade:** uso exclusivo para atendimento comercial do anúncio pela loja que o gerou. Os dados **não** são compartilhados com terceiros para outras finalidades.
- **Retenção:** payloads brutos recebidos são expurgados após prazo configurável; dados cadastrais do atendimento (nome, telefone, e-mail) são mantidos pela loja para **suporte pós-venda** e relacionamento, sujeitos à política publicada e aos direitos do titular; o **conteúdo das conversas** de atendimentos encerrados (arquivados ou concluídos) é removido **3 meses após o encerramento**; dados de lead permanecem enquanto a loja os mantiver, com anonimização sob solicitação. **(depende da Fase 2 / T2.5 e da Fase 4 / T4.9; prazos sujeitos à revisão jurídica.)**

### B3. Canal oficial para solicitações de anonimização e SLA
- **Canal:** `[PREENCHER] privacidade@<dominio>` e página pública `/privacidade`. **(depende da Fase 3.)**
- **Papéis:** a concessionária é a **controladora** dos dados e a Astro, **operadora**. Pedidos de titulares recebidos diretamente pela Astro são registrados e encaminhados/atendidos em nome da loja.
- **Exclusão pela própria loja:** o responsável da concessionária pode **excluir ou anonimizar um lead manualmente** pela plataforma, a qualquer momento, com registro de auditoria sem dados pessoais. **(depende da Fase 2 / T2.7.)**
- **Processo:** cada pedido é **registrado com prazo**, validado quanto à identidade do titular e executado por rotina de **anonimização** que cobre o lead, as mensagens do chat, as sessões do chatbot e os payloads brutos associados, com relatório de execução e trilha de auditoria. **(depende da Fase 2 / T2.1–T2.3.)**
- **SLA proposto:** confirmação de recebimento em até **2 dias úteis** e conclusão em até **15 dias corridos** (prazo máximo do art. 19, II da LGPD). `[PREENCHER]` se o Ruy preferir declarar um prazo menor, **só se o processo comprovadamente o cumprir**.
- **Limitação a declarar com transparência:** a anonimização não retroage a cópias de segurança, que seguem a janela de retenção definida no runbook de privacidade.

> **Revisão jurídica obrigatória** das respostas B2 e B3 (papéis, base legal do contato por WhatsApp, prazo) antes do envio.

## C. Roteiro de envio

1. Concluir Fases 2 e 3 do plano e receber os insumos do Ruy (seção 6 do plano).
2. Registrar o app no Portal do Desenvolvedor e anotar o nome exato.
3. Preencher os campos `[PREENCHER]`; revisar B2/B3 com o jurídico.
4. Abrir o chamado de "liberação da API de Consulta de Leads" com os dados acima.
5. Na resposta/reunião de homologação, confirmar os itens H3–H13 da Fase 10 do plano (paginação, contato antes/depois do aceite, tipos, canais, rate limit, produção, significado comercial de "aceitar").

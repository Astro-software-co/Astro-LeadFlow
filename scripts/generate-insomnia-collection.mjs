import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

const output = resolve('docs/insomnia/astro-leadflow-insomnia.json');
const now = Date.now();
const workspaceId = 'wrk_astro_leadflow';
const envId = 'env_astro_leadflow_base';

const commonHeaders = [{ name: 'Content-Type', value: 'application/json' }];
const bearerAuth = { type: 'bearer', token: '{{ _.access_token }}', prefix: 'Bearer' };
const noAuth = {};

const envData = {
  base_url: 'http://localhost:8080',
  access_token: '',
  astro_admin_token: '',
  manager_token: '',
  seller_token: '',
  x_api_key: '',
  webhook_token: 'dev-marketplace-webhook-token',
  meta_verify_token: 'dev-webhook-token',
  abacatepay_webhook_secret: 'dev-abacate-webhook-secret',
  organization_id: '00000000-0000-0000-0000-000000000000',
  user_id: '00000000-0000-0000-0000-000000000000',
  seller_user_id: '00000000-0000-0000-0000-000000000000',
  lead_id: '00000000-0000-0000-0000-000000000000',
  vehicle_id: '00000000-0000-0000-0000-000000000000',
  vehicle_photo_id: '00000000-0000-0000-0000-000000000000',
  plan_id: '00000000-0000-0000-0000-000000000000',
  marketplace_config_id: '00000000-0000-0000-0000-000000000000',
  onboarding_id: '00000000-0000-0000-0000-000000000000',
  bot_session_id: '00000000-0000-0000-0000-000000000000',
  error_id: '1',
  lead_phone: '+5511999999999',
  auth_email: 'admin@astro.dev',
  auth_password: 'password123',
};

const groups = [
  {
    id: 'auth',
    name: '00 Auth & Sessao',
    description: 'Login, refresh, 2FA, registro e logout.',
    requests: [
      post('Register Organization', '/api/v1/auth/register', {
        organizationName: 'Astro Demo Motors',
        slug: 'astro-demo-motors',
        adminName: 'Admin Demo',
        adminEmail: 'admin.demo@astro.dev',
        password: 'password123',
      }, noAuth),
      post('Login', '/api/v1/auth/login', {
        email: '{{ _.auth_email }}',
        password: '{{ _.auth_password }}',
      }, noAuth),
      post('Verify 2FA', '/api/v1/auth/2fa/verify', {
        email: '{{ _.auth_email }}',
        code: '123456',
      }, noAuth),
      post('Refresh Session', '/api/v1/auth/refresh', {}, noAuth),
      post('Logout', '/api/v1/auth/logout', {}, bearerAuth),
    ],
  },
  {
    id: 'iam_users',
    name: '01 IAM - Usuarios',
    description: 'Perfil, equipe, RBAC e preferencias de usuario.',
    requests: [
      get('Get Me', '/api/v1/users/me'),
      patch('Update Me', '/api/v1/users/me', { name: 'Gerente Demo', phoneNumber: '+5511988887777', avatarUrl: null }),
      patch('Change Password', '/api/v1/users/me/password', { currentPassword: 'password123', newPassword: 'newPassword123', confirmNewPassword: 'newPassword123' }),
      patch('Update Notifications', '/api/v1/users/me/notifications', {
        notifyNewLead: true,
        notifyLeadWaiting: true,
        notifySaleCompleted: true,
        notifyCardMoved: false,
        notifyViaEmail: true,
        notifyViaPush: false,
      }),
      patch('Update Two Factor', '/api/v1/users/me/two-factor', { emailEnabled: true, smsEnabled: false }),
      get('List Team Users', '/api/v1/users'),
      get('List Assignable Users', '/api/v1/users/assignable'),
      get('Get User By ID', '/api/v1/users/{{ _.user_id }}'),
      post('Invite Seller', '/api/v1/users/invite', { name: 'Vendedor Demo', email: 'seller.demo@astro.dev', role: 'SELLER', phoneNumber: '+5511977776666' }),
      patch('Update Team User', '/api/v1/users/{{ _.user_id }}', { name: 'Vendedor Atualizado', email: 'seller.demo@astro.dev', phoneNumber: '+5511966665555' }),
      patch('Update User Role', '/api/v1/users/{{ _.user_id }}/role', { role: 'SELLER' }),
      patch('Suspend User', '/api/v1/users/{{ _.user_id }}/suspend', null),
      patch('Reactivate User', '/api/v1/users/{{ _.user_id }}/reactivate', null),
      del('Delete User', '/api/v1/users/{{ _.user_id }}'),
    ],
  },
  {
    id: 'iam_org',
    name: '02 IAM - Organizacao',
    description: 'Dados da organizacao e credenciais WhatsApp/Meta por organizacao.',
    requests: [
      get('Get My Organization', '/api/v1/organization'),
      put('Update Organization', '/api/v1/organization', { name: 'Astro Demo Motors', slug: 'astro-demo-motors' }),
      patch('Update WhatsApp Credentials', '/api/v1/organization/whatsapp', {
        whatsappToken: 'EAAB...',
        whatsappPhoneNumberId: '123456789012345',
      }),
    ],
  },
  {
    id: 'onboarding',
    name: '03 Onboarding',
    description: 'Solicitacao publica e aprovacao/rejeicao pelo Astro Admin.',
    requests: [
      post('Submit Public Onboarding', '/api/onboarding', {
        companyName: 'Nova Loja Motors',
        tradeName: 'Nova Loja',
        cnpj: '12345678000190',
        planCode: 'PROFISSIONAL_MONTHLY',
        ownerName: 'Maria Responsavel',
        ownerCpf: '12345678901',
        ownerEmail: 'maria@novaloja.com.br',
        ownerPhone: '+5511999999999',
        cep: '01310-100',
        street: 'Avenida Paulista',
        number: '1000',
        complement: 'Sala 10',
        neighborhood: 'Bela Vista',
        city: 'Sao Paulo',
        state: 'SP',
        ownerRole: 'Socio administrador',
        dealershipSegment: 'Seminovos',
        branchCount: 1,
        employeeCount: 8,
        stockCount: 45,
        monthlyLeadsAvg: 180,
        marketYears: 6,
        website: 'https://novaloja.example.com',
        intendedMarketplaces: ['WEBMOTORS', 'MERCADO_LIVRE'],
        otherMarketplaces: 'Napista',
        referralCode: '123456',
      }, noAuth),
      get('Admin List Onboarding', '/api/v1/admin/onboarding', [
        param('status', 'PENDING_APPROVAL'),
        param('query', ''),
        param('page', '0'),
        param('size', '20'),
      ]),
      get('Admin Get Onboarding', '/api/v1/admin/onboarding/{{ _.onboarding_id }}'),
      post('Admin Approve Onboarding', '/api/v1/admin/onboarding/{{ _.onboarding_id }}/approve', null),
      post('Admin Reject Onboarding', '/api/v1/admin/onboarding/{{ _.onboarding_id }}/reject', { rejectionReason: 'Dados incompletos para aprovacao.' }),
      get('Legacy Admin List Onboarding (/api/admin)', '/api/admin/onboarding', [param('page', '0'), param('size', '20')]),
      get('Legacy Admin Get Onboarding (/api/admin)', '/api/admin/onboarding/{{ _.onboarding_id }}'),
      post('Legacy Admin Approve Onboarding (/api/admin)', '/api/admin/onboarding/{{ _.onboarding_id }}/approve', null),
      post('Legacy Admin Reject Onboarding (/api/admin)', '/api/admin/onboarding/{{ _.onboarding_id }}/reject', { rejectionReason: 'Dados incompletos para aprovacao.' }),
    ],
  },
  {
    id: 'crm',
    name: '04 CRM - Leads',
    description: 'CRUD, timeline, movimentacao de etapa, atribuicao e arquivamento.',
    requests: [
      post('Create Lead', '/api/v1/leads', {
        name: 'Joao Cliente',
        phoneNumber: '{{ _.lead_phone }}',
        email: 'joao.cliente@example.com',
        origin: 'WebMotors',
        vehicleInterest: 'Honda Civic 2023',
        budget: 120000,
        urgency: 'Alta',
        purchaseForecast: '30 dias',
        interestType: 'BUY',
        notes: 'Quer avaliar troca.',
        vehicleId: null,
      }),
      get('List Leads', '/api/v1/leads', [
        param('search', ''),
        param('origin', ''),
        param('stage', ''),
        param('assignedTo', ''),
        param('dateFrom', ''),
        param('dateTo', ''),
        param('page', '0'),
        param('size', '20'),
      ]),
      get('Get Lead', '/api/v1/leads/{{ _.lead_id }}'),
      get('Get Lead Activities', '/api/v1/leads/{{ _.lead_id }}/activities'),
      put('Update Lead', '/api/v1/leads/{{ _.lead_id }}', {
        name: 'Joao Cliente Atualizado',
        email: 'joao.novo@example.com',
        vehicleInterest: 'Corolla 2022',
        budget: 105000,
        urgency: 'Media',
        purchaseForecast: '60 dias',
        interestType: 'BUY',
        notes: 'Atualizacao via Insomnia.',
        vehicleId: null,
      }),
      patch('Move Lead Stage', '/api/v1/leads/{{ _.lead_id }}/move', { toStage: 'IN_ATTENDANCE' }),
      patch('Archive Lead', '/api/v1/leads/{{ _.lead_id }}/archive', null),
      patch('Assume Lead', '/api/v1/leads/{{ _.lead_id }}/assume', null),
      patch('Assign Lead', '/api/v1/leads/{{ _.lead_id }}/assign', { userId: '{{ _.seller_user_id }}' }),
      del('Delete Lead', '/api/v1/leads/{{ _.lead_id }}'),
    ],
  },
  {
    id: 'kanban',
    name: '05 CRM - Kanban',
    description: 'Board agrupado por etapa.',
    requests: [
      get('Get Kanban Board', '/api/v1/kanban/board'),
    ],
  },
  {
    id: 'inventory',
    name: '06 Estoque - Veiculos',
    description: 'Cadastro, filtros, detalhe e resumo de estoque.',
    requests: [
      post('Create Vehicle', '/api/v1/vehicles', {
        brand: 'Honda',
        model: 'Civic Touring',
        year: 2023,
        color: 'Prata',
        licensePlate: 'ABC1D23',
        salePrice: 149900,
        costPrice: 132000,
        mileageKm: 18000,
        ipvaPaid: true,
        category: 'SEDAN',
        status: 'AVAILABLE',
      }),
      get('List Vehicles', '/api/v1/vehicles', [
        param('search', 'Honda'),
        param('status', 'AVAILABLE'),
        param('category', ''),
        param('year', ''),
        param('minPrice', ''),
        param('maxPrice', ''),
        param('page', '0'),
        param('size', '20'),
      ]),
      get('Get Vehicle', '/api/v1/vehicles/{{ _.vehicle_id }}'),
      put('Update Vehicle', '/api/v1/vehicles/{{ _.vehicle_id }}', {
        brand: 'Honda',
        model: 'Civic Touring',
        year: 2023,
        color: 'Preto',
        salePrice: 147900,
        mileageKm: 19000,
        status: 'AVAILABLE',
      }),
      del('Delete Vehicle', '/api/v1/vehicles/{{ _.vehicle_id }}'),
      get('Vehicle Stock Summary', '/api/v1/vehicles/summary'),
    ],
  },
  {
    id: 'vehicle_photos',
    name: '07 Estoque - Fotos',
    description: 'Upload, listagem, remocao e ordenacao de fotos.',
    requests: [
      multipart('Upload Vehicle Photo', '/api/v1/vehicles/{{ _.vehicle_id }}/photos', [
        { name: 'file', value: '', type: 'file' },
        { name: 'position', value: '0', type: 'text' },
      ]),
      get('List Vehicle Photos', '/api/v1/vehicles/{{ _.vehicle_id }}/photos'),
      del('Delete Vehicle Photo', '/api/v1/vehicles/{{ _.vehicle_id }}/photos/{{ _.vehicle_photo_id }}'),
      patch('Reorder Vehicle Photos', '/api/v1/vehicles/{{ _.vehicle_id }}/photos/reorder', {
        photoIds: ['{{ _.vehicle_photo_id }}'],
      }),
    ],
  },
  {
    id: 'analytics',
    name: '08 Analytics',
    description: 'KPIs e graficos do dashboard operacional.',
    requests: [
      get('Dashboard KPIs', '/api/v1/analytics/kpis'),
      get('Monthly Sales Chart', '/api/v1/analytics/charts/monthly-sales'),
      get('Sales By Category Chart', '/api/v1/analytics/charts/sales-by-category'),
    ],
  },
  {
    id: 'communication',
    name: '09 Comunicacao - WhatsApp',
    description: 'Historico do lead, envio manual e sessao de bot por lead.',
    requests: [
      get('Get Lead Chat History', '/api/v1/chat/lead/{{ _.lead_id }}'),
      get('Get Lead Bot Session', '/api/v1/chat/lead/{{ _.lead_id }}/bot-session'),
      post('Send WhatsApp Message', '/api/v1/chat/send', {
        phoneNumber: '{{ _.lead_phone }}',
        message: 'Ola! Aqui e da concessionaria. Posso te ajudar com o veiculo?',
        leadId: '{{ _.lead_id }}',
      }),
    ],
  },
  {
    id: 'chatbot',
    name: '10 Chatbot',
    description: 'Sessao, metricas e handoff manual do bot.',
    requests: [
      get('List Bot Sessions', '/api/v1/chatbot/sessions', [param('status', 'ACTIVE'), param('page', '0'), param('size', '20')]),
      get('Get Bot Session Detail', '/api/v1/chatbot/sessions/{{ _.bot_session_id }}'),
      get('Chatbot Metrics', '/api/v1/chatbot/metrics'),
      post('Force Bot Handoff', '/api/v1/chatbot/sessions/{{ _.bot_session_id }}/handoff', null),
    ],
  },
  {
    id: 'saas',
    name: '11 SaaS - Planos Billing Marketplaces',
    description: 'Planos, assinatura, checkout AbacatePay, uso, billing history e quotas de marketplace.',
    requests: [
      get('List Public Plans', '/api/v1/plans', [], noAuth),
      get('Get Current Subscription', '/api/v1/saas/subscription'),
      patch('Change Plan Directly (legacy/internal)', '/api/v1/saas/subscription/plan/{{ _.plan_id }}', null),
      post('Create AbacatePay Checkout', '/api/v1/saas/subscription/checkout/{{ _.plan_id }}', {
        returnUrl: 'http://localhost:3001/app/configuracoes?tab=assinatura',
        completionUrl: 'http://localhost:3001/app/configuracoes?tab=assinatura&checkout=success',
      }),
      post('Cancel AbacatePay Subscription', '/api/v1/saas/subscription/cancel', null),
      get('Get Usage Summary', '/api/v1/saas/usage'),
      get('Get Billing History', '/api/v1/saas/billing-history'),
      get('List Marketplace Configs', '/api/v1/saas/marketplaces'),
      post('Add Marketplace Config', '/api/v1/saas/marketplaces', { marketplaceName: 'Webmotors', monthlyLimit: 500 }),
      patch('Update Marketplace Config', '/api/v1/saas/marketplaces/{{ _.marketplace_config_id }}', { enabled: true, monthlyLimit: 750 }),
      del('Delete Marketplace Config', '/api/v1/saas/marketplaces/{{ _.marketplace_config_id }}'),
    ],
  },
  {
    id: 'referrals',
    name: '12 Indicacoes',
    description: 'Programa de indicacao para concessionaria e visao Astro Admin.',
    requests: [
      get('My Referral Code', '/api/v1/referral/my-code'),
      get('My Referrals', '/api/v1/referral/my-referrals'),
      get('My Referral Balance', '/api/v1/referral/my-balance'),
      get('Legacy My Referral Code', '/api/referral/my-code'),
      get('Legacy My Referrals', '/api/referral/my-referrals'),
      get('Legacy My Referral Balance', '/api/referral/my-balance'),
      get('Admin Referral Summary', '/api/admin/referral/summary'),
    ],
  },
  {
    id: 'admin',
    name: '13 Astro Admin - Financeiro Observability Sistema',
    description: 'Dashboards administrativos, erros, webhooks, infraestrutura e saude.',
    requests: [
      get('Admin Finance Dashboard', '/api/v1/admin/dashboard'),
      get('Observability Overview', '/api/v1/admin/observability/overview'),
      get('Performance Metrics', '/api/v1/admin/observability/performance'),
      get('Search Application Errors', '/api/v1/admin/observability/errors', [
        param('level', 'ERROR'),
        param('from', ''),
        param('to', ''),
        param('service', ''),
        param('correlationId', ''),
        param('page', '0'),
        param('size', '20'),
      ]),
      get('Get Application Error', '/api/v1/admin/observability/errors/{{ _.error_id }}'),
      get('Search Webhook Events', '/api/v1/admin/observability/webhooks', [
        param('source', 'META'),
        param('outcome', ''),
        param('from', ''),
        param('to', ''),
        param('page', '0'),
        param('size', '20'),
      ]),
      get('Webhook Summary', '/api/v1/admin/observability/webhooks/summary', [param('from', ''), param('to', '')]),
      get('Chatbot Insights', '/api/v1/admin/observability/chatbot-insights'),
      get('Infrastructure', '/api/v1/admin/observability/infrastructure'),
      get('System Health', '/api/v1/admin/system/health'),
    ],
  },
  {
    id: 'integrations',
    name: '14 Webhooks e Marketplaces',
    description: 'Meta, AbacatePay, Webmotors, Mercado Livre, scraper/Napista e OAuth Mercado Livre.',
    requests: [
      get('Meta Verify Webhook', '/api/v1/webhooks/meta', [
        param('hub.mode', 'subscribe'),
        param('hub.verify_token', '{{ _.meta_verify_token }}'),
        param('hub.challenge', 'challenge-from-meta'),
      ], noAuth),
      post('Meta Receive Webhook', '/api/v1/webhooks/meta', {
        object: 'whatsapp_business_account',
        entry: [
          {
            id: 'waba-id',
            changes: [
              {
                field: 'messages',
                value: {
                  messaging_product: 'whatsapp',
                  metadata: { display_phone_number: '5511999999999', phone_number_id: '123456789012345' },
                  contacts: [{ profile: { name: 'Cliente WhatsApp' }, wa_id: '5511999999999' }],
                  messages: [{ from: '5511999999999', id: 'wamid.demo', timestamp: '1710000000', type: 'text', text: { body: 'Ola, tenho interesse.' } }],
                },
              },
            ],
          },
        ],
      }, noAuth),
      post('AbacatePay Webhook', '/api/v1/webhooks/abacatepay', {
        id: 'evt_demo_001',
        event: 'subscription.completed',
        apiVersion: 2,
        devMode: true,
        data: {
          subscription: { id: 'sub_demo', amount: 19900, status: 'ACTIVE', frequency: 'MONTHLY', createdAt: '2026-04-29T12:00:00Z', updatedAt: '2026-04-29T12:00:00Z' },
          payment: { id: 'pay_demo', externalId: 'leadflow-subscription:00000000-0000-0000-0000-000000000000:plan:00000000-0000-0000-0000-000000000000', amount: 19900, paidAmount: 19900, status: 'PAID', receiptUrl: 'https://example.com/receipt', createdAt: '2026-04-29T12:00:00Z', updatedAt: '2026-04-29T12:00:00Z' },
          checkout: { id: 'chk_demo', externalId: 'leadflow-subscription:00000000-0000-0000-0000-000000000000:plan:00000000-0000-0000-0000-000000000000', amount: 19900, status: 'PAID' },
          customer: { id: 'cus_demo', name: 'Cliente Demo', email: 'cliente@example.com', taxId: '12345678901' },
        },
      }, noAuth, [param('webhookSecret', '{{ _.abacatepay_webhook_secret }}')]),
      get('Mercado Livre Connect URL', '/api/v1/auth/mercadolivre/connect', [param('organizationId', '{{ _.organization_id }}')]),
      get('Mercado Livre OAuth Callback', '/api/v1/auth/mercadolivre/callback', [param('code', 'auth-code-from-ml'), param('state', '{{ _.organization_id }}:nonce')], noAuth),
      post('Webhook Webmotors', '/api/v1/webhooks/webmotors', {
        IdLead: 'wm-lead-001',
        CodigoCliente: 'cliente-123',
        Cnpj: '12345678000190',
        IdTipoLead: 1,
      }, noAuth, [], [{ name: 'X-Webhook-Token', value: '{{ _.webhook_token }}' }]),
      post('Webhook Mercado Livre', '/api/v1/webhooks/mercadolivre', {
        _id: 'ml-notification-001',
        resource: '/vis/leads/ml-lead-001',
        user_id: 123456789,
        topic: 'vis_leads',
        application_id: 987654321,
        attempts: 1,
        sent: '2026-04-29T12:00:00.000Z',
        received: '2026-04-29T12:00:01.000Z',
      }, noAuth, [], [{ name: 'X-Webhook-Token', value: '{{ _.webhook_token }}' }]),
      post('Webhook Scraper / Napista', '/api/v1/webhooks/scraper', {
        organizationSlug: 'astro-demo-motors',
        phoneNumber: '{{ _.lead_phone }}',
        name: 'Lead Napista',
        email: 'lead.napista@example.com',
        marketplace: 'NAPISTA',
        vehicleInterest: 'Jeep Compass 2021',
        vehicleUrl: 'https://napista.example/anuncio/123',
        rawMessage: 'Tenho interesse no veiculo.',
      }, noAuth, [], [{ name: 'X-Api-Key', value: '{{ _.x_api_key }}' }]),
    ],
  },
];

const resources = [
  {
    _id: workspaceId,
    parentId: null,
    modified: now,
    created: now,
    name: 'Astro LeadFlow API',
    description: 'Colecao completa de endpoints do backend Astro LeadFlow, organizada por modulo.',
    scope: 'collection',
    _type: 'workspace',
  },
  {
    _id: envId,
    parentId: workspaceId,
    modified: now,
    created: now,
    name: 'Base Environment',
    data: envData,
    dataPropertyOrder: { '&': Object.keys(envData) },
    color: null,
    isPrivate: false,
    metaSortKey: 0,
    _type: 'environment',
  },
];

groups.forEach((group, groupIndex) => {
  const groupId = `fld_${group.id}`;
  resources.push({
    _id: groupId,
    parentId: workspaceId,
    modified: now,
    created: now,
    name: group.name,
    description: group.description,
    environment: {},
    environmentPropertyOrder: null,
    metaSortKey: groupIndex,
    _type: 'request_group',
  });

  group.requests.forEach((request, requestIndex) => {
    resources.push({
      ...request,
      parentId: groupId,
      modified: now,
      created: now,
      metaSortKey: requestIndex,
    });
  });
});

const exportPayload = {
  _type: 'export',
  __export_format: 4,
  __export_date: new Date(now).toISOString(),
  __export_source: 'Astro LeadFlow generated Insomnia collection',
  resources,
};

mkdirSync(dirname(output), { recursive: true });
writeFileSync(output, `${JSON.stringify(exportPayload, null, 2)}\n`);

function request(method, name, path, body = null, authentication = bearerAuth, parameters = [], extraHeaders = []) {
  const normalizedParams = Array.isArray(body) && method === 'GET' ? body : parameters;
  const normalizedBody = Array.isArray(body) && method === 'GET' ? null : body;
  const headers = normalizedBody && !isMultipartBody(normalizedBody) ? [...commonHeaders, ...extraHeaders] : extraHeaders;

  return {
    _id: `req_${slug(name)}`,
    name,
    description: '',
    url: `{{ _.base_url }}${path}`,
    method,
    body: buildBody(normalizedBody),
    parameters: normalizedParams,
    headers,
    authentication,
    isPrivate: false,
    settingStoreCookies: true,
    settingSendCookies: true,
    settingDisableRenderRequestBody: false,
    settingEncodeUrl: true,
    settingRebuildPath: true,
    settingFollowRedirects: 'global',
    _type: 'request',
  };
}

function get(name, path, parameters = [], authentication = bearerAuth) {
  return request('GET', name, path, null, authentication, parameters);
}

function post(name, path, body, authentication = bearerAuth, parameters = [], extraHeaders = []) {
  return request('POST', name, path, body, authentication, parameters, extraHeaders);
}

function put(name, path, body, authentication = bearerAuth) {
  return request('PUT', name, path, body, authentication);
}

function patch(name, path, body, authentication = bearerAuth) {
  return request('PATCH', name, path, body, authentication);
}

function del(name, path, authentication = bearerAuth) {
  return request('DELETE', name, path, null, authentication);
}

function multipart(name, path, fields, authentication = bearerAuth) {
  return {
    ...request('POST', name, path, { mimeType: 'multipart/form-data', params: fields }, authentication),
    headers: [],
  };
}

function buildBody(body) {
  if (!body) return {};
  if (isMultipartBody(body)) {
    return {
      mimeType: body.mimeType,
      params: body.params.map((field) => ({
        name: field.name,
        value: field.value,
        type: field.type,
        disabled: false,
      })),
    };
  }
  return {
    mimeType: 'application/json',
    text: JSON.stringify(body, null, 2),
  };
}

function isMultipartBody(body) {
  return body?.mimeType === 'multipart/form-data';
}

function param(name, value, disabled = false) {
  return { name, value, disabled };
}

function slug(value) {
  return value
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '');
}

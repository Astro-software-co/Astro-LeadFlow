# V3 Motors - Sistema de Gestão

Sistema de gestão de vendas de veículos com dashboard, kanban de leads, controle de estoque e configurações.

## 🚀 Tecnologias

- **React 19** - Framework frontend
- **TypeScript** - Tipagem estática
- **Vite** - Build tool e dev server
- **Tailwind CSS** - Framework CSS utility-first
- **shadcn/ui** - Componentes UI acessíveis
- **React Router** - Roteamento
- **TanStack Query** - Gerenciamento de estado e cache
- **Recharts** - Gráficos e visualizações
- **Lucide React** - Ícones

## 📁 Estrutura do Projeto

```
frontend/
├── src/
│   ├── components/         # Componentes reutilizáveis
│   │   ├── dashboard/      # Componentes do dashboard
│   │   ├── kanban/         # Componentes do kanban
│   │   ├── layout/         # Layouts e header
│   │   ├── modals/         # Modais
│   │   └── ui/             # Componentes UI (shadcn)
│   ├── contexts/           # Contextos React (Theme, Notifications)
│   ├── data/               # Dados mock para desenvolvimento
│   ├── hooks/              # Hooks customizados
│   ├── lib/                # Utilitários
│   ├── pages/              # Páginas da aplicação
│   ├── types/              # Definições TypeScript
│   ├── App.tsx             # Componente raiz
│   └── main.tsx            # Entry point
├── public/                 # Arquivos estáticos
└── package.json            # Dependências e scripts
```

## 🛠️ Instalação

```bash
# Instalar dependências
npm install

# Iniciar servidor de desenvolvimento
npm run dev

# Build para produção
npm run build

# Preview do build de produção
npm run preview

# Lint do código
npm run lint
```

## 📄 Páginas Disponíveis

- **Dashboard** (`/`) - Visão geral com KPIs, gráficos e tabelas
- **Kanban** (`/kanban`) - Gestão de leads em colunas (Chatbot, Aguardando, Negociação, Concluído)
- **Estoque** (`/estoque`) - Controle de veículos disponíveis
- **Configurações** (`/configuracoes`) - Gerenciamento de usuários e configurações do sistema

## 🎨 Tema

O sistema suporta modo claro e escuro, com tema personalizável através do ThemeContext.

## 🔧 Desenvolvimento

O projeto utiliza:
- **ESLint** para linting
- **SWC** para compilação rápida
- **Path aliases** (`@/`) para importações limpas

## 📝 Licença

Projeto privado - V3 Motors © 2026

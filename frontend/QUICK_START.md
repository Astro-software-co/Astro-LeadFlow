# 🚀 Guia de Início Rápido - V3 Motors

## Pré-requisitos

- Node.js 18+ (atualmente você tem v12, precisa atualizar)
- npm 9+ ou superior

## 📋 Passos para Começar

### 1. Atualizar Node.js (OBRIGATÓRIO)

Seu sistema está usando Node.js v12.22.12, que é incompatível. Siga o guia:

```bash
# Ver instruções detalhadas
cat NODE_UPDATE_GUIDE.md
```

**Recomendado**: Usar NVM (Node Version Manager)

```bash
# Instalar NVM
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
source ~/.bashrc

# Instalar Node 20 (LTS)
nvm install 20
nvm use 20
nvm alias default 20

# Verificar
node --version  # Deve mostrar v20.x.x
```

### 2. Navegar para o Projeto

```bash
cd /home/ruy-de-castro/Desktop/Ruy/v3_motors/frontend
```

### 3. Instalar Dependências

```bash
# Limpar instalação anterior (se existir)
rm -rf node_modules package-lock.json

# Instalar
npm install
```

### 4. Validar Ambiente (Opcional)

```bash
# Executar script de validação
./validate.sh
```

### 5. Iniciar Servidor de Desenvolvimento

```bash
npm run dev
```

Acesse: **http://localhost:8080**

## 📦 Scripts Disponíveis

```bash
npm run dev      # Inicia servidor de desenvolvimento (porta 8080)
npm run build    # Build para produção
npm run preview  # Preview do build de produção
npm run lint     # Verifica erros de lint
```

## 🗺️ Estrutura de Rotas

- `/` - Dashboard com KPIs e gráficos
- `/kanban` - Gestão de leads (drag and drop)
- `/estoque` - Controle de veículos
- `/configuracoes` - Configurações e usuários

## 📁 Principais Arquivos

```
frontend/
├── src/
│   ├── App.tsx              # Rotas e providers principais
│   ├── main.tsx             # Entry point da aplicação
│   ├── pages/               # Páginas (Dashboard, Kanban, etc)
│   ├── components/          # Componentes reutilizáveis
│   ├── contexts/            # Contextos (Theme, Notifications)
│   └── types/               # Tipos TypeScript
├── package.json             # Dependências
├── vite.config.ts           # Configuração Vite
├── tailwind.config.ts       # Configuração Tailwind
└── README.md                # Documentação completa
```

## 🎨 Tema e Customização

O sistema suporta **modo claro e escuro**. O tema é controlado pelo `ThemeContext`.

As cores são definidas em:
- `src/index.css` - Variáveis CSS
- `tailwind.config.ts` - Configuração Tailwind

## 🔧 Desenvolvimento

### Hot Module Replacement (HMR)

O Vite oferece HMR instantâneo. Salve qualquer arquivo `.tsx` e veja as mudanças imediatamente.

### Path Aliases

Use `@/` para importações:

```typescript
// ✅ Bom
import { Button } from '@/components/ui/button'

// ❌ Evite
import { Button } from '../../../components/ui/button'
```

### Adicionar Novos Componentes UI

O projeto usa shadcn/ui. Para adicionar componentes:

```bash
npx shadcn-ui@latest add [component-name]
```

Exemplo:
```bash
npx shadcn-ui@latest add table
```

## 🐛 Troubleshooting

### Erro: "Unexpected token '.'"
**Causa**: Node.js muito antigo  
**Solução**: Atualizar para Node.js 18+

### Porta 8080 já em uso
```bash
# Matar processo na porta 8080
lsof -ti:8080 | xargs kill -9

# Ou alterar porta em vite.config.ts
```

### Dependências faltando
```bash
rm -rf node_modules package-lock.json
npm install
```

### Erro de TypeScript
```bash
# Verificar erros
npx tsc --noEmit
```

## 📚 Documentação Adicional

- `README.md` - Documentação completa do projeto
- `REFACTORING_SUMMARY.md` - Resumo da refatoração
- `NODE_UPDATE_GUIDE.md` - Guia de atualização do Node
- `validate.sh` - Script de validação do ambiente

## 🎯 Próximos Passos

1. ✅ Atualizar Node.js para versão 18+
2. ✅ Instalar dependências (`npm install`)
3. ✅ Rodar servidor (`npm run dev`)
4. 🔄 Conectar com backend (quando disponível)
5. 🔄 Substituir dados mock por API real
6. 🔄 Implementar autenticação

## 💡 Dicas

- Use React DevTools para debug
- Verifique o console do navegador para erros
- Os dados são mock (em `src/data/mockData.ts`)
- Componentes UI estão em `src/components/ui/`

## 📞 Suporte

Em caso de dúvidas:
1. Consulte a documentação nos arquivos `.md`
2. Verifique erros no console
3. Execute `./validate.sh` para diagnóstico

---

**Status do Projeto**: ✅ Pronto para desenvolvimento  
**Última Atualização**: 06/02/2026

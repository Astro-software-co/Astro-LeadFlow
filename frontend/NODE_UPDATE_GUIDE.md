# Guia de Atualização do Node.js

## ⚠️ Problema Identificado

Seu sistema está usando Node.js v12.22.12, que é muito antiga e não suporta as funcionalidades modernas necessárias para este projeto (React 19, Vite 7, etc.).

## ✅ Solução: Atualizar para Node.js 18+

### Opção 1: Usando NVM (Recomendado)

NVM (Node Version Manager) permite gerenciar múltiplas versões do Node.js:

```bash
# Instalar NVM
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash

# Recarregar o shell
source ~/.bashrc
# ou source ~/.zshrc (se usar zsh)

# Instalar Node.js LTS (versão 20)
nvm install 20

# Usar Node.js 20 como padrão
nvm use 20
nvm alias default 20

# Verificar instalação
node --version  # Deve mostrar v20.x.x
npm --version   # Deve mostrar 10.x.x
```

### Opção 2: Instalação Direta (Ubuntu/Debian)

```bash
# Adicionar repositório NodeSource
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -

# Instalar Node.js
sudo apt-get install -y nodejs

# Verificar instalação
node --version
npm --version
```

### Opção 3: Usando snap

```bash
sudo snap install node --classic --channel=20
```

## 🚀 Após Atualizar o Node.js

```bash
# Navegar para a pasta do projeto
cd /home/ruy-de-castro/Desktop/Ruy/v3_motors/frontend

# Limpar instalação anterior
rm -rf node_modules package-lock.json

# Reinstalar dependências
npm install

# Iniciar servidor de desenvolvimento
npm run dev
```

O servidor estará disponível em: http://localhost:8080

## 📝 Verificação Pós-Instalação

Execute os seguintes comandos para garantir que tudo está funcionando:

```bash
node --version    # Deve ser >= 18.0.0
npm --version     # Deve ser >= 9.0.0
```

## 🔗 Links Úteis

- [NVM GitHub](https://github.com/nvm-sh/nvm)
- [Node.js Downloads](https://nodejs.org/)
- [NodeSource Distributions](https://github.com/nodesource/distributions)

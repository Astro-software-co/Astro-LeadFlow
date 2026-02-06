#!/bin/bash

echo "🔍 V3 Motors - Validação do Ambiente"
echo "===================================="
echo ""

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Verificar Node.js
echo "📦 Verificando Node.js..."
NODE_VERSION=$(node --version 2>/dev/null)
if [ $? -eq 0 ]; then
    MAJOR_VERSION=$(echo $NODE_VERSION | cut -d'.' -f1 | sed 's/v//')
    if [ $MAJOR_VERSION -ge 18 ]; then
        echo -e "${GREEN}✓ Node.js: $NODE_VERSION (OK)${NC}"
    else
        echo -e "${RED}✗ Node.js: $NODE_VERSION (Versão muito antiga!)${NC}"
        echo -e "${YELLOW}  → Necessário: Node.js 18 ou superior${NC}"
        echo -e "${YELLOW}  → Consulte NODE_UPDATE_GUIDE.md para atualizar${NC}"
        exit 1
    fi
else
    echo -e "${RED}✗ Node.js não encontrado!${NC}"
    exit 1
fi

# Verificar npm
echo "📦 Verificando npm..."
NPM_VERSION=$(npm --version 2>/dev/null)
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ npm: $NPM_VERSION (OK)${NC}"
else
    echo -e "${RED}✗ npm não encontrado!${NC}"
    exit 1
fi

# Verificar se as dependências estão instaladas
echo ""
echo "📚 Verificando dependências..."
if [ -d "node_modules" ]; then
    echo -e "${GREEN}✓ node_modules encontrado${NC}"
else
    echo -e "${YELLOW}⚠ node_modules não encontrado${NC}"
    echo "  Executando npm install..."
    npm install
fi

# Verificar arquivos essenciais
echo ""
echo "📄 Verificando arquivos essenciais..."
FILES=(
    "package.json"
    "vite.config.ts"
    "tailwind.config.ts"
    "tsconfig.json"
    "src/App.tsx"
    "src/main.tsx"
    "src/index.css"
)

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo -e "${GREEN}✓ $file${NC}"
    else
        echo -e "${RED}✗ $file não encontrado!${NC}"
    fi
done

# Tentar compilar TypeScript
echo ""
echo "🔨 Verificando compilação TypeScript..."
npx tsc --noEmit 2>/dev/null
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Compilação TypeScript OK${NC}"
else
    echo -e "${YELLOW}⚠ Avisos na compilação TypeScript${NC}"
fi

echo ""
echo "===================================="
echo -e "${GREEN}✅ Validação concluída!${NC}"
echo ""
echo "Para iniciar o servidor de desenvolvimento:"
echo "  npm run dev"
echo ""
echo "O servidor estará disponível em:"
echo "  http://localhost:8080"

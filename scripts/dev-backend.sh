#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${1:-$ROOT_DIR/.env.local}"

cd "$ROOT_DIR/backend"

./gradlew --stop || true

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
else
  echo "WARN: $ENV_FILE nao encontrado; usando variaveis ja exportadas."
fi

exec ./gradlew --no-daemon bootRun

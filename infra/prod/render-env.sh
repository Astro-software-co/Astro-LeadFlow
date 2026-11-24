#!/usr/bin/env bash
# Gera /opt/leadflow/.env a partir do SSM Parameter Store (/leadflow/<env>/*). Nunca imprime valores.
set -euo pipefail
export HOME="${HOME:-/root}"
APP_DIR="${APP_DIR:-/opt/leadflow}"
ENVIRONMENT="${LEADFLOW_ENV:-staging}"
PARAM_PATH="/leadflow/${ENVIRONMENT}"

if [[ -z "${AWS_REGION:-}" ]]; then
  TOKEN="$(curl -fsS -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60')"
  AWS_REGION="$(curl -fsS -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/placement/region)"
fi
export AWS_REGION AWS_DEFAULT_REGION="$AWS_REGION"

tmp="$(mktemp)"; trap 'rm -f "$tmp"' EXIT
aws ssm get-parameters-by-path --path "$PARAM_PATH" --recursive --with-decryption --output json > "$tmp"

umask 077
python3 - "$tmp" "$PARAM_PATH" "$APP_DIR/.env.new" <<'PY'
import json, sys
src, prefix, dst = sys.argv[1:4]
params = json.load(open(src))["Parameters"]
if not params:
    sys.exit(f"nenhum parâmetro encontrado em {prefix}")
kv = {}
# 1) Parâmetro opcional ENV_FILE: um único SecureString com linhas CHAVE=valor (útil quando se cria pelo console).
for p in params:
    if p["Name"] == f"{prefix}/ENV_FILE":
        for ln in p["Value"].splitlines():
            ln = ln.strip()
            if not ln or ln.startswith("#") or "=" not in ln:
                continue
            k, v = ln.split("=", 1)
            kv[k.strip()] = v
# 2) Parâmetros individuais (/leadflow/<env>/CHAVE) têm precedência sobre o ENV_FILE.
for p in params:
    key = p["Name"][len(prefix):].lstrip("/")
    if key == "ENV_FILE":
        continue
    kv[key] = p["Value"]
lines = []
for key in sorted(kv):
    val = kv[key]
    if "\n" in val or "\r" in val:
        sys.exit(f"valor de {key} contém quebra de linha")
    lines.append(f"{key}={val.replace('$', '$$')}")   # $ literal no Compose
open(dst, "w").write("\n".join(lines) + "\n")
print(f".env gerado com {len(lines)} variáveis (valores não exibidos)")
PY
mv "$APP_DIR/.env.new" "$APP_DIR/.env"
chmod 600 "$APP_DIR/.env"

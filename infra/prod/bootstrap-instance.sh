#!/bin/bash
# User-data da EC2 (roda uma vez, como root, no primeiro boot). Amazon Linux 2023.
# Instala Docker + Compose, configura rotação de logs, swap e o timer de backup do Postgres.
set -euxo pipefail
exec > >(tee /var/log/leadflow-bootstrap.log) 2>&1

dnf -y install docker jq python3

# Rotação de logs do Docker: sem isso os logs dos containers enchem o disco.
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'JSON'
{ "log-driver": "json-file", "log-opts": { "max-size": "20m", "max-file": "5" } }
JSON
systemctl enable --now docker

# Plugin do Compose (versão fixada).
COMPOSE_VERSION="v2.29.7"
mkdir -p /usr/local/lib/docker/cli-plugins
curl -fsSL "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-$(uname -m)" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
docker compose version

# Swap de 2 GB como rede de segurança para a JVM.
if [ ! -f /swapfile ]; then
  dd if=/dev/zero of=/swapfile bs=1M count=2048
  chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

mkdir -p /opt/leadflow && chmod 750 /opt/leadflow

# Backup diário do Postgres (03:00 em Brasília = 06:00 UTC). Só roda depois do 1º deploy.
cat > /etc/systemd/system/leadflow-backup.service <<'UNIT'
[Unit]
Description=Backup do Postgres do LeadFlow para o S3
ConditionPathExists=/opt/leadflow/backup-postgres.sh
[Service]
Type=oneshot
ExecStart=/opt/leadflow/backup-postgres.sh
UNIT
cat > /etc/systemd/system/leadflow-backup.timer <<'UNIT'
[Unit]
Description=Agenda o backup diário do LeadFlow
[Timer]
OnCalendar=*-*-* 06:00:00 UTC
Persistent=true
[Install]
WantedBy=timers.target
UNIT
systemctl daemon-reload
systemctl enable --now leadflow-backup.timer

touch /opt/leadflow/.bootstrapped

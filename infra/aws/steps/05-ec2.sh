#!/usr/bin/env bash
# Security group + EC2 (Amazon Linux 2023, sem SSH: acesso por SSM) + Elastic IP. Idempotente.
source "$(dirname "$0")/../lib.sh"
BOOTSTRAP="$REPO_ROOT/infra/prod/bootstrap-instance.sh"
[[ -f "$BOOTSTRAP" ]] || die "não encontrei $BOOTSTRAP"

VPC_ID="$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)"
[[ "$VPC_ID" != "None" && -n "$VPC_ID" ]] || die "não há VPC padrão em $AWS_REGION; crie uma (aws ec2 create-default-vpc)"
SUBNET_ID="$(aws ec2 describe-subnets --filters Name=vpc-id,Values="$VPC_ID" Name=default-for-az,Values=true \
  --query 'Subnets[0].SubnetId' --output text)"

# --- Security group: só 80/443; sem SSH ---
SG_ID="$(aws ec2 describe-security-groups --filters Name=group-name,Values="$NAME" Name=vpc-id,Values="$VPC_ID" \
  --query 'SecurityGroups[0].GroupId' --output text)"
if [[ "$SG_ID" == "None" || -z "$SG_ID" ]]; then
  log "criando security group $NAME"
  SG_ID="$(w aws ec2 create-security-group --group-name "$NAME" --description "LeadFlow ${ENVNAME}: HTTP/HTTPS" \
    --vpc-id "$VPC_ID" --tag-specifications "ResourceType=security-group,Tags=[{Key=Project,Value=$PROJECT},{Key=Environment,Value=$ENVNAME}]" \
    --query GroupId --output text)"
  for port in 80 443; do
    w aws ec2 authorize-security-group-ingress --group-id "$SG_ID" --protocol tcp --port "$port" --cidr 0.0.0.0/0 >/dev/null
  done
else
  log "security group $SG_ID já existe"
fi

# --- Instância ---
INSTANCE_ID="$(aws ec2 describe-instances \
  --filters Name=tag:Project,Values="$PROJECT" Name=tag:Environment,Values="$ENVNAME" Name=instance-state-name,Values=pending,running,stopped \
  --query 'Reservations[].Instances[].InstanceId | [0]' --output text)"
if [[ "$INSTANCE_ID" == "None" || -z "$INSTANCE_ID" ]]; then
  AMI_ID="$(aws ssm get-parameter --name /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 \
    --query Parameter.Value --output text)"
  log "lançando $INSTANCE_TYPE com a AMI $AMI_ID"
  # HttpPutResponseHopLimit=2: sem isso os containers não alcançam o IMDS (credenciais do instance profile).
  INSTANCE_ID="$(w aws ec2 run-instances --image-id "$AMI_ID" --instance-type "$INSTANCE_TYPE" \
    --iam-instance-profile Name="${NAME}-instance" --security-group-ids "$SG_ID" --subnet-id "$SUBNET_ID" \
    --associate-public-ip-address \
    --metadata-options HttpTokens=required,HttpEndpoint=enabled,HttpPutResponseHopLimit=2 \
    --block-device-mappings "DeviceName=/dev/xvda,Ebs={VolumeSize=${ROOT_VOLUME_GB},VolumeType=gp3,Encrypted=true,DeleteOnTermination=true}" \
    --user-data "fileb://${BOOTSTRAP}" \
    --tag-specifications \
      "ResourceType=instance,Tags=[{Key=Name,Value=$NAME},{Key=Project,Value=$PROJECT},{Key=Environment,Value=$ENVNAME}]" \
      "ResourceType=volume,Tags=[{Key=Project,Value=$PROJECT},{Key=Environment,Value=$ENVNAME}]" \
    --query 'Instances[0].InstanceId' --output text)"
else
  log "instância $INSTANCE_ID já existe"
fi
[[ "$DRY_RUN" == "1" ]] && { log "dry-run: parando aqui"; exit 0; }
log "aguardando a instância ficar em execução"; aws ec2 wait instance-running --instance-ids "$INSTANCE_ID"

# --- Elastic IP ---
ALLOC_ID="$(aws ec2 describe-addresses --filters Name=tag:Project,Values="$PROJECT" Name=tag:Environment,Values="$ENVNAME" \
  --query 'Addresses[0].AllocationId' --output text)"
if [[ "$ALLOC_ID" == "None" || -z "$ALLOC_ID" ]]; then
  log "alocando Elastic IP"
  ALLOC_ID="$(aws ec2 allocate-address --domain vpc \
    --tag-specifications "ResourceType=elastic-ip,Tags=[{Key=Project,Value=$PROJECT},{Key=Environment,Value=$ENVNAME}]" \
    --query AllocationId --output text)"
fi
aws ec2 associate-address --instance-id "$INSTANCE_ID" --allocation-id "$ALLOC_ID" --allow-reassociation >/dev/null
PUBLIC_IP="$(aws ec2 describe-addresses --allocation-ids "$ALLOC_ID" --query 'Addresses[0].PublicIp' --output text)"

state_set INSTANCE_ID "$INSTANCE_ID"; state_set PUBLIC_IP "$PUBLIC_IP"; state_set SG_ID "$SG_ID"
state_set SSLIP_DOMAIN "${PUBLIC_IP//./-}.sslip.io"
log "instância $INSTANCE_ID  IP $PUBLIC_IP  (domínio temporário: ${PUBLIC_IP//./-}.sslip.io)"

# --- Espera o agente SSM registrar e o bootstrap terminar ---
log "aguardando o agente SSM (até 5 min)"
for _ in $(seq 1 30); do
  ping="$(aws ssm describe-instance-information --filters Key=InstanceIds,Values="$INSTANCE_ID" \
    --query 'InstanceInformationList[0].PingStatus' --output text 2>/dev/null || true)"
  [[ "$ping" == "Online" ]] && break; sleep 10
done
[[ "${ping:-}" == "Online" ]] || warn "agente SSM ainda não está Online; verifique o instance profile e tente de novo"

log "verificando o bootstrap (Docker/Compose instalados)"
CMD_ID="$(aws ssm send-command --instance-ids "$INSTANCE_ID" --document-name AWS-RunShellScript \
  --parameters 'commands=["for i in $(seq 1 60); do [ -f /opt/leadflow/.bootstrapped ] && break; sleep 5; done; test -f /opt/leadflow/.bootstrapped && docker --version && docker compose version"]' \
  --query Command.CommandId --output text)"
for _ in $(seq 1 60); do
  st="$(aws ssm get-command-invocation --command-id "$CMD_ID" --instance-id "$INSTANCE_ID" --query Status --output text 2>/dev/null || echo Pending)"
  [[ "$st" =~ ^(Success|Failed|Cancelled|TimedOut)$ ]] && break; sleep 10
done
[[ "$st" == "Success" ]] && log "bootstrap OK" || warn "bootstrap não confirmado (status: $st). Veja /var/log/leadflow-bootstrap.log na instância (aws ssm start-session)."

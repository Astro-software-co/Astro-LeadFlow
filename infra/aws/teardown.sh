#!/usr/bin/env bash
# Remove a EC2, o Elastic IP e o security group do ambiente (para economizar). Mantém ECR, bucket, IAM e parâmetros.
#   CONFIRM=sim ./infra/aws/teardown.sh
# ATENÇÃO: o banco vive no disco da EC2. Faça/valide um backup (s3://<bucket>/backups/) antes.
source "$(dirname "$0")/lib.sh"
[[ "${CONFIRM:-}" == "sim" ]] || die "operação destrutiva: rode com CONFIRM=sim"
IID="$(state_get INSTANCE_ID)"; [[ -n "$IID" ]] || die "sem INSTANCE_ID no estado"
log "encerrando $IID"; w aws ec2 terminate-instances --instance-ids "$IID" >/dev/null
[[ "${DRY_RUN:-0}" == "1" ]] || aws ec2 wait instance-terminated --instance-ids "$IID"
ALLOC="$(aws ec2 describe-addresses --filters Name=tag:Project,Values="$PROJECT" Name=tag:Environment,Values="$ENVNAME" --query 'Addresses[0].AllocationId' --output text)"
[[ "$ALLOC" != "None" ]] && { log "liberando Elastic IP"; w aws ec2 release-address --allocation-id "$ALLOC"; }
SG="$(state_get SG_ID)"; [[ -n "$SG" ]] && { log "removendo security group"; w aws ec2 delete-security-group --group-id "$SG" || warn "SG ainda em uso; tente de novo em instantes"; }
log "feito"

#!/usr/bin/env bash
# Remove todos os recursos do Valence criados por infra-criar.sh.
set -euo pipefail

RG="${RG:-rg-valence}"

az group delete --name "$RG" --yes --no-wait
echo "Remoção do grupo $RG solicitada."

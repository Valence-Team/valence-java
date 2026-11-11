#!/usr/bin/env bash
# Provisionamento do Valence no Azure com Azure CLI: Resource Group, Azure SQL e App Service (Java 21).
# Uso (Azure Cloud Shell ou Git Bash com az login feito):
#   export SQL_ADMIN_PASSWORD='...'   # senha forte do administrador do Azure SQL
#   export LLM_API_KEY='...'          # opcional: chave da Groq
#   export RABBITMQ_URI='amqps://...' # opcional: URI do CloudAMQP
#   bash scripts/infra-criar.sh
set -euo pipefail

: "${SQL_ADMIN_PASSWORD:?Defina SQL_ADMIN_PASSWORD antes de executar.}"

# Parâmetros
RG="${RG:-rg-valence}"
LOCATION="${LOCATION:-brazilsouth}"
SQL_SERVER="${SQL_SERVER:-sql-valence-rm563620}"
SQL_DB="${SQL_DB:-valence}"
SQL_ADMIN="${SQL_ADMIN:-valenceadmin}"
PLAN="${PLAN:-plan-valence}"
WEBAPP="${WEBAPP:-app-valence-api-rm563620}"
JWT_SECRET="${JWT_SECRET:-$(openssl rand -base64 48)}"
LLM_MODEL="${LLM_MODEL:-openai/gpt-oss-120b}"
IA_SERVICE_URL="${IA_SERVICE_URL:-http://localhost:8000}"

# Grupo de recursos
az group create --name "$RG" --location "$LOCATION" --output table

# Banco Azure SQL (PaaS)
az sql server create --name "$SQL_SERVER" --resource-group "$RG" --location "$LOCATION" \
  --admin-user "$SQL_ADMIN" --admin-password "$SQL_ADMIN_PASSWORD" --output table
az sql server firewall-rule create --resource-group "$RG" --server "$SQL_SERVER" \
  --name PermitirServicosAzure --start-ip-address 0.0.0.0 --end-ip-address 0.0.0.0 --output table
az sql db create --resource-group "$RG" --server "$SQL_SERVER" --name "$SQL_DB" \
  --service-objective Basic --backup-storage-redundancy Local --output table

# Plano e Web App Linux com Java 21
az appservice plan create --name "$PLAN" --resource-group "$RG" --location "$LOCATION" \
  --is-linux --sku B1 --output table
az webapp create --name "$WEBAPP" --resource-group "$RG" --plan "$PLAN" \
  --runtime "JAVA:21-java21" --output table
az webapp config set --name "$WEBAPP" --resource-group "$RG" --always-on true \
  --generic-configurations '{"healthCheckPath": "/actuator/health/liveness"}' --output table

# Variáveis de ambiente da API (App Settings)
DB_URL="jdbc:sqlserver://${SQL_SERVER}.database.windows.net:1433;database=${SQL_DB};encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;"
az webapp config appsettings set --name "$WEBAPP" --resource-group "$RG" --output none --settings \
  SPRING_PROFILES_ACTIVE=prod \
  SERVER_PORT=80 \
  DB_URL="$DB_URL" \
  DB_USERNAME="$SQL_ADMIN" \
  DB_PASSWORD="$SQL_ADMIN_PASSWORD" \
  JWT_SECRET="$JWT_SECRET" \
  JWT_EXPIRATION_MINUTES=120 \
  LLM_BASE_URL=https://api.groq.com/openai \
  LLM_MODEL="$LLM_MODEL" \
  IA_SERVICE_URL="$IA_SERVICE_URL" \
  IA_TIMEOUT_SECONDS=10 \
  IA_CONFIANCA_MINIMA=0.6 \
  ALERTA_RAIO_KM=10 \
  FOTOS_DIR=/home/data/fotos

if [ -n "${LLM_API_KEY:-}" ]; then
  az webapp config appsettings set --name "$WEBAPP" --resource-group "$RG" --output none \
    --settings LLM_API_KEY="$LLM_API_KEY"
fi
if [ -n "${RABBITMQ_URI:-}" ]; then
  az webapp config appsettings set --name "$WEBAPP" --resource-group "$RG" --output none \
    --settings RABBITMQ_URI="$RABBITMQ_URI"
fi
if [ -n "${IA_API_KEY:-}" ]; then
  az webapp config appsettings set --name "$WEBAPP" --resource-group "$RG" --output none \
    --settings IA_API_KEY="$IA_API_KEY"
fi

# DDL do banco (opcional: exige sqlcmd; sem ele, a API cria as tabelas ao subir)
if command -v sqlcmd >/dev/null 2>&1; then
  MEU_IP="$(curl -s https://api.ipify.org)"
  az sql server firewall-rule create --resource-group "$RG" --server "$SQL_SERVER" \
    --name MaquinaDoScript --start-ip-address "$MEU_IP" --end-ip-address "$MEU_IP" --output none
  sqlcmd -S "${SQL_SERVER}.database.windows.net" -d "$SQL_DB" -U "$SQL_ADMIN" -P "$SQL_ADMIN_PASSWORD" \
    -i "$(dirname "$0")/script-bd.sql"
fi

echo "API: https://${WEBAPP}.azurewebsites.net/swagger-ui.html"

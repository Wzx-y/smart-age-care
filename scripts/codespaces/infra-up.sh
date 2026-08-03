#!/usr/bin/env bash
set -euo pipefail

required=(
  MYSQL_ROOT_PASSWORD CARE_DB_USERNAME CARE_DB_PASSWORD RUOYI_DB_USERNAME
  RUOYI_DB_PASSWORD NACOS_DB_USERNAME NACOS_DB_PASSWORD NACOS_AUTH_TOKEN_SECRET
  NACOS_SERVER_IDENTITY_KEY NACOS_SERVER_IDENTITY_VALUE NACOS_PASSWORD
)

for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required Codespaces secret: ${name}" >&2
    exit 1
  fi
done

docker compose -f infra/codespaces/compose.yaml up -d --wait
docker compose -f infra/codespaces/compose.yaml ps

mkdir -p .codespaces
if [[ ! -f .codespaces/nacos-admin-initialized ]]; then
  curl --fail --silent --show-error -X POST http://127.0.0.1:8848/nacos/v3/auth/user/admin \
    --data-urlencode "password=${NACOS_PASSWORD}" >/dev/null
  touch .codespaces/nacos-admin-initialized
  echo "Initialized the Nacos administrator from NACOS_PASSWORD."
fi

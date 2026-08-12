#!/usr/bin/env bash
set -euo pipefail

workspace_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
env_file="$workspace_root/.env.codespaces"
compose=(docker compose --env-file "$env_file" -f "$workspace_root/docker-compose.codespaces.yml")

fail() {
  printf 'Codespaces stack: %s\n' "$*" >&2
  exit 1
}

[[ -f "$env_file" ]] || fail "create .env.codespaces from .env.codespaces.example and provide the required secrets"
command -v docker >/dev/null 2>&1 || fail "Docker is unavailable. Rebuild this Codespace after pulling the updated devcontainer configuration."
docker compose version >/dev/null 2>&1 || fail "Docker Compose is unavailable in this Codespace"

set -a
# shellcheck disable=SC1090
source "$env_file"
set +a

required=(MYSQL_ROOT_PASSWORD CARE_DB_PASSWORD NACOS_USERNAME NACOS_PASSWORD NACOS_AUTH_TOKEN_SECRET NACOS_SERVER_IDENTITY_KEY NACOS_SERVER_IDENTITY_VALUE RUOYI_JWT_SECRET PLATFORM_INTERNAL_AUTH_KEY)
for variable in "${required[@]}"; do
  [[ -n "${!variable:-}" ]] || fail "$variable is missing from .env.codespaces"
done

for variable in MYSQL_ROOT_PASSWORD CARE_DB_PASSWORD; do
  [[ "${!variable}" =~ ^[A-Za-z0-9._~!@#%+=:-]{16,}$ ]] || fail "$variable must be at least 16 characters and use only letters, digits, . _ ~ ! @ # % + = : -"
done

wait_for_mysql() {
  for _ in {1..60}; do
    # mysqladmin ping succeeds even while MySQL is still applying its initial root password.
    if "${compose[@]}" exec -T mysql mysql -N -s -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SELECT 1" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  fail "MySQL did not become ready; inspect: docker compose -f docker-compose.codespaces.yml logs mysql"
}

seed_databases() {
  local seed_complete
  seed_complete="$("${compose[@]}" exec -T mysql mysql -N -s -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'smart_age_care' AND TABLE_NAME = 'care_tenant_directory'" 2>/dev/null || true)"
  if [[ "$seed_complete" == "1" ]]; then
    return
  fi

  printf 'Initializing isolated Codespaces databases from repository SQL...\n'
  "${compose[@]}" exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < "$workspace_root/backend/ruoyi-cloud/sql/ry_config_20260611.sql"
  "${compose[@]}" exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS \`ry-cloud\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE DATABASE IF NOT EXISTS smart_age_care CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  "${compose[@]}" exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" ry-cloud < "$workspace_root/backend/ruoyi-cloud/sql/ry_20260417.sql"
  "${compose[@]}" exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" smart_age_care < "$workspace_root/backend/ruoyi-cloud/sql/smart-age-care/001_tenant_member_directory.sql"
  "${compose[@]}" exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" smart_age_care < "$workspace_root/backend/ruoyi-cloud/sql/smart-age-care/003_tenant_directory.sql"
  "${compose[@]}" exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "CREATE USER IF NOT EXISTS 'care_service'@'%' IDENTIFIED BY '$CARE_DB_PASSWORD'; ALTER USER 'care_service'@'%' IDENTIFIED BY '$CARE_DB_PASSWORD'; GRANT ALL PRIVILEGES ON smart_age_care.* TO 'care_service'@'%'; FLUSH PRIVILEGES; UPDATE \`ry-config\`.config_info SET content = REPLACE(REPLACE(REPLACE(content, 'host: localhost', 'host: redis'), 'jdbc:mysql://localhost', 'jdbc:mysql://mysql'), 'password: password', 'password: $MYSQL_ROOT_PASSWORD') WHERE data_id IN ('ruoyi-gateway-dev.yml', 'ruoyi-auth-dev.yml', 'ruoyi-system-dev.yml');"
}

sync_gateway_routes() {
  "${compose[@]}" exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" ry-config < "$workspace_root/backend/ruoyi-cloud/sql/smart-age-care/002_gateway_care_route.sql"
}

disable_gateway_captcha() {
  "${compose[@]}" exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "UPDATE \`ry-config\`.config_info SET content = REPLACE(content, CONCAT('captcha:', CHAR(10), '    enabled: true'), CONCAT('captcha:', CHAR(10), '    enabled: false')) WHERE data_id = 'ruoyi-gateway-dev.yml' AND content LIKE '%captcha:%';"
}

stage_jar() {
  local source="$1"
  local target="$2"
  [[ -f "$source" ]] || fail "expected build artifact is missing: $source"
  cp "$source" "$target"
}

cd "$workspace_root"
"${compose[@]}" up -d mysql redis
wait_for_mysql
seed_databases
sync_gateway_routes
disable_gateway_captcha
"${compose[@]}" up -d --force-recreate nacos

printf 'Building cloud services in Codespaces...\n'
mvn --batch-mode --file backend/ruoyi-cloud/pom.xml -pl ruoyi-gateway,ruoyi-auth,ruoyi-modules/ruoyi-system -am package -DskipTests
mvn --batch-mode --file backend/pom.xml -pl care-service -am package -DskipTests

mkdir -p .codespaces/runtime
stage_jar backend/ruoyi-cloud/ruoyi-gateway/target/ruoyi-gateway.jar .codespaces/runtime/ruoyi-gateway.jar
stage_jar backend/ruoyi-cloud/ruoyi-auth/target/ruoyi-auth.jar .codespaces/runtime/ruoyi-auth.jar
stage_jar backend/ruoyi-cloud/ruoyi-modules/ruoyi-system/target/ruoyi-modules-system.jar .codespaces/runtime/ruoyi-system.jar
care_jar="$(find backend/care-service/target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' -print -quit)"
stage_jar "$care_jar" .codespaces/runtime/care-service.jar

"${compose[@]}" up -d --force-recreate ruoyi-auth ruoyi-system ruoyi-gateway care-service frontend
printf '\nStack started. In the Codespaces Ports panel, open port 5173.\n'
printf 'Follow startup logs with: docker compose --env-file .env.codespaces -f docker-compose.codespaces.yml logs -f\n'

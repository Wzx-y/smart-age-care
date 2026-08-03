#!/usr/bin/env bash
set -euo pipefail

mysql_root() {
  mysql --protocol=socket -uroot "-p${MYSQL_ROOT_PASSWORD}" "$@"
}

mysql_root <<SQL
CREATE DATABASE IF NOT EXISTS smart_age_care CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${CARE_DB_USERNAME}'@'%' IDENTIFIED BY '${CARE_DB_PASSWORD}';
CREATE USER IF NOT EXISTS '${RUOYI_DB_USERNAME}'@'%' IDENTIFIED BY '${RUOYI_DB_PASSWORD}';
CREATE USER IF NOT EXISTS '${NACOS_DB_USERNAME}'@'%' IDENTIFIED BY '${NACOS_DB_PASSWORD}';
GRANT ALL PRIVILEGES ON smart_age_care.* TO '${CARE_DB_USERNAME}'@'%';
GRANT ALL PRIVILEGES ON \`ry-cloud\`.* TO '${RUOYI_DB_USERNAME}'@'%';
GRANT ALL PRIVILEGES ON \`ry-config\`.* TO '${NACOS_DB_USERNAME}'@'%';
FLUSH PRIVILEGES;
SQL

mysql_root --database=ry-cloud < /seed/ruoyi/ry_20260417.sql
mysql_root --database=ry-cloud < /seed/ruoyi/smart-age-care/001_tenant_member_directory.sql
mysql_root --database=ry-cloud < /seed/ruoyi/smart-age-care/003_tenant_directory.sql

config_sql="$(mktemp)"
sed \
  -e '/INSERT INTO users (username, password, enabled)/d' \
  -e '/INSERT INTO roles (username, role)/d' \
  -e "s|username: root|username: ${RUOYI_DB_USERNAME}|g" \
  -e "s|password: password|password: ${RUOYI_DB_PASSWORD}|g" \
  -e 's|useSSL=true|useSSL=false|g' \
  /seed/ruoyi/ry_config_20260611.sql > "$config_sql"
mysql_root < "$config_sql"
rm -f "$config_sql"
mysql_root --database=ry-config < /seed/ruoyi/smart-age-care/002_gateway_care_route.sql

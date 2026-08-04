#!/usr/bin/env bash
set -euo pipefail

check() {
  local name="$1"
  local url="$2"
  curl --fail --silent --show-error "$url" >/dev/null
  echo "Healthy: ${name}"
}

docker compose -f infra/codespaces/compose.yaml exec -T ruoyi-nacos \
  curl --fail --silent --show-error http://localhost:8080/v3/console/health/readiness >/dev/null
echo "Healthy: nacos"
check auth http://127.0.0.1:9200/actuator/health
check system http://127.0.0.1:9201/actuator/health
check gateway http://127.0.0.1:8080/actuator/health
check care http://127.0.0.1:8081/actuator/health

npm run lint
npm run typecheck
npm run test
npm run build
mvn --batch-mode --file backend/pom.xml verify
mvn --batch-mode --file backend/ruoyi-cloud/pom.xml verify

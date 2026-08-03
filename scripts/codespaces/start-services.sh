#!/usr/bin/env bash
set -euo pipefail

mkdir -p .codespaces/logs
export NACOS_SERVER_ADDR="${NACOS_SERVER_ADDR:-127.0.0.1:8848}"
export RUOYI_MEMBER_DIRECTORY_URL="${RUOYI_MEMBER_DIRECTORY_URL:-http://127.0.0.1:9201}"

required=(
  CARE_DB_URL CARE_DB_USERNAME CARE_DB_PASSWORD NACOS_PASSWORD
  PLATFORM_INTERNAL_AUTH_KEY RUOYI_JWT_SECRET
)

for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required Codespaces secret: ${name}" >&2
    exit 1
  fi
done

launch() {
  local name="$1"
  local command="$2"
  nohup bash -lc "$command" > ".codespaces/logs/${name}.log" 2>&1 &
  echo "$name started with PID $!"
}

launch system "mvn --file backend/ruoyi-cloud/pom.xml -pl ruoyi-modules/ruoyi-system spring-boot:run"
launch auth "mvn --file backend/ruoyi-cloud/pom.xml -pl ruoyi-auth spring-boot:run"
launch care "mvn --file backend/pom.xml -pl care-service spring-boot:run"
launch gateway "mvn --file backend/ruoyi-cloud/pom.xml -pl ruoyi-gateway spring-boot:run"
launch frontend "VITE_AUTH_MODE=gateway VITE_RUOYI_GATEWAY_URL=/gateway npm run dev -- --port 5173"

echo "Follow startup logs with: tail -f .codespaces/logs/<service>.log"

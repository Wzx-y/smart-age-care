import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const readProjectFile = (path) => readFile(new URL(`../../${path}`, import.meta.url), "utf8");

test("Codespaces 全栈编排只从未提交的环境文件读取密钥", async () => {
  const [compose, gitignore, sample] = await Promise.all([
    readProjectFile("docker-compose.codespaces.yml"),
    readProjectFile(".gitignore"),
    readProjectFile(".env.codespaces.example"),
  ]);

  for (const service of ["mysql:", "redis:", "nacos:", "ruoyi-auth:", "ruoyi-system:", "ruoyi-gateway:", "care-service:", "frontend:"]) {
    assert.match(compose, new RegExp(`^  ${service}`, "m"));
  }
  assert.match(compose, /\$\{MYSQL_ROOT_PASSWORD:\?/);
  assert.match(compose, /\$\{PLATFORM_INTERNAL_AUTH_KEY:\?/);
  assert.match(compose, /MYSQL_SERVICE_DB_PARAM:.*allowPublicKeyRetrieval=true/);
  assert.match(compose, /http:\/\/localhost:8080\/v3\/console\/health\/readiness/);
  assert.doesNotMatch(compose, /nacos\/v1\/console\/health\/readiness|8080\/actuator\/health/);
  assert.match(gitignore, /^\.env\.codespaces$/m);
  assert.equal(sample.includes("MYSQL_ROOT_PASSWORD=\n"), true);
  assert.equal(sample.includes("PLATFORM_INTERNAL_AUTH_KEY=\n"), true);
});

test("Codespaces 容器提供 Docker，并让 Vite 代理 Gateway 请求", async () => {
  const [devcontainer, viteConfig] = await Promise.all([
    readProjectFile(".devcontainer/devcontainer.json"),
    readProjectFile("vite.config.mjs"),
  ]);

  assert.match(devcontainer, /docker-in-docker/);
  assert.match(viteConfig, /VITE_GATEWAY_PROXY_TARGET/);
  assert.match(viteConfig, /"\/gateway"/);
});

test("Codespaces Nacos 健康检查使用 Nacos 3 Console 就绪接口", async () => {
  const [infraCompose, verifyScript, nacosConfig] = await Promise.all([
    readProjectFile("infra/codespaces/compose.yaml"),
    readProjectFile("scripts/codespaces/verify.sh"),
    readProjectFile("backend/ruoyi-cloud/docker/nacos/conf/application.properties"),
  ]);

  for (const source of [infraCompose, verifyScript]) {
    assert.match(source, /http:\/\/localhost:8080\/v3\/console\/health\/readiness/);
  }
  assert.doesNotMatch(infraCompose, /nacos\/v1\/console\/health\/readiness|8848\/nacos\/actuator\/health/);
  assert.match(nacosConfig, /\/v3\/console\/health\/\*\*/);
});

test("Codespaces 启动脚本等待 MySQL 根密码认证完成后再导入 SQL", async () => {
  const [script, gatewayRouteSql, gatewayBootstrap] = await Promise.all([
    readProjectFile("scripts/start-codespaces-stack.sh"),
    readProjectFile("backend/ruoyi-cloud/sql/smart-age-care/002_gateway_care_route.sql"),
    readProjectFile("backend/ruoyi-cloud/ruoyi-gateway/src/main/resources/bootstrap.yml"),
  ]);

  assert.match(script, /mysql -N -s -uroot -p"\$MYSQL_ROOT_PASSWORD" -e "SELECT 1"/);
  assert.doesNotMatch(script, /mysqladmin ping/);
  assert.match(script, /CREATE DATABASE IF NOT EXISTS `ry-cloud`/);
  assert.match(script, /mysql -uroot -p"\$MYSQL_ROOT_PASSWORD" ry-cloud < /);
  assert.match(script, /mysql -uroot -p"\$MYSQL_ROOT_PASSWORD" smart_age_care < /);
  assert.match(script, /mysql -uroot -p"\$MYSQL_ROOT_PASSWORD" ry-config < /);
  assert.match(script, /up -d --force-recreate nacos/);
  assert.match(script, /TABLE_SCHEMA = 'smart_age_care' AND TABLE_NAME = 'care_tenant_directory'/);
  assert.match(gatewayRouteSql, /update config_info/);
  assert.match(gatewayRouteSql, /- id: care-service/);
  assert.match(gatewayRouteSql, /content not like '%- id: care-service%'/);
  assert.doesNotMatch(gatewayBootstrap, /care-gateway-routes-/);
});

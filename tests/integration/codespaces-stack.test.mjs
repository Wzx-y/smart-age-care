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

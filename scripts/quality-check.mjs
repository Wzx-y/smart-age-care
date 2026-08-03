import { readdir, readFile } from "node:fs/promises";
import path from "node:path";

const mode = process.argv[2];
const root = path.resolve("src");

async function findFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = await Promise.all(entries.map(async (entry) => {
    const target = path.join(directory, entry.name);
    return entry.isDirectory() ? findFiles(target) : [target];
  }));
  return files.flat();
}

const allFiles = await findFiles(root);
const files = allFiles.filter((file) => /\.(js|jsx)$/.test(file));
const contents = await Promise.all(files.map((file) => readFile(file, "utf8")));

if (mode === "lint") {
  const violations = files.filter((file, index) => /console\.log\(/.test(contents[index]));
  if (violations.length) throw new Error(`禁止提交 console.log: ${violations.join(", ")}`);
  console.log(`静态规范检查通过：${files.length} 个源文件。`);
} else if (mode === "typecheck") {
  const typedFiles = allFiles.filter((file) => /\.(ts|tsx)$/.test(file));
  if (typedFiles.length) throw new Error("检测到 TypeScript 文件；请在云端引入 TypeScript 编译配置后执行类型检查。");
  console.log("当前前端为 JavaScript 原型；未发现待检查的 TypeScript 文件。");
} else {
  throw new Error("用法：node scripts/quality-check.mjs <lint|typecheck>");
}

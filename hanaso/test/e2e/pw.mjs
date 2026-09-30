// Playwright はプロジェクトの依存に入れず、ローカル or グローバルにあるものを使う。
import { execSync } from 'node:child_process';
import { createRequire } from 'node:module';
import path from 'node:path';

export async function loadPlaywright() {
  try {
    return await import('playwright');
  } catch {
    const globalRoot = execSync('npm root -g').toString().trim();
    return createRequire(path.join(globalRoot, 'noop.js'))('playwright');
  }
}

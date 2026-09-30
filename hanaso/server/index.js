// サーバー起動スクリプト。設定は環境変数 (または hanaso/.env) で行う。
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createApp } from './app.js';
import { createClaudeEngine, DEFAULT_MODEL } from './ai/claude.js';
import { createMockEngine } from './ai/mock.js';

const here = path.dirname(fileURLToPath(import.meta.url));
const envFile = path.join(here, '..', '.env');
if (fs.existsSync(envFile) && typeof process.loadEnvFile === 'function') {
  process.loadEnvFile(envFile);
}

const env = process.env;
const hasCredentials = Boolean(env.ANTHROPIC_API_KEY || env.ANTHROPIC_AUTH_TOKEN);
const mode = env.AI_MODE || (hasCredentials ? 'claude' : 'mock');

let engine;
if (mode === 'claude') {
  if (!hasCredentials) {
    console.warn('⚠ ANTHROPIC_API_KEY が未設定です (ant auth login のプロファイルがあればそれを使います)。');
  }
  engine = createClaudeEngine({
    model: env.CLAUDE_MODEL || DEFAULT_MODEL,
    fastEffort: env.CLAUDE_EFFORT || 'low',
    summaryEffort: env.CLAUDE_SUMMARY_EFFORT || 'medium',
  });
} else if (mode === 'mock') {
  engine = createMockEngine();
} else {
  console.error(`AI_MODE は "claude" か "mock" を指定してください (現在: ${mode})`);
  process.exit(1);
}

const app = createApp({ engine, passcode: env.APP_PASSCODE || '' });
const port = Number(env.PORT) || 3000;
const host = env.HOST || '0.0.0.0';

app.listen(port, host, () => {
  console.log(`Hanaso を起動しました: http://localhost:${port}`);
  if (engine.name === 'mock') {
    console.log('⚠ デモモードで動いています (決まった返答のみ)。本物の AI と話すには ANTHROPIC_API_KEY を設定してください。');
  } else {
    console.log(`AI モデル: ${engine.model}`);
  }
  if (!env.APP_PASSCODE && host === '0.0.0.0') {
    console.log('ヒント: インターネットに公開する場合は APP_PASSCODE を設定して API の不正利用を防いでください。');
  }
});

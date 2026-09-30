// ブラウザで主要な画面の流れを通しで確認する E2E テスト (デモモードの AI を使用)。
// 音声認識・音声合成はテスト用の偽物に差し替えて、マイク入力やハンズフリー会話も検証する。
//   node test/e2e/smoke.mjs            … 実行
//   SCREENSHOTS=dir node test/e2e/smoke.mjs … スクリーンショットも保存
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { loadPlaywright } from './pw.mjs';
import { createApp } from '../../server/app.js';
import { createMockEngine } from '../../server/ai/mock.js';

const { chromium } = await loadPlaywright();
const shotsDir = process.env.SCREENSHOTS;
if (shotsDir) fs.mkdirSync(shotsDir, { recursive: true });

const server = createApp({ engine: createMockEngine({ delayMs: 5 }) }).listen(0);
await new Promise((r) => server.once('listening', r));
const base = `http://localhost:${server.address().port}`;

// ---- 偽の音声 API ----
function installFakeSpeech() {
  window.__speechQueue = [];
  window.__spoken = [];
  window.__recStarts = 0;
  class FakeRecognition {
    start() {
      window.__recStarts++;
      const text = window.__speechQueue.shift();
      this._stopped = false;
      if (text == null) return; // 何も話さない → アプリ側のタイムアウトで終わる
      const words = text.split(' ');
      let i = 0;
      const step = () => {
        if (this._stopped) return;
        i++;
        const result = [{ transcript: words.slice(0, i).join(' '), confidence: 0.9 }];
        result.isFinal = i === words.length;
        this.onresult?.({ results: [result], resultIndex: 0 });
        if (!result.isFinal) setTimeout(step, 20);
      };
      setTimeout(step, 40);
    }
    stop() {
      this._stopped = true;
      setTimeout(() => this.onend?.(), 10);
    }
    abort() {
      this._stopped = true;
      setTimeout(() => this.onend?.(), 0);
    }
  }
  window.SpeechRecognition = FakeRecognition;
  window.webkitSpeechRecognition = FakeRecognition;
  const synth = {
    paused: false,
    speaking: false,
    getVoices: () => [],
    addEventListener() {},
    removeEventListener() {},
    speak(u) {
      window.__spoken.push(u.text);
      setTimeout(() => u.onend?.(new Event('end')), 20);
    },
    cancel() {},
    resume() {},
  };
  Object.defineProperty(window, 'speechSynthesis', { value: synth, configurable: true });
}

const errors = [];
const browser = await chromium.launch();
const context = await browser.newContext({ viewport: { width: 390, height: 844 }, locale: 'ja-JP', hasTouch: true });
await context.addInitScript(installFakeSpeech);
await context.addInitScript(() => {
  if (!localStorage.getItem('hanaso:v1')) {
    localStorage.setItem('hanaso:v1', JSON.stringify({ settings: { silenceMs: 600 } }));
  }
});
const page = await context.newPage();
page.on('pageerror', (e) => errors.push(`pageerror: ${e.message}`));
page.on('console', (m) => m.type() === 'error' && errors.push(`console: ${m.text()}`));
page.on('dialog', (d) => d.accept());

const shot = async (name) => shotsDir && page.screenshot({ path: path.join(shotsDir, `${name}.png`) });
const step = (msg) => console.log(`✓ ${msg}`);

try {
  // ホーム
  await page.goto(base);
  await page.getByText('今日のおすすめ').waitFor();
  assert.ok(await page.getByText('デモモードで動作中').isVisible());
  await shot('01-home');
  step('ホームが表示される (デモモードの案内つき)');

  // シナリオ一覧 → カフェ → 説明シート
  await page.getByRole('link', { name: '会話' }).click();
  await page.getByRole('button', { name: /カフェで注文する/ }).click();
  await page.locator('.sheet').getByText('🎯 ミッション').waitFor();
  await shot('02-intro');
  await page.getByRole('button', { name: '会話をはじめる' }).click();
  await page.locator('.msg.ai .text', { hasText: 'What can I get started' }).waitFor();
  await page.waitForFunction(() => window.__spoken.some((t) => t.includes('What can I get started')));
  step('会話画面で AI の最初のセリフが表示・読み上げされる');

  // マイクで話す (偽の音声認識) → 自動送信 → AI 返答 & フィードバック
  await page.evaluate(() => window.__speechQueue.push('i want a medium latte with oat milk'));
  await page.locator('.mic-btn').click();
  await page.locator('.msg.user .text', { hasText: 'i want a medium latte with oat milk' }).waitFor();
  await page.locator('.msg.ai .text', { hasText: 'Could you tell me a little more' }).waitFor();
  await page.locator('.fb-chip.fix').waitFor();
  assert.ok(await page.locator('.fb-card').first().isVisible(), '修正ありのフィードバックは自動で開く');
  assert.ok(await page.getByText("I'd like a medium latte with oat milk.").isVisible());
  await page.waitForFunction(() => window.__spoken.some((t) => t.includes('Could you tell me a little more')));
  assert.equal(await page.locator('.missions li.done').count(), 1);
  await shot('03-talk-feedback');
  step('音声入力 → 自動送信 → AI の返答 (読み上げ) → 添削 → ミッション進捗');

  // ヒント → そのまま送る
  await page.getByRole('button', { name: 'ヒント' }).click();
  await page.locator('.hint-item').first().waitFor();
  assert.equal(await page.locator('.hint-item').count(), 3);
  await shot('04-hint');
  await page.locator('.hint-item').first().getByRole('button', { name: 'そのまま送る' }).click();
  await page.locator('.msg.user').nth(1).waitFor();
  await page.locator('.msg.ai').nth(2).locator('.actions').waitFor();
  step('ヒントの提案 (3件) から送信できる');

  // キーボード入力 (日本語) → 言い方の提案シート
  await page.getByRole('button', { name: '入力' }).click();
  await page.locator('.talk-foot .text-input').fill('砂糖なしでお願いします');
  await page.locator('.talk-foot .text-input').press('Enter');
  await page.getByText('英語でどう言う？').waitFor();
  await page.locator('.hint-item').first().waitFor();
  await page.keyboard.press('Escape');
  step('日本語を入力すると英語の言い方を提案する');

  // キーボード入力 (英語)
  await page.locator('.talk-foot .text-input').fill('Thank you so much');
  await page.locator('.talk-foot .text-input').press('Enter');
  await page.locator('.msg.user').nth(2).waitFor();
  await page.locator('.celebrate').waitFor();
  step('3つのミッション達成で完了表示が出る');

  // 翻訳ボタン
  await page.locator('.msg.ai').first().getByRole('button', { name: '訳' }).click();
  await page.locator('.translation', { hasText: 'デモモード' }).first().waitFor();

  // フレーズ保存
  await page.locator('.msg.ai').first().getByRole('button', { name: 'フレーズ帳に保存' }).click();
  await page.getByText('フレーズ帳に保存しました').waitFor();
  step('翻訳とフレーズ保存');

  // 終了 → 振り返り
  await page.locator('.msg.ai').nth(3).locator('.actions').waitFor();
  await page.getByRole('button', { name: '終了' }).click();
  await page.locator('.score-ring').waitFor();
  assert.match(await page.locator('.score-big').innerText(), /^\d+$/);
  await page.getByText('今回の言い直し').waitFor();
  if (shotsDir) await page.locator('.toast.show').waitFor({ state: 'detached' }).catch(() => page.waitForTimeout(2800));
  await shot('05-summary');
  await page.getByRole('button', { name: 'すべて保存' }).click();
  step('振り返り (スコア・改善点・覚えたいフレーズ) が表示される');

  // ホームの記録が増えている
  await page.getByRole('button', { name: 'ホームへ' }).click();
  await page.getByText('最近の会話').waitFor();
  assert.match(await page.locator('.recent li').first().innerText(), /カフェで注文する/);

  // フレーズ帳 → 発音チェック
  await page.getByRole('link', { name: 'フレーズ帳' }).click();
  await page.locator('.phrase-item').first().waitFor();
  const count = await page.locator('.phrase-item').count();
  assert.ok(count >= 4, `フレーズが保存されている (${count})`);
  const firstEn = await page.locator('.phrase-item .en').first().innerText();
  await page.evaluate((t) => window.__speechQueue.push(t.replace(/[.?!]/g, '')), firstEn);
  await page.locator('.phrase-item').first().getByRole('button', { name: '言ってみる' }).click();
  await page.locator('.phrase-item .score-result.great').first().waitFor();
  assert.equal(await page.locator('.phrase-item .score-num').first().innerText(), '100');
  await shot('06-phrases');
  step('フレーズ帳で発音チェック (100点)');

  // フラッシュカード
  await page.getByRole('button', { name: 'フラッシュカード' }).click();
  await page.getByText('英語で言ってみよう').waitFor();
  await page.evaluate(() => window.__speechQueue.push('hello world'));
  await page.locator('.flash .mic-btn').click();
  await page.locator('.flash .score-result').waitFor();
  await shot('07-flashcard');
  await page.locator('.flash').getByRole('button', { name: '閉じる' }).click();
  step('フラッシュカードで日本語 → 英語の練習');

  // 設定: ハンズフリーをオン
  await page.getByRole('link', { name: '設定' }).click();
  await page.getByText('ハンズフリー会話').click();
  const saved = await page.evaluate(() => JSON.parse(localStorage.getItem('hanaso:v1')).settings.handsFree);
  assert.equal(saved, true);
  await shot('08-settings');
  step('設定の保存');

  // ハンズフリーのフリートーク: マイクを押さなくても会話が続く
  await page.evaluate(() => window.__speechQueue.push('i went to the gym today', 'it was really fun'));
  await page.getByRole('link', { name: 'ホーム' }).click();
  await page.getByRole('button', { name: /今日の出来事/ }).click();
  await page.getByRole('button', { name: '会話をはじめる' }).click();
  await page.locator('.msg.user .text', { hasText: 'i went to the gym today' }).waitFor();
  await page.locator('.msg.user .text', { hasText: 'it was really fun' }).waitFor();
  await page.locator('.msg.ai').nth(2).locator('.actions').waitFor();
  assert.equal(await page.locator('.missions').isVisible(), false, 'フリートークにミッション欄はない');
  await shot('09-handsfree');
  step('ハンズフリーで自動的に会話が続く');

  // やめる (確認ダイアログは自動で OK)
  await page.getByRole('button', { name: 'やめる' }).click();
  await page.getByText('今日のおすすめ').waitFor();

  assert.deepEqual(errors, [], 'ブラウザでエラーが出ていない');
  console.log('\nE2E: すべて成功');
} catch (err) {
  await shot('failure');
  console.error('\nE2E 失敗:', err);
  if (errors.length) console.error('ブラウザのエラー:', errors);
  process.exitCode = 1;
} finally {
  await browser.close();
  server.close();
}

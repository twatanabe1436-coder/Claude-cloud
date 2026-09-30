import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { createApp } from '../server/app.js';
import { createMockEngine } from '../server/ai/mock.js';
import { AIError } from '../server/ai/errors.js';

/** テスト用にアプリを起動して base URL を返す */
async function start(opts) {
  const server = createApp({ log: () => {}, ...opts }).listen(0);
  await new Promise((r) => server.once('listening', r));
  return { server, base: `http://localhost:${server.address().port}` };
}

const post = (base, path, body, headers = {}) =>
  fetch(base + path, { method: 'POST', headers: { 'content-type': 'application/json', ...headers }, body: JSON.stringify(body) });

const HISTORY = [
  { role: 'ai', text: 'Hi there! What can I get started for you today?' },
  { role: 'user', text: 'i want a latte' },
];

let app;
before(async () => {
  app = await start({ engine: createMockEngine({ delayMs: 1 }) });
});
after(() => app.server.close());

test('GET /api/config と /api/catalog', async () => {
  const config = await (await fetch(app.base + '/api/config')).json();
  assert.deepEqual(config, { mode: 'mock', model: 'demo', needsPasscode: false });
  const catalog = await (await fetch(app.base + '/api/catalog')).json();
  assert.ok(catalog.scenarios.length >= 10);
});

test('POST /api/reply: NDJSON でストリーミングし、最後に done が来る', async () => {
  const res = await post(app.base, '/api/reply', { scenarioId: 'cafe', level: 'beginner', history: HISTORY });
  assert.equal(res.status, 200);
  assert.match(res.headers.get('content-type'), /ndjson/);
  const events = (await res.text()).trim().split('\n').map((l) => JSON.parse(l));
  const done = events.at(-1);
  assert.equal(done.type, 'done');
  const streamed = events.filter((e) => e.type === 'delta').map((e) => e.text).join('');
  assert.equal(streamed, done.text);
});

test('POST /api/reply: 入力の検証', async () => {
  let res = await post(app.base, '/api/reply', { scenarioId: 'cafe', level: 'beginner', history: [{ role: 'ai', text: 'hi' }] });
  assert.equal(res.status, 400);
  assert.match((await res.json()).error, /learner message/);

  res = await post(app.base, '/api/reply', { scenarioId: 'nope', level: 'beginner', history: HISTORY });
  assert.equal(res.status, 404);

  res = await post(app.base, '/api/reply', { scenarioId: 'cafe', level: 'expert', history: HISTORY });
  assert.equal(res.status, 400);

  res = await fetch(app.base + '/api/reply', { method: 'POST', headers: { 'content-type': 'application/json' }, body: '{bad' });
  assert.equal(res.status, 400);
});

test('POST /api/feedback, /api/hint, /api/translate', async () => {
  const fb = await (await post(app.base, '/api/feedback', { scenarioId: 'cafe', level: 'beginner', history: HISTORY })).json();
  assert.equal(fb.rating, 'fix');
  assert.deepEqual(fb.completed_missions, ['drink']);

  const hint = await (await post(app.base, '/api/hint', { scenarioId: 'free:free', level: 'advanced', history: HISTORY })).json();
  assert.equal(hint.suggestions.length, 3);

  const tr = await post(app.base, '/api/translate', { text: 'Hello!' });
  assert.equal(tr.status, 200);
});

test('POST /api/summary: スコアを 0-100 に丸め、不明なミッション id を除く', async () => {
  let received;
  const engine = {
    ...createMockEngine(),
    async summary(input) {
      received = input;
      return { score: 142.6, headline_ja: '', good_points_ja: [], improve_points: [], key_phrases: [], next_challenge_ja: '' };
    },
  };
  const a = await start({ engine });
  try {
    const res = await post(a.base, '/api/summary', { scenarioId: 'cafe', level: 'beginner', history: HISTORY, completedMissions: ['drink', 'hack'] });
    const body = await res.json();
    assert.equal(body.score, 100);
    assert.deepEqual(received.completedMissions, ['drink']);
  } finally {
    a.server.close();
  }
});

test('AI のエラーは種類に応じたステータスと日本語メッセージになる', async () => {
  const engine = {
    ...createMockEngine(),
    async feedback() {
      throw new AIError('busy');
    },
    async streamReply(_input, onDelta) {
      onDelta('Hel');
      throw new AIError('refusal');
    },
  };
  const a = await start({ engine });
  try {
    const res = await post(a.base, '/api/feedback', { scenarioId: 'cafe', level: 'beginner', history: HISTORY });
    assert.equal(res.status, 503);
    assert.equal((await res.json()).kind, 'busy');

    const stream = await post(a.base, '/api/reply', { scenarioId: 'cafe', level: 'beginner', history: HISTORY });
    const events = (await stream.text()).trim().split('\n').map((l) => JSON.parse(l));
    assert.deepEqual(events[0], { type: 'delta', text: 'Hel' });
    assert.equal(events.at(-1).type, 'error');
    assert.equal(events.at(-1).kind, 'refusal');
  } finally {
    a.server.close();
  }
});

test('合言葉 (APP_PASSCODE) を設定すると AI の API は保護される', async () => {
  const a = await start({ engine: createMockEngine({ delayMs: 1 }), passcode: 's3cret' });
  try {
    const config = await (await fetch(a.base + '/api/config')).json();
    assert.equal(config.needsPasscode, true);
    // カタログは合言葉なしで読める
    assert.equal((await fetch(a.base + '/api/catalog')).status, 200);

    assert.equal((await post(a.base, '/api/translate', { text: 'Hi' })).status, 401);
    assert.equal((await post(a.base, '/api/translate', { text: 'Hi' }, { 'x-hanaso-passcode': 'wrong' })).status, 401);
    assert.equal((await post(a.base, '/api/translate', { text: 'Hi' }, { 'x-hanaso-passcode': 's3cret' })).status, 200);
    assert.equal((await post(a.base, '/api/check-passcode', {}, { 'x-hanaso-passcode': 's3cret' })).status, 200);
  } finally {
    a.server.close();
  }
});

test('静的ファイルを配信する', async () => {
  const res = await fetch(app.base + '/');
  assert.equal(res.status, 200);
  assert.match(await res.text(), /<div id="app">/);
  assert.equal((await fetch(app.base + '/api/unknown')).status, 404);
});

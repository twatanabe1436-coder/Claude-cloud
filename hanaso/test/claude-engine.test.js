// Claude エンジンが正しいパラメータで SDK を呼ぶかを、偽のクライアントで確認する (API は呼ばない)。
import { test } from 'node:test';
import assert from 'node:assert/strict';
import Anthropic from '@anthropic-ai/sdk';
import { createClaudeEngine, DEFAULT_MODEL } from '../server/ai/claude.js';
import { AIError } from '../server/ai/errors.js';
import { resolveScenario } from '../server/scenarios.js';

function fakeClient({ streamText = 'Sure! What size would you like?', stopReason = 'end_turn', parsed = {}, parseError } = {}) {
  const calls = { stream: [], parse: [] };
  const client = {
    beta: {
      messages: {
        stream(params, options) {
          calls.stream.push({ params, options });
          const listeners = [];
          return {
            on(event, cb) {
              if (event === 'text') listeners.push(cb);
              return this;
            },
            async finalMessage() {
              for (const piece of streamText.match(/\S+\s*/g) ?? []) listeners.forEach((cb) => cb(piece));
              return { stop_reason: stopReason, content: [{ type: 'thinking', thinking: '' }, { type: 'text', text: streamText }] };
            },
          };
        },
        async parse(params) {
          calls.parse.push(params);
          if (parseError) throw parseError;
          return { stop_reason: stopReason, parsed_output: stopReason === 'refusal' ? null : parsed };
        },
      },
    },
  };
  return { client, calls };
}

const scenario = resolveScenario('cafe');
const history = [
  { role: 'ai', text: scenario.opener },
  { role: 'user', text: 'a latte please' },
];

test('streamReply: 既定モデル・effort・フォールバック・キャッシュ設定で呼び、テキストを流す', async () => {
  const { client, calls } = fakeClient();
  const engine = createClaudeEngine({ client });
  const deltas = [];
  const text = await engine.streamReply({ scenario, level: 'beginner', history }, (d) => deltas.push(d));

  assert.equal(text, 'Sure! What size would you like?');
  assert.equal(deltas.join(''), text);
  const { params, options } = calls.stream[0];
  assert.equal(params.model, DEFAULT_MODEL);
  assert.equal(params.model, 'claude-opus-5-5');
  assert.deepEqual(params.output_config, { effort: 'low' });
  assert.deepEqual(params.betas, ['server-side-fallback-2026-07-01']);
  assert.equal(params.fallbacks, 'default');
  assert.deepEqual(params.cache_control, { type: 'ephemeral' });
  assert.ok(params.system.includes(scenario.aiRole));
  assert.equal(params.messages[0].content, '[start]');
  assert.equal(params.messages.at(-1).content, 'a latte please');
  assert.ok('signal' in options);
});

test('streamReply: 拒否 (refusal) は AIError になる', async () => {
  const { client } = fakeClient({ stopReason: 'refusal' });
  const engine = createClaudeEngine({ client });
  await assert.rejects(engine.streamReply({ scenario, level: 'beginner', history }, () => {}), (e) => e instanceof AIError && e.kind === 'refusal');
});

test('feedback: 構造化出力 (JSON スキーマ) で呼ぶ', async () => {
  const parsed = { rating: 'good', corrected: 'A latte, please.', natural: "Can I get a latte, please?", explanation_ja: '…', mistakes: [], completed_missions: ['drink'] };
  const { client, calls } = fakeClient({ parsed });
  const engine = createClaudeEngine({ client });
  const fb = await engine.feedback({ scenario, level: 'beginner', history });
  assert.deepEqual(fb, parsed);
  const p = calls.parse[0];
  assert.equal(p.output_config.effort, 'low');
  assert.equal(p.output_config.format.type, 'json_schema');
  assert.ok(p.output_config.format.schema.properties.rating);
  assert.ok(p.messages[0].content.includes('<utterance_to_review>'));
});

test('summary は summaryEffort を使う', async () => {
  const { client, calls } = fakeClient({ parsed: { score: 80 } });
  const engine = createClaudeEngine({ client, summaryEffort: 'high' });
  await engine.summary({ scenario, level: 'beginner', history, completedMissions: [] });
  assert.equal(calls.parse[0].output_config.effort, 'high');
});

test('SDK の例外は画面向けの種類に変換される', async () => {
  const cases = [
    [Anthropic.RateLimitError, 'busy'],
    [Anthropic.AuthenticationError, 'auth'],
    [Anthropic.APIConnectionError, 'network'],
    [Anthropic.BadRequestError, 'bad_request'],
  ];
  for (const [Cls, kind] of cases) {
    const { client } = fakeClient({ parseError: Object.create(Cls.prototype) });
    const engine = createClaudeEngine({ client });
    await assert.rejects(engine.translate({ text: 'hi' }), (e) => e instanceof AIError && e.kind === kind, kind);
  }
});

test('Haiku では effort とフォールバックを送らない', async () => {
  const { client, calls } = fakeClient({ parsed: { ja: 'やあ', words: [] } });
  const engine = createClaudeEngine({ client, model: 'claude-haiku-4-5' });
  await engine.translate({ text: 'hi' });
  const p = calls.parse[0];
  assert.equal(p.model, 'claude-haiku-4-5');
  assert.equal(p.output_config.effort, undefined);
  assert.equal(p.betas, undefined);
  assert.equal(p.fallbacks, undefined);
});

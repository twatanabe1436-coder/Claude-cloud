import { test } from 'node:test';
import assert from 'node:assert/strict';
import { resolveScenario, publicCatalog, SCENARIOS, LEVELS } from '../server/scenarios.js';
import {
  buildPartnerSystem,
  toPartnerMessages,
  buildFeedbackPrompt,
  buildHintPrompt,
  buildSummaryPrompt,
  LEVEL_GUIDE,
} from '../server/ai/prompts.js';

test('シナリオデータ: id が一意で、必要な項目がそろっている', () => {
  const ids = new Set();
  for (const s of SCENARIOS) {
    assert.ok(!ids.has(s.id), `重複: ${s.id}`);
    ids.add(s.id);
    assert.ok(LEVELS.includes(s.level), s.id);
    for (const key of ['titleJa', 'setting', 'aiName', 'aiRole', 'userRole', 'userRoleJa', 'opener', 'descriptionJa']) {
      assert.ok(s[key], `${s.id}.${key}`);
    }
    assert.ok(s.missions.length >= 1, s.id);
    assert.equal(new Set(s.missions.map((m) => m.id)).size, s.missions.length, `${s.id}: mission id 重複`);
    assert.ok(s.keyPhrases.every((p) => p.en && p.ja), s.id);
  }
});

test('resolveScenario: ロールプレイ・フリートーク・存在しない id', () => {
  assert.equal(resolveScenario('cafe').kind, 'roleplay');
  const free = resolveScenario('free:hobbies');
  assert.equal(free.kind, 'free');
  assert.equal(free.aiName, 'Alex');
  assert.deepEqual(free.missions, []);
  assert.equal(resolveScenario('free:nope'), null);
  assert.equal(resolveScenario('nope'), null);
  assert.equal(resolveScenario(undefined), null);
});

test('publicCatalog: プロンプト用の英語設定は公開しない', () => {
  const cat = publicCatalog();
  const json = JSON.stringify(cat);
  assert.ok(!json.includes('"aiRole"'));
  assert.ok(!json.includes('"setting"'));
  assert.equal(cat.scenarios.length, SCENARIOS.length);
  assert.ok(cat.freeTalkTopics.every((t) => t.id.startsWith('free:') && t.opener));
});

test('buildPartnerSystem: 役・レベル・ミッションが入る', () => {
  const s = resolveScenario('cafe');
  const sys = buildPartnerSystem(s, 'beginner');
  assert.ok(sys.includes(s.aiRole));
  assert.ok(sys.includes(LEVEL_GUIDE.beginner.partner));
  assert.ok(sys.includes('[drink]'));
  assert.ok(sys.includes('text-to-speech'));

  const free = buildPartnerSystem(resolveScenario('free:free'), 'advanced');
  assert.ok(free.includes('free conversation'));
  assert.ok(free.includes(LEVEL_GUIDE.advanced.partner));
});

test('toPartnerMessages: [start] の user ターンから始まり、交互に並ぶ', () => {
  const msgs = toPartnerMessages([
    { role: 'ai', text: 'Hi!' },
    { role: 'user', text: 'hello' },
  ]);
  assert.deepEqual(msgs, [
    { role: 'user', content: '[start]' },
    { role: 'assistant', content: 'Hi!' },
    { role: 'user', content: 'hello' },
  ]);
});

test('フィードバック / ヒント / 振り返りのプロンプト', () => {
  const s = resolveScenario('hotel');
  const history = [
    { role: 'ai', text: s.opener },
    { role: 'user', text: 'yes i have reservation' },
  ];
  const fb = buildFeedbackPrompt(s, 'intermediate', history);
  assert.ok(fb.includes('<utterance_to_review>\nyes i have reservation\n</utterance_to_review>'));
  assert.ok(fb.includes(`${s.aiName}: ${s.opener}`));
  assert.ok(fb.includes('Learner: yes i have reservation'));

  assert.ok(!buildHintPrompt(s, 'intermediate', history).includes('<learner_wants_to_say>'));
  assert.ok(buildHintPrompt(s, 'intermediate', history, '朝食は何時？').includes('朝食は何時？'));

  const sum = buildSummaryPrompt(s, 'intermediate', history, ['checkin']);
  assert.ok(sum.includes('- [x] 予約名を伝えてチェックインする'));
  assert.ok(sum.includes('- [ ] 朝食の時間や場所を聞く'));
});

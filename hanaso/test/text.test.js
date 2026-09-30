import { test } from 'node:test';
import assert from 'node:assert/strict';
import { SentenceChunker, scoreSpeech, sameSentence } from '../public/js/text.js';
import { computeStreak } from '../public/js/store.js';

test('SentenceChunker: 文が完成するたびに取り出し、残りは flush で出す', () => {
  const c = new SentenceChunker();
  assert.deepEqual(c.push('Hi there! How are'), ['Hi there!']);
  assert.deepEqual(c.push(' you today? I'), ['How are you today?']);
  assert.deepEqual(c.push("'m Jamie"), []);
  assert.deepEqual(c.flush(), ["I'm Jamie"]);
  assert.deepEqual(c.flush(), []);
});

test('SentenceChunker: Dr. などの略語では区切らない', () => {
  const c = new SentenceChunker();
  assert.deepEqual(c.push("Hi, I'm Dr. Lee. So, what brings you in? "), ["Hi, I'm Dr. Lee.", 'So, what brings you in?']);
});

test('scoreSpeech: 完全一致は 100 点 (大文字小文字・句読点は無視)', () => {
  const r = scoreSpeech('Could you make it with oat milk?', 'could you make it with oat milk');
  assert.equal(r.score, 100);
  assert.ok(r.words.every((w) => w.ok));
});

test('scoreSpeech: 言えなかった単語だけ miss になる', () => {
  const r = scoreSpeech("I'd like a medium latte, please.", 'I would like a latte please');
  assert.deepEqual(
    r.words.map((w) => w.ok),
    [true, true, true, false, true, true],
  );
  assert.ok(r.score > 70 && r.score < 100);
});

test('scoreSpeech: 短縮形と数字の読み方の違いを同一視する', () => {
  assert.equal(scoreSpeech("I'm staying for 2 weeks.", 'I am staying for two weeks').score, 100);
  assert.equal(scoreSpeech("I can't go.", 'I cannot go').score, 100);
});

test('scoreSpeech: 何も聞き取れなければ 0 点', () => {
  assert.equal(scoreSpeech('Hello there.', '').score, 0);
});

test('sameSentence', () => {
  assert.ok(sameSentence('i want coffee', 'I want coffee.'));
  assert.ok(!sameSentence('I want coffee.', "I'd like coffee."));
});

test('computeStreak: 今日または昨日から連続した日数', () => {
  const day = 24 * 60 * 60 * 1000;
  const now = new Date(2026, 8, 30, 12).getTime();
  assert.equal(computeStreak([], now), 0);
  assert.equal(computeStreak([now, now - day, now - 2 * day], now), 3);
  // 今日はまだでも昨日まで続いていれば継続
  assert.equal(computeStreak([now - day, now - 2 * day], now), 2);
  // 1日空いたらリセット
  assert.equal(computeStreak([now, now - 2 * day], now), 1);
});

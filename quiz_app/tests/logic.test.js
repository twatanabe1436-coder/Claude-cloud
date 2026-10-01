import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  buildPanelTiles,
  checkAnswer,
  createClock,
  panelSize,
  pickQuestions,
  rankFor,
  scoreFor,
  scramble,
  timeLimitMs,
} from '../js/logic.js';
import { normalizeAnswer } from '../js/normalize.js';
import { seededRandom } from '../js/util.js';

test('タイピングの表記ゆれを吸収する', () => {
  assert.equal(normalizeAnswer('ニューヨーク'), normalizeAnswer('にゅーよーく'));
  assert.equal(normalizeAnswer('ＮＹ'), 'ny');
  assert.equal(normalizeAnswer(' 夏目 漱石 '), '夏目漱石');
  assert.equal(normalizeAnswer('ﾆｭｰﾖｰｸ'), normalizeAnswer('ニューヨーク'));
  assert.equal(normalizeAnswer('ニュ-ヨ-ク'), normalizeAnswer('ニューヨーク'));
  assert.equal(normalizeAnswer('３．１４'), '3.14');
});

test('形式ごとの正誤判定', () => {
  assert.ok(checkAnswer({ type: 'ox', answer: false }, false));
  assert.ok(!checkAnswer({ type: 'ox', answer: false }, true));
  assert.ok(checkAnswer({ type: 'choice', answer: '鉄' }, '鉄'));
  assert.ok(checkAnswer({ type: 'sort', answer: 'リゾット' }, ['リ', 'ゾ', 'ッ', 'ト']));
  assert.ok(!checkAnswer({ type: 'sort', answer: 'リゾット' }, ['ゾ', 'リ', 'ッ', 'ト']));
  // 同じ文字が2回出てくる答えでも、文字列として合っていれば正解
  assert.ok(checkAnswer({ type: 'panel', answer: 'ここ' }, ['こ', 'こ']));
  const multi = { type: 'multi', correct: ['a', 'b'], wrong: ['c'] };
  assert.ok(checkAnswer(multi, ['b', 'a']));
  assert.ok(!checkAnswer(multi, ['a']));
  assert.ok(!checkAnswer(multi, ['a', 'b', 'c']));
  const order = { type: 'order', items: ['水星', '金星', '地球'] };
  assert.ok(checkAnswer(order, ['水星', '金星', '地球']));
  assert.ok(!checkAnswer(order, ['金星', '水星', '地球']));
  const typing = { type: 'typing', answer: '嘉納治五郎', alt: ['かのうじごろう'] };
  assert.ok(checkAnswer(typing, 'カノウジゴロウ'));
  assert.ok(!checkAnswer(typing, ''));
  assert.ok(!checkAnswer(typing, 'かのう'));
});

test('得点は正解50点 + 残り時間で最大50点', () => {
  assert.equal(scoreFor(10000, 10000), 100);
  assert.equal(scoreFor(5000, 10000), 75);
  assert.equal(scoreFor(0, 10000), 50);
  assert.equal(scoreFor(-100, 10000), 50);
  assert.equal(timeLimitMs({ type: 'ox' }), 10000);
  assert.equal(timeLimitMs({ type: 'ox', time: 7 }), 7000);
});

test('文字パネルは答えの文字を全部含み、ダミーは答えにない文字', () => {
  const rng = seededRandom(1);
  for (const answer of ['でんき', 'しゅうぶん', 'まくらのそうし', 'ワシントン', 'バーディー']) {
    const tiles = buildPanelTiles({ answer }, rng);
    const len = [...answer].length;
    assert.equal(tiles.length, panelSize(len));
    const rest = [...tiles];
    for (const c of answer) rest.splice(rest.indexOf(c), 1);
    assert.equal(rest.length, tiles.length - len);
    assert.ok(rest.every((c) => !answer.includes(c)), `${answer}: ${rest.join('')}`);
    assert.equal(new Set(rest).size, rest.length);
  }
  // 漢字の答えは dummies から選ぶ
  const tiles = buildPanelTiles({ answer: '東京都', dummies: '京都府大阪北海道' }, rng);
  assert.equal(tiles.length, 8);
});

test('並べ替えは最初から正解の並びにならない', () => {
  const rng = seededRandom(7);
  for (let i = 0; i < 50; i++) {
    assert.notDeepEqual(scramble(['ア', 'イ'], rng), ['ア', 'イ']);
  }
  assert.deepEqual(scramble(['あ', 'あ'], rng), ['あ', 'あ']);
});

test('出題はまだ解いていない問題を優先する', () => {
  const qs = Array.from({ length: 10 }, (_, i) => ({ id: `q${i}`, genre: i < 5 ? 'a' : 'b', type: 'ox' }));
  const stats = { q0: { n: 1, c: 1 }, q1: { n: 3, c: 0, miss: true }, q2: { n: 1, c: 0, miss: true } };
  const rng = seededRandom(3);
  const picked = pickQuestions(qs, { count: 7 }, stats, rng);
  assert.equal(picked.length, 7);
  assert.equal(picked.filter((q) => stats[q.id]).length, 0);
  assert.deepEqual(pickQuestions(qs, { mode: 'review', count: 10 }, stats, rng).map((q) => q.id).sort(), ['q1', 'q2']);
  assert.equal(pickQuestions(qs, { mode: 'new', count: 10 }, stats, rng).length, 7);
  assert.ok(pickQuestions(qs, { genres: ['b'], count: 10 }, stats, rng).every((q) => q.genre === 'b'));
});

test('段位', () => {
  assert.equal(rankFor(1), '名人');
  assert.equal(rankFor(0.7), '師範');
  assert.equal(rankFor(0.5), '有段者');
  assert.equal(rankFor(0), '入門者');
});

test('一時停止中は時間が進まない', () => {
  let now = 0;
  const clock = createClock(() => now);
  now = 1000;
  clock.pause();
  now = 5000;
  assert.equal(clock.elapsed(), 1000);
  clock.resume();
  now = 6000;
  assert.equal(clock.elapsed(), 2000);
});

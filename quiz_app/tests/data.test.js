import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';
import { assemble, validateQuestion } from '../js/schema.js';
import { loadLocalData } from '../tools/validate.js';

test('同梱の問題データにエラーがない', async () => {
  const { questions, problems } = await loadLocalData();
  assert.deepEqual(problems, []);
  assert.ok(questions.length > 0);
});

test('問題データのまちがいを見つけられる', () => {
  const genres = new Set(['g']);
  assert.deepEqual(validateQuestion({ id: 'a', type: 'ox', genre: 'g', q: '問', answer: true }, genres), []);
  assert.ok(validateQuestion({ id: 'a', type: 'ox', genre: 'g', q: '問', answer: 'true' }, genres).length > 0);
  assert.ok(validateQuestion({ id: 'a', type: 'quiz', genre: 'g', q: '問' }, genres).length > 0);
  assert.ok(validateQuestion({ id: 'a', type: 'ox', genre: 'x', q: '問', answer: true }, genres).length > 0);
  assert.ok(validateQuestion({ id: 'a', type: 'choice', genre: 'g', q: '問', answer: 'A', wrong: ['A', 'B'] }, genres).length > 0);
  assert.ok(validateQuestion({ id: 'a', type: 'sort', genre: 'g', q: '問', answer: 'ABC', pieces: ['A', 'C'] }, genres).length > 0);
  assert.ok(validateQuestion({ id: 'a', type: 'panel', genre: 'g', q: '問', answer: '東京' }, genres).length > 0);
  assert.deepEqual(validateQuestion({ id: 'a', type: 'panel', genre: 'g', q: '問', answer: '東京', dummies: '大阪名古屋札幌' }, genres), []);
  assert.deepEqual(validateQuestion({ id: 'a', type: 'rensou', genre: 'g', hints: ['h1', 'h2'], answer: 'A', wrong: ['B'] }, genres), []);
  assert.ok(validateQuestion({ id: 'a', type: 'rensou', genre: 'g', hints: ['h1'], answer: 'A', wrong: ['B'] }, genres).length > 0);
});

test('壊れた問題だけを除外して、残りは使う', () => {
  const manifest = { genres: [{ id: 'g' }], packs: ['ok.json', 'broken.json', 'missing.json'] };
  const { questions, problems } = assemble(manifest, [
    { status: 'fulfilled', value: { genre: 'g', questions: [{ id: 'a', type: 'ox', q: '問', answer: true }, { id: 'a', type: 'ox', q: '重複', answer: false }] } },
    { status: 'fulfilled', value: { genre: 'g' } },
    { status: 'rejected', reason: new Error('404') },
  ]);
  assert.deepEqual(questions.map((q) => q.id), ['a']);
  assert.equal(problems.length, 3);
});

test('オフライン用に保存するファイルがすべて存在する', async () => {
  const sw = await readFile(new URL('../sw.js', import.meta.url), 'utf8');
  const list = sw.match(/const SHELL = \[([\s\S]*?)\]/)[1].match(/'([^']+)'/g).map((s) => s.slice(1, -1));
  for (const path of list.filter((p) => p !== './')) {
    await assert.doesNotReject(readFile(new URL(`../${path}`, import.meta.url)), path);
  }
});

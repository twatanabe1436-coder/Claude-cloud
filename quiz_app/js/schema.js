// 問題データのチェック。アプリ（読み込み時）と tools/validate.js の両方で使う。
import { panelPool, panelSize } from './logic.js';
import { chars } from './util.js';

export const TYPES = {
  ox: '○×',
  choice: '四択',
  rensou: '連想',
  sort: '並べ替え',
  panel: '文字パネル',
  multi: '多答',
  order: '順番当て',
  typing: 'タイピング',
};

const isText = (v) => typeof v === 'string' && v.trim() !== '';
const isTextList = (v, min, max) => Array.isArray(v) && v.length >= min && v.length <= max && v.every(isText);
const hasDuplicate = (list) => new Set(list).size !== list.length;

// 問題1つをチェックして、問題点の一覧を返す（問題なければ空配列）
export function validateQuestion(q, genreIds) {
  if (!q || typeof q !== 'object' || Array.isArray(q)) return ['問題が { } で囲まれたオブジェクトになっていません'];
  const e = [];
  if (!isText(q.id)) e.push('id がありません');
  if (!Object.hasOwn(TYPES, q.type)) e.push(`type "${q.type}" は使えません（${Object.keys(TYPES).join(' / ')}）`);
  if (!isText(q.genre)) e.push('genre がありません');
  else if (genreIds && !genreIds.has(q.genre)) e.push(`genre "${q.genre}" が manifest.json にありません`);
  if (q.type !== 'rensou' && !isText(q.q)) e.push('q（問題文）がありません');
  if (q.q != null && typeof q.q !== 'string') e.push('q は文字列で書いてください');
  if (q.explain != null && typeof q.explain !== 'string') e.push('explain は文字列で書いてください');
  if (q.time != null && !(Number.isFinite(q.time) && q.time >= 3 && q.time <= 120)) e.push('time は 3〜120（秒）の数値で書いてください');

  switch (q.type) {
    case 'ox':
      if (typeof q.answer !== 'boolean') e.push('answer は true（○）か false（×）で書いてください');
      break;
    case 'rensou':
      if (!isTextList(q.hints, 2, 6)) e.push('hints にヒントを2〜6個並べてください');
    // falls through: 連想は四択と同じく answer と wrong を持つ
    case 'choice':
      if (!isText(q.answer)) e.push('answer がありません');
      if (!isTextList(q.wrong, 1, 5)) e.push('wrong に誤答を1〜5個並べてください');
      else if (hasDuplicate([q.answer, ...q.wrong])) e.push('選択肢が重複しています');
      break;
    case 'sort':
      if (!isText(q.answer)) e.push('answer がありません');
      else if (q.pieces != null) {
        if (!isTextList(q.pieces, 2, 12)) e.push('pieces に2〜12個のかたまりを並べてください');
        else if (q.pieces.join('') !== q.answer) e.push('pieces をつなげると answer と同じになるようにしてください');
      } else if (chars(q.answer).length < 2 || chars(q.answer).length > 12) {
        e.push('answer は2〜12文字にしてください（長い答えは pieces で区切れます）');
      }
      break;
    case 'panel': {
      if (!isText(q.answer)) {
        e.push('answer がありません');
        break;
      }
      const len = chars(q.answer).length;
      if (len < 2 || len > 12) e.push('answer は2〜12文字にしてください');
      if (q.dummies != null && typeof q.dummies !== 'string') e.push('dummies は文字列で書いてください');
      if (!panelPool(q.answer)) {
        const extra = new Set(chars(q.dummies ?? '').filter((c) => !q.answer.includes(c)));
        const need = panelSize(len) - len;
        if (extra.size < need) e.push(`ひらがな・カタカナ以外の答えには、答えにない文字を ${need} 種類以上 dummies に書いてください`);
      }
      break;
    }
    case 'multi':
      if (!isTextList(q.correct, 1, 7)) e.push('correct に正解を1つ以上並べてください');
      if (!isTextList(q.wrong, 1, 7)) e.push('wrong に誤答を1つ以上並べてください');
      if (isTextList(q.correct, 1, 7) && isTextList(q.wrong, 1, 7)) {
        if (q.correct.length + q.wrong.length > 8) e.push('選択肢は合計8個までにしてください');
        if (hasDuplicate([...q.correct, ...q.wrong])) e.push('選択肢が重複しています');
      }
      break;
    case 'order':
      if (!isTextList(q.items, 2, 6)) e.push('items に正しい順番で2〜6個並べてください');
      else if (hasDuplicate(q.items)) e.push('items が重複しています');
      break;
    case 'typing':
      if (!isText(q.answer)) e.push('answer がありません');
      if (q.alt != null && !isTextList(q.alt, 0, 20)) e.push('alt（別解）は文字列の配列で書いてください');
      break;
  }
  return e;
}

// manifest と各問題ファイルの読み込み結果（Promise.allSettled の形）から、使える問題だけを集める
export function assemble(manifest, packResults) {
  const problems = [];
  const questions = [];
  const ids = new Set();
  const genreIds = new Set((manifest.genres ?? []).map((g) => g.id));

  (manifest.packs ?? []).forEach((path, i) => {
    const result = packResults[i];
    if (!result || result.status !== 'fulfilled') {
      problems.push(`${path}: 読み込めませんでした（${result?.reason?.message ?? '不明なエラー'}）`);
      return;
    }
    const pack = result.value;
    if (!Array.isArray(pack?.questions)) {
      problems.push(`${path}: "questions" の配列がありません`);
      return;
    }
    pack.questions.forEach((raw, j) => {
      const q = { genre: pack.genre, ...raw };
      const errors = validateQuestion(q, genreIds);
      if (isText(q.id) && ids.has(q.id)) errors.push(`id "${q.id}" が他の問題と重複しています`);
      if (errors.length > 0) {
        const label = isText(raw?.id) ? `（${raw.id}）` : '';
        problems.push(`${path} の ${j + 1}問目${label}: ${errors.join(' / ')}`);
        return;
      }
      ids.add(q.id);
      questions.push(q);
    });
  });
  return { questions, problems };
}

export function checkManifest(manifest) {
  if (!Array.isArray(manifest?.genres) || !Array.isArray(manifest?.packs)) {
    throw new Error('manifest.json に "genres" と "packs" の配列が必要です');
  }
}

// 画面に依存しないゲームのルール（判定・得点・出題の選び方）
import { normalizeAnswer } from './normalize.js';
import { chars, shuffle } from './util.js';

// 形式ごとの制限時間（秒）。問題に "time" があればそちらを使う
export const DEFAULT_TIME = { ox: 10, choice: 15, rensou: 20, sort: 20, panel: 20, multi: 20, order: 20, typing: 25 };

export const timeLimitMs = (q) => (q.time ?? DEFAULT_TIME[q.type] ?? 20) * 1000;

// 正解なら 50点 + 残り時間に応じて最大50点
export function scoreFor(remainingMs, limitMs) {
  const ratio = Math.min(1, Math.max(0, remainingMs / limitMs));
  return 50 + Math.round(50 * ratio);
}

export function checkAnswer(q, response) {
  switch (q.type) {
    case 'ox':
    case 'choice':
    case 'rensou':
      return response === q.answer;
    case 'sort':
    case 'panel':
      return Array.isArray(response) && response.join('') === q.answer;
    case 'multi': {
      if (!Array.isArray(response)) return false;
      const picked = new Set(response);
      return picked.size === q.correct.length && q.correct.every((c) => picked.has(c));
    }
    case 'order':
      return Array.isArray(response) && response.length === q.items.length && response.every((x, i) => x === q.items[i]);
    case 'typing': {
      const input = normalizeAnswer(response);
      return input !== '' && [q.answer, ...(q.alt ?? [])].some((a) => normalizeAnswer(a) === input);
    }
    default:
      return false;
  }
}

export function answerText(q) {
  switch (q.type) {
    case 'ox':
      return q.answer ? '○' : '×';
    case 'multi':
      return q.correct.join('、');
    case 'order':
      return q.items.join(' → ');
    default:
      return q.answer;
  }
}

export const sortPieces = (q) => q.pieces ?? chars(q.answer);

// 並べ替え・順番当てで、最初から正解の並びにならないように混ぜる
export function scramble(items, rng = Math.random) {
  if (new Set(items).size < 2) return items.slice();
  for (let i = 0; i < 20; i++) {
    const s = shuffle(items, rng);
    if (s.some((x, j) => x !== items[j])) return s;
  }
  return items.slice().reverse();
}

// 文字パネル: 答えの文字 + ダミー文字。ダミーは答えと同じ文字種から選ぶ
const HIRAGANA = 'あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわんがぎぐげござじずぜぞだでどばびぶべぼぱぴぷぺぽゃゅょっ';
const KATAKANA = [...HIRAGANA].map((c) => String.fromCharCode(c.charCodeAt(0) + 0x60)).join('') + 'ーヴ';

export function panelPool(answer) {
  if (/^[ぁ-ゖー]+$/.test(answer)) return chars(HIRAGANA);
  if (/^[ァ-ヺー]+$/.test(answer)) return chars(KATAKANA);
  return null; // 漢字や英数字の答えは問題側で dummies を指定する
}

export const panelSize = (answerLength) => (answerLength <= 5 ? 8 : answerLength <= 9 ? 12 : 16);

export function buildPanelTiles(q, rng = Math.random) {
  const answer = chars(q.answer);
  const need = panelSize(answer.length) - answer.length;
  const used = new Set(answer);
  const candidates = [...shuffle(q.dummies ? chars(q.dummies) : [], rng), ...shuffle(panelPool(q.answer) ?? [], rng)];
  const dummies = [];
  for (const c of candidates) {
    if (dummies.length >= need) break;
    if (!used.has(c)) {
      used.add(c);
      dummies.push(c);
    }
  }
  return shuffle([...answer, ...dummies], rng);
}

export function filterPool(questions, { genres = [], types = [] } = {}) {
  return questions.filter(
    (q) => (genres.length === 0 || genres.includes(q.genre)) && (types.length === 0 || types.includes(q.type)),
  );
}

// 出題の選び方: まだ解いていない問題 → 解いた回数が少ない問題 の順に優先する。
// 問題を追加すると、次のプレイで新しい問題から出題される。
export function pickQuestions(questions, { genres, types, count = 10, mode = 'normal' } = {}, stats = {}, rng = Math.random) {
  let pool = filterPool(questions, { genres, types });
  if (mode === 'new') pool = pool.filter((q) => !stats[q.id]);
  if (mode === 'review') pool = pool.filter((q) => stats[q.id]?.miss);
  const ranked = pool
    .map((q) => ({ q, key: (stats[q.id]?.n ?? 0) + rng() }))
    .sort((a, b) => a.key - b.key)
    .map((x) => x.q);
  return shuffle(ranked.slice(0, count), rng);
}

export const RANKS = [
  [0.85, '名人'],
  [0.7, '師範'],
  [0.5, '有段者'],
  [0.3, '門下生'],
  [0, '入門者'],
];

export const rankFor = (ratio) => RANKS.find(([min]) => ratio >= min)[1];

// 一時停止できるストップウォッチ
export function createClock(now = () => performance.now()) {
  const start = now();
  let pausedAt = null;
  let pausedTotal = 0;
  return {
    elapsed: () => (pausedAt ?? now()) - start - pausedTotal,
    pause() {
      if (pausedAt == null) pausedAt = now();
    },
    resume() {
      if (pausedAt != null) {
        pausedTotal += now() - pausedAt;
        pausedAt = null;
      }
    },
  };
}

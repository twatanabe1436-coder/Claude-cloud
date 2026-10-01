// 成績と設定はこの端末のブラウザ（localStorage）にだけ保存する。
// 保存できない環境（プライベートブラウズなど）でも遊べるよう、失敗は無視する。
const PREFIX = 'quiz-dojo:';

function read(key, fallback) {
  try {
    const raw = localStorage.getItem(PREFIX + key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}

function write(key, value) {
  try {
    localStorage.setItem(PREFIX + key, JSON.stringify(value));
  } catch {
    // 保存できなくてもゲームは続ける
  }
}

export const getSettings = () => ({ sound: true, ...read('settings', {}) });
export const saveSettings = (settings) => write('settings', settings);

// { [問題id]: { n: 解いた回数, c: 正解した回数, miss: 直近で間違えたか, t: 最後に解いた時刻 } }
export const getStats = () => read('stats', {});

export function recordAnswer(id, correct) {
  const stats = getStats();
  const prev = stats[id] ?? { n: 0, c: 0 };
  stats[id] = { n: prev.n + 1, c: prev.c + (correct ? 1 : 0), miss: !correct, t: Date.now() };
  write('stats', stats);
}

export const getBest = (key) => read('best', {})[key];

export function saveBest(key, score) {
  const best = read('best', {});
  best[key] = score;
  write('best', best);
}

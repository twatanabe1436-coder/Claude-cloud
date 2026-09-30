// 端末内 (localStorage) に保存するデータ: 設定・フレーズ帳・会話履歴。
// ストレージが使えない環境 (プライベートモード等) でもメモリ上で動くようにする。

const KEY = 'hanaso:v1';
const MAX_SESSIONS = 100;

const DEFAULT_SETTINGS = {
  level: 'beginner', // beginner | intermediate | advanced
  rate: 0.95, // 読み上げ速度
  voiceURI: '', // 空なら自動選択
  autoSpeak: true, // AI のセリフを自動で読み上げる
  showText: true, // AI のセリフを文字で表示する (オフでリスニング練習)
  handsFree: false, // 読み上げが終わったら自動でマイクを開始
  autoSend: true, // 話し終わったら自動で送信
  autoTranslate: false, // AI のセリフに自動で日本語訳を付ける
  silenceMs: 2500, // 話し終わりと判断するまでの無音時間
};

function blank() {
  return { settings: { ...DEFAULT_SETTINGS }, phrases: [], sessions: [], passcode: '' };
}

function load() {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return blank();
    const data = JSON.parse(raw);
    return {
      ...blank(),
      ...data,
      settings: { ...DEFAULT_SETTINGS, ...(data.settings || {}) },
      phrases: Array.isArray(data.phrases) ? data.phrases : [],
      sessions: Array.isArray(data.sessions) ? data.sessions : [],
    };
  } catch {
    return blank();
  }
}

let state = load();

function save() {
  try {
    localStorage.setItem(KEY, JSON.stringify(state));
  } catch {
    // 容量不足・保存不可でもアプリは動かし続ける
  }
}

const uid = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 7);
export const normalizePhrase = (s) => s.toLowerCase().replace(/[^a-z0-9']+/g, ' ').trim();

/** ローカル日付 (YYYY-MM-DD) */
export function dayKey(ts) {
  const d = new Date(ts);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** 今日 (または昨日) から遡って、会話した日が何日続いているか */
export function computeStreak(timestamps, now = Date.now()) {
  const days = new Set(timestamps.map(dayKey));
  const oneDay = 24 * 60 * 60 * 1000;
  let cursor = now;
  if (!days.has(dayKey(cursor))) cursor -= oneDay; // 今日まだでも昨日まで続いていれば継続
  let streak = 0;
  while (days.has(dayKey(cursor))) {
    streak++;
    cursor -= oneDay;
  }
  return streak;
}

export const store = {
  get settings() {
    return state.settings;
  },
  updateSettings(patch) {
    state.settings = { ...state.settings, ...patch };
    save();
  },

  get passcode() {
    return state.passcode;
  },
  set passcode(v) {
    state.passcode = v;
    save();
  },

  // ---- フレーズ帳 ----
  get phrases() {
    return state.phrases;
  },
  hasPhrase(en) {
    const n = normalizePhrase(en);
    return state.phrases.some((p) => normalizePhrase(p.en) === n);
  },
  /** @returns {object|null} 追加したフレーズ (既にあれば null) */
  addPhrase({ en, ja = '', source = '' }) {
    if (!en || this.hasPhrase(en)) return null;
    const p = { id: uid(), en: en.trim(), ja, source, addedAt: Date.now(), practiceCount: 0, bestScore: null };
    state.phrases.unshift(p);
    save();
    return p;
  },
  updatePhrase(id, patch) {
    const p = state.phrases.find((x) => x.id === id);
    if (p) Object.assign(p, patch);
    save();
  },
  removePhrase(id) {
    state.phrases = state.phrases.filter((p) => p.id !== id);
    save();
  },

  // ---- 会話の記録 ----
  get sessions() {
    return state.sessions;
  },
  addSession(rec) {
    const s = { id: uid(), ...rec };
    state.sessions.unshift(s);
    state.sessions = state.sessions.slice(0, MAX_SESSIONS);
    save();
    return s;
  },
  updateSession(id, patch) {
    const s = state.sessions.find((x) => x.id === id);
    if (s) Object.assign(s, patch);
    save();
  },

  stats() {
    const sessions = state.sessions;
    return {
      streak: computeStreak(sessions.map((s) => s.endedAt)),
      sessions: sessions.length,
      turns: sessions.reduce((n, s) => n + (s.userTurns || 0), 0),
      phrases: state.phrases.length,
    };
  },

  resetAll() {
    const passcode = state.passcode;
    state = blank();
    state.passcode = passcode;
    save();
  },
};

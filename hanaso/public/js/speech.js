// ブラウザの音声認識 (Web Speech API) と音声合成 (speechSynthesis) のラッパー。

const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
export const canListen = Boolean(Recognition);
export const canSpeak = 'speechSynthesis' in window && typeof SpeechSynthesisUtterance !== 'undefined';

const ERROR_JA = {
  'not-allowed': 'マイクの使用が許可されていません。ブラウザの設定でマイクを許可してください。',
  'service-not-allowed': 'このブラウザでは音声認識が使えません。キーボード入力をお使いください。',
  'audio-capture': 'マイクが見つかりません。接続を確認してください。',
  network: '音声認識サービスに接続できませんでした。通信状況を確認してください。',
  'language-not-supported': 'このブラウザは英語の音声認識に対応していません。',
};

// ---- 音声認識 ----------------------------------------------------------------

let active = null; // 同時に1つだけ動かす

/** Android Chrome の連続認識では同じ内容が重複して届くことがあるので、前の結果を含む場合は置き換える */
function joinSegment(acc, seg) {
  const a = acc.trim();
  const s = seg.trim();
  if (!a) return s;
  if (s.toLowerCase().startsWith(a.toLowerCase())) return s;
  return `${a} ${s}`;
}

/**
 * 英語の音声認識を開始する。
 * @param {object} opts
 * @param {(text: string) => void} [opts.onUpdate]   認識途中のテキスト
 * @param {(text: string, info: { aborted: boolean }) => void} opts.onEnd  終了時 (聞き取れなければ空文字)
 * @param {(messageJa: string) => void} [opts.onError]
 * @param {number} [opts.silenceMs]  この時間なにも聞こえなければ終了
 * @param {number} [opts.startTimeoutMs] 話し始めを待つ時間
 * @returns {{ stop: () => void, abort: () => void }}
 */
export function listen({ onUpdate, onEnd, onError, silenceMs = 2500, startTimeoutMs = 10000 }) {
  if (!canListen) {
    onError?.('このブラウザは音声認識に対応していません。Chrome か Safari をお使いください。');
    onEnd('', { aborted: true });
    return { stop() {}, abort() {} };
  }
  active?.abort();

  const rec = new Recognition();
  rec.lang = 'en-US';
  rec.interimResults = true;
  rec.continuous = true;
  rec.maxAlternatives = 1;

  let finalText = '';
  let interim = '';
  let finished = false;
  let timer = setTimeout(() => handle.stop(), startTimeoutMs);
  const current = () => `${finalText} ${interim}`.replace(/\s+/g, ' ').trim();

  const finish = (aborted) => {
    if (finished) return;
    finished = true;
    clearTimeout(timer);
    if (active === handle) active = null;
    onEnd(aborted ? '' : current(), { aborted });
  };

  rec.onresult = (e) => {
    let fin = '';
    let inter = '';
    for (let i = 0; i < e.results.length; i++) {
      const r = e.results[i];
      if (r.isFinal) fin = joinSegment(fin, r[0].transcript);
      else inter += r[0].transcript;
    }
    finalText = fin;
    interim = inter;
    onUpdate?.(current());
    clearTimeout(timer);
    timer = setTimeout(() => handle.stop(), silenceMs);
  };
  rec.onerror = (e) => {
    if (e.error === 'no-speech' || e.error === 'aborted') return;
    onError?.(ERROR_JA[e.error] || `音声認識でエラーが発生しました (${e.error})`);
  };
  rec.onend = () => finish(false);

  const handle = {
    stop() {
      try {
        rec.stop();
      } catch {
        finish(false);
      }
    },
    abort() {
      finish(true);
      try {
        rec.abort();
      } catch {
        /* already stopped */
      }
    },
  };
  active = handle;
  try {
    rec.start();
  } catch {
    onError?.('音声認識を開始できませんでした。もう一度お試しください。');
    finish(true);
  }
  return handle;
}

export function stopListening() {
  active?.abort();
}

// ---- 音声合成 ----------------------------------------------------------------

let voicesCache = [];
function loadVoices() {
  if (!canSpeak) return [];
  voicesCache = speechSynthesis.getVoices();
  return voicesCache;
}
if (canSpeak) {
  loadVoices();
  speechSynthesis.addEventListener?.('voiceschanged', loadVoices);
}

const PREFERRED = [/natural/i, /neural/i, /premium/i, /enhanced/i, /google us english/i, /samantha/i, /aria/i, /jenny/i, /ava/i, /allison/i];

function voiceRank(v) {
  let score = 0;
  if (v.lang === 'en-US' || v.lang === 'en_US') score += 20;
  else if (/^en[-_](GB|AU|CA)/.test(v.lang)) score += 10;
  const i = PREFERRED.findIndex((re) => re.test(v.name));
  if (i >= 0) score += 30 - i;
  if (v.localService === false) score += 2; // オンライン音声は高品質なことが多い
  return score;
}

/** 英語の音声一覧 (おすすめ順) */
export function englishVoices() {
  const voices = voicesCache.length ? voicesCache : loadVoices();
  return voices.filter((v) => /^en[-_]/i.test(v.lang)).sort((a, b) => voiceRank(b) - voiceRank(a));
}

function pickVoice(voiceURI) {
  const list = englishVoices();
  return list.find((v) => v.voiceURI === voiceURI) || list[0] || null;
}

/**
 * 読み上げキュー。文単位で順番に読み上げ、すべて終わったら whenIdle のコールバックを呼ぶ。
 * @param {() => { rate: number, voiceURI: string }} getSettings
 */
export function createSpeaker(getSettings) {
  let generation = 0;
  let pending = 0;
  let idleCallbacks = [];
  const keep = new Set(); // iOS で途中の発話が GC されて onend が来ない問題を避ける

  const becameIdle = () => {
    const cbs = idleCallbacks;
    idleCallbacks = [];
    cbs.forEach((cb) => cb());
  };

  return {
    /** @param {string} text @param {{ rate?: number }} [opts] */
    say(text, { rate } = {}) {
      if (!canSpeak || !text.trim()) return;
      const s = getSettings();
      const u = new SpeechSynthesisUtterance(text);
      const voice = pickVoice(s.voiceURI);
      if (voice) {
        u.voice = voice;
        u.lang = voice.lang;
      } else {
        u.lang = 'en-US';
      }
      u.rate = rate ?? s.rate;
      const gen = generation;
      pending++;
      keep.add(u);
      const done = () => {
        keep.delete(u);
        if (gen !== generation) return;
        pending = Math.max(0, pending - 1);
        if (pending === 0) becameIdle();
      };
      u.onend = done;
      u.onerror = done;
      speechSynthesis.speak(u);
      // Chrome で稀に一時停止状態のまま止まるのを防ぐ
      if (speechSynthesis.paused) speechSynthesis.resume();
    },
    cancel() {
      generation++;
      pending = 0;
      idleCallbacks = [];
      keep.clear();
      if (canSpeak) speechSynthesis.cancel();
    },
    whenIdle(cb) {
      if (pending === 0) cb();
      else idleCallbacks.push(cb);
    },
    get speaking() {
      return pending > 0;
    },
  };
}

/**
 * iOS Safari などはユーザー操作の中で一度 speak しないと、その後の自動読み上げが鳴らない。
 * ボタンのクリック処理の中で呼ぶ。
 */
export function unlockAudio() {
  if (!canSpeak) return;
  try {
    const u = new SpeechSynthesisUtterance(' ');
    u.volume = 0;
    speechSynthesis.speak(u);
  } catch {
    /* ignore */
  }
}

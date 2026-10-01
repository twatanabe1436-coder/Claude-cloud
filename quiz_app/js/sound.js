// 効果音は音声ファイルを使わず Web Audio で鳴らす。
// iOS は画面をタップするまで音を出せないので、最初のタップで unlock() を呼ぶ。
let ctx = null;
let enabled = true;

export const setEnabled = (value) => {
  enabled = value;
};

export function unlock() {
  try {
    if (!ctx) {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (!AudioCtx) return;
      ctx = new AudioCtx();
    }
    if (ctx.state === 'suspended') ctx.resume();
  } catch {
    ctx = null;
  }
}

function tone(freq, at, duration, { type = 'sine', gain = 0.18 } = {}) {
  const t0 = ctx.currentTime + at;
  const osc = ctx.createOscillator();
  const amp = ctx.createGain();
  osc.type = type;
  osc.frequency.value = freq;
  amp.gain.setValueAtTime(0.0001, t0);
  amp.gain.exponentialRampToValueAtTime(gain, t0 + 0.01);
  amp.gain.exponentialRampToValueAtTime(0.0001, t0 + duration);
  osc.connect(amp).connect(ctx.destination);
  osc.start(t0);
  osc.stop(t0 + duration + 0.02);
}

function play(fn) {
  if (!enabled || !ctx) return;
  try {
    fn();
  } catch {
    // 音が出せなくてもゲームは続ける
  }
}

// ピンポーン
export const correct = () =>
  play(() => {
    tone(1318.5, 0, 0.2);
    tone(1046.5, 0.17, 0.5);
  });

// ブー
export const wrong = () =>
  play(() => {
    tone(130, 0, 0.55, { type: 'square', gain: 0.1 });
    tone(137, 0, 0.55, { type: 'sawtooth', gain: 0.06 });
  });

// 残り3秒のカウント
export const tick = () => play(() => tone(880, 0, 0.07, { type: 'triangle', gain: 0.08 }));

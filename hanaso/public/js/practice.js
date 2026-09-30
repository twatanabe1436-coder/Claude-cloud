// 「言ってみる」: お手本の英文を発話して、どの単語が言えたかを表示する部品。
import { h, icon, clear, toast } from './dom.js';
import { listen, canListen } from './speech.js';
import { scoreSpeech } from './text.js';
import { store } from './store.js';

/** 採点結果の表示 (単語ごとに色分け) */
export function renderScore(result, spoken) {
  const grade = result.score >= 90 ? 'great' : result.score >= 60 ? 'good' : 'fix';
  const label = result.score >= 90 ? 'すばらしい！' : result.score >= 60 ? 'おしい！' : 'もう一度！';
  return h(
    'div',
    { class: `score-result ${grade}` },
    h('div', { class: 'score-head' }, h('span', { class: 'score-num' }, `${result.score}`), h('span', { class: 'score-label' }, label)),
    h('div', { class: 'score-words' }, result.words.map((w) => h('span', { class: w.ok ? 'w ok' : 'w miss' }, w.text))),
    h('div', { class: 'score-heard' }, '聞き取り: ', spoken ? `“${spoken}”` : '（聞き取れませんでした）'),
  );
}

/**
 * @param {object} opts
 * @param {string} opts.target  お手本の英文
 * @param {object} opts.ctx     アプリ共通のコンテキスト (speaker)
 * @param {(result: {score: number}) => void} [opts.onResult]
 * @param {string} [opts.label]
 */
export function practiceWidget({ target, ctx, onResult, label = '言ってみる' }) {
  const out = h('div', { class: 'practice-out' });
  let handle = null;

  const btn = h('button', { class: 'chip-btn practice-btn', type: 'button', onclick: toggle }, icon('mic'), h('span', null, label));

  function setListening(on) {
    btn.classList.toggle('listening', on);
    btn.lastChild.textContent = on ? '話し終わったらタップ' : label;
  }

  function toggle() {
    if (!canListen) {
      toast('このブラウザは音声認識に対応していません。Chrome か Safari をお使いください。', { kind: 'error' });
      return;
    }
    if (handle) {
      handle.stop();
      return;
    }
    ctx.speaker.cancel();
    clear(out);
    const live = h('div', { class: 'practice-live' }, '🎤 どうぞ…');
    out.append(live);
    setListening(true);
    handle = listen({
      silenceMs: Math.min(store.settings.silenceMs, 2000),
      onUpdate: (t) => (live.textContent = t),
      onError: (msg) => toast(msg, { kind: 'error' }),
      onEnd: (text, { aborted }) => {
        handle = null;
        setListening(false);
        clear(out);
        if (aborted) return;
        const result = scoreSpeech(target, text);
        out.append(renderScore(result, text));
        onResult?.(result);
      },
    });
  }

  return { el: h('div', { class: 'practice' }, btn, out), btn };
}

// フレーズ帳: 保存したフレーズの一覧・発音練習・フラッシュカード。
import { h, icon, fill, toast } from '../dom.js';
import { store } from '../store.js';
import { practiceWidget, renderScore } from '../practice.js';
import { listen, canListen } from '../speech.js';
import { scoreSpeech } from '../text.js';

function recordResult(p, score) {
  store.updatePhrase(p.id, {
    practiceCount: (p.practiceCount || 0) + 1,
    bestScore: Math.max(p.bestScore ?? 0, score),
  });
}

export function phrasesView(ctx) {
  const list = h('div', { class: 'phrase-list' });
  const header = h('div', { class: 'section-head' });
  const el = h('div', { class: 'page phrases' }, h('h1', null, 'フレーズ帳'), header, list);

  function render() {
    const phrases = store.phrases;
    fill(
      header,
      h('span', { class: 'muted' }, `${phrases.length} フレーズ`),
      phrases.length > 0 && h('button', { class: 'btn primary small', onclick: () => flashcards(ctx, phrases, render) }, icon('cards'), 'フラッシュカード'),
    );
    if (!phrases.length) {
      fill(
        list,
        h(
          'div',
          { class: 'empty' },
          h('div', { class: 'emoji huge' }, '⭐'),
          h('p', null, 'まだフレーズがありません'),
          h('p', { class: 'muted small' }, '会話中の ☆ ボタンや、振り返り画面の「覚えたいフレーズ」から保存できます。'),
          h('button', { class: 'btn primary', onclick: () => ctx.navigate('#/scenarios') }, '会話をはじめる'),
        ),
      );
      return;
    }
    fill(list, phrases.map((p) => item(p)));
  }

  function item(p) {
    const practice = practiceWidget({ target: p.en, ctx, onResult: (r) => recordResult(p, r.score) });
    return h(
      'div',
      { class: 'phrase-item card' },
      h(
        'div',
        { class: 'phrase-row' },
        h('div', { class: 'phrase-text' }, h('div', { class: 'en' }, p.en), p.ja && h('div', { class: 'ja' }, p.ja)),
        h('button', { class: 'icon-btn', 'aria-label': '再生', onclick: () => (ctx.speaker.cancel(), ctx.speaker.say(p.en)) }, icon('volume')),
        h(
          'button',
          {
            class: 'icon-btn danger',
            'aria-label': '削除',
            onclick: () => {
              if (!confirm(`「${p.en}」を削除しますか？`)) return;
              store.removePhrase(p.id);
              render();
            },
          },
          icon('trash'),
        ),
      ),
      h(
        'div',
        { class: 'phrase-meta' },
        p.source && h('span', null, p.source),
        p.bestScore != null && h('span', null, `ベスト ${p.bestScore}点・${p.practiceCount}回練習`),
      ),
      practice.el,
    );
  }

  render();
  return { el };
}

/** 日本語を見て英語で言うフラッシュカード練習 */
function flashcards(ctx, phrases, onDone) {
  const deck = [...phrases].sort(() => Math.random() - 0.5).slice(0, 10);
  let i = 0;
  let handle = null;
  const backdrop = h('div', { class: 'flash' });
  document.body.append(backdrop);

  function close() {
    handle?.abort();
    ctx.speaker.cancel();
    backdrop.remove();
    onDone();
  }

  function show(revealed = false, result = null, spoken = '') {
    const p = deck[i];
    const micBtn = h('button', { class: 'mic-btn', 'aria-label': '話す', onclick: toggle }, icon('mic'));
    const live = h('div', { class: 'live-transcript', hidden: true });

    function toggle() {
      if (!canListen) {
        toast('このブラウザは音声認識に対応していません', { kind: 'error' });
        return;
      }
      if (handle) return handle.stop();
      ctx.speaker.cancel();
      micBtn.classList.add('listening');
      micBtn.replaceChildren(icon('stop'));
      live.hidden = false;
      live.textContent = 'どうぞ…';
      handle = listen({
        silenceMs: 2000,
        onUpdate: (t) => (live.textContent = t),
        onError: (m) => toast(m, { kind: 'error' }),
        onEnd: (text, { aborted }) => {
          handle = null;
          if (aborted) return;
          const r = scoreSpeech(p.en, text);
          recordResult(p, r.score);
          show(true, r, text);
          ctx.speaker.say(p.en);
        },
      });
    }

    fill(
      backdrop,
      h(
        'div',
        { class: 'flash-card' },
        h('div', { class: 'flash-head' }, h('span', { class: 'muted' }, `${i + 1} / ${deck.length}`), h('button', { class: 'icon-btn', 'aria-label': '閉じる', onclick: close }, icon('x'))),
        h('div', { class: 'flash-q' }, h('div', { class: 'muted small' }, '英語で言ってみよう'), h('div', { class: 'flash-ja' }, p.ja || '（訳なし）')),
        revealed
          ? h('div', { class: 'flash-a' }, h('div', { class: 'en' }, p.en), result && renderScore(result, spoken))
          : h('div', { class: 'flash-a hidden-answer' }, '？'),
        live,
        h(
          'div',
          { class: 'controls' },
          h('button', { class: 'round-btn', onclick: () => (ctx.speaker.cancel(), ctx.speaker.say(p.en), show(true, result, spoken)) }, icon('volume'), h('span', null, '答えを見る')),
          micBtn,
          h(
            'button',
            {
              class: 'round-btn',
              onclick: () => {
                handle?.abort();
                if (i + 1 >= deck.length) {
                  toast('おつかれさまでした！', { kind: 'success' });
                  close();
                  return;
                }
                i++;
                show();
              },
            },
            icon('next'),
            h('span', null, i + 1 >= deck.length ? '終わる' : '次へ'),
          ),
        ),
      ),
    );
  }

  show();
}

// 設定: レベル・音声・会話モード・データ。
import { h, fill, toast, LEVEL_JA } from '../dom.js';
import { store } from '../store.js';
import { englishVoices, canListen, canSpeak } from '../speech.js';

export function settingsView(ctx) {
  const el = h('div', { class: 'page settings' });

  function toggle(key, label, desc) {
    return h(
      'label',
      { class: 'setting-row' },
      h('div', { class: 'grow' }, h('div', null, label), desc && h('div', { class: 'muted small' }, desc)),
      h('input', {
        type: 'checkbox',
        class: 'switch',
        checked: store.settings[key],
        onchange: (e) => store.updateSettings({ [key]: e.target.checked }),
      }),
    );
  }

  function render() {
    const s = store.settings;
    const voices = englishVoices();
    fill(
      el,
      h('h1', null, '設定'),
      h(
        'section',
        { class: 'card' },
        h('h2', null, '英語レベル'),
        h('p', { class: 'muted small' }, 'AI の話すスピード感・語彙・文の長さが変わります。'),
        h(
          'div',
          { class: 'segmented' },
          ['beginner', 'intermediate', 'advanced'].map((lv) =>
            h('button', { class: s.level === lv ? 'on' : '', onclick: () => (store.updateSettings({ level: lv }), render()) }, LEVEL_JA[lv]),
          ),
        ),
      ),
      h(
        'section',
        { class: 'card' },
        h('h2', null, '音声'),
        !canSpeak && h('p', { class: 'error-text' }, 'このブラウザは音声の読み上げに対応していません。'),
        h(
          'div',
          { class: 'setting-row col' },
          h('div', null, '読み上げの声'),
          h(
            'select',
            {
              class: 'select',
              onchange: (e) => store.updateSettings({ voiceURI: e.target.value }),
            },
            h('option', { value: '' }, '自動（おすすめ）'),
            voices.map((v) => h('option', { value: v.voiceURI, selected: v.voiceURI === s.voiceURI }, `${v.name} (${v.lang})`)),
          ),
        ),
        h(
          'div',
          { class: 'setting-row col' },
          h('div', null, `読み上げの速さ: ${s.rate.toFixed(2)}x`),
          h('input', {
            type: 'range',
            min: '0.6',
            max: '1.3',
            step: '0.05',
            value: String(s.rate),
            class: 'range',
            onchange: (e) => (store.updateSettings({ rate: Number(e.target.value) }), render()),
          }),
        ),
        h('button', { class: 'btn small', onclick: () => (ctx.speaker.cancel(), ctx.speaker.say("Hi! Nice to meet you. Let's practice English together.")) }, '🔊 試しに聞く'),
        toggle('autoSpeak', 'AI のセリフを自動で読み上げる'),
      ),
      h(
        'section',
        { class: 'card' },
        h('h2', null, '会話モード'),
        !canListen && h('p', { class: 'error-text' }, 'このブラウザは音声認識に対応していません。Chrome（Android / PC）か Safari（iPhone）をお使いください。'),
        toggle('handsFree', 'ハンズフリー会話', 'AI が話し終わると自動でマイクが ON になり、電話のように会話が続きます。'),
        toggle('autoSend', '話し終わったら自動で送信', 'オフにすると、認識した文を確認・修正してから送信できます。'),
        h(
          'div',
          { class: 'setting-row col' },
          h('div', null, '話し終わりの待ち時間'),
          h('div', { class: 'muted small' }, '考えながら話すなら長めがおすすめ。'),
          h(
            'div',
            { class: 'segmented' },
            [
              [1500, '短め'],
              [2500, 'ふつう'],
              [4000, '長め'],
            ].map(([ms, label]) => h('button', { class: s.silenceMs === ms ? 'on' : '', onclick: () => (store.updateSettings({ silenceMs: ms }), render()) }, label)),
          ),
        ),
        toggle('showText', 'AI のセリフを文字で表示', 'オフにすると聞き取りの練習になります（タップで表示）。'),
        toggle('autoTranslate', '日本語訳を自動で表示', 'AI のセリフに毎回日本語訳を付けます。'),
      ),
      h(
        'section',
        { class: 'card' },
        h('h2', null, 'データ'),
        h('p', { class: 'muted small' }, '会話の記録とフレーズ帳は、この端末のブラウザ内にだけ保存されます。'),
        h(
          'button',
          {
            class: 'btn danger small',
            onclick: () => {
              if (!confirm('会話の記録・フレーズ帳・設定をすべて削除しますか？')) return;
              store.resetAll();
              toast('データを削除しました');
              render();
            },
          },
          'すべてのデータを削除',
        ),
      ),
      h(
        'p',
        { class: 'muted small center' },
        `Hanaso v0.1・AI: ${ctx.config.mode === 'mock' ? 'デモモード' : ctx.config.model}`,
      ),
    );
  }

  render();
  // 声の一覧は非同期で読み込まれることがある
  const onVoices = () => render();
  if (canSpeak) speechSynthesis.addEventListener?.('voiceschanged', onVoices);
  return {
    el,
    destroy() {
      if (canSpeak) speechSynthesis.removeEventListener?.('voiceschanged', onVoices);
    },
  };
}

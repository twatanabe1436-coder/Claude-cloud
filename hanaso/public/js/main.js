// アプリの起動とルーティング (#/ ハッシュで画面を切り替える)。
import { h, icon, fill, toast } from './dom.js';
import { api, ApiError } from './api.js';
import { store } from './store.js';
import { createSpeaker, stopListening } from './speech.js';
import { homeView } from './views/home.js';
import { scenariosView } from './views/scenarios.js';
import { talkView } from './views/talk.js';
import { summaryView } from './views/summary.js';
import { phrasesView } from './views/phrases.js';
import { settingsView } from './views/settings.js';

const root = document.getElementById('app');

const ROUTES = [
  [/^#?\/?$/, homeView, 'home'],
  [/^#\/scenarios$/, scenariosView, 'talk'],
  [/^#\/talk\/(.+)$/, talkView, 'talk'],
  [/^#\/summary$/, summaryView, 'talk'],
  [/^#\/phrases$/, phrasesView, 'phrases'],
  [/^#\/settings$/, settingsView, 'settings'],
];

const TABS = [
  ['home', '#/', 'home', 'ホーム'],
  ['talk', '#/scenarios', 'chat', '会話'],
  ['phrases', '#/phrases', 'book', 'フレーズ帳'],
  ['settings', '#/settings', 'sliders', '設定'],
];

async function boot() {
  fill(root, h('div', { class: 'splash' }, h('div', { class: 'logo' }, 'Hanaso'), h('span', { class: 'spinner' })));
  let config;
  let catalog;
  try {
    config = await api.config();
    if (config.needsPasscode && !(await passcodeOk())) return askPasscode();
    catalog = await api.catalog();
  } catch (err) {
    fill(
      root,
      h(
        'div',
        { class: 'page center' },
        h('p', { class: 'error-text' }, err.message),
        h('button', { class: 'btn primary', onclick: boot }, 'もう一度'),
      ),
    );
    return;
  }

  const ctx = {
    config,
    catalog,
    speaker: createSpeaker(() => store.settings),
    session: { last: null },
    navigate(hash) {
      if (location.hash === hash) render();
      else location.hash = hash;
    },
    findScenario(id) {
      return catalog.scenarios.find((s) => s.id === id) || catalog.freeTalkTopics.find((t) => t.id === id) || null;
    },
    /** フレーズ帳に保存。訳がなければ裏で翻訳して埋める */
    savePhrase({ en, ja = '', source = '' }) {
      const p = store.addPhrase({ en, ja, source });
      if (p && !p.ja) {
        api.translate(en).then((tr) => store.updatePhrase(p.id, { ja: tr.ja }), () => {});
      }
      return p;
    },
  };

  const view = h('main', { id: 'view' });
  const tabbar = h(
    'nav',
    { class: 'tabbar' },
    TABS.map(([id, href, ic, label]) => h('a', { href, dataset: { tab: id } }, icon(ic), h('span', null, label))),
  );
  fill(root, view, tabbar);

  let current = null;
  function render() {
    const hash = location.hash || '#/';
    const route = ROUTES.find(([re]) => re.test(hash)) || ROUTES[0];
    const params = hash.match(route[0])?.slice(1) ?? [];

    current?.destroy?.();
    stopListening();
    ctx.speaker.cancel();
    document.querySelectorAll('.sheet-backdrop, .flash').forEach((n) => n.remove());

    current = route[1](ctx, params);
    fill(view, current.el);
    document.body.classList.toggle('fullscreen', Boolean(current.fullscreen));
    tabbar.querySelectorAll('a').forEach((a) => a.classList.toggle('on', a.dataset.tab === route[2]));
    if (!current.fullscreen) window.scrollTo(0, 0);
  }

  window.addEventListener('hashchange', render);
  render();
}

async function passcodeOk() {
  if (!store.passcode) return false;
  try {
    await api.checkPasscode();
    return true;
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return false;
    throw err;
  }
}

function askPasscode() {
  const input = h('input', { class: 'text-input', type: 'password', placeholder: '合言葉', autocomplete: 'current-password' });
  const submit = async () => {
    store.passcode = input.value.trim();
    if (await passcodeOk().catch(() => false)) boot();
    else toast('合言葉が違います', { kind: 'error' });
  };
  input.addEventListener('keydown', (e) => e.key === 'Enter' && submit());
  fill(
    root,
    h(
      'div',
      { class: 'page center passcode' },
      h('div', { class: 'logo big' }, 'Hanaso'),
      h('p', null, 'このサーバーは合言葉で保護されています。'),
      input,
      h('button', { class: 'btn primary block', onclick: submit }, 'はじめる'),
    ),
  );
  input.focus();
}

boot();

if ('serviceWorker' in navigator && location.protocol === 'https:') {
  navigator.serviceWorker.register('/sw.js').catch(() => {});
}

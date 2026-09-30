// シナリオ一覧と、会話を始める前の説明シート。
import { h, icon, openSheet, fill, LEVEL_JA } from '../dom.js';
import { store } from '../store.js';
import { unlockAudio } from '../speech.js';

/** シナリオの説明シートを開く (ホームからも使う) */
export function openScenarioIntro(ctx, s) {
  const isFree = s.id.startsWith('free:');
  const sheet = openSheet({ title: isFree ? 'フリートーク' : 'ロールプレイ' });
  let level = store.settings.level;

  const levelSeg = h('div', { class: 'segmented' });
  const renderLevel = () =>
    fill(
      levelSeg,
      ['beginner', 'intermediate', 'advanced'].map((lv) =>
        h(
          'button',
          {
            class: lv === level ? 'on' : '',
            onclick: () => {
              level = lv;
              renderLevel();
            },
          },
          LEVEL_JA[lv],
        ),
      ),
    );
  renderLevel();

  sheet.body.append(
    h(
      'div',
      { class: 'intro' },
      h('div', { class: 'intro-top' }, h('span', { class: 'emoji huge' }, s.emoji), h('div', null, h('div', { class: 'title' }, s.titleJa), s.titleEn && h('div', { class: 'muted' }, s.titleEn))),
      s.descriptionJa && h('p', null, s.descriptionJa),
      !isFree && h('div', { class: 'roles' }, h('div', null, h('span', { class: 'tag' }, 'あなた'), s.userRoleJa), h('div', null, h('span', { class: 'tag ai' }, '相手'), s.aiName)),
      isFree && h('p', { class: 'muted' }, '会話相手の Alex と、このテーマで自由に話しましょう。ミッションはありません。困ったら「ヒント」を使ってね。'),
      s.missions.length > 0 && h('div', { class: 'intro-block' }, h('h3', null, '🎯 ミッション'), h('ul', { class: 'mission-list' }, s.missions.map((m) => h('li', null, icon('flag'), m.ja)))),
      s.keyPhrases.length > 0 &&
        h(
          'div',
          { class: 'intro-block' },
          h('h3', null, '🔑 使えるフレーズ'),
          s.keyPhrases.map((p) =>
            h(
              'div',
              { class: 'phrase-row' },
              h('div', { class: 'phrase-text' }, h('div', { class: 'en' }, p.en), h('div', { class: 'ja' }, p.ja)),
              h('button', { class: 'icon-btn', 'aria-label': '再生', onclick: () => (ctx.speaker.cancel(), ctx.speaker.say(p.en)) }, icon('volume')),
            ),
          ),
        ),
      h('div', { class: 'intro-block' }, h('h3', null, 'レベル'), levelSeg),
      h(
        'button',
        {
          class: 'btn primary block big',
          onclick: () => {
            unlockAudio(); // iOS: ユーザー操作の中で音声を有効化しておく
            if (level !== store.settings.level) store.updateSettings({ level });
            sheet.close();
            ctx.navigate(`#/talk/${encodeURIComponent(s.id)}`);
          },
        },
        icon('mic'),
        '会話をはじめる',
      ),
      h('p', { class: 'muted small center' }, 'マイクの使用を許可してください。静かな場所がおすすめです。'),
    ),
  );
}

export function scenariosView(ctx) {
  const { catalog } = ctx;
  let levelFilter = 'all';
  let catFilter = 'all';
  const list = h('div', { class: 'scenario-list' });
  const levelSeg = h('div', { class: 'segmented' });
  const catChips = h('div', { class: 'chips' });

  function render() {
    fill(
      levelSeg,
      [['all', 'すべて'], ...['beginner', 'intermediate', 'advanced'].map((l) => [l, LEVEL_JA[l]])].map(([id, label]) =>
        h('button', { class: id === levelFilter ? 'on' : '', onclick: () => ((levelFilter = id), render()) }, label),
      ),
    );
    fill(
      catChips,
      [{ id: 'all', ja: 'すべて' }, ...catalog.categories].map((c) =>
        h('button', { class: `chip ${c.id === catFilter ? 'on' : ''}`, onclick: () => ((catFilter = c.id), render()) }, c.ja),
      ),
    );
    const items = catalog.scenarios.filter((s) => (levelFilter === 'all' || s.level === levelFilter) && (catFilter === 'all' || s.category === catFilter));
    fill(
      list,
      items.length ? items.map((s) => card(s)) : h('p', { class: 'muted center' }, '該当するシナリオがありません'),
    );
  }

  function card(s) {
    return h(
      'button',
      { class: 'scenario-card', onclick: () => openScenarioIntro(ctx, s) },
      h('span', { class: 'emoji big' }, s.emoji),
      h(
        'div',
        { class: 'grow' },
        h('div', { class: 'title' }, s.titleJa, ' ', h('span', { class: `lv lv-${s.level}` }, LEVEL_JA[s.level])),
        h('div', { class: 'muted small' }, s.descriptionJa),
        h('div', { class: 'meta' }, `🎯 ミッション ${s.missions.length}`),
      ),
      icon('next', 'chev'),
    );
  }

  render();
  const el = h(
    'div',
    { class: 'page' },
    h('h1', null, '会話する'),
    h('section', null, h('h2', null, 'フリートーク'), h('div', { class: 'topic-grid' }, catalog.freeTalkTopics.map((t) => h('button', { class: 'topic', onclick: () => openScenarioIntro(ctx, t) }, h('span', { class: 'emoji' }, t.emoji), h('span', null, t.titleJa))))),
    h('section', null, h('h2', null, 'ロールプレイ'), levelSeg, catChips, list),
  );
  return { el };
}

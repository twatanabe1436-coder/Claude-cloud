// ホーム: 学習の記録、今日のおすすめ、フリートーク、最近の会話。
import { h, icon, LEVEL_JA } from '../dom.js';
import { store } from '../store.js';
import { openScenarioIntro } from './scenarios.js';

function todaysPick(catalog, level) {
  const pool = catalog.scenarios.filter((s) => s.level === level);
  const list = pool.length ? pool : catalog.scenarios;
  const day = Math.floor(Date.now() / 86400000);
  return list[day % list.length];
}

export function homeView(ctx) {
  const { catalog, config } = ctx;
  const st = store.stats();
  const level = store.settings.level;
  const pick = todaysPick(catalog, level);
  const recent = store.sessions.slice(0, 5);

  const el = h(
    'div',
    { class: 'page home' },
    h(
      'header',
      { class: 'home-head' },
      h('div', { class: 'brand' }, h('span', { class: 'logo' }, 'Hanaso'), h('span', { class: 'tagline' }, 'AIと話して、英語が口から出るように')),
      h('button', { class: 'badge level-badge', onclick: () => ctx.navigate('#/settings') }, LEVEL_JA[level]),
    ),
    config.mode === 'mock' &&
      h('div', { class: 'banner' }, '🧪 デモモードで動作中です。AI は決まった返答しかしません。サーバーに ANTHROPIC_API_KEY を設定すると本物の AI と会話できます。'),
    h(
      'div',
      { class: 'stat-row' },
      stat(`🔥 ${st.streak}`, '日連続'),
      stat(`💬 ${st.sessions}`, '会話'),
      stat(`🗣️ ${st.turns}`, '発話'),
      stat(`⭐ ${st.phrases}`, 'フレーズ'),
    ),
    h(
      'section',
      { class: 'today card', onclick: () => openScenarioIntro(ctx, pick) },
      h('div', { class: 'today-label' }, '今日のおすすめ'),
      h('div', { class: 'today-main' }, h('span', { class: 'emoji big' }, pick.emoji), h('div', null, h('div', { class: 'title' }, pick.titleJa), h('div', { class: 'muted' }, pick.titleEn))),
      h('div', { class: 'today-desc' }, pick.descriptionJa),
      h('div', { class: 'btn primary block' }, icon('mic'), 'この会話をはじめる'),
    ),
    h(
      'section',
      null,
      h('div', { class: 'section-head' }, h('h2', null, 'フリートーク'), h('span', { class: 'muted small' }, 'AIのAlexと自由に話そう')),
      h(
        'div',
        { class: 'topic-grid' },
        catalog.freeTalkTopics.map((t) =>
          h('button', { class: 'topic', onclick: () => openScenarioIntro(ctx, t) }, h('span', { class: 'emoji' }, t.emoji), h('span', null, t.titleJa)),
        ),
      ),
    ),
    h(
      'section',
      null,
      h('div', { class: 'section-head' }, h('h2', null, 'ロールプレイ'), h('a', { href: '#/scenarios', class: 'link' }, 'すべて見る', icon('next'))),
      h(
        'div',
        { class: 'hscroll' },
        catalog.scenarios
          .filter((s) => s.level === level)
          .concat(catalog.scenarios.filter((s) => s.level !== level))
          .slice(0, 8)
          .map((s) => miniCard(ctx, s)),
      ),
    ),
    recent.length > 0 &&
      h(
        'section',
        null,
        h('div', { class: 'section-head' }, h('h2', null, '最近の会話')),
        h(
          'ul',
          { class: 'recent' },
          recent.map((r) =>
            h(
              'li',
              { onclick: () => ctx.navigate(`#/talk/${encodeURIComponent(r.scenarioId)}`) },
              h('span', { class: 'emoji' }, r.emoji),
              h('div', { class: 'grow' }, h('div', null, r.titleJa), h('div', { class: 'muted small' }, `${new Date(r.endedAt).toLocaleDateString('ja-JP')}・${r.userTurns}回発話`)),
              r.score != null && h('span', { class: 'score-pill' }, `${r.score}点`),
            ),
          ),
        ),
      ),
  );

  return { el };
}

function stat(value, label) {
  return h('div', { class: 'stat' }, h('div', { class: 'stat-value' }, value), h('div', { class: 'stat-label' }, label));
}

function miniCard(ctx, s) {
  return h(
    'button',
    { class: 'mini-card', onclick: () => openScenarioIntro(ctx, s) },
    h('span', { class: 'emoji big' }, s.emoji),
    h('div', { class: 'title' }, s.titleJa),
    h('span', { class: `lv lv-${s.level}` }, LEVEL_JA[s.level]),
  );
}

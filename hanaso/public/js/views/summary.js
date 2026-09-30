// 会話の振り返り: スコア・良かった点・改善点・覚えたいフレーズ・今回の修正。
import { h, icon, fill, toast, LEVEL_JA } from '../dom.js';
import { api } from '../api.js';
import { store } from '../store.js';

export function summaryView(ctx) {
  const s = ctx.session.last;
  if (!s) {
    ctx.navigate('#/');
    return { el: h('div') };
  }
  const sc = s.scenario;
  const body = h('div', { class: 'summary-body' });
  const el = h(
    'div',
    { class: 'page summary' },
    h('div', { class: 'summary-hero' }, h('div', { class: 'emoji big' }, sc.emoji), h('h1', null, 'おつかれさまでした！'), h('div', { class: 'muted' }, `${sc.titleJa}・${LEVEL_JA[s.level]}`)),
    body,
  );

  const minutes = Math.max(1, Math.round((s.endedAt - s.startedAt) / 60000));
  const corrections = s.turns.filter((t) => t.feedback && t.feedback.rating !== 'great');

  const phraseRow = (en, ja) =>
    h(
      'div',
      { class: 'phrase-row' },
      h('div', { class: 'phrase-text' }, h('div', { class: 'en' }, en), ja && h('div', { class: 'ja' }, ja)),
      h('button', { class: 'icon-btn', 'aria-label': '再生', onclick: () => (ctx.speaker.cancel(), ctx.speaker.say(en)) }, icon('volume')),
      saveBtn(en, ja),
    );

  function saveBtn(en, ja) {
    const btn = h('button', { class: `icon-btn star ${store.hasPhrase(en) ? 'saved' : ''}`, 'aria-label': 'フレーズ帳に保存' }, icon('star'));
    btn.addEventListener('click', () => {
      if (ctx.savePhrase({ en, ja, source: sc.titleJa })) toast('フレーズ帳に保存しました ⭐', { kind: 'success' });
      btn.classList.add('saved');
    });
    return btn;
  }

  const stats = h(
    'div',
    { class: 'stat-row' },
    stat('🗣️', s.turns.length, '発話'),
    stat('⏱️', minutes, '分'),
    sc.missions.length > 0 && stat('🎯', `${s.completed.length}/${sc.missions.length}`, 'ミッション'),
  );

  function stat(emoji, value, label) {
    return h('div', { class: 'stat' }, h('div', { class: 'stat-value' }, `${emoji} ${value}`), h('div', { class: 'stat-label' }, label));
  }

  const correctionSection =
    corrections.length > 0 &&
    h(
      'section',
      { class: 'card' },
      h('h2', null, '✏️ 今回の言い直し'),
      corrections.map((t) =>
        h(
          'div',
          { class: 'correction' },
          h('div', { class: 'yours' }, h('span', { class: 'tag' }, 'あなた'), t.text),
          phraseRow(t.feedback.natural, ''),
          h('div', { class: 'muted small' }, t.feedback.explanation_ja),
        ),
      ),
    );

  async function load() {
    fill(body, stats, h('div', { class: 'card loading-block' }, h('span', { class: 'spinner' }), 'AI コーチが振り返りを作成中…'), correctionSection);
    try {
      const r = await api.summary({ scenarioId: sc.id, level: s.level, history: s.history, completedMissions: s.completed });
      store.updateSession(s.recordId, { score: r.score });
      const allPhrases = r.key_phrases;
      fill(
        body,
        h('section', { class: 'card score-card' }, scoreRing(r.score), h('div', { class: 'headline' }, r.headline_ja)),
        stats,
        sc.missions.length > 0 &&
          h(
            'section',
            { class: 'card' },
            h('h2', null, '🎯 ミッション'),
            h('ul', { class: 'mission-result' }, sc.missions.map((m) => h('li', { class: s.completed.includes(m.id) ? 'done' : '' }, icon(s.completed.includes(m.id) ? 'check' : 'x'), m.ja))),
          ),
        h('section', { class: 'card' }, h('h2', null, '👍 よかった点'), h('ul', { class: 'bullets' }, r.good_points_ja.map((p) => h('li', null, p)))),
        h(
          'section',
          { class: 'card' },
          h('h2', null, '📈 伸ばしたいポイント'),
          r.improve_points.map((p) => h('div', { class: 'improve' }, h('div', null, p.point_ja), phraseRow(p.example_en, ''))),
        ),
        h(
          'section',
          { class: 'card' },
          h('div', { class: 'card-head' }, h('h2', null, '⭐ 覚えたいフレーズ'), h('button', {
            class: 'btn small',
            onclick: () => {
              const added = allPhrases.filter((p) => ctx.savePhrase({ en: p.en, ja: p.ja, source: sc.titleJa })).length;
              toast(added ? `${added}件をフレーズ帳に保存しました ⭐` : 'すべて保存済みです', { kind: 'success' });
              body.querySelectorAll('.key-phrases .star').forEach((b) => b.classList.add('saved'));
            },
          }, 'すべて保存')),
          h('div', { class: 'key-phrases' }, allPhrases.map((p) => phraseRow(p.en, p.ja))),
        ),
        correctionSection,
        h('section', { class: 'card next' }, h('h2', null, '🚀 次のチャレンジ'), h('div', null, r.next_challenge_ja)),
        actions(),
      );
    } catch (err) {
      fill(body, stats, h('div', { class: 'card' }, h('div', { class: 'error-text' }, err.message), h('button', { class: 'btn small', onclick: load }, 'もう一度')), correctionSection, actions());
    }
  }

  function actions() {
    return h(
      'div',
      { class: 'summary-actions' },
      h('button', { class: 'btn primary', onclick: () => ctx.navigate(`#/talk/${encodeURIComponent(sc.id)}`) }, icon('retry'), 'もう一度話す'),
      h('button', { class: 'btn', onclick: () => ctx.navigate('#/') }, icon('home'), 'ホームへ'),
    );
  }

  load();
  return { el };
}

function scoreRing(score) {
  const color = score >= 80 ? 'var(--great)' : score >= 60 ? 'var(--good)' : 'var(--fix)';
  return h(
    'div',
    { class: 'score-ring', style: { '--p': String(score), '--c': color }, role: 'img', 'aria-label': `スコア ${score} 点` },
    h('div', { class: 'score-inner' }, h('div', { class: 'score-big' }, String(score)), h('div', { class: 'muted' }, 'スコア')),
  );
}

import { h } from './dom.js';
import { loadData } from './data.js';
import { renderFormat } from './formats.js';
import { answerText, checkAnswer, createClock, filterPool, pickQuestions, rankFor, scoreFor, timeLimitMs } from './logic.js';
import { TYPES } from './schema.js';
import * as sound from './sound.js';
import * as store from './storage.js';

const app = document.getElementById('app');

const state = {
  data: null, // { manifest, questions, problems }
  setup: { genres: [], types: [], count: 10 },
  game: null, // { opts, qs, i, score, log }
  round: null, // 出題中の1問（タイマーの停止・一時停止用）
};

// ---------- 共通 ----------

function show(screen, { keepScroll = false } = {}) {
  stopRound();
  const focusedKey = document.activeElement?.dataset?.key;
  const y = window.scrollY;
  app.replaceChildren(screen);
  if (keepScroll) {
    window.scrollTo(0, y);
    if (focusedKey) app.querySelector(`[data-key="${CSS.escape(focusedKey)}"]`)?.focus({ preventScroll: true });
  } else {
    window.scrollTo(0, 0);
  }
}

function stopRound() {
  state.round?.stop();
  state.round = null;
}

const genreOf = (id) => state.data.manifest.genres.find((g) => g.id === id) ?? { id, name: id, hue: 0 };
const hueStyle = (id) => ({ '--hue': String(genreOf(id).hue ?? 0) });
const genreTag = (id) => h('span', { class: 'genre', style: hueStyle(id) }, genreOf(id).name);
const typeTag = (type) => h('span', { class: 'type-tag' }, TYPES[type]);
const logo = (cls = 'logo') => h('h1', { class: cls }, 'クイズ', h('span', null, '道場'));

const countBadge = (n) => h('span', { class: 'count' }, n);
const availableCount = (opts) => pickQuestions(state.data.questions, opts, store.getStats()).length;

function modeLabel(opts) {
  if (opts.mode === 'new') return '新しい問題';
  if (opts.mode === 'review') return '復習';
  const genres = opts.genres?.length ? opts.genres.map((id) => genreOf(id).name).join('・') : 'すべてのジャンル';
  const types = opts.types?.length ? opts.types.map((t) => TYPES[t]).join('・') : 'すべての形式';
  return `${genres} / ${types}`;
}

const bestKey = (opts) =>
  [opts.mode, [...(opts.genres ?? [])].sort().join(','), [...(opts.types ?? [])].sort().join(','), opts.count].join('|');

// ---------- 起動 ----------

async function boot() {
  sound.setEnabled(store.getSettings().sound);
  document.addEventListener('pointerdown', sound.unlock, { once: true });
  registerServiceWorker();
  try {
    state.data = await loadData();
  } catch (err) {
    renderLoadError(err);
    return;
  }
  renderHome();
}

function registerServiceWorker() {
  try {
    const secure = location.protocol === 'https:' || location.hostname === 'localhost';
    if (secure && 'serviceWorker' in navigator) navigator.serviceWorker.register('sw.js').catch(() => {});
  } catch {
    // オフライン対応が使えない環境では、オンラインのみで動く
  }
}

function renderLoadError(err) {
  const fromFile = location.protocol === 'file:';
  show(
    h(
      'section',
      { class: 'screen message' },
      logo('logo small'),
      h('p', null, '問題データを読み込めませんでした。'),
      h(
        'p',
        { class: 'muted' },
        fromFile
          ? 'index.html をファイルとして直接開くと読み込めません。README の手順でローカルサーバーを起動して開いてください。'
          : '通信状況を確認して、もう一度読み込んでください。',
      ),
      h('p', { class: 'muted small' }, String(err?.message ?? err)),
      h('button', { class: 'btn primary', type: 'button', onclick: () => location.reload() }, '再読み込み'),
    ),
  );
}

// ---------- ホーム ----------

function renderHome() {
  const { questions, manifest, problems } = state.data;
  const stats = store.getStats();
  const settings = store.getSettings();
  let tried = 0;
  let answered = 0;
  let correct = 0;
  for (const q of questions) {
    const s = stats[q.id];
    if (s) {
      tried++;
      answered += s.n;
      correct += s.c;
    }
  }
  const unseen = availableCount({ mode: 'new', count: Infinity });
  const missed = availableCount({ mode: 'review', count: Infinity });

  show(
    h(
      'section',
      { class: 'screen home' },
      h(
        'div',
        { class: 'home-hero' },
        logo(),
        h(
          'p',
          { class: 'tagline' },
          h('span', null, `${Object.keys(TYPES).length}つの出題形式 × ${manifest.genres.length}ジャンル。`),
          h('span', null, '早く答えるほど高得点。'),
        ),
        h('ul', { class: 'type-strip', 'aria-label': '出題形式' }, Object.values(TYPES).map((t) => h('li', null, t))),
      ),
      h(
        'dl',
        { class: 'record' },
        h('div', null, h('dt', null, '問題'), h('dd', null, questions.length)),
        h('div', null, h('dt', null, '挑戦済み'), h('dd', null, tried)),
        h('div', null, h('dt', null, '正答率'), h('dd', null, answered ? `${Math.round((correct / answered) * 100)}%` : '—')),
      ),
      h(
        'section',
        { class: 'genre-pick', 'aria-labelledby': 'genre-pick-title' },
        h('h2', { class: 'section-title', id: 'genre-pick-title' }, 'ジャンルを選んで10問'),
        h(
          'div',
          { class: 'genre-grid' },
          manifest.genres.map((g) => {
            const inGenre = questions.filter((q) => q.genre === g.id);
            const done = inGenre.filter((q) => stats[q.id]).length;
            return h(
              'button',
              {
                class: 'btn genre-btn',
                type: 'button',
                disabled: inGenre.length === 0,
                onclick: () => startGame({ mode: 'normal', genres: [g.id], types: [], count: 10 }),
              },
              h('i', { class: 'dot', style: hueStyle(g.id), 'aria-hidden': 'true' }),
              h(
                'span',
                { class: 'genre-label' },
                h('span', { class: 'genre-name' }, g.name),
                h('span', { class: 'genre-progress' }, `${inGenre.length}問中 ${done}問挑戦済み`),
              ),
            );
          }),
        ),
      ),
      h(
        'div',
        { class: 'home-actions' },
        h('button', { class: 'btn primary big', type: 'button', onclick: () => renderSetup() }, '形式や問題数を選んで挑戦'),
        h(
          'div',
          { class: 'pair' },
          h(
            'button',
            { class: 'btn', type: 'button', disabled: unseen === 0, onclick: () => startGame({ mode: 'new', count: 10 }) },
            '新しい問題',
            countBadge(unseen),
          ),
          h(
            'button',
            { class: 'btn', type: 'button', disabled: missed === 0, onclick: () => startGame({ mode: 'review', count: 10 }) },
            '間違えた問題',
            countBadge(missed),
          ),
        ),
      ),
      h(
        'footer',
        { class: 'home-foot' },
        h('p', { class: 'muted small' }, manifest.updated ? `問題データ ${manifest.updated} 更新` : null),
        h(
          'button',
          {
            class: 'btn ghost small',
            type: 'button',
            'aria-pressed': String(settings.sound),
            onclick: () => {
              store.saveSettings({ ...settings, sound: !settings.sound });
              sound.setEnabled(!settings.sound);
              renderHome();
            },
          },
          `効果音 ${settings.sound ? 'ON' : 'OFF'}`,
        ),
      ),
      problems.length > 0
        ? h(
            'details',
            { class: 'problems' },
            h('summary', null, `読み込めなかった問題が ${problems.length}件 あります`),
            h('ul', null, problems.map((p) => h('li', null, p))),
          )
        : null,
    ),
  );
}

// ---------- 出題設定 ----------

function renderSetup() {
  const s = state.setup;
  const { questions, manifest } = state.data;
  const pool = filterPool(questions, s);
  const total = Math.min(s.count, pool.length);
  const rerender = () => renderSetup();
  const toggle = (list, value) => {
    const i = list.indexOf(value);
    if (i >= 0) list.splice(i, 1);
    else list.push(value);
    rerender();
  };
  const chip = (key, pressed, onclick, ...children) =>
    h('button', { class: 'chip', type: 'button', 'data-key': key, 'aria-pressed': String(pressed), onclick }, ...children);

  show(
    h(
      'section',
      { class: 'screen setup' },
      h('div', { class: 'topbar' }, h('button', { class: 'btn ghost small', type: 'button', onclick: () => renderHome() }, '← もどる'), h('h2', null, '出題設定')),
      h(
        'fieldset',
        null,
        h('legend', null, 'ジャンル'),
        h(
          'div',
          { class: 'chips' },
          chip('g:all', s.genres.length === 0, () => ((s.genres = []), rerender()), 'すべて'),
          manifest.genres.map((g) =>
            chip(
              `g:${g.id}`,
              s.genres.includes(g.id),
              () => toggle(s.genres, g.id),
              h('i', { class: 'dot', style: hueStyle(g.id), 'aria-hidden': 'true' }),
              g.name,
            ),
          ),
        ),
      ),
      h(
        'fieldset',
        null,
        h('legend', null, '形式'),
        h(
          'div',
          { class: 'chips' },
          chip('t:all', s.types.length === 0, () => ((s.types = []), rerender()), 'すべて'),
          Object.entries(TYPES).map(([id, label]) => chip(`t:${id}`, s.types.includes(id), () => toggle(s.types, id), label)),
        ),
      ),
      h(
        'fieldset',
        null,
        h('legend', null, '問題数'),
        h('div', { class: 'chips' }, [5, 10, 20].map((n) => chip(`n:${n}`, s.count === n, () => ((s.count = n), rerender()), `${n}問`))),
      ),
      h(
        'div',
        { class: 'setup-foot' },
        h('p', { class: 'muted' }, pool.length > 0 ? `条件に合う問題 ${pool.length}問` : '条件に合う問題がありません。ジャンルか形式を増やしてください。'),
        h(
          'button',
          {
            class: 'btn primary big',
            type: 'button',
            disabled: total === 0,
            onclick: () => startGame({ mode: 'normal', genres: [...s.genres], types: [...s.types], count: s.count }),
          },
          `${total}問で開始`,
        ),
      ),
    ),
    { keepScroll: true },
  );
}

// ---------- クイズ ----------

function startGame(opts) {
  sound.unlock();
  const qs = pickQuestions(state.data.questions, opts, store.getStats());
  if (qs.length === 0) return;
  state.game = { opts, qs, i: 0, score: 0, log: [] };
  renderQuestion();
}

function renderQuestion() {
  const game = state.game;
  const q = game.qs[game.i];
  const limit = timeLimitMs(q);

  const scoreNum = h('span', { class: 'score-num' }, game.score);
  const timerFill = h('div', { class: 'timer-fill' });
  const timerBar = h('div', { class: 'timer', 'aria-hidden': 'true' }, timerFill);
  const timerSec = h('span', { class: 'timer-sec', role: 'timer', 'aria-label': '残り秒数' }, Math.ceil(limit / 1000));
  // タイピングは入力欄がキーボードに隠れないよう、問題文のすぐ下に置く
  const answerArea = h('div', { class: q.type === 'typing' ? 'answer-area near' : 'answer-area' });
  const format = renderFormat(q, (response) => finish(response, false), { limit });
  answerArea.append(format.answer);

  const screen = h(
    'section',
    { class: 'screen quiz' },
    h(
      'header',
      { class: 'quiz-head' },
      h('button', { class: 'btn ghost small', type: 'button', onclick: confirmQuit }, '中断'),
      h('p', { class: 'q-no' }, `第${game.i + 1}問`, h('small', null, ` / ${game.qs.length}`)),
      h('p', { class: 'score' }, h('small', null, 'SCORE'), scoreNum),
    ),
    h('div', { class: 'timer-row' }, timerBar, timerSec),
    h('article', { class: 'q-card' }, h('div', { class: 'q-meta' }, genreTag(q.genre), typeTag(q.type)), format.prompt),
    answerArea,
  );
  show(screen);
  format.focus?.();

  const clock = createClock();
  let finished = false;
  let lastSec = null;
  let raf = 0;

  const tick = () => {
    const elapsed = clock.elapsed();
    const remaining = Math.max(0, limit - elapsed);
    timerFill.style.transform = `scaleX(${remaining / limit})`;
    const sec = Math.ceil(remaining / 1000);
    if (sec !== lastSec) {
      lastSec = sec;
      timerSec.textContent = sec;
      if (sec <= 3) {
        timerBar.classList.add('low');
        timerSec.classList.add('low');
        if (sec > 0) sound.tick();
      }
    }
    format.onTick?.(elapsed);
    if (remaining <= 0) finish(null, true);
    else raf = requestAnimationFrame(tick);
  };
  state.round = {
    stop: () => cancelAnimationFrame(raf),
    pause: clock.pause,
    resume: clock.resume,
    isFinished: () => finished,
  };
  raf = requestAnimationFrame(tick);

  function finish(response, timedOut) {
    if (finished) return;
    finished = true;
    cancelAnimationFrame(raf);
    const remaining = Math.max(0, limit - clock.elapsed());
    const correct = !timedOut && checkAnswer(q, response);
    const points = correct ? scoreFor(remaining, limit) : 0;
    game.score += points;
    scoreNum.textContent = game.score;
    game.log.push({ q, correct, points, timedOut });
    store.recordAnswer(q.id, correct);

    answerArea.querySelectorAll('button, input').forEach((el) => {
      el.disabled = true;
    });
    answerArea.classList.add('locked');
    format.reveal(response, correct);
    if (correct) sound.correct();
    else {
      sound.wrong();
      try {
        navigator.vibrate?.(120);
      } catch {
        // 振動に対応していない端末（iOS など）では何もしない
      }
    }
    showVerdict(screen, { q, correct, points, timedOut });
  }
}

function showVerdict(screen, { q, correct, points, timedOut }) {
  const game = state.game;
  const last = game.i + 1 >= game.qs.length;

  const stamp = h('div', { class: 'stamp', 'aria-hidden': 'true' }, h('i', { class: correct ? 'maru' : 'batsu' }));
  setTimeout(() => stamp.remove(), 1100);

  const next = h(
    'button',
    {
      class: 'btn primary big',
      type: 'button',
      onclick: () => {
        game.i++;
        if (last) renderResult();
        else renderQuestion();
      },
    },
    last ? '結果を見る' : '次の問題',
  );
  const sheet = h(
    'div',
    { class: 'sheet' },
    h(
      'div',
      { class: `sheet-inner verdict ${correct ? 'ok' : 'ng'}`, role: 'status' },
      h(
        'div',
        { class: 'verdict-head' },
        h('p', { class: 'verdict-word' }, correct ? '正解' : timedOut ? '時間切れ' : '不正解'),
        correct ? h('p', { class: 'verdict-points' }, `+${points}`) : null,
      ),
      h('p', { class: 'verdict-answer' }, h('span', { class: 'label' }, '答え'), h('span', null, answerText(q))),
      q.explain ? h('p', { class: 'verdict-explain' }, q.explain) : null,
      next,
    ),
  );
  app.append(stamp, sheet);
  // 判定シートの下に隠れた選択肢もスクロールで見られるようにする
  screen.style.paddingBottom = `${sheet.firstChild.offsetHeight + 16}px`;
  next.focus({ preventScroll: true });
}

function confirmQuit() {
  const round = state.round;
  if (!round || round.isFinished()) {
    renderHome();
    return;
  }
  round.pause();
  const resume = () => {
    dialog.remove();
    round.resume();
  };
  const dialog = h(
    'div',
    { class: 'sheet modal' },
    h(
      'div',
      { class: 'sheet-inner', role: 'dialog', 'aria-modal': 'true', 'aria-labelledby': 'quit-title' },
      h('p', { class: 'sheet-title', id: 'quit-title' }, '中断してホームに戻りますか？'),
      h('p', { class: 'muted' }, 'ここまでに答えた問題の成績は記録されています。'),
      h(
        'div',
        { class: 'pair' },
        h('button', { class: 'btn', type: 'button', onclick: resume }, '続ける'),
        h('button', { class: 'btn primary', type: 'button', onclick: () => renderHome() }, 'ホームに戻る'),
      ),
    ),
  );
  app.append(dialog);
  dialog.querySelector('button').focus({ preventScroll: true });
}

// ---------- 結果 ----------

function renderResult() {
  const game = state.game;
  const total = game.qs.length;
  const nCorrect = game.log.filter((e) => e.correct).length;
  const key = bestKey(game.opts);
  const prevBest = store.getBest(key);
  const newBest = prevBest == null || game.score > prevBest;
  if (newBest) store.saveBest(key, game.score);
  const againCount = availableCount(game.opts);
  const missed = availableCount({ mode: 'review', count: Infinity });

  let bestLine = null;
  if (prevBest != null && newBest) bestLine = h('strong', { class: 'best' }, `自己ベスト更新（前回 ${prevBest}点）`);
  else if (prevBest != null) bestLine = h('span', { class: 'muted' }, `自己ベスト ${prevBest}点`);

  show(
    h(
      'section',
      { class: 'screen result' },
      h('p', { class: 'eyebrow' }, modeLabel(game.opts)),
      h(
        'div',
        { class: 'result-top' },
        h('p', { class: 'result-score' }, game.score, h('small', null, '点')),
        h('p', { class: 'hanko', 'aria-label': `段位 ${rankFor(game.score / (total * 100))}` }, rankFor(game.score / (total * 100))),
      ),
      h('p', { class: 'result-sub' }, h('span', null, `${total}問中 ${nCorrect}問正解`), bestLine),
      h(
        'ol',
        { class: 'review-list' },
        game.log.map((e) =>
          h(
            'li',
            { class: e.correct ? 'ok' : 'ng' },
            h('span', { class: 'mini-mark', role: 'img', 'aria-label': e.correct ? '正解' : '不正解' }),
            h(
              'div',
              { class: 'review-body' },
              h('p', { class: 'review-q' }, e.q.q ?? `連想: ${e.q.hints.join('・')}`),
              h('p', { class: 'review-a' }, '答え: ', answerText(e.q)),
            ),
            h('span', { class: 'review-pts' }, e.correct ? `+${e.points}` : e.timedOut ? '時間切れ' : '0'),
          ),
        ),
      ),
      h(
        'div',
        { class: 'result-actions' },
        h(
          'button',
          { class: 'btn primary big', type: 'button', disabled: againCount === 0, onclick: () => startGame(game.opts) },
          'もう一度',
        ),
        h(
          'div',
          { class: 'pair' },
          h(
            'button',
            { class: 'btn', type: 'button', disabled: missed === 0, onclick: () => startGame({ mode: 'review', count: 10 }) },
            '間違えた問題',
            countBadge(missed),
          ),
          h('button', { class: 'btn', type: 'button', onclick: () => renderHome() }, 'ホーム'),
        ),
      ),
    ),
  );
}

boot();

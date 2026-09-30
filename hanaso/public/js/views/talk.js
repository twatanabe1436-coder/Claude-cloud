// 会話画面: AI と音声 (またはキーボード) でやりとりし、発話ごとにフィードバックを受ける。
import { h, icon, clear, fill, toast, openSheet, LEVEL_JA, hasJapanese } from '../dom.js';
import { api } from '../api.js';
import { listen, canListen, stopListening } from '../speech.js';
import { SentenceChunker, sameSentence } from '../text.js';
import { store } from '../store.js';
import { practiceWidget } from '../practice.js';

const RATING = {
  great: { cls: 'great', label: 'Great!', icon: 'check' },
  good: { cls: 'good', label: 'もっと自然に', icon: 'bulb' },
  fix: { cls: 'fix', label: '修正あり', icon: 'retry' },
};

export function talkView(ctx, [rawId]) {
  const sc = ctx.findScenario(decodeURIComponent(rawId));
  if (!sc) {
    ctx.navigate('#/');
    return { el: h('div') };
  }
  const settings = () => store.settings;
  const level = settings().level;

  const state = {
    history: [], // API に送る会話 [{role, text}]
    turns: [], // 学習者の発話とフィードバック [{text, feedback}]
    completed: new Set(),
    busy: false, // AI の返答待ち
    listening: null, // 音声認識のハンドル
    replyAbort: null,
    alive: true,
    allDoneShown: false,
    muted: !settings().autoSpeak,
    startedAt: Date.now(),
  };

  // ---- 画面の骨組み ----
  const chat = h('div', { class: 'chat', 'aria-live': 'polite' });
  const missionBar = h('div', { class: 'missions' });
  const live = h('div', { class: 'live-transcript', hidden: true });
  const input = h('input', {
    class: 'text-input',
    type: 'text',
    placeholder: '英語で入力（日本語なら言い方を提案します）',
    enterkeyhint: 'send',
    autocomplete: 'off',
    autocapitalize: 'sentences',
    maxlength: 500,
    onkeydown: (e) => {
      if (e.key === 'Enter' && !e.isComposing) {
        e.preventDefault();
        submitTyped();
      }
    },
  });
  const typeRow = h(
    'div',
    { class: 'type-row', hidden: canListen },
    input,
    h('button', { class: 'icon-btn primary', 'aria-label': '送信', onclick: submitTyped }, icon('send')),
  );

  const micBtn = h('button', { class: 'mic-btn', 'aria-label': '話す', onclick: toggleMic }, icon('mic'));
  const hintBtn = h('button', { class: 'round-btn', onclick: () => openHint() }, icon('bulb'), h('span', null, 'ヒント'));
  const kbBtn = h(
    'button',
    {
      class: 'round-btn',
      onclick: () => {
        typeRow.hidden = !typeRow.hidden;
        if (!typeRow.hidden) input.focus();
      },
    },
    icon('keyboard'),
    h('span', null, '入力'),
  );
  const status = h('div', { class: 'talk-status' });
  const muteBtn = h('button', { class: 'icon-btn', 'aria-label': '読み上げ切り替え', onclick: toggleMute }, icon(state.muted ? 'mute' : 'volume'));

  const el = h(
    'div',
    { class: 'talk' },
    h(
      'header',
      { class: 'talk-head' },
      h('button', { class: 'icon-btn', 'aria-label': 'やめる', onclick: quit }, icon('x')),
      h('div', { class: 'talk-title' }, h('span', { class: 'emoji' }, sc.emoji), h('span', null, sc.titleJa), h('span', { class: 'badge' }, LEVEL_JA[level])),
      muteBtn,
      h('button', { class: 'end-btn', onclick: finish }, '終了'),
    ),
    missionBar,
    chat,
    h(
      'footer',
      { class: 'talk-foot' },
      live,
      status,
      typeRow,
      h('div', { class: 'controls' }, hintBtn, micBtn, kbBtn),
      !canListen && h('p', { class: 'note' }, 'このブラウザは音声入力に未対応のため、キーボードで入力してください（Chrome / Safari 推奨）。'),
    ),
  );

  renderMissions();

  // ---- 会話の開始: AI の最初のセリフ ----
  const opener = addAiBubble();
  opener.finish(sc.opener);
  state.history.push({ role: 'ai', text: sc.opener });
  speakAll(sc.opener, () => afterAiSpoke());

  // ---- ミッション ----
  function renderMissions() {
    clear(missionBar);
    if (!sc.missions.length) {
      missionBar.hidden = true;
      return;
    }
    const done = sc.missions.filter((m) => state.completed.has(m.id)).length;
    missionBar.append(
      h('div', { class: 'missions-head' }, h('span', null, '🎯 ミッション'), h('span', { class: 'count' }, `${done} / ${sc.missions.length}`)),
      h(
        'ul',
        null,
        sc.missions.map((m) => h('li', { class: state.completed.has(m.id) ? 'done' : '' }, icon(state.completed.has(m.id) ? 'check' : 'flag'), m.ja)),
      ),
    );
    if (done === sc.missions.length && !state.allDoneShown) {
      state.allDoneShown = true;
      chat.append(
        h(
          'div',
          { class: 'celebrate' },
          h('div', null, '🎉 すべてのミッションを達成しました！'),
          h('button', { class: 'btn primary small', onclick: finish }, '会話を終えて振り返る'),
          h('div', { class: 'sub' }, 'このまま会話を続けることもできます'),
        ),
      );
      scrollDown();
      toast('ミッションコンプリート！', { kind: 'success' });
    }
  }

  // ---- メッセージ表示 ----
  function scrollDown() {
    requestAnimationFrame(() => chat.scrollTo({ top: chat.scrollHeight, behavior: 'smooth' }));
  }

  function addAiBubble() {
    const text = h('div', { class: 'text' });
    const typing = h('div', { class: 'typing' }, h('i'), h('i'), h('i'));
    const trBox = h('div', { class: 'translation', hidden: true });
    const actions = h('div', { class: 'actions', hidden: true });
    const textWrap = h('div', { class: settings().showText ? 'text-wrap' : 'text-wrap veiled', onclick: () => textWrap.classList.remove('veiled') }, typing, text, h('div', { class: 'veil-label' }, '👂 聞き取りに挑戦中 — タップで表示'));
    const bubble = h('div', { class: 'bubble' }, h('div', { class: 'name' }, sc.aiName), textWrap, trBox, actions);
    const row = h('div', { class: 'msg ai' }, h('div', { class: 'avatar' }, sc.aiName.replace(/^(Dr\.|Ms\.|Mr\.|Officer)\s*/, '').charAt(0)), bubble);
    chat.append(row);
    scrollDown();

    let translation = null;
    async function toggleTranslation(forceOpen = false) {
      if (!forceOpen && !trBox.hidden) {
        trBox.hidden = true;
        return;
      }
      trBox.hidden = false;
      if (translation) return;
      fill(trBox, h('div', { class: 'muted' }, '翻訳中…'));
      try {
        translation = await api.translate(text.textContent);
        fill(trBox, 
          h('div', null, translation.ja),
          translation.words.length > 0 &&
            h('ul', { class: 'words' }, translation.words.map((w) => h('li', null, h('b', null, w.en), ` ${w.ja}`))),
        );
      } catch (err) {
        fill(trBox, h('div', { class: 'error-text' }, err.message));
      }
    }

    return {
      row,
      append(delta) {
        typing.remove();
        text.textContent += delta;
        scrollDown();
      },
      finish(full) {
        typing.remove();
        text.textContent = full;
        actions.hidden = false;
        actions.append(
          h('button', { class: 'chip-btn', onclick: () => sayNow(full) }, icon('volume'), '再生'),
          h('button', { class: 'chip-btn', onclick: () => sayNow(full, 0.7) }, icon('slow'), 'ゆっくり'),
          h('button', { class: 'chip-btn', onclick: () => toggleTranslation() }, icon('translate'), '訳'),
          savePhraseBtn(full, () => translation?.ja || ''),
        );
        if (settings().autoTranslate) toggleTranslation(true);
      },
      fail(message, retry) {
        typing.remove();
        textWrap.classList.remove('veiled');
        row.classList.add('error');
        text.textContent = message;
        actions.hidden = false;
        actions.append(h('button', { class: 'chip-btn', onclick: retry }, icon('retry'), 'もう一度'));
      },
    };
  }

  function savePhraseBtn(en, getJa) {
    const saved = () => store.hasPhrase(en);
    const btn = h('button', { class: `chip-btn ${saved() ? 'saved' : ''}`, 'aria-label': 'フレーズ帳に保存', onclick: save }, icon('star'), saved() ? '保存済み' : '保存');
    function save() {
      if (saved()) return;
      ctx.savePhrase({ en, ja: getJa(), source: sc.titleJa });
      btn.classList.add('saved');
      btn.lastChild.textContent = '保存済み';
      toast('フレーズ帳に保存しました ⭐', { kind: 'success' });
    }
    return btn;
  }

  function addUserBubble(text) {
    const chip = h('button', { class: 'fb-chip loading', disabled: true }, h('span', { class: 'spinner' }), 'チェック中…');
    const card = h('div', { class: 'fb-card', hidden: true });
    chip.addEventListener('click', () => {
      card.hidden = !card.hidden;
      if (!card.hidden) card.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    });
    const row = h('div', { class: 'msg user' }, h('div', { class: 'bubble' }, h('div', { class: 'text' }, text)), chip, card);
    chat.append(row);
    scrollDown();

    return {
      setFeedback(fb) {
        const r = RATING[fb.rating] || RATING.good;
        chip.disabled = false;
        chip.className = `fb-chip ${r.cls}`;
        fill(chip, icon(r.icon), r.label);
        const showCorrected = fb.rating === 'fix' && !sameSentence(fb.corrected, text);
        const showNatural = !sameSentence(fb.natural, text) && !sameSentence(fb.natural, fb.corrected || '');
        fill(card, 
          showCorrected && h('div', { class: 'fb-row' }, h('div', { class: 'fb-label' }, '✏️ 正しくは'), h('div', { class: 'fb-en' }, fb.corrected)),
          (showNatural || fb.rating !== 'great') &&
            h(
              'div',
              { class: 'fb-row' },
              h('div', { class: 'fb-label' }, '✨ 自然な言い方'),
              h('div', { class: 'fb-en' }, fb.natural),
              h(
                'div',
                { class: 'actions' },
                h('button', { class: 'chip-btn', onclick: () => sayNow(fb.natural) }, icon('volume'), '再生'),
                savePhraseBtn(fb.natural, () => ''),
              ),
              practiceWidget({ target: fb.natural, ctx }).el,
            ),
          h('div', { class: 'fb-explain' }, fb.explanation_ja),
          fb.mistakes.length > 0 &&
            h(
              'ul',
              { class: 'fb-mistakes' },
              fb.mistakes.map((m) => h('li', null, h('s', null, m.wrong), ' → ', h('b', null, m.right), h('div', { class: 'muted' }, m.note_ja))),
            ),
        );
        // 修正があるときは自動で開いて気づけるようにする
        if (fb.rating === 'fix') card.hidden = false;
      },
      setError() {
        chip.disabled = true;
        chip.className = 'fb-chip error';
        fill(chip, 'フィードバックを取得できませんでした');
      },
    };
  }

  // ---- 音声 ----
  function sayNow(text, rate) {
    ctx.speaker.cancel();
    ctx.speaker.say(text, { rate });
  }

  function speakAll(text, then) {
    if (state.muted) return then?.();
    ctx.speaker.say(text);
    if (then) ctx.speaker.whenIdle(then);
  }

  function toggleMute() {
    state.muted = !state.muted;
    store.updateSettings({ autoSpeak: !state.muted });
    muteBtn.replaceChildren(icon(state.muted ? 'mute' : 'volume'));
    if (state.muted) ctx.speaker.cancel();
    toast(state.muted ? '自動読み上げ: オフ' : '自動読み上げ: オン');
  }

  /** AI が話し終わったあと: ハンズフリーなら自動でマイクを開始 */
  function afterAiSpoke() {
    if (!state.alive || state.busy || state.listening) return;
    if (settings().handsFree && canListen) startListening();
  }

  // ---- 音声入力 ----
  function setStatus(text) {
    status.textContent = text;
  }

  function toggleMic() {
    if (state.listening) state.listening.stop();
    else startListening();
  }

  function startListening() {
    if (state.busy) return;
    if (!canListen) {
      typeRow.hidden = false;
      input.focus();
      return;
    }
    ctx.speaker.cancel();
    live.hidden = false;
    live.textContent = '';
    live.classList.add('placeholder');
    live.textContent = 'どうぞ話してください…';
    micBtn.classList.add('listening');
    micBtn.replaceChildren(icon('stop'));
    setStatus('聞いています — 話し終わると自動で送信されます');
    state.listening = listen({
      silenceMs: settings().silenceMs,
      onUpdate(text) {
        live.classList.remove('placeholder');
        live.textContent = text;
      },
      onError(msg) {
        toast(msg, { kind: 'error', ms: 4000 });
      },
      onEnd(text, { aborted }) {
        state.listening = null;
        micBtn.classList.remove('listening');
        micBtn.replaceChildren(icon('mic'));
        live.hidden = true;
        setStatus('');
        if (aborted || !state.alive) return;
        if (!text) {
          setStatus('聞き取れませんでした。マイクをタップしてもう一度どうぞ');
          return;
        }
        if (settings().autoSend) send(text);
        else {
          typeRow.hidden = false;
          input.value = text;
          input.focus();
        }
      },
    });
  }

  function submitTyped() {
    const text = input.value.trim();
    if (!text) return;
    input.value = '';
    if (hasJapanese(text)) {
      openHint(text);
      return;
    }
    send(text);
  }

  // ---- 送信 → フィードバック & AI の返答 ----
  function setBusy(on) {
    state.busy = on;
    micBtn.disabled = on;
    hintBtn.disabled = on;
    el.classList.toggle('busy', on);
  }

  function send(text) {
    if (state.busy || !state.alive) return;
    if (hasJapanese(text)) {
      openHint(text);
      return;
    }
    cueEl?.remove();
    state.history.push({ role: 'user', text });
    const turn = { text, feedback: null };
    state.turns.push(turn);
    const bubble = addUserBubble(text);
    const snapshot = state.history.slice();

    // フィードバック (返答と並行して取得)
    api
      .feedback({ scenarioId: sc.id, level, history: snapshot })
      .then((fb) => {
        turn.feedback = fb;
        bubble.setFeedback(fb);
        let changed = false;
        for (const id of fb.completed_missions) {
          if (sc.missions.some((m) => m.id === id) && !state.completed.has(id)) {
            state.completed.add(id);
            changed = true;
          }
        }
        if (changed && state.alive) renderMissions();
      })
      .catch(() => bubble.setError());

    requestReply(snapshot);
  }

  async function requestReply(snapshot) {
    setBusy(true);
    setStatus(`${sc.aiName} が考え中…`);
    const bubble = addAiBubble();
    const chunker = new SentenceChunker();
    const abort = new AbortController();
    state.replyAbort = abort;
    ctx.speaker.cancel();
    try {
      const full = await api.reply(
        { scenarioId: sc.id, level, history: snapshot },
        {
          signal: abort.signal,
          onDelta(delta) {
            bubble.append(delta);
            // 文が完成したものから順に読み上げを始める
            if (!state.muted) chunker.push(delta).forEach((s) => ctx.speaker.say(s));
          },
        },
      );
      if (!state.alive) return;
      bubble.finish(full);
      state.history.push({ role: 'ai', text: full });
      setBusy(false);
      setStatus('');
      if (!state.muted) chunker.flush().forEach((s) => ctx.speaker.say(s));
      ctx.speaker.whenIdle(afterAiSpoke);
    } catch (err) {
      if (!state.alive || err?.name === 'AbortError') return;
      ctx.speaker.cancel();
      setBusy(false);
      setStatus('');
      bubble.fail(err.message || 'エラーが発生しました', () => {
        bubble.row.remove();
        requestReply(snapshot);
      });
    } finally {
      if (state.replyAbort === abort) state.replyAbort = null;
    }
  }

  // ---- ヒント ----
  function openHint(want = '') {
    if (state.busy) return;
    state.listening?.abort();
    const sheet = openSheet({ title: want ? '英語でどう言う？' : 'ヒント：何て言えばいい？' });
    const list = h('div', { class: 'hint-list' });
    const wantInput = h('input', {
      class: 'text-input',
      type: 'text',
      placeholder: '言いたいことを日本語で（例：砂糖なしでお願いします）',
      value: want,
      enterkeyhint: 'go',
      onkeydown: (e) => {
        if (e.key === 'Enter' && !e.isComposing) {
          e.preventDefault();
          load(wantInput.value.trim());
        }
      },
    });
    sheet.body.append(
      list,
      h(
        'div',
        { class: 'want-box' },
        h('div', { class: 'want-label' }, '🇯🇵 言いたいことを日本語で入力すると、英語の言い方を提案します'),
        h('div', { class: 'type-row' }, wantInput, h('button', { class: 'btn primary small', onclick: () => load(wantInput.value.trim()) }, '英語にする')),
      ),
    );

    let seq = 0;
    async function load(w) {
      const my = ++seq;
      fill(list, h('div', { class: 'loading-block' }, h('span', { class: 'spinner' }), w ? '英語の言い方を考えています…' : 'ヒントを考えています…'));
      try {
        const res = await api.hint({ scenarioId: sc.id, level, history: state.history, want: w || undefined });
        if (my !== seq) return;
        fill(list, 
          res.suggestions.map((s) =>
            h(
              'div',
              { class: 'hint-item' },
              h('div', { class: 'hint-label' }, s.label_ja),
              h('div', { class: 'hint-en' }, s.en),
              h('div', { class: 'hint-ja' }, s.ja),
              h(
                'div',
                { class: 'actions' },
                h('button', { class: 'chip-btn', onclick: () => sayNow(s.en) }, icon('volume'), '聞く'),
                canListen &&
                  h(
                    'button',
                    {
                      class: 'chip-btn accent',
                      onclick: () => {
                        sheet.close();
                        toast('ヒントを見ながら、自分の声で言ってみましょう！');
                        showCue(s);
                        startListening();
                      },
                    },
                    icon('mic'),
                    '自分で言う',
                  ),
                h(
                  'button',
                  {
                    class: 'chip-btn',
                    onclick: () => {
                      sheet.close();
                      send(s.en);
                    },
                  },
                  icon('send'),
                  'そのまま送る',
                ),
              ),
            ),
          ),
        );
      } catch (err) {
        if (my !== seq) return;
        fill(list, h('div', { class: 'error-text' }, err.message), h('button', { class: 'btn small', onclick: () => load(w) }, 'もう一度'));
      }
    }
    load(want);
  }

  /** 「自分で言う」を選んだとき、言う内容を画面下に小さく表示しておく */
  let cueEl = null;
  function showCue(s) {
    cueEl?.remove();
    cueEl = h('div', { class: 'cue' }, h('div', { class: 'cue-en' }, s.en), h('div', { class: 'cue-ja' }, s.ja), h('button', { class: 'icon-btn', 'aria-label': '閉じる', onclick: () => cueEl.remove() }, icon('x')));
    status.before(cueEl);
    const remove = () => cueEl?.remove();
    setTimeout(remove, 30000);
  }

  // ---- 終了 ----
  function quit() {
    if (state.turns.length && !confirm('会話をやめますか？（振り返りは作成されません）')) return;
    ctx.navigate('#/');
  }

  function finish() {
    if (!state.turns.length) {
      ctx.navigate('#/');
      return;
    }
    state.replyAbort?.abort();
    // 返答待ちのまま終了した場合、履歴は学習者の発話で終わる
    const history = state.history.slice();
    const endedAt = Date.now();
    const record = store.addSession({
      scenarioId: sc.id,
      titleJa: sc.titleJa,
      emoji: sc.emoji,
      level,
      startedAt: state.startedAt,
      endedAt,
      userTurns: state.turns.length,
      missionsDone: state.completed.size,
      missionsTotal: sc.missions.length,
      score: null,
    });
    ctx.session.last = {
      recordId: record.id,
      scenario: sc,
      level,
      history,
      turns: state.turns,
      completed: [...state.completed],
      startedAt: state.startedAt,
      endedAt,
    };
    ctx.navigate('#/summary');
  }

  return {
    el,
    fullscreen: true,
    destroy() {
      state.alive = false;
      state.replyAbort?.abort();
      state.listening?.abort();
      stopListening();
      ctx.speaker.cancel();
      cueEl?.remove();
    },
  };
}

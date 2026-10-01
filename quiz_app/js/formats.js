// 出題形式ごとの画面。どの形式も次の形のオブジェクトを返す:
//   prompt   問題カードに入れる要素
//   answer   解答エリアに入れる要素
//   reveal(response, correct)  答え合わせの表示
//   onTick(elapsedMs)          （任意）時間経過で表示を変える形式用
//   focus()                    （任意）出題直後に入力欄へフォーカスする形式用
import { h } from './dom.js';
import { buildPanelTiles, scramble, sortPieces } from './logic.js';
import { chars, shuffle } from './util.js';

const KEYS = ['A', 'B', 'C', 'D', 'E', 'F'];

export function renderFormat(q, submit, context) {
  return RENDERERS[q.type](q, submit, context);
}

const questionText = (text) => h('p', { class: 'q-text' }, text);
const note = (text) => h('p', { class: 'q-note' }, text);
const button = (cls, label, onclick, extra = {}) => h('button', { class: cls, type: 'button', onclick, ...extra }, label);

function markResult(el, isCorrect, isPicked) {
  if (isCorrect) el.classList.add('is-correct');
  else if (isPicked) el.classList.add('is-wrong');
}

function ox(q, submit) {
  const choice = (value, cls, label) =>
    h(
      'button',
      { class: `btn ox-btn ${cls}`, type: 'button', 'aria-label': label, 'data-value': String(value), onclick: () => submit(value) },
      h('i', { class: `mark mark-${cls}`, 'aria-hidden': 'true' }),
    );
  const answer = h('div', { class: 'ox-row' }, choice(true, 'maru', '○ 正しい'), choice(false, 'batsu', '× 誤り'));
  return {
    prompt: [questionText(q.q), note('正しければ○、誤りなら×')],
    answer,
    reveal(response) {
      for (const b of answer.children) {
        const value = b.dataset.value === 'true';
        markResult(b, value === q.answer, value === response);
      }
    },
  };
}

function choiceList(q, submit) {
  const options = shuffle([q.answer, ...shuffle(q.wrong).slice(0, 3)]);
  const answer = h(
    'div',
    { class: 'choice-list' },
    options.map((text, i) =>
      h(
        'button',
        { class: 'btn choice', type: 'button', 'data-value': text, onclick: () => submit(text) },
        h('span', { class: 'key', 'aria-hidden': 'true' }, KEYS[i]),
        h('span', { class: 'choice-text' }, text),
      ),
    ),
  );
  return {
    answer,
    reveal(response) {
      for (const b of answer.children) markResult(b, b.dataset.value === q.answer, b.dataset.value === response);
    },
  };
}

function choice(q, submit) {
  return { prompt: [questionText(q.q)], ...choiceList(q, submit) };
}

// 連想: ヒントが時間とともに1つずつ開く。選択肢は最初から押せるので、早く当てるほど高得点
function rensou(q, submit, { limit }) {
  const list = choiceList(q, submit);
  const interval = (limit * 0.6) / q.hints.length;
  const items = q.hints.map((hint, i) =>
    h('li', { class: i === 0 ? 'hint' : 'hint is-hidden' }, h('span', { class: 'hint-no' }, i + 1), h('span', { class: 'hint-text' }, i === 0 ? hint : '？')),
  );
  let shown = 1;
  const showUpTo = (n) => {
    for (; shown < n; shown++) {
      items[shown].classList.remove('is-hidden');
      items[shown].querySelector('.hint-text').textContent = q.hints[shown];
    }
  };
  return {
    prompt: [questionText(q.q ?? 'ヒントから連想されるものは？'), h('ol', { class: 'hints' }, items)],
    answer: list.answer,
    onTick: (elapsed) => showUpTo(Math.min(q.hints.length, 1 + Math.floor(elapsed / interval))),
    reveal(response) {
      showUpTo(q.hints.length);
      list.reveal(response);
    },
  };
}

// 並べ替え / 文字パネル: タイルを順にタップして答えを組み立てる
function builder(q, submit) {
  const isPanel = q.type === 'panel';
  const tiles = isPanel ? buildPanelTiles(q) : scramble(sortPieces(q));
  const need = isPanel ? chars(q.answer).length : tiles.length;
  const picked = [];

  const slots = Array.from({ length: need }, () => h('span', { class: 'slot' }));
  const tileButtons = tiles.map((text, i) => button('btn tile', text, () => pick(i)));
  const undoButton = button('btn', '1つ戻す', () => {
    picked.pop();
    update();
  });
  const okButton = button('btn primary', '決定', () => submit(picked.map((i) => tiles[i])));

  function pick(i) {
    if (picked.length < need && !picked.includes(i)) {
      picked.push(i);
      update();
    }
  }
  function update() {
    slots.forEach((slot, i) => {
      slot.textContent = i < picked.length ? tiles[picked[i]] : '';
      slot.classList.toggle('filled', i < picked.length);
    });
    tileButtons.forEach((b, i) => {
      const used = picked.includes(i);
      b.disabled = used;
      b.classList.toggle('used', used);
    });
    undoButton.disabled = picked.length === 0;
    okButton.disabled = picked.length !== need;
  }
  update();

  const slotRow = h('div', { class: 'slots', 'aria-label': '解答欄' }, slots);
  const answer = h(
    'div',
    { class: 'builder' },
    slotRow,
    h('div', { class: `tiles ${isPanel ? 'is-panel' : 'is-sort'}` }, tileButtons),
    h('div', { class: 'controls' }, undoButton, okButton),
  );
  return {
    prompt: [questionText(q.q), note(isPanel ? `パネルから${need}文字を順に選ぶ` : 'タップして正しい順に並べる')],
    answer,
    reveal(_response, correct) {
      slotRow.classList.add(correct ? 'is-correct' : 'is-wrong');
    },
  };
}

// 多答: 正しいものをすべて選んで決定
function multi(q, submit) {
  const options = shuffle([...q.correct, ...q.wrong]);
  const selected = new Set();
  const okButton = button('btn primary', '決定', () => submit([...selected]), { disabled: true });
  const optionButtons = options.map((text) => {
    const b = h(
      'button',
      { class: 'btn opt', type: 'button', 'aria-pressed': 'false', 'data-value': text },
      h('span', { class: 'check', 'aria-hidden': 'true' }),
      h('span', { class: 'opt-text' }, text),
    );
    b.addEventListener('click', () => {
      if (selected.has(text)) selected.delete(text);
      else selected.add(text);
      b.setAttribute('aria-pressed', String(selected.has(text)));
      okButton.disabled = selected.size === 0;
    });
    return b;
  });
  return {
    prompt: [questionText(q.q), note('当てはまるものをすべて選んで「決定」')],
    answer: h('div', { class: 'opt-wrap' }, h('div', { class: 'opt-list' }, optionButtons), h('div', { class: 'controls single' }, okButton)),
    reveal() {
      for (const b of optionButtons) {
        const shouldPick = q.correct.includes(b.dataset.value);
        const picked = selected.has(b.dataset.value);
        if (shouldPick) b.classList.add(picked ? 'is-correct' : 'is-missed');
        else if (picked) b.classList.add('is-wrong');
      }
    },
  };
}

// 順番当て: 1番目から順にタップ。最後に選んだものをもう一度タップすると取り消し
function order(q, submit) {
  const items = scramble(q.items);
  const sequence = [];
  const undoButton = button('btn', '1つ戻す', () => {
    sequence.pop();
    update();
  });
  const okButton = button('btn primary', '決定', () => submit(sequence.map((i) => items[i])));
  const itemButtons = items.map((text, i) =>
    h(
      'button',
      {
        class: 'btn opt',
        type: 'button',
        onclick: () => {
          if (!sequence.includes(i)) sequence.push(i);
          else if (sequence.at(-1) === i) sequence.pop();
          update();
        },
      },
      h('span', { class: 'num', 'aria-hidden': 'true' }),
      h('span', { class: 'opt-text' }, text),
      h('span', { class: 'true-num', 'aria-hidden': 'true' }),
    ),
  );
  function update() {
    itemButtons.forEach((b, i) => {
      const pos = sequence.indexOf(i);
      b.querySelector('.num').textContent = pos >= 0 ? pos + 1 : '';
      b.classList.toggle('chosen', pos >= 0);
      b.setAttribute('aria-label', pos >= 0 ? `${pos + 1}番目: ${items[i]}` : items[i]);
    });
    undoButton.disabled = sequence.length === 0;
    okButton.disabled = sequence.length !== items.length;
  }
  update();
  return {
    prompt: [questionText(q.q), note('1番目から順にタップ')],
    answer: h('div', { class: 'opt-wrap' }, h('div', { class: 'opt-list' }, itemButtons), h('div', { class: 'controls' }, undoButton, okButton)),
    reveal(response) {
      itemButtons.forEach((b, i) => {
        const truePos = q.items.indexOf(items[i]);
        b.querySelector('.true-num').textContent = `正解 ${truePos + 1}`;
        markResult(b, response?.[truePos] === items[i], true);
      });
    },
  };
}

// タイピング: ひらがな/カタカナ・全角/半角の違いは正解扱い（normalize.js）
function typing(q, submit) {
  const input = h('input', {
    id: 'answer-input',
    class: 'answer-input',
    type: 'text',
    autocomplete: 'off',
    autocapitalize: 'off',
    spellcheck: 'false',
    enterkeyhint: 'done',
    placeholder: '答えを入力',
    'aria-label': '答えを入力',
  });
  const okButton = h('button', { class: 'btn primary', type: 'submit', disabled: true }, '決定');
  input.addEventListener('input', () => {
    okButton.disabled = input.value.trim() === '';
  });
  const form = h(
    'form',
    {
      class: 'typing-form',
      onsubmit: (event) => {
        event.preventDefault();
        if (input.value.trim() !== '') submit(input.value);
      },
    },
    input,
    okButton,
  );
  return {
    prompt: [questionText(q.q), note('ひらがな・カタカナどちらでもOK')],
    answer: form,
    focus: () => input.focus({ preventScroll: true }),
    reveal(_response, correct) {
      form.classList.add(correct ? 'is-correct' : 'is-wrong');
    },
  };
}

const RENDERERS = { ox, choice, rensou, sort: builder, panel: builder, multi, order, typing };

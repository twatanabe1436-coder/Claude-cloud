// DOM に依存しないテキスト処理 (テスト対象)。
// - 読み上げ用の文分割 (ストリーミング中に文単位で読み上げを始めるため)
// - 発音チェック: お手本の文と音声認識結果を単語単位で突き合わせる

const ABBREVIATIONS = /\b(mr|mrs|ms|dr|st|vs|etc|e\.g|i\.e|jr|sr|no)\.$/i;

/**
 * ストリーミングで届くテキストを、文が完成するたびに取り出す。
 */
export class SentenceChunker {
  constructor() {
    this.buf = '';
  }

  /** @returns {string[]} 完成した文 */
  push(delta) {
    this.buf += delta;
    const out = [];
    // 文末記号 + 空白 で区切る。略語 (Dr. など) の後では区切らない
    const re = /[.!?]+["')\]]*\s+/g;
    let start = 0;
    let m;
    while ((m = re.exec(this.buf))) {
      const end = m.index + m[0].length;
      const piece = this.buf.slice(start, end).trim();
      if (ABBREVIATIONS.test(piece)) continue;
      if (piece) out.push(piece);
      start = end;
    }
    this.buf = this.buf.slice(start);
    return out;
  }

  /** 残りをすべて取り出す */
  flush() {
    const rest = this.buf.trim();
    this.buf = '';
    return rest ? [rest] : [];
  }
}

// ---- 発音チェック ------------------------------------------------------------

const CONTRACTIONS = {
  "i'm": 'i am', "you're": 'you are', "we're": 'we are', "they're": 'they are',
  "he's": 'he is', "she's": 'she is', "it's": 'it is', "that's": 'that is',
  "what's": 'what is', "where's": 'where is', "there's": 'there is', "here's": 'here is',
  "how's": 'how is', "let's": 'let us',
  "i've": 'i have', "you've": 'you have', "we've": 'we have', "they've": 'they have',
  "i'll": 'i will', "you'll": 'you will', "we'll": 'we will', "they'll": 'they will',
  "he'll": 'he will', "she'll": 'she will', "it'll": 'it will',
  "i'd": 'i would', "you'd": 'you would', "we'd": 'we would', "they'd": 'they would',
  "he'd": 'he would', "she'd": 'she would',
  "don't": 'do not', "doesn't": 'does not', "didn't": 'did not', "isn't": 'is not',
  "aren't": 'are not', "wasn't": 'was not', "weren't": 'were not', "can't": 'can not',
  "couldn't": 'could not', "won't": 'will not', "wouldn't": 'would not',
  "shouldn't": 'should not', "haven't": 'have not', "hasn't": 'has not',
  cannot: 'can not', gonna: 'going to', wanna: 'want to',
};

const NUMBERS = [
  'zero', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten',
  'eleven', 'twelve', 'thirteen', 'fourteen', 'fifteen', 'sixteen', 'seventeen', 'eighteen', 'nineteen', 'twenty',
];

/** 比較用に単語を正規化 (小文字化・短縮形の展開・数字の読み) */
function normalizeWord(w) {
  const lw = w.toLowerCase().replace(/[’‘]/g, "'").replace(/^'+|'+$/g, '');
  if (CONTRACTIONS[lw]) return CONTRACTIONS[lw].split(' ');
  if (/^\d+$/.test(lw) && Number(lw) <= 20) return [NUMBERS[Number(lw)]];
  const noPunct = lw.replace(/[^a-z0-9']/g, '');
  return noPunct ? [noPunct] : [];
}

/**
 * お手本の文を「表示用の単語」と「比較用のトークン」に分ける。
 * 短縮形 (I'll) は比較用に2トークン (i, will) になるが、表示は1単語のまま。
 */
function tokenizeTarget(text) {
  const words = text.split(/\s+/).filter(Boolean);
  const tokens = []; // { norm, wordIndex }
  words.forEach((w, wordIndex) => {
    for (const norm of normalizeWord(w)) tokens.push({ norm, wordIndex });
  });
  return { words, tokens };
}

const tokenizeSpoken = (text) => text.split(/\s+/).flatMap(normalizeWord);

/** 最長共通部分列で、お手本のどのトークンが発話に含まれていたかを求める */
function matchedFlags(target, spoken) {
  const n = target.length;
  const m = spoken.length;
  const dp = Array.from({ length: n + 1 }, () => new Uint16Array(m + 1));
  for (let i = n - 1; i >= 0; i--) {
    for (let j = m - 1; j >= 0; j--) {
      dp[i][j] = target[i] === spoken[j] ? dp[i + 1][j + 1] + 1 : Math.max(dp[i + 1][j], dp[i][j + 1]);
    }
  }
  const flags = new Array(n).fill(false);
  let i = 0;
  let j = 0;
  while (i < n && j < m) {
    if (target[i] === spoken[j]) {
      flags[i] = true;
      i++;
      j++;
    } else if (dp[i + 1][j] >= dp[i][j + 1]) i++;
    else j++;
  }
  return flags;
}

/**
 * 発話をお手本と比べて採点する。
 * @param {string} target お手本の英文
 * @param {string} spoken 音声認識の結果
 * @returns {{ score: number, words: { text: string, ok: boolean }[] }}
 */
export function scoreSpeech(target, spoken) {
  const { words, tokens } = tokenizeTarget(target);
  if (!tokens.length) return { score: 0, words: words.map((text) => ({ text, ok: false })) };
  const flags = matchedFlags(
    tokens.map((t) => t.norm),
    tokenizeSpoken(spoken),
  );
  const okByWord = words.map(() => true);
  tokens.forEach((t, i) => {
    if (!flags[i]) okByWord[t.wordIndex] = false;
  });
  const matched = flags.filter(Boolean).length;
  return {
    score: Math.round((matched / tokens.length) * 100),
    words: words.map((text, i) => ({ text, ok: okByWord[i] })),
  };
}

/** 大文字小文字と句読点を無視して同じ文か */
export function sameSentence(a, b) {
  const norm = (s) => tokenizeSpoken(s).join(' ');
  return norm(a) === norm(b);
}

// API キーなしで画面の動作確認ができる「デモモード」用のエンジン。
// 本物の AI ではなく、決まった返答と簡単なルールで動く。

const PARTNER_LINES = [
  "I see! Could you tell me a little more about that?",
  "Oh, that's interesting. Why do you say that?",
  "Got it. And what would you like to do next?",
  "That sounds great! How do you feel about it?",
  "Okay, no problem. Is there anything else?",
];

const GENERIC_PHRASES = [
  { en: 'Could you say that again, please?', ja: 'もう一度言ってもらえますか？' },
  { en: "That sounds interesting! Tell me more.", ja: 'おもしろそう！もっと教えて。' },
  { en: 'What do you think about it?', ja: 'それについてどう思いますか？' },
  { en: "I'm not sure, but I think so.", ja: 'よくわからないけど、そう思います。' },
];

// よくある「直訳っぽい」言い方を自然な言い方に置き換えるルール
const NATURAL_RULES = [
  [/\bI want\b/i, "I'd like"],
  [/\bgive me\b/i, 'could I have'],
  [/\bI think so too\b/i, 'I agree'],
  [/\bvery very\b/i, 'really'],
];

const sleep = (ms, signal) =>
  new Promise((resolve, reject) => {
    const t = setTimeout(resolve, ms);
    signal?.addEventListener('abort', () => {
      clearTimeout(t);
      reject(Object.assign(new Error('aborted'), { name: 'AbortError' }));
    });
  });

function tidy(text) {
  let s = text.trim().replace(/\s+/g, ' ');
  s = s.replace(/\bi\b/g, 'I').replace(/\bi'm\b/gi, "I'm");
  s = s.charAt(0).toUpperCase() + s.slice(1);
  if (!/[.!?]$/.test(s)) s += /^(what|where|when|why|how|who|can|could|do|does|is|are|would)\b/i.test(s) ? '?' : '.';
  return s;
}

export function createMockEngine({ delayMs = 35 } = {}) {
  return {
    name: 'mock',
    model: 'demo',

    async streamReply({ history }, onDelta, signal) {
      const turn = history.filter((m) => m.role === 'user').length;
      const text = PARTNER_LINES[(turn - 1) % PARTNER_LINES.length];
      const words = text.split(' ');
      await sleep(delayMs * 4, signal);
      for (let i = 0; i < words.length; i++) {
        onDelta((i ? ' ' : '') + words[i]);
        await sleep(delayMs, signal);
      }
      return text;
    },

    async feedback({ scenario, history }) {
      const original = history[history.length - 1].text;
      const corrected = tidy(original);
      let natural = corrected;
      for (const [re, rep] of NATURAL_RULES) natural = natural.replace(re, rep);
      natural = natural.charAt(0).toUpperCase() + natural.slice(1);
      const hadErrors = /\bi\b/.test(original);
      const rating = hadErrors ? 'fix' : natural !== corrected ? 'good' : 'great';
      const userTurns = history.filter((m) => m.role === 'user').length;
      return {
        rating,
        corrected,
        natural,
        explanation_ja:
          rating === 'great'
            ? '（デモ）自然に言えています！この調子で文で答えてみましょう。'
            : rating === 'good'
              ? '（デモ）通じますが、こう言うとより自然で丁寧に聞こえます。'
              : '（デモ）「I（私）」は文中でも必ず大文字で書きます。',
        mistakes: hadErrors ? [{ wrong: 'i', right: 'I', note_ja: '「私」は常に大文字の I' }] : [],
        completed_missions: scenario.missions.slice(0, userTurns).map((m) => m.id),
      };
    },

    async hint({ scenario, want }) {
      const pool = scenario.keyPhrases.length ? scenario.keyPhrases : GENERIC_PHRASES;
      const labels = want ? ['シンプル', '自然な言い方', '丁寧'] : ['シンプル', '自然な言い方', '会話を進める'];
      return {
        suggestions: labels.map((label_ja, i) => ({
          label_ja,
          en: pool[i % pool.length].en,
          ja: want ? `（デモ：「${want}」の英訳は API キー設定後に表示されます）` : pool[i % pool.length].ja,
        })),
      };
    },

    async translate() {
      return {
        ja: '（デモモードでは翻訳できません。API キーを設定すると日本語訳が表示されます）',
        words: [],
      };
    },

    async summary({ scenario, history, completedMissions }) {
      const userLines = history.filter((m) => m.role === 'user').map((m) => m.text);
      const avgWords = userLines.reduce((n, t) => n + t.split(/\s+/).length, 0) / Math.max(1, userLines.length);
      const missionRate = scenario.missions.length ? completedMissions.length / scenario.missions.length : 1;
      const score = Math.round(Math.min(95, 45 + avgWords * 3 + missionRate * 25));
      return {
        score,
        headline_ja: '（デモ）最後まで英語で会話できました！',
        good_points_ja: [
          `${userLines.length}回、英語で返答できました。`,
          userLines[0] ? `「${userLines[0]}」のように自分の言葉で話せています。` : '会話に参加できました。',
        ],
        improve_points: [
          { point_ja: '一言で終わらせず、理由や詳しい情報を足してみましょう。', example_en: "I'd like a latte because I need some energy this morning." },
          { point_ja: '丁寧に頼むときは I want より I\'d like を使いましょう。', example_en: "I'd like a medium coffee, please." },
        ],
        key_phrases: (scenario.keyPhrases.length ? scenario.keyPhrases : GENERIC_PHRASES).slice(0, 4),
        next_challenge_ja: '次は1回の返答で2文以上話すことに挑戦してみましょう。',
      };
    },
  };
}

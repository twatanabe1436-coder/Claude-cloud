// タイピング問題の表記ゆれを吸収する。
// 全角/半角・ひらがな/カタカナ・大文字/小文字・空白や記号の違いは同じ答えとみなす。
const DASHES = /[-‐‑‒–—―−]/g; // 長音の代わりに打たれがちな記号
const IGNORED = /[\s・･、。，,！!？?「」『』（）()［］[\]〜~"'“”‘’]/g;

export function normalizeAnswer(text) {
  return String(text ?? '')
    .normalize('NFKC')
    .toLowerCase()
    .replace(/[ァ-ヶ]/g, (c) => String.fromCharCode(c.charCodeAt(0) - 0x60))
    .replace(DASHES, 'ー')
    .replace(IGNORED, '');
}

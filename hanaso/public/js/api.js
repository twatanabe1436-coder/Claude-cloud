// サーバー API の呼び出し。
import { store } from './store.js';

export class ApiError extends Error {
  constructor(message, status = 0, kind = '') {
    super(message);
    this.status = status;
    this.kind = kind;
  }
}

function headers() {
  const h = { 'content-type': 'application/json' };
  if (store.passcode) h['x-hanaso-passcode'] = store.passcode;
  return h;
}

async function request(path, { method = 'GET', body, signal } = {}) {
  let res;
  try {
    res = await fetch(path, { method, headers: headers(), body: body && JSON.stringify(body), signal });
  } catch (err) {
    if (err?.name === 'AbortError') throw err;
    throw new ApiError('サーバーに接続できませんでした。通信状況を確認してください。');
  }
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new ApiError(data.error || `通信エラーが発生しました (${res.status})`, res.status, data.kind);
  }
  return res;
}

const post = async (path, body, opts) => (await request(path, { method: 'POST', body, ...opts })).json();

export const api = {
  config: async () => (await request('/api/config')).json(),
  catalog: async () => (await request('/api/catalog')).json(),
  checkPasscode: () => post('/api/check-passcode', {}),
  feedback: (body, opts) => post('/api/feedback', body, opts),
  hint: (body, opts) => post('/api/hint', body, opts),
  translate: (text, opts) => post('/api/translate', { text }, opts),
  summary: (body, opts) => post('/api/summary', body, opts),

  /**
   * 会話相手の返答をストリーミングで受け取る。
   * @param {object} body
   * @param {{ onDelta: (text: string) => void, signal?: AbortSignal }} opts
   * @returns {Promise<string>} 返答全文
   */
  async reply(body, { onDelta, signal }) {
    const res = await request('/api/reply', { method: 'POST', body, signal });
    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buf = '';
    for (;;) {
      const { value, done } = await reader.read();
      if (value) buf += decoder.decode(value, { stream: !done });
      let nl;
      while ((nl = buf.indexOf('\n')) >= 0) {
        const line = buf.slice(0, nl).trim();
        buf = buf.slice(nl + 1);
        if (!line) continue;
        const ev = JSON.parse(line);
        if (ev.type === 'delta') onDelta(ev.text);
        else if (ev.type === 'done') return ev.text;
        else if (ev.type === 'error') throw new ApiError(ev.message, 0, ev.kind);
      }
      if (done) break;
    }
    throw new ApiError('応答が途中で途切れました。もう一度お試しください。');
  },
};

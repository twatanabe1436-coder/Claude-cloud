import { assemble, checkManifest } from './schema.js';

// cache: 'no-cache' で毎回サーバーに更新を確認する。
// 問題ファイルを差し替えて公開すれば、次に開いたときから新しい問題が出る。
async function fetchJson(url) {
  const res = await fetch(url, { cache: 'no-cache' });
  if (!res.ok) throw new Error(`${url} が見つかりません（HTTP ${res.status}）`);
  return res.json();
}

export async function loadData(base = 'data/') {
  const manifest = await fetchJson(`${base}manifest.json`);
  checkManifest(manifest);
  const results = await Promise.allSettled(manifest.packs.map((path) => fetchJson(base + path)));
  return { manifest, ...assemble(manifest, results) };
}

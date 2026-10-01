// 問題データのチェック: node tools/validate.js
// 問題を追加・修正したら公開前に実行する（GitHub に push すると CI でも自動で実行される）。
import { readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';
import { assemble, checkManifest, TYPES } from '../js/schema.js';

export async function loadLocalData() {
  const base = new URL('../data/', import.meta.url);
  const manifest = JSON.parse(await readFile(new URL('manifest.json', base), 'utf8'));
  checkManifest(manifest);
  const results = await Promise.allSettled(
    manifest.packs.map(async (path) => JSON.parse(await readFile(new URL(path, base), 'utf8'))),
  );
  return { manifest, ...assemble(manifest, results) };
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  const { manifest, questions, problems } = await loadLocalData();
  const table = manifest.genres.map((g) => {
    const row = { ジャンル: g.name };
    for (const [type, label] of Object.entries(TYPES)) {
      row[label] = questions.filter((q) => q.genre === g.id && q.type === type).length;
    }
    row['合計'] = questions.filter((q) => q.genre === g.id).length;
    return row;
  });
  console.table(table);
  console.log(`使える問題: ${questions.length}問`);
  if (problems.length > 0) {
    console.error(`\n直してほしい問題: ${problems.length}件`);
    for (const p of problems) console.error(`  - ${p}`);
    process.exit(1);
  }
}

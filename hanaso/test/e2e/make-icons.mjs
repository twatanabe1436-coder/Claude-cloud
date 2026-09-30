// icons/icon.svg から PNG アイコンを書き出す開発用スクリプト (node test/e2e/make-icons.mjs)。
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import fs from 'node:fs';
import { loadPlaywright } from './pw.mjs';

const { chromium } = await loadPlaywright();

const here = path.dirname(fileURLToPath(import.meta.url));
const iconsDir = path.join(here, '..', '..', 'public', 'icons');
const svg = fs.readFileSync(path.join(iconsDir, 'icon.svg'), 'utf8');

const browser = await chromium.launch();
const page = await browser.newPage();
// 180 (apple-touch-icon) と maskable は OS 側で角丸にするので、角丸なしの正方形で書き出す
const targets = [
  { name: 'icon-180.png', size: 180, square: true },
  { name: 'icon-192.png', size: 192, square: false },
  { name: 'icon-512.png', size: 512, square: false },
  { name: 'icon-maskable-512.png', size: 512, square: true },
];
for (const { name, size, square } of targets) {
  const src = square ? svg.replace('rx="112"', 'rx="0"') : svg;
  await page.setViewportSize({ width: size, height: size });
  await page.setContent(`<style>html,body{margin:0;background:transparent}svg{width:${size}px;height:${size}px;display:block}</style>${src}`);
  await page.screenshot({ path: path.join(iconsDir, name), omitBackground: true });
}
await browser.close();
console.log('icons written');

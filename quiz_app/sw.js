// オフライン対応。常にネットワークを先に見に行き、つながらないときだけ保存済みのものを使う。
// こうしておくと、問題やアプリを更新したときに古いものが表示され続けることがない。
const CACHE = 'quiz-dojo-v1';
const SHELL = [
  './',
  'index.html',
  'css/style.css',
  'js/main.js',
  'js/data.js',
  'js/dom.js',
  'js/formats.js',
  'js/logic.js',
  'js/normalize.js',
  'js/schema.js',
  'js/sound.js',
  'js/storage.js',
  'js/util.js',
  'manifest.webmanifest',
  'icons/icon.svg',
  'data/manifest.json',
];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(SHELL)));
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => Promise.all(keys.filter((key) => key !== CACHE).map((key) => caches.delete(key)))),
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET' || new URL(request.url).origin !== self.location.origin) return;
  event.respondWith(
    fetch(request)
      .then((response) => {
        if (response.ok) {
          const copy = response.clone();
          caches.open(CACHE).then((cache) => cache.put(request, copy));
        }
        return response;
      })
      .catch(() => caches.match(request, { ignoreSearch: true }).then((cached) => cached ?? Response.error())),
  );
});

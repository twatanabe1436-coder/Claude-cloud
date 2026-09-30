// オフラインでも画面の枠だけは開けるようにする Service Worker (ネットワーク優先)。
const CACHE = 'hanaso-v1';
const SHELL = ['/', '/css/app.css', '/js/main.js', '/manifest.webmanifest', '/icons/icon.svg'];

self.addEventListener('install', (e) => {
  self.skipWaiting();
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(SHELL)));
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener('fetch', (e) => {
  const url = new URL(e.request.url);
  if (e.request.method !== 'GET' || url.origin !== location.origin) return;
  // AI を呼ぶ API はキャッシュしない (カタログと設定だけオフライン用に保存)
  if (url.pathname.startsWith('/api/') && !['/api/catalog', '/api/config'].includes(url.pathname)) return;
  e.respondWith(
    fetch(e.request)
      .then((res) => {
        if (res.ok) {
          const copy = res.clone();
          caches.open(CACHE).then((c) => c.put(e.request, copy));
        }
        return res;
      })
      .catch(() => caches.match(e.request).then((r) => r || caches.match('/'))),
  );
});

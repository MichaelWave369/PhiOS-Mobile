/* PhiOS Mobile Rung 1: cache only the public, static shell. No network interception beyond this origin and scope. */
const CACHE = "phios-mobile-operator-r4-v1";
const SHELL = [
  "./", "./index.html", "./styles.css", "./manifest.webmanifest",
  "./assets/icon.svg", "./src/core.mjs", "./src/main.js", "./src/living.mjs", "./src/operator-passport.mjs"
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE).then((cache) => cache.addAll(SHELL)).then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((key) => key.startsWith("phios-mobile-") && key !== CACHE)
        .map((key) => caches.delete(key))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  if (event.request.method !== "GET") return;
  const url = new URL(event.request.url);
  const scope = new URL(self.registration.scope);
  if (url.origin !== scope.origin || !url.pathname.startsWith(scope.pathname)) return;
  event.respondWith((async () => {
    const cached = await caches.match(event.request);
    if (cached) return cached;
    try { return await fetch(event.request); }
    catch {
      if (event.request.mode === "navigate") {
        const fallback = await caches.match(new URL("./index.html", scope));
        if (fallback) return fallback;
      }
      return Response.error();
    }
  })());
});

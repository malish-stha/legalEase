const CACHE_NAME = "legalease-offline-v1";
const DYNAMIC_CACHE = "legalease-dynamic-v1";

const ASSETS_TO_CACHE = [
  "/dashboard",
  "/lawyers",
  "/templates",
];

// Install Event
self.addEventListener("install", (e) => {
  e.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(ASSETS_TO_CACHE);
    })
  );
  self.skipWaiting();
});

// Activate Event
self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME && key !== DYNAMIC_CACHE) {
            return caches.delete(key);
          }
        })
      );
    })
  );
  self.clients.claim();
});

// Fetch Event
self.addEventListener("fetch", (e) => {
  const url = new URL(e.request.url);

  // Cache API GET requests for offline document summaries
  if (e.request.method === "GET" && (url.pathname.includes("/api/documents") || url.pathname.includes("/api/users/health-score"))) {
    e.respondWith(
      fetch(e.request)
        .then((response) => {
          const clonedResponse = response.clone();
          caches.open(DYNAMIC_CACHE).then((cache) => {
            cache.put(e.request, clonedResponse);
          });
          return response;
        })
        .catch(() => {
          return caches.match(e.request);
        })
    );
    return;
  }

  // General Strategy for static assets
  e.respondWith(
    caches.match(e.request).then((cachedResponse) => {
      return (
        cachedResponse ||
        fetch(e.request)
          .then((response) => {
            if (e.request.method === "GET" && (url.pathname.endsWith(".js") || url.pathname.endsWith(".css") || url.pathname.includes("/static/"))) {
              const clonedResponse = response.clone();
              caches.open(DYNAMIC_CACHE).then((cache) => {
                cache.put(e.request, clonedResponse);
              });
            }
            return response;
          })
          .catch(() => {
            if (e.request.mode === "navigate") {
              return caches.match("/dashboard");
            }
          })
      );
    })
  );
});

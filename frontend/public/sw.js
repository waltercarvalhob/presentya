// Service worker da PresentYá: recebe as notificações de ofertas mesmo com o site fechado.

self.addEventListener("install", () => self.skipWaiting());
self.addEventListener("activate", (e) => e.waitUntil(self.clients.claim()));

self.addEventListener("push", (e) => {
  let d = {};
  try { d = e.data ? e.data.json() : {}; } catch { d = { body: e.data ? e.data.text() : "" }; }
  e.waitUntil(self.registration.showNotification(d.title || "PresentYá", {
    body: d.body || "Confira as ofertas de hoje!",
    icon: "/icone-192.png",
    badge: "/icone-192.png",
    tag: "presentya-ofertas",
    renotify: true,
    data: { url: d.url || "/ofertas" },
  }));
});

// Tocar na notificação abre (ou traz para frente) a página de ofertas.
self.addEventListener("notificationclick", (e) => {
  e.notification.close();
  const url = new URL(e.notification.data?.url || "/ofertas", self.location.origin).href;
  e.waitUntil(self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((abertas) => {
    const aba = abertas.find((c) => c.url.startsWith(self.location.origin));
    if (aba) return aba.navigate(url).then((c) => (c || aba).focus()).catch(() => self.clients.openWindow(url));
    return self.clients.openWindow(url);
  }));
});

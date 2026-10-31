import { api } from "./api";

const temSuporte = () => typeof window !== "undefined" && "serviceWorker" in navigator && "PushManager" in window && "Notification" in window;
const iPhone = () => /iPhone|iPad|iPod/.test(navigator.userAgent);
const instalado = () => matchMedia("(display-mode: standalone)").matches || navigator.standalone === true;

/**
 * Em que pé estão as notificações neste aparelho:
 * "ativas" | "pode-ativar" | "bloqueadas" | "iphone-instalar" (iPhone só recebe com o site na Tela de Início) | "sem-suporte"
 */
export async function situacaoPush() {
  if (!temSuporte()) return iPhone() && !instalado() ? "iphone-instalar" : "sem-suporte";
  if (Notification.permission === "denied") return "bloqueadas";
  if (Notification.permission === "granted") {
    const reg = await navigator.serviceWorker.getRegistration("/");
    if (await reg?.pushManager.getSubscription()) return "ativas";
  }
  return "pode-ativar";
}

const chaveBinaria = (b64) => {
  const s = atob((b64 + "=".repeat((4 - (b64.length % 4)) % 4)).replace(/-/g, "+").replace(/_/g, "/"));
  return Uint8Array.from(s, (c) => c.charCodeAt(0));
};
const emTexto = (bytes) => btoa(String.fromCharCode(...new Uint8Array(bytes)));

/** Pede a permissão (precisa vir de um toque da pessoa) e inscreve o aparelho; o servidor já manda as ofertas do dia. */
export async function ativarPush(origem) {
  const reg = await navigator.serviceWorker.register("/sw.js");
  if ((await Notification.requestPermission()) !== "granted") return situacaoPush();
  await navigator.serviceWorker.ready;
  const { chave } = await api("/api/push/chave");
  let inscricao = await reg.pushManager.getSubscription();
  // Inscrição feita com outra chave (ex.: banco recriado) não recebe nada: refaz.
  const atual = inscricao?.options?.applicationServerKey;
  if (inscricao && atual && emTexto(atual) !== emTexto(chaveBinaria(chave))) {
    await inscricao.unsubscribe();
    inscricao = null;
  }
  inscricao ??= await reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: chaveBinaria(chave) });
  await api("/api/push/inscricoes", { method: "POST", body: { ...inscricao.toJSON(), origem } });
  return "ativas";
}

export async function desativarPush() {
  const reg = await navigator.serviceWorker.getRegistration("/");
  const inscricao = await reg?.pushManager.getSubscription();
  if (inscricao) {
    await api("/api/push/cancelar", { method: "POST", body: { endpoint: inscricao.endpoint } }).catch(() => {});
    await inscricao.unsubscribe();
  }
  return situacaoPush();
}

/** Notificação gerada no próprio aparelho (aviso de proximidade), uma vez por visita. */
export async function notificarAqui(titulo, texto) {
  try {
    if (!temSuporte() || Notification.permission !== "granted" || sessionStorage.getItem("py-perto-notificado")) return;
    sessionStorage.setItem("py-perto-notificado", "1");
    const reg = await navigator.serviceWorker.register("/sw.js");
    await reg.showNotification(titulo, { body: texto, icon: "/icone-192.png", tag: "presentya-perto", data: { url: "/ofertas" } });
  } catch { /* sem notificação: a faixa na tela continua aparecendo */ }
}

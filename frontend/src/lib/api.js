// Endereço da API: vazio em desenvolvimento (o Vite repassa /api para a porta 8080).
export const API = (import.meta.env.VITE_API_URL || "").replace(/\/$/, "");

const TOKEN = "py-token";
export const getToken = () => { try { return localStorage.getItem(TOKEN); } catch { return null; } };
export const setToken = (t) => { try { t ? localStorage.setItem(TOKEN, t) : localStorage.removeItem(TOKEN); } catch { /* sem storage */ } };

let aoExpirar = () => {};
export const quandoSessaoExpirar = (fn) => { aoExpirar = fn; };

// A API (plano gratuito do Render) dorme sem visitas e leva até ~2 min para acordar.
// Consultas e login podem ser repetidos sem risco; pedidos e pagamentos nunca são repetidos sozinhos.
const ESPERA_MAX = 120000;
const pausa = (ms) => new Promise((ok) => setTimeout(ok, ms));
const acordando = (status) => status === 502 || status === 503 || status === 504;

export async function api(caminho, { method = "GET", body, form } = {}) {
  const headers = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const repetivel = method === "GET" || caminho.startsWith("/api/auth/");
  const inicio = Date.now();
  let res;
  for (let tentativa = 0; ; tentativa++) {
    try {
      res = await fetch(API + caminho, { method, headers, body: form ?? (body !== undefined ? JSON.stringify(body) : undefined) });
      if (!(repetivel && acordando(res.status) && Date.now() - inicio < ESPERA_MAX)) break;
    } catch {
      if (!repetivel || Date.now() - inicio >= ESPERA_MAX) {
        throw new Error("Não foi possível falar com a loja. Verifique sua internet e tente de novo em 1 minuto.");
      }
    }
    await pausa(Math.min(2000 * 2 ** tentativa, 15000));
  }
  if (res.status === 401 && token) aoExpirar();
  if (res.status === 204) return null;
  const dados = await res.json().catch(() => null);
  if (!res.ok) throw new Error(dados?.erro || "Algo deu errado. Tente novamente.");
  return dados;
}

export const imagemUrl = (url) => (url ? API + url : null);

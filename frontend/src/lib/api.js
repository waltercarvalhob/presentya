// Endereço da API: vazio em desenvolvimento (o Vite repassa /api para a porta 8080).
export const API = (import.meta.env.VITE_API_URL || "").replace(/\/$/, "");

const TOKEN = "py-token";
export const getToken = () => { try { return localStorage.getItem(TOKEN); } catch { return null; } };
export const setToken = (t) => { try { t ? localStorage.setItem(TOKEN, t) : localStorage.removeItem(TOKEN); } catch { /* sem storage */ } };

let aoExpirar = () => {};
export const quandoSessaoExpirar = (fn) => { aoExpirar = fn; };

export async function api(caminho, { method = "GET", body, form } = {}) {
  const headers = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  let res;
  try {
    res = await fetch(API + caminho, { method, headers, body: form ?? (body !== undefined ? JSON.stringify(body) : undefined) });
  } catch {
    throw new Error("Não foi possível falar com a loja. Verifique sua conexão.");
  }
  if (res.status === 401 && token) aoExpirar();
  if (res.status === 204) return null;
  const dados = await res.json().catch(() => null);
  if (!res.ok) throw new Error(dados?.erro || "Algo deu errado. Tente novamente.");
  return dados;
}

export const imagemUrl = (url) => (url ? API + url : null);

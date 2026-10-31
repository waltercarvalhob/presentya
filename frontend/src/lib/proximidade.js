import { useEffect, useState } from "react";

/** Distância em metros entre dois pontos (fórmula de haversine). */
export function distanciaMetros(lat1, lng1, lat2, lng2) {
  const rad = (g) => (g * Math.PI) / 180;
  const dLat = rad(lat2 - lat1), dLng = rad(lng2 - lng1);
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLng / 2) ** 2;
  return 2 * 6371000 * Math.asin(Math.sqrt(a));
}

const num = (v) => { const n = parseFloat(String(v ?? "").replace(",", ".")); return Number.isFinite(n) ? n : null; };

/** Coordenadas e raio configurados em "Dados da loja"; null quando o aviso está desligado. */
export function lojaNoMapa(config) {
  const lat = num(config.loja_latitude), lng = num(config.loja_longitude);
  if (lat == null || lng == null) return null;
  return { lat, lng, raio: num(config.raio_proximidade) || 20 };
}

/** O navegador já deixou usar a localização? (aí dá para acompanhar sem perguntar de novo) */
export async function localizacaoLiberada() {
  try { return (await navigator.permissions.query({ name: "geolocation" })).state === "granted"; }
  catch { return false; }
}

/**
 * Acompanha a distância do celular até a loja enquanto `ligado` for true.
 * estado: "parado" | "sem-suporte" | "buscando" | "negado" | "ok"
 */
export function useProximidade(loja, ligado) {
  const [s, setS] = useState({ estado: "parado", distancia: null, precisao: null });
  useEffect(() => {
    if (!loja || !ligado) return;
    if (!("geolocation" in navigator)) { setS({ estado: "sem-suporte", distancia: null, precisao: null }); return; }
    setS((x) => ({ ...x, estado: x.estado === "ok" ? "ok" : "buscando" }));
    const id = navigator.geolocation.watchPosition(
      (p) => setS({
        estado: "ok",
        distancia: distanciaMetros(p.coords.latitude, p.coords.longitude, loja.lat, loja.lng),
        precisao: p.coords.accuracy,
      }),
      (e) => setS({ estado: e.code === e.PERMISSION_DENIED ? "negado" : "sem-suporte", distancia: null, precisao: null }),
      { enableHighAccuracy: true, maximumAge: 10000, timeout: 30000 },
    );
    return () => navigator.geolocation.clearWatch(id);
  }, [loja?.lat, loja?.lng, ligado]); // eslint-disable-line react-hooks/exhaustive-deps
  const perto = s.estado === "ok" && loja && s.distancia <= loja.raio;
  return { ...s, perto };
}

export const metros = (m) => (m < 1000 ? `${Math.round(m)} m` : `${(m / 1000).toLocaleString("pt-BR", { maximumFractionDigits: 1 })} km`);

/** Texto pronto das ofertas vigentes, para mandar no WhatsApp. */
export function textoOfertas(ofertas, link, nome) {
  const linhas = ofertas.map((o) => `• *${o.titulo}*${o.descricao ? ` – ${o.descricao}` : ""}${o.cupom ? ` (cupom ${o.cupom})` : ""}`);
  return [`Olá${nome ? ", " + nome.split(" ")[0] : ""}! 🎁 Ofertas da PresentYá:`, ...linhas, "", `Veja todas: ${link}`].join("\n");
}

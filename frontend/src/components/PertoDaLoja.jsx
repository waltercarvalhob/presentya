import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { localizacaoLiberada, lojaNoMapa, metros, useProximidade } from "../lib/proximidade";
import { notificarAqui } from "../lib/push";
import { useLoja } from "../lib/store";

const FECHADO = "py-perto-fechado";

/**
 * Faixa "você está perto da loja" em qualquer página do site. Só acompanha quem já liberou a
 * localização antes (na página /ofertas): aqui nunca pedimos permissão.
 */
export default function PertoDaLoja() {
  const { config } = useLoja();
  const { pathname } = useLocation();
  const loja = lojaNoMapa(config);
  const [liberada, setLiberada] = useState(false);
  const [fechado, setFechado] = useState(() => { try { return !!sessionStorage.getItem(FECHADO); } catch { return false; } });
  useEffect(() => { if (loja) localizacaoLiberada().then(setLiberada); }, [!!loja]); // eslint-disable-line react-hooks/exhaustive-deps
  const { perto, distancia } = useProximidade(loja, liberada && !fechado && pathname !== "/ofertas");
  useEffect(() => { if (perto) notificarAqui("📍 Você está pertinho da PresentYá!", config.mensagem_proximidade); }, [perto]); // eslint-disable-line react-hooks/exhaustive-deps
  if (!perto || fechado || pathname === "/ofertas") return null;
  const fechar = () => { setFechado(true); try { sessionStorage.setItem(FECHADO, "1"); } catch { /* sem storage */ } };
  return (
    <div className="perto-faixa" role="status">
      <span>📍 <b>{config.mensagem_proximidade}</b> <small>({metros(distancia)})</small></span>
      <Link className="btn btn-dark btn-sm" to="/ofertas" onClick={fechar}>Ver ofertas</Link>
      <button className="x" onClick={fechar} aria-label="Fechar aviso">×</button>
    </div>
  );
}

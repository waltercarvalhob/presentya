import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";

const FECHADO = "py-app-fechado";
const IOS = /iPhone|iPad|iPod/.test(navigator.userAgent) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
const instalado = () => matchMedia("(display-mode: standalone)").matches || navigator.standalone === true;
const lerFechado = () => { try { return !!localStorage.getItem(FECHADO); } catch { return false; } };

// Android e computador: o navegador avisa que dá para instalar e guardamos o convite para o botão.
let convite = null;
const ouvintes = new Set();
addEventListener("beforeinstallprompt", (e) => { e.preventDefault(); convite = e; ouvintes.forEach((f) => f()); });
addEventListener("appinstalled", () => { convite = null; ouvintes.forEach((f) => f()); });

/** Faixa "Instale o app da PresentYá": instala direto no Android/computador; no iPhone mostra o passo a passo. */
export default function InstalarApp() {
  const { pathname } = useLocation();
  const [, atualizar] = useState(0);
  const [fechado, setFechado] = useState(lerFechado);
  const [visivel, setVisivel] = useState(false);
  useEffect(() => {
    const f = () => atualizar((n) => n + 1);
    ouvintes.add(f);
    const t = setTimeout(() => setVisivel(true), 12000);
    return () => { ouvintes.delete(f); clearTimeout(t); };
  }, []);

  if (!visivel || fechado || instalado() || pathname.startsWith("/admin") || (!convite && !IOS)) return null;
  const fechar = () => { setFechado(true); try { localStorage.setItem(FECHADO, "1"); } catch { /* sem storage */ } };
  const instalar = async () => {
    convite.prompt();
    const { outcome } = await convite.userChoice;
    convite = null;
    if (outcome === "accepted") fechar(); else atualizar((n) => n + 1);
  };
  return (
    <div className="perto-faixa app-faixa" role="dialog" aria-label="Instalar o app">
      <img src="/icone-192.png" alt="" width={40} height={40} />
      {convite ? (
        <>
          <span><b>Instale o app da PresentYá</b><br /><small>Ofertas e avisos direto no celular.</small></span>
          <button className="btn btn-gold btn-sm" onClick={instalar}>Instalar</button>
        </>
      ) : (
        <span><b>Instale o app da PresentYá:</b> toque em <b>Compartilhar</b> <span aria-hidden="true">(□↑)</span> e depois em <b>Adicionar à Tela de Início</b>.</span>
      )}
      <button className="x" onClick={fechar} aria-label="Fechar">×</button>
    </div>
  );
}

import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";

const FECHADO = "py-app-fechado";
const UA = navigator.userAgent;
const IOS = /iPhone|iPad|iPod/.test(UA) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
const ANDROID = /Android/.test(UA);
// Navegador embutido do WhatsApp, Instagram, Facebook…: não instala apps.
const EMBUTIDO = /FBAN|FBAV|Instagram|WhatsApp|Line\/|; wv\)/.test(UA);
const instalado = () => matchMedia("(display-mode: standalone)").matches || navigator.standalone === true;
const lerFechado = () => { try { return !!localStorage.getItem(FECHADO); } catch { return false; } };

// Android e computador: o navegador avisa que dá para instalar e guardamos o convite para os botões.
let convite = null;
const ouvintes = new Set();
const avisar = () => ouvintes.forEach((f) => f());
addEventListener("beforeinstallprompt", (e) => { e.preventDefault(); convite = e; avisar(); });
addEventListener("appinstalled", () => { convite = null; avisar(); });

function useConvite() {
  const [, atualizar] = useState(0);
  useEffect(() => {
    const f = () => atualizar((n) => n + 1);
    ouvintes.add(f);
    return () => ouvintes.delete(f);
  }, []);
  return convite;
}

async function instalarAgora() {
  const c = convite;
  c.prompt();
  const { outcome } = await c.userChoice;
  convite = null;
  avisar();
  return outcome === "accepted";
}

/** Passo a passo para quando o navegador não oferece o botão de instalar. */
function ComoInstalar({ fechar }) {
  return (
    <div className="app-ajuda" role="dialog" aria-modal="true" aria-label="Como instalar o app" onClick={fechar}>
      <div className="app-ajuda-box" onClick={(e) => e.stopPropagation()}>
        <img src="/icone-192.png" alt="" width={56} height={56} />
        <h3>Instalar o app da PresentYá</h3>
        {EMBUTIDO ? (
          <p>Você abriu a loja <b>dentro de outro app</b> (WhatsApp, Instagram…), e por aqui não dá para instalar.
            Toque nos <b>três pontinhos ⋮</b> (ou em <b>Compartilhar</b>) e escolha <b>Abrir no {IOS ? "Safari" : "Chrome"}</b>. Depois toque em “Instalar app” de novo.</p>
        ) : IOS ? (
          <ol>
            <li>Abra a loja no <b>Safari</b>.</li>
            <li>Toque em <b>Compartilhar</b> <span aria-hidden="true">(□↑)</span>, na barra de baixo.</li>
            <li>Role e toque em <b>Adicionar à Tela de Início</b> → <b>Adicionar</b>.</li>
          </ol>
        ) : ANDROID ? (
          <ol>
            <li>Abra a loja no <b>Google Chrome</b>.</li>
            <li>Toque nos <b>três pontinhos ⋮</b>, no canto de cima.</li>
            <li>Toque em <b>Instalar app</b> (ou <b>Adicionar à tela inicial</b>) → <b>Instalar</b>.</li>
          </ol>
        ) : (
          <p>No Chrome ou no Edge do computador, clique no ícone de <b>instalar</b> no fim da barra de endereço, ou no menu ⋮ → <b>Instalar PresentYá</b>.</p>
        )}
        <p><small>Acabou de desinstalar? Feche o navegador por completo e abra a loja de novo antes de instalar.</small></p>
        <button className="btn btn-gold" onClick={fechar}>Entendi</button>
      </div>
    </div>
  );
}

/** Botão fixo "Instalar app" (menu e rodapé): instala direto quando dá; senão mostra o passo a passo. */
export function BotaoInstalar({ className = "", children = "📲 Instalar app" }) {
  const c = useConvite();
  const [ajuda, setAjuda] = useState(false);
  if (instalado()) return null;
  return (
    <>
      <button type="button" className={className} onClick={() => (c ? instalarAgora() : setAjuda(true))}>{children}</button>
      {ajuda && <ComoInstalar fechar={() => setAjuda(false)} />}
    </>
  );
}

/** Faixa "Instale o app da PresentYá", que aparece sozinha uma vez; o botão fixo continua no menu e no rodapé. */
export default function InstalarApp() {
  const { pathname } = useLocation();
  const c = useConvite();
  const [fechado, setFechado] = useState(lerFechado);
  const [visivel, setVisivel] = useState(false);
  const [ajuda, setAjuda] = useState(false);
  useEffect(() => { const t = setTimeout(() => setVisivel(true), 12000); return () => clearTimeout(t); }, []);

  if (!visivel || fechado || instalado() || pathname.startsWith("/admin") || (!c && !IOS && !EMBUTIDO)) return null;
  const fechar = () => { setFechado(true); try { localStorage.setItem(FECHADO, "1"); } catch { /* sem storage */ } };
  return (
    <div className="perto-faixa app-faixa" role="dialog" aria-label="Instalar o app">
      <img src="/icone-192.png" alt="" width={40} height={40} />
      <span><b>Instale o app da PresentYá</b><br /><small>Ofertas e avisos direto no celular.</small></span>
      <button className="btn btn-gold btn-sm" onClick={async () => { if (!c) return setAjuda(true); if (await instalarAgora()) fechar(); }}>Instalar</button>
      <button className="x" onClick={fechar} aria-label="Fechar">×</button>
      {ajuda && <ComoInstalar fechar={() => setAjuda(false)} />}
    </div>
  );
}

import { useEffect, useState } from "react";
import { Outlet, useLocation } from "react-router-dom";
import Header from "./Header";
import Drawers from "./Drawers";
import Logo from "./Logo";
import { Icon, SecLink } from "./nav";
import { useLoja } from "../lib/store";
import { RM } from "../lib/util";

function Footer() {
  const { config } = useLoja();
  return (
    <>
      <section className="comeback" aria-label="Convite">
        <div className="wrap"><p className="hand">Não deixe de conferir nossas novidades: sempre tem algo especial chegando!</p><SecLink id="novidades" className="btn btn-line btn-sm">Ver novidades</SecLink></div>
      </section>
      <footer className="site">
        <div className="wrap foot">
          <div style={{ display: "grid", gap: ".9rem", alignContent: "start", justifyItems: "start" }}>
            <SecLink id="inicio" className="foot-logo" aria-label="PresentYá, início"><Logo /></SecLink>
            <p>PresentYá • Estilo • Criatividade. Presentes e personalizados em São Luís – MA, com envio para todo o Brasil.</p>
          </div>
          <div><h4>Loja</h4><ul>
            <li><SecLink id="produtos">Nossos Produtos</SecLink></li><li><SecLink id="ideias">Ideias de Presente</SecLink></li>
            <li><SecLink id="novidades">Novidades</SecLink></li><li><SecLink id="clube">Clube Fidelidade</SecLink></li>
          </ul></div>
          <div><h4>Ajuda</h4><ul>
            <li><SecLink id="como-comprar">Como Comprar</SecLink></li><li><SecLink id="entrega">Entrega e Devolução</SecLink></li>
            <li><SecLink id="faq">Perguntas Frequentes</SecLink></li><li><SecLink id="sobre">Nossa história</SecLink></li>
          </ul></div>
          <div><h4>Contato</h4><ul>
            <li>{config.whatsapp_exibicao}</li><li>{config.email}</li>
            <li><a href={`https://instagram.com/${config.instagram}`} target="_blank" rel="noopener">@{config.instagram}</a></li>
            <li>São Luís – MA</li>
          </ul></div>
        </div>
        <div className="wrap legal"><span>© {new Date().getFullYear()} PresentYá · {config.site}</span><span>Compra segura · Embalagem especial · Cartão grátis</span></div>
      </footer>
    </>
  );
}

function Floating() {
  const { toastMsg, wa } = useLoja();
  const [topo, setTopo] = useState(false);
  useEffect(() => {
    const f = () => setTopo(scrollY > 900);
    addEventListener("scroll", f, { passive: true });
    return () => removeEventListener("scroll", f);
  }, []);
  return (
    <>
      <button className={"round totop" + (topo ? " show" : "")} onClick={() => scrollTo({ top: 0, behavior: RM ? "auto" : "smooth" })} aria-label="Voltar ao topo">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M6 15l6-6 6 6" /></svg>
      </button>
      <a className="wa-float" href={wa("Olá, PresentYá! Quero ajuda para escolher um presente.")} target="_blank" rel="noopener" aria-label="Falar no WhatsApp">
        {Icon.wa}<span className="lbl">Fale conosco</span>
      </a>
      {toastMsg && <div className="toast" role="status">{toastMsg}</div>}
    </>
  );
}

export default function Layout() {
  const { pathname } = useLocation();
  useEffect(() => { if (pathname !== "/") scrollTo(0, 0); }, [pathname]);
  return (
    <>
      <Header />
      <main><Outlet /></main>
      <Footer />
      <Floating />
      <Drawers />
    </>
  );
}

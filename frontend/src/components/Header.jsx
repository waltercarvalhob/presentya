import { useEffect, useRef, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import Logo from "./Logo";
import { Icon, SecLink, useIrCatalogo } from "./nav";
import { useLoja } from "../lib/store";

const SECS = [["inicio", "Início"], ["produtos", "Produtos"], ["ideias", "Ideias"], ["sobre", "Sobre"], ["clube", "Clube"], ["novidades", "Novidades"], ["contato", "Contato"], ["ajuda", "Ajuda"]];
const MOB = { inicio: "Início", produtos: "Nossos Produtos", ideias: "Ideias de Presente", sobre: "Por que comprar conosco", clube: "Clube Fidelidade", novidades: "Novidades e Ofertas", contato: "Contato", ajuda: "Ajuda" };

function fimDaSemana() {
  const d = new Date(), e = new Date(d);
  e.setDate(d.getDate() + ((7 - d.getDay()) % 7));
  e.setHours(23, 59, 59, 0);
  return e;
}

/** Barra preta do topo: produto com maior desconto + contagem regressiva até domingo. */
function OfferBar() {
  const { produtos, config } = useLoja();
  const [, tick] = useState(0);
  useEffect(() => { const t = setInterval(() => tick((x) => x + 1), 30000); return () => clearInterval(t); }, []);
  const oferta = produtos.filter((p) => p.old).sort((a, b) => b.old / b.price - a.old / a.price)[0];
  if (!oferta) {
    return (
      <div className="offerbar" role="region" aria-label="Avisos">
        <div className="wrap"><span>{config.aviso_topo || <>Cartão grátis em todo pedido · <b>Entrega em {config.cidade_entrega || "São Luís"}</b> e envio para todo o Brasil</>}</span></div>
      </div>
    );
  }
  const ms = fimDaSemana() - new Date();
  const d = Math.floor(ms / 864e5), h = Math.floor(ms / 36e5) % 24, m = Math.floor(ms / 6e4) % 60;
  const pct = Math.round((1 - oferta.price / oferta.old) * 100);
  return (
    <div className="offerbar" role="region" aria-label="Ofertas">
      <div className="wrap">
        <span className="pill">Oferta limitada</span>
        <span><b>{oferta.name}</b> com {pct}% de desconto</span>
        <span className="count">termina em {d}d {String(h).padStart(2, "0")}h {String(m).padStart(2, "0")}min</span>
      </div>
    </div>
  );
}

function CatBar() {
  const { categorias, filtro } = useLoja();
  const ir = useIrCatalogo();
  if (!categorias.length) return null;
  return (
    <nav className="catbar" aria-label="Categorias">
      <div className="wrap">
        <ul>
          <li><button className={filtro.novidade ? "on" : ""} onClick={() => ir({ novidade: true, sort: "new" })}>Lançamentos</button></li>
          {categorias.map((c) => (
            <li key={c.id}>
              <button className={filtro.cat === c.slug ? "on" : ""} onClick={() => ir({ cat: c.slug })} aria-haspopup={c.filhas.length ? "true" : undefined}>{c.nome}</button>
              {c.filhas.length > 0 && (
                <div className="sub">
                  <button onClick={() => ir({ cat: c.slug })}>Ver tudo em {c.nome}</button>
                  {c.filhas.map((f) => <button key={f.id} onClick={() => ir({ cat: f.slug })}>{f.nome}</button>)}
                </div>
              )}
            </li>
          ))}
        </ul>
      </div>
    </nav>
  );
}

export default function Header() {
  const { wish, cart, usuario, equipe, setDrawer } = useLoja();
  const { pathname } = useLocation();
  const home = pathname === "/";
  const [cur, setCur] = useState("inicio");
  const [scrolled, setScrolled] = useState(false);
  const [menu, setMenu] = useState(false);
  const ulRef = useRef(), pillRef = useRef();
  const qtdCart = cart.reduce((s, i) => s + i.qtd, 0);

  useEffect(() => {
    const spy = () => {
      setScrolled(scrollY > 10);
      if (!home) return;
      const y = scrollY + 140;
      let c = "inicio";
      SECS.forEach(([id]) => { const s = document.getElementById(id); if (s && s.offsetTop <= y) c = id; });
      setCur(c);
    };
    spy();
    addEventListener("scroll", spy, { passive: true });
    addEventListener("resize", spy);
    return () => { removeEventListener("scroll", spy); removeEventListener("resize", spy); };
  }, [home]);

  useEffect(() => {
    const ul = ulRef.current, p = pillRef.current;
    if (!ul || !p) return;
    const a = home && ul.querySelector(`a[data-sec="${cur}"]`);
    if (!a) { p.style.width = "0"; return; }
    const r = a.getBoundingClientRect(), u = ul.getBoundingClientRect();
    p.style.left = r.left - u.left + "px";
    p.style.width = r.width + "px";
  }, [cur, home, scrolled]);

  useEffect(() => setMenu(false), [pathname]);

  return (
    <>
      <OfferBar />
      <header className={"site" + (scrolled ? " scrolled" : "")} id="hdr">
        <div className="wrap hdr">
          <Link className="logo" to="/" aria-label="PresentYá, início"><Logo /></Link>
          <ul className="navlinks" ref={ulRef}>
            <li className="navpill" ref={pillRef} aria-hidden="true" />
            {SECS.map(([id, t]) => <li key={id}><SecLink id={id} data-sec={id} className={home && cur === id ? "on" : ""}>{t}</SecLink></li>)}
          </ul>
          <div className="hdr-actions">
            <button className="icon-btn" onClick={() => setDrawer("wish")} aria-label="Abrir lista de desejos">
              {Icon.heart}<span className="t">Desejos</span><span className={"badge" + (wish.length ? " bump" : "")} key={wish.length}>{wish.length}</span>
            </button>
            <button className="icon-btn" onClick={() => setDrawer("cart")} aria-label="Abrir sacola de compras">
              {Icon.bag}<span className="t">Sacola</span><span className={"badge" + (qtdCart ? " bump" : "")} key={qtdCart}>{qtdCart}</span>
            </button>
            <Link className="icon-btn" to={usuario ? "/minha-conta" : "/entrar"} aria-label={usuario ? "Minha conta" : "Entrar"}>
              {Icon.user}<span className="t">{usuario ? usuario.nome.split(" ")[0] : "Entrar"}</span>
            </Link>
            {equipe ? <Link className="btn btn-dark btn-sm" to="/admin">Painel</Link> : <SecLink id="produtos" className="btn btn-dark btn-sm">Comprar</SecLink>}
            <button className="icon-btn menu-btn" onClick={() => setMenu(!menu)} aria-expanded={menu} aria-controls="mobnav" aria-label="Abrir menu">{Icon.menu}</button>
          </div>
        </div>
        <nav className={"mobnav wrap" + (menu ? " open" : "")} id="mobnav" aria-label="Menu">
          <ul>
            {SECS.map(([id]) => <li key={id}><SecLink id={id} className={home && cur === id ? "on" : ""} onClick={() => setMenu(false)}>{MOB[id]}</SecLink></li>)}
            {equipe && <li><Link to="/admin">Painel administrativo</Link></li>}
          </ul>
        </nav>
      </header>
      <CatBar />
    </>
  );
}

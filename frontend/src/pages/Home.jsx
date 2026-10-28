import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import ProductCard from "../components/ProductCard";
import { Icon, useIrCatalogo } from "../components/nav";
import { Art } from "../lib/art";
import { FILTRO_VAZIO, useLoja } from "../lib/store";
import { brl, OCC, PARA, PRICES, RM } from "../lib/util";

/* ---------- efeitos visuais do site original ---------- */

function useReveal(pronto) {
  useEffect(() => {
    if (!pronto || RM || !("IntersectionObserver" in window)) return;
    const vh = innerHeight;
    const io = new IntersectionObserver((es) => es.forEach((e) => {
      if (!e.isIntersecting) return;
      const el = e.target;
      const sib = [...el.parentElement.children].filter((c) => c.classList.contains("rv"));
      el.style.transitionDelay = Math.max(0, sib.indexOf(el)) * 90 + "ms";
      el.classList.remove("pre");
      io.unobserve(el);
    }), { threshold: 0.15, rootMargin: "0px 0px -40px" });
    document.querySelectorAll(".rv").forEach((el) => {
      if (el.getBoundingClientRect().top > vh * 0.9) { el.classList.add("pre"); io.observe(el); }
    });
    return () => io.disconnect();
  }, [pronto]);
}

function CountUp({ n }) {
  const [v, setV] = useState(RM ? n : 0);
  useEffect(() => {
    if (RM) { setV(n); return; }
    let raf, st = performance.now();
    const f = (t) => { const k = Math.min(1, (t - st) / 1200); setV(Math.round(n * (1 - Math.pow(1 - k, 3)))); if (k < 1) raf = requestAnimationFrame(f); };
    raf = requestAnimationFrame(f);
    return () => cancelAnimationFrame(raf);
  }, [n]);
  return <b>{v}</b>;
}

const svg = (paths, w = "1.7") => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={w} dangerouslySetInnerHTML={{ __html: paths }} />;

/* ---------- seções ---------- */

function Hero({ produtos }) {
  const ordenados = useMemo(() => [...produtos].sort((a, b) => (b.tags.length - a.tags.length) || b.pop - a.pop), [produtos]);
  const show = ordenados.slice(0, 6);
  const orbits = ordenados.slice(6, 9);
  const [si, setSi] = useState(0);
  const [prev, setPrev] = useState(-1);
  const ir = useIrCatalogo();
  const ir2 = (n) => { setPrev(si); setSi((n + show.length) % show.length); };
  useEffect(() => {
    if (RM || show.length < 2) return;
    const t = setTimeout(() => { setPrev(si); setSi((si + 1) % show.length); }, 3200);
    return () => clearTimeout(t);
  }, [show.length, si]);
  const atual = show[si];
  const palavras = ["Todo", "presente", "guarda", "um"];
  return (
    <section className="hero" id="inicio">
      <span className="blob b1" /><span className="blob b2" /><span className="blob b3" />
      <div className="wrap hero-grid">
        <div className="hero-copy">
          <span className="eyebrow">Presentes em São Luís – MA</span>
          <h1>
            {palavras.map((w, i) => <span key={w}><span className="word" style={{ animationDelay: i * 70 + "ms" }}>{w}</span> </span>)}
            <span className="word" style={{ animationDelay: "280ms" }}><em>“lembrei de você”</em></span>
            <span className="word" style={{ animationDelay: "350ms" }}>.</span>
          </h1>
          <p className="lead">Escolhemos, personalizamos e embrulhamos com carinho para que abrir o pacote seja tão especial quanto quem vai receber.</p>
          <div className="hero-ctas">
            <button className="btn btn-gold" onClick={() => ir()}>Escolher um presente</button>
            <a className="btn btn-line" href="#ideias">Preciso de ideias</a>
          </div>
          <div className="stats">
            <div className="stat"><CountUp n={produtos.length} /><span>presentes no catálogo</span></div>
            <div className="stat"><CountUp n={Object.keys(OCC).length} /><span>ocasiões especiais</span></div>
            <div className="stat"><b>Grátis</b><span>cartão em todo pedido</span></div>
          </div>
        </div>
        <div className="stage" aria-label="Vitrine de produtos">
          <div className="ring" /><div className="ring r2" /><div className="disc" />
          <div className="showcase">
            {show.map((p, i) => <div key={p.id} className={"show-item" + (i === si ? " on" : "") + (i === prev && i !== si ? " out" : "")}><Art p={p} /></div>)}
          </div>
          {orbits.map((p, i) => <div key={p.id} className={"orbit o" + (i + 1)} style={{ background: p.bg }}><Art p={p} /></div>)}
          <div className="tag t1"><span className="hand">com amor ♡</span></div>
          {atual && <div className="show-label"><b>{atual.name}</b><span>{brl(atual.price)}</span></div>}
          <div className="dots">
            {show.map((p, i) => <button key={p.id} aria-label={"Ver " + p.name} className={i === si ? "on" : ""} onClick={() => ir2(i)} />)}
          </div>
        </div>
      </div>
    </section>
  );
}

function Carousel({ produtos }) {
  const ref = useRef(), prog = useRef();
  const lista = useMemo(() => [...produtos].sort((a, b) => (b.tags.includes("off") - a.tags.includes("off")) || (b.pop - a.pop)).slice(0, 8), [produtos]);
  useEffect(() => {
    const c = ref.current;
    if (!c) return;
    const upd = () => {
      const max = c.scrollWidth - c.clientWidth, f = c.clientWidth / c.scrollWidth;
      prog.current.style.width = f * 100 + "%";
      prog.current.style.transform = `translateX(${max ? (c.scrollLeft / max) * ((1 - f) / f) * 100 : 0}%)`;
    };
    c.addEventListener("scroll", upd, { passive: true }); upd();
    let t;
    const auto = () => { clearInterval(t); if (!RM) t = setInterval(() => passo(1), 4200); };
    const parar = () => clearInterval(t);
    c.addEventListener("mouseenter", parar); c.addEventListener("mouseleave", auto); c.addEventListener("touchstart", parar, { passive: true });
    auto();
    return () => { parar(); c.removeEventListener("scroll", upd); c.removeEventListener("mouseenter", parar); c.removeEventListener("mouseleave", auto); };
  }, [lista]);
  const passo = (d) => {
    const c = ref.current;
    if (!c?.firstElementChild) return;
    const w = c.firstElementChild.getBoundingClientRect().width + 22, max = c.scrollWidth - c.clientWidth;
    if (d > 0 && c.scrollLeft >= max - 4) c.scrollTo({ left: 0 }); else c.scrollBy({ left: d * w });
  };
  return (
    <section className="sec tint">
      <div className="wrap">
        <div className="head split rv">
          <div><span className="eyebrow">Mais amados</span><h2>Mais vendidos e novidades</h2></div>
          <div className="car-ctl">
            <button className="round" onClick={() => passo(-1)} aria-label="Anterior"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M15 6l-6 6 6 6" /></svg></button>
            <button className="round" onClick={() => passo(1)} aria-label="Próximo"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M9 6l6 6-6 6" /></svg></button>
          </div>
        </div>
        <div className="carousel" ref={ref}>{lista.map((p, i) => <ProductCard key={p.id} p={p} i={i} />)}</div>
        <div className="progress"><i ref={prog} /></div>
      </div>
    </section>
  );
}

const OCC_UI = [
  ["aniversario", "Mais um ano para celebrar", "var(--gold-soft)", '<path d="M4 11h16v9H4zM12 11v9M6 11V8h12v3M12 8V5"/>'],
  ["namoro", "Para quem faz seu coração bater", "var(--rose-soft)", '<path d="M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z"/>'],
  ["natal", "Mimos para a família toda", "var(--sage-soft)", '<path d="M12 3l6 8h-3l4 6H5l4-6H6zM12 17v4"/>'],
  ["maes", "Para quem cuida de você", "var(--rose-soft)", '<circle cx="12" cy="8" r="3"/><path d="M6 20c0-4 3-7 6-7s6 3 6 7"/>'],
  ["pais", "Para o herói de todo dia", "var(--gold-soft)", '<circle cx="12" cy="7" r="3"/><path d="M6 21v-3a6 6 0 0 1 12 0v3M10 13l2 3 2-3"/>'],
  ["corporativo", "Clientes, equipes e eventos", "var(--sky)", '<rect x="4" y="8" width="16" height="11" rx="2"/><path d="M9 8V5h6v3"/>'],
];

function Catalogo() {
  const { produtos, categorias, filtro, setFiltro } = useLoja();
  const f = filtro;
  const set = (patch) => setFiltro({ ...f, ...patch });
  const toggle = (k, v) => set({ [k]: f[k].includes(v) ? f[k].filter((x) => x !== v) : [...f[k], v] });

  const slugsDaCategoria = useMemo(() => {
    if (!f.cat) return null;
    const s = new Set([f.cat]);
    categorias.forEach((c) => { if (c.slug === f.cat) c.filhas.forEach((x) => s.add(x.slug)); });
    return s;
  }, [f.cat, categorias]);
  const nomeCat = useMemo(() => {
    for (const c of categorias) { if (c.slug === f.cat) return c.nome; const x = c.filhas.find((y) => y.slug === f.cat); if (x) return `${c.nome} › ${x.nome}`; }
    return null;
  }, [f.cat, categorias]);

  const lista = useMemo(() => {
    const l = produtos.filter((p) => {
      if (slugsDaCategoria && !slugsDaCategoria.has(p.categoriaSlug)) return false;
      if (f.novidade && !p.novidade) return false;
      if (f.occ.length && !p.occ.some((o) => f.occ.includes(o))) return false;
      if (f.para.length && !p.para.some((o) => f.para.includes(o))) return false;
      if (f.perso && !p.perso) return false;
      if (f.price) { const [lo, hi] = f.price.split("-").map(Number); if (p.price < lo || p.price > hi) return false; }
      return true;
    });
    const ord = { pop: (a, b) => b.pop - a.pop, new: (a, b) => b.novo - a.novo, low: (a, b) => a.price - b.price, high: (a, b) => b.price - a.price };
    return l.sort(ord[f.sort]);
  }, [produtos, f, slugsDaCategoria]);

  const Chips = ({ k, mapa }) => Object.entries(mapa).map(([v, t]) => (
    <button key={v} className="chip" aria-pressed={k === "price" ? f.price === v : f[k].includes(v)}
      onClick={() => (k === "price" ? set({ price: f.price === v ? null : v }) : toggle(k, v))}>{t}</button>
  ));

  return (
    <>
      <div className="head rv"><span className="eyebrow">Nossos produtos</span><h2>Encontre o presente certo</h2><p className="lead">Filtre por categoria, ocasião, para quem e valor. Toque no coração para salvar.</p></div>
      <div className="chipbar"><span className="lbl">Ocasião</span><span className="chips"><Chips k="occ" mapa={OCC} /></span></div>
      <div className="chipbar"><span className="lbl">Para quem</span><span className="chips"><Chips k="para" mapa={PARA} /></span></div>
      <div className="chipbar"><span className="lbl">Valor</span><span className="chips"><Chips k="price" mapa={PRICES} /></span></div>
      <div className="shop-top" id="catalogo">
        <span className="count-txt">
          {lista.length} {lista.length === 1 ? "presente encontrado" : "presentes encontrados"}
          {nomeCat && <> em <b style={{ color: "var(--ink)" }}>{nomeCat}</b></>}
          {f.novidade && <> · <b style={{ color: "var(--ink)" }}>Lançamentos</b></>}
        </span>
        <div style={{ display: "flex", gap: "1rem", alignItems: "center", flexWrap: "wrap" }}>
          <select value={f.cat || ""} onChange={(e) => set({ cat: e.target.value || null })} aria-label="Categoria">
            <option value="">Todas as categorias</option>
            {categorias.map((c) => c.filhas.length ? (
              <optgroup key={c.id} label={c.nome}>
                <option value={c.slug}>Tudo em {c.nome}</option>
                {c.filhas.map((x) => <option key={x.id} value={x.slug}>{x.nome}</option>)}
              </optgroup>
            ) : <option key={c.id} value={c.slug}>{c.nome}</option>)}
          </select>
          <label className="switch"><input type="checkbox" checked={f.perso} onChange={(e) => set({ perso: e.target.checked })} /> Só personalizados</label>
          <select value={f.sort} onChange={(e) => set({ sort: e.target.value })} aria-label="Ordenar">
            <option value="pop">Mais vendidos</option><option value="new">Novidades</option><option value="low">Menor preço</option><option value="high">Maior preço</option>
          </select>
          <button className="chip" onClick={() => setFiltro(FILTRO_VAZIO)}>Limpar</button>
        </div>
      </div>
      <div className="grid" key={JSON.stringify(f)}>
        {lista.length ? lista.map((p, i) => <ProductCard key={p.id} p={p} i={i} className="entra" />) : (
          <div className="empty"><p>Nenhum presente com esses filtros.</p><p>Fale com a gente no WhatsApp que encontramos a opção ideal.</p><button className="btn btn-line btn-sm" onClick={() => setFiltro(FILTRO_VAZIO)}>Limpar filtros</button></div>
        )}
      </div>
    </>
  );
}

const CUR = {
  ela: { t: "Para Ela", txt: "Fotos, cuidados e mimos que fazem ela se sentir lembrada.", f: { para: ["ela"] } },
  ele: { t: "Para Ele", txt: "Presentes práticos com toque pessoal, para usar todo dia.", f: { para: ["ele"] } },
  criancas: { t: "Para Crianças", txt: "Fofura, cor e o nome deles em destaque.", f: { para: ["criancas"] } },
  familia: { t: "Para a Família", txt: "Para dividir na mesa, na sala e nas memórias.", f: { para: ["familia"] } },
  exclusivos: { t: "Presentes Exclusivos", txt: "Peças 100% personalizadas, montadas para uma pessoa só.", f: { perso: true } },
};

function Ideias() {
  const { produtos } = useLoja();
  const ir = useIrCatalogo();
  const [k, setK] = useState("ela");
  const c = CUR[k];
  const itens = produtos.filter((p) => (c.f.perso ? p.perso : p.para.includes(c.f.para[0]))).sort((a, b) => b.pop - a.pop).slice(0, 4);
  return (
    <section className="sec" id="ideias">
      <div className="wrap">
        <div className="head rv"><span className="eyebrow">Ideias de presente</span><h2>Curadoria para quem você ama</h2></div>
        <div className="tabs" role="tablist">
          {Object.entries(CUR).map(([kk, v]) => <button key={kk} role="tab" aria-selected={kk === k} onClick={() => setK(kk)}>{v.t}</button>)}
        </div>
        <div className="curation">
          <div className="intro"><span className="eyebrow">Curadoria</span><h3 style={{ fontSize: "1.7rem" }}>{c.t}</h3><p style={{ color: "var(--muted)" }}>{c.txt}</p><div><button className="btn btn-gold btn-sm" onClick={() => ir(c.f)}>Ver todos</button></div></div>
          <div className="mini-grid" key={k}>
            {itens.map((p, i) => (
              <button className="mini" key={p.id} style={{ animationDelay: i * 80 + "ms" }} onClick={() => ir(c.f)}>
                <div className="art" style={{ background: p.bg }}><Art p={p} /></div>
                <div className="t"><b>{p.name}</b><span>{brl(p.price)}</span></div>
              </button>
            ))}
          </div>
        </div>

        <div className="tips">
          <div className="tipcard rv"><span className="hand">Dica 1</span><h3>Do que a pessoa fala sempre?</h3><p>Café, plantas, viagens, um time. O assunto preferido é a melhor pista.</p></div>
          <div className="tipcard rv"><span className="hand">Dica 2</span><h3>Ela vai usar ou guardar?</h3><p>Uso diário pede algo prático. Lembrança pede algo personalizado.</p></div>
          <div className="tipcard rv"><span className="hand">Dica 3</span><h3>Qual memória vocês dividem?</h3><p>Uma data, uma foto, uma frase. Isso vale mais que o preço.</p></div>
        </div>

        <div className="subhead rv"><span className="eyebrow">O que cada presente diz</span></div>
        <div className="meaning">
          {[["Caneca personalizada", "Penso em você logo no primeiro café."], ["Quadro de fotos", "Nossa história merece ficar à vista."], ["Kit spa", "Você merece uma pausa."],
            ["Cesta de café da manhã", "Quero começar o seu dia com um sorriso."], ["Luminária com nome", "Você ilumina a minha casa."], ["Caixa surpresa", "Cada detalhe foi pensado em você."]]
            .map(([b, p]) => <div className="rv" key={b}><b>{b}</b><p>“{p}”</p></div>)}
        </div>

        <div className="subhead rv"><span className="eyebrow">Por orçamento</span></div>
        <div className="budget">
          {[["0-50", "Até R$ 50", ["Chaveiro personalizado", "Caneca com foto", "Cartão presente"]],
            ["50-120", "R$ 50 a R$ 120", ["Quadro de memórias", "Coração de bombons", "Luminária com nome"]],
            ["120-9999", "Acima de R$ 120", ["Cesta café da manhã", "Caixa surpresa", "Kit Natal em família"]]].map(([v, t, l]) => (
            <div className="b rv" key={v}><span className="range">{t}</span><ul>{l.map((x) => <li key={x}>{x}</li>)}</ul><button className="btn btn-line btn-sm" onClick={() => ir({ price: v })}>Ver opções</button></div>
          ))}
        </div>
      </div>
    </section>
  );
}

function Clube() {
  const { usuario } = useLoja();
  const ir = useIrCatalogo();
  const ref = useRef();
  const [n, setN] = useState(RM ? 2 : 0);
  useEffect(() => {
    if (RM || !ref.current) return;
    let timers = [];
    const io = new IntersectionObserver(([e]) => {
      if (!e.isIntersecting) return;
      io.disconnect();
      for (let i = 1; i <= 5; i++) timers.push(setTimeout(() => setN(i), 300 + (i - 1) * 450));
    }, { threshold: 0.3 });
    io.observe(ref.current);
    return () => { io.disconnect(); timers.forEach(clearTimeout); };
  }, []);
  const perks = [
    ['<path d="M5 19L19 5M7 7h.01M17 17h.01"/>', "Descontos progressivos", "Quanto mais compra, maior o desconto do seu nível."],
    ['<rect x="4" y="9" width="16" height="11" rx="1"/><path d="M12 9v11M4 13h16"/>', "Surpresa na 5ª compra", "Completou a cartela, ganhou um mimo."],
    ['<path d="M12 3l2.6 5.6 6 .7-4.5 4.1 1.2 6-5.3-3-5.3 3 1.2-6L3.4 9.3l6-.7z"/>', "Acesso antecipado", "Veja as novidades antes de todo mundo."],
    ['<path d="M4 11h16v9H4zM12 11v9M6 11V8h12v3M12 8V5"/>', "Mês do aniversário", "Uma oferta especial esperando por você."],
  ];
  return (
    <section className="sec rose" id="clube">
      <div className="wrap club">
        <div className="rv l">
          <div className="head" style={{ marginBottom: "1rem" }}><span className="eyebrow">Clube PresentYá</span><h2>A cada compra, você ganha pontos que viram descontos e mimos!</h2></div>
          <div className="perks">
            {perks.map(([ic, b, p]) => <div className="perk" key={b}><span className="ic">{svg(ic, "2")}</span><p><b>{b}</b>{p}</p></div>)}
          </div>
          <div className="form" style={{ marginTop: "1.8rem" }}>
            <h3>{usuario ? `Olá, ${usuario.nome.split(" ")[0]}! Você tem ${usuario.pontos} pontos.` : "Cadastre-se e comece a acumular agora. É grátis!"}</h3>
            <p style={{ color: "var(--muted)", fontSize: ".95rem" }}>Seus pontos e sua cartela são registrados automaticamente a cada compra paga no site. Na loja física, apresente seu cartão fidelidade.</p>
            <div style={{ display: "flex", gap: ".6rem", flexWrap: "wrap" }}>
              {usuario ? <Link className="btn btn-dark" to="/minha-conta">Ver minha cartela</Link> : <Link className="btn btn-dark" to="/cadastro">Quero entrar no Clube</Link>}
              <button className="btn btn-line" onClick={() => ir({ cat: "cartao-presente" })}>{Icon.gift} Cartão Presente</button>
            </div>
            <small>Não sabe o que dar? O <b>Cartão Presente PresentYá</b> deixa a pessoa escolher.</small>
          </div>
        </div>
        <div className="stampcard rv r" ref={ref} aria-label="Cartela de fidelidade de exemplo">
          <div className="top"><h3>Sua cartela</h3><span className="hand">5ª compra = surpresa</span></div>
          <div className="stamps">
            {[1, 2, 3, 4].map((i) => <div key={i} className={"stamp" + (n >= i ? " done" : "")}>{i}ª</div>)}
            <div className={"stamp gift" + (n >= 5 ? " win" : "")} title="Presente surpresa">{Icon.gift}</div>
          </div>
          <p style={{ color: "var(--muted)", fontSize: ".9rem" }}>R$ 1 em compras = 1 ponto · 100 pontos = R$ 10 de desconto</p>
          <div className="tiers">
            <div className="tier"><b>5%</b><small>Nível Laço<br />da 2ª compra</small></div>
            <div className="tier"><b>8%</b><small>Nível Fita<br />da 5ª compra</small></div>
            <div className="tier"><b>10%</b><small>Nível Ouro<br />da 10ª compra</small></div>
          </div>
        </div>
      </div>
    </section>
  );
}

function Novidades() {
  const { produtos, wa } = useLoja();
  const [email, setEmail] = useState("");
  const [ok, setOk] = useState(false);
  const oferta = produtos.filter((p) => p.old).sort((a, b) => b.old / b.price - a.old / a.price)[0];
  const fresh = [...produtos].sort((a, b) => b.novo - a.novo).filter((p) => p !== oferta).slice(0, oferta ? 2 : 3);
  const row = (p, when, sub) => (
    <div className="drop" key={p.id}><span className="ic" style={{ background: p.bg }}><Art p={p} /></span><div><span className="when">{when}</span><br /><b>{p.name}</b><br /><small style={{ color: "var(--muted)" }}>{sub}</small></div></div>
  );
  return (
    <section className="sec" id="novidades">
      <div className="wrap news">
        <div className="rv l" style={{ display: "grid", gap: "1.2rem" }}>
          <span className="eyebrow">Novidades e ofertas</span>
          <h2>Receba ideias de presentes e descontos exclusivos por e-mail</h2>
          <p style={{ color: "var(--muted)" }}>Não enviamos spam, apenas coisas boas 💌</p>
          <form className="form" onSubmit={(e) => { e.preventDefault(); setOk(true); }}>
            <label>Seu melhor e-mail<input type="email" required autoComplete="email" placeholder="voce@email.com" value={email} onChange={(e) => setEmail(e.target.value)} /></label>
            <button className="btn btn-gold" type="submit">Quero receber</button>
            {ok && <div className="ok">Quase lá! Confirme seu cadastro com a loja: <a href={wa(`Olá, PresentYá! Quero receber novidades e ofertas por e-mail.\nE-mail: ${email}`)} target="_blank" rel="noopener"><b>enviar pelo WhatsApp</b></a>.</div>}
          </form>
        </div>
        <div className="rv r" style={{ display: "grid", gap: "1rem" }}>
          <h3>Esta semana na loja</h3>
          <div className="drops">
            {oferta && row(oferta, "Oferta da semana", `De ${brl(oferta.old)} por ${brl(oferta.price)}`)}
            {fresh.map((p) => row(p, "Chegou", "Novidade na loja"))}
          </div>
          <p className="hand">Toda semana uma nova oferta limitada</p>
        </div>
      </div>
    </section>
  );
}

function Contato() {
  const { config, wa, toast } = useLoja();
  const copiar = (v) => { try { navigator.clipboard.writeText(v).then(() => toast("Copiado: " + v), () => toast(v)); } catch { toast(v); } };
  const loja = (n) => (
    <div className="cbox rv"><span className="k">Loja · {config.horario_resumo}</span><span className="big">{config[`loja${n}_nome`]}</span>
      <p style={{ whiteSpace: "pre-line" }}>{config[`loja${n}_horario`]}</p></div>
  );
  return (
    <section className="sec tint" id="contato">
      <div className="wrap">
        <div className="head rv"><span className="eyebrow">Contato e localização</span><h2>Fale com a PresentYá</h2></div>
        <div className="contact">
          <div className="cbox rv"><span className="k">WhatsApp</span><span className="big">{config.whatsapp_exibicao}</span>
            <a className="btn btn-wa btn-sm" href={wa("Olá, PresentYá! Vim pelo site.")} target="_blank" rel="noopener">Abrir conversa</a>
            <button className="copy" onClick={() => copiar(config.whatsapp_exibicao)}>Copiar número</button></div>
          <div className="cbox rv"><span className="k">E-mail</span><span className="big">{config.email}</span>
            <button className="copy" onClick={() => copiar(config.email)}>Copiar e-mail</button><p>Respondemos em até 1 dia útil.</p></div>
          <div className="cbox rv"><span className="k">Instagram</span><span className="big">@{config.instagram}</span>
            <a className="btn btn-line btn-sm" href={`https://instagram.com/${config.instagram}`} target="_blank" rel="noopener">Ver o Instagram</a></div>
          {loja(1)}
          {loja(2)}
          <div className="cbox rv"><span className="k">Entregas e site</span><span className="big">São Luís – MA</span>
            <p>Entrega na cidade com hora marcada e envio para todo o Brasil. Pedidos pelo site ({config.site}) a qualquer hora.</p></div>
        </div>
      </div>
    </section>
  );
}

function Ajuda() {
  return (
    <section className="sec" id="ajuda">
      <div className="wrap">
        <div className="head rv" id="como-comprar"><span className="eyebrow">Como comprar</span><h2>Do clique ao laço em 4 passos</h2></div>
        <div className="howto">
          <div className="s rv"><span className="n">1</span><b>Escolha</b><p>Navegue, salve favoritos no coração e coloque na sacola.</p></div>
          <div className="s rv"><span className="n">2</span><b>Personalize</b><p>Escreva o nome, data ou frase direto na sacola.</p></div>
          <div className="s rv"><span className="n">3</span><b>Pague com segurança</b><p>Cartão de crédito, débito ou Pix, aqui mesmo no site.</p></div>
          <div className="s rv"><span className="n">4</span><b>Receba</b><p>Embrulhado, com cartão, no horário combinado ou retire na loja.</p></div>
        </div>
        <div className="help2">
          <div className="policy rv" id="entrega">
            <h3 style={{ fontSize: "1.6rem" }}>Entrega e Devolução</h3>
            <h4>Entrega em São Luís – MA</h4>
            <ul><li>Pronta entrega: no mesmo dia ou no seguinte, conforme o horário do pedido.</li><li>Personalizados: prazo contado a partir da aprovação da arte.</li><li>Também é possível retirar no Pátio Norte Shopping ou no Quiosque Mix Mateus Forquilha.</li></ul>
            <h4>Envio para todo o Brasil</h4>
            <ul><li>Correios ou transportadora, com código de rastreio.</li><li>Prazo e frete calculados pelo CEP.</li></ul>
            <h4>Trocas e devoluções</h4>
            <ul><li>Compras pela internet podem ser canceladas em até 7 dias após o recebimento, conforme o Código de Defesa do Consumidor.</li><li>Personalizados são feitos sob medida e só são trocados em caso de defeito ou erro de produção.</li><li>Recebeu com defeito? Envie fotos pelo WhatsApp em até 7 dias.</li></ul>
          </div>
          <div className="rv" id="faq">
            <h3 style={{ fontSize: "1.6rem", marginBottom: ".4rem" }}>Perguntas frequentes</h3>
            <details><summary>Quanto tempo leva um personalizado?</summary><p>Informamos o prazo exato no atendimento. A produção começa depois que você aprova a arte.</p></details>
            <details><summary>Posso enviar direto para outra pessoa?</summary><p>Sim. Informe o endereço de quem recebe e a mensagem do cartão. Não colocamos preço na embalagem.</p></details>
            <details><summary>O cartão é mesmo grátis?</summary><p>Sim, todo pedido pode levar um cartão com a sua mensagem.</p></details>
            <details><summary>Quais as formas de pagamento?</summary><p>Pix, cartão de crédito (em até 12x) e cartão de débito, direto no site, em ambiente seguro do Mercado Pago. A loja não guarda os dados do seu cartão.</p></details>
            <details><summary>Vocês têm cartão presente?</summary><p>Sim! O Cartão Presente PresentYá vale em qualquer produto da loja. Você encontra na categoria Cartão Presente.</p></details>
            <details><summary>Fazem kits para empresas?</summary><p>Sim, com a identidade da sua empresa, para clientes, equipes e eventos.</p></details>
            <details><summary>Como funciona a lista de desejos?</summary><p>Toque no coração do produto. A lista fica salva neste navegador e pode ser enviada pelo WhatsApp.</p></details>
          </div>
        </div>
      </div>
    </section>
  );
}

/* ---------- página ---------- */

export default function Home() {
  const { produtos, carregando, erro, recarregar } = useLoja();
  const ir = useIrCatalogo();
  const { hash } = useLocation();
  useReveal(!carregando);

  useEffect(() => {
    if (carregando || !hash) return;
    const el = document.getElementById(hash.slice(1));
    if (el) setTimeout(() => el.scrollIntoView({ behavior: RM ? "auto" : "smooth", block: "start" }), 60);
  }, [hash, carregando]);

  if (carregando) return <section className="sec"><div className="wrap empty"><p className="hand">Preparando a vitrine…</p></div></section>;
  if (erro) return <section className="sec"><div className="wrap empty"><p>{erro}</p><button className="btn btn-gold" onClick={recarregar}>Tentar de novo</button></div></section>;

  const perso = produtos.filter((p) => p.perso && p.kind !== "card").slice(0, 3);
  return (
    <>
      <Hero produtos={produtos} />

      <div className="marquee" aria-hidden="true">
        <div className="track">
          {[0, 1].map((k) => produtos.slice(0, 14).map((p) => (
            <span className="mq" key={k + "-" + p.id}><span className="ic" style={{ background: p.bg }}><Art p={p} /></span>{p.name}<i>✦</i></span>
          )))}
        </div>
      </div>

      <section className="sec">
        <div className="wrap">
          <div className="trio">
            <div className="feat rv"><div className="ic">{svg('<path d="M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z"/>')}</div><h3>Presentes que tocam o coração</h3><p>Cada peça é escolhida pensando na emoção de quem recebe.</p></div>
            <div className="feat rv"><div className="ic">{svg('<path d="M4 20l4-1 11-11-3-3L5 16l-1 4z"/><path d="M14 6l3 3"/>')}</div><h3>Personalização única</h3><p>Nome, foto, data ou uma frase só de vocês. Ninguém terá um igual.</p></div>
            <div className="feat rv"><div className="ic">{svg('<path d="M3 7h11v9H3zM14 10h4l3 3v3h-7"/><circle cx="7" cy="17.5" r="1.8"/><circle cx="17" cy="17.5" r="1.8"/>')}</div><h3>Entrega rápida em São Luís-MA</h3><p>Levamos até a porta com hora combinada. Enviamos para todo o Brasil.</p></div>
          </div>
        </div>
      </section>

      <Carousel produtos={produtos} />

      <section className="sec">
        <div className="wrap">
          <div className="head center rv"><span className="eyebrow">Ideias por ocasião</span><h2>Qual é o motivo da surpresa?</h2></div>
          <div className="occ">
            {OCC_UI.map(([k, s, bg, ic]) => (
              <button className="rv" key={k} onClick={() => ir({ occ: [k] })}><span className="dot" style={{ background: bg }}>{svg(ic)}</span><b>{OCC[k]}</b><small>{s}</small></button>
            ))}
          </div>
        </div>
      </section>

      <section className="sec tint" id="produtos">
        <div className="wrap">
          <div className="perso rv z" id="personalizados">
            <div style={{ display: "grid", gap: "1rem" }}>
              <span className="eyebrow">Destaque especial</span>
              <h2>Presentes Personalizados</h2>
              <p>O que transforma um objeto em lembrança é o detalhe que só vocês entendem.</p>
              <div><button className="btn btn-dark" onClick={() => ir({ perso: true })}>Ver personalizados</button></div>
            </div>
            <div className="perso-stage" aria-hidden="true">
              {perso.map((p) => <div className="pc" key={p.id} style={{ background: p.bg }}><Art p={p} /></div>)}
            </div>
          </div>
          <Catalogo />
        </div>
      </section>

      <Ideias />

      <section className="sec tint" id="sobre">
        <div className="wrap">
          <div className="story">
            <div className="story-copy rv l">
              <span className="eyebrow">Nossa história</span>
              <h2>Uma loja de São Luís feita para quem gosta de cuidar dos outros</h2>
              <p>A PresentYá nasceu de uma ideia simples: o presente certo faz alguém se sentir lembrado. Por isso cada pedido passa pelas nossas mãos, do laço ao cartão.</p>
              <p>Aqui você conversa com gente de verdade, pelo WhatsApp, e recebe ajuda para escolher. Estilo e criatividade em cada detalhe.</p>
            </div>
            <div className="letter rv r">
              <p className="hand">Querido cliente,</p>
              <p style={{ marginTop: ".6rem", color: "var(--muted)" }}>cada embrulho que sai daqui leva um pouco do nosso carinho. Obrigado por escolher a PresentYá para fazer alguém feliz.</p>
              <p className="hand" style={{ marginTop: "1rem" }}>Equipe PresentYá</p>
            </div>
          </div>
          <div className="diffs">
            <div className="diff rv"><div className="ic">{svg('<rect x="4" y="9" width="16" height="11" rx="1"/><path d="M12 9v11M4 13h16M12 9c-2-4-6-3-5 0M12 9c2-4 6-3 5 0"/>')}</div><h3>Embalagem especial</h3><p>Papel, fita e laço escolhidos para a ocasião.</p></div>
            <div className="diff rv"><div className="ic">{svg('<path d="M4 6h16v12H4zM4 7l8 6 8-6"/>')}</div><h3>Cartão grátis</h3><p>Sua mensagem num cartão bonito, sem custo.</p></div>
            <div className="diff rv"><div className="ic">{svg('<path d="M4 5h16v11H8l-4 4z"/>')}</div><h3>Atendimento próximo</h3><p>Fale direto com a loja e aprove a arte antes.</p></div>
            <div className="diff rv"><div className="ic">{svg('<path d="M12 21s-6-5.5-6-11a6 6 0 0 1 12 0c0 5.5-6 11-6 11z"/><circle cx="12" cy="10" r="2.2"/>')}</div><h3>Entrega na cidade</h3><p>São Luís com hora marcada, e envio para o Brasil.</p></div>
          </div>
        </div>
      </section>

      <section className="sec">
        <div className="wrap"><div className="head center rv"><span className="eyebrow">Quem recebeu, conta</span><h2>Depoimentos de clientes</h2><span className="note-ex">Modelos: troque pelos comentários reais dos seus clientes</span></div></div>
        <div className="tmarquee">
          <div className="track">
            {[0, 1].map((k) => [["[Depoimento sobre a embalagem e a reação de quem recebeu.]", "Nome · bairro, São Luís"], ["[Depoimento sobre a personalização com nome ou foto.]", "Nome · bairro, São Luís"], ["[Depoimento sobre a entrega no horário combinado.]", "Nome · cidade"], ["[Depoimento sobre o atendimento pelo WhatsApp.]", "Nome · bairro, São Luís"], ["[Depoimento sobre um presente corporativo.]", "Nome · empresa"]]
              .map(([t, n], i) => <figure className="quote" key={k + "-" + i}><span className="stars">★★★★★</span><p>“{t}”</p><footer>{n}</footer></figure>))}
          </div>
        </div>
        <div className="wrap seals">
          <span className="seal rv">{svg('<rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/>', "1.8")}Compra segura</span>
          <span className="seal rv">{svg('<path d="M3 7h11v9H3zM14 10h4l3 3v3h-7"/>', "1.8")}Entrega rápida</span>
          <span className="seal rv">{svg('<rect x="4" y="9" width="16" height="11" rx="1"/><path d="M12 9v11M4 13h16"/>', "1.8")}Embalagem especial</span>
        </div>
      </section>

      <Clube />
      <Novidades />
      <Contato />
      <Ajuda />
    </>
  );
}

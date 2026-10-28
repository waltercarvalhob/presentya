import { useRef, useState } from "react";
import { Art } from "../lib/art";
import { useLoja } from "../lib/store";
import { brl, OCC, RM } from "../lib/util";
import { Icon } from "./nav";

const FL = { new: ["new", "Novidade"], hot: ["hot", "Mais vendido"], off: ["off", "Oferta"] };
const semHover = typeof matchMedia !== "undefined" && matchMedia("(hover:none)").matches;

export default function ProductCard({ p, i = 0, className = "" }) {
  const { wish, toggleWish, addCart, wa } = useLoja();
  const [pop, setPop] = useState(0);
  const ref = useRef();
  const salvo = wish.includes(p.id);
  const esgotado = p.estoque === 0;
  const msg = `Olá, PresentYá! Quero comprar: ${p.name} (${brl(p.price)}). Vim pelo site.`;

  const tilt = (e) => {
    if (RM || semHover) return;
    const el = ref.current, r = el.getBoundingClientRect();
    const x = (e.clientX - r.left) / r.width, y = (e.clientY - r.top) / r.height;
    el.style.transform = `perspective(900px) rotateX(${(0.5 - y) * 8}deg) rotateY(${(x - 0.5) * 10}deg) translateY(-6px)`;
    const a = el.querySelector(".art");
    a.style.setProperty("--mx", x * 100 + "%");
    a.style.setProperty("--my", y * 100 + "%");
  };

  return (
    <article className={"prod " + className} ref={ref} onMouseMove={tilt} onMouseLeave={() => (ref.current.style.transform = "")}>
      <div className="art" style={{ background: p.bg, "--d": -(i % 4) * 1.1 + "s" }}>
        <Art p={p} />
        <div className="flags">
          {p.tags.map((t) => <span key={t} className={"flag " + FL[t][0]}>{FL[t][1]}</span>)}
          {p.perso && <span className="flag">Personalizável</span>}
          {esgotado && <span className="flag">Esgotado</span>}
        </div>
        <button className={"heart" + (pop ? " pop" : "")} key={pop} aria-pressed={salvo} aria-label={`Salvar ${p.name} na lista de desejos`}
          onClick={() => { toggleWish(p.id); setPop((x) => x + 1); }}>{Icon.heart}</button>
        <div className="quick"><a className="btn btn-gold" target="_blank" rel="noopener" href={wa(msg)} tabIndex={-1}>Compra rápida no WhatsApp</a></div>
      </div>
      <div className="body">
        <span className="meta">{p.occ.map((o) => OCC[o]).filter(Boolean).join(" · ") || p.categoriaNome}</span>
        <h3>{p.name}</h3>
        <p className="desc">{p.desc}</p>
        <div className="price-row">
          <span className="price">{p.old && <s>{brl(p.old)}</s>}{brl(p.price)}</span>
          <button className="btn btn-gold btn-sm" disabled={esgotado} onClick={() => addCart(p.id)}>{esgotado ? "Esgotado" : "Comprar Agora"}</button>
        </div>
      </div>
    </article>
  );
}

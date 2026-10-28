import { useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { Art } from "../lib/art";
import { useLoja } from "../lib/store";
import { brl } from "../lib/util";
import { useIrCatalogo } from "./nav";

function Wish({ fechar }) {
  const { wish, porId, toggleWish, wa, addCart } = useLoja();
  const ir = useIrCatalogo();
  const items = wish.map((id) => porId.get(id)).filter(Boolean);
  const txt = "Olá, PresentYá! Esta é a minha lista de desejos:\n" + items.map((p) => `• ${p.name} (${brl(p.price)})`).join("\n");
  const dica = "Dica de presente pra mim — PresentYá (presentya.com.br):\n" + items.map((p) => "• " + p.name).join("\n");
  return (
    <>
      <p style={{ color: "var(--muted)", fontSize: ".9rem" }}>Salva neste navegador. Envie para a loja ou mande como dica para alguém.</p>
      <div style={{ display: "grid", gap: ".7rem" }}>
        {items.length === 0 && <div className="empty"><p>Sua lista está vazia.</p><p className="hand">Toque no coração dos produtos que você amar.</p></div>}
        {items.map((p, i) => (
          <div className="wl-item" key={p.id} style={{ animationDelay: i * 60 + "ms" }}>
            <div className="art" style={{ background: p.bg }}><Art p={p} /></div>
            <div><b>{p.name}</b><span>{brl(p.price)}</span></div>
            <div style={{ display: "grid", gap: ".3rem", justifyItems: "end" }}>
              <button onClick={() => addCart(p.id)}>sacola</button>
              <button onClick={() => toggleWish(p.id)}>remover</button>
            </div>
          </div>
        ))}
      </div>
      <div style={{ display: "grid", gap: ".6rem", marginTop: "auto" }}>
        {items.length ? (
          <>
            <a className="btn btn-wa" target="_blank" rel="noopener" href={wa(txt)}>Enviar lista para a loja</a>
            <a className="btn btn-line" target="_blank" rel="noopener" href={"https://wa.me/?text=" + encodeURIComponent(dica)}>Mandar como dica para alguém</a>
          </>
        ) : (
          <button className="btn btn-gold" onClick={() => { fechar(); ir(); }}>Ver produtos</button>
        )}
      </div>
    </>
  );
}

function Cart({ fechar }) {
  const { cart, setQtd, setPerso, subtotal } = useLoja();
  const nav = useNavigate();
  const ir = useIrCatalogo();
  return (
    <>
      <div style={{ display: "grid", gap: ".7rem" }}>
        {cart.length === 0 && <div className="empty"><p>Sua sacola está vazia.</p><p className="hand">Que tal escolher um presente?</p></div>}
        {cart.map((i) => (
          <div className="cart-item" key={i.id}>
            <div className="art" style={{ background: i.p.bg }}><Art p={i.p} /></div>
            <div className="info">
              <b>{i.p.name}</b>
              <div className="linha">
                <div className="qty">
                  <button onClick={() => setQtd(i.id, i.qtd - 1)} aria-label="Diminuir">−</button>
                  <span>{i.qtd}</span>
                  <button onClick={() => setQtd(i.id, i.qtd + 1)} aria-label="Aumentar">+</button>
                </div>
                <span className="price" style={{ fontSize: "1rem" }}>{brl(i.p.price * i.qtd)}</span>
              </div>
              {i.p.perso && (
                <input value={i.perso} maxLength={500} onChange={(e) => setPerso(i.id, e.target.value)}
                  placeholder="Personalização: nome, data ou frase" aria-label={`Personalização de ${i.p.name}`} />
              )}
              <button className="link-btn" style={{ justifySelf: "start" }} onClick={() => setQtd(i.id, 0)}>remover</button>
            </div>
          </div>
        ))}
      </div>
      <div style={{ display: "grid", gap: ".6rem", marginTop: "auto" }}>
        {cart.length ? (
          <>
            <div className="total-box"><div><span>Subtotal</span><b>{brl(subtotal)}</b></div><small style={{ color: "var(--muted)" }}>Frete calculado no próximo passo. Cartão grátis!</small></div>
            <button className="btn btn-gold" onClick={() => { fechar(); nav("/checkout"); }}>Finalizar compra</button>
            <button className="btn btn-line" onClick={fechar}>Continuar comprando</button>
          </>
        ) : (
          <button className="btn btn-gold" onClick={() => { fechar(); ir(); }}>Ver produtos</button>
        )}
      </div>
    </>
  );
}

export default function Drawers() {
  const { drawer, setDrawer } = useLoja();
  const closeRef = useRef();
  const fechar = () => setDrawer(null);
  useEffect(() => {
    if (!drawer) return;
    closeRef.current?.focus();
    const esc = (e) => e.key === "Escape" && setDrawer(null);
    addEventListener("keydown", esc);
    return () => removeEventListener("keydown", esc);
  }, [drawer, setDrawer]);
  if (!drawer) return null;
  const titulo = drawer === "cart" ? "Minha sacola" : "Minha lista de desejos";
  return (
    <div className="drawer" onClick={(e) => e.target === e.currentTarget && fechar()}>
      <div className="panel" role="dialog" aria-modal="true" aria-labelledby="drTitle">
        <div className="panel-head"><h3 id="drTitle">{titulo}</h3><button className="close" ref={closeRef} onClick={fechar} aria-label="Fechar">×</button></div>
        {drawer === "cart" ? <Cart fechar={fechar} /> : <Wish fechar={fechar} />}
      </div>
    </div>
  );
}

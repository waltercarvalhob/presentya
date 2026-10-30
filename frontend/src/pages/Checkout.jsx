import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Art } from "../lib/art";
import { api } from "../lib/api";
import { useLoja } from "../lib/store";
import { brl } from "../lib/util";

const norm = (s) => (s || "").normalize("NFD").replace(/\p{M}/gu, "").toLowerCase().trim();

export default function Checkout() {
  const { cart, subtotal, config, usuario, limparCart, toast } = useLoja();
  const nav = useNavigate();
  const lojas = [config.loja1_nome, config.loja2_nome].filter(Boolean);
  const [d, setD] = useState({
    tipoEntrega: "ENTREGA", lojaRetirada: lojas[0] || "", formaPagamento: "PIX",
    nomeDestinatario: usuario?.nome || "", telefone: usuario?.telefone || "", email: usuario?.email || "",
    cep: "", endereco: "", numero: "", complemento: "", bairro: "", cidade: config.cidade_entrega || "São Luís", uf: "MA",
    mensagemCartao: "", observacoes: "",
  });
  const [erro, setErro] = useState(null);
  const [enviando, setEnviando] = useState(false);
  const campo = (k) => ({ value: d[k], onChange: (e) => setD({ ...d, [k]: e.target.value }) });

  const buscarCep = async () => {
    const cep = d.cep.replace(/\D/g, "");
    if (cep.length !== 8) return;
    try {
      const r = await fetch(`https://viacep.com.br/ws/${cep}/json/`).then((x) => x.json());
      if (!r.erro) setD((x) => ({ ...x, endereco: r.logradouro || x.endereco, bairro: r.bairro || x.bairro, cidade: r.localidade || x.cidade, uf: r.uf || x.uf }));
    } catch { /* preenchimento manual */ }
  };

  const retirada = d.tipoEntrega === "RETIRADA";
  const local = norm(d.cidade) === norm(config.cidade_entrega);
  const frete = retirada ? 0 : Number(local ? config.frete_cidade : config.frete_brasil) || 0;

  if (!cart.length) {
    return <section className="sec"><div className="wrap empty"><h2>Sua sacola está vazia</h2><Link className="btn btn-gold" to="/#produtos">Escolher presentes</Link></div></section>;
  }

  const finalizar = async (e) => {
    e.preventDefault();
    setErro(null); setEnviando(true);
    try {
      const pedido = await api("/api/pedidos", {
        method: "POST",
        body: { ...d, itens: cart.map((i) => ({ produtoId: i.id, quantidade: i.qtd, personalizacao: i.perso })) },
      });
      limparCart();
      if (pedido.linkPagamento) { toast("Levando você ao pagamento seguro…"); window.location.assign(pedido.linkPagamento); }
      else nav("/pedido/" + pedido.codigo);
    } catch (err) { setErro(err.message); setEnviando(false); }
  };

  return (
    <>
      <div className="wrap page-head">
        <span className="eyebrow">Finalizar compra</span><h2>Quase lá!</h2>
        {!usuario && <p style={{ color: "var(--muted)" }}>Pode comprar sem criar conta. Se quiser acompanhar seus pedidos e pontos, a gente oferece isso ao final.</p>}
      </div>
      <form className="wrap page-grid" onSubmit={finalizar} style={{ paddingBottom: "4rem" }}>
        <div style={{ display: "grid", gap: "1.2rem" }}>
          <div className="form">
            <h3>1. Como você quer receber?</h3>
            <div className="opts two">
              <label className="opt"><input type="radio" name="te" checked={!retirada} onChange={() => setD({ ...d, tipoEntrega: "ENTREGA" })} />Entrega<small>São Luís com hora marcada · Brasil pelos Correios</small></label>
              <label className="opt"><input type="radio" name="te" checked={retirada} onChange={() => setD({ ...d, tipoEntrega: "RETIRADA" })} />Retirar na loja<small>Grátis</small></label>
            </div>
            <div className="row">
              <label>Quem recebe<input required maxLength={120} {...campo("nomeDestinatario")} /></label>
              <label>Telefone / WhatsApp<input required type="tel" maxLength={30} {...campo("telefone")} /></label>
            </div>
            <label>E-mail<input required type="email" maxLength={160} {...campo("email")} /><small>Usamos para enviar a confirmação do pedido.</small></label>
            {retirada ? (
              <label>Loja para retirada
                <select {...campo("lojaRetirada")}>{lojas.map((l) => <option key={l}>{l}</option>)}</select>
              </label>
            ) : (
              <>
                <div className="row">
                  <label>CEP<input required inputMode="numeric" maxLength={9} {...campo("cep")} onBlur={buscarCep} /></label>
                  <label>Cidade<input required maxLength={120} {...campo("cidade")} /></label>
                </div>
                <label>Endereço<input required maxLength={200} {...campo("endereco")} /></label>
                <div className="row">
                  <label>Número<input required maxLength={20} {...campo("numero")} /></label>
                  <label>Complemento<input maxLength={120} {...campo("complemento")} /></label>
                </div>
                <div className="row">
                  <label>Bairro<input required maxLength={120} {...campo("bairro")} /></label>
                  <label>UF<input required maxLength={2} {...campo("uf")} /></label>
                </div>
              </>
            )}
          </div>

          <div className="form">
            <h3>2. Cartão grátis</h3>
            <label>Mensagem do cartão<textarea maxLength={500} placeholder="Escreva sua mensagem com carinho…" {...campo("mensagemCartao")} /></label>
            <label>Observações<textarea maxLength={1000} placeholder="Horário preferido, ponto de referência…" {...campo("observacoes")} /></label>
          </div>

          <div className="form">
            <h3>3. Pagamento</h3>
            <div className="opts">
              <label className="opt"><input type="radio" name="fp" checked={d.formaPagamento === "PIX"} onChange={() => setD({ ...d, formaPagamento: "PIX" })} />Pix<small>Aprovação na hora</small></label>
              <label className="opt"><input type="radio" name="fp" checked={d.formaPagamento === "CREDITO"} onChange={() => setD({ ...d, formaPagamento: "CREDITO" })} />Crédito<small>Em até 12x</small></label>
              <label className="opt"><input type="radio" name="fp" checked={d.formaPagamento === "DEBITO"} onChange={() => setD({ ...d, formaPagamento: "DEBITO" })} />Débito<small>À vista</small></label>
            </div>
            <small>🔒 Cartões são pagos no ambiente seguro do Mercado Pago. A PresentYá não vê nem guarda os dados do seu cartão.</small>
          </div>
        </div>

        <aside className="form sticky">
          <h3>Resumo</h3>
          {cart.map((i) => (
            <div key={i.id} style={{ display: "grid", gridTemplateColumns: "48px 1fr auto", gap: ".7rem", alignItems: "center" }}>
              <div className="tbl"><div className="thumb" style={{ background: i.p.bg }}><Art p={i.p} /></div></div>
              <div><b style={{ fontSize: ".92rem" }}>{i.qtd}× {i.p.name}</b>{i.perso && <small style={{ display: "block" }}>“{i.perso}”</small>}</div>
              <span style={{ fontVariantNumeric: "tabular-nums" }}>{brl(i.p.price * i.qtd)}</span>
            </div>
          ))}
          <div className="total-box">
            <div><span>Subtotal</span><span>{brl(subtotal)}</span></div>
            <div><span>Frete {retirada ? "(retirada)" : local ? `(${config.cidade_entrega})` : "(Brasil)"}</span><span>{frete ? brl(frete) : "Grátis"}</span></div>
            <div className="grand"><span>Total</span><span>{brl(subtotal + frete)}</span></div>
          </div>
          {erro && <div className="alert" role="alert">{erro}</div>}
          <button className="btn btn-gold" disabled={enviando}>{enviando ? "Processando…" : d.formaPagamento === "PIX" ? "Gerar Pix" : "Ir para o pagamento"}</button>
          <small>O valor final é confirmado pela loja no momento do pedido.</small>
        </aside>
      </form>
    </>
  );
}

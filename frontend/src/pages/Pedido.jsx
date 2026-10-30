import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api } from "../lib/api";
import { useLoja } from "../lib/store";
import { brl, dataHora, PAGAMENTO, STATUS } from "../lib/util";

const ETAPAS = ["AGUARDANDO_PAGAMENTO", "PAGO", "EM_PRODUCAO", "ENVIADO", "ENTREGUE"];

export function Itens({ p }) {
  return (
    <>
      {p.itens.map((i, k) => (
        <div key={k} style={{ display: "flex", justifyContent: "space-between", gap: "1rem" }}>
          <span>{i.quantidade}× {i.nomeProduto}{i.personalizacao && <small style={{ display: "block", color: "var(--muted)" }}>Personalização: “{i.personalizacao}”</small>}</span>
          <span style={{ fontVariantNumeric: "tabular-nums" }}>{brl(i.total)}</span>
        </div>
      ))}
      <div className="total-box">
        <div><span>Subtotal</span><span>{brl(p.subtotal)}</span></div>
        <div><span>Frete</span><span>{Number(p.frete) ? brl(p.frete) : "Grátis"}</span></div>
        <div className="grand"><span>Total</span><span>{brl(p.total)}</span></div>
      </div>
    </>
  );
}

export function Entrega({ p }) {
  return p.tipoEntrega === "RETIRADA"
    ? <p>Retirada em <b>{p.lojaRetirada}</b> · {p.nomeDestinatario} · {p.telefone}</p>
    : <p>{p.nomeDestinatario} · {p.telefone}<br />{p.endereco}, {p.numero}{p.complemento ? ` – ${p.complemento}` : ""} · {p.bairro}<br />{p.cidade}/{p.uf} · CEP {p.cep}</p>;
}

export default function Pedido() {
  const { codigo } = useParams();
  const { toast, atualizarUsuario } = useLoja();
  const [p, setP] = useState(null);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    let vivo = true, tentativas = 0, t;
    const ciclo = async () => {
      try {
        const r = await api(`/api/pedidos/${codigo}/sincronizar`, { method: "POST" });
        if (!vivo) return;
        setP(r);
        if (r.status === "PAGO") atualizarUsuario().catch(() => {});
        if (r.status === "AGUARDANDO_PAGAMENTO" && ++tentativas < 120) t = setTimeout(ciclo, 5000);
      } catch (e) { if (vivo) setErro(e.message); }
    };
    ciclo();
    return () => { vivo = false; clearTimeout(t); };
  }, [codigo]); // eslint-disable-line react-hooks/exhaustive-deps

  if (erro) return <section className="sec"><div className="wrap empty"><p>{erro}</p><Link className="btn btn-gold" to="/minha-conta">Meus pedidos</Link></div></section>;
  if (!p) return <section className="sec"><div className="wrap empty"><p className="hand">Carregando seu pedido…</p></div></section>;

  const aguardando = p.status === "AGUARDANDO_PAGAMENTO";
  const etapa = ETAPAS.indexOf(p.status);
  const copiar = () => { try { navigator.clipboard.writeText(p.pixCopiaCola).then(() => toast("Código Pix copiado!")); } catch { toast("Selecione e copie o código"); } };

  return (
    <>
      <div className="wrap page-head">
        <span className="eyebrow">Pedido {p.codigo}</span>
        <h2>{aguardando ? "Falta só o pagamento" : p.status === "CANCELADO" ? "Pedido cancelado" : "Obrigado pela compra! ♡"}</h2>
        <p style={{ color: "var(--muted)" }}>Feito em {dataHora(p.criadoEm)} · {PAGAMENTO[p.formaPagamento]}</p>
      </div>
      <div className="wrap page-grid" style={{ paddingBottom: "4rem" }}>
        <div style={{ display: "grid", gap: "1.2rem" }}>
          {p.status !== "CANCELADO" && <div className="steps">{ETAPAS.map((s, i) => <span key={s} className={i <= etapa ? "on" : ""}>{STATUS[s]}</span>)}</div>}

          {aguardando && p.formaPagamento === "PIX" && (
            <div className="pix-box">
              <h3>Pague com Pix</h3>
              {p.pixQrBase64 ? <img src={`data:image/png;base64,${p.pixQrBase64}`} alt="QR Code Pix" /> : <p className="note">Modo de teste: nenhum QR Code real foi gerado.</p>}
              <p style={{ color: "var(--muted)" }}>Abra o app do seu banco, escolha Pix e leia o QR Code ou use o “copia e cola”:</p>
              <div className="pix-code">{p.pixCopiaCola}</div>
              <button className="btn btn-dark btn-sm" onClick={copiar}>Copiar código Pix</button>
              <small className="hand">Esta página atualiza sozinha quando o pagamento cair.</small>
            </div>
          )}
          {aguardando && p.formaPagamento !== "PIX" && p.linkPagamento && (
            <div className="form"><p>Seu pagamento ainda não foi confirmado.</p><a className="btn btn-gold" href={p.linkPagamento}>Pagar com cartão</a></div>
          )}
          {aguardando && p.pagamentoSimulado && <SimularBotao codigo={p.codigo} aoPagar={setP} />}

          <div className="form"><h3>Entrega</h3><Entrega p={p} />{p.mensagemCartao && <p><b>Cartão:</b> “{p.mensagemCartao}”</p>}</div>

          {p.convidado && (
            <div className="form">
              <h3>Quer acompanhar seus pedidos?</h3>
              <p style={{ color: "var(--muted)" }}>Crie sua conta grátis com o mesmo e-mail da compra e esse pedido (e os próximos) aparecem em "Meus pedidos", além de entrar no Clube PresentYá.</p>
              <div style={{ display: "flex", gap: ".6rem", flexWrap: "wrap" }}>
                <Link
                  className="btn btn-gold btn-sm"
                  to={"/cadastro?next=" + encodeURIComponent("/pedido/" + p.codigo)
                    + "&nome=" + encodeURIComponent(p.clienteNome || "")
                    + "&email=" + encodeURIComponent(p.clienteEmail || "")}
                >Criar minha conta</Link>
                <Link className="btn btn-line btn-sm" to="/">Não, voltar à loja</Link>
              </div>
            </div>
          )}
        </div>
        <aside className="form sticky"><h3>Itens</h3><Itens p={p} /><span className={"status " + p.status}>{STATUS[p.status]}</span><Link to="/minha-conta" className="btn btn-line btn-sm">Meus pedidos</Link></aside>
      </div>
    </>
  );
}

function SimularBotao({ codigo, aoPagar }) {
  const { toast, atualizarUsuario } = useLoja();
  const simular = async () => {
    try {
      aoPagar(await api(`/api/pedidos/${codigo}/simular-pagamento`, { method: "POST" }));
      atualizarUsuario().catch(() => {});
      toast("Pagamento de teste aprovado");
    } catch (e) { toast(e.message); }
  };
  return (
    <div className="note" style={{ display: "grid", gap: ".6rem" }}>
      <b>Modo de teste (sem Mercado Pago configurado)</b>
      <span>Nenhum dinheiro é cobrado. Use o botão para simular a aprovação.</span>
      <button className="btn btn-dark btn-sm" style={{ justifySelf: "start" }} onClick={simular}>Simular pagamento aprovado</button>
    </div>
  );
}

/** Página de cartão em modo de teste: não coleta dados de cartão. */
export function PagamentoSimulado() {
  const { codigo } = useParams();
  const nav = useNavigate();
  return (
    <section className="sec tint">
      <div className="wrap auth-box">
        <div className="form">
          <span className="eyebrow">Ambiente de teste</span>
          <h3>Pagamento com cartão (simulado)</h3>
          <p>Em produção, aqui abre a página segura do Mercado Pago para crédito ou débito. Configure <code>MP_ACCESS_TOKEN</code> no backend para ativar.</p>
          <SimularBotao codigo={codigo} aoPagar={() => nav("/pedido/" + codigo)} />
          <Link className="btn btn-line btn-sm" to={"/pedido/" + codigo}>Voltar ao pedido</Link>
        </div>
      </div>
    </section>
  );
}

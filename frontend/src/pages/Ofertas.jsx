import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import ProductCard from "../components/ProductCard";
import { api } from "../lib/api";
import { lojaNoMapa, metros, useProximidade } from "../lib/proximidade";
import { ativarPush, desativarPush, notificarAqui, situacaoPush } from "../lib/push";
import { useLoja } from "../lib/store";

export const dataCurta = (iso) => new Date(iso + "T12:00:00").toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit" });

/** Página aberta pelo QR Code da vitrine: ofertas do dia, aviso de proximidade e cadastro para receber promoções. */
export default function Ofertas() {
  const { produtos, wa } = useLoja();
  const [busca] = useSearchParams();
  const [ofertas, setOfertas] = useState(null);
  const origem = busca.get("origem") === "qr" ? "qrcode" : "site";
  useEffect(() => { api("/api/ofertas").then(setOfertas).catch(() => setOfertas([])); }, []);
  const emPromocao = produtos.filter((p) => p.old).sort((a, b) => b.old / b.price - a.old / a.price);

  return (
    <section className="sec ofertas-pg">
      <div className="wrap" style={{ display: "grid", gap: "2rem" }}>
        <header style={{ display: "grid", gap: ".8rem" }}>
          <span className="eyebrow">{origem === "qrcode" ? "Que bom que você escaneou!" : "Ofertas da loja"}</span>
          <h2>Ofertas e promoções da PresentYá</h2>
          <p className="lead">Aproveite enquanto durarem: mostre esta tela no caixa ou peça pelo WhatsApp.</p>
        </header>

        <AtivarNotificacoes origem={origem} />

        <AvisoProximidade />

        {ofertas === null ? <p className="hand">Carregando ofertas…</p> : ofertas.length > 0 && (
          <div className="ofertas-grid">
            {ofertas.map((o) => (
              <article className="oferta-card" key={o.id}>
                <h3>{o.titulo}</h3>
                {o.descricao && <p>{o.descricao}</p>}
                {o.cupom && <div className="cupom"><small>Cupom</small><b>{o.cupom}</b></div>}
                <div className="oferta-pe">
                  {o.validaAte ? <small>Válida até {dataCurta(o.validaAte)}</small> : <span />}
                  <a className="btn btn-wa btn-sm" target="_blank" rel="noopener"
                    href={wa(`Olá, PresentYá! Quero aproveitar a oferta "${o.titulo}"${o.cupom ? ` (cupom ${o.cupom})` : ""}.`)}>Quero esta</a>
                </div>
              </article>
            ))}
          </div>
        )}

        {emPromocao.length > 0 && (
          <div style={{ display: "grid", gap: "1rem" }}>
            <h3>Produtos com desconto</h3>
            <div className="grid">{emPromocao.map((p, i) => <ProductCard key={p.id} p={p} i={i} />)}</div>
          </div>
        )}

        {ofertas?.length === 0 && emPromocao.length === 0 && (
          <div className="empty"><p>Nenhuma oferta no momento. Deixe seu WhatsApp e avisamos a próxima!</p></div>
        )}

        <ReceberOfertas origem={origem} />
      </div>
    </section>
  );
}

/** O passo principal do QR Code: um toque e o celular passa a receber as ofertas como notificação. */
function AtivarNotificacoes({ origem }) {
  const [situacao, setSituacao] = useState(null);
  const [ativando, setAtivando] = useState(false);
  const [erro, setErro] = useState(null);
  useEffect(() => { situacaoPush().then(setSituacao); }, []);
  if (!situacao || situacao === "sem-suporte") return null;
  const ativar = async () => {
    setErro(null); setAtivando(true);
    try { setSituacao(await ativarPush(origem)); }
    catch (e) { setErro(e.message || "Não foi possível ativar agora. Tente de novo."); }
    finally { setAtivando(false); }
  };
  return (
    <div className={"push-card" + (situacao === "ativas" ? " ok" : "")} role="status">
      {situacao === "pode-ativar" && <>
        <div>
          <b>🔔 Receba as ofertas direto no seu celular</b>
          <p>Toque em ativar e mande “Permitir”: na hora chegam as promoções de hoje, e as próximas chegam sozinhas.</p>
        </div>
        <button className="btn btn-dark" onClick={ativar} disabled={ativando}>{ativando ? "Ativando…" : "Ativar notificações"}</button>
      </>}
      {situacao === "ativas" && <>
        <div><b>✅ Notificações ativadas!</b><p>Você já recebeu as ofertas de hoje e vai ser avisado das próximas.</p></div>
        <button className="btn btn-line btn-sm" onClick={async () => setSituacao(await desativarPush())}>Parar de receber</button>
      </>}
      {situacao === "bloqueadas" && <div><b>Notificações bloqueadas neste navegador.</b><p>Para receber as ofertas, libere as notificações deste site nas configurações do navegador, ou deixe seu WhatsApp aqui embaixo.</p></div>}
      {situacao === "iphone-instalar" && <div>
        <b>🔔 No iPhone, falta um passo para receber as ofertas</b>
        <p>Toque em <b>Compartilhar</b> (o quadrado com a seta) → <b>Adicionar à Tela de Início</b>. Depois abra a PresentYá pela tela de início e toque em “Ativar notificações”.</p>
      </div>}
      {erro && <div className="alert" style={{ flexBasis: "100%" }}>{erro}</div>}
    </div>
  );
}

function AvisoProximidade() {
  const { config } = useLoja();
  const loja = lojaNoMapa(config);
  const [ligado, setLigado] = useState(false);
  const { estado, distancia, precisao, perto } = useProximidade(loja, ligado);
  useEffect(() => { if (perto) notificarAqui("📍 Você está pertinho da PresentYá!", config.mensagem_proximidade); }, [perto]); // eslint-disable-line react-hooks/exhaustive-deps
  if (!loja) return null;
  return (
    <div className={"perto-card" + (perto ? " perto" : "")} role="status">
      {!ligado && <>
        <p><b>Está passando pela loja?</b> Libere a localização e mostramos quando você estiver a até {metros(loja.raio)} da PresentYá.</p>
        <button className="btn btn-dark btn-sm" onClick={() => setLigado(true)}>Usar minha localização</button>
      </>}
      {ligado && estado === "buscando" && <p>Procurando sua localização…</p>}
      {estado === "negado" && <p>Localização bloqueada. Tudo bem: as ofertas abaixo valem do mesmo jeito.</p>}
      {estado === "sem-suporte" && <p>Não conseguimos sua localização agora. As ofertas abaixo valem do mesmo jeito.</p>}
      {estado === "ok" && (perto
        ? <p><b>🎉 {config.mensagem_proximidade}</b><br /><small>Você está a cerca de {metros(distancia)} da loja.</small></p>
        : <p>Você está a cerca de <b>{metros(distancia)}</b> da loja{precisao > 50 ? " (sinal de GPS fraco)" : ""}. Passe por aqui e aproveite!</p>)}
    </div>
  );
}

const CADASTRO_VAZIO = { nome: "", whatsapp: "", email: "", aceite: false };

export function ReceberOfertas({ origem }) {
  const [f, setF] = useState(CADASTRO_VAZIO);
  const [erro, setErro] = useState(null);
  const [ok, setOk] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const campo = (k) => ({ value: f[k], onChange: (e) => setF({ ...f, [k]: e.target.value }) });
  const enviar = async (e) => {
    e.preventDefault();
    setErro(null); setEnviando(true);
    try {
      await api("/api/interessados", { method: "POST", body: { ...f, origem } });
      setOk(true); setF(CADASTRO_VAZIO);
    } catch (err) { setErro(err.message); }
    finally { setEnviando(false); }
  };
  return (
    <form className="form" onSubmit={enviar} style={{ maxWidth: 640 }}>
      <h3>Receba as próximas ofertas no seu celular</h3>
      {erro && <div className="alert">{erro}</div>}
      {ok ? <div className="ok">Pronto! Você vai receber nossas promoções pelo WhatsApp. 💛</div> : <>
        <div className="row">
          <label>Seu nome<input required maxLength={120} autoComplete="name" {...campo("nome")} /></label>
          <label>WhatsApp com DDD<input required type="tel" inputMode="tel" maxLength={30} autoComplete="tel" placeholder="(98) 90000-0000" {...campo("whatsapp")} /></label>
        </div>
        <label>E-mail (opcional)<input type="email" maxLength={160} autoComplete="email" {...campo("email")} /></label>
        <label className="aceite">
          <input type="checkbox" checked={f.aceite} onChange={(e) => setF({ ...f, aceite: e.target.checked })} required />
          <span>Aceito receber ofertas da PresentYá pelo WhatsApp. Posso pedir para sair a qualquer momento.</span>
        </label>
        <button className="btn btn-gold" disabled={enviando}>{enviando ? "Enviando…" : "Quero receber"}</button>
      </>}
    </form>
  );
}

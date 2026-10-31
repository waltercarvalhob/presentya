import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import QRCode from "qrcode";
import { Topo } from "./Admin";
import Logo from "../components/Logo";
import { dataCurta } from "../pages/Ofertas";
import { api } from "../lib/api";
import { textoOfertas } from "../lib/proximidade";
import { useLoja } from "../lib/store";
import { dataHora, waLink } from "../lib/util";

/** Endereço que o QR Code abre: a página de ofertas deste mesmo site. */
export const linkOfertas = () => `${location.origin}/ofertas?origem=qr`;
const local = () => ["localhost", "127.0.0.1"].includes(location.hostname);

export function QrOfertas({ tamanho = 560, className }) {
  const [src, setSrc] = useState(null);
  useEffect(() => {
    QRCode.toDataURL(linkOfertas(), { width: tamanho, margin: 2, errorCorrectionLevel: "M", color: { dark: "#141414", light: "#FFFFFF" } })
      .then(setSrc);
  }, [tamanho]);
  return src ? <img className={className} src={src} width={tamanho} height={tamanho} alt="QR Code da página de ofertas da PresentYá" /> : null;
}

const OFERTA_VAZIA = { titulo: "", descricao: "", cupom: "", validaAte: "", ordem: 0, ativo: true };
const PROX = ["loja_latitude", "loja_longitude", "raio_proximidade", "mensagem_proximidade"];

export function Divulgacao() {
  const { toast } = useLoja();
  const [ofertas, setOfertas] = useState(null);
  const [editando, setEditando] = useState(null);
  useEffect(() => { api("/api/admin/ofertas").then(setOfertas).catch((e) => toast(e.message)); }, [toast]);

  const salvo = (o) => {
    setOfertas(ofertas.some((x) => x.id === o.id) ? ofertas.map((x) => (x.id === o.id ? o : x)) : [o, ...ofertas]);
    setEditando(null);
    toast("Oferta salva");
  };
  const alternar = async (o) => {
    try { salvo(await api(`/api/admin/ofertas/${o.id}`, { method: "PUT", body: { ...o, ativo: !o.ativo } })); }
    catch (e) { toast(e.message); }
  };
  const remover = async (o) => {
    if (!confirm(`Excluir a oferta "${o.titulo}"?`)) return;
    try { await api(`/api/admin/ofertas/${o.id}`, { method: "DELETE" }); setOfertas(ofertas.filter((x) => x.id !== o.id)); toast("Oferta excluída"); }
    catch (e) { toast(e.message); }
  };
  const baixarQr = async () => {
    const a = document.createElement("a");
    a.href = await QRCode.toDataURL(linkOfertas(), { width: 1200, margin: 2, errorCorrectionLevel: "M" });
    a.download = "presentya-qrcode-ofertas.png";
    a.click();
  };

  return (
    <>
      <Topo titulo="Ofertas e QR Code">
        <button className="btn btn-gold btn-sm" onClick={() => setEditando(editando ? null : OFERTA_VAZIA)}>{editando ? "Fechar" : "+ Nova oferta"}</button>
      </Topo>

      <div className="qr-painel">
        <QrOfertas tamanho={220} className="qr-img" />
        <div style={{ display: "grid", gap: ".7rem", alignContent: "start" }}>
          <h3>QR Code da vitrine</h3>
          <p style={{ color: "var(--muted)" }}>Cole na vitrine, na porta e nas laterais da loja. Quem passar aponta a câmera do celular, abre as ofertas e toca em “Ativar notificações”: na hora recebe as ofertas de hoje no celular, e as próximas chegam sozinhas.</p>
          <code className="qr-link">{linkOfertas()}</code>
          {local() && <div className="alert">Você está no endereço local: imprima o cartaz pelo site publicado (presentya.vercel.app), senão o QR não abre no celular dos clientes.</div>}
          <div style={{ display: "flex", gap: ".5rem", flexWrap: "wrap" }}>
            <Link className="btn btn-dark btn-sm" to="/cartaz" target="_blank">Imprimir cartaz</Link>
            <button className="btn btn-line btn-sm" onClick={baixarQr}>Baixar QR (PNG)</button>
            <a className="btn btn-line btn-sm" href="/ofertas" target="_blank" rel="noopener">Ver página de ofertas</a>
          </div>
        </div>
      </div>

      <Notificacoes />

      {editando && <FormOferta inicial={editando} salvo={salvo} />}

      <h3 style={{ margin: "1.6rem 0 .8rem" }}>Ofertas</h3>
      {ofertas === null ? <p className="hand">Carregando…</p> : !ofertas.length ? (
        <p style={{ color: "var(--muted)" }}>Nenhuma oferta cadastrada. Produtos com preço promocional também aparecem na página de ofertas.</p>
      ) : (
        <div className="tbl-wrap">
          <table className="tbl">
            <thead><tr><th>Oferta</th><th>Cupom</th><th>Validade</th><th className="num">Ordem</th><th>Situação</th><th /></tr></thead>
            <tbody>
              {ofertas.map((o) => (
                <tr key={o.id} className={o.ativo ? "" : "off"}>
                  <td><b>{o.titulo}</b>{o.descricao && <><br /><small>{o.descricao}</small></>}</td>
                  <td>{o.cupom || "—"}</td>
                  <td>{o.validaAte ? dataCurta(o.validaAte) : "Sem prazo"}</td>
                  <td className="num">{o.ordem}</td>
                  <td><button className={"mini-btn" + (o.ativo ? " danger" : "")} onClick={() => alternar(o)}>{o.ativo ? "Desativar" : "Ativar"}</button></td>
                  <td>
                    <button className="mini-btn" onClick={() => setEditando({ ...OFERTA_VAZIA, ...o, descricao: o.descricao || "", cupom: o.cupom || "", validaAte: o.validaAte || "" })}>Editar</button>
                    <button className="mini-btn danger" onClick={() => remover(o)}>Excluir</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <LocalLoja />
    </>
  );
}

/** Quantos celulares recebem as notificações e envio de um aviso avulso. */
function Notificacoes() {
  const { toast } = useLoja();
  const [inscritos, setInscritos] = useState(null);
  const [f, setF] = useState({ titulo: "🎁 PresentYá", mensagem: "" });
  const [enviando, setEnviando] = useState(false);
  useEffect(() => { api("/api/admin/push").then((r) => setInscritos(r.inscritos)).catch(() => {}); }, []);
  const enviar = async (e) => {
    e.preventDefault();
    if (!confirm(`Enviar esta notificação para ${inscritos} celular(es)?`)) return;
    setEnviando(true);
    try { const r = await api("/api/admin/push/enviar", { method: "POST", body: f }); toast(`Notificação enviada para ${r.enviadas} celular(es)`); setF({ ...f, mensagem: "" }); }
    catch (err) { toast(err.message); }
    finally { setEnviando(false); }
  };
  return (
    <form className="form" onSubmit={enviar} style={{ maxWidth: 900, marginTop: "1.4rem" }}>
      <h3>🔔 Notificações no celular</h3>
      <p style={{ color: "var(--muted)" }}>
        <b style={{ color: "var(--ink)", fontSize: "1.2rem" }}>{inscritos ?? "…"}</b> celular(es) recebem as notificações.
        Envio automático: <b>quem ativa recebe na hora as ofertas do dia</b>, e <b>toda oferta nova ou reativada vai para todos</b> assim que você salva.
        Use o campo abaixo só para um aviso extra (ex.: “Chegaram os kits de Natal!”).
      </p>
      <div className="row">
        <label>Título<input required maxLength={80} value={f.titulo} onChange={(e) => setF({ ...f, titulo: e.target.value })} /></label>
        <label>Mensagem<input required maxLength={300} value={f.mensagem} onChange={(e) => setF({ ...f, mensagem: e.target.value })} placeholder="Ex.: Hoje tudo com 10% na loja!" /></label>
      </div>
      <button className="btn btn-gold" disabled={enviando || !inscritos}>{enviando ? "Enviando…" : "Enviar agora para todos"}</button>
    </form>
  );
}

function FormOferta({ inicial, salvo }) {
  const [d, setD] = useState(inicial);
  const [erro, setErro] = useState(null);
  const [enviando, setEnviando] = useState(false);
  useEffect(() => setD(inicial), [inicial]);
  const campo = (k) => ({ value: d[k] ?? "", onChange: (e) => setD({ ...d, [k]: e.target.value }) });
  const salvar = async (e) => {
    e.preventDefault();
    setErro(null); setEnviando(true);
    const body = { ...d, validaAte: d.validaAte || null, ordem: d.ordem === "" ? 0 : parseInt(d.ordem, 10) };
    try { salvo(await api(d.id ? `/api/admin/ofertas/${d.id}` : "/api/admin/ofertas", { method: d.id ? "PUT" : "POST", body })); }
    catch (err) { setErro(err.message); }
    finally { setEnviando(false); }
  };
  return (
    <form className="form" onSubmit={salvar} style={{ maxWidth: 760, marginTop: "1.4rem" }}>
      <h3>{d.id ? "Editar oferta" : "Nova oferta"}</h3>
      {erro && <div className="alert">{erro}</div>}
      <label>Título<input required maxLength={120} placeholder="Ex.: Caneca personalizada com 20% off" {...campo("titulo")} /></label>
      <label>Descrição<textarea maxLength={1000} placeholder="Detalhes, regras, quantidade limitada…" {...campo("descricao")} /></label>
      <div className="row">
        <label>Cupom (opcional)<input maxLength={40} placeholder="Ex.: VITRINE10" {...campo("cupom")} /></label>
        <label>Válida até (opcional)<input type="date" {...campo("validaAte")} /></label>
        <label>Ordem na página<input type="number" min={0} {...campo("ordem")} /><small>Menor aparece primeiro.</small></label>
        <label className="aceite"><input type="checkbox" checked={d.ativo} onChange={(e) => setD({ ...d, ativo: e.target.checked })} /><span>Mostrar na página de ofertas</span></label>
      </div>
      {d.ativo && !(inicial.id && inicial.ativo) && <small>Ao salvar, todos os celulares inscritos recebem esta oferta como notificação.</small>}
      <button className="btn btn-gold" disabled={enviando}>{enviando ? "Salvando…" : "Salvar oferta"}</button>
    </form>
  );
}

/** Coordenadas usadas pelo aviso "você está perto da loja". */
function LocalLoja() {
  const { recarregar, toast } = useLoja();
  const [d, setD] = useState(null);
  const [buscando, setBuscando] = useState(false);
  useEffect(() => { api("/api/admin/configuracoes").then((c) => setD(Object.fromEntries(PROX.map((k) => [k, c[k] || ""])))); }, []);
  if (!d) return null;
  const campo = (k) => ({ value: d[k], onChange: (e) => setD({ ...d, [k]: e.target.value }) });
  const aqui = () => {
    if (!("geolocation" in navigator)) return toast("Este aparelho não informa a localização");
    setBuscando(true);
    navigator.geolocation.getCurrentPosition(
      (p) => { setD({ ...d, loja_latitude: p.coords.latitude.toFixed(6), loja_longitude: p.coords.longitude.toFixed(6) }); setBuscando(false); toast(`Localização capturada (precisão ~${Math.round(p.coords.accuracy)} m)`); },
      () => { setBuscando(false); toast("Não foi possível obter a localização"); },
      { enableHighAccuracy: true, timeout: 30000 },
    );
  };
  const salvar = async (e) => {
    e.preventDefault();
    try { await api("/api/admin/configuracoes", { method: "PUT", body: d }); await recarregar(); toast("Localização da loja salva"); }
    catch (err) { toast(err.message); }
  };
  return (
    <form className="form" onSubmit={salvar} style={{ maxWidth: 760, marginTop: "1.6rem" }}>
      <h3>Aviso de proximidade</h3>
      <p style={{ color: "var(--muted)" }}>
        Quem abre a página de ofertas e libera a localização vê um aviso quando está a até o raio abaixo da loja.
        Para pegar as coordenadas: abra esta tela no celular <b>dentro da loja</b> e toque em “Usar minha localização atual”,
        ou no Google Maps clique com o botão direito no ponto da loja e copie os números. Deixe vazio para desligar.
      </p>
      <div className="row">
        <label>Latitude<input inputMode="decimal" placeholder="-2.5307" {...campo("loja_latitude")} /></label>
        <label>Longitude<input inputMode="decimal" placeholder="-44.3068" {...campo("loja_longitude")} /></label>
        <label>Raio (metros)<input type="number" min={10} max={2000} {...campo("raio_proximidade")} /><small>O GPS do celular erra de 5 a 20 m.</small></label>
        <label>Mensagem do aviso<input maxLength={200} {...campo("mensagem_proximidade")} /></label>
      </div>
      <div style={{ display: "flex", gap: ".5rem", flexWrap: "wrap" }}>
        <button className="btn btn-gold">Salvar</button>
        <button type="button" className="btn btn-line" onClick={aqui} disabled={buscando}>{buscando ? "Buscando…" : "Usar minha localização atual"}</button>
      </div>
    </form>
  );
}

const ORIGEM = { qrcode: "QR Code", proximidade: "Proximidade", site: "Site" };

export function Interessados() {
  const { toast } = useLoja();
  const [lista, setLista] = useState(null);
  const [ofertas, setOfertas] = useState([]);
  useEffect(() => {
    api("/api/admin/interessados").then(setLista).catch((e) => { toast(e.message); setLista([]); });
    api("/api/ofertas").then(setOfertas).catch(() => {});
  }, [toast]);
  const link = linkOfertas().replace("?origem=qr", "");
  const remover = async (i) => {
    if (!confirm(`Remover ${i.nome} da lista? (use quando a pessoa pedir para não receber mais)`)) return;
    try { await api(`/api/admin/interessados/${i.id}`, { method: "DELETE" }); setLista(lista.filter((x) => x.id !== i.id)); toast("Removido da lista"); }
    catch (e) { toast(e.message); }
  };
  const exportar = () => {
    const linhas = [["Nome", "WhatsApp", "E-mail", "Origem", "Cadastro"], ...lista.map((i) => [i.nome, i.whatsapp, i.email || "", ORIGEM[i.origem] || i.origem, dataHora(i.criadoEm)])];
    const csv = linhas.map((l) => l.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(";")).join("\r\n");
    const a = document.createElement("a");
    a.href = URL.createObjectURL(new Blob(["﻿" + csv], { type: "text/csv;charset=utf-8" }));
    a.download = "presentya-interessados.csv";
    a.click();
  };
  return (
    <>
      <Topo titulo="Quem quer receber ofertas">
        {lista?.length > 0 && <button className="btn btn-line btn-sm" onClick={exportar}>Exportar planilha (CSV)</button>}
      </Topo>
      <p style={{ color: "var(--muted)", marginBottom: "1rem", maxWidth: "75ch" }}>
        Pessoas que escanearam o QR Code e aceitaram receber promoções. O botão <b>WhatsApp</b> abre a conversa já com as ofertas ativas escritas.
        {!ofertas.length && " Cadastre ofertas em “Ofertas e QR Code” para a mensagem vir preenchida."}
      </p>
      {lista === null ? <p className="hand">Carregando…</p> : !lista.length ? <p style={{ color: "var(--muted)" }}>Ninguém cadastrado ainda.</p> : (
        <div className="tbl-wrap">
          <table className="tbl">
            <thead><tr><th>Nome</th><th>WhatsApp</th><th>E-mail</th><th>Origem</th><th>Cadastro</th><th /></tr></thead>
            <tbody>
              {lista.map((i) => (
                <tr key={i.id}>
                  <td>{i.nome}</td><td>{i.whatsapp}</td><td>{i.email || "—"}</td><td>{ORIGEM[i.origem] || i.origem}</td><td>{dataHora(i.criadoEm)}</td>
                  <td>
                    <a className="mini-btn" target="_blank" rel="noopener" href={waLink(i.whatsapp, textoOfertas(ofertas, link, i.nome))}>WhatsApp</a>
                    <button className="mini-btn danger" onClick={() => remover(i)}>Remover</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

/** Cartaz A4 para a vitrine (abre em outra aba e manda imprimir). */
export function Cartaz() {
  const [ofertas, setOfertas] = useState([]);
  useEffect(() => { api("/api/ofertas").then(setOfertas).catch(() => {}); }, []);
  return (
    <div className="cartaz-pg">
      <div className="cartaz-acoes">
        <button className="btn btn-gold" onClick={() => print()}>Imprimir</button>
        <span>Dica: papel A4, sem margens, em cores. Cole na altura dos olhos.</span>
      </div>
      <div className="cartaz">
        <Logo />
        <h1>Ofertas no seu celular</h1>
        <p className="cartaz-sub">Aponte a câmera e ative as notificações</p>
        <QrOfertas tamanho={640} className="cartaz-qr" />
        {ofertas.length > 0 && <ul className="cartaz-ofertas">{ofertas.slice(0, 3).map((o) => <li key={o.id}>{o.titulo}</li>)}</ul>}
        <p className="cartaz-pe">Promoções exclusivas · Cupons · Novidades</p>
      </div>
    </div>
  );
}

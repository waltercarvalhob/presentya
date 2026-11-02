import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import QRCode from "qrcode";
import { Topo } from "./Admin";
import Logo from "../components/Logo";
import { dataCurta } from "../pages/Ofertas";
import { API, api, getToken, imagemUrl } from "../lib/api";
import { textoOfertas } from "../lib/proximidade";
import { useLoja } from "../lib/store";
import { dataHora, waLink } from "../lib/util";

/** Endereço que o QR Code abre: a página de ofertas deste mesmo site. */
export const linkOfertas = () => `${location.origin}/ofertas?origem=qr`;
/** Gravado na etiqueta NFC: encostou o celular, a página de ofertas abre na hora. */
export const linkNfc = () => `${location.origin}/ofertas?origem=nfc&abrir=whatsapp`;
const local = () => ["localhost", "127.0.0.1"].includes(location.hostname);

export function QrOfertas({ tamanho = 560, className, link, rotulo = "da página de ofertas" }) {
  const [src, setSrc] = useState(null);
  const destino = link || linkOfertas();
  useEffect(() => {
    QRCode.toDataURL(destino, { width: tamanho, margin: 2, errorCorrectionLevel: "M", color: { dark: "#141414", light: "#FFFFFF" } })
      .then(setSrc);
  }, [tamanho, destino]);
  return src ? <img className={className} src={src} width={tamanho} height={tamanho} alt={"QR Code " + rotulo + " da PresentYá"} /> : null;
}

/** QR Codes que o cartaz usa: os canais do robô que estão ligados; sem robô, a página de ofertas. */
export function qrsDoCartaz(canais) {
  const lista = [];
  if (canais?.whatsapp?.ativo) lista.push({ id: "whatsapp", nome: "WhatsApp", link: canais.whatsapp.link, chamada: "Receba no WhatsApp" });
  if (canais?.telegram?.ativo) lista.push({ id: "telegram", nome: "Telegram", link: canais.telegram.link, chamada: "Receba no Telegram" });
  if (!lista.length) lista.push({ id: "site", nome: "Site", link: linkOfertas(), chamada: "Aponte a câmera e ative as notificações" });
  return lista;
}

const baixar = async (link, nome) => {
  const a = document.createElement("a");
  a.href = await QRCode.toDataURL(link, { width: 1200, margin: 2, errorCorrectionLevel: "M" });
  a.download = nome;
  a.click();
};

/** Robô de ofertas: quem lê o QR manda uma mensagem e recebe as ofertas sozinho no WhatsApp ou no Telegram. */
function Canais() {
  const { toast } = useLoja();
  const [c, setC] = useState(null);
  const copiar = (link) => navigator.clipboard.writeText(link).then(
    () => toast("Link da etiqueta copiado: no NFC Tools, Escrever → URL → cole e encoste a etiqueta"),
    () => prompt("Copie o link para gravar na etiqueta NFC:", link));
  useEffect(() => { api("/api/admin/canais").then(setC).catch(() => {}); }, []);
  if (!c) return null;
  const situacao = (ativo) => (ativo ? <span className="status PAGO">Ligado</span> : <span className="status AGUARDANDO_PAGAMENTO">Falta configurar</span>);
  return (
    <div className="form" style={{ maxWidth: 900, marginTop: "1.4rem" }}>
      <h3>🤖 Robô de ofertas no WhatsApp e no Telegram</h3>
      <p style={{ color: "var(--muted)" }}>
        A pessoa lê o QR Code, o WhatsApp (ou Telegram) abre com a mensagem pronta e ela só toca em <b>Enviar</b>.
        O robô responde <b>na hora</b> com as ofertas do dia (fotos e vídeos) e, daí em diante, <b>toda oferta nova chega sozinha</b>.
        Quem mandar <b>PARAR</b> sai da lista.
      </p>
      <div className="canais">
        <div className="canal">
          <div className="canal-topo"><b>WhatsApp</b>{situacao(c.whatsapp.ativo)}</div>
          <QrOfertas tamanho={180} className="qr-img" link={c.whatsapp.link} rotulo="do WhatsApp" />
          <small>{c.whatsapp.contatos} contato(s) recebendo</small>
          {c.whatsapp.ativo && !c.whatsapp.modelo && <small>⚠️ Sem modelo aprovado: ofertas novas só chegam a quem falou com a loja nas últimas 24 h (regra da Meta).</small>}
          {!c.whatsapp.ativo && <small>Sem a API configurada, este QR abre a conversa, mas quem responde é você, manualmente.</small>}
          <div style={{ display: "flex", gap: ".4rem", flexWrap: "wrap" }}>
            <button type="button" className="btn btn-line btn-sm" onClick={() => baixar(c.whatsapp.link, "presentya-qr-whatsapp.png")}>Baixar QR</button>
            <button type="button" className="btn btn-line btn-sm" onClick={() => copiar(linkNfc())}>Copiar link para NFC</button>
          </div>
        </div>
        <div className="canal">
          <div className="canal-topo"><b>Telegram</b>{situacao(c.telegram.ativo)}</div>
          {c.telegram.ativo ? <>
            <QrOfertas tamanho={180} className="qr-img" link={c.telegram.link} rotulo="do Telegram" />
            <small>@{c.telegram.bot} · {c.telegram.contatos} contato(s) recebendo</small>
            <button type="button" className="btn btn-line btn-sm" onClick={() => baixar(c.telegram.link, "presentya-qr-telegram.png")}>Baixar QR</button>
          </> : <small>Gratuito e pronto em 2 minutos: veja “Como ligar” abaixo.</small>}
        </div>
      </div>
      {c.ultimoEnvio && <small>Último envio do robô: {c.ultimoEnvio.endsWith(" OK") ? "✅ entregue" : "⚠️ " + c.ultimoEnvio.replace(/^\S+ /, "")}</small>}
      <details>
        <summary><b>Etiqueta NFC na vitrine (“encoste o celular”)</b></summary>
        <div className="como-ligar">
          <p>Compre etiquetas <b>NFC NTAG215</b> (uns R$ 2 a 5 cada). Instale o app gratuito <b>NFC Tools</b> no celular,
            toque em <i>Escrever → Adicionar registro → URL</i>, cole o link (botão “Copiar link para NFC” acima: <code>{linkNfc()}</code>),
            toque em <i>Escrever</i> e encoste a etiqueta. Cole-a do lado de dentro do vidro, atrás do círculo do cartaz.
            Quem encostar o celular <b>cai direto no WhatsApp da loja com a mensagem pronta</b> (Android e iPhone XS ou mais novo):
            só toca em Enviar e o robô responde sozinho com as ofertas. Ao voltar do WhatsApp, a página de ofertas está aberta.
            Vidro muito grosso (mais de ~1 cm) pode impedir a leitura: teste antes.</p>
        </div>
      </details>
      <details>
        <summary><b>Como ligar</b></summary>
        <div className="como-ligar">
          <p><b>Telegram (gratuito, 2 minutos):</b> no Telegram, abra <b>@BotFather</b> → <code>/newbot</code> → dê o nome “PresentYá Ofertas” e um usuário terminado em “bot”.
            Copie o token que ele mandar e, no Render (serviço <i>presentya-api</i> → Environment), crie <code>TELEGRAM_BOT_TOKEN</code> com esse valor. Pronto: o servidor reinicia e liga sozinho.</p>
          <p><b>WhatsApp (API oficial da Meta):</b> em developers.facebook.com crie um app do tipo <i>Business</i> com o produto <i>WhatsApp</i> e cadastre o número da loja.
            No Render crie <code>WHATSAPP_TOKEN</code> (token permanente de usuário do sistema), <code>WHATSAPP_PHONE_NUMBER_ID</code>, <code>WHATSAPP_APP_SECRET</code> (Configurações do app → Básico)
            e <code>WHATSAPP_VERIFY_TOKEN</code> (uma senha que você inventa). No painel do WhatsApp → Configuração → Webhook, use a URL <code>https://presentya-api.onrender.com/api/whatsapp/webhook</code>,
            a mesma senha do VERIFY_TOKEN, e assine o campo <i>messages</i>.
            Para mandar ofertas a quem não falou com a loja nas últimas 24 h, aprove um modelo de mensagem de <i>Marketing</i> com uma variável <code>{"{{1}}"}</code> (o título da oferta) e informe o nome dele em <code>WHATSAPP_MODELO</code>.
            A Meta cobra por conversa de marketing.</p>
        </div>
      </details>
    </div>
  );
}

const OFERTA_VAZIA = { titulo: "", descricao: "", cupom: "", validaAte: "", iniciaEm: "", ordem: 0, ativo: true, imagemId: null, imagemUrl: null, videoId: null, videoUrl: null };
const dataHoraCurta = (iso) => new Date(iso).toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });
/** iniciaEm vem no horário da loja (sem fuso); a comparação usa o relógio do aparelho, que na loja é o mesmo. */
const agendada = (o) => o.iniciaEm && new Date(o.iniciaEm) > new Date();
const MB = 1024 * 1024;

/** Envio com barra de progresso (vídeo pelo celular pode demorar). */
function enviarMidia(arquivo, aoProgredir) {
  return new Promise((ok, falha) => {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", API + "/api/admin/midias");
    const token = getToken();
    if (token) xhr.setRequestHeader("Authorization", "Bearer " + token);
    xhr.upload.onprogress = (e) => e.lengthComputable && aoProgredir(Math.round((e.loaded / e.total) * 100));
    xhr.onload = () => {
      let r = null;
      try { r = JSON.parse(xhr.responseText); } catch { /* resposta vazia */ }
      xhr.status < 300 ? ok(r) : falha(new Error(r?.erro || "Não foi possível enviar o arquivo"));
    };
    xhr.onerror = () => falha(new Error("Falha de conexão ao enviar o arquivo"));
    const form = new FormData();
    form.append("arquivo", arquivo);
    xhr.send(form);
  });
}

/** Foto ou vídeo da oferta: tirar na hora pela câmera ou escolher da galeria. */
function CampoMidia({ tipo, url, aoMudar }) {
  const [progresso, setProgresso] = useState(null);
  const [erro, setErro] = useState(null);
  const video = tipo === "video";
  const escolher = async (e) => {
    const arq = e.target.files[0];
    e.target.value = "";
    if (!arq) return;
    if (arq.size > (video ? 25 : 5) * MB) return setErro(video ? "Vídeo maior que 25 MB: grave mais curto (até ~30 s) ou em resolução menor." : "Foto maior que 5 MB.");
    setErro(null); setProgresso(0);
    try { const r = await enviarMidia(arq, setProgresso); aoMudar(r.id, r.url); }
    catch (err) { setErro(err.message); }
    finally { setProgresso(null); }
  };
  const aceita = video ? "video/mp4,video/webm,video/quicktime" : "image/jpeg,image/png,image/webp,image/gif";
  return (
    <div className="campo-midia">
      <b>{video ? "🎬 Vídeo da oferta" : "📷 Foto da oferta"}</b>
      {url && (video
        ? <video src={imagemUrl(url)} muted playsInline controls preload="metadata" />
        : <img src={imagemUrl(url)} alt="Foto da oferta" />)}
      {progresso !== null ? <div className="barra" role="progressbar" aria-valuenow={progresso}><span style={{ width: progresso + "%" }} />{progresso}%</div> : (
        <div style={{ display: "flex", gap: ".4rem", flexWrap: "wrap" }}>
          <label className="btn btn-dark btn-sm">{video ? "Gravar vídeo" : "Tirar foto"}<input hidden type="file" accept={video ? "video/*" : "image/*"} capture="environment" onChange={escolher} /></label>
          <label className="btn btn-line btn-sm">Da galeria<input hidden type="file" accept={aceita} onChange={escolher} /></label>
          {url && <button type="button" className="mini-btn danger" onClick={() => aoMudar(null, null)}>Remover</button>}
        </div>
      )}
      <small>{video ? "MP4 ou MOV até 25 MB (uns 30 s). Toca sozinho, sem som." : "JPG, PNG ou WEBP até 5 MB. Vai junto na notificação."}</small>
      {erro && <div className="alert">{erro}</div>}
    </div>
  );
}
const PROX = ["loja_latitude", "loja_longitude", "raio_proximidade", "mensagem_proximidade"];

export function Divulgacao() {
  const { toast } = useLoja();
  const [ofertas, setOfertas] = useState(null);
  const [busca] = useSearchParams();
  const [editando, setEditando] = useState(busca.get("nova") ? OFERTA_VAZIA : null);
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
      <Topo titulo="Ofertas e promoções">
        <button className="btn btn-gold btn-sm" onClick={() => setEditando(editando ? null : OFERTA_VAZIA)}>{editando ? "Fechar" : "+ Nova oferta / promoção"}</button>
        <Link className="btn btn-dark btn-sm" to="/cartaz" target="_blank">Imprimir cartaz</Link>
      </Topo>

      {editando && <FormOferta inicial={editando} salvo={salvo} />}

      <div className="qr-painel" style={{ marginTop: editando ? "1.4rem" : 0 }}>
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

      <Canais />

      <Notificacoes />

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
                  <td><b>{o.titulo}</b>{(o.imagemUrl || o.videoUrl) && <> {o.imagemUrl && "📷"}{o.videoUrl && "🎬"}</>}{o.descricao && <><br /><small>{o.descricao}</small></>}</td>
                  <td>{o.cupom || "—"}</td>
                  <td>{agendada(o) ? <>⏰ Começa {dataHoraCurta(o.iniciaEm)}<br /></> : null}{o.validaAte ? "Até " + dataCurta(o.validaAte) : "Sem prazo"}</td>
                  <td className="num">{o.ordem}</td>
                  <td><button className={"mini-btn" + (o.ativo ? " danger" : "")} onClick={() => alternar(o)}>{o.ativo ? "Desativar" : "Ativar"}</button></td>
                  <td>
                    <button className="mini-btn" onClick={() => setEditando({ ...OFERTA_VAZIA, ...o, descricao: o.descricao || "", cupom: o.cupom || "", validaAte: o.validaAte || "", iniciaEm: o.iniciaEm ? o.iniciaEm.slice(0, 16) : "" })}>Editar</button>
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
  const [ultimo, setUltimo] = useState("");
  const [diario, setDiario] = useState(null);
  useEffect(() => {
    api("/api/admin/configuracoes").then((c) => setDiario({ ativo: c.envio_diario_ativo === "true", hora: c.envio_diario_hora || "10:00", ultimo: c.envio_diario_ultimo || "" })).catch(() => {});
  }, []);
  const salvarDiario = async (novo) => {
    setDiario(novo);
    try {
      await api("/api/admin/configuracoes", { method: "PUT", body: { envio_diario_ativo: String(novo.ativo), envio_diario_hora: novo.hora } });
      toast(novo.ativo ? `Envio diário ligado: todo dia às ${novo.hora}` : "Envio diário desligado");
    } catch (e) { toast(e.message); }
  };
  const [f, setF] = useState({ titulo: "🎁 PresentYá", mensagem: "" });
  const [enviando, setEnviando] = useState(false);
  useEffect(() => { api("/api/admin/push").then((r) => { setInscritos(r.inscritos); setUltimo(r.ultimoEnvio); }).catch(() => {}); }, []);
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
      {ultimo && <small>Último envio: {ultimo.endsWith(" OK") ? "✅ entregue" : "⚠️ " + ultimo.replace(/^\S+ /, "")} <span style={{ opacity: .7 }}>({dataHora(ultimo.split(" ")[0])})</span></small>}
      <div className="row">
        <label>Título<input required maxLength={80} value={f.titulo} onChange={(e) => setF({ ...f, titulo: e.target.value })} /></label>
        <label>Mensagem<input required maxLength={300} value={f.mensagem} onChange={(e) => setF({ ...f, mensagem: e.target.value })} placeholder="Ex.: Hoje tudo com 10% na loja!" /></label>
      </div>
      <button className="btn btn-gold" disabled={enviando || !inscritos}>{enviando ? "Enviando…" : "Enviar agora para todos"}</button>
      {diario && (
        <div className="diario">
          <label className="aceite"><input type="checkbox" checked={diario.ativo} onChange={(e) => salvarDiario({ ...diario, ativo: e.target.checked })} />
            <span><b>Envio diário automático</b>: todo dia manda sozinho as ofertas do dia e os produtos em promoção para todos (notificação, WhatsApp e Telegram).</span></label>
          <label style={{ maxWidth: 200 }}>Horário
            <input type="time" value={diario.hora} onChange={(e) => setDiario({ ...diario, hora: e.target.value })} onBlur={() => salvarDiario(diario)} />
          </label>
          <small>{diario.ultimo ? `Último envio diário: ${dataCurta(diario.ultimo)}.` : "Ainda não houve envio diário."} Dias sem oferta ativa nem produto em promoção não mandam nada.
            No plano gratuito o servidor dorme sem visitas: se estiver dormindo no horário, envia assim que acordar (até as 21 h).
            Para pontualidade, cadastre em cron-job.org (grátis) uma chamada a cada 10 minutos para https://presentya-api.onrender.com/api/status.</small>
        </div>
      )}
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
    const body = { ...d, validaAte: d.validaAte || null, iniciaEm: d.iniciaEm || null, ordem: d.ordem === "" ? 0 : parseInt(d.ordem, 10) };
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
        <CampoMidia tipo="imagem" url={d.imagemUrl} aoMudar={(id, url) => setD((x) => ({ ...x, imagemId: id, imagemUrl: url }))} />
        <CampoMidia tipo="video" url={d.videoUrl} aoMudar={(id, url) => setD((x) => ({ ...x, videoId: id, videoUrl: url }))} />
      </div>
      <div className="row">
        <label>Cupom (opcional)<input maxLength={40} placeholder="Ex.: VITRINE10" {...campo("cupom")} /></label>
        <label>Começa em (opcional)<input type="datetime-local" {...campo("iniciaEm")} /><small>Vazio = já. Agendada: entra no ar e é enviada sozinha nessa hora.</small></label>
        <label>Válida até (opcional)<input type="date" {...campo("validaAte")} /><small>Sai do ar sozinha depois desse dia.</small></label>
        <label>Ordem na página<input type="number" min={0} {...campo("ordem")} /><small>Menor aparece primeiro.</small></label>
        <label className="aceite"><input type="checkbox" checked={d.ativo} onChange={(e) => setD({ ...d, ativo: e.target.checked })} /><span>Mostrar na página de ofertas</span></label>
      </div>
      {d.ativo && !(inicial.id && inicial.ativo) && <small>{agendada(d)
        ? `Agendada: em ${dataHoraCurta(d.iniciaEm)} ela entra no ar e vai sozinha para todos (notificação, WhatsApp e Telegram).`
        : "Ao salvar, todos recebem esta oferta na hora (notificação, WhatsApp e Telegram)."}</small>}
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
        {!ofertas.length && " Cadastre ofertas em “Ofertas e promoções” para a mensagem vir preenchida."}
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
  const [qrs, setQrs] = useState(null);
  const [escolha, setEscolha] = useState("todos");
  const [nfc, setNfc] = useState(true);
  useEffect(() => {
    api("/api/ofertas").then(setOfertas).catch(() => {});
    api("/api/admin/canais").then((c) => setQrs(qrsDoCartaz(c))).catch(() => setQrs(qrsDoCartaz(null)));
  }, []);
  if (!qrs) return <p className="hand" style={{ padding: "2rem" }}>Carregando…</p>;
  const mostrar = escolha === "todos" ? qrs : qrs.filter((q) => q.id === escolha);
  return (
    <div className="cartaz-pg">
      <div className="cartaz-acoes">
        <button className="btn btn-gold" onClick={() => print()}>Imprimir</button>
        {qrs.length > 1 && (
          <select value={escolha} onChange={(e) => setEscolha(e.target.value)} aria-label="QR Codes do cartaz">
            <option value="todos">WhatsApp e Telegram</option>
            {qrs.map((q) => <option key={q.id} value={q.id}>Só {q.nome}</option>)}
          </select>
        )}
        <label style={{ display: "flex", gap: ".4rem", alignItems: "center" }}>
          <input type="checkbox" checked={nfc} onChange={(e) => setNfc(e.target.checked)} /> Área NFC (“Encoste o celular aqui”)
        </label>
        <span>Dica: papel A4, sem margens, em cores. Cole na altura dos olhos{nfc ? " e a etiqueta NFC atrás do círculo, do lado de dentro do vidro" : ""}.</span>
      </div>
      <div className={"cartaz" + (nfc ? " com-nfc" : "")}>
        <Logo />
        <h1>Ofertas no seu celular</h1>
        <p className="cartaz-sub">{mostrar.length > 1 ? "Aponte a câmera e escolha onde receber" : mostrar[0].chamada}</p>
        <div className={"cartaz-qrs" + (mostrar.length > 1 ? " dois" : "")}>
          {mostrar.map((q) => (
            <figure key={q.id}>
              <QrOfertas tamanho={640} className="cartaz-qr" link={q.link} rotulo={"do " + q.nome} />
              {mostrar.length > 1 && <figcaption>{q.nome}</figcaption>}
            </figure>
          ))}
        </div>
        {nfc && (
          <div className="cartaz-nfc">
            <span className="nfc-alvo" aria-hidden="true">
              <svg viewBox="0 0 48 48" fill="none" stroke="currentColor" strokeWidth="3.2" strokeLinecap="round">
                <rect x="9" y="6" width="18" height="36" rx="4" /><path d="M15 36h6" />
                <path d="M32 18a9 9 0 0 1 0 12M36 14a15 15 0 0 1 0 20M40 10a21 21 0 0 1 0 28" />
              </svg>
            </span>
            <div><b>ou encoste o celular aqui</b><small>Funciona com aproximação (NFC), como no pagamento</small></div>
          </div>
        )}
        {ofertas.length > 0 && <ul className="cartaz-ofertas">{ofertas.slice(0, 3).map((o) => <li key={o.id}>{o.titulo}</li>)}</ul>}
        <p className="cartaz-pe">Promoções exclusivas · Cupons · Novidades</p>
      </div>
    </div>
  );
}

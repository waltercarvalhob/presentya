import { useEffect, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import QRCode from "qrcode";
import { Topo } from "./Admin";
import { linkNfc } from "./Promocao";
import { useLoja } from "../lib/store";
import { waLink } from "../lib/util";

// Gravação de placas NFC pelo navegador (Web NFC): só existe no Chrome para Android.
const SITE = location.protocol === "https:" ? location.origin : "https://presentya.vercel.app";
const UA = navigator.userAgent;
const IOS = /iPhone|iPad|iPod/.test(UA) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
const ANDROID = /Android/.test(UA);
const SUPORTA = "NDEFReader" in window;

const DESTINOS = {
  ofertas: "Página de ofertas (abre o WhatsApp sozinha, igual ao cartaz)",
  loja: "Loja virtual (página inicial)",
  clube: "Cadastro no Clube PresentYá",
  instagram: "Seguir no Instagram",
  whatsapp: "Conversar no WhatsApp",
  google: "Avaliar no Google",
  link: "Outro link",
};

function montarLink(f, config) {
  const origem = "utm_source=nfc&utm_medium=placa&utm_campaign=" + encodeURIComponent(f.local.trim() || "loja");
  switch (f.destino) {
    case "ofertas": return linkNfc().replace(location.origin, SITE);
    case "loja": return `${SITE}/?${origem}`;
    case "clube": return `${SITE}/cadastro?${origem}`;
    case "instagram": return `https://instagram.com/${(config.instagram || "presentya_").replace("@", "")}`;
    case "whatsapp": return waLink(config.whatsapp || "5598984084256", f.msg);
    default: return /^https?:\/\/\S+$/.test(f.link.trim()) ? f.link.trim() : "";
  }
}

function lerRegistros(e) {
  const textos = [...e.message.records].map((r) => {
    if (["url", "absolute-url", "text"].includes(r.recordType) && r.data) {
      return new TextDecoder(r.encoding || "utf-8").decode(r.data);
    }
    return `(registro do tipo ${r.recordType})`;
  });
  return textos.length ? "A placa contém: " + textos.join(" · ") : "A placa está vazia.";
}

function mensagemErro(err) {
  switch (err.name) {
    case "NotAllowedError": return "O Chrome não recebeu permissão para usar o NFC. Toque no cadeado ao lado do endereço do site e permita o NFC.";
    case "NotSupportedError": return "O NFC está desligado ou o celular não tem NFC. Ative em Configurações > Conexões > NFC.";
    case "NetworkError": return "A placa se afastou antes de terminar, ou está bloqueada. Tente de novo segurando a placa parada por 2 segundos.";
    default: return err.message || "Não foi possível usar a placa.";
  }
}

export function PlacasNfc() {
  const { config, toast } = useLoja();
  const [sp] = useSearchParams();
  const [f, setF] = useState({
    destino: DESTINOS[sp.get("destino")] ? sp.get("destino") : "ofertas",
    local: sp.get("local") ?? "balcao",
    msg: sp.get("msg") ?? "Oi! Vim pela placa da loja PresentYá",
    link: sp.get("link") ?? "",
  });
  const [bloquear, setBloquear] = useState(false);
  const [estado, setEstado] = useState(null); // { tipo: "aguardando" | "ok" | "erro", texto }
  const [qrLink, setQrLink] = useState("");
  const [qrPagina, setQrPagina] = useState("");
  const operacao = useRef(null);

  const url = montarLink(f, config);
  const campo = (k) => ({ value: f[k], onChange: (e) => setF({ ...f, [k]: e.target.value }) });
  const paginaNoCelular = `${SITE}/admin/nfc?` + new URLSearchParams(f).toString();

  useEffect(() => {
    if (url) QRCode.toDataURL(url, { width: 360, margin: 2 }).then(setQrLink);
    else setQrLink("");
  }, [url]);
  useEffect(() => {
    if (!SUPORTA && !IOS) QRCode.toDataURL(paginaNoCelular, { width: 280, margin: 2 }).then(setQrPagina);
  }, [paginaNoCelular]);
  useEffect(() => () => operacao.current?.abort(), []);

  const cancelar = () => { operacao.current?.abort(); operacao.current = null; setEstado(null); };

  const usarPlaca = async (gravar) => {
    if (gravar && !url) return;
    if (gravar && bloquear && !confirm("Bloquear é DEFINITIVO: a placa nunca mais poderá ser regravada. Continuar?")) return;
    operacao.current?.abort();
    const ctrl = new AbortController();
    operacao.current = ctrl;
    const atual = () => operacao.current === ctrl;
    const ndef = new window.NDEFReader();
    setEstado({ tipo: "aguardando", texto: "Encoste a placa na parte de trás do celular e segure parada…" });
    try {
      if (gravar) {
        await ndef.write({ records: [{ recordType: "url", data: url }] }, { signal: ctrl.signal, overwrite: true });
        if (bloquear) {
          if (atual()) setEstado({ tipo: "aguardando", texto: "Gravada! Mantenha a placa encostada para bloquear…" });
          await ndef.makeReadOnly({ signal: ctrl.signal });
        }
        if (atual()) setEstado({ tipo: "ok", texto: bloquear ? "Placa gravada e bloqueada." : "Placa gravada! Teste encostando outro celular nela." });
      } else {
        const evento = await new Promise((ok, falha) => {
          ndef.onreading = ok;
          ndef.onreadingerror = () => falha(new DOMException("Não consegui ler a placa. Tente de novo.", "NotReadableError"));
          ctrl.signal.addEventListener("abort", () => falha(new DOMException("", "AbortError")));
          ndef.scan({ signal: ctrl.signal }).catch(falha);
        });
        if (atual()) setEstado({ tipo: "ok", texto: lerRegistros(evento) });
      }
    } catch (err) {
      if (atual() && err.name !== "AbortError") setEstado({ tipo: "erro", texto: mensagemErro(err) });
    } finally {
      if (atual()) { ctrl.abort(); operacao.current = null; }
    }
  };

  const copiar = async () => {
    try { await navigator.clipboard.writeText(url); toast("Link copiado"); }
    catch { toast("Não consegui copiar; selecione o link e copie manualmente"); }
  };

  const ocupado = estado?.tipo === "aguardando";
  return (
    <>
      <Topo titulo="Placas NFC" />
      <div className="nfc">
        <form className="form" onSubmit={(e) => e.preventDefault()}>
          <h3>1. O que a placa vai abrir</h3>
          <div className="row">
            <label>Destino
              <select {...campo("destino")} disabled={ocupado}>
                {Object.entries(DESTINOS).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
              </select>
            </label>
            {(f.destino === "loja" || f.destino === "clube") && (
              <label>Onde a placa vai ficar<input {...campo("local")} placeholder="balcao, vitrine, embalagem…" maxLength={40} disabled={ocupado} />
                <small>Serve para saber de qual placa veio cada visita.</small></label>
            )}
            {f.destino === "whatsapp" && (
              <label>Mensagem que já vem escrita<input {...campo("msg")} maxLength={200} disabled={ocupado} /></label>
            )}
            {(f.destino === "google" || f.destino === "link") && (
              <label>Link (começando com https://)<input {...campo("link")} type="url" placeholder="https://…" disabled={ocupado} />
                {f.destino === "google" && <small>No Google: Perfil da Empresa &gt; Pedir avaliações &gt; copiar link.</small>}</label>
            )}
          </div>
          {url ? <div className="detail nfc-url"><span>{url}</span><button type="button" className="mini-btn" onClick={copiar}>Copiar</button></div>
            : <div className="alert">Cole um link válido, começando com https://</div>}
        </form>

        <div className="form">
          <h3>2. Gravar na placa</h3>
          {SUPORTA ? (
            <>
              <label className="nfc-check"><input type="checkbox" checked={bloquear} onChange={(e) => setBloquear(e.target.checked)} disabled={ocupado} />
                Bloquear a placa depois de gravar (definitivo — ninguém mais consegue trocar o link)</label>
              <div style={{ display: "flex", gap: ".6rem", flexWrap: "wrap" }}>
                <button type="button" className="btn btn-gold" disabled={!url || ocupado} onClick={() => usarPlaca(true)}>Gravar na placa</button>
                <button type="button" className="btn btn-line" disabled={ocupado} onClick={() => usarPlaca(false)}>Ler placa</button>
                {ocupado && <button type="button" className="btn btn-line" onClick={cancelar}>Cancelar</button>}
              </div>
              {estado && <div className={"nfc-status " + estado.tipo} role="status">{estado.texto}</div>}
            </>
          ) : IOS ? (
            <div className="alert">
              O iPhone não deixa sites gravarem NFC. Toque em <b>Copiar</b> acima e grave com o app <b>NFC Tools</b>:
              Gravar &gt; Adicionar registro &gt; URL/URI &gt; colar &gt; Gravar. Ou abra esta página no Chrome de um celular Android.
            </div>
          ) : ANDROID ? (
            <div className="alert">Abra esta página no <b>Google Chrome</b> para gravar a placa (outros navegadores do Android não têm NFC).</div>
          ) : (
            <>
              <p>O computador não tem NFC. <b>Aponte a câmera de um celular Android</b> para este código: a página abre no Chrome do celular já preenchida, pronta para gravar (entre com seu login do painel).</p>
              {qrPagina && <img className="nfc-qr" src={qrPagina} alt="QR Code para abrir esta página no celular" width={220} height={220} />}
              <small>iPhone: copie o link e grave com o app NFC Tools.</small>
            </>
          )}
        </div>

        {qrLink && (
          <div className="form">
            <h3>3. QR Code para imprimir junto da placa</h3>
            <p style={{ color: "var(--muted)" }}>Para quem estiver com o NFC desligado: o QR abre o mesmo link.</p>
            <img className="nfc-qr" src={qrLink} alt={"QR Code de " + url} width={220} height={220} />
            <a className="btn btn-line btn-sm" href={qrLink} download={`qr-presentya-${f.destino}.png`}>Baixar QR Code</a>
          </div>
        )}
      </div>
    </>
  );
}

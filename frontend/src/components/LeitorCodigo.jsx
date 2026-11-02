import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";

/**
 * Lê código de barras (EAN, UPC, Code 128...) ou QR Code pela câmera traseira.
 * Também aceita digitar o código ou usar um leitor USB/Bluetooth (que "digita" e aperta Enter).
 */
export default function LeitorCodigo({ aoLer, fechar }) {
  const video = useRef();
  const [erro, setErro] = useState(null);
  const [digitado, setDigitado] = useState("");
  const retorno = useRef(aoLer);
  retorno.current = aoLer;

  useEffect(() => {
    let controles, ativo = true;
    (async () => {
      try {
        const { BrowserMultiFormatReader } = await import("@zxing/browser");
        if (!ativo) return;
        const leitor = new BrowserMultiFormatReader();
        controles = await leitor.decodeFromConstraints(
          { video: { facingMode: { ideal: "environment" }, width: { ideal: 1280 } } },
          video.current,
          (resultado, _erro, c) => {
            if (!resultado || !ativo) return;
            ativo = false;
            c.stop();
            navigator.vibrate?.(80);
            retorno.current(resultado.getText().trim());
          },
        );
        if (!ativo) controles.stop();
      } catch (e) {
        setErro(e?.name === "NotAllowedError"
          ? "Câmera bloqueada. Libere a câmera para este site nas configurações do navegador, ou digite o código abaixo."
          : "Não foi possível abrir a câmera neste aparelho. Digite o código abaixo ou use um leitor USB.");
      }
    })();
    return () => { ativo = false; controles?.stop(); };
  }, []);

  const enviar = (e) => { e.preventDefault(); if (digitado.trim()) aoLer(digitado.trim()); };
  // Portal: o leitor abre de dentro do formulário do produto, e formulário dentro de formulário não vale.
  return createPortal(
    <div className="leitor" role="dialog" aria-modal="true" aria-label="Ler código de barras ou QR Code" onClick={(e) => e.target === e.currentTarget && fechar()}>
      <div className="leitor-caixa">
        <div className="panel-head"><h3>Ler código do produto</h3><button type="button" className="close" onClick={fechar} aria-label="Fechar">×</button></div>
        {erro ? <div className="alert">{erro}</div> : (
          <div className="leitor-video">
            <video ref={video} muted playsInline />
            <span className="leitor-mira" aria-hidden="true" />
          </div>
        )}
        <p style={{ color: "var(--muted)", fontSize: ".9rem" }}>Aponte a câmera para o código de barras ou QR Code da embalagem. A leitura é automática.</p>
        <form onSubmit={enviar} style={{ display: "flex", gap: ".5rem" }}>
          <input className="chip" style={{ flex: 1, cursor: "text" }} autoFocus={!!erro} inputMode="numeric" placeholder="Ou digite / use o leitor USB"
            value={digitado} onChange={(e) => setDigitado(e.target.value)} aria-label="Código do produto" />
          <button className="btn btn-gold btn-sm">Buscar</button>
        </form>
      </div>
    </div>,
    document.body,
  );
}

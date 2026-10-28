import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Topo } from "./Admin";
import { Art, ILUSTRACOES } from "../lib/art";
import { api } from "../lib/api";
import { useLoja } from "../lib/store";
import { brl, OCC, PARA, produtoUI } from "../lib/util";

const CORES = {
  "var(--rose-soft)": "Amarelo claro", "var(--gold-soft)": "Creme", "var(--sky)": "Marfim", "var(--paper)": "Papel",
  "var(--sage-soft)": "Areia", "var(--rose)": "Amarelo", "var(--gold-bright)": "Amarelo PresentYá", "var(--brand)": "Amarelo marca",
  "var(--gold)": "Dourado escuro", "var(--sage)": "Grafite", "var(--ink)": "Preto", "var(--card)": "Branco",
};

const VAZIO = {
  nome: "", descricao: "", preco: "", precoPromocional: "", estoque: "", personalizavel: false, novidade: true, maisVendido: false, ativo: true,
  imagemId: null, imagemUrl: null, ilustracao: "box", corFundo: "var(--rose-soft)", corA: "var(--rose)", corB: "var(--gold-bright)",
  categoriaId: "", ocasioes: [], publicos: [],
};

function OpcoesCategoria({ categorias }) {
  return categorias.map((c) => c.filhas.length ? (
    <optgroup key={c.id} label={c.nome}>
      <option value={c.id}>{c.nome} (geral)</option>
      {c.filhas.map((f) => <option key={f.id} value={f.id}>{f.nome}</option>)}
    </optgroup>
  ) : <option key={c.id} value={c.id}>{c.nome}</option>);
}

function FormProduto({ inicial, categorias, fechar, salvo }) {
  const { toast } = useLoja();
  const [d, setD] = useState(inicial);
  const [erro, setErro] = useState(null);
  const [enviando, setEnviando] = useState(false);
  const set = (k) => (e) => setD({ ...d, [k]: e.target.type === "checkbox" ? e.target.checked : e.target.value });
  const marca = (k, v) => setD({ ...d, [k]: d[k].includes(v) ? d[k].filter((x) => x !== v) : [...d[k], v] });

  const enviarFoto = async (e) => {
    const f = e.target.files[0];
    if (!f) return;
    const form = new FormData();
    form.append("arquivo", f);
    try { const r = await api("/api/admin/imagens", { method: "POST", form }); setD({ ...d, imagemId: r.id, imagemUrl: r.url }); }
    catch (err) { toast(err.message); }
  };

  const salvar = async (e) => {
    e.preventDefault();
    setErro(null); setEnviando(true);
    const num = (v) => (v === "" || v == null ? null : Number(String(v).replace(",", ".")));
    const body = { ...d, preco: num(d.preco), precoPromocional: num(d.precoPromocional), estoque: d.estoque === "" ? null : parseInt(d.estoque, 10), categoriaId: d.categoriaId ? Number(d.categoriaId) : null };
    try {
      const r = await api(d.id ? `/api/admin/produtos/${d.id}` : "/api/admin/produtos", { method: d.id ? "PUT" : "POST", body });
      salvo(r);
    } catch (err) { setErro(err.message); setEnviando(false); }
  };

  const prev = produtoUI({ ...d, precoAtual: 0, criadoEm: new Date().toISOString() });
  return (
    <div className="drawer" onClick={(e) => e.target === e.currentTarget && fechar()}>
      <form className="panel wide form" style={{ borderRadius: 0 }} onSubmit={salvar} role="dialog" aria-modal="true" aria-label="Produto">
        <div className="panel-head"><h3>{d.id ? "Editar produto" : "Novo produto"}</h3><button type="button" className="close" onClick={fechar} aria-label="Fechar">×</button></div>
        {erro && <div className="alert" role="alert">{erro}</div>}
        <label>Nome<input required maxLength={140} value={d.nome} onChange={set("nome")} /></label>
        <label>Descrição<textarea maxLength={4000} value={d.descricao || ""} onChange={set("descricao")} /></label>
        <label>Categoria<select value={d.categoriaId || ""} onChange={set("categoriaId")}><option value="">Sem categoria</option><OpcoesCategoria categorias={categorias} /></select></label>
        <div className="row">
          <label>Preço (R$)<input required inputMode="decimal" value={d.preco} onChange={set("preco")} placeholder="49,90" /></label>
          <label>Preço promocional (R$)<input inputMode="decimal" value={d.precoPromocional ?? ""} onChange={set("precoPromocional")} placeholder="vazio = sem oferta" /></label>
        </div>
        <label>Estoque<input inputMode="numeric" value={d.estoque ?? ""} onChange={set("estoque")} placeholder="vazio = sob encomenda (sem controle)" /></label>
        <div className="check-grid">
          <label><input type="checkbox" checked={d.ativo} onChange={set("ativo")} /> Visível na loja</label>
          <label><input type="checkbox" checked={d.personalizavel} onChange={set("personalizavel")} /> Personalizável</label>
          <label><input type="checkbox" checked={d.novidade} onChange={set("novidade")} /> Lançamento</label>
          <label><input type="checkbox" checked={d.maisVendido} onChange={set("maisVendido")} /> Selo “Mais vendido”</label>
        </div>
        <label>Ocasiões</label>
        <div className="check-grid">{Object.entries(OCC).map(([v, t]) => <label key={v}><input type="checkbox" checked={d.ocasioes.includes(v)} onChange={() => marca("ocasioes", v)} /> {t}</label>)}</div>
        <label>Para quem</label>
        <div className="check-grid">{Object.entries(PARA).map(([v, t]) => <label key={v}><input type="checkbox" checked={d.publicos.includes(v)} onChange={() => marca("publicos", v)} /> {t}</label>)}</div>

        <label>Foto do produto</label>
        <div style={{ display: "flex", gap: "1rem", alignItems: "center", flexWrap: "wrap" }}>
          <div className="preview-art" style={{ background: d.corFundo }}><Art p={prev} /></div>
          <div style={{ display: "grid", gap: ".4rem" }}>
            <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" onChange={enviarFoto} />
            {d.imagemId && <button type="button" className="mini-btn" onClick={() => setD({ ...d, imagemId: null, imagemUrl: null })}>Remover foto (usar ilustração)</button>}
            <small>JPG, PNG ou WEBP até 5 MB. Sem foto, a loja mostra a ilustração abaixo.</small>
          </div>
        </div>
        <div className="row">
          <label>Ilustração<select value={d.ilustracao} onChange={set("ilustracao")}>{Object.entries(ILUSTRACOES).map(([v, t]) => <option key={v} value={v}>{t}</option>)}</select></label>
          <label>Cor de fundo<select value={d.corFundo} onChange={set("corFundo")}>{Object.entries(CORES).map(([v, t]) => <option key={v} value={v}>{t}</option>)}</select></label>
          <label>Cor principal<select value={d.corA} onChange={set("corA")}>{Object.entries(CORES).map(([v, t]) => <option key={v} value={v}>{t}</option>)}</select></label>
          <label>Cor de detalhe<select value={d.corB} onChange={set("corB")}>{Object.entries(CORES).map(([v, t]) => <option key={v} value={v}>{t}</option>)}</select></label>
        </div>
        <div style={{ display: "flex", gap: ".6rem", marginTop: "auto" }}>
          <button className="btn btn-gold" disabled={enviando}>{enviando ? "Salvando…" : "Salvar produto"}</button>
          <button type="button" className="btn btn-line" onClick={fechar}>Cancelar</button>
        </div>
      </form>
    </div>
  );
}

export function Produtos() {
  const { recarregar, toast } = useLoja();
  const [lista, setLista] = useState(null);
  const [categorias, setCategorias] = useState([]);
  const [editando, setEditando] = useState(null);
  const [busca, setBusca] = useState("");
  const [q, setQ] = useSearchParams();

  const carregar = () => api("/api/admin/produtos").then(setLista);
  useEffect(() => {
    carregar();
    api("/api/admin/categorias").catch(() => api("/api/categorias")).then(setCategorias);
  }, []);
  useEffect(() => { if (q.get("novo")) { setEditando(VAZIO); setQ({}); } }, [q, setQ]);

  const salvo = async (r) => { setEditando(null); toast(`“${r.nome}” salvo`); await carregar(); recarregar(); };
  const remover = async (p) => {
    if (!confirm(`Remover “${p.nome}”? Produtos que já foram vendidos são apenas ocultados.`)) return;
    try { await api(`/api/admin/produtos/${p.id}`, { method: "DELETE" }); toast("Produto removido"); await carregar(); recarregar(); }
    catch (e) { toast(e.message); }
  };
  const filtrados = useMemo(() => (lista || []).filter((p) => (p.nome + (p.categoriaNome || "")).toLowerCase().includes(busca.toLowerCase())), [lista, busca]);

  return (
    <>
      <Topo titulo="Produtos">
        <input className="chip" placeholder="Buscar produto" value={busca} onChange={(e) => setBusca(e.target.value)} />
        <button className="btn btn-gold btn-sm" onClick={() => setEditando(VAZIO)}>+ Novo produto</button>
      </Topo>
      {lista === null ? <p className="hand">Carregando…</p> : (
        <div className="tbl-wrap">
          <table className="tbl">
            <thead><tr><th /><th>Produto</th><th>Categoria</th><th className="num">Preço</th><th className="num">Estoque</th><th className="num">Vendidos</th><th /></tr></thead>
            <tbody>
              {filtrados.map((d) => {
                const p = produtoUI(d);
                return (
                  <tr key={d.id} className={d.ativo ? "" : "off"}>
                    <td><div className="thumb" style={{ background: p.bg }}><Art p={p} /></div></td>
                    <td><b>{d.nome}</b><br /><small>{[d.ativo ? null : "Oculto", d.personalizavel && "Personalizável", d.novidade && "Lançamento"].filter(Boolean).join(" · ")}</small></td>
                    <td>{d.categoriaNome || "—"}</td>
                    <td className="num">{p.old && <s style={{ color: "var(--muted)", marginRight: ".3rem" }}>{brl(p.old)}</s>}{brl(p.price)}</td>
                    <td className="num">{d.estoque ?? "sob encomenda"}</td>
                    <td className="num">{d.vendidos}</td>
                    <td><div className="acts">
                      <button className="mini-btn" onClick={() => setEditando({ ...VAZIO, ...d, precoPromocional: d.precoPromocional ?? "", estoque: d.estoque ?? "", categoriaId: d.categoriaId || "" })}>Editar</button>
                      <button className="mini-btn danger" onClick={() => remover(d)}>Remover</button>
                    </div></td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
      {editando && <FormProduto inicial={editando} categorias={categorias} fechar={() => setEditando(null)} salvo={salvo} />}
    </>
  );
}

export function Categorias() {
  const { recarregar, toast } = useLoja();
  const [lista, setLista] = useState([]);
  const [d, setD] = useState({ nome: "", paiId: "", ordem: 0, ativo: true });
  const carregar = () => api("/api/admin/categorias").then(setLista);
  useEffect(() => { carregar(); }, []);

  const salvar = async (e) => {
    e.preventDefault();
    try {
      const body = { ...d, paiId: d.paiId ? Number(d.paiId) : null, ordem: Number(d.ordem) || 0 };
      await api(d.id ? `/api/admin/categorias/${d.id}` : "/api/admin/categorias", { method: d.id ? "PUT" : "POST", body });
      toast("Categoria salva"); setD({ nome: "", paiId: "", ordem: 0, ativo: true }); await carregar(); recarregar();
    } catch (err) { toast(err.message); }
  };
  const remover = async (c) => {
    if (!confirm(`Excluir a categoria “${c.nome}”?`)) return;
    try { await api(`/api/admin/categorias/${c.id}`, { method: "DELETE" }); await carregar(); recarregar(); }
    catch (err) { toast(err.message); }
  };
  const editar = (c) => setD({ id: c.id, nome: c.nome, paiId: c.paiId || "", ordem: c.ordem, ativo: c.ativo });
  const linha = (c, sub) => (
    <tr key={c.id} className={c.ativo ? "" : "off"}>
      <td style={{ paddingLeft: sub ? "2.2rem" : undefined }}>{sub ? "↳ " : ""}<b>{c.nome}</b>{!c.ativo && <small> (oculta)</small>}</td>
      <td><small>{c.slug}</small></td><td className="num">{c.ordem}</td>
      <td><div className="acts"><button className="mini-btn" onClick={() => editar(c)}>Editar</button><button className="mini-btn danger" onClick={() => remover(c)}>Excluir</button></div></td>
    </tr>
  );
  return (
    <>
      <Topo titulo="Categorias" />
      <form className="form" onSubmit={salvar} style={{ marginBottom: "1.4rem" }}>
        <h3>{d.id ? "Editar categoria" : "Nova categoria"}</h3>
        <div className="row">
          <label>Nome<input required maxLength={80} value={d.nome} onChange={(e) => setD({ ...d, nome: e.target.value })} /></label>
          <label>Dentro de (opcional)<select value={d.paiId} onChange={(e) => setD({ ...d, paiId: e.target.value })}>
            <option value="">— Categoria principal —</option>
            {lista.filter((c) => c.id !== d.id).map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
          </select></label>
          <label>Ordem no menu<input type="number" value={d.ordem} onChange={(e) => setD({ ...d, ordem: e.target.value })} /></label>
          <label className="switch" style={{ alignSelf: "end" }}><input type="checkbox" checked={d.ativo} onChange={(e) => setD({ ...d, ativo: e.target.checked })} /> Visível na loja</label>
        </div>
        <div style={{ display: "flex", gap: ".5rem" }}>
          <button className="btn btn-gold btn-sm">Salvar</button>
          {d.id && <button type="button" className="btn btn-line btn-sm" onClick={() => setD({ nome: "", paiId: "", ordem: 0, ativo: true })}>Cancelar</button>}
        </div>
      </form>
      <div className="tbl-wrap">
        <table className="tbl">
          <thead><tr><th>Categoria</th><th>Endereço</th><th className="num">Ordem</th><th /></tr></thead>
          <tbody>{lista.flatMap((c) => [linha(c, false), ...c.filhas.map((f) => linha(f, true))])}</tbody>
        </table>
      </div>
    </>
  );
}

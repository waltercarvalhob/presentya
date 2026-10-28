import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { useLoja } from "../lib/store";
import { brl, dataHora, STATUS } from "../lib/util";
import { Icon } from "../components/nav";

function nivel(compras) {
  if (compras >= 10) return ["Ouro", "10%"];
  if (compras >= 5) return ["Fita", "8%"];
  if (compras >= 2) return ["Laço", "5%"];
  return ["Iniciante", "—"];
}

export default function Conta() {
  const { usuario, sair, equipe, toast, atualizarUsuario } = useLoja();
  const nav = useNavigate();
  const [pedidos, setPedidos] = useState(null);
  const [d, setD] = useState({ nome: usuario.nome, telefone: usuario.telefone || "", senhaAtual: "", novaSenha: "" });
  const [erro, setErro] = useState(null);

  useEffect(() => { api("/api/pedidos/meus").then(setPedidos).catch(() => setPedidos([])); }, []);

  const salvar = async (e) => {
    e.preventDefault();
    setErro(null);
    try {
      await api("/api/usuarios/me", { method: "PUT", body: { ...d, novaSenha: d.novaSenha || null } });
      await atualizarUsuario();
      setD({ ...d, senhaAtual: "", novaSenha: "" });
      toast("Dados atualizados");
    } catch (err) { setErro(err.message); }
  };

  const cheias = usuario.comprasPagas % 5;
  const [nv, desc] = nivel(usuario.comprasPagas);

  return (
    <>
      <div className="wrap page-head" style={{ display: "flex", justifyContent: "space-between", alignItems: "end", gap: "1rem", flexWrap: "wrap" }}>
        <div><span className="eyebrow">Minha conta</span><h2>Olá, {usuario.nome.split(" ")[0]}!</h2></div>
        <div style={{ display: "flex", gap: ".5rem" }}>
          {equipe && <Link className="btn btn-dark btn-sm" to="/admin">Painel administrativo</Link>}
          <button className="btn btn-line btn-sm" onClick={() => { sair(); nav("/"); }}>Sair</button>
        </div>
      </div>
      <div className="wrap page-grid" style={{ paddingBottom: "4rem" }}>
        <div style={{ display: "grid", gap: "1.2rem", alignContent: "start" }}>
          <div className="form">
            <h3>Meus pedidos</h3>
            {pedidos === null && <p className="hand">Carregando…</p>}
            {pedidos?.length === 0 && <p style={{ color: "var(--muted)" }}>Você ainda não fez pedidos. <Link to="/#produtos"><b>Escolher um presente</b></Link></p>}
            <div className="orders">
              {pedidos?.map((p) => (
                <Link className="order-row" key={p.codigo} to={"/pedido/" + p.codigo}>
                  <div><b>{p.codigo}</b><br /><small>{dataHora(p.criadoEm)} · {p.itens.length} {p.itens.length === 1 ? "item" : "itens"}</small></div>
                  <span className={"status " + p.status}>{STATUS[p.status]}</span>
                  <b style={{ fontVariantNumeric: "tabular-nums" }}>{brl(p.total)}</b>
                </Link>
              ))}
            </div>
          </div>
          <form className="form" onSubmit={salvar}>
            <h3>Meus dados</h3>
            {erro && <div className="alert">{erro}</div>}
            <div className="row">
              <label>Nome<input required value={d.nome} onChange={(e) => setD({ ...d, nome: e.target.value })} /></label>
              <label>WhatsApp<input value={d.telefone} onChange={(e) => setD({ ...d, telefone: e.target.value })} /></label>
            </div>
            <label>E-mail<input value={usuario.email} disabled /></label>
            <div className="row">
              <label>Senha atual<input type="password" autoComplete="current-password" value={d.senhaAtual} onChange={(e) => setD({ ...d, senhaAtual: e.target.value })} /></label>
              <label>Nova senha<input type="password" minLength={6} autoComplete="new-password" value={d.novaSenha} onChange={(e) => setD({ ...d, novaSenha: e.target.value })} /></label>
            </div>
            <button className="btn btn-gold">Salvar</button>
          </form>
        </div>

        <aside className="stampcard sticky" aria-label="Minha cartela fidelidade">
          <div className="top"><h3>Sua cartela</h3><span className="hand">5ª compra = surpresa</span></div>
          <div className="stamps">
            {[1, 2, 3, 4].map((i) => <div key={i} className={"stamp" + (cheias >= i ? " done" : "")}>{i}ª</div>)}
            <div className={"stamp gift" + (usuario.comprasPagas > 0 && cheias === 0 ? " win" : "")}>{Icon.gift}</div>
          </div>
          <div className="tiers">
            <div className="tier"><b>{usuario.pontos}</b><small>pontos</small></div>
            <div className="tier"><b>{usuario.comprasPagas}</b><small>compras pagas</small></div>
            <div className="tier"><b>{desc}</b><small>Nível {nv}</small></div>
          </div>
          <p style={{ color: "var(--muted)", fontSize: ".9rem" }}>R$ 1 em compras = 1 ponto · 100 pontos = R$ 10 de desconto. Para usar seus pontos, fale com a loja no WhatsApp.</p>
        </aside>
      </div>
    </>
  );
}

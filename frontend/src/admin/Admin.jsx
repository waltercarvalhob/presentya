import { useEffect, useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import Logo from "../components/Logo";
import { Entrega, Itens } from "../pages/Pedido";
import { api } from "../lib/api";
import { useLoja } from "../lib/store";
import { brl, dataHora, PAGAMENTO, STATUS } from "../lib/util";

export function AdminLayout() {
  const { usuario, sair } = useLoja();
  const nav = useNavigate();
  const admin = usuario.perfil === "ADMIN";
  return (
    <div className="adm">
      <aside className="adm-side">
        <Link className="foot-logo" to="/" aria-label="Ver a loja"><Logo /></Link>
        <nav>
          <NavLink to="/admin" end>Visão geral</NavLink>
          <NavLink to="/admin/pedidos">Pedidos</NavLink>
          <NavLink to="/admin/produtos">Produtos</NavLink>
          {admin && <NavLink to="/admin/categorias">Categorias</NavLink>}
          {admin && <NavLink to="/admin/usuarios">Usuários</NavLink>}
          {admin && <NavLink to="/admin/loja">Dados da loja</NavLink>}
        </nav>
        <div className="who">
          <span>{usuario.nome}<br /><b style={{ color: "var(--brand)" }}>{usuario.perfil === "ADMIN" ? "Administrador" : "Técnico"}</b></span>
          <Link to="/">Ver a loja</Link>
          <Link to="/minha-conta">Minha conta</Link>
          <button onClick={() => { sair(); nav("/"); }}>Sair</button>
        </div>
      </aside>
      <main className="adm-main"><Outlet /></main>
    </div>
  );
}

export function Topo({ titulo, children }) {
  return <div className="adm-top"><h1>{titulo}</h1><div style={{ display: "flex", gap: ".5rem", flexWrap: "wrap" }}>{children}</div></div>;
}

export function Painel() {
  const [d, setD] = useState(null);
  const [erro, setErro] = useState(null);
  useEffect(() => { api("/api/admin/painel").then(setD).catch((e) => setErro(e.message)); }, []);
  if (erro) return <div className="alert">{erro}</div>;
  if (!d) return <p className="hand">Carregando…</p>;
  const k = [["Faturamento (pagos)", brl(d.faturamento)], ["Aguardando pagamento", d.aguardandoPagamento], ["Pagos (a produzir)", d.pagos],
    ["Em produção", d.emProducao], ["Saíram para entrega", d.enviados], ["Produtos ativos", d.produtosAtivos], ["Contas cadastradas", d.clientes]];
  return (
    <>
      <Topo titulo="Visão geral"><Link className="btn btn-gold btn-sm" to="/admin/produtos?novo=1">+ Novo produto</Link></Topo>
      <div className="cards">{k.map(([t, v]) => <div className="kpi" key={t}><span>{t}</span><b>{v}</b></div>)}</div>
      <h3 style={{ marginBottom: ".8rem" }}>Últimos pedidos</h3>
      <TabelaPedidos pedidos={d.ultimosPedidos} />
    </>
  );
}

function TabelaPedidos({ pedidos, aoMudar }) {
  const { toast } = useLoja();
  const [aberto, setAberto] = useState(null);
  const mudar = async (p, status) => {
    if (status === "CANCELADO" && !confirm(`Cancelar o pedido ${p.codigo}? O estoque será devolvido. Se já foi pago, faça o estorno no Mercado Pago.`)) return;
    try {
      const r = await api(`/api/admin/pedidos/${p.codigo}/status`, { method: "PATCH", body: { status } });
      aoMudar?.(r);
      toast(`Pedido ${p.codigo}: ${STATUS[r.status]}`);
    } catch (e) { toast(e.message); }
  };
  if (!pedidos.length) return <p style={{ color: "var(--muted)" }}>Nenhum pedido por aqui.</p>;
  return (
    <div className="tbl-wrap">
      <table className="tbl">
        <thead><tr><th>Pedido</th><th>Cliente</th><th>Pagamento</th><th>Entrega</th><th className="num">Total</th><th>Status</th><th /></tr></thead>
        <tbody>
          {pedidos.map((p) => [
            <tr key={p.codigo}>
              <td><b>{p.codigo}</b><br /><small>{dataHora(p.criadoEm)}</small></td>
              <td>{p.clienteNome}<br /><small>{p.clienteEmail}</small></td>
              <td>{PAGAMENTO[p.formaPagamento]}</td>
              <td>{p.tipoEntrega === "RETIRADA" ? "Retirada" : `${p.bairro || ""} · ${p.cidade || ""}`}</td>
              <td className="num">{brl(p.total)}</td>
              <td>{aoMudar && p.status !== "CANCELADO" ? (
                <select value={p.status} onChange={(e) => mudar(p, e.target.value)} aria-label={"Status do pedido " + p.codigo}>
                  {Object.entries(STATUS).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
                </select>
              ) : <span className={"status " + p.status}>{STATUS[p.status]}</span>}</td>
              <td><button className="mini-btn" onClick={() => setAberto(aberto === p.codigo ? null : p.codigo)}>{aberto === p.codigo ? "Fechar" : "Detalhes"}</button></td>
            </tr>,
            aberto === p.codigo && (
              <tr key={p.codigo + "-d"}><td colSpan={7}>
                <div className="detail">
                  <Itens p={p} />
                  <Entrega p={p} />
                  {p.mensagemCartao && <p><b>Mensagem do cartão:</b> “{p.mensagemCartao}”</p>}
                  {p.observacoes && <p><b>Observações:</b> {p.observacoes}</p>}
                </div>
              </td></tr>
            ),
          ])}
        </tbody>
      </table>
    </div>
  );
}

export function Pedidos() {
  const [status, setStatus] = useState("");
  const [lista, setLista] = useState(null);
  useEffect(() => { setLista(null); api("/api/admin/pedidos" + (status ? "?status=" + status : "")).then(setLista).catch(() => setLista([])); }, [status]);
  return (
    <>
      <Topo titulo="Pedidos" />
      <div className="tabs" role="tablist">
        <button role="tab" aria-selected={status === ""} onClick={() => setStatus("")}>Todos</button>
        {Object.entries(STATUS).map(([v, t]) => <button key={v} role="tab" aria-selected={status === v} onClick={() => setStatus(v)}>{t}</button>)}
      </div>
      {lista === null ? <p className="hand">Carregando…</p> : <TabelaPedidos pedidos={lista} aoMudar={(r) => setLista(lista.map((p) => (p.codigo === r.codigo ? r : p)))} />}
    </>
  );
}

export function Usuarios() {
  const { usuario, toast } = useLoja();
  const [lista, setLista] = useState([]);
  const [busca, setBusca] = useState("");
  const [novo, setNovo] = useState(false);
  useEffect(() => { api("/api/admin/usuarios").then(setLista).catch((e) => toast(e.message)); }, [toast]);
  const alterar = async (u, patch) => {
    try {
      const r = await api(`/api/admin/usuarios/${u.id}`, { method: "PATCH", body: patch });
      setLista(lista.map((x) => (x.id === r.id ? r : x)));
      toast("Usuário atualizado");
    } catch (e) { toast(e.message); }
  };
  const filtrados = lista.filter((u) => (u.nome + u.email).toLowerCase().includes(busca.toLowerCase()));
  return (
    <>
      <Topo titulo="Usuários e perfis">
        <input className="chip" placeholder="Buscar nome ou e-mail" value={busca} onChange={(e) => setBusca(e.target.value)} />
        <button className="btn btn-gold btn-sm" onClick={() => setNovo(!novo)}>{novo ? "Fechar" : "+ Novo usuário"}</button>
      </Topo>
      <p style={{ color: "var(--muted)", marginBottom: "1rem", maxWidth: "70ch" }}>
        <b>Administrador</b>: acesso total. <b>Técnico</b>: cadastra produtos e cuida dos pedidos (produção e entrega). <b>Usuário</b>: cliente da loja.
      </p>
      {novo && <NovoUsuario aoCriar={(u) => { setLista([...lista, u].sort((a, b) => a.nome.localeCompare(b.nome))); setNovo(false); }} />}
      <div className="tbl-wrap">
        <table className="tbl">
          <thead><tr><th>Nome</th><th>E-mail</th><th>Telefone</th><th className="num">Pontos</th><th>Perfil</th><th>Situação</th></tr></thead>
          <tbody>
            {filtrados.map((u) => (
              <tr key={u.id} className={u.ativo ? "" : "off"}>
                <td>{u.nome}</td><td>{u.email}</td><td>{u.telefone}</td><td className="num">{u.pontos}</td>
                <td>
                  <select value={u.perfil} disabled={u.id === usuario.id} onChange={(e) => alterar(u, { perfil: e.target.value })} aria-label={"Perfil de " + u.nome}>
                    <option value="USUARIO">Usuário</option><option value="TECNICO">Técnico</option><option value="ADMIN">Administrador</option>
                  </select>
                </td>
                <td><button className={"mini-btn" + (u.ativo ? " danger" : "")} disabled={u.id === usuario.id} onClick={() => alterar(u, { ativo: !u.ativo })}>{u.ativo ? "Desativar" : "Reativar"}</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

const USUARIO_VAZIO = { nome: "", email: "", telefone: "", senha: "", perfil: "TECNICO" };

function NovoUsuario({ aoCriar }) {
  const { toast } = useLoja();
  const [f, setF] = useState(USUARIO_VAZIO);
  const [erro, setErro] = useState(null);
  const [enviando, setEnviando] = useState(false);
  const campo = (k) => ({ value: f[k], onChange: (e) => setF({ ...f, [k]: e.target.value }) });
  const salvar = async (e) => {
    e.preventDefault();
    setErro(null); setEnviando(true);
    try {
      const u = await api("/api/admin/usuarios", { method: "POST", body: f });
      toast(`${u.nome} cadastrado`);
      aoCriar(u);
    } catch (err) { setErro(err.message); }
    finally { setEnviando(false); }
  };
  return (
    <form className="form" onSubmit={salvar} style={{ maxWidth: 760, marginBottom: "1.4rem" }}>
      <h3>Novo usuário</h3>
      {erro && <div className="alert">{erro}</div>}
      <div className="row">
        <label>Nome<input required maxLength={120} {...campo("nome")} /></label>
        <label>E-mail<input required type="email" maxLength={160} {...campo("email")} /></label>
        <label>Telefone<input maxLength={30} {...campo("telefone")} /></label>
        <label>Perfil
          <select {...campo("perfil")}>
            <option value="USUARIO">Usuário (cliente)</option><option value="TECNICO">Técnico</option><option value="ADMIN">Administrador</option>
          </select>
        </label>
        <label>Senha inicial<input required type="password" minLength={6} maxLength={72} autoComplete="new-password" {...campo("senha")} />
          <small>A pessoa pode trocar em "Minha conta".</small></label>
      </div>
      <button className="btn btn-gold" disabled={enviando}>{enviando ? "Salvando…" : "Cadastrar"}</button>
    </form>
  );
}

const CAMPOS = [
  ["whatsapp", "WhatsApp (só números, com 55 e DDD)"], ["whatsapp_exibicao", "WhatsApp (como aparece no site)"], ["email", "E-mail"],
  ["instagram", "Instagram (sem @)"], ["site", "Site"], ["horario_resumo", "Dias de funcionamento"],
  ["loja1_nome", "Loja 1 – nome"], ["loja1_horario", "Loja 1 – horários", true], ["loja2_nome", "Loja 2 – nome"], ["loja2_horario", "Loja 2 – horários", true],
  ["cidade_entrega", "Cidade com frete local"], ["frete_cidade", "Frete na cidade (R$)"], ["frete_brasil", "Frete para outras cidades (R$)"],
  ["aviso_topo", "Aviso na barra do topo (quando não há oferta)"],
];

export function DadosLoja() {
  const { recarregar, toast } = useLoja();
  const [d, setD] = useState(null);
  useEffect(() => { api("/api/admin/configuracoes").then(setD); }, []);
  if (!d) return <p className="hand">Carregando…</p>;
  const salvar = async (e) => {
    e.preventDefault();
    try { setD(await api("/api/admin/configuracoes", { method: "PUT", body: d })); await recarregar(); toast("Dados da loja salvos"); }
    catch (err) { toast(err.message); }
  };
  return (
    <>
      <Topo titulo="Dados da loja" />
      <form className="form" onSubmit={salvar} style={{ maxWidth: 760 }}>
        <div className="row">
          {CAMPOS.map(([k, t, multi]) => (
            <label key={k}>{t}{multi
              ? <textarea value={d[k] || ""} onChange={(e) => setD({ ...d, [k]: e.target.value })} />
              : <input value={d[k] || ""} onChange={(e) => setD({ ...d, [k]: e.target.value })} />}</label>
          ))}
        </div>
        <button className="btn btn-gold">Salvar</button>
      </form>
    </>
  );
}

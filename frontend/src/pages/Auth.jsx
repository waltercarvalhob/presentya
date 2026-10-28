import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { useLoja } from "../lib/store";

/** Protege páginas que exigem login (e, opcionalmente, um perfil). */
export function Protegido({ perfis, children }) {
  const { usuario, verificandoSessao } = useLoja();
  const loc = useLocation();
  if (verificandoSessao) return <section className="sec"><div className="wrap empty"><p className="hand">Carregando…</p></div></section>;
  if (!usuario) return <Navigate to={"/entrar?next=" + encodeURIComponent(loc.pathname + loc.search)} replace />;
  if (perfis && !perfis.includes(usuario.perfil)) return <Navigate to="/" replace />;
  return children;
}

function useDestino() {
  const [q] = useSearchParams();
  const next = q.get("next");
  return next && next.startsWith("/") && !next.startsWith("//") ? next : null;
}

export function Entrar() {
  const { entrar } = useLoja();
  const nav = useNavigate();
  const destino = useDestino();
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [erro, setErro] = useState(null);
  const [enviando, setEnviando] = useState(false);
  const enviar = async (e) => {
    e.preventDefault();
    setErro(null); setEnviando(true);
    try {
      const u = await entrar(email, senha);
      nav(destino || (u.perfil === "USUARIO" ? "/minha-conta" : "/admin"), { replace: true });
    } catch (err) { setErro(err.message); } finally { setEnviando(false); }
  };
  return (
    <section className="sec tint">
      <div className="wrap auth-box">
        <div className="head center"><span className="eyebrow">Minha conta</span><h2>Entrar</h2></div>
        <form className="form" onSubmit={enviar}>
          {erro && <div className="alert" role="alert">{erro}</div>}
          <label>E-mail<input type="email" required autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} /></label>
          <label>Senha<input type="password" required autoComplete="current-password" value={senha} onChange={(e) => setSenha(e.target.value)} /></label>
          <button className="btn btn-gold" disabled={enviando}>{enviando ? "Entrando…" : "Entrar"}</button>
          <small>Ainda não tem conta? <Link to={"/cadastro" + (destino ? "?next=" + encodeURIComponent(destino) : "")}><b>Cadastre-se grátis</b></Link> e entre para o Clube PresentYá.</small>
        </form>
      </div>
    </section>
  );
}

export function Cadastro() {
  const { registrar, toast } = useLoja();
  const nav = useNavigate();
  const destino = useDestino();
  const [d, setD] = useState({ nome: "", email: "", telefone: "", senha: "" });
  const [erro, setErro] = useState(null);
  const [enviando, setEnviando] = useState(false);
  const campo = (k) => ({ value: d[k], onChange: (e) => setD({ ...d, [k]: e.target.value }) });
  const enviar = async (e) => {
    e.preventDefault();
    setErro(null); setEnviando(true);
    try {
      await registrar(d);
      toast("Bem-vindo(a) ao Clube PresentYá! ♡");
      nav(destino || "/minha-conta", { replace: true });
    } catch (err) { setErro(err.message); } finally { setEnviando(false); }
  };
  return (
    <section className="sec tint">
      <div className="wrap auth-box">
        <div className="head center"><span className="eyebrow">Clube PresentYá</span><h2>Criar conta</h2><p className="lead">Acompanhe seus pedidos e acumule pontos a cada compra.</p></div>
        <form className="form" onSubmit={enviar}>
          {erro && <div className="alert" role="alert">{erro}</div>}
          <label>Nome<input required maxLength={120} autoComplete="name" placeholder="Como podemos te chamar?" {...campo("nome")} /></label>
          <label>E-mail<input type="email" required autoComplete="email" {...campo("email")} /></label>
          <label>WhatsApp<input type="tel" autoComplete="tel" placeholder="(98) 9 0000-0000" {...campo("telefone")} /></label>
          <label>Senha<input type="password" required minLength={6} maxLength={72} autoComplete="new-password" {...campo("senha")} /></label>
          <button className="btn btn-gold" disabled={enviando}>{enviando ? "Criando…" : "Criar minha conta"}</button>
          <small>Já tem conta? <Link to={"/entrar" + (destino ? "?next=" + encodeURIComponent(destino) : "")}><b>Entrar</b></Link></small>
        </form>
      </div>
    </section>
  );
}

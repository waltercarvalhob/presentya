import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { api, getToken, quandoSessaoExpirar, setToken } from "./api";
import { produtoUI, waLink } from "./util";

const Ctx = createContext(null);
export const useLoja = () => useContext(Ctx);

export const FILTRO_VAZIO = { cat: null, novidade: false, occ: [], para: [], price: null, perso: false, sort: "pop" };

const ler = (k, d) => { try { return JSON.parse(localStorage.getItem(k)) ?? d; } catch { return d; } };
const gravar = (k, v) => { try { localStorage.setItem(k, JSON.stringify(v)); } catch { /* sem storage */ } };

export function LojaProvider({ children }) {
  const [produtos, setProdutos] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [config, setConfig] = useState({});
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState(null);
  const [usuario, setUsuario] = useState(null);
  const [verificandoSessao, setVerificandoSessao] = useState(!!getToken());
  const [wish, setWish] = useState(() => ler("py-wish", []));
  const [cart, setCart] = useState(() => ler("py-cart", []));
  const [drawer, setDrawer] = useState(null);
  const [toastMsg, setToastMsg] = useState(null);
  const [filtro, setFiltro] = useState(FILTRO_VAZIO);
  const toastTimer = useRef();

  const carregar = useCallback(async () => {
    try {
      const [p, c, cfg] = await Promise.all([api("/api/produtos"), api("/api/categorias"), api("/api/configuracoes")]);
      setProdutos(p.map(produtoUI));
      setCategorias(c);
      setConfig(cfg);
      setErro(null);
    } catch (e) {
      setErro(e.message);
    } finally {
      setCarregando(false);
    }
  }, []);

  const sair = useCallback(() => { setToken(null); setUsuario(null); }, []);

  useEffect(() => {
    carregar();
    quandoSessaoExpirar(sair);
    if (getToken()) {
      api("/api/usuarios/me").then(setUsuario).catch(sair).finally(() => setVerificandoSessao(false));
    }
  }, [carregar, sair]);

  useEffect(() => gravar("py-wish", wish), [wish]);
  useEffect(() => gravar("py-cart", cart), [cart]);

  const toast = useCallback((t) => {
    setToastMsg(null);
    requestAnimationFrame(() => setToastMsg(t));
    clearTimeout(toastTimer.current);
    toastTimer.current = setTimeout(() => setToastMsg(null), 2200);
  }, []);

  const entrar = async (email, senha) => {
    const r = await api("/api/auth/login", { method: "POST", body: { email, senha } });
    setToken(r.token); setUsuario(r.usuario); return r.usuario;
  };
  const registrar = async (dados) => {
    const r = await api("/api/auth/registrar", { method: "POST", body: dados });
    setToken(r.token); setUsuario(r.usuario); return r.usuario;
  };
  const atualizarUsuario = async () => setUsuario(await api("/api/usuarios/me"));

  const porId = useMemo(() => new Map(produtos.map((p) => [p.id, p])), [produtos]);

  const toggleWish = (id) => {
    const tem = wish.includes(id);
    setWish(tem ? wish.filter((x) => x !== id) : [...wish, id]);
    toast(tem ? "Removido da lista de desejos" : "Salvo na sua lista de desejos ♡");
  };

  const addCart = (id, qtd = 1) => {
    setCart((c) => (c.some((x) => x.id === id)
      ? c.map((x) => (x.id === id ? { ...x, qtd: Math.min(50, x.qtd + qtd) } : x))
      : [...c, { id, qtd, perso: "" }]));
    toast("Adicionado à sacola 🛍️");
    setDrawer("cart");
  };
  const setQtd = (id, qtd) => setCart((c) => (qtd <= 0 ? c.filter((x) => x.id !== id) : c.map((x) => (x.id === id ? { ...x, qtd: Math.min(50, qtd) } : x))));
  const setPerso = (id, perso) => setCart((c) => c.map((x) => (x.id === id ? { ...x, perso } : x)));
  const limparCart = () => setCart([]);

  const itensCart = cart.map((i) => ({ ...i, p: porId.get(i.id) })).filter((i) => i.p);
  const subtotal = itensCart.reduce((s, i) => s + i.p.price * i.qtd, 0);
  const wa = (texto) => waLink(config.whatsapp || "5598984084256", texto);
  const equipe = usuario && (usuario.perfil === "ADMIN" || usuario.perfil === "TECNICO");

  const valor = {
    produtos, categorias, config, carregando, erro, recarregar: carregar, porId,
    usuario, verificandoSessao, entrar, registrar, sair, atualizarUsuario, equipe,
    wish, toggleWish, cart: itensCart, addCart, setQtd, setPerso, limparCart, subtotal,
    drawer, setDrawer, toast, toastMsg, filtro, setFiltro, wa,
  };
  return <Ctx.Provider value={valor}>{children}</Ctx.Provider>;
}

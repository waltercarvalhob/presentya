import { lazy, Suspense } from "react";
import { Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import Home from "./pages/Home";
import Checkout from "./pages/Checkout";
import Conta from "./pages/Conta";
import Pedido, { PagamentoSimulado } from "./pages/Pedido";
import { Cadastro, Entrar, Protegido } from "./pages/Auth";
import Ofertas from "./pages/Ofertas";
import Privacidade from "./pages/Privacidade";

// O painel só é baixado por quem entra nele: a loja do cliente abre mais leve.
const tela = (carregar, nome) => lazy(() => carregar().then((m) => ({ default: m[nome] })));
const admin = () => import("./admin/Admin");
const catalogo = () => import("./admin/Catalogo");
const promocao = () => import("./admin/Promocao");
const AdminLayout = tela(admin, "AdminLayout"), Painel = tela(admin, "Painel"), Pedidos = tela(admin, "Pedidos");
const Usuarios = tela(admin, "Usuarios"), DadosLoja = tela(admin, "DadosLoja");
const Produtos = tela(catalogo, "Produtos"), Categorias = tela(catalogo, "Categorias");
const Divulgacao = tela(promocao, "Divulgacao"), Interessados = tela(promocao, "Interessados"), Cartaz = tela(promocao, "Cartaz");
const PlacasNfc = tela(() => import("./admin/Nfc"), "PlacasNfc");
const Carregando = () => <p className="hand" style={{ padding: "2rem" }}>Carregando…</p>;

const EQUIPE = ["ADMIN", "TECNICO"];
const ADMIN = ["ADMIN"];

export default function App() {
  return (
    <Suspense fallback={<Carregando />}>
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Home />} />
        <Route path="entrar" element={<Entrar />} />
        <Route path="cadastro" element={<Cadastro />} />
        <Route path="checkout" element={<Checkout />} />
        <Route path="ofertas" element={<Ofertas />} />
        <Route path="privacidade" element={<Privacidade />} />
        <Route path="minha-conta" element={<Protegido><Conta /></Protegido>} />
        <Route path="pedido/:codigo" element={<Pedido />} />
        <Route path="pagamento-simulado/:codigo" element={<PagamentoSimulado />} />
        <Route path="*" element={<section className="sec"><div className="wrap empty"><h2>Página não encontrada</h2><a className="btn btn-gold" href="/">Voltar à loja</a></div></section>} />
      </Route>
      <Route path="admin" element={<Protegido perfis={EQUIPE}><AdminLayout /></Protegido>}>
        <Route index element={<Painel />} />
        <Route path="pedidos" element={<Pedidos />} />
        <Route path="produtos" element={<Produtos />} />
        <Route path="nfc" element={<PlacasNfc />} />
        <Route path="categorias" element={<Protegido perfis={ADMIN}><Categorias /></Protegido>} />
        <Route path="usuarios" element={<Protegido perfis={ADMIN}><Usuarios /></Protegido>} />
        <Route path="loja" element={<Protegido perfis={ADMIN}><DadosLoja /></Protegido>} />
        <Route path="ofertas" element={<Protegido perfis={ADMIN}><Divulgacao /></Protegido>} />
        <Route path="interessados" element={<Protegido perfis={ADMIN}><Interessados /></Protegido>} />
      </Route>
      <Route path="cartaz" element={<Protegido perfis={ADMIN}><Cartaz /></Protegido>} />
    </Routes>
    </Suspense>
  );
}

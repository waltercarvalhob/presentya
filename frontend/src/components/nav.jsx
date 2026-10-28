import { Link, useLocation, useNavigate } from "react-router-dom";
import { FILTRO_VAZIO, useLoja } from "../lib/store";
import { RM } from "../lib/util";

/** Link para uma seção da página inicial, funcionando de qualquer página. */
export function SecLink({ id, children, ...rest }) {
  const { pathname } = useLocation();
  if (pathname === "/") return <a href={"#" + id} {...rest}>{children}</a>;
  return <Link to={"/#" + id} {...rest}>{children}</Link>;
}

/** Aplica um filtro no catálogo e leva o cliente até ele. */
export function useIrCatalogo() {
  const { setFiltro } = useLoja();
  const nav = useNavigate();
  const { pathname } = useLocation();
  return (f = {}) => {
    setFiltro({ ...FILTRO_VAZIO, ...f });
    if (pathname === "/") {
      document.getElementById("catalogo")?.scrollIntoView({ behavior: RM ? "auto" : "smooth", block: "start" });
    } else {
      nav("/#catalogo");
    }
  };
}

export const Icon = {
  heart: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z" /></svg>,
  bag: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M5 8h14l-1 12H6z" /><path d="M9 8V6a3 3 0 0 1 6 0v2" /></svg>,
  user: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><circle cx="12" cy="8" r="3.5" /><path d="M5 20c0-4 3.2-6.5 7-6.5s7 2.5 7 6.5" /></svg>,
  menu: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M4 7h16M4 12h16M4 17h16" /></svg>,
  gift: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><rect x="4" y="9" width="16" height="11" rx="1" /><path d="M12 9v11M4 13h16M12 9c-2-4-6-3-5 0M12 9c2-4 6-3 5 0" /></svg>,
  wa: <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2a10 10 0 0 0-8.6 15.1L2 22l5-1.3A10 10 0 1 0 12 2zm0 18.2a8.2 8.2 0 0 1-4.2-1.2l-.3-.2-3 .8.8-2.9-.2-.3A8.2 8.2 0 1 1 12 20.2zm4.5-6.1c-.2-.1-1.5-.7-1.7-.8s-.4-.1-.6.1-.6.8-.8 1-.3.2-.5.1a6.7 6.7 0 0 1-3.3-2.9c-.3-.4.2-.4.7-1.4a.5.5 0 0 0 0-.4l-.8-1.8c-.2-.5-.4-.4-.6-.4h-.5a1 1 0 0 0-.7.3 3 3 0 0 0-.9 2.2 5.2 5.2 0 0 0 1.1 2.7 11.8 11.8 0 0 0 4.5 4c1.7.7 2.3.8 3.2.6a2.7 2.7 0 0 0 1.8-1.2 2.2 2.2 0 0 0 .1-1.2c0-.1-.2-.2-.4-.3z" /></svg>,
};

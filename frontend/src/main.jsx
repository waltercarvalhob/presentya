import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { LojaProvider } from "./lib/store";
import "./styles/site.css";

document.documentElement.classList.add("js");

// App instalável (PWA): o service worker também recebe as notificações de ofertas.
if ("serviceWorker" in navigator && import.meta.env.PROD) {
  addEventListener("load", () => navigator.serviceWorker.register("/sw.js").catch(() => {}));
}

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <BrowserRouter>
      <LojaProvider>
        <App />
      </LojaProvider>
    </BrowserRouter>
  </StrictMode>
);

import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { LojaProvider } from "./lib/store";
import "./styles/site.css";

document.documentElement.classList.add("js");

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <BrowserRouter>
      <LojaProvider>
        <App />
      </LojaProvider>
    </BrowserRouter>
  </StrictMode>
);

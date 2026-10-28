import { imagemUrl } from "./api";

export const brl = (n) => Number(n || 0).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
export const dataHora = (iso) => new Date(iso).toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
export const RM = typeof matchMedia !== "undefined" && matchMedia("(prefers-reduced-motion: reduce)").matches;

export const waLink = (numero, texto) => `https://wa.me/${numero}?text=${encodeURIComponent(texto)}`;

export const OCC = { aniversario: "Aniversário", namoro: "Namoro", natal: "Natal", maes: "Dia das Mães", pais: "Dia dos Pais", corporativo: "Corporativo" };
export const PARA = { ela: "Para Ela", ele: "Para Ele", criancas: "Crianças", familia: "Família", empresa: "Empresas" };
export const PRICES = { "0-50": "Até R$ 50", "50-120": "R$ 50 a 120", "120-9999": "Acima de R$ 120" };

export const STATUS = {
  AGUARDANDO_PAGAMENTO: "Aguardando pagamento",
  PAGO: "Pago",
  EM_PRODUCAO: "Em produção",
  ENVIADO: "Saiu para entrega",
  ENTREGUE: "Entregue",
  CANCELADO: "Cancelado",
};
export const PAGAMENTO = { CREDITO: "Cartão de crédito", DEBITO: "Cartão de débito", PIX: "Pix" };

/** Converte o produto da API para o formato usado pela vitrine (o mesmo do site original). */
export function produtoUI(d) {
  const promo = d.precoPromocional != null && Number(d.precoPromocional) < Number(d.preco);
  return {
    id: d.id,
    name: d.nome,
    desc: d.descricao || "",
    price: Number(d.precoAtual),
    old: promo ? Number(d.preco) : null,
    occ: d.ocasioes || [],
    para: d.publicos || [],
    perso: d.personalizavel,
    kind: d.ilustracao || "box",
    bg: d.corFundo, a: d.corA, b: d.corB,
    tags: [d.novidade && "new", d.maisVendido && "hot", promo && "off"].filter(Boolean),
    pop: d.vendidos,
    novo: new Date(d.criadoEm).getTime(),
    novidade: d.novidade,
    img: imagemUrl(d.imagemUrl),
    categoriaId: d.categoriaId,
    categoriaNome: d.categoriaNome,
    categoriaSlug: d.categoriaSlug,
    estoque: d.estoque,
    raw: d,
  };
}

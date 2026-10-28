// Ilustrações dos produtos (as mesmas do site original). Aparecem quando o produto ainda não tem foto.
const COR = /^(var\(--[a-z-]{1,30}\)|#[0-9a-fA-F]{3,8})$/;
const cor = (c, padrao) => (c && COR.test(c) ? c : padrao);

export const ILUSTRACOES = {
  box: "Caixa de presente", tall: "Caixa alta", mug: "Caneca", frame: "Quadro", basket: "Cesta", lamp: "Luminária",
  bear: "Ursinho", spa: "Kit spa", key: "Chaveiro", tree: "Árvore de Natal", choc: "Coração de bombons", card: "Cartão presente",
};

export function artSvg(kind, a0, b0) {
  const a = cor(a0, "var(--rose)"), b = cor(b0, "var(--gold-bright)");
  const S = (c) => `<svg viewBox="0 0 200 160" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">${c}</svg>`;
  const sh = `<ellipse cx="100" cy="146" rx="62" ry="7" fill="var(--ink)" opacity=".08"/>`;
  switch (kind) {
    case "tall": return S(sh + `<rect x="62" y="40" width="76" height="104" rx="4" fill="${a}"/><rect x="58" y="30" width="84" height="18" rx="4" fill="${a}" style="filter:brightness(1.06)"/><rect x="94" y="30" width="12" height="114" fill="${b}"/><rect x="62" y="84" width="76" height="10" fill="${b}"/><path d="M100 30c-10-18-32-16-26-3 5 7 19 4 26 3zM100 30c10-18 32-16 26-3-5 7-19 4-26 3z" fill="${b}"/>`);
    case "mug": return S(sh + `<path d="M134 70h10a18 18 0 0 1 0 36h-10" fill="none" stroke="${a}" stroke-width="10"/><rect x="56" y="54" width="82" height="90" rx="10" fill="${a}"/><ellipse cx="97" cy="56" rx="41" ry="7" fill="var(--ink)" opacity=".15"/><path d="M97 112c-14-9-20-16-20-23a9 9 0 0 1 20-3 9 9 0 0 1 20 3c0 7-6 14-20 23z" fill="${b}"/><path d="M84 40c0-8 8-8 8-16M104 40c0-8 8-8 8-16" stroke="var(--muted)" stroke-width="3" fill="none" stroke-linecap="round" opacity=".5"/>`);
    case "frame": return S(sh + `<rect x="46" y="22" width="108" height="122" rx="3" fill="${b}"/><rect x="58" y="34" width="84" height="98" fill="var(--card)"/><rect x="64" y="40" width="72" height="86" fill="${a}" opacity=".55"/><circle cx="84" cy="66" r="9" fill="var(--card)"/><path d="M64 126l26-34 16 18 12-12 18 28z" fill="${a}"/>`);
    case "basket": return S(sh + `<path d="M52 82c0-46 96-46 96 0" fill="none" stroke="${b}" stroke-width="7"/><circle cx="80" cy="76" r="14" fill="${a}"/><rect x="96" y="56" width="18" height="30" rx="4" fill="var(--sage)"/><circle cx="124" cy="78" r="11" fill="${b}" style="filter:brightness(1.1)"/><path d="M40 84h120l-14 58H54z" fill="${b}"/><path d="M48 98h104M52 112h96M56 126h88" stroke="var(--card)" stroke-width="2" opacity=".5"/>`);
    case "lamp": return S(sh + `<circle cx="100" cy="70" r="52" fill="${b}" opacity=".18"/><path d="M68 34h64l14 50H54z" fill="${a}"/><rect x="96" y="84" width="8" height="44" fill="${b}"/><rect x="70" y="128" width="60" height="14" rx="6" fill="${b}"/><text x="100" y="66" text-anchor="middle" font-family="Caveat,cursive" font-size="20" fill="var(--ink)" opacity=".7">Ana</text>`);
    case "bear": return S(sh + `<circle cx="70" cy="44" r="14" fill="${a}"/><circle cx="130" cy="44" r="14" fill="${a}"/><circle cx="100" cy="64" r="34" fill="${a}"/><ellipse cx="100" cy="116" rx="40" ry="30" fill="${a}"/><ellipse cx="100" cy="74" rx="13" ry="10" fill="var(--card)" opacity=".7"/><circle cx="88" cy="58" r="3.5" fill="var(--ink)"/><circle cx="112" cy="58" r="3.5" fill="var(--ink)"/><path d="M100 114c-8-5-12-10-12-14a6 6 0 0 1 12-2 6 6 0 0 1 12 2c0 4-4 9-12 14z" fill="${b}"/>`);
    case "spa": return S(sh + `<rect x="40" y="96" width="120" height="48" rx="6" fill="${b}"/><rect x="56" y="48" width="26" height="56" rx="6" fill="${a}"/><rect x="62" y="38" width="14" height="12" fill="var(--ink)" opacity=".5"/><rect x="92" y="62" width="40" height="40" rx="20" fill="var(--sage)"/><path d="M112 70c-4 8-4 16 0 24 4-8 4-16 0-24z" fill="var(--card)" opacity=".7"/><rect x="138" y="70" width="14" height="32" rx="3" fill="${a}" style="filter:brightness(1.1)"/>`);
    case "key": return S(sh + `<circle cx="76" cy="68" r="26" fill="none" stroke="${b}" stroke-width="7"/><rect x="96" y="80" width="62" height="40" rx="8" fill="${a}" transform="rotate(18 127 100)"/><text x="127" y="106" text-anchor="middle" font-family="Marcellus,serif" font-size="18" fill="var(--ink)" transform="rotate(18 127 100)">M&amp;J</text>`);
    case "tree": return S(sh + `<path d="M100 18l40 52h-18l26 34H52l26-34H60z" fill="var(--sage)"/><rect x="92" y="104" width="16" height="16" fill="${b}"/><rect x="62" y="116" width="34" height="28" rx="3" fill="${a}"/><rect x="76" y="116" width="6" height="28" fill="${b}"/><rect x="106" y="122" width="30" height="22" rx="3" fill="${b}"/><circle cx="100" cy="18" r="6" fill="${b}"/><circle cx="88" cy="62" r="4" fill="${a}"/><circle cx="114" cy="84" r="4" fill="${a}"/>`);
    case "choc": return S(sh + `<path d="M100 60c-34-40-84-6-60 34 12 20 40 36 60 50 20-14 48-30 60-50 24-40-26-74-60-34z" fill="${a}"/><circle cx="78" cy="80" r="9" fill="var(--ink)" opacity=".55"/><circle cx="100" cy="96" r="9" fill="var(--ink)" opacity=".55"/><circle cx="122" cy="80" r="9" fill="var(--ink)" opacity=".55"/><circle cx="100" cy="118" r="9" fill="var(--ink)" opacity=".55"/>`);
    case "card": return S(sh + `<rect x="34" y="40" width="132" height="86" rx="10" fill="${a}" transform="rotate(-6 100 83)"/><rect x="34" y="40" width="132" height="86" rx="10" fill="none" stroke="${b}" stroke-width="2" transform="rotate(-6 100 83)" opacity=".25"/><rect x="40" y="58" width="132" height="12" fill="${b}" transform="rotate(-6 100 83)"/><path d="M118 98c-8-5-12-9-12-13a6 6 0 0 1 12-2 6 6 0 0 1 12 2c0 4-4 8-12 13z" fill="${b}" transform="rotate(-6 100 83)"/><text x="72" y="112" font-family="Poppins,sans-serif" font-weight="700" font-size="15" fill="${b}" transform="rotate(-6 100 83)">PresentYá</text>`);
    default: return S(sh + `<rect x="42" y="66" width="116" height="78" rx="4" fill="${a}"/><rect x="36" y="52" width="128" height="22" rx="4" fill="${a}" style="filter:brightness(1.06)"/><rect x="92" y="52" width="16" height="92" fill="${b}"/><path d="M100 52c-14-24-44-22-36-4 6 10 26 6 36 4z" fill="${b}"/><path d="M100 52c14-24 44-22 36-4-6 10-26 6-36 4z" fill="${b}"/><circle cx="100" cy="51" r="6" fill="${b}" style="filter:brightness(.9)"/>`);
  }
}

/** Foto do produto, ou a ilustração quando não houver foto. */
export function Art({ p }) {
  if (p.img) return <img className="foto" src={p.img} alt="" loading="lazy" />;
  return <span className="artsvg" dangerouslySetInnerHTML={{ __html: artSvg(p.kind, p.a, p.b) }} />;
}
